package com.example.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static com.example.key.SingleJwkKeyProviderFactory.*;

public class KeyUtil {
    private static final Logger logger = Logger.getLogger(KeyUtil.class);
    // Map JWK curve names to Java standard curve names
    private static final Map<String, String> CURVE_NAME_MAPPING = Map.of(
            "P-256", "secp256r1",
            "P-384", "secp384r1",
            "P-521", "secp521r1",
            "secp256k1", "secp256k1"
    );

    private KeyUtil() {
        // Utility class
    }

    public static String inferUseFromAlg(String alg) {
        if (alg == null) {
            logger.warn("Algorithm is null, defaulting key use to 'sig'");
            return "sig";
        }

        var a = alg.toUpperCase();
        if (a.startsWith("ES")) {
            logger.infof("Algorithm '%s' indicates signature use", alg);
            return "sig";
        }

        if (a.startsWith("ECDH-ES") || a.contains("KW")) {
            logger.infof("Algorithm '%s' indicates encryption use", alg);
            return "enc";
        }

        logger.warnf("Unable to infer key use from algorithm '%s', defaulting to 'sig'", alg);
        return "sig";
    }

    public static String normalizeAlg(String alg, String use) {
        if (alg != null && !alg.isBlank()) {
            logger.infof("Using provided algorithm '%s'", alg);
            return alg;
        }

        if ("sig".equals(use)) {
            logger.infof("No algorithm provided, defaulting to 'ES256' for signature use");
            return "ES256";
        }

        logger.infof("No algorithm provided, defaulting to 'ECDH-ES+A256KW' for encryption use %s", use);
        return "ECDH-ES+A256KW";
    }

    public static KeyWrapper toKeyWrapper(ComponentModel model) {
        try {
            var raw = model.getConfig().getFirst(CFG_JWK);
            if (raw == null || raw.isBlank()) {
                logger.infof("No JWK found in component model with id %s", model.getId());
                return null;
            }

            logger.infof("Parsing JWK from component model with id %s \n%s", model.getId(), raw);
            var om = new ObjectMapper();
            var jwk = om.readTree(raw);
            if (!jwk.isObject()) {
                logger.infof("Invalid JWK format in component model with id %s", model.getId());
                return null;
            }

            var kty = jwk.path("kty").asText();
            if (!"EC".equals(kty)) {
                logger.infof("Unsupported key type '%s' in component model with id %s", kty, model.getId());
                return null; // keep it EC for now
            }

            var crv = Optional.ofNullable(jwk.path("crv"))
                    .map(JsonNode::asText)
                    .orElse("P-256");
            var x = jwk.path("x").asText();
            var y = jwk.path("y").asText();
            var d = jwk.path("d").asText();
            var alg = jwk.path("alg").asText();
            var kid = jwk.path("kid").asText();
            logger.infof("Importing EC key (kid=%s, crv=%s) from component model with id %s", kid, crv, model.getId());

            // decide use
            var forced = model.getConfig().getFirst(CFG_USE);
            var use = switch (forced == null ? "auto" : forced) {
                case "sig" -> "sig";
                case "enc" -> "enc";
                default -> inferUseFromAlg(alg);
            };
            logger.infof("Inferred key use '%s' for key (kid=%s) from component model with id %s", use, kid, model.getId());

            PrivateKey prv = null;
            if (d != null && !d.isBlank()) {
                logger.infof("Building EC private key (kid=%s) from JWK components", kid);
                prv = toECPrivateKey(x, y, d, crv);
                logger.infof("Successfully built EC private key (kid=%s) from JWK components", kid);
            }

            PublicKey pub = null;
            if (x != null && !x.isBlank() && y != null && !y.isBlank()) {
                logger.infof("Building EC public key (kid=%s) from JWK components", kid);
                pub = toECPublicKey(x, y, crv);
                logger.infof("Successfully built EC public key (kid=%s) from JWK components", kid);
            }

            if (pub == null && prv == null) {
                logger.infof("No valid public or private key could be built for key (kid=%s) from component model with id %s", kid, model.getId());
                return null;
            }

            var algorithm = normalizeAlg(alg, use); // ensure ES256 or ECDH-ES+A256KW defaults
            var w = new KeyWrapper();
            w.setKid(kid != null ? kid : (use + "-" + System.currentTimeMillis()));
            w.setUse(mapToKeyUse(use)); // Use the mapping function instead of valueOf
            w.setType(KeyType.EC);
            w.setAlgorithm(algorithm);
            w.setStatus(KeyStatus.ACTIVE);
            w.setProviderId(model.getId());
            logger.infof("Setting key provider ID to %s for key (kid=%s)", model.getId(), w.getKid());

            try {
                w.setProviderPriority(Integer.parseInt(model.getConfig().getFirst(CFG_PRIORITY)));
            } catch (Exception e) {
                logger.errorf(e, "Invalid priority value in component model with id %s, using default priority 100", model.getId());
                w.setProviderPriority(100);
            }

            if (pub != null) {
                logger.infof("Setting public key for key (kid=%s)", w.getKid());
                w.setPublicKey(pub);
            }
            if (prv != null) {
                logger.infof("Setting private key for key (kid=%s)", w.getKid());
                w.setPrivateKey(prv);
            }

            return w;
        } catch (Exception e) {
            logger.errorf(e, "Failed to build KeyWrapper from component model with id %s", model.getId());
            return null;
        }
    }

    /**
     * Convert lowercase key use string to Keycloak KeyUse enum
     */
    static KeyUse mapToKeyUse(String use) {
        return switch (use.toLowerCase()) {
            case "sig" -> KeyUse.SIG;
            case "enc" -> KeyUse.ENC;
            default -> {
                logger.warnf("Unknown key use '%s', defaulting to SIG", use);
                yield KeyUse.SIG;
            }
        };
    }

    /**
     * Build EC private key from JWK components
     */
    static PrivateKey toECPrivateKey(@SuppressWarnings("unused") String x,
                                     @SuppressWarnings("unused") String y,
                                     String d,
                                     String crv) throws Exception {
        var params = getECParameterSpec(crv);

        // Decode the private key value
        var dBytes = Base64.getUrlDecoder().decode(d);
        var dValue = new BigInteger(1, dBytes);

        var privateKeySpec = new ECPrivateKeySpec(dValue, params);
        var keyFactory = KeyFactory.getInstance("EC");

        return keyFactory.generatePrivate(privateKeySpec);
    }

    /**
     * Build EC public key from JWK components
     */
    static PublicKey toECPublicKey(String x,
                                   String y,
                                   String crv) throws Exception {
        var params = getECParameterSpec(crv);

        // Decode the coordinates
        var xBytes = Base64.getUrlDecoder().decode(x);
        var yBytes = Base64.getUrlDecoder().decode(y);
        var xCoord = new BigInteger(1, xBytes);
        var yCoord = new BigInteger(1, yBytes);

        var point = new ECPoint(xCoord, yCoord);
        var publicKeySpec = new ECPublicKeySpec(point, params);
        var keyFactory = KeyFactory.getInstance("EC");

        return keyFactory.generatePublic(publicKeySpec);
    }

    /**
     * Get EC parameter spec for the given curve
     */
    static ECParameterSpec getECParameterSpec(String curveName) throws NoSuchAlgorithmException, InvalidParameterSpecException {
        // Map JWK curve names to Java standard names
        var javaStandardName = CURVE_NAME_MAPPING.getOrDefault(curveName, curveName);
        logger.infof("Mapping curve name '%s' to Java standard name '%s'", curveName, javaStandardName);

        try {
            var params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec(javaStandardName));
            logger.infof("Successfully initialized curve with standard name '%s'", javaStandardName);

            return params.getParameterSpec(ECParameterSpec.class);
        } catch (InvalidParameterSpecException e) {
            logger.errorf("Failed to initialize curve with standard name '%s', trying alternate names", javaStandardName);

            // Try alternative naming conventions
            var alternativeNames = getAlternativeCurveNames(curveName);
            for (var altName : alternativeNames) {
                try {
                    logger.infof("Trying alternative curve name: '%s'", altName);
                    var params = AlgorithmParameters.getInstance("EC");
                    params.init(new ECGenParameterSpec(altName));
                    return params.getParameterSpec(ECParameterSpec.class);
                } catch (InvalidParameterSpecException ex) {
                    logger.errorf(ex, "Alternative curve name '%s' also failed", altName);
                    // Continue to next alternative
                }
            }

            // If all alternatives fail, throw the original exception with better error message
            throw new InvalidParameterSpecException("Unsupported curve: " + curveName +
                    ". Tried alternatives: " + String.join(", ", alternativeNames) +
                    ". Original error: " + e.getMessage());
        }
    }

    /**
     * Get alternative curve names to try if the primary mapping fails
     */
    static String[] getAlternativeCurveNames(String curveName) {
        return switch (curveName.toLowerCase()) {
            case "p-256" -> new String[]{"prime256v1", "1.2.840.10045.3.1.7"};
            case "p-384" -> new String[]{"prime384v1", "1.3.132.0.34"};
            case "p-521" -> new String[]{"prime521v1", "1.3.132.0.35"};
            case "secp256k1" -> new String[]{"1.3.132.0.10"};
            default -> new String[]{curveName};
        };
    }
}
