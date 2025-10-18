import './common.js';
import Keycloak from 'keycloak-js';
import { signal, effect } from '@preact/signals';
import { callApi, log, showLoading, hideLoading } from "../shared/index.js";

const loginBtn = document.getElementById("login");
const logoutBtn = document.getElementById("logout");
const userInfoBtn = document.getElementById("userinfo");
const callApiBtn = document.getElementById("callapi");
const switchBtn = document.getElementById("switch");

const state = (function() {
    let st = {
        kc: new Keycloak({
            url: "http://eservice.localhost/auth",
            realm: "agency-realm",
            clientId: "aceas-spa",
        }),
        isAuthenticated: signal(false),
        isLoading: signal(0),
        userInfo: signal(null),
    };

    return {
        ...st,
        kcInit: async () => {
            try {
                return await st.kc.init({
                    onLoad: 'check-sso',
                    checkLoginIframe: false,
                    pkceMethod: 'S256',
                    flow: 'standard',
                    redirectUri: window.location.href,
                });
            } catch (e) {
                st.kc = new Keycloak({
                    url: "http://eservice.localhost/auth",
                    realm: "agency-realm",
                    clientId: "aceas-spa",
                });
                return await st.kc.init({
                    onLoad: 'login-required',
                    checkLoginIframe: false,
                    pkceMethod: 'S256',
                    flow: 'standard',
                    redirectUri: window.location.href,
                });
            } finally {
                st.kc.onTokenExpired = async () => {
                    try {
                        await st.kc.updateToken(30);
                        log("out", "Token refreshed");
                    } catch (e) {
                        log("out", "Token refresh failed: " + e);
                    }
                };
            }
        },
    };
})();

effect(() => {
    console.log('is authencticated changes: ', state.isAuthenticated.value);
    if (state.isAuthenticated.value === true) {
        loginBtn.style.display = "none";
        logoutBtn.style.display = "inline-block";
        userInfoBtn.style.display = "inline-block";
        callApiBtn.style.display = "inline-block";

        !logoutBtn.onclick && (logoutBtn.onclick = () => {
            state.kc.logout({ redirectUri: window.location.origin + "/aceas/" });
        });
        !userInfoBtn.onclick && (userInfoBtn.onclick = async () => {
            if (!state.kc.authenticated)
                return log("out", "Not logged in");
            const res = await fetch(
                "http://eservice.localhost/auth/realms/agency-realm/protocol/openid-connect/userinfo",
                { headers: { Authorization: "Bearer " + state.kc.token } }
            );
            const userInfo = await res.json();

            state.userInfo.value = userInfo;
            log("out", "userinfo: " + JSON.stringify(userInfo, null, 2));
        });
        !callApiBtn.onclick && (callApiBtn.onclick = async () => {
            if (!state.kc.authenticated)
                return log("out", "Login first");
            const r = await callApi("/aceas/api/hello", state.kc.token);
            log("out", `ACEAS API [${r.status}]:\n${r.body}`);
        });
    } else {
        loginBtn.style.display = "inline-block";
        logoutBtn.style.display = "none";
        userInfoBtn.style.display = "none";
        callApiBtn.style.display = "none";
    }
});
effect(() => {
    if (state.isLoading.value == true) {
        showLoading();
    } else {
        hideLoading();
    }
});

main();
async function main() {
    console.log('init main');
    try {
        state.isLoading.value = true;

        loginBtn.onclick = () => {
            console.log("Logging in...");
            state.kc.login();
        };
        switchBtn.onclick = () => {
            const go = encodeURIComponent("/cpds/");
            window.location.href = `/cpds/#switcher`;
        };

        
        const auth = await state.kcInit();
        state.isAuthenticated.value = state.kc.authenticated ?? auth;
        if (state.isAuthenticated.value) {
            log("out", "Authenticated.");
        } else {
            log("out", "Not authenticated.");
            if (window.location.hash.includes("switcher")) {
                console.log("Switching");
                state.kc.login();
            }
        }
    } catch (e) {
        state.isAuthenticated.value = false;
        console.log(e);
        log("out", "Init error: " + e);
    } finally {
        state.isLoading.value = false;
    }
}