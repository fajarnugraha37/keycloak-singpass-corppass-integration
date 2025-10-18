package com.example.resources;

import com.example.resources.dto.ErrorResponse;
import com.example.resources.dto.UserInfoDto;
import com.example.resources.dto.UserPayloadDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.util.CacheControlUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class UserInfoAlternativeApiProvider extends AlternateApiProvider {

    public UserInfoAlternativeApiProvider() {
        super();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInfo(@Context final KeycloakSession keycloakSession) throws JsonProcessingException {
        var request = keycloakSession.getContext().getHttpRequest();
        var realm = keycloakSession.getContext().getRealm();
        var connection = keycloakSession.getContext().getConnection();
        var event = new EventBuilder(realm, keycloakSession, connection)
                .event(EventType.USER_INFO_REQUEST)
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);

        var authorizationHeaders = request.getHttpHeaders().getHeaderString("Authorization");
        if (isNull(authorizationHeaders) || authorizationHeaders.isEmpty()) {
            event.detail(Details.TOKEN_ID, "not provided");
            event.error(Errors.INVALID_TOKEN);
            return Response.status(400).entity(new ErrorResponse("Authorization not found in the header")).build();
        }
        var accessToken = authorizationHeaders.replace("Bearer", "").trim();
        var encodedPayload = accessToken.split("\\.")[1];

        var payload = new String(Base64.getUrlDecoder().decode(encodedPayload));
        var userPayloadDto = objectMapper.readValue(payload, UserPayloadDto.class);
        event.detail(Details.USERNAME, userPayloadDto.getSub());

        if (Time.currentTime() > userPayloadDto.getExp()) {
            event.detail(Details.TOKEN_ID, "expired");
            event.detail("token_exp_time", DateTimeFormatter.ISO_DATE_TIME
                    .format(Instant.ofEpochSecond(1639386603).atZone(ZoneId.of("Asia/Singapore"))));
            event.error(Errors.INVALID_TOKEN);
            return Response.status(400).entity(new ErrorResponse("Token already expired")).build();
        }

        var user = keycloakSession.users().getUserById(realm, userPayloadDto.getSub());
        if (isNull(user)) {
            event.error(Errors.USER_NOT_FOUND);
            return Response.status(400).entity(new ErrorResponse("User not found")).build();
        }
        event.user(user);

        var tenantAndZone = extractTenantAndZone(realm);
        var userInfoDto = new UserInfoDto();
        userInfoDto.setSub(UUID.fromString(user.getId()));
        userInfoDto.setPreferredUsername(user.getUsername());
        userInfoDto.setEmail(user.getEmail());
        userInfoDto.setGivenName(user.getFirstName());
        userInfoDto.setFamilyName(user.getLastName());
        userInfoDto.setTenant(tenantAndZone.get("tenant"));
        userInfoDto.setZone(tenantAndZone.get("zone"));
        userInfoDto.setRoles(user.getAttributes().get("roles"));
        userInfoDto.setBusinessUnitUri(returnFirstElementIfNotEmpty(user.getAttributes().get("business_unit_uri")));
        userInfoDto.setIdentityNumber(returnFirstElementIfNotEmpty(user.getAttributes().get("identity_number")));
        userInfoDto.setNric(returnFirstElementIfNotEmpty(user.getAttributes().get("nric")));

        var userInfo = objectMapper.writeValueAsString(userInfoDto);
        var responseBuilder = Response.ok(userInfo).cacheControl(CacheControlUtil.getDefaultCacheControl());

        event.success();
        return Cors
                .builder()
                .allowedOrigins("*")
                .auth()
                .add(responseBuilder);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInfoPostMethod(@Context final KeycloakSession keycloakSession)
            throws JsonProcessingException {
        return getUserInfo(keycloakSession);
    }

    private String returnFirstElementIfNotEmpty(List<String> userAttributes) {
        return nonNull(userAttributes) && !userAttributes.isEmpty() ? userAttributes.get(0) : null;
    }

    private Map<String, String> extractTenantAndZone(RealmModel realm) {
        var result = new HashMap<String, String>(2);
        result.put("tenant", "custom");
        result.put("zone", "all");
        if (nonNull(realm.getDisplayName())) {
            var tenantAndZone = realm.getDisplayName().split("-");
            if (tenantAndZone.length < 2) {
                result.put("tenant", tenantAndZone[0]);
                result.put("zone", "all");
            } else {
                result.put("tenant", tenantAndZone[0]);
                result.put("zone", tenantAndZone[1]);
            }
        }

        return result;
    }
}