package com.example.utils;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.function.BiFunction;

import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTParser;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;

import com.example.config.CustomOIDCIdentityProviderConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

public class JwtUtil {
    private static final org.jboss.logging.Logger logger = org.jboss.logging.Logger.getLogger(JwtUtil.class);

    private JwtUtil() {
        // Private constructor to prevent instantiation
    }

    public static boolean isJwt(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        var parts = token.split("\\.");
        return parts.length == 3;
    }

    public static JsonWebToken validateToken(OIDCIdentityProviderConfig config,
                                             String encodedToken,
                                             boolean ignoreAudience,
                                             BiFunction<String, Boolean, String> parseTokenInput) {
        JsonWebToken token;
        try {
            var parseToken = parseTokenInput.apply(encodedToken, true);
            token = JsonSerialization.readValue(parseToken, JsonWebToken.class);
        } catch (IOException e) {
            throw new IdentityBrokerException("[validateToken] Invalid token", e);
        }

        var iss = token.getIssuer();
        var allowedTimeSkew = config.getAllowedClockSkew();
        if (!token.isActive(allowedTimeSkew)) {
            throw new IdentityBrokerException("[validateToken] Token is no longer valid");
        }

        var clientId = config.getClientId();
        var aud = token.getAudience();
        if (!ignoreAudience && !token.hasAudience(clientId)) {
            throw new IdentityBrokerException("[validateToken] Wrong audience from token: " + Arrays.toString(aud));
        }

        var issuerFor = token.getIssuedFor();
        if (!ignoreAudience && (issuerFor != null && !config.getClientId().equals(issuerFor))) {
            throw new IdentityBrokerException("[validateToken] Token issued for does not match client id");
        }

        var trustedIssuers = config.getIssuer();
        if (trustedIssuers != null && !trustedIssuers.isEmpty()) {
            var issuers = trustedIssuers.split(",");
            for (var trustedIssuer : issuers) {
                if (iss != null && iss.equals(trustedIssuer.trim())) {
                    return token;
                }
            }

            throw new IdentityBrokerException("[validateToken] Wrong issuer from token. Got: " + iss + " expected: " + config.getIssuer());
        }

        return token;
    }

    public static String createBearer(CustomOIDCIdentityProviderConfig configuration,
                                      KeyWrapper keyWrapper,
                                      JWSAlgorithm algorithm) {
        var header = new JWSHeader.Builder(algorithm)
                .type(JOSEObjectType.JWT)
                .keyID(keyWrapper.getKid())
                .build();
        var differentTimes = isNull(configuration.getIdpDifferentTimes())
                ? 0
                : Integer.parseInt(configuration.getIdpDifferentTimes());
        var now = Instant.now().plusSeconds(differentTimes);
        var payload = new JWTClaimsSet.Builder()
                .subject(configuration.getClientId())
                .issuer(configuration.getClientId())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(120)))
                .audience(configuration.getIssuer())
                .build();

        var signedJWT = new SignedJWT(header, payload);
        try {
            signedJWT.sign(new ECDSASigner((ECPrivateKey) keyWrapper.getPrivateKey()));
        } catch (JOSEException e) {
            logger.errorf(e, "Failed to sign the JWT. Messages: %s", e.getMessage());
        }

        return signedJWT.serialize();
    }

    public static AccessTokenResponse decryptAccessTokenResponse(String accessTokenResponse,
                                                                 KeycloakSession session,
                                                                 boolean isEncryptedIdTokenFlag) {
        try {
            var tokenResponse = JsonSerialization.readValue(accessTokenResponse, AccessTokenResponse.class);
            if (isEncryptedIdTokenFlag) {
                var idToken = tokenResponse.getIdToken();
                if (idToken == null || idToken.isEmpty()) {
                    var exception = new IOException("Error parsing the response: " + accessTokenResponse);
                    logger.errorf(exception, "Error parsing the response: %", accessTokenResponse);
                    throw exception;
                }

                var encryptedJWT = (EncryptedJWT) JWTParser.parse(idToken);
                var keyID = encryptedJWT.getHeader().getKeyID();
                var algorithm = encryptedJWT.getHeader().getAlgorithm().getName();
                logger.infof("Decrypting ID Token with Key ID: %s and Algorithm: %s", keyID, algorithm);

                var key = session.keys().getKey(session.getContext().getRealm(), keyID, KeyUse.ENC, algorithm);
                var privateKey = (ECPrivateKey) key.getPrivateKey();
                var decrypter = new ECDHDecrypter(privateKey);
                logger.infof("Using private key with format: %s", privateKey.getFormat());

                encryptedJWT.decrypt(decrypter);
                tokenResponse.setIdToken(encryptedJWT.getPayload().toString());
            }

            logger.info("Decrypted Access Token Response");
            logger.infof("getToken: %s", tokenResponse.getToken());
            logger.infof("getIdToken: %s", tokenResponse.getIdToken());
            logger.infof("getRefreshToken: %s", tokenResponse.getRefreshToken());
            logger.infof("getRefreshExpiresIn: %s", tokenResponse.getRefreshExpiresIn());
            logger.infof("getError: %s", tokenResponse.getError());
            logger.infof("getErrorDescription: %s", tokenResponse.getErrorDescription());
            logger.infof("getErrorUri: %s", tokenResponse.getErrorUri());
            logger.infof("getExpiresIn: %s", tokenResponse.getExpiresIn());
            logger.infof("getNotBeforePolicy: %s", tokenResponse.getNotBeforePolicy());
            logger.infof("getScope: %s", tokenResponse.getScope());
            logger.infof("getSessionState: %s", tokenResponse.getSessionState());
            logger.infof("getTokenType: %s", tokenResponse.getTokenType());
            tokenResponse.getOtherClaims()
                    .forEach((key, value) -> logger.infof("Other Claim - %s: %s", key, value));

            return tokenResponse;
        } catch (Exception e) {
            logger.errorf(e, "Could not decode access token response. Message: %s", e.getMessage());
            throw new IdentityBrokerException("Could not decode access token response.", e);
        }
    }

    public static String verifyAccessToken(AccessTokenResponse tokenResponse) {
        var accessToken = tokenResponse.getToken();
        if (accessToken == null) {
            var exception = new RuntimeException("No access_token from server.");
            logger.errorf(exception, "No access_token from server.");
            throw exception;
        }

        return accessToken;
    }
}
