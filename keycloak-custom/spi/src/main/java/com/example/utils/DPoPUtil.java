package com.example.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.crypto.KeyWrapper;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Utility class for DPoP (Demonstrating Proof-of-Possession) token generation
 * as required by FAPI 2.0 specification.
 * <p>
 * DPoP is a mechanism to bind access tokens to a specific client by using
 * proof-of-possession of a private key.
 */
public class DPoPUtil {
    private static final Logger logger = Logger.getLogger(DPoPUtil.class);
    private static final String DPOP_TYPE = "dpop+jwt";

    private DPoPUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generates a DPoP proof token for FAPI 2.0 compliance
     *
     * @param httpMethod      The HTTP method (GET, POST, etc.)
     * @param httpUri         The full HTTP URI
     * @param keyWrapper      The key wrapper containing the private key for signing
     * @param accessTokenHash Optional SHA-256 hash of the access token (for token endpoint)
     * @return The serialized DPoP JWT
     */
    public static String generateDPoPProof(String httpMethod,
                                           String httpUri,
                                           KeyWrapper keyWrapper,
                                           String accessTokenHash) {
        try {
            // Create JWK thumbprint for the public key
            var publicKey = (ECPublicKey) keyWrapper.getPublicKey();
            var privateKey = (ECPrivateKey) keyWrapper.getPrivateKey();

            // Build JWK from public key
            var jwk = new ECKey.Builder(getCurve(keyWrapper.getAlgorithm()), publicKey)
                    .keyID(keyWrapper.getKid())
                    .build();

            // Create DPoP header
            var header = new JWSHeader.Builder(JWSAlgorithm.parse(keyWrapper.getAlgorithm()))
                    .type(new com.nimbusds.jose.JOSEObjectType(DPOP_TYPE))
                    .jwk(jwk.toPublicJWK())
                    .build();

            // Create DPoP claims
            var now = Instant.now();
            var claimsBuilder = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .claim("htm", httpMethod)
                    .claim("htu", httpUri)
                    .issueTime(Date.from(now))
                    .claim("iat", now.getEpochSecond());

            // Add access token hash if provided (for use with access token)
            if (accessTokenHash != null && !accessTokenHash.isEmpty()) {
                claimsBuilder.claim("ath", accessTokenHash);
            }

            var claims = claimsBuilder.build();

            // Sign the DPoP proof
            var signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new ECDSASigner(privateKey));

            var dpopProof = signedJWT.serialize();
            logger.infof("[generateDPoPProof] Generated DPoP proof for %s %s", httpMethod, httpUri);

            return dpopProof;

        } catch (JOSEException e) {
            logger.errorf(e, "[generateDPoPProof] Failed to generate DPoP proof: %s", e.getMessage());
            throw new RuntimeException("Failed to generate DPoP proof", e);
        }
    }

    /**
     * Generates SHA-256 hash of access token for DPoP binding
     *
     * @param accessToken The access token to hash
     * @return Base64URL encoded SHA-256 hash
     */
    public static String generateAccessTokenHash(String accessToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(accessToken.getBytes());
            return Base64Url.encode(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.errorf(e, "[generateAccessTokenHash] Failed to generate access token hash: %s", e.getMessage());
            throw new RuntimeException("Failed to generate access token hash", e);
        }
    }

    /**
     * Gets the curve for the given algorithm
     */
    private static com.nimbusds.jose.jwk.Curve getCurve(String algorithm) {
        if (algorithm.contains("256")) {
            return com.nimbusds.jose.jwk.Curve.P_256;
        } else if (algorithm.contains("384")) {
            return com.nimbusds.jose.jwk.Curve.P_384;
        } else if (algorithm.contains("521")) {
            return com.nimbusds.jose.jwk.Curve.P_521;
        }
        // Default to P-256 for ES256
        return com.nimbusds.jose.jwk.Curve.P_256;
    }

    /**
     * Generates a new key pair for DPoP if not already available
     * This should be called once and the key pair stored for the session
     */
    public static String generateNonce() {
        return Base64Url.encode(SecretGenerator.getInstance().randomBytes(32));
    }

    /**
     * Generates JWK thumbprint (SHA-256) for the dpop_jkt parameter in authorization request.
     * This binds the authorization to a specific public key.
     *
     * @param publicKey The public key to generate thumbprint for
     * @return Base64URL encoded SHA-256 thumbprint of the JWK
     */
    public static String generateJWKThumbprint(Key publicKey) {
        try {
            if (!(publicKey instanceof ECPublicKey ecPublicKey)) {
                throw new IllegalArgumentException("Only EC public keys are supported for DPoP");
            }

            // Build JWK representation
            var jwk = new ECKey.Builder(getCurve("ES256"), ecPublicKey)
                    .build();

            // Generate SHA-256 thumbprint
            var thumbprint = jwk.computeThumbprint();

            logger.infof("[generateJWKThumbprint] Generated JWK thumbprint for public key");
            return thumbprint.toString();

        } catch (Exception e) {
            logger.errorf(e, "[generateJWKThumbprint] Failed to generate JWK thumbprint: %s", e.getMessage());
            throw new RuntimeException("Failed to generate JWK thumbprint", e);
        }
    }
}
