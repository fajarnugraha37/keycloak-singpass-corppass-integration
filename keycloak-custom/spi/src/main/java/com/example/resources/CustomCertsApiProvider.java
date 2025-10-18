package com.example.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.crypto.KeyType;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.util.CacheControlUtil;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class CustomCertsApiProvider implements RealmResourceProvider {

    public CustomCertsApiProvider() {

    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
    }

    @GET
    @Path("certs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response certs(@Context final KeycloakSession session) {
        var realm = session.getContext().getRealm();
        var jwks = session.keys().getKeysStream(realm)
                .filter(k -> k.getStatus().isEnabled() && nonNull(k.getPublicKey()))
                .map(k -> {
                    JWKBuilder b = JWKBuilder.create().kid(k.getKid()).algorithm(k.getAlgorithmOrDefault());
                    List<X509Certificate> certificates = Optional.ofNullable(k.getCertificateChain())
                            .filter(certs -> !certs.isEmpty())
                            .orElseGet(() -> Collections.singletonList(k.getCertificate()));
                    if (k.getType().equals(KeyType.RSA)) {
                        return b.rsa(k.getPublicKey(), certificates, k.getUse());
                    } else if (k.getType().equalsIgnoreCase(KeyType.EC)) {
                        JWK ecKey = b.ec(k.getPublicKey());
                        ecKey.setPublicKeyUse(k.getUse().getSpecName());
                        return ecKey;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toArray(JWK[]::new);
        var keySet = new JSONWebKeySet();
        keySet.setKeys(jwks);

        var responseBuilder = Response.ok(keySet)
                .cacheControl(CacheControlUtil.getDefaultCacheControl());
        // return Cors.add(session.getContext().getHttpRequest(),
        // responseBuilder).allowedOrigins("*").auth().build();
        return Cors.builder()
                .allowedOrigins("*")
                .auth()
                .add(responseBuilder);
    }

}