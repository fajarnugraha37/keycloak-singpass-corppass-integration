package com.example.utils;

import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MapperUtilTest {
    @Test
    void testToMap_withNestedJsonObject() {
        var token = Mockito.mock(JsonWebToken.class);
        var nested = new HashMap<String, Object>();
        nested.put("innerKey", "innerValue");

        var claims = new HashMap<String, Object>();
        claims.put("nested", nested);
        Mockito.when(token.getOtherClaims()).thenReturn(claims);
        Mockito.when(token.getCategory()).thenReturn(null);
        Mockito.when(token.getId()).thenReturn("id123");
        Mockito.when(token.getExp()).thenReturn(123L);
        Mockito.when(token.getNbf()).thenReturn(456L);
        Mockito.when(token.getIat()).thenReturn(789L);
        Mockito.when(token.getIssuer()).thenReturn("issuer");
        Mockito.when(token.getAudience()).<List<String>>thenReturn(new String[]{"aud1", "aud2"});
        Mockito.when(token.getSubject()).thenReturn("subject");
        Mockito.when(token.getType()).thenReturn("type");
        Mockito.when(token.getIssuedFor()).thenReturn("issuedFor");

        var result = MapperUtil.toMap(token);
        assertEquals("innerValue", result.get("id_token.nested.innerKey"));
        assertEquals("id123", result.get("id_token.id"));
        assertEquals("123", result.get("id_token.exp"));
        assertEquals("456", result.get("id_token.nbf"));
        assertEquals("789", result.get("id_token.iat"));
        assertEquals("issuer", result.get("id_token.issuer"));
        assertEquals("aud1, aud2", result.get("id_token.audience"));
        assertEquals("subject", result.get("id_token.subject"));
        assertEquals("type", result.get("id_token.type"));
        assertEquals("issuedFor", result.get("id_token.issued_for"));
    }

    @Test
    void testToMap_withArrayClaim() {
        var token = Mockito.mock(JsonWebToken.class);
        var claims = new HashMap<String, Object>();
        claims.put("arrayClaim", new String[]{"a", "b", "c"});
        Mockito.when(token.getOtherClaims()).thenReturn(claims);
        Mockito.when(token.getCategory()).thenReturn(null);
        Mockito.when(token.getId()).thenReturn(null);
        Mockito.when(token.getExp()).thenReturn(null);
        Mockito.when(token.getNbf()).thenReturn(null);
        Mockito.when(token.getIat()).thenReturn(null);
        Mockito.when(token.getIssuer()).thenReturn(null);
        Mockito.when(token.getAudience()).thenReturn(null);
        Mockito.when(token.getSubject()).thenReturn(null);
        Mockito.when(token.getType()).thenReturn(null);
        Mockito.when(token.getIssuedFor()).thenReturn(null);

        var result = MapperUtil.toMap(token);
        assertTrue(result.get("id_token.arrayClaim").contains("a"));
        assertTrue(result.get("id_token.arrayClaim").contains("b"));
        assertTrue(result.get("id_token.arrayClaim").contains("c"));
    }

    @Test
    void testToMap_withIterableClaim() {
        var token = Mockito.mock(JsonWebToken.class);
        var claims = new HashMap<String, Object>();
        claims.put("iterableClaim", Arrays.asList(1, 2, 3));
        Mockito.when(token.getOtherClaims()).thenReturn(claims);
        Mockito.when(token.getCategory()).thenReturn(null);
        Mockito.when(token.getId()).thenReturn(null);
        Mockito.when(token.getExp()).thenReturn(null);
        Mockito.when(token.getNbf()).thenReturn(null);
        Mockito.when(token.getIat()).thenReturn(null);
        Mockito.when(token.getIssuer()).thenReturn(null);
        Mockito.when(token.getAudience()).thenReturn(null);
        Mockito.when(token.getSubject()).thenReturn(null);
        Mockito.when(token.getType()).thenReturn(null);
        Mockito.when(token.getIssuedFor()).thenReturn(null);

        var result = MapperUtil.toMap(token);
        assertTrue(result.get("id_token.iterableClaim").contains("1"));
        assertTrue(result.get("id_token.iterableClaim").contains("2"));
        assertTrue(result.get("id_token.iterableClaim").contains("3"));
    }

    @Test
    void testToMap_withNullToken() {
        var result = MapperUtil.toMap((JsonWebToken) null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testToMap_withNestedJsonObject_AccessTokenResponse() {
        var response = Mockito.mock(AccessTokenResponse.class);
        var nested = new HashMap<String, Object>();
        nested.put("innerKey", "innerValue");
        var claims = new HashMap<String, Object>();
        claims.put("nested", nested);
        Mockito.when(response.getOtherClaims()).thenReturn(claims);
        Mockito.when(response.getToken()).thenReturn("token");
        Mockito.when(response.getExpiresIn()).thenReturn(100L);
        Mockito.when(response.getRefreshExpiresIn()).thenReturn(200L);
        Mockito.when(response.getRefreshToken()).thenReturn("refreshToken");
        Mockito.when(response.getTokenType()).thenReturn("Bearer");
        Mockito.when(response.getIdToken()).thenReturn("idToken");
        Mockito.when(response.getNotBeforePolicy()).thenReturn(0);
        Mockito.when(response.getSessionState()).thenReturn("sessionState");
        Mockito.when(response.getScope()).thenReturn("scope");
        Mockito.when(response.getError()).thenReturn(null);
        Mockito.when(response.getErrorDescription()).thenReturn(null);
        Mockito.when(response.getErrorUri()).thenReturn(null);

        var result = MapperUtil.toMap(response);
        assertEquals("innerValue", result.get("access_token.nested.innerKey"));
        assertEquals("token", result.get("access_token.token"));
        assertEquals("100", result.get("access_token.expires_in"));
        assertEquals("200", result.get("access_token.refresh_expires_in"));
        assertEquals("refreshToken", result.get("access_token.refresh_token"));
        assertEquals("Bearer", result.get("access_token.token_type"));
        assertEquals("idToken", result.get("access_token.id_token"));
        assertEquals("0", result.get("access_token.not_before_policy"));
        assertEquals("sessionState", result.get("access_token.session_state"));
        assertEquals("scope", result.get("access_token.scope"));
    }

    @Test
    void testToMap_withArrayClaim_AccessTokenResponse() {
        var response = Mockito.mock(AccessTokenResponse.class);
        var claims = new HashMap<String, Object>();
        claims.put("arrayClaim", new String[]{"x", "y"});
        Mockito.when(response.getOtherClaims()).thenReturn(claims);
        Mockito.when(response.getToken()).thenReturn(null);
        Mockito.when(response.getExpiresIn()).thenReturn(0L);
        Mockito.when(response.getRefreshExpiresIn()).thenReturn(0L);
        Mockito.when(response.getRefreshToken()).thenReturn(null);
        Mockito.when(response.getTokenType()).thenReturn(null);
        Mockito.when(response.getIdToken()).thenReturn(null);
        Mockito.when(response.getNotBeforePolicy()).thenReturn(0);
        Mockito.when(response.getSessionState()).thenReturn(null);
        Mockito.when(response.getScope()).thenReturn(null);
        Mockito.when(response.getError()).thenReturn(null);
        Mockito.when(response.getErrorDescription()).thenReturn(null);
        Mockito.when(response.getErrorUri()).thenReturn(null);

        var result = MapperUtil.toMap(response);
        assertTrue(result.get("access_token.arrayClaim").contains("x"));
        assertTrue(result.get("access_token.arrayClaim").contains("y"));
    }

    @Test
    void testToMap_withNullAccessTokenResponse() {
        var result = MapperUtil.toMap((AccessTokenResponse) null);
        assertTrue(result.isEmpty());
    }
}

