import { UserManager, WebStorageStateStore } from "oidc-client-ts";

export const oidc = new UserManager({
    authority: "http://eservice.localhost/auth/realms/agency-realm",
    client_id: "aceas-spa",
    redirect_uri: `${window.location.origin}/aceas/callback.html`,        // FIXED path
    // silent_redirect_uri: `${window.location.origin}/aceas/silent-callback.html`,
    silent_redirect_uri: `${window.location.origin}/aceas/callback.html?type=silent`, // workaround: some browser block third-party cookie in iframe
    response_type: "code", // pkce
    scope: "openid profile email",
    userStore: new WebStorageStateStore({ store: window.localStorage }),
    automaticSilentRenew: true, // try renew via iframe
    monitorSession: true,       // watch RP session (check-session-iframe)
});
