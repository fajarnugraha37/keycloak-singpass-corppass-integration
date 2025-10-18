import './common.js';
import { callApi, log, showLoading, hideLoading } from "../shared";

async function authCheck() {
    const r = await fetch("/ids/auth/refresh", {
        method: "POST",
        credentials: "include"
    });
    if (!r.ok) {
        window.localStorage.removeItem("access_token");
        window.localStorage.removeItem("refresh_token");
        window.localStorage.setItem("is_authenticated", "false");

        return false;
    } else {
        const { access_token, refresh_token } = await r.json();
        window.localStorage.setItem("access_token", access_token);
        window.localStorage.setItem("refresh_token", refresh_token);
        window.localStorage.setItem("is_authenticated", "true");

        return true;
    }
}
window.onload = async () => {
    // ensure loading overlay is visible while we decide
    showLoading();
    let isNeedToHide = false;
    try {
        const isAuth = await authCheck();
        if (!isAuth) {
            log("out", `State: not authenticated, please login`);
            if (window.location.hash.includes("switcher")) {
                console.log("Switching");
                // keep overlay visible while redirecting
                location.href = '/ids/auth/login';
            } else {
                isNeedToHide = true; // prevent hiding loading overlay
            }

            return;
        }

        log("out", `Authenticated.`);
        var interval = setInterval(async () => {
            const isAuth = await authCheck();
            if (!isAuth) {
                log("out", `State: not authenticated, clearing interval`);
                clearInterval(interval);
                return;
            }
        }, 60000); // Check every minute 
        
        isNeedToHide = true; // prevent hiding loading overlay
    } catch (e) {
        console.error(e);
        log("out", "Init error: " + e);
    } finally {
        // hide loading overlay after auth check
        if (isNeedToHide) {
            hideLoading();
        }
    }
}

document.getElementById("login").onclick = () => {
    // show loading while redirecting to login
    showLoading();
    location.href = '/ids/auth/login';
};
document.getElementById("logout").onclick = () => {
    showLoading();
    location.href = '/ids/auth/logout';
};
document.getElementById("userinfo").onclick = async () => {
    const isAuthenticated = window.localStorage.getItem("is_authenticated") === "true" && window.localStorage.getItem("access_token");
    if (!isAuthenticated) return log("out", "Not logged in");

    const res = await fetch('/ids/me', {
        credentials: 'include',
        headers: { Authorization: "Bearer " + window.localStorage.getItem("access_token") }
    });

    log("out", "userinfo: " + JSON.stringify(await res.json(), null, 2));
};
document.getElementById("callapi").onclick = async () => {
    const isAuthenticated = window.localStorage.getItem("is_authenticated") === "true" && window.localStorage.getItem("access_token");
    if (!isAuthenticated) return log("out", "Not logged in");
    const r = await callApi("/cpds/api/hello", window.localStorage.getItem("access_token"));
    log("out", `CPDS API [${r.status}]:\n${r.body}`);
};
document.getElementById("switch").onclick = () => {
    const go = encodeURIComponent("/aceas/");
    window.location.href = `/aceas/#switcher`;
};