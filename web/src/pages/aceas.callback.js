import { oidc } from "./oidc.js";

if (new URLSearchParams(window.location.search)?.get("type")?.toLocaleLowerCase() === "silent") {
    console.log("Processing Silent callback...");
    oidc.signinSilentCallback().then(() => {
        console.log("Silent callback successful, redirecting...");
        window.location.replace("/aceas/");
    });
} else {
    console.log("Processing callback...");
    oidc.signinCallback().then(() => {
        console.log("Callback successful, redirecting...");
        window.location.replace("/aceas/");
    });
}


