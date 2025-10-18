package com.example.resources;

import com.example.resources.dto.UserPayloadDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.keycloak.common.util.Time;
import org.keycloak.services.resource.RealmResourceProvider;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.Base64;
import java.util.Locale;

public abstract class AlternateApiProvider implements RealmResourceProvider {
    protected static final String NON_CORPPASS_FORBIDDEN_ERROR_MESSAGE = "Forbidden!! Only a CorpPass user could perform this action.";
    protected static final String AUTHORIZATION_TOKEN_EXPIRED_ERROR_MESSAGE = "Error!! Using expired authorization token.";

    protected final ObjectMapper objectMapper;

    public AlternateApiProvider() {
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, Boolean.FALSE);
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public Object getResource() {
        return this;
    }

    protected UserPayloadDto parseAuthorizationToken(String authorizationHeader) throws JsonProcessingException {
        var accessToken = authorizationHeader.replace("Bearer", "").trim();
        var encodedPayload = accessToken.split("\\.")[1];
        var payload = new String(Base64.getUrlDecoder().decode(encodedPayload));

        return objectMapper.readValue(payload, UserPayloadDto.class);
    }

    protected void validateAuthorizationTokenExpiry(UserPayloadDto userPayloadDto) {
        if (Time.currentTime() > userPayloadDto.getExp()) {
            throw new NotAuthorizedException(AUTHORIZATION_TOKEN_EXPIRED_ERROR_MESSAGE);
        }
    }

    protected void validateOnlyCorpPassAccountAllowed(UserPayloadDto userPayloadDto) {
        if (!userPayloadDto.getPreferred_username().toLowerCase(Locale.ROOT).contains("corppass")) {
            throw new ForbiddenException(NON_CORPPASS_FORBIDDEN_ERROR_MESSAGE);
        }
    }
}