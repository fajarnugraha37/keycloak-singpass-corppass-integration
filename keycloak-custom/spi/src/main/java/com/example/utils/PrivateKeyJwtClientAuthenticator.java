package com.example.utils;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class PrivateKeyJwtClientAuthenticator {
    private static final Logger logger = Logger.getLogger(PrivateKeyJwtClientAuthenticator.class);

    private final JWK privateSigJwk;

    public PrivateKeyJwtClientAuthenticator(String privateSigJwkJson) {
        this.privateSigJwk = JwkUtils.parsePrivateJwk(privateSigJwkJson);
    }

    public void apply(SimpleHttp tokenReq, String tokenEndpoint, String clientId) throws Exception {
        if (privateSigJwk == null) {
            throw new IllegalStateException("private signing JWK is required for private_key_jwt");
        }
        var signer = new ECDSASigner(privateSigJwk.toECKey().toECPrivateKey());
        var header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(privateSigJwk.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();
        var now = Instant.now().getEpochSecond();
        var claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(tokenEndpoint)
                .expirationTime(Date.from(Instant.ofEpochSecond(now + 300)))
                .issueTime(Date.from(Instant.ofEpochSecond(now)))
                .jwtID(UUID.randomUUID().toString())
                .build();
        var jwt = new SignedJWT(header, claims);
        jwt.sign(signer);
        var assertion = jwt.serialize();
        logger.infof("generated client assertion JWT with kid=%s", privateSigJwk.getKeyID());
        logger.infof("client assertion JWT claims: %s", claims.toString());
        logger.infof("client assertion JWT: %s", assertion);
        logger.infof("client id: %s", clientId);

        tokenReq.param("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        tokenReq.param("client_assertion", assertion);
        tokenReq.param("client_id", clientId);
    }
}
