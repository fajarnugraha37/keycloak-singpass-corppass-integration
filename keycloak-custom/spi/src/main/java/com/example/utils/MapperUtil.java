package com.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;

import java.util.Map;

public class MapperUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private MapperUtil() {
        // Private constructor to prevent instantiation
    }

    public static Map<String, String> toMap(JsonWebToken jsonWebToken) {
        if (jsonWebToken == null) {
            return new java.util.HashMap<>();
        }

        var result = new java.util.HashMap<String, String>();
        result.put("id_token.category", jsonWebToken.getCategory() != null ? jsonWebToken.getCategory().name() : null);
        result.put("id_token.id", jsonWebToken.getId() != null ? jsonWebToken.getId() : null);
        result.put("id_token.exp", jsonWebToken.getExp() != null ? String.valueOf(jsonWebToken.getExp()) : null);
        result.put("id_token.nbf", jsonWebToken.getNbf() != null ? String.valueOf(jsonWebToken.getNbf()) : null);
        result.put("id_token.iat", jsonWebToken.getIat() != null ? String.valueOf(jsonWebToken.getIat()) : null);
        result.put("id_token.issuer", jsonWebToken.getIssuer());
        result.put("id_token.audience", jsonWebToken.getAudience() != null ? String.join(", ", jsonWebToken.getAudience()) : null);
        result.put("id_token.subject", jsonWebToken.getSubject());
        result.put("id_token.type", jsonWebToken.getType());
        result.put("id_token.issued_for", jsonWebToken.getIssuedFor());

        if (jsonWebToken.getOtherClaims() != null) {
            jsonWebToken.getOtherClaims().forEach((key, value) -> {
                if (value != null) {
                    if (value instanceof Map) {
                        flattenMap("id_token." + key, (Map<?, ?>) value, result);
                    } else if (value instanceof Object[] || value instanceof Iterable<?>) {
                        try {
                            result.put("id_token." + key, objectMapper.writeValueAsString(value));
                        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                            result.put("id_token." + key, value.toString());
                        }
                    } else {
                        result.put("id_token." + key, value.toString());
                    }
                } else {
                    result.put("id_token." + key, null);
                }
            });
        }
        return result;
    }

    public static Map<String, String> toMap(AccessTokenResponse accessTokenResponse) {
        if (accessTokenResponse == null) {
            return new java.util.HashMap<>();
        }

        var result = new java.util.HashMap<String, String>();
        result.put("access_token.token", accessTokenResponse.getToken());
        result.put("access_token.expires_in", String.valueOf(accessTokenResponse.getExpiresIn()));
        result.put("access_token.refresh_expires_in", String.valueOf(accessTokenResponse.getRefreshExpiresIn()));
        result.put("access_token.refresh_token", accessTokenResponse.getRefreshToken());
        result.put("access_token.token_type", accessTokenResponse.getTokenType());
        result.put("access_token.id_token", accessTokenResponse.getIdToken());
        result.put("access_token.not_before_policy", String.valueOf(accessTokenResponse.getNotBeforePolicy()));
        result.put("access_token.session_state", accessTokenResponse.getSessionState());
        result.put("access_token.scope", accessTokenResponse.getScope());
        result.put("access_token.error", accessTokenResponse.getError());
        result.put("access_token.error_description", accessTokenResponse.getErrorDescription());
        result.put("access_token.error_uri", accessTokenResponse.getErrorUri());

        if (accessTokenResponse.getOtherClaims() != null) {
            accessTokenResponse.getOtherClaims().forEach((key, value) -> {
                if (value != null) {
                    if (value instanceof Map) {
                        flattenMap("access_token." + key, (Map<?, ?>) value, result);
                    } else if (value instanceof Object[] || value instanceof Iterable<?>) {
                        try {
                            result.put("access_token." + key, objectMapper.writeValueAsString(value));
                        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                            result.put("access_token." + key, value.toString());
                        }
                    } else {
                        result.put("access_token." + key, value.toString());
                    }
                } else {
                    result.put("access_token." + key, null);
                }
            });
        }
        return result;
    }

    static void flattenMap(String prefix, Map<?, ?> map, Map<String, String> result) {
        map.forEach((key, value) -> {
            String newKey = prefix + "." + key;
            if (value != null) {
                if (value instanceof Map) {
                    flattenMap(newKey, (Map<?, ?>) value, result);
                } else {
                    result.put(newKey, value.toString());
                }
            } else {
                result.put(newKey, null);
            }
        });
    }
}
