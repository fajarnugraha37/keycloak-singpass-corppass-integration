package com.example.identity.endpoints;

import com.example.config.CustomSAMLIdentityProviderConfig;
import com.example.identity.CustomSAMLProvider;
import jakarta.ws.rs.core.HttpHeaders;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.validators.DestinationValidator;

import javax.xml.crypto.dsig.XMLSignature;

public class CustomSAMLPostBinding extends CustomSAMLBinding {
    public CustomSAMLPostBinding(RealmModel realm,
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
        var nl = documentHolder.getSamlDocument().getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        return (nl != null && nl.getLength() > 0);
    }

    @Override
    protected void verifySignature(String key, SAMLDocumentHolder documentHolder) throws VerificationException {
        if ((!containsUnencryptedSignature(documentHolder))
                && (documentHolder.getSamlObject() instanceof ResponseType responseType)) {
            var assertions = responseType.getAssertions();
            if (!assertions.isEmpty()) {
                // Only relax verification if the response is an authnresponse and contains (encrypted/plaintext) assertion.
                // In that case, signature is validated on assertion element
                return;
            }
        }

        SamlProtocolUtils.verifyDocumentSignature(documentHolder.getSamlDocument(), getIDPKeyLocator());
    }

    @Override
    protected SAMLDocumentHolder extractRequestDocument(String samlRequest) {
        return SAMLRequestParser.parseRequestPostBinding(samlRequest);
    }

    @Override
    protected SAMLDocumentHolder extractResponseDocument(String response) {
        var samlBytes = PostBindingUtil.base64Decode(response);

        return SAMLRequestParser.parseResponseDocument(samlBytes);
    }

    @Override
    protected String getBindingType() {
        return SamlProtocol.SAML_POST_BINDING;
    }
}
