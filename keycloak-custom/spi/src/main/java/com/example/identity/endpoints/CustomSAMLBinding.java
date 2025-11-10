package com.example.identity.endpoints;

import com.example.config.CustomSAMLIdentityProviderConfig;
import com.example.identity.CustomSAMLProvider;
import com.example.utils.SamlUtil;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.dom.saml.v2.assertion.*;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SAMLDecryptionKeysLocator;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlSessionUtils;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.saml.validators.ConditionsValidator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.keycloak.broker.saml.SAMLEndpoint.*;

public abstract class CustomSAMLBinding extends CustomBinding {
    public CustomSAMLBinding(RealmModel realm,
                             CustomSAMLIdentityProviderConfig config,
                             KeycloakSession session,
                             ClientConnection clientConnection,
                             DestinationValidator destinationValidator,
                             HttpHeaders headers,
                             IdentityProvider.AuthenticationCallback callback,
                             CustomSAMLProvider provider,
                             EventBuilder event) {
        super(realm, config, session, clientConnection, destinationValidator, headers, callback, provider, event);
    }

    @Override
    protected Response handleSamlRequest(String samlRequest, String relayState) {
        var holder = extractRequestDocument(samlRequest);
        var requestAbstractType = (RequestAbstractType) holder.getSamlObject();
        // validate destination
        if (isDestinationRequired()
                && requestAbstractType.getDestination() == null
                && containsUnencryptedSignature(holder)) {
            event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
            event.detail(Details.REASON, Errors.MISSING_REQUIRED_DESTINATION);
            event.error(Errors.INVALID_REQUEST);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
        }

        if (!destinationValidator.validate(getExpectedDestination(config.getAlias(), null), requestAbstractType.getDestination())) {
            event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
            event.detail(Details.REASON, Errors.INVALID_DESTINATION);
            event.error(Errors.INVALID_SAML_RESPONSE);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
        }

        if (config.isValidateSignature()) {
            try {
                verifySignature(GeneralConstants.SAML_REQUEST_KEY, holder);
            } catch (VerificationException e) {
                logger.error("validation failed", e);
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SIGNATURE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }
        }

        if (requestAbstractType instanceof LogoutRequestType logout) {
            logger.debug("** logout request");
            event.event(EventType.LOGOUT);
            return logoutRequest(logout, relayState);

        } else {
            event.event(EventType.LOGIN);
            event.error(Errors.INVALID_TOKEN);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
        }
    }

    @Override
    protected Response handleSamlResponse(String samlResponse, String relayState, String clientId) {
        var holder = extractResponseDocument(samlResponse);
        if (holder == null) {
            event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
            event.detail(Details.REASON, Errors.INVALID_SAML_DOCUMENT);
            event.error(Errors.INVALID_SAML_RESPONSE);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_INVALID_RESPONSE);
        }

        // validate destination
        var statusResponse = (StatusResponseType) holder.getSamlObject();
        if (isDestinationRequired()
                && statusResponse.getDestination() == null
                && containsUnencryptedSignature(holder)) {
            event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
            event.detail(Details.REASON, Errors.MISSING_REQUIRED_DESTINATION);
            event.error(Errors.INVALID_SAML_RESPONSE);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
        }

        if (!destinationValidator.validate(getExpectedDestination(config.getAlias(), clientId), statusResponse.getDestination())) {
            event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
            event.detail(Details.REASON, Errors.INVALID_DESTINATION);
            event.error(Errors.INVALID_SAML_RESPONSE);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
        }

        if (config.isValidateSignature()) {
            try {
                verifySignature(GeneralConstants.SAML_RESPONSE_KEY, holder);
            } catch (VerificationException e) {
                logger.error("validation failed", e);
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SIGNATURE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_INVALID_SIGNATURE);
            }
        }
        if (statusResponse instanceof ResponseType) {
            return handleLoginResponse(samlResponse, holder, (ResponseType) statusResponse, relayState, clientId);
        }

        // todo need to check that it is actually a LogoutResponse
        return handleLogoutResponse(holder, statusResponse, relayState);
    }

    @Override
    protected Response handleLoginResponse(String samlResponse, SAMLDocumentHolder holder, ResponseType responseType, String relayState, String clientId) {
        try {
            AuthenticationSessionModel authSession;
            if (StringUtil.isNotBlank(clientId)) {
                authSession = samlIdpInitiatedSSO(clientId);
            } else if (StringUtil.isNotBlank(relayState)) {
                authSession = callback.getAndVerifyAuthenticationSession(relayState);
            } else {
                logger.error("SAML RelayState parameter was null when it should be returned by the IDP");
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            session.getContext().setAuthenticationSession(authSession);

            if (!isSuccessfulSamlResponse(responseType)) {
                var statusMessage = responseType.getStatus() == null || responseType.getStatus().getStatusMessage() == null ? Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR : responseType.getStatus().getStatusMessage();
                if (Constants.AUTHENTICATION_EXPIRED_MESSAGE.equals(statusMessage)) {
                    return callback.retryLogin(provider, authSession);
                } else {
                    return callback.error(statusMessage);
                }
            }
            if (responseType.getAssertions() == null || responseType.getAssertions().isEmpty()) {
                return callback.error(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }

            var assertionIsEncrypted = AssertionUtil.isAssertionEncrypted(responseType);
            if (config.isWantAssertionsEncrypted() && !assertionIsEncrypted) {
                logger.error("The assertion is not encrypted, which is required.");
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            Element assertionElement;
            if (assertionIsEncrypted) {
                try {
                    var decryptionKeyLocator = new SAMLDecryptionKeysLocator(session, realm, config.getEncryptionAlgorithm());
                    assertionElement = AssertionUtil.decryptAssertion(responseType, decryptionKeyLocator);
                } catch (ProcessingException ex) {
                    logger.warnf(ex, "Not possible to decrypt SAML assertion. Please check realm keys of usage ENC in the realm '%s' and make sure there is a key able to decrypt the assertion encrypted by identity provider '%s'", realm.getName(), config.getAlias());
                    throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
                }
            } else {
                    /* We verify the assertion using original document to handle cases where the IdP
                    includes whitespace and/or newlines inside tags. */
                assertionElement = DocumentUtil.getElement(holder.getSamlDocument(), new QName(JBossSAMLConstants.ASSERTION.get()));
            }

            // Validate the response Issuer
            final var responseIssuer = responseType.getIssuer() != null ? responseType.getIssuer().getValue() : null;
            final var responseIssuerValidationSuccess = config.getIdpEntityId() == null
                    || (responseIssuer != null && responseIssuer.equals(config.getIdpEntityId()));
            if (!responseIssuerValidationSuccess) {
                logger.errorf("Response Issuer validation failed: expected %s, actual %s", config.getIdpEntityId(), responseIssuer);
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            // Validate InResponseTo attribute: must match the generated request ID
            var expectedRequestId = authSession.getClientNote(SamlProtocol.SAML_REQUEST_ID_BROKER);
            final var inResponseToValidationSuccess = validateInResponseToAttribute(responseType, expectedRequestId);
            if (!inResponseToValidationSuccess) {
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            var signed = AssertionUtil.isSignedElement(assertionElement);
            final var assertionSignatureNotExistsWhenRequired = config.isWantAssertionsSigned() && !signed;
            final var signatureNotValid = signed && config.isValidateSignature() && !AssertionUtil.isSignatureValid(assertionElement, getIDPKeyLocator());
            final var hasNoSignatureWhenRequired = !signed && config.isValidateSignature() && !containsUnencryptedSignature(holder);

            if (assertionSignatureNotExistsWhenRequired || signatureNotValid || hasNoSignatureWhenRequired) {
                logger.error("validation failed");
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SIGNATURE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            if (AssertionUtil.isIdEncrypted(responseType)) {
                try {
                    XMLEncryptionUtil.DecryptionKeyLocator decryptionKeyLocator = new SAMLDecryptionKeysLocator(session, realm, config.getEncryptionAlgorithm());
                    AssertionUtil.decryptId(responseType, decryptionKeyLocator);
                } catch (ProcessingException ex) {
                    logger.warnf(ex, "Not possible to decrypt SAML encryptedId. Please check realm keys of usage ENC in the realm '%s' and make sure there is a key able to decrypt the encryptedId encrypted by identity provider '%s'", realm.getName(), config.getAlias());
                    throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
                }
            }

            // Validate the assertion Issuer
            var assertion = responseType.getAssertions().get(0).getAssertion();
            final var assertionIssuer = assertion.getIssuer() != null ? assertion.getIssuer().getValue() : null;
            final var assertionIssuerValidationSuccess = config.getIdpEntityId() == null
                    || (assertionIssuer != null && assertionIssuer.equals(config.getIdpEntityId()));
            if (!assertionIssuerValidationSuccess) {
                logger.errorf("Assertion Issuer validation failed: expected %s, actual %s", config.getIdpEntityId(), assertionIssuer);
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            var subjectNameID = getSubjectNameID(assertion);
            var principal = getPrincipal(assertion);
            if (principal == null) {
                logger.errorf("no principal in assertion; expected: %s", expectedPrincipalType());
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            //Map<String, String> notes = new HashMap<>();
            var identity = new BrokeredIdentityContext(principal, config);
            identity.getContextData().put(SAML_LOGIN_RESPONSE, responseType);
            identity.getContextData().put(SAML_ASSERTION, assertion);
            identity.setAuthenticationSession(authSession);
            identity.setUsername(principal);

            // map all saml response document and object
            {
                for (var document : SamlUtil.flatten(holder.getSamlDocument()).entrySet()) {
                    var key = document.getKey();
                    var value = document.getValue();
                    logger.infof("SAML Response Document - %s: %s", key, value);
                    identity.setUserAttribute(key, value);
                }

                for (var object : SamlUtil.flatten(holder.getSamlObject()).entrySet()) {
                    var key = object.getKey();
                    var value = object.getValue();
                    logger.infof("SAML Response Object - %s: %s", key, value);
                    identity.setUserAttribute(key, value);
                }
            }

            //SAML Spec 2.2.2 Format is optional
            if (subjectNameID != null && subjectNameID.getFormat() != null && subjectNameID.getFormat().toString().equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
                identity.setEmail(subjectNameID.getValue());
            }

            if (config.isStoreToken()) {
                identity.setToken(samlResponse);
            }

            var cvb = new ConditionsValidator
                    .Builder(assertion.getID(), assertion.getConditions(), destinationValidator)
                    .clockSkewInMillis(1000 * config.getAllowedClockSkew());
            try {
                var issuerURL = getEntityId(session.getContext().getUri(), realm);
                cvb.addAllowedAudience(URI.create(issuerURL));
                // getDestination has been validated to match request URL already so it matches SAML endpoint
                if (responseType.getDestination() != null) {
                    cvb.addAllowedAudience(URI.create(responseType.getDestination()));
                }
            } catch (IllegalArgumentException ex) {
                // warning has been already emitted in DeploymentBuilder
            }
            if (!cvb.build().isValid()) {
                logger.error("Assertion expired.");
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.EXPIRED_CODE);
            }

            AuthnStatementType authn = null;
            for (Object statement : assertion.getStatements()) {
                if (statement instanceof AuthnStatementType) {
                    authn = (AuthnStatementType) statement;
                    identity.getContextData().put(SAML_AUTHN_STATEMENT, authn);
                    break;
                }
            }
            if (assertion.getAttributeStatements() != null) {
                var email = getX500Attribute(assertion, X500SAMLProfileConstants.EMAIL);
                if (email != null)
                    identity.setEmail(email);
            }

            var brokerUserId = config.getAlias() + "." + principal;
            identity.setBrokerUserId(brokerUserId);
            identity.setIdp(provider);
            if (authn != null && authn.getSessionIndex() != null) {
                identity.setBrokerSessionId(config.getAlias() + "." + authn.getSessionIndex());
            }

            return callback.authenticated(identity);
        } catch (WebApplicationException e) {
            return e.getResponse();
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not process response from SAML identity provider.", e);
        }
    }

    @Override
    protected Response logoutRequest(LogoutRequestType request, String relayState) {
        if (request.getNameID() == null
                && request.getBaseID() == null
                && request.getEncryptedID() == null) {
            logger.error("SAML IdP Logout request must contain at least one of BaseID, NameID and EncryptedID");
            event.error(Errors.INVALID_SAML_LOGOUT_REQUEST);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_LOGOUT_FAILURE);
        }

        if (request.getSessionIndex() == null
                || request.getSessionIndex().isEmpty()) {
            if (request.getNameID() == null) {
                //TODO this need to be implemented
                logger.error("SAML IdP Logout request contains BaseID or EncryptedID without Session Index");
                event.error(Errors.INVALID_SAML_LOGOUT_REQUEST);
                return ErrorPage.error(session, null, Response.Status.NOT_IMPLEMENTED, Messages.IDENTITY_PROVIDER_LOGOUT_FAILURE);
            }

            var brokerUserId = config.getAlias() + "." + request.getNameID().getValue();
            var ref = new AtomicReference<>(request);
            session.sessions().getUserSessionByBrokerUserIdStream(realm, brokerUserId)
                    .filter(userSession -> userSession.getState() != UserSessionModel.State.LOGGING_OUT &&
                            userSession.getState() != UserSessionModel.State.LOGGED_OUT)
                    .toList() // collect to avoid concurrent modification as backchannelLogout removes the user sessions.
                    .forEach(processLogout(ref));
            request = ref.get();
        } else {
            for (var sessionIndex : request.getSessionIndex()) {
                var brokerSessionId = config.getAlias() + "." + sessionIndex;
                var userSession = session.sessions().getUserSessionByBrokerSessionId(realm, brokerSessionId);
                if (userSession != null) {
                    if (userSession.getState() == UserSessionModel.State.LOGGING_OUT || userSession.getState() == UserSessionModel.State.LOGGED_OUT) {
                        continue;
                    }

                    for (var it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext(); ) {
                        request = it.next().beforeProcessingLogoutRequest(request, userSession, null);
                    }

                    try {
                        AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, false);
                    } catch (Exception e) {
                        logger.warn("failed to do backchannel logout for userSession", e);
                    }
                }
            }
        }

        var issuerURL = getEntityId(session.getContext().getUri(), realm);
        var builder = new SAML2LogoutResponseBuilder();
        builder.logoutRequestID(request.getID());
        builder.destination(config.getSingleLogoutServiceUrl());
        builder.issuer(issuerURL);
        var binding = new JaxrsSAML2BindingBuilder(session)
                .relayState(relayState);
        var postBinding = config.isPostBindingLogout();
        if (config.isWantAuthnRequestsSigned()) {
            var keys = session.keys()
                    .getActiveRsaKey(realm);
            var keyName = config.getXmlSigKeyInfoKeyNameTransformer()
                    .getKeyName(keys.getKid(), keys.getCertificate());
            binding.signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate())
                    .signatureAlgorithm(provider.getSignatureAlgorithm())
                    .signDocument();
            if (!postBinding && config.isAddExtensionsElementWithKeyInfo()) {    // Only include extension if REDIRECT binding and signing whole SAML protocol message
                builder.addExtension(new KeycloakKeySamlExtensionGenerator(keyName));
            }
        }
        try {
            if (postBinding) {
                return binding.postBinding(builder.buildDocument()).response(config.getSingleLogoutServiceUrl());
            } else {
                return binding.redirectBinding(builder.buildDocument()).response(config.getSingleLogoutServiceUrl());
            }
        } catch (ConfigurationException | ProcessingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Consumer<UserSessionModel> processLogout(AtomicReference<LogoutRequestType> ref) {
        return userSession -> {
            for (var it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext(); ) {
                ref.set(it.next().beforeProcessingLogoutRequest(ref.get(), userSession, null));
            }
            try {
                AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, false);
            } catch (Exception e) {
                logger.warn("failed to do backchannel logout for userSession", e);
            }
        };
    }

    @Override
    protected Response handleLogoutResponse(SAMLDocumentHolder holder, StatusResponseType responseType, String relayState) {
        if (relayState == null) {
            logger.error("no valid user session");
            event.event(EventType.LOGOUT);
            event.error(Errors.USER_SESSION_NOT_FOUND);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
        }

        var userSession = session.sessions().getUserSession(realm, relayState);
        if (userSession == null) {
            logger.error("no valid user session");
            event.event(EventType.LOGOUT);
            event.error(Errors.USER_SESSION_NOT_FOUND);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
        }

        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            logger.error("usersession in different state");
            event.event(EventType.LOGOUT);
            event.error(Errors.USER_SESSION_NOT_FOUND);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.SESSION_NOT_ACTIVE);
        }

        return AuthenticationManager.finishBrowserLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers);
    }
}
