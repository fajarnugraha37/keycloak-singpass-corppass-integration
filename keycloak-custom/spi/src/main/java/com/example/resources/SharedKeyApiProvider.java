package com.example.resources;

import com.example.resources.dto.ResponseModelUtil;
import com.example.resources.dto.SharedKeyRequest;
import com.example.resources.dto.UserPayloadDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.crypto.KeyUse;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.util.CacheControlUtil;

import java.security.interfaces.ECPrivateKey;
import java.util.Optional;

public class SharedKeyApiProvider extends AlternateApiProvider {
    private static final Logger LOGGER = Logger.getLogger(SharedKeyApiProvider.class);

    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";
    private static final String UNEXPECTED_ERROR_MESSAGE = "Error!! Unable to retrieve shared key";
    private static final String UNAUTHORIZED_ERROR_MESSAGE = "You're not authorized to access this resources";
    private static final String ADMIN_ROLE = "admin";

    private final RealmModel masterRealm;

    public SharedKeyApiProvider(RealmModel masterRealm) {
        super();
        this.masterRealm = masterRealm;
    }

    /**
     * Endpoint:<br>
     * {@code http|https://<host>/auth/realms/<realm>/shared-key}
     * <br>
     * Payload:<br>
     * {@code
     * {
     *     "kid": "enc-aceas-2022-02-16",
     *     "alg": "ECDH-ES+A256KW",
     *     "keySpec": "ENC"
     * }
     * }
     * <br>
     * Notes:<br>
     * <ul>
     * <li>Use REST Client to execute the endpoint</li>
     * <li>The endpoint will allowed admin user only</li>
     * </ul>
     *
     * @param keycloakSession  Keycloak Session Object
     * @param sharedKeyRequest User request payload
     * @return JSON data containing shared key data
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSharedKey(@Context final KeycloakSession keycloakSession, SharedKeyRequest sharedKeyRequest) {
        var httpRequest = keycloakSession.getContext().getHttpRequest();
        var correlationId = httpRequest.getHttpHeaders().getHeaderString(CORRELATION_ID_HEADER_NAME);
        try {
            var authorization = httpRequest.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
            var userPayloadDto = parseAuthorizationToken(authorization);

            validateAllowedUser(keycloakSession, userPayloadDto);
            validateAuthorizationTokenExpiry(userPayloadDto);

            var key = keycloakSession
                    .keys()
                    .getKey(
                            keycloakSession.getContext().getRealm(),
                            sharedKeyRequest.getKid(),
                            KeyUse.valueOf(sharedKeyRequest.getKeySpec()),
                            sharedKeyRequest.getAlg());
            var privateKey = (ECPrivateKey) key.getPrivateKey();
            var builder = Response.ok()
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .cacheControl(CacheControlUtil.getDefaultCacheControl())
                    .header(CORRELATION_ID_HEADER_NAME, correlationId)
                    .entity(ResponseModelUtil.createDataMapResponse(
                            Base64.encodeBytes(privateKey.getEncoded()),
                            Response.Status.OK.getStatusCode(),
                            correlationId));

            return builder.build();
        } catch (ForbiddenException exception) {
            LOGGER.error(exception.getMessage());
            return sendResponse(httpRequest, Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header(CORRELATION_ID_HEADER_NAME, correlationId)
                    .entity(
                            ResponseModelUtil.createDataMapResponse(
                                    exception.getMessage(),
                                    Response.Status.FORBIDDEN.getStatusCode(),
                                    correlationId)));
        } catch (NotAuthorizedException notAuthorizedException) {
            return sendResponse(httpRequest, Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header(CORRELATION_ID_HEADER_NAME, correlationId)
                    .entity(
                            ResponseModelUtil.createDataMapResponse(
                                    notAuthorizedException.getMessage(),
                                    Response.Status.UNAUTHORIZED.getStatusCode(),
                                    correlationId)));
        } catch (JsonProcessingException | RuntimeException exception) {
            LOGGER.error(UNEXPECTED_ERROR_MESSAGE, exception);
            return sendResponse(httpRequest, Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header(CORRELATION_ID_HEADER_NAME, correlationId)
                    .entity(
                            ResponseModelUtil.createDataMapResponse(
                                    UNEXPECTED_ERROR_MESSAGE,
                                    Response.Status.BAD_REQUEST.getStatusCode(),
                                    correlationId)));
        }
    }

    /**
     * Only allow user have `admin` role and should be member of `master` realm
     *
     * @param keycloakSession Keycloak session object
     * @param userPayloadDto  Current user information extracted from bearer token
     */
    void validateAllowedUser(KeycloakSession keycloakSession, UserPayloadDto userPayloadDto) {
        var isAllowed = Optional
                .of(keycloakSession.users().getUserByUsername(masterRealm, userPayloadDto.getPreferred_username()))
                .orElseThrow(() -> new ForbiddenException(UNAUTHORIZED_ERROR_MESSAGE))
                .getRoleMappingsStream()
                .map(RoleModel::getName)
                .anyMatch(ADMIN_ROLE::equalsIgnoreCase);
        if (!isAllowed) {
            throw new NotAuthorizedException(UNAUTHORIZED_ERROR_MESSAGE);
        }
        ;
    }

    Response sendResponse(HttpRequest httpRequest, Response.ResponseBuilder responseBuilder) {
        return Cors.builder()
                .allowedOrigins("*")
                .auth()
                .add(responseBuilder);
    }
}