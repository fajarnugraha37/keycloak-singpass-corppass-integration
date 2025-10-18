package com.example.resources;

import com.example.resources.dto.CustomResponseStatusCode;
import com.example.resources.dto.ResponseModelUtil;
import com.example.resources.dto.UserCheckRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jboss.logging.Logger;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.util.CacheControlUtil;

import java.util.stream.Collectors;

/**
 * Response status:
 * 400: request error
 * 401: Invalid request's authorization token
 * 403: Forbidden request
 * 404: User not found
 * 290: User exist and already assigned to the requested role
 * 291: User exist and not assigned to the requested role
 * 292: User exist but being disabled
 */
public class UserCheckApiProvider extends AlternateApiProvider {
    private static final Logger LOGGER = Logger.getLogger(UserCheckApiProvider.class);
    private static final String IDENTITY_NUMBER = "identity_number";
    private static final String NRIC = "nric";
    private static final String ROLE = "roles";
    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";
    private static final String USER_NOT_FOUND_MESSAGE = "User not found in the IDP record.";
    private static final String USER_DISABLED = "User is disabled.";
    private static final String USER_ALREADY_ASSIGNED_WITH_REQUESTED_ROLE = "User already assigned with the requested role";
    private static final String USER_NOT_ASSIGNED_WITH_REQUESTED_ROLE = "User not assigned with the requested role";
    private static final String UNEXPECTED_ERROR_MESSAGE = "Error!! Unable to complete user checking.";

    public UserCheckApiProvider() {
        super();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkUserAvailability(@Context final KeycloakSession keycloakSession,
            UserCheckRequest request) {
        var realm = keycloakSession.getContext().getRealm();
        var httpRequest = keycloakSession.getContext().getHttpRequest();
        var correlationId = httpRequest.getHttpHeaders().getHeaderString(CORRELATION_ID_HEADER_NAME);
        try {
            var authorization = httpRequest.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
            var userPayloadDto = parseAuthorizationToken(authorization);
            validateAuthorizationTokenExpiry(userPayloadDto);
            validateOnlyCorpPassAccountAllowed(userPayloadDto);

            var result = keycloakSession.users()
                    .searchForUserByUserAttributeStream(realm, NRIC, request.getSgId())
                    .filter(userModel -> userModel.getAttributeStream(IDENTITY_NUMBER)
                            .anyMatch(identityNumber -> identityNumber.equalsIgnoreCase(request.getIdentityNumber())))
                    .collect(Collectors.toList());

            var builder = Response.ok()
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .cacheControl(CacheControlUtil.getDefaultCacheControl())
                    .header(CORRELATION_ID_HEADER_NAME, correlationId);

            if (result.isEmpty()) {
                return builder.entity(
                        ResponseModelUtil.createDataMapResponse(USER_NOT_FOUND_MESSAGE,
                                Response.Status.NOT_FOUND.getStatusCode(),
                                correlationId))
                        .build();
            }

            var isDisabled = result.stream()
                    .anyMatch(userModel -> !userModel.isEnabled());
            if (isDisabled) {
                return builder.entity(
                        ResponseModelUtil.createDataMapResponse(
                                USER_DISABLED,
                                CustomResponseStatusCode.USER_DISABLED.getStatus(),
                                correlationId))
                        .build();
            }

            var isAlreadyAssignedWithRole = result.stream()
                    .anyMatch(userModel -> userModel.getAttributeStream(ROLE)
                            .anyMatch(role -> role.equalsIgnoreCase(request.getIdpRole())));
            if (isAlreadyAssignedWithRole) {
                builder.entity(
                        ResponseModelUtil.createDataMapResponse(
                                USER_ALREADY_ASSIGNED_WITH_REQUESTED_ROLE,
                                CustomResponseStatusCode.USER_EXIST_ASSIGNED.getStatus(),
                                correlationId));
            } else {
                builder.entity(
                        ResponseModelUtil.createDataMapResponse(
                                USER_NOT_ASSIGNED_WITH_REQUESTED_ROLE,
                                CustomResponseStatusCode.USER_EXIST_NOT_ASSIGNED.getStatus(),
                                correlationId));
            }
            return sendResponse(httpRequest, builder);
        } catch (NotAuthorizedException notAuthorizedException) {
            return sendResponse(httpRequest, Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header(CORRELATION_ID_HEADER_NAME, correlationId)
                    .entity(
                            ResponseModelUtil.createDataMapResponse(
                                    notAuthorizedException.getMessage(),
                                    Response.Status.UNAUTHORIZED.getStatusCode(),
                                    correlationId)));
        } catch (ForbiddenException forbiddenException) {
            return sendResponse(httpRequest, Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header(CORRELATION_ID_HEADER_NAME, correlationId)
                    .entity(
                            ResponseModelUtil.createDataMapResponse(
                                    forbiddenException.getMessage(),
                                    Response.Status.FORBIDDEN.getStatusCode(),
                                    correlationId)));
        } catch (RuntimeException | JsonProcessingException runtimeException) {
            LOGGER.error(UNEXPECTED_ERROR_MESSAGE, runtimeException);
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

    Response sendResponse(HttpRequest httpRequest, Response.ResponseBuilder responseBuilder) {
        return Cors.builder()
                .allowedOrigins("*")
                .auth()
                .add(responseBuilder);
    }
}