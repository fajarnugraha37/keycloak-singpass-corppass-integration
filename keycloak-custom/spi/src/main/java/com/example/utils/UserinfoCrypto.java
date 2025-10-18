package com.example.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;

import java.net.URL;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Map;

public class UserinfoCrypto {

    private final JWK encJwk;           // your private key for decrypting JWE
    private final URL jwksUri;          // OP JWKS for verifying JWS
    private final RemoteJWKSet<SecurityContext> jwkSource; // cached by Nimbus

    public UserinfoCrypto(JWK encJwk, URL jwksUri) {
        this.encJwk = encJwk;
        this.jwksUri = jwksUri;
        this.jwkSource = jwksUri != null ? new RemoteJWKSet<>(jwksUri) : null;
    }

    public static boolean looksLikeCompactJWT(String body) {
        if (body == null)
            return false;
        var dots = (int) body.chars().filter(c -> c == '.').count();
        return dots == 2 || dots == 4; // JWS=2, JWE=4
    }

    public Map<String, Object> parseClaims(String body) throws Exception {
        if (!looksLikeCompactJWT(body)) {
            return JSONObjectUtils.parse(body);
        }

        // try JWE first
        if (body.chars().filter(c -> c == '.').count() == 4) {
            if (encJwk == null)
                throw new JOSEException("no privateEncJwk configured for JWE userinfo");

            var inner = decryptJWE(body);
            if (looksLikeCompactJWT(inner) && inner.chars().filter(c -> c == '.').count() == 2) {
                var json = verifyJWS(inner);
                return JSONObjectUtils.parse(json);
            }

            return JSONObjectUtils.parse(inner);
        }

        var json = verifyJWS(body);
        return JSONObjectUtils.parse(json);
    }

    private String decryptJWE(String jweCompact) throws Exception {
        var jwe = JWEObject.parse(jweCompact);
        var alg = jwe.getHeader().getAlgorithm();
        var enc = jwe.getHeader().getEncryptionMethod();
        if (encJwk == null)
            throw new JOSEException("encJwk is null");

        if ("RSA".equalsIgnoreCase(encJwk.getKeyType().getValue())) {
            var dec = new RSADecrypter((RSAPrivateKey) encJwk.toRSAKey().toPrivateKey());
            jwe.decrypt(dec);
        } else if ("EC".equalsIgnoreCase(encJwk.getKeyType().getValue())) {
            var dec = new ECDHDecrypter((ECPrivateKey) encJwk.toECKey().toPrivateKey());
            jwe.decrypt(dec);
        } else {
            throw new JOSEException("unsupported enc key type: " + encJwk.getKeyType());
        }

        var p = jwe.getPayload();
        return p.toString();
    }

    private String verifyJWS(String jwsCompact) throws Exception {
        if (jwkSource == null)
            throw new JOSEException("no jwks_uri configured to verify JWS");

        var jws = SignedJWT.parse(jwsCompact);
        var jwks = JWKSet.load(jwksUri);
        var candidates = jwks.getKeys();
        var ok = false;
        for (var k : candidates) {
            if (k.getKeyID() != null && !k.getKeyID().equals(jws.getHeader().getKeyID()))
                continue;
            if (k.getKeyType() == null)
                continue;

            JWSVerifier verifier = null;
            switch (String.valueOf(k.getKeyType())) {
                case "EC" -> verifier = new ECDSAVerifier(k.toECKey());
                case "RSA" -> verifier = new RSASSAVerifier(k.toRSAKey());
                default -> {
                }
            }
            if (verifier != null && jws.verify(verifier)) {
                ok = true;
                break;
            }
        }
        if (!ok)
            throw new JOSEException("userinfo JWS signature verification failed");

        return jws.getPayload().toString();
    }
}
