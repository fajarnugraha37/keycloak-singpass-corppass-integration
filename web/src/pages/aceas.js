import 'htmx.org';
import './common.js';
import { signal, effect } from "@preact/signals";
import { callApi, log, showLoading, hideLoading } from "../shared/index.js";
import { oidc } from "./oidc.js";

// DOM elements
const loginBtn = document.getElementById("login");
const logoutBtn = document.getElementById("logout");
const userInfoBtn = document.getElementById("userinfo");
const callApiBtn = document.getElementById("callapi");
const switchBtn = document.getElementById("switch");
const authStatus = document.getElementById("auth-status");
const clearConsoleBtn = document.getElementById("clear-console");

// State management
const state = {
    isAuthenticated: signal(false),
    isLoading: signal(false),
    userInfo: signal(null),
};

// Enhanced logging with timestamps and styling
function enhancedLog(message, type = 'info') {
    const timestamp = new Date().toLocaleTimeString();
    const prefix = `[${timestamp}] [${type.toUpperCase()}]`;
    const styledMessage = `${prefix} ${message}`;
    log("out", styledMessage);
    console.log(styledMessage);
}

// Clear console functionality
function clearConsole() {
    const consoleOutput = document.getElementById('out');
    if (consoleOutput) {
        consoleOutput.textContent = 'Console cleared.\nWaiting for user interaction...';
    }
}

// Update authentication status indicator
function updateAuthStatus(isAuthenticated, userInfo = null) {
    if (!authStatus) return;
    
    const statusDot = authStatus.querySelector('div');
    const statusText = authStatus.querySelector('span');
    
    if (isAuthenticated) {
        authStatus.className = 'status-indicator status-online';
        if (statusDot) statusDot.className = 'w-2 h-2 bg-green-500 rounded-full animate-pulse';
        if (statusText) statusText.textContent = userInfo ? `${userInfo.preferred_username || 'User'}` : 'Authenticated';
    } else {
        authStatus.className = 'status-indicator status-offline';
        if (statusDot) statusDot.className = 'w-2 h-2 bg-red-500 rounded-full';
        if (statusText) statusText.textContent = 'Not Authenticated';
    }
}

// Enhanced loading states
function setLoadingState(message) {
    const loadingOverlay = document.getElementById('loading-overlay');
    const loadingMessage = loadingOverlay?.querySelector('.loading-message');
    if (loadingMessage) {
        loadingMessage.textContent = message;
    }
    showLoading();
}

async function checkSSO() {
    enhancedLog("Checking SSO status...", "info");
    setLoadingState("Checking local cache...");
    
    try {
        // 1) Check local cache first
        const cached = await oidc.getUser();
        if (cached && !cached.expired) {
            enhancedLog("Using cached user session", "success");
            updateAuthStatus(true, cached.profile);
            return true;
        }

        // 2) Try silent signin (prompt=none)
        setLoadingState("Checking SSO session...");
        const user = await oidc.signinSilent();
        if (user && !user.expired) {
            enhancedLog("Silent signin successful - SSO session active", "success");
            updateAuthStatus(true, user.profile);
            return true;
        }
    } catch (error) {
        enhancedLog(`SSO check failed: ${error.message}`, "warn");
    }
    
    enhancedLog("No active SSO session found", "info");
    updateAuthStatus(false);
    return false;
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
    // ui toggle - use consistent CSS classes
    const authed = state.isAuthenticated.value;
    if (authed) {
        loginBtn.classList.add("hidden");
        logoutBtn.classList.remove("hidden");
        userInfoBtn.classList.remove("hidden");
        callApiBtn.classList.remove("hidden");
    } else {
        loginBtn.classList.remove("hidden");
        logoutBtn.classList.add("hidden");
        userInfoBtn.classList.add("hidden");
        callApiBtn.classList.add("hidden");
    }

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

// Add clear console event listener
if (clearConsoleBtn) {
    clearConsoleBtn.addEventListener('click', clearConsole);
}

bootstrapAuth();
