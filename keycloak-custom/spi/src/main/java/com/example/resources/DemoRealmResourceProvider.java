package com.example.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;

import com.example.utils.Encryptor;

@Path("demo")
public final class DemoRealmResourceProvider
        implements RealmResourceProvider, org.keycloak.services.resource.RealmResourceProviderFactory {
    private static final Logger logger = Logger.getLogger(DemoRealmResourceProvider.class);
    private final AuthenticationManager.AuthResult auth;

    public DemoRealmResourceProvider() {
        this.auth = null;
    }

    public DemoRealmResourceProvider(KeycloakSession session) {
        this.auth = new Supplier<AuthenticationManager.AuthResult>() {
            @Override
            public AuthenticationManager.AuthResult get() {
                var realm = session.getContext().getRealm();
                var bearerTokenCheck = new AppAuthManager.BearerTokenAuthenticator(session);
                var authResult = bearerTokenCheck.authenticate();
                if (authResult == null) {
                    var cookieAuth = new AppAuthManager().authenticateIdentityCookie(session, realm);
                    if (cookieAuth != null) {
                        return cookieAuth;
                    }
                }
                return authResult;
            }
        }.get();

        if (this.auth != null) {
            logger.infof("Authenticated user: %s", this.auth.getUser().getUsername());
        } else {
            logger.info("Unauthenticated request");
        }
    }

    @GET
    @Path("ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ping() throws Exception {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("""
                            {
                                "error": "unauthorized",
                                "error_description": "User must be authenticated to access this resource"
                            }
                            """)
                    .build();
        }
        var clientId = this.auth.getClient().getClientId();
        var username = auth.getUser().getUsername();
        var encryptedValue = Encryptor.encrypt("some-value", clientId + "-" + username);
        logger.infof("Client ID: %s with Username: %s", clientId, username);

        return Response.ok(String.format("""
                {
                    "status": "ok",
                    "message": "Hello from custom realm resource provider!",
                    "timestamp": "%s",
                    "clientId": "%s",
                    "token": "%s"
                }
                """, System.currentTimeMillis(), clientId, encryptedValue)).build();
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "demo";
    }

    @Override
    public RealmResourceProvider create(KeycloakSession s) {
        return new DemoRealmResourceProvider(s);
    }

    @Override
    public void init(org.keycloak.Config.Scope s) {
    }

    @Override
    public void postInit(org.keycloak.models.KeycloakSessionFactory f) {
    }
}
