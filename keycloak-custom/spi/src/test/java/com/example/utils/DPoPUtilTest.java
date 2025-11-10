package com.example.utils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.KeyWrapper;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DPoPUtil - FAPI 2.0 DPoP proof generation
 */
class DPoPUtilTest {

    private KeyWrapper keyWrapper;
    private ECPublicKey publicKey;
    private ECPrivateKey privateKey;

    @BeforeEach
    void setUp() throws Exception {
        // Generate EC key pair for testing (P-256 curve for ES256)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyGen.generateKeyPair();

        publicKey = (ECPublicKey) keyPair.getPublic();
        privateKey = (ECPrivateKey) keyPair.getPrivate();

        // Create KeyWrapper mock
        keyWrapper = new KeyWrapper();
        keyWrapper.setKid("test-key-id");
        keyWrapper.setAlgorithm("ES256");
        keyWrapper.setPublicKey(publicKey);
        keyWrapper.setPrivateKey(privateKey);
        keyWrapper.setType("EC");
        keyWrapper.setUse("sig");
    }

    @Test
    void testGenerateDPoPProof_WithoutAccessTokenHash() throws Exception {
        // Given
        String httpMethod = "POST";
        String httpUri = "https://example.com/token";

        // When
        String dpopProof = DPoPUtil.generateDPoPProof(httpMethod, httpUri, keyWrapper, null);

        // Then
        assertNotNull(dpopProof, "DPoP proof should not be null");
        assertFalse(dpopProof.isEmpty(), "DPoP proof should not be empty");

        // Parse the JWT
        SignedJWT signedJWT = SignedJWT.parse(dpopProof);

        // Verify header
        assertEquals("dpop+jwt", signedJWT.getHeader().getType().getType(), "Type should be dpop+jwt");
        assertEquals(JWSAlgorithm.ES256, signedJWT.getHeader().getAlgorithm(), "Algorithm should be ES256");
        assertNotNull(signedJWT.getHeader().getJWK(), "JWK should be present in header");

        // Verify claims
        assertNotNull(signedJWT.getJWTClaimsSet().getJWTID(), "jti claim should be present");
        assertEquals(httpMethod, signedJWT.getJWTClaimsSet().getClaim("htm"), "htm claim should match HTTP method");
        assertEquals(httpUri, signedJWT.getJWTClaimsSet().getClaim("htu"), "htu claim should match HTTP URI");
        assertNotNull(signedJWT.getJWTClaimsSet().getClaim("iat"), "iat claim should be present");
        assertNull(signedJWT.getJWTClaimsSet().getClaim("ath"), "ath claim should not be present when no access token hash provided");

        // Verify signature
        JWSVerifier verifier = new ECDSAVerifier(publicKey);
        assertTrue(signedJWT.verify(verifier), "DPoP proof signature should be valid");
    }

    @Test
    void testGenerateDPoPProof_WithAccessTokenHash() throws Exception {
        // Given
        String httpMethod = "GET";
        String httpUri = "https://example.com/userinfo";
        String accessTokenHash = "test-access-token-hash";

        // When
        String dpopProof = DPoPUtil.generateDPoPProof(httpMethod, httpUri, keyWrapper, accessTokenHash);

        // Then
        assertNotNull(dpopProof, "DPoP proof should not be null");

        // Parse the JWT
        SignedJWT signedJWT = SignedJWT.parse(dpopProof);

        // Verify ath claim is present
        assertEquals(accessTokenHash, signedJWT.getJWTClaimsSet().getClaim("ath"),
                    "ath claim should match provided access token hash");

        // Verify signature
        JWSVerifier verifier = new ECDSAVerifier(publicKey);
        assertTrue(signedJWT.verify(verifier), "DPoP proof signature should be valid");
    }

    @Test
    void testGenerateDPoPProof_MultipleCallsProduceDifferentProofs() throws Exception {
        // Given
        String httpMethod = "POST";
        String httpUri = "https://example.com/token";

        // When
        String proof1 = DPoPUtil.generateDPoPProof(httpMethod, httpUri, keyWrapper, null);
        Thread.sleep(10); // Small delay to ensure different timestamps
        String proof2 = DPoPUtil.generateDPoPProof(httpMethod, httpUri, keyWrapper, null);

        // Then
        assertNotEquals(proof1, proof2, "Multiple DPoP proofs should be different (different jti and iat)");

        // Both should have different jti
        SignedJWT jwt1 = SignedJWT.parse(proof1);
        SignedJWT jwt2 = SignedJWT.parse(proof2);
        assertNotEquals(jwt1.getJWTClaimsSet().getJWTID(), jwt2.getJWTClaimsSet().getJWTID(),
                       "Each DPoP proof should have unique jti");
    }

    @Test
    void testGenerateDPoPProof_ValidatesIatClaim() throws Exception {
        // Given
        String httpMethod = "POST";
        String httpUri = "https://example.com/token";
        long beforeTime = System.currentTimeMillis() / 1000;

        // When
        String dpopProof = DPoPUtil.generateDPoPProof(httpMethod, httpUri, keyWrapper, null);
        long afterTime = System.currentTimeMillis() / 1000;

        // Then
        SignedJWT signedJWT = SignedJWT.parse(dpopProof);
        long iat = (long) signedJWT.getJWTClaimsSet().getClaim("iat");

        assertTrue(iat >= beforeTime && iat <= afterTime,
                  "iat claim should be within the time window of proof generation");
    }

    @Test
    void testGenerateAccessTokenHash() {
        // Given
        String accessToken = "test-access-token-12345";

        // When
        String hash = DPoPUtil.generateAccessTokenHash(accessToken);

        // Then
        assertNotNull(hash, "Access token hash should not be null");
        assertFalse(hash.isEmpty(), "Access token hash should not be empty");

        // Verify it's Base64URL encoded
        assertDoesNotThrow(() -> Base64Url.decode(hash), "Hash should be valid Base64URL");

        // Verify same input produces same hash
        String hash2 = DPoPUtil.generateAccessTokenHash(accessToken);
        assertEquals(hash, hash2, "Same access token should produce same hash");

        // Verify different input produces different hash
        String differentToken = "different-token";
        String differentHash = DPoPUtil.generateAccessTokenHash(differentToken);
        assertNotEquals(hash, differentHash, "Different access tokens should produce different hashes");
    }

    @Test
    void testGenerateAccessTokenHash_SHA256Length() {
        // Given
        String accessToken = "test-token";

        // When
        String hash = DPoPUtil.generateAccessTokenHash(accessToken);
        byte[] decoded = Base64Url.decode(hash);

        // Then
        assertEquals(32, decoded.length, "SHA-256 hash should be 32 bytes");
    }

    @Test
    void testGenerateJWKThumbprint() throws Exception {
        // When
        String thumbprint = DPoPUtil.generateJWKThumbprint(publicKey);

        // Then
        assertNotNull(thumbprint, "JWK thumbprint should not be null");
        assertFalse(thumbprint.isEmpty(), "JWK thumbprint should not be empty");

        // Verify same key produces same thumbprint
        String thumbprint2 = DPoPUtil.generateJWKThumbprint(publicKey);
        assertEquals(thumbprint, thumbprint2, "Same public key should produce same thumbprint");
    }

    @Test
    void testGenerateJWKThumbprint_DifferentKeysProduceDifferentThumbprints() throws Exception {
        // Given - generate another key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair2 = keyGen.generateKeyPair();
        ECPublicKey publicKey2 = (ECPublicKey) keyPair2.getPublic();

        // When
        String thumbprint1 = DPoPUtil.generateJWKThumbprint(publicKey);
        String thumbprint2 = DPoPUtil.generateJWKThumbprint(publicKey2);

        // Then
        assertNotEquals(thumbprint1, thumbprint2, "Different public keys should produce different thumbprints");
    }

    @Test
    void testGenerateNonce() {
        // When
        String nonce1 = DPoPUtil.generateNonce();
        String nonce2 = DPoPUtil.generateNonce();

        // Then
        assertNotNull(nonce1, "Nonce should not be null");
        assertNotNull(nonce2, "Nonce should not be null");
        assertFalse(nonce1.isEmpty(), "Nonce should not be empty");
        assertFalse(nonce2.isEmpty(), "Nonce should not be empty");
        assertNotEquals(nonce1, nonce2, "Each nonce should be unique");

        // Verify it's Base64URL encoded and 32 bytes
        byte[] decoded = Base64Url.decode(nonce1);
        assertEquals(32, decoded.length, "Nonce should be 32 bytes");
    }

    @Test
    void testGenerateDPoPProof_WithES384Algorithm() throws Exception {
        // Given - generate ES384 key pair (P-384 curve)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair keyPair = keyGen.generateKeyPair();

        KeyWrapper keyWrapper384 = new KeyWrapper();
        keyWrapper384.setKid("test-key-id-384");
        keyWrapper384.setAlgorithm("ES384");
        keyWrapper384.setPublicKey(keyPair.getPublic());
        keyWrapper384.setPrivateKey(keyPair.getPrivate());

        // When
        String dpopProof = DPoPUtil.generateDPoPProof("POST", "https://example.com/token", keyWrapper384, null);

        // Then
        SignedJWT signedJWT = SignedJWT.parse(dpopProof);
        assertEquals(JWSAlgorithm.ES384, signedJWT.getHeader().getAlgorithm(), "Algorithm should be ES384");

        // Verify signature with ES384
        JWSVerifier verifier = new ECDSAVerifier((ECPublicKey) keyPair.getPublic());
        assertTrue(signedJWT.verify(verifier), "DPoP proof with ES384 signature should be valid");
    }

    @Test
    void testGenerateDPoPProof_JWKInHeader() throws Exception {
        // Given
        String httpMethod = "POST";
        String httpUri = "https://example.com/token";

        // When
        String dpopProof = DPoPUtil.generateDPoPProof(httpMethod, httpUri, keyWrapper, null);
        SignedJWT signedJWT = SignedJWT.parse(dpopProof);

        // Then
        assertNotNull(signedJWT.getHeader().getJWK(), "JWK should be in header");
        assertTrue(signedJWT.getHeader().getJWK().isPublic(), "JWK in header should be public only");
        assertEquals("test-key-id", signedJWT.getHeader().getJWK().getKeyID(), "JWK should have correct key ID");
    }

    @Test
    void testGenerateDPoPProof_HandlesDifferentHTTPMethods() throws Exception {
        // Test all common HTTP methods
        String[] httpMethods = {"GET", "POST", "PUT", "DELETE", "PATCH"};
        String httpUri = "https://example.com/resource";

        for (String method : httpMethods) {
            // When
            String dpopProof = DPoPUtil.generateDPoPProof(method, httpUri, keyWrapper, null);
            SignedJWT signedJWT = SignedJWT.parse(dpopProof);

            // Then
            assertEquals(method, signedJWT.getJWTClaimsSet().getClaim("htm"),
                        "htm claim should match HTTP method: " + method);

            // Verify signature
            JWSVerifier verifier = new ECDSAVerifier(publicKey);
            assertTrue(signedJWT.verify(verifier), "DPoP proof for " + method + " should be valid");
        }
    }
}

