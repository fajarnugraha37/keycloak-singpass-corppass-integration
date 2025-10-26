package com.example.identity.endpoints;

import com.example.config.CustomSAMLIdentityProviderConfig;
import com.example.identity.CustomSAMLProvider;
import com.example.utils.SamlUtil;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.dom.saml.v2.assertion.*;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;


public class CustomSAMLEndpoint extends SAMLEndpoint {
    private static final Logger logger = Logger.getLogger(CustomSAMLEndpoint.class);

    protected final KeycloakSession session;
    protected final CustomSAMLProvider provider;
    protected final CustomSAMLIdentityProviderConfig config;
    protected final DestinationValidator destinationValidator;
    protected final ClientConnection clientConnection;
    protected final HttpHeaders headers;


    public CustomSAMLEndpoint(KeycloakSession session,
                              CustomSAMLProvider provider,
                              CustomSAMLIdentityProviderConfig config,
                              DestinationValidator destinationValidator,
                              IdentityProvider.AuthenticationCallback callback) {
        super(session, provider, config, callback, destinationValidator);
        this.session = session;
        this.provider = provider;
        this.config = config;
        this.clientConnection = session.getContext().getConnection();
        this.destinationValidator = destinationValidator;
        this.headers = session.getContext().getRequestHeaders();
    }

    @Override
    public Response getSPDescriptor() {
        logger.infof("Generating SP Descriptor for CustomSAMLProvider");
        var response = super.getSPDescriptor();
        logger.infof("SP Descriptor Response with status status %s, location %s\n entity %s",
                response.getStatus(),
                response.getLocation(),
                response.getEntity());
        return response;
    }

    @Override
    public Response redirectBinding(String samlRequest,
                                    String samlResponse,
                                    String relayState) {
        return this.redirectBinding(samlRequest, samlResponse, relayState, null);
    }

    @Override
    public Response redirectBinding(String samlRequest,
                                    String samlResponse,
                                    String relayState,
                                    String clientId) {
        logger.infof("Handling SAML Redirect Binding for clientId: %s", clientId);
        logger.infof("SAML Request: %s", samlRequest);
        logger.infof("SAML Response: %s", samlResponse);
        logger.infof("Relay State: %s", relayState);
        var binding = new CustomSAMLRedirectBinding(realm, config, session, clientConnection, destinationValidator, headers, callback, provider, event);
        var response = binding.execute(samlRequest, samlResponse, relayState, clientId);
        logger.infof("Redirect Binding Response with status status %s, location %s\n entity %s",
                response.getStatus(),
                response.getLocation(),
                response.getEntity());
        return response;
    }

    @Override
    public Response postBinding(String samlRequest,
                                String samlResponse,
                                String relayState) {
        return this.postBinding(samlRequest, samlResponse, relayState, null);
    }

    @Override
    public Response postBinding(String samlRequest,
                                String samlResponse,
                                String relayState,
                                String clientId) {
        logger.infof("Handling SAML Post Binding for clientId: %s", clientId);
        logger.infof("SAML Request: %s", samlRequest);
        logger.infof("SAML Response: %s", samlResponse);
        logger.infof("Relay State: %s", relayState);
        var binding = new CustomSAMLPostBinding(realm, config, session, clientConnection, destinationValidator, headers, callback, provider, event);
        var response = binding.execute(samlRequest, samlResponse, relayState, clientId);
        logger.infof("Post Binding Response with status status %s, location %s\n entity %s",
                response.getStatus(),
                response.getLocation(),
                response.getEntity());
        return response;
    }

    @Deprecated
    public class SAMLPostBinding extends PostBinding {
        private SAMLPostBinding() {
            super();
        }

        @Override
        public Response execute(String samlRequest,
                                String samlResponse,
                                String relayState,
                                String clientId) {
            logger.infof("Executing SAML Post Binding for clientId: %s with relyState", clientId, relayState);
            event = new EventBuilder(realm, session, clientConnection);
            var response = basicChecks(samlRequest, samlResponse);
            if (response != null) {
                logger.infof("Basic checks failed. Returning response with status %s", response.getStatus());
                return response;
            }
            if (samlRequest != null) {
                logger.infof("Handling SAML Request in Post Binding");
                return handleSamlRequest(samlRequest, relayState);
            }

            logger.info("Handling SAML Response in Post Binding");
            return handleSamlResponse(samlResponse, relayState, clientId);
        }

        @Override
        public Response handleSamlResponse(String samlResponse,
                                           String relayState,
                                           String clientId) {
            var holder = extractResponseDocument(samlResponse);
            if (holder == null) {
                logger.infof("Invalid SAML Document: %s", samlResponse);
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, Errors.INVALID_SAML_DOCUMENT);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_INVALID_RESPONSE);
            }

            var statusResponse = (StatusResponseType) holder.getSamlObject();
            if (isDestinationRequired()
                    && statusResponse.getDestination() == null
                    && containsUnencryptedSignature(holder)) {
                logger.infof("Missing required destination in SAML Response: %\n %s", samlResponse, holder.toString());
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, Errors.MISSING_REQUIRED_DESTINATION);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            var expectedDestination = getExpectedDestination(config.getAlias(), clientId);
            var responseDestination = statusResponse.getDestination();
            logger.infof("Expected destination: %s and response %s", expectedDestination, responseDestination);
            if (!destinationValidator.validate(expectedDestination, responseDestination)) {
                logger.infof("Invalid destination in SAML Response: %\n %s", samlResponse);
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.detail(Details.REASON, Errors.INVALID_DESTINATION);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REQUEST);
            }

            if (config.isValidateSignature()) {
                try {
                    logger.infof("Verifying signature in SAML Response: %\n %s", samlResponse);
                    verifySignature(GeneralConstants.SAML_RESPONSE_KEY, holder);
                } catch (VerificationException e) {
                    logger.error("validation failed", e);
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                    event.error(Errors.INVALID_SIGNATURE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_INVALID_SIGNATURE);
                }
            }

            if (statusResponse instanceof ResponseType responseType) {
                logger.infof("Handling Login Response in Post Binding");
                try {
                    return handleLoginResponse(samlResponse, holder, responseType, relayState, clientId);
                } finally {
                    var principal = getPrincipal(responseType.getAssertions().get(0).getAssertion());
                    var identity = new BrokeredIdentityContext(principal, config);

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

                    if (responseType.getAssertions() != null) {
                        logger.infof("Number of Assertions in Response: %d", responseType.getAssertions().size());
                        for (var assertion : responseType.getAssertions()) {
                            var as = assertion.getAssertion();

                            logger.infof("SAML Assertion: %s", assertion.getID());
                            logger.infof("Issuer: %s", as.getIssuer());
                            logger.infof("Advice: %s", as.getAdvice());
                            for (var confirmation : as.getSubject().getConfirmation()) {
                                logger.infof("Subject Base ID: %s", confirmation.getBaseID());
                                logger.infof("Subject Name ID: %s", confirmation.getNameID());
                                logger.infof("Subject Confirmation Method: %s", confirmation.getMethod());
                                logger.infof("Subject Confirmation Data: %s", confirmation.getSubjectConfirmationData());
                            }
                            for (var attributeStatement : assertion.getAssertion().getAttributeStatements()) {
                                for (var attribute : attributeStatement.getAttributes()) {
                                    var att = attribute.getAttribute();
                                    logger.infof("Attribute: Name=%s, Values=%s", att.getName(), att.getAttributeValue());
                                }
                            }
                            for (var statement : assertion.getAssertion().getStatements()) {
                                logger.infof("Statement: %s", statement);
                            }
                            for (var condition : assertion.getAssertion().getConditions().getConditions()) {
                                logger.infof("Condition: %s", condition);
                            }
                        }
                    }
                }
            }

            logger.infof("Handling Logout Response in Post Binding");
            return handleLogoutResponse(holder, statusResponse, relayState);
        }

        String getPrincipal(AssertionType assertion) {
            var principalType = config.getPrincipalType();
            if (principalType == null || principalType.equals(SamlPrincipalType.SUBJECT)) {
                NameIDType subjectNameID = getSubjectNameID(assertion);
                return subjectNameID != null ? subjectNameID.getValue() : null;
            } else if (principalType.equals(SamlPrincipalType.ATTRIBUTE)) {
                return getAttributeByName(assertion, config.getPrincipalAttribute());
            } else {
                return getAttributeByFriendlyName(assertion, config.getPrincipalAttribute());
            }
        }

        String getExpectedDestination(String providerAlias, String clientId) {
            if (clientId != null) {
                return session.getContext().getUri().getAbsolutePath().toString();
            }
            return Urls.identityProviderAuthnResponse(session.getContext().getUri().getBaseUri(), providerAlias, realm.getName()).toString();
        }

        NameIDType getSubjectNameID(final AssertionType assertion) {
            var subject = assertion.getSubject();
            SubjectType.STSubType subType = subject.getSubType();
            return subType != null
                    ? (NameIDType) subType.getBaseID()
                    : null;
        }

        String getAttributeByName(AssertionType assertion, String name) {
            return getFirstMatchingAttribute(assertion, attribute -> Objects.equals(attribute.getName(), name));
        }

        String getAttributeByFriendlyName(AssertionType assertion, String friendlyName) {
            return getFirstMatchingAttribute(assertion, attribute -> Objects.equals(attribute.getFriendlyName(), friendlyName));
        }

        String getFirstMatchingAttribute(AssertionType assertion, Predicate<AttributeType> predicate) {
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
    }

    @Deprecated
    public class SAMLRedirectBinding extends RedirectBinding {
        private SAMLRedirectBinding() {
            super();
        }

        @Override
        public Response execute(String samlRequest,
                                String samlResponse,
                                String relayState,
                                String clientId) {
            logger.infof("Executing SAML Redirect Binding for clientId: %s with relyState %s", clientId, relayState);
            event = new EventBuilder(realm, session, clientConnection);
            var response = basicChecks(samlRequest, samlResponse);
            if (response != null) {
                logger.infof("Basic checks failed. Returning response with status %s", response.getStatus());
                return response;
            }
            if (samlRequest != null) {
                logger.infof("Handling SAML Request in Redirect Binding");
                return handleSamlRequest(samlRequest, relayState);
            }

            logger.infof("Handling SAML Response in Redirect Binding");
            return handleSamlResponse(samlResponse, relayState, clientId);
        }
    }
}