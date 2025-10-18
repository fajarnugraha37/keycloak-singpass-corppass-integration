package com.example.key;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.AbstractEcdsaKeyProvider;
import org.keycloak.models.RealmModel;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class ECDSAKeyProvider extends AbstractEcdsaKeyProvider {
    private static final Logger LOGGER = Logger.getLogger(ECDSAKeyProvider.class);

    private static final String ECDSA_PRIVATE_KEY_KEY = "ecdsaPrivateKey";
    private static final String ECDSA_PUBLIC_KEY_KEY = "ecdsaPublicKey";
    private static final String ECDSA_ELLIPTIC_CURVE_KEY = "ecdsaEllipticCurveKey";

    public ECDSAKeyProvider() {
        super(null, null);
    }

    public ECDSAKeyProvider(RealmModel realm, ComponentModel model) {
        super(realm, model);
    }

    @Override
    protected KeyWrapper loadKey(RealmModel realmModel, ComponentModel model) {
        var privateEcdsaKeyBase64Encoded = model.getConfig().getFirst(ECDSA_PRIVATE_KEY_KEY);
        var publicEcdsaKeyBase64Encoded = model.getConfig().getFirst(ECDSA_PUBLIC_KEY_KEY);
        var ecInNistRep = model.getConfig().getFirst(ECDSA_ELLIPTIC_CURVE_KEY);

        try {
            var privateKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateEcdsaKeyBase64Encoded));
            var kf = KeyFactory.getInstance("EC");
            var decodedPrivateKey = kf.generatePrivate(privateKeySpec);

            var publicKeySpec = new X509EncodedKeySpec(Base64.decode(publicEcdsaKeyBase64Encoded));
            var decodedPublicKey = kf.generatePublic(publicKeySpec);

            var keyPair = new KeyPair(decodedPublicKey, decodedPrivateKey);
            var keyWrapper = createKeyWrapper(keyPair, ecInNistRep);

            var keyUse = KeyUse
                    .valueOf(model.get(ECDSAKeyProviderFactory.ECDSA_USE_KEY, KeyUse.ENC.name()).toUpperCase());

            keyWrapper.setKid(model.get(ECDSAKeyProviderFactory.ECDSA_KEY_ID_KEY));
            keyWrapper.setUse(keyUse);
            keyWrapper.setAlgorithm(model.get(ECDSAKeyProviderFactory.ECDSA_ALGORITHM_KEY));

            return keyWrapper;
        } catch (Exception e) {
            LOGGER.errorf("Exception at decodeEcdsaPublicKey %s", e.toString(), e);
            return null;
        }
    }

}