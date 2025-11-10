package com.example.identity.endpoints;


import com.example.config.CustomSAMLIdentityProviderConfig;
import com.example.identity.CustomSAMLProvider;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyUse;
import org.keycloak.dom.saml.v2.assertion.*;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.models.*;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.saml.*;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.security.Key;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;


public abstract class CustomBinding {
    protected static final Logger logger = Logger.getLogger(CustomBinding.class);

    protected final RealmModel realm;
    protected final CustomSAMLIdentityProviderConfig config;
    protected final KeycloakSession session;
    protected final ClientConnection clientConnection;
    protected final DestinationValidator destinationValidator;
    protected final HttpHeaders headers;
    protected final IdentityProvider.AuthenticationCallback callback;
    protected final CustomSAMLProvider provider;
    protected EventBuilder event;

    public CustomBinding(RealmModel realm,
                         CustomSAMLIdentityProviderConfig config,
                         KeycloakSession session,
                         ClientConnection clientConnection,
                         DestinationValidator destinationValidator,
                         HttpHeaders headers,
                         IdentityProvider.AuthenticationCallback callback,
                         CustomSAMLProvider provider,
                         EventBuilder event) {
        this.realm = realm;
        this.config = config;
        this.session = session;
        this.clientConnection = clientConnection;
        this.headers = headers;
        this.destinationValidator = destinationValidator;
        this.callback = callback;
        this.provider = provider;
        this.event = event;
    }

    protected abstract String getBindingType();

    protected abstract boolean containsUnencryptedSignature(SAMLDocumentHolder documentHolder);

    protected abstract void verifySignature(String key, SAMLDocumentHolder documentHolder) throws VerificationException;

    protected abstract SAMLDocumentHolder extractRequestDocument(String samlRequest);

    protected abstract SAMLDocumentHolder extractResponseDocument(String response);

    protected abstract Response handleSamlRequest(String samlRequest, String relayState);

    protected abstract Response handleSamlResponse(String samlResponse, String relayState, String clientId);

    protected abstract Response handleLoginResponse(String samlResponse, SAMLDocumentHolder holder, ResponseType responseType, String relayState, String clientId);

    protected abstract Response logoutRequest(LogoutRequestType request, String relayState);

    protected abstract Consumer<UserSessionModel> processLogout(AtomicReference<LogoutRequestType> ref);

    protected abstract Response handleLogoutResponse(SAMLDocumentHolder holder, StatusResponseType responseType, String relayState);

    public Response execute(String samlRequest, String samlResponse, String relayState, String clientId) {
        event = new EventBuilder(realm, session, clientConnection);
        var response = basicChecks(samlRequest, samlResponse);
        if (response != null) {
            logger.infof("Basic checks failed for SAML %s binding", getBindingType());
            return response;
        }
        if (samlRequest != null) {
            logger.infof("Handling SAML %s request", getBindingType());
            return handleSamlRequest(samlRequest, relayState);
        }

        logger.infof("Handling SAML %s response", getBindingType());
        return handleSamlResponse(samlResponse, relayState, clientId);
    }

    protected boolean checkSsl() {
        if (session.getContext().getUri().getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
    }

    protected Response basicChecks(String samlRequest, String samlResponse) {
        if (!checkSsl()) {
            event.event(EventType.LOGIN);
            event.error(Errors.SSL_REQUIRED);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.HTTPS_REQUIRED);
        }
        if (!realm.isEnabled()) {
            event.event(EventType.LOGIN_ERROR);
            event.error(Errors.REALM_DISABLED);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.REALM_NOT_ENABLED);
        }

        if (samlRequest == null && samlResponse == null) {
            event.event(EventType.LOGIN);
            event.error(Errors.INVALID_REQUEST);
            return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);

        }
        return null;
    }

    protected boolean isDestinationRequired() {
        return true;
    }

    protected boolean isSuccessfulSamlResponse(ResponseType responseType) {
        return responseType != null
                && responseType.getStatus() != null
                && responseType.getStatus().getStatusCode() != null
                && responseType.getStatus().getStatusCode().getValue() != null
                && Objects.equals(responseType.getStatus().getStatusCode().getValue().toString(), JBossSAMLURIConstants.STATUS_SUCCESS.get());
    }

    protected KeyLocator getIDPKeyLocator() {
        if (StringUtil.isNotBlank(config.getMetadataDescriptorUrl()) && config.isUseMetadataDescriptorUrl()) {
            String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(realm.getId(), config.getInternalId());
            PublicKeyLoader keyLoader = new SamlMetadataPublicKeyLoader(session, config.getMetadataDescriptorUrl());
            PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);
            return new SamlMetadataKeyLocator(modelKey, keyLoader, KeyUse.SIG, keyStorage);
        }

        List<Key> keys = new LinkedList<>();
        for (String signingCertificate : config.getSigningCertificates()) {
            X509Certificate cert = null;
            try {
                cert = XMLSignatureUtil.getX509CertificateFromKeyInfoString(signingCertificate.replaceAll("\\s", ""));
                cert.checkValidity();
                keys.add(cert.getPublicKey());
            } catch (CertificateException e) {
                logger.warnf("Ignoring invalid certificate: %s", cert);
            } catch (ProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return new HardcodedKeyLocator(keys);
    }

    protected String getX500Attribute(AssertionType assertion, X500SAMLProfileConstants attribute) {
        return getFirstMatchingAttribute(assertion, attribute::correspondsTo);
    }

    protected String getAttributeByName(AssertionType assertion, String name) {
        return getFirstMatchingAttribute(assertion, attribute -> Objects.equals(attribute.getName(), name));
    }

    protected String getAttributeByFriendlyName(AssertionType assertion, String friendlyName) {
        return getFirstMatchingAttribute(assertion, attribute -> Objects.equals(attribute.getFriendlyName(), friendlyName));
    }

    protected String getFirstMatchingAttribute(AssertionType assertion, Predicate<AttributeType> predicate) {
        return assertion.getAttributeStatements().stream()
                .map(AttributeStatementType::getAttributes)
                .flatMap(Collection::stream)
                .map(AttributeStatementType.ASTChoiceType::getAttribute)
                .filter(predicate)
                .map(AttributeType::getAttributeValue)
                .flatMap(Collection::stream)
                .findFirst()
                .map(Object::toString)
                .orElse(null);
    }

    protected String expectedPrincipalType() {
        var principalType = config.getPrincipalType();
        return switch (principalType) {
            case SUBJECT -> principalType.name();
            case ATTRIBUTE, FRIENDLY_ATTRIBUTE ->
                    String.format("%s(%s)", principalType.name(), config.getPrincipalAttribute());
        };
    }

    protected String getPrincipal(AssertionType assertion) {
        var principalType = config.getPrincipalType();
        if (principalType == null || principalType.equals(SamlPrincipalType.SUBJECT)) {
            var subjectNameID = getSubjectNameID(assertion);
            return subjectNameID != null ? subjectNameID.getValue() : null;
        } else if (principalType.equals(SamlPrincipalType.ATTRIBUTE)) {
            return getAttributeByName(assertion, config.getPrincipalAttribute());
        } else {
            return getAttributeByFriendlyName(assertion, config.getPrincipalAttribute());
        }

    }

    protected NameIDType getSubjectNameID(final AssertionType assertion) {
        var subject = assertion.getSubject();
        SubjectType.STSubType subType = subject.getSubType();
        return subType != null ? (NameIDType) subType.getBaseID() : null;
    }

    protected String getEntityId(UriInfo uriInfo, RealmModel realm) {
        var configEntityId = config.getEntityId();
        if (configEntityId == null || configEntityId.isEmpty())
            return UriBuilder.fromUri(uriInfo.getBaseUri()).path("realms").path(realm.getName()).build().toString();
        else
            return configEntityId;
    }

    protected String getExpectedDestination(String providerAlias, String clientId) {
        if (clientId != null) {
            return session.getContext().getUri().getAbsolutePath().toString();
        }
        return Urls.identityProviderAuthnResponse(session.getContext().getUri().getBaseUri(), providerAlias, realm.getName()).toString();
    }

    protected boolean validateInResponseToAttribute(ResponseType responseType, String expectedRequestId) {
        // If we are not expecting a request ID, don't bother
        if (expectedRequestId == null || expectedRequestId.isEmpty())
            return true;

        // We are expecting a request ID so we are in SP-initiated login, attribute InResponseTo must be present
        if (responseType.getInResponseTo() == null) {
            logger.error("Response Validation Error: InResponseTo attribute was expected but not present in received response");
            return false;
        }

        // Attribute is present, proceed with validation
        // 1) Attribute Response > InResponseTo must not be empty
        var responseInResponseToValue = responseType.getInResponseTo();
        if (responseInResponseToValue.isEmpty()) {
            logger.error("Response Validation Error: InResponseTo attribute was expected but it is empty in received response");
            return false;
        }

        // 2) Attribute Response > InResponseTo must match request ID
        if (!responseInResponseToValue.equals(expectedRequestId)) {
            logger.error("Response Validation Error: received InResponseTo attribute does not match the expected request ID");
            return false;
        }

        // If present, Assertion > Subject > Confirmation > SubjectConfirmationData > InResponseTo must also be validated
        if (responseType.getAssertions().isEmpty())
            return true;

        var subjectElement = responseType.getAssertions().get(0).getAssertion().getSubject();
        if (subjectElement != null) {
            if (subjectElement.getConfirmation() != null && !subjectElement.getConfirmation().isEmpty()) {
                var subjectConfirmationElement = subjectElement.getConfirmation().get(0);

                if (subjectConfirmationElement != null) {
                    var subjectConfirmationDataElement = subjectConfirmationElement.getSubjectConfirmationData();

                    if (subjectConfirmationDataElement != null) {
                        if (subjectConfirmationDataElement.getInResponseTo() != null) {
                            // 3) Assertion > Subject > Confirmation > SubjectConfirmationData > InResponseTo is empty
                            var subjectConfirmationDataInResponseToValue = subjectConfirmationDataElement.getInResponseTo();
                            if (subjectConfirmationDataInResponseToValue.isEmpty()) {
                                logger.error("Response Validation Error: SubjectConfirmationData InResponseTo attribute was expected but it is empty in received response");
                                return false;
                            }

                            // 4) Assertion > Subject > Confirmation > SubjectConfirmationData > InResponseTo does not match request ID
                            if (!subjectConfirmationDataInResponseToValue.equals(expectedRequestId)) {
                                logger.error("Response Validation Error: received SubjectConfirmationData InResponseTo attribute does not match the expected request ID");
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * If there is a client whose SAML IDP-initiated SSO URL name is set to the
     * given {@code clientUrlName}, creates a fresh authentication session for that
     * client and returns a {@link AuthenticationSessionModel} object with that session.
     * Otherwise returns "client not found" response.
     *
     * @return see description
     */
    protected AuthenticationSessionModel samlIdpInitiatedSSO(final String clientUrlName) {
        event.event(EventType.LOGIN);
        CacheControlUtil.noBackButtonCacheControlHeader(session);
        var oClient = this.session.clients()
                .searchClientsByAttributes(realm, Collections.singletonMap(SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME, clientUrlName), 0, 1)
                .findFirst();

        if (oClient.isEmpty()) {
            event.error(Errors.CLIENT_NOT_FOUND);
            var response = ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.CLIENT_NOT_FOUND);
            throw new WebApplicationException(response);
        }

        var factory = (LoginProtocolFactory) session.getKeycloakSessionFactory().getProviderFactory(LoginProtocol.class, SamlProtocol.LOGIN_PROTOCOL);
        var samlService = (SamlService) factory.createProtocolEndpoint(this.session, event);
        var authSession = samlService.getOrCreateLoginSessionForIdpInitiatedSso(session, this.realm, oClient.get(), null);
        if (authSession == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            var response = ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REDIRECT_URI);
            throw new WebApplicationException(response);
        }

        return authSession;
    }
}