package com.example.utils;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

/**
 * Utility class for PAR (Pushed Authorization Request) as required by FAPI 2.0
 * <p>
 * PAR allows clients to push authorization request parameters directly to the
 * authorization server before redirecting the user, improving security by keeping
 * sensitive parameters server-side.
 */
public class PARUtil {
    private static final Logger logger = Logger.getLogger(PARUtil.class);

    private PARUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Pushes authorization request parameters to the PAR endpoint
     *
     * @param session     The Keycloak session
     * @param parEndpoint The PAR endpoint URL
     * @param request     The SimpleHttp request with all authorization parameters
     * @return The request_uri returned by the PAR endpoint
     * @throws IOException if the PAR request fails
     */
    public static String pushAuthorizationRequest(KeycloakSession session,
                                                  String parEndpoint,
                                                  SimpleHttp request) throws IOException {
        logger.infof("[pushAuthorizationRequest] Pushing authorization request to PAR endpoint: %s", parEndpoint);

        try (var response = request.asResponse()) {
            var status = response.getStatus();

            if (status < 200 || status >= 300) {
                var errorBody = response.asString();
                logger.errorf("[pushAuthorizationRequest] PAR request failed with status %d: %s", status, errorBody);
                throw new IOException("PAR request failed with status " + status + ": " + errorBody);
            }

            var responseJson = response.asJson();
            var requestUri = responseJson.path("request_uri").asText();
            var expiresIn = responseJson.path("expires_in").asInt(0);

            if (requestUri == null || requestUri.isEmpty()) {
                logger.error("[pushAuthorizationRequest] No request_uri in PAR response");
                throw new IOException("No request_uri in PAR response");
            }

            logger.infof("[pushAuthorizationRequest] PAR request successful. request_uri: %s, expires_in: %d",
                    requestUri, expiresIn);

            return requestUri;
        }
    }

    /**
     * Validates PAR endpoint URL
     *
     * @param parEndpoint The PAR endpoint to validate
     * @return true if the endpoint is valid
     */
    public static boolean isValidPAREndpoint(String parEndpoint) {
        if (parEndpoint == null || parEndpoint.isEmpty()) {
            return false;
        }

        return parEndpoint.startsWith("https://");
    }
}
