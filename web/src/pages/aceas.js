import './common.js';
import { signal, effect } from "@preact/signals";
import { callApi, log, showLoading, hideLoading } from "../shared/index.js";
import { oidc } from "./oidc.js";

const loginBtn = document.getElementById("login");
const logoutBtn = document.getElementById("logout");
const userInfoBtn = document.getElementById("userinfo");
const callApiBtn = document.getElementById("callapi");
const switchBtn = document.getElementById("switch");

const state = {
    isAuthenticated: signal(false),
    isLoading: signal(false),
    userInfo: signal(null),
};

async function checkSSO() {
    console.log("Checking SSO...");
    // 1) local cache
    console.log("Checking local cache...");
    const cached = await oidc.getUser();
    if (cached && !cached.expired) {
        console.log("Using cached user:", cached);
        log("out", "Using cached user: " + JSON.stringify(cached, null, 2));
        return true;
    }

    // 2) silent (prompt=none) → sukses jika masih ada SSO cookie di keycloak
    try {
        console.log("Trying silent signin...");
        const u = await oidc.signinSilent();
        console.log("Silent signin success:", u);
        log("out", "Silent signin success: " + JSON.stringify(u, null, 2));
        return !!u && !u.expired;
    } catch {
        return false;
    }
}

async function bootstrapAuth() {
    console.log("Bootstrapping auth...");
    state.isLoading.value = true;
    try {
        // init handlers
        loginBtn.onclick = () => oidc.signinRedirect(); // sama dg kc.login()
        switchBtn.onclick = () => {
            const go = encodeURIComponent("/cpds/");
            window.location.href = `/cpds/#switcher`;     // sama seperti contohmu
        };

        // “check-sso” dulu
        const ok = await checkSSO();
        state.isAuthenticated.value = ok;

        if (!ok) {
            console.log("Not authenticated.");
            log("out", "Not authenticated.");
            // jika datang dengan hash #switcher → paksa login (login-required)
            if (window.location.hash.includes("switcher")) {
                console.log("Switch detected, forcing login...");
                await oidc.signinRedirect();
                return;
            }
        } else {
            console.log("Authenticated.");
            log("out", "Authenticated.");
        }

        // token lifecycle (mirip kc.updateToken)
        oidc.events.addAccessTokenExpiring(async () => {
            console.log("Access token expiring...");
            try {
                await oidc.signinSilent(); log("out", "token expiring → silent renew");
                log("out", "silent renew success");
            } catch (error) {
                log("out", "silent renew error: " + error);
            }
        });
        oidc.events.addAccessTokenExpired(async () => {
            console.log("Access token expired");
            // coba renew; jika gagal, status jadi logged out
            try {
                await oidc.signinSilent(); 
                log("out", "token expired → silent renew");
            } catch (error) {
                log("out", "silent renew error: " + error);
                state.isAuthenticated.value = false;
            }
        });
        oidc.events.addUserSignedOut(async () => {
            console.log("User signed out of SSO");
            // sesi di server berakhir
            await oidc.removeUser();
            state.isAuthenticated.value = false;
        });

    } catch (e) {
        console.error(e);
        log("out", "Init error: " + e);
        state.isAuthenticated.value = false;
    } finally {
        state.isLoading.value = false;
    }
}

effect(() => {
    // ui toggle
    const authed = state.isAuthenticated.value;
    loginBtn.style.display = authed ? "none" : "inline-block";
    logoutBtn.style.display = authed ? "inline-block" : "none";
    userInfoBtn.style.display = authed ? "inline-block" : "none";
    callApiBtn.style.display = authed ? "inline-block" : "none";

    if (authed) {
        if (!logoutBtn.onclick) {
            logoutBtn.onclick = async () => {
                try {
                    await oidc.signoutRedirect({ post_logout_redirect_uri: `${window.location.origin}/aceas/` });
                } finally {
                    await oidc.removeUser(); // local cleanup
                    state.isAuthenticated.value = false;
                }
            };
        }

        if (!userInfoBtn.onclick) {
            userInfoBtn.onclick = async () => {
                const u = await oidc.getUser();
                if (!u) 
                    return log("out", "Not logged in!");
                const res = await fetch(
                    "http://eservice.localhost/auth/realms/agency-realm/protocol/openid-connect/userinfo",
                    { headers: { Authorization: `Bearer ${u.access_token}` } }
                );
                const info = await res.json();
                state.userInfo.value = info;
                log("out", "userinfo: " + JSON.stringify(info, null, 2));

                const ping = await fetch(
                    "http://eservice.localhost/auth/realms/agency-realm/demo/ping",
                    { headers: { Authorization: `Bearer ${u.access_token}` } }
                );
                log("out", "ping: " + JSON.stringify(await ping.json(), null, 2));
            };
        }

        if (!callApiBtn.onclick) {
            callApiBtn.onclick = async () => {
                const u = await oidc.getUser();
                if (!u) 
                    return log("out", "Login first");
                const r = await callApi("/aceas/api/hello", u.access_token);
                log("out", `ACEAS API [${r.status}]:\n${r.body}`);
            };
        }
    }
});

effect(() => state.isLoading.value ? showLoading() : hideLoading());

bootstrapAuth();
