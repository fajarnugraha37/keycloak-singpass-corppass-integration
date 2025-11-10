package com.example.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PARUtil - FAPI 2.0 Pushed Authorization Request
 */
@ExtendWith(MockitoExtension.class)
class PARUtilTest {

    @Mock
    private KeycloakSession session;

    @Mock
    private SimpleHttp.Response mockResponse;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testPushAuthorizationRequest_Success() throws IOException {
        // Given
        String parEndpoint = "https://auth.example.com/par";
        String expectedRequestUri = "urn:ietf:params:oauth:request_uri:bwc4JK-ESC0w8acc191e-Y1LTC2";
        int expiresIn = 60;

        // Create mock response JSON
        ObjectNode responseJson = objectMapper.createObjectNode();
        responseJson.put("request_uri", expectedRequestUri);
        responseJson.put("expires_in", expiresIn);

        // Mock SimpleHttp and Response
        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.asJson()).thenReturn(responseJson);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);

        // When
        String requestUri = PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);

        // Then
        assertNotNull(requestUri, "Request URI should not be null");
        assertEquals(expectedRequestUri, requestUri, "Request URI should match expected value");
        verify(mockResponse).getStatus();
        verify(mockResponse).asJson();
    }

    @Test
    void testPushAuthorizationRequest_SuccessWithStatus200() throws IOException {
        // Given
        String parEndpoint = "https://auth.example.com/par";
        String expectedRequestUri = "urn:ietf:params:oauth:request_uri:test123";

        ObjectNode responseJson = objectMapper.createObjectNode();
        responseJson.put("request_uri", expectedRequestUri);
        responseJson.put("expires_in", 90);

        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockResponse.asJson()).thenReturn(responseJson);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);

        // When
        String requestUri = PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);

        // Then
        assertEquals(expectedRequestUri, requestUri, "Request URI should match expected value");
    }

    @Test
    void testPushAuthorizationRequest_FailureWithError() throws IOException {
        // Given
        String parEndpoint = "https://auth.example.com/par";
        String errorBody = "{\"error\":\"invalid_request\",\"error_description\":\"Missing required parameter\"}";

        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.asString()).thenReturn(errorBody);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);
        when(mockSimpleHttp.getUrl()).thenReturn(parEndpoint);

        // When & Then
        IOException exception = assertThrows(IOException.class, () -> {
            PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);
        });

        assertTrue(exception.getMessage().contains("PAR request failed with status 400"),
                  "Exception message should indicate PAR request failure");
        assertTrue(exception.getMessage().contains(errorBody),
                  "Exception message should contain error body");
    }

    @Test
    void testPushAuthorizationRequest_Status500() throws IOException {
        // Given
        String parEndpoint = "https://auth.example.com/par";
        String errorBody = "Internal Server Error";

        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(500);
        when(mockResponse.asString()).thenReturn(errorBody);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);
        when(mockSimpleHttp.getUrl()).thenReturn(parEndpoint);

        // When & Then
        IOException exception = assertThrows(IOException.class, () -> {
            PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);
        });

        assertTrue(exception.getMessage().contains("500"),
                  "Exception should indicate status 500");
    }

    @Test
    void testPushAuthorizationRequest_MissingRequestUri() throws IOException {
        // Given
        String parEndpoint = "https://auth.example.com/par";

        // Response without request_uri
        ObjectNode responseJson = objectMapper.createObjectNode();
        responseJson.put("expires_in", 60);

        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.asJson()).thenReturn(responseJson);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);

        // When & Then
        IOException exception = assertThrows(IOException.class, () -> {
            PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);
        });

        assertTrue(exception.getMessage().contains("No request_uri in PAR response"),
                  "Exception should indicate missing request_uri");
    }

    @Test
    void testPushAuthorizationRequest_EmptyRequestUri() throws IOException {
        // Given
        String parEndpoint = "https://auth.example.com/par";

        ObjectNode responseJson = objectMapper.createObjectNode();
        responseJson.put("request_uri", "");
        responseJson.put("expires_in", 60);

        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.asJson()).thenReturn(responseJson);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);

        // When & Then
        IOException exception = assertThrows(IOException.class, () -> {
            PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);
        });

        assertTrue(exception.getMessage().contains("No request_uri in PAR response"),
                  "Exception should indicate missing request_uri");
    }

    @Test
    void testPushAuthorizationRequest_WithoutExpiresIn() throws IOException {
        // Given
        String parEndpoint = "https://auth.example.com/par";
        String expectedRequestUri = "urn:ietf:params:oauth:request_uri:test";

        // Response without expires_in
        ObjectNode responseJson = objectMapper.createObjectNode();
        responseJson.put("request_uri", expectedRequestUri);

        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.asJson()).thenReturn(responseJson);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);

        // When
        String requestUri = PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);

        // Then
        assertEquals(expectedRequestUri, requestUri, "Should still succeed without expires_in");
    }

    @Test
    void testIsValidPAREndpoint_ValidHttps() {
        // Given
        String validEndpoint = "https://auth.example.com/par";

        // When
        boolean isValid = PARUtil.isValidPAREndpoint(validEndpoint);

        // Then
        assertTrue(isValid, "HTTPS endpoint should be valid");
    }

    @Test
    void testIsValidPAREndpoint_InvalidHttp() {
        // Given
        String httpEndpoint = "http://auth.example.com/par";

        // When
        boolean isValid = PARUtil.isValidPAREndpoint(httpEndpoint);

        // Then
        assertFalse(isValid, "HTTP endpoint should not be valid (HTTPS required for FAPI 2.0)");
    }

    @Test
    void testIsValidPAREndpoint_Null() {
        // When
        boolean isValid = PARUtil.isValidPAREndpoint(null);

        // Then
        assertFalse(isValid, "Null endpoint should not be valid");
    }

    @Test
    void testIsValidPAREndpoint_Empty() {
        // When
        boolean isValid = PARUtil.isValidPAREndpoint("");

        // Then
        assertFalse(isValid, "Empty endpoint should not be valid");
    }

    @Test
    void testIsValidPAREndpoint_InvalidScheme() {
        // Given
        String[] invalidEndpoints = {
            "ftp://auth.example.com/par",
            "ws://auth.example.com/par",
            "file:///path/to/par",
            "auth.example.com/par"
        };

        // When & Then
        for (String endpoint : invalidEndpoints) {
            assertFalse(PARUtil.isValidPAREndpoint(endpoint),
                       "Invalid endpoint should not be valid: " + endpoint);
        }
    }

    @Test
    void testIsValidPAREndpoint_ValidComplexUrls() {
        // Given
        String[] validEndpoints = {
            "https://auth.example.com/par",
            "https://auth.example.com:8443/oauth/par",
            "https://auth.example.com/v2/par?client_id=123",
            "https://subdomain.auth.example.com/par"
        };

        // When & Then
        for (String endpoint : validEndpoints) {
            assertTrue(PARUtil.isValidPAREndpoint(endpoint),
                      "Valid HTTPS endpoint should be valid: " + endpoint);
        }
    }

    @Test
    void testPushAuthorizationRequest_Status201Created() throws IOException {
        // Given - PAR spec suggests 201 Created as the success status
        String parEndpoint = "https://auth.example.com/par";
        String expectedRequestUri = "urn:ietf:params:oauth:request_uri:created";

        ObjectNode responseJson = objectMapper.createObjectNode();
        responseJson.put("request_uri", expectedRequestUri);
        responseJson.put("expires_in", 60);

        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.asJson()).thenReturn(responseJson);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);

        // When
        String requestUri = PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);

        // Then
        assertEquals(expectedRequestUri, requestUri, "Should handle 201 Created status");
    }

    @Test
    void testPushAuthorizationRequest_Status401Unauthorized() throws IOException {
        // Given
        String parEndpoint = "https://auth.example.com/par";
        String errorBody = "{\"error\":\"unauthorized_client\"}";

        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.asString()).thenReturn(errorBody);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);
        when(mockSimpleHttp.getUrl()).thenReturn(parEndpoint);

        // When & Then
        IOException exception = assertThrows(IOException.class, () -> {
            PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);
        });

        assertTrue(exception.getMessage().contains("401"),
                  "Should throw exception for 401 Unauthorized");
    }

    @Test
    void testPushAuthorizationRequest_Status403Forbidden() throws IOException {
        // Given
        String parEndpoint = "https://auth.example.com/par";
        String errorBody = "{\"error\":\"access_denied\"}";

        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(403);
        when(mockResponse.asString()).thenReturn(errorBody);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);
        when(mockSimpleHttp.getUrl()).thenReturn(parEndpoint);

        // When & Then
        IOException exception = assertThrows(IOException.class, () -> {
            PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);
        });

        assertTrue(exception.getMessage().contains("403"),
                  "Should throw exception for 403 Forbidden");
    }

    @Test
    void testPushAuthorizationRequest_ValidRequestUriFormat() throws IOException {
        // Given - Verify URN format as per RFC 9126
        String parEndpoint = "https://auth.example.com/par";
        String expectedRequestUri = "urn:ietf:params:oauth:request_uri:6esc_11ACC5bwc014ltc14eY";

        ObjectNode responseJson = objectMapper.createObjectNode();
        responseJson.put("request_uri", expectedRequestUri);
        responseJson.put("expires_in", 60);

        SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.asJson()).thenReturn(responseJson);
        when(mockSimpleHttp.asResponse()).thenReturn(mockResponse);

        // When
        String requestUri = PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);

        // Then
        assertTrue(requestUri.startsWith("urn:ietf:params:oauth:request_uri:"),
                  "Request URI should follow URN format from RFC 9126");
    }

    @Test
    void testPushAuthorizationRequest_ExpiresInRange() throws IOException {
        // Given - Test different expires_in values
        String parEndpoint = "https://auth.example.com/par";
        int[] expiresInValues = {30, 60, 90, 120, 600};

        for (int expiresIn : expiresInValues) {
            ObjectNode responseJson = objectMapper.createObjectNode();
            responseJson.put("request_uri", "urn:ietf:params:oauth:request_uri:test");
            responseJson.put("expires_in", expiresIn);

            SimpleHttp mockSimpleHttp = mock(SimpleHttp.class);
            SimpleHttp.Response mockResp = mock(SimpleHttp.Response.class);
            when(mockResp.getStatus()).thenReturn(201);
            when(mockResp.asJson()).thenReturn(responseJson);
            when(mockSimpleHttp.asResponse()).thenReturn(mockResp);

            // When
            String requestUri = PARUtil.pushAuthorizationRequest(session, parEndpoint, mockSimpleHttp);

            // Then
            assertNotNull(requestUri, "Should handle expires_in value: " + expiresIn);
        }
    }
}

