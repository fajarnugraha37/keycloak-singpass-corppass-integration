package com.example.key;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.keys.AbstractEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;

import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;

public class ECDSAKeyProviderFactory extends AbstractEcdsaKeyProviderFactory {
    private static final Logger LOGGER = Logger.getLogger(ECDSAKeyProviderFactory.class);

    public static final String ID = "ecdsa";
    public static final String ECDSA_USE_KEY = "ecdsaKeyType";
    public static final String ECDSA_KEY_ID_KEY = "ecdsaKeyId";
    public static final String ECDSA_ALGORITHM_KEY = "ecdsaAlgorithm";

    protected static ProviderConfigProperty ECDSA_ELLIPTIC_CURVE_PROPERTY = new ProviderConfigProperty(
            "ecdsaEllipticCurveKey",
            "Elliptic Curve",
            "Elliptic Curve used in ECDSA",
            "List",
            "P-256",
            "P-256",
            "P-384",
            "P-521");
    protected static ProviderConfigProperty ECDSA_USE_PROPERTY = new ProviderConfigProperty(
            ECDSA_USE_KEY,
            "Use",
            "use",
            LIST_TYPE,
            "sig",
            "sig",
            "enc");
    protected static ProviderConfigProperty ECDSA_KEY_ID_PROPERTY = new ProviderConfigProperty(
            ECDSA_KEY_ID_KEY,
            "Key ID",
            "kid",
            "String",
            "key-id-123");
    protected static ProviderConfigProperty ECDSA_ALGORITHM_PROPERTY = new ProviderConfigProperty(
            ECDSA_ALGORITHM_KEY,
            "Algorithm",
            "alg",
            LIST_TYPE,
            "ECDH-ES+A128KW",
            "ECDH-ES+A128KW",
            "ECDH-ES+A192KW",
            "ECDH-ES+A256KW");

    private static final String HELP_TEXT = "Create ECDSA key";
    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = AbstractEcdsaKeyProviderFactory
            .configurationBuilder()
            .property(ECDSA_ELLIPTIC_CURVE_PROPERTY)
            .property(ECDSA_USE_PROPERTY)
            .property(ECDSA_KEY_ID_PROPERTY)
            .property(ECDSA_ALGORITHM_PROPERTY)
            .build();

    private static final Map<String, Curve> CURVE_MAP = Map.ofEntries(
            Map.entry("P-256", Curve.P_256),
            Map.entry("P-384", Curve.P_384),
            Map.entry("P-521", Curve.P_521)
    );
    private static final Map<String, Algorithm> ALGORITHM_MAP = Map.ofEntries(
        Map.entry("ECDH-ES+A128KW", JWEAlgorithm.ECDH_ES_A128KW),
        Map.entry("ECDH-ES+A192KW", JWEAlgorithm.ECDH_ES_A192KW),
        Map.entry("ECDH-ES+A256KW", JWEAlgorithm.ECDH_ES_A256KW)
    );
    private static final Map<String, KeyUse> KEY_USE_MAP = Map.ofEntries(
        Map.entry("sig", KeyUse.SIGNATURE),
        Map.entry("enc", KeyUse.ENCRYPTION)
    );

    public ECDSAKeyProviderFactory() {
        super();
    }

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        return new ECDSAKeyProvider(session.getContext().getRealm(), model);
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model)
            throws ComponentValidationException {
        super.validateConfiguration(session, realm, model);

        ConfigurationValidationHelper.check(model).checkList(ECDSA_ELLIPTIC_CURVE_PROPERTY, true);
        ConfigurationValidationHelper.check(model).checkList(ECDSA_ALGORITHM_PROPERTY, true);
        ConfigurationValidationHelper.check(model).checkList(ECDSA_USE_PROPERTY, true);

        var ecInNistRep = model.get(ECDSA_ELLIPTIC_CURVE_KEY);

        generateKeys(model, ecInNistRep);
    }

    void generateKeys(ComponentModel model, String ecInNistRep) {
        try {
            var ecKeyPair = new ECKeyGenerator(CURVE_MAP.get(model.get(ECDSA_ELLIPTIC_CURVE_KEY)))
                    .keyID(model.get(ECDSA_KEY_ID_KEY))
                    .algorithm(ALGORITHM_MAP.get(model.get(ECDSA_ALGORITHM_KEY)))
                    .keyUse(KEY_USE_MAP.get(model.get(ECDSA_USE_KEY)))
                    .generate();

            LOGGER.infof("KID: %s", model.get(ECDSA_KEY_ID_KEY));
            LOGGER.infof("Public JWK: %s", ecKeyPair.toPublicJWK());
            LOGGER.infof("Private JWK: %s", ecKeyPair.toPrivateKey());

            var keyPair = ecKeyPair.toKeyPair();
            model.put(ECDSA_PRIVATE_KEY_KEY, Base64.encodeBytes(keyPair.getPrivate().getEncoded()));
            model.put(ECDSA_PUBLIC_KEY_KEY, Base64.encodeBytes(keyPair.getPublic().getEncoded()));
            model.put(ECDSA_ELLIPTIC_CURVE_KEY, ecInNistRep);
        } catch (Exception e) {
            LOGGER.error("Failed to generate ECDSA keys %s", e.getMessage(), e);
            throw new ComponentValidationException("Failed to generate ECDSA keys", e);
        }
    }
}