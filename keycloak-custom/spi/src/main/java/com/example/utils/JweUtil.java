package com.example.utils;

import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.JOSEParser;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.loader.PublicKeyStorageManager;
import org.keycloak.models.KeycloakSession;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;

import static org.keycloak.keys.loader.PublicKeyStorageManager.getIdentityProviderKeyWrapper;

public class JweUtil {
    private static final Logger logger = Logger.getLogger(JweUtil.class);

    private JweUtil() {
        // Private constructor to prevent instantiation
    }

    public static boolean verify(KeycloakSession session,
                                 OIDCIdentityProviderConfig configuration,
                                 JWSInput jwsInput) {
        var hdr = jwsInput.getHeader();
        logger.infof("[verify] Verifying JWS with Algorithm: %s, Key ID: %s", hdr.getAlgorithm().name(), hdr.getKeyId());
        if (!configuration.isValidateSignature()) {
            logger.warnf("Skipping signature validation as per configuration.");
            return true;
        }
        if (!configuration.isUseJwksUrl()) {
            logger.infof("[verify] Using default signature verification in superclass.");
            return verifyViaKey(session,
                    configuration,
                    getIdentityProviderKeyWrapper(session, session.getContext().getRealm(), configuration, jwsInput),
                    jwsInput);
        }

        return verifyViaJwks(session, configuration.getJwksUrl(), jwsInput);
    }

    public static boolean verifyViaKey(KeycloakSession session,
                                       OIDCIdentityProviderConfig config,
                                       KeyWrapper key,
                                       JWSInput jwsInput) {
        if (!config.isValidateSignature())
            return true;

        try {
            if (key == null) {
                logger.debugf("Failed to verify token, key not found for algorithm %s", jwsInput.getHeader().getRawAlgorithm());
                return false;
            }
            var algorithm = jwsInput.getHeader().getRawAlgorithm();
            if (key.getAlgorithm() == null) {
                key.setAlgorithm(algorithm);
            }
            var signatureProvider = session.getProvider(SignatureProvider.class, algorithm);
            if (signatureProvider == null) {
                logger.debugf("Failed to verify token, signature provider not found for algorithm %s", algorithm);
                return false;
            }

            return signatureProvider.verifier(key).verify(jwsInput.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8), jwsInput.getSignature());
        } catch (Exception e) {
            logger.debug("Failed to verify token", e);
            return false;
        }
    }

    public static boolean verifyViaJwks(KeycloakSession session,
                                        String jwksUrl,
                                        JWSInput jwsInput) {
        var hdr = jwsInput.getHeader();
        try {
            var jwksJson = SimpleHttp.doGet(jwksUrl, session).asJson();
            var jwkSet = JWKSet.parse(jwksJson.toString());
            logger.infof("[verify] Fetched JWKS from URL: %s with result %s", jwksUrl, jwksJson.toString());

            var kid = hdr.getKeyId();
            var jwk = jwkSet.getKeyByKeyId(kid);
            if (jwk == null) {
                logger.errorf("[verify] No matching key found in JWKS for Key ID: %s", kid);
                return false;
            }

            var compact = jwsInput.getWireString();
            var sjwt = SignedJWT.parse(compact);
            if (!hdr.getAlgorithm().name().equalsIgnoreCase(sjwt.getHeader().getAlgorithm().getName())) {
                logger.errorf("[verify] JWS alg header %s does not match SignedJWT alg %s",
                        hdr.getAlgorithm().name(),
                        sjwt.getHeader().getAlgorithm().getName());
                return false;
            }
            if (jwk.getKeyUse() != null && !"sig".equalsIgnoreCase(jwk.getKeyUse().identifier())) {
                logger.errorf("[verify] JWK key use is not 'sig' for Key ID: %s", kid);
                return false;
            }

            var ok = false;
            if ("EC".equals(jwk.getKeyType().getValue())) {
                var ecJwk = jwk.toECKey();
                ok = sjwt.verify(new ECDSAVerifier(ecJwk));
            } else if ("RSA".equals(jwk.getKeyType().getValue())) {
                ok = sjwt.verify(new RSASSAVerifier(jwk.toRSAKey()));
            } else {
                logger.errorf("[verify] unsupported kty=%s", jwk.getKeyType());
                return false;
            }

            if (!ok) {
                logger.error("[verify] signature verification failed");
                return false;
            } else {
                logger.info("[verify] signature successfully verified");
                return true;
            }
        } catch (Exception e) {
            logger.error("[verify] Failed to verify token", e);
            return false;
        }
    }

    public static String parse(KeycloakSession session,
                               OIDCIdentityProviderConfig configuration,
                               String encodedToken,
                               boolean shouldBeSigned) {
        if (encodedToken == null) {
            throw new IdentityBrokerException("[parse] No token from server.");
        }

        try {
            JWSInput jws;
            var joseToken = JOSEParser.parse(encodedToken);
            if (joseToken instanceof JWE) {
                var jwe = JWEObject.parse(encodedToken);
                var hdr = jwe.getHeader();
                var kid = hdr.getKeyID();
                var alg = String.valueOf(hdr.getAlgorithm()); // e.g. ECDH-ES+A256KW
                var enc = String.valueOf(hdr.getEncryptionMethod()); // e.g. A256CBC-HS512
                var cty = hdr.getContentType(); // often "JWT" for nested JWS
                logger.infof("[parse] JWE hdr: kid=%s alg=%s enc=%s cty=%s", kid, alg, enc, cty);

                KeyWrapper key;
                if (kid != null && !kid.isBlank()) {
                    logger.infof("[parse] No kid in header, looking for active decryption key");
                    key = session.keys()
                            .getKeysStream(session.getContext().getRealm())
                            .peek(kw -> logger.infof("[parse] key: kid=%s use=%s alg=%s status=%s type=%s",
                                    kw.getKid(),
                                    kw.getUse(),
                                    kw.getAlgorithm(),
                                    kw.getStatus(),
                                    kw.getPrivateKey() != null ? kw.getPrivateKey().getAlgorithm() : "null"))
                            .filter(kw -> kw.getStatus() == KeyStatus.ACTIVE)
                            .filter(kw -> kw.getUse() == KeyUse.ENC)
                            .filter(kw -> kid.equals(kw.getKid()))
                            .peek(kw -> logger.infof("[parse] Found key with kid: %s with use %s and algorithm %s", kw.getKid(), kw.getUse(), kw.getAlgorithm()))
                            .findFirst()
                            .orElse(null);
                } else {
                    logger.infof("[parse] Kid is exists, Looking for decryption key with kid: %s", kid);
                    key = session.keys()
                            .getKeysStream(session.getContext().getRealm())
                            .peek(kw -> logger.infof("[parse] key: kid=%s use=%s alg=%s status=%s type=%s",
                                    kw.getKid(),
                                    kw.getUse(),
                                    kw.getAlgorithm(),
                                    kw.getStatus(),
                                    kw.getPrivateKey() != null ? kw.getPrivateKey().getAlgorithm() : "null"))
                            .filter(kw -> kw.getStatus() == KeyStatus.ACTIVE)
                            .filter(kw -> kw.getUse() == KeyUse.ENC)
                            .filter(kw -> alg.equalsIgnoreCase(kw.getAlgorithm()))
                            .peek(kw -> logger.infof("[parse] Found key with kid: %s with use %s and algorithm %s", kw.getKid(), kw.getUse(), kw.getAlgorithm()))
                            .findFirst()
                            .orElse(null);
                }
                if (key == null || key.getPrivateKey() == null) {
                    throw new IdentityBrokerException("[parse] No ENC private key to decrypt JWE (alg=" + alg + ", kid=" + kid + ")");
                }
                if (alg.startsWith("ECDH-ES") && !(key.getPrivateKey() instanceof ECPrivateKey)) {
                    throw new IdentityBrokerException("[parse] ENC key is not EC private key required by " + alg + " (kid=" + key.getKid() + ") actually " + key.getPrivateKey().getClass().getName());
                }

                var decrypter = new ECDHDecrypter((ECPrivateKey) key.getPrivateKey());
                jwe.decrypt(decrypter);
                var content = jwe.getPayload().toString();

                try {
                    joseToken = JOSEParser.parse(content);
                } catch (Exception e) {
                    if (shouldBeSigned) {
                        throw new IdentityBrokerException("[parse] Token is not a signed JWS", e);
                    }
                    logger.infof("[parse] Decrypted content is not a JOSE token, returning raw content");
                    return content;
                }

                if (!(joseToken instanceof JWSInput)) {
                    throw new IdentityBrokerException("[parse] Invalid token type");
                }

                jws = (JWSInput) joseToken;
            } else if (joseToken instanceof JWSInput jwsInput) {
                logger.infof("[parse] Token is a JWS");
                jws = jwsInput;
            } else {
                throw new IdentityBrokerException("[parse] Invalid token type");
            }

            if (!verify(session, configuration, jws)) {
                throw new IdentityBrokerException("[parse] token signature validation failed");
            }

            return new String(jws.getContent(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IdentityBrokerException("[parse] Invalid token", e);
        }
    }
}
