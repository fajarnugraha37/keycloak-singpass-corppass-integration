import { oidc } from "./oidc.js";

console.log("Processing Silent callback...");
oidc.signinSilentCallback().then(() => {
    console.log("Silent callback successful, redirecting...");
    window.location.replace("/aceas/");
});
