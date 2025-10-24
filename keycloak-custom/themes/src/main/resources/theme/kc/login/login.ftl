<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
        <!-- Header -->
        <div class="header">
            <h1>Please select the below option</h1>
            <h2>to log in to e-Services:</h2>
        </div>
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if realm.password>
                    <!-- Form Section -->
                    <form class="form-section" id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                        <#if !usernameHidden??>
                            <!-- Email Input -->
                            <div class="form-group">
                                <label for="username">Email Address</label>
                                <div class="input-wrapper">
                                    <input tabindex="1" id="username" 
                                           name="username" value="${(login.username!'')}"  
                                           type="email" placeholder="you@example.com" autofocus autocomplete="off"
                                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                                           required />
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <rect x="2" y="4" width="20" height="16" rx="2"></rect>
                                        <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"></path>
                                    </svg>
                                </div>
                                <#if messagesPerField.existsError('username','password')>
                                    <span class="error-message">
                                        ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                                    </span>
                                </#if>
                            </div>
                        </#if>

                        <!-- Password Input -->
                        <div class="form-group">
                            <label for="password">Password</label>
                            <div class="input-wrapper">
                                <input tabindex="2" id="password"
                                       name="password" type="password" placeholder="••••••••"
                                       autocomplete="off"
                                       aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                                       required />
                                <button type="button" class="password-toggle" id="passwordToggle">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                    </svg>
                                </button>
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="position: absolute; left: 12px;">
                                    <path d="M12 1C6.48 1 2 5.48 2 11s4.48 10 10 10 10-4.48 10-10S17.52 1 12 1zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/>
                                </svg>
                            </div>
                        </div>

                        <!-- Remember & Forgot -->
                        <div class="remember-forgot">
                            <#if realm.rememberMe && !usernameHidden??>
                                <label>
                                    <#if login.rememberMe??>
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked />
                                    <#else>
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" />
                                    </#if>
                                    <span>Remember me</span>
                                </label>
                            </#if>
                            <#if realm.resetPasswordAllowed>
                                <a tabindex="5" href="${url.loginResetCredentialsUrl}">Forgot password?</a>
                            </#if>
                        </div>

                        <!-- Submit Button -->
                        <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                            <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                            <input  tabindex="4" 
                                    class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" 
                                    name="login" 
                                    id="kc-login" 
                                    type="submit" 
                                    value="${msg("doLogIn")}"/>
                        </div>
                    </form>

                    <#if social.providers??>
                        <!-- Divider -->
                        <div class="divider">
                            <div class="divider-line"></div>
                            <span class="divider-text">Or</span>
                            <div class="divider-line"></div>
                        </div>

                        <!-- Social Providers Section -->
                        <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
                            <div class="singpass-grid">
                                <#list social.providers as provider>
                                    <div class="singpass-option">
                                        <#if provider.alias == 'mockpass-singpass'>
                                            <h3>${msg("individual")}</h3>
                                        <#elseif provider.alias == 'mockpass-corppass'>
                                            <h3>${msg("corporate")}</h3>
                                        <#else>
                                            <h3>${provider.displayName!}</h3>
                                        </#if>
                                        <a href="${provider.loginUrl}" id="social-${provider.alias}" 
                                           class="singpass-btn ${properties.kcFormSocialAccountListButtonClass!}" 
                                           type="button">
                                            <#if provider.iconClasses?has_content>
                                                <i class="${provider.iconClasses!}" aria-hidden="true"></i>
                                            <#else>
                                                <svg viewBox="0 0 24 24" fill="currentColor">
                                                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/>
                                                </svg>
                                            </#if>
                                            <span>Log In with ${provider.displayName}</span>
                                        </a>
                                    </div>
                                </#list>
                            </div>
                        </div>
                    </#if>
                </#if>
            </div>
        </div>
    <#elseif section = "info">
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <!-- Footer -->
            <p class="footer">
                Don't have an account? <a tabindex="6" href="${url.registrationUrl}">Sign up</a>
            </p>
        </#if>
    </#if>
</@layout.registrationLayout>