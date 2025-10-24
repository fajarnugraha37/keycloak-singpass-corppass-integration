<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    
    <link rel="icon" type="image/svg+xml" href="${url.resourcesPath}/images/sso-icon.svg" />
    <meta name="description" content="Singpass Login - Space Theme | Digital Identity Platform" />
    <title>Singpass Login - Space Theme</title>

    <script type="module" crossorigin src="${url.resourcesPath}/kc/index.js"></script>
    <link rel="stylesheet" crossorigin href="${url.resourcesPath}/styles/index.css">
</head>
<body>
    <!-- Canvas for space background -->
    <canvas id="spaceCanvas"></canvas>

    <!-- Accent Elements -->
    <div class="accent-orb accent-orb-1"></div>
    <div class="accent-orb accent-orb-2"></div>
    <div class="accent-orb accent-dot-1"></div>
    <div class="accent-orb accent-dot-2"></div>

    <!-- Main Container -->
    <div class="container">
        <div class="card-wrapper" id="cardWrapper">
            <div class="card">
                <!-- Header -->
                <div class="header">
                    <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                        <div class="alert alert-${message.type}">
                            <span class="message-text">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>
                    <#nested "header">
                </div>

                <!-- Content -->
                <#nested "form">

                <#if displayInfo>
                    <#nested "info">
                </#if>

                <#if social.providers??>
                    <#nested "socialProviders">
                </#if>
            </div>
        </div>
    </div>
</body>
</html>
</#macro>