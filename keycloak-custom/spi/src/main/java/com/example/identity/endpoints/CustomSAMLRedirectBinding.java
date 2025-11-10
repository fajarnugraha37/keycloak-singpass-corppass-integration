package com.example.identity.endpoints;

import com.example.config.CustomSAMLIdentityProviderConfig;
import com.example.identity.CustomSAMLProvider;
import jakarta.ws.rs.core.HttpHeaders;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.validators.DestinationValidator;

public class CustomSAMLRedirectBinding extends CustomSAMLBinding {
    public CustomSAMLRedirectBinding(RealmModel realm,
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
    protected boolean containsUnencryptedSignature(SAMLDocumentHolder documentHolder) {
        var encodedParams = session.getContext().getUri().getQueryParameters(false);
        var algorithm = encodedParams.getFirst(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY);
        var signature = encodedParams.getFirst(GeneralConstants.SAML_SIGNATURE_REQUEST_KEY);
        return algorithm != null && signature != null;
    }

    @Override
    protected void verifySignature(String key, SAMLDocumentHolder documentHolder) throws VerificationException {
        var locator = getIDPKeyLocator();
        SamlProtocolUtils.verifyRedirectSignature(documentHolder, locator, session.getContext().getUri(), key);
    }

    @Override
    protected SAMLDocumentHolder extractRequestDocument(String samlRequest) {
        return SAMLRequestParser.parseRequestRedirectBinding(samlRequest);
    }

    @Override
    protected SAMLDocumentHolder extractResponseDocument(String response) {
        return SAMLRequestParser.parseResponseRedirectBinding(response);
    }

    @Override
    protected String getBindingType() {
        return SamlProtocol.SAML_REDIRECT_BINDING;
    }

}