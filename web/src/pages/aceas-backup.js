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
const authStatus = document.getElementById("auth-status-3d");
const clearConsoleBtn = document.getElementById("clear-console");
const copyConsoleBtn = document.getElementById("copy-console");
const themeToggle = document.getElementById("theme-toggle-3d");

// State management
const state = {
    isAuthenticated: signal(false),
    isLoading: signal(false),
    userInfo: signal(null),
};

// Enhanced 3D Animation System
class AceaAnimationController {
    constructor() {
        this.isInitialized = false;
        this.animationQueue = [];
        this.observers = new Map();
    }

    init() {
        if (this.isInitialized) return;
        
        this.initializeScrollAnimations();
        this.initializeParallaxEffects();
        this.initializeMouseTracking();
        this.initializeLoadingAnimations();
        this.initializeConsoleEffects();
        
        this.isInitialized = true;
    }

    initializeScrollAnimations() {
        const observerOptions = {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        };

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('animate');
                }
            });
        }, observerOptions);

        // Observe all animatable elements
        document.querySelectorAll('.fade-in-up, .fade-in-left, .fade-in-right, .fade-in-scale').forEach(el => {
            observer.observe(el);
        });

        this.observers.set('scroll', observer);
    }

    initializeParallaxEffects() {
        const parallaxElements = document.querySelectorAll('.hero-pattern, .hero-particles');
        
        window.addEventListener('scroll', () => {
            const scrolled = window.pageYOffset;
            const rate = scrolled * -0.5;
            
            parallaxElements.forEach((element, index) => {
                const offset = rate * (0.5 + index * 0.2);
                element.style.transform = `translateY(${offset}px)`;
            });
        });
    }

    initializeMouseTracking() {
        const cards = document.querySelectorAll('.action-card, .info-card, .dashboard-preview');
        
        cards.forEach(card => {
            card.addEventListener('mousemove', (e) => {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                const centerX = rect.width / 2;
                const centerY = rect.height / 2;
                
                const rotateX = (y - centerY) / centerY * -10;
                const rotateY = (x - centerX) / centerX * 10;
                
                card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateZ(20px)`;
            });

            card.addEventListener('mouseleave', () => {
                card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateZ(0px)';
            });
        });
    }

    initializeLoadingAnimations() {
        const loadingOverlay = document.getElementById('loading-overlay');
        if (loadingOverlay) {
            setTimeout(() => {
                loadingOverlay.style.opacity = '0';
                loadingOverlay.style.transform = 'scale(0.9)';
                setTimeout(() => {
                    loadingOverlay.style.display = 'none';
                }, 500);
            }, 2000);
        }
    }

    initializeConsoleEffects() {
        const consoleWrapper = document.querySelector('.console-wrapper');
        if (consoleWrapper) {
            // Add pulse effect when new content is added
            const observer = new MutationObserver(() => {
                this.triggerConsolePulse();
            });
            
            observer.observe(document.getElementById('out'), {
                childList: true,
                subtree: true,
                characterData: true
            });
        }
    }

    triggerConsolePulse() {
        const consoleWrapper = document.querySelector('.console-wrapper');
        if (consoleWrapper) {
            consoleWrapper.classList.add('console-pulse');
            setTimeout(() => {
                consoleWrapper.classList.remove('console-pulse');
            }, 2000);
        }
    }

    triggerButtonAnimation(button, type = 'success') {
        if (!button) return;
        
        // Add animation classes
        button.classList.add('animate-press');
        button.style.transform = 'scale(0.95)';
        
        // Create ripple effect
        const ripple = document.createElement('div');
        ripple.className = 'button-ripple';
        button.appendChild(ripple);
        
        setTimeout(() => {
            button.style.transform = '';
            button.classList.remove('animate-press');
            if (ripple.parentNode) {
                ripple.parentNode.removeChild(ripple);
            }
        }, 300);
    }

    showToast(message, type = 'success') {
        const toast = document.getElementById('toast-3d');
        const messageElement = document.querySelector('.toast-message-3d');
        
        if (toast && messageElement) {
            messageElement.textContent = message;
            toast.classList.add('show');
            
            setTimeout(() => {
                toast.classList.remove('show');
            }, 3000);
        }
    }
}

// Initialize animation controller
const animationController = new AceaAnimationController();

// Enhanced logging with 3D console effects
function enhancedLog(message, type = 'info') {
    const timestamp = new Date().toLocaleTimeString();
    const prefix = `[${timestamp}]`;
    
    let styledMessage;
    switch (type) {
        case 'success':
            styledMessage = `<span class="console-success">${prefix} ✅ ${message}</span>`;
            break;
        case 'error':
            styledMessage = `<span class="console-error">${prefix} ❌ ${message}</span>`;
            break;
        case 'info':
            styledMessage = `<span class="console-info">${prefix} ℹ️  ${message}</span>`;
            break;
        case 'warning':
            styledMessage = `<span class="console-warning">${prefix} ⚠️  ${message}</span>`;
            break;
        default:
            styledMessage = `<span class="console-timestamp">${prefix}</span> ${message}`;
    }
    
    const consoleOutput = document.getElementById('out');
    if (consoleOutput) {
        consoleOutput.innerHTML += '\n' + styledMessage;
        consoleOutput.scrollTop = consoleOutput.scrollHeight;
        
        // Add highlight animation
        const newLine = consoleOutput.lastElementChild;
        if (newLine) {
            newLine.classList.add('api-result-highlight');
        }
    }
    
    console.log(`${prefix} [${type.toUpperCase()}] ${message}`);
    
    // Trigger console pulse animation
    animationController.triggerConsolePulse();
}

// Clear console functionality with 3D effects
function clearConsole() {
    const consoleOutput = document.getElementById('out');
    if (consoleOutput) {
        consoleOutput.innerHTML = `
<span class="console-welcome">🔐 Welcome to ACEAS Authentication Platform</span>
<span class="console-info">🚀 Enterprise SSO ready for interaction...</span>
<span class="console-timestamp">[${new Date().toLocaleTimeString()}] Console cleared</span>
        `.trim();
    }
    animationController.showToast('Console cleared successfully!');
}

// Copy console content with animation
function copyConsoleContent() {
    const consoleOutput = document.getElementById('out');
    if (consoleOutput) {
        const textContent = consoleOutput.textContent;
        navigator.clipboard.writeText(textContent).then(() => {
            animationController.showToast('Console content copied to clipboard!');
        }).catch(() => {
            animationController.showToast('Failed to copy console content', 'error');
        });
    }
}

// Update authentication status indicator with 3D effects
function updateAuthStatus(isAuthenticated, userInfo = null) {
    if (!authStatus) return;
    
    const statusPulse = authStatus.querySelector('.status-pulse');
    const statusText = authStatus.querySelector('.status-text');
    
    if (isAuthenticated) {
        authStatus.className = 'status-indicator-3d status-online';
        if (statusPulse) statusPulse.style.background = '#10b981';
        if (statusText) statusText.textContent = userInfo ? `${userInfo.preferred_username || 'User'}` : 'Authenticated';
        
        // Update console status
        const consoleStatus = document.getElementById('console-status');
        if (consoleStatus) {
            const indicator = consoleStatus.querySelector('.status-indicator');
            const text = consoleStatus.querySelector('span');
            if (indicator) indicator.style.background = '#10b981';
            if (text) {
                text.textContent = 'Authenticated';
                text.style.color = '#10b981';
            }
        }
    } else {
        authStatus.className = 'status-indicator-3d status-offline';
        if (statusPulse) statusPulse.style.background = '#dc2626';
        if (statusText) statusText.textContent = 'Not Authenticated';
        
        // Update console status
        const consoleStatus = document.getElementById('console-status');
        if (consoleStatus) {
            const indicator = consoleStatus.querySelector('.status-indicator');
            const text = consoleStatus.querySelector('span');
            if (indicator) indicator.style.background = '#dc2626';
            if (text) {
                text.textContent = 'Ready';
                text.style.color = '#10b981';
            }
        }
    }
    
    // Animate status change
    authStatus.style.transform = 'scale(1.1)';
    setTimeout(() => {
        authStatus.style.transform = '';
    }, 200);
}

// Enhanced button visibility with 3D animations
function updateButtonVisibility(isAuthenticated) {
    const buttons = [
        { element: loginBtn, showWhen: !isAuthenticated },
        { element: logoutBtn, showWhen: isAuthenticated },
        { element: userInfoBtn, showWhen: isAuthenticated },
        { element: callApiBtn, showWhen: isAuthenticated },
    ];

    buttons.forEach(({ element, showWhen }) => {
        if (!element) return;
        
        if (showWhen) {
            element.classList.remove('hidden');
            element.style.opacity = '0';
            element.style.transform = 'translateY(20px) scale(0.9)';
            
            setTimeout(() => {
                element.style.transition = 'all 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55)';
                element.style.opacity = '1';
                element.style.transform = 'translateY(0) scale(1)';
            }, 100);
        } else {
            element.style.transition = 'all 0.3s ease';
            element.style.opacity = '0';
            element.style.transform = 'translateY(-20px) scale(0.9)';
            
            setTimeout(() => {
                element.classList.add('hidden');
            }, 300);
        }
    });
}

// Authentication functions with enhanced animations
async function handleLogin() {
    try {
        enhancedLog("🔐 Initiating ACEAS login process...", 'info');
        animationController.triggerButtonAnimation(loginBtn);
        
        showLoading("Redirecting to Keycloak...");
        
        const result = await oidc.login();
        enhancedLog("✅ Login successful! Keycloak authentication completed.", 'success');
        enhancedLog(`🎯 Session details: ${JSON.stringify(result, null, 2)}`, 'info');
        
        animationController.showToast('Login successful! Welcome to ACEAS.', 'success');
        
    } catch (error) {
        enhancedLog(`❌ Login failed: ${error.message}`, 'error');
        animationController.showToast('Login failed. Please try again.', 'error');
        console.error("Login error:", error);
    } finally {
        hideLoading();
    }
}

async function handleLogout() {
    try {
        enhancedLog("🚪 Initiating ACEAS logout process...", 'info');
        animationController.triggerButtonAnimation(logoutBtn);
        
        showLoading("Logging out...");
        
        await oidc.logout();
        enhancedLog("✅ Logout successful! Session terminated.", 'success');
        
        animationController.showToast('Logout successful. Session ended.', 'success');
        
    } catch (error) {
        enhancedLog(`❌ Logout failed: ${error.message}`, 'error');
        animationController.showToast('Logout failed. Please try again.', 'error');
        console.error("Logout error:", error);
    } finally {
        hideLoading();
    }
}

async function handleUserInfo() {
    try {
        enhancedLog("👤 Fetching user information from Keycloak...", 'info');
        animationController.triggerButtonAnimation(userInfoBtn);
        
        showLoading("Loading user profile...");
        
        const userInfo = await oidc.loadUserProfile();
        enhancedLog("✅ User information retrieved successfully:", 'success');
        enhancedLog(`📋 User Profile:\n${JSON.stringify(userInfo, null, 2)}`, 'info');
        
        state.userInfo.value = userInfo;
        animationController.showToast('User profile loaded successfully!', 'success');
        
    } catch (error) {
        enhancedLog(`❌ Failed to load user info: ${error.message}`, 'error');
        animationController.showToast('Failed to load user profile.', 'error');
        console.error("UserInfo error:", error);
    } finally {
        hideLoading();
    }
}

async function handleCallApi() {
    try {
        enhancedLog("🌐 Testing API call with ACEAS authentication...", 'info');
        animationController.triggerButtonAnimation(callApiBtn);
        
        showLoading("Calling protected API...");
        
        const response = await callApi();
        enhancedLog("✅ API call successful! Response received:", 'success');
        enhancedLog(`📊 API Response:\n${JSON.stringify(response, null, 2)}`, 'info');
        
        animationController.showToast('API call completed successfully!', 'success');
        
    } catch (error) {
        enhancedLog(`❌ API call failed: ${error.message}`, 'error');
        animationController.showToast('API call failed. Check authentication.', 'error');
        console.error("API call error:", error);
    } finally {
        hideLoading();
    }
}

function handleSwitch() {
    enhancedLog("🔄 Switching to CPDS platform...", 'info');
    animationController.triggerButtonAnimation(switchBtn);
    
    setTimeout(() => {
        enhancedLog("🎯 Redirecting to CPDS authentication system...", 'info');
        animationController.showToast('Switching to CPDS platform...', 'info');
        window.location.href = '/cpds/';
    }, 1000);
}
// Theme toggle functionality
function toggleTheme() {
    const body = document.body;
    const isDark = body.classList.contains('dark');
    
    if (isDark) {
        body.classList.remove('dark');
        localStorage.setItem('theme', 'light');
        animationController.showToast('Switched to light theme');
    } else {
        body.classList.add('dark');
        localStorage.setItem('theme', 'dark');
        animationController.showToast('Switched to dark theme');
    }
    
    enhancedLog(`🎨 Theme switched to ${isDark ? 'light' : 'dark'} mode`, 'info');
}

// Enhanced loading states
function setLoadingState(message) {
    const loadingOverlay = document.getElementById('loading-overlay');
    const loadingMessage = loadingOverlay?.querySelector('.loading-message-3d');
    if (loadingMessage) {
        loadingMessage.textContent = message;
    }
    showLoading();
}

async function checkSSO() {
    enhancedLog("🔍 Checking ACEAS SSO status...", "info");
    setLoadingState("Checking authentication cache...");
    
    try {
        // 1) Check local cache first
        const cached = await oidc.getUser();
        if (cached && !cached.expired) {
            enhancedLog("✅ Using cached ACEAS session", "success");
            updateAuthStatus(true, cached.profile);
            return true;
        }

        // 2) Try silent signin (prompt=none)
        setLoadingState("Verifying SSO session...");
        const user = await oidc.signinSilent();
        if (user && !user.expired) {
            enhancedLog("✅ Silent signin successful - ACEAS SSO session active", "success");
            updateAuthStatus(true, user.profile);
            return true;
        }
    } catch (error) {
        enhancedLog(`⚠️ ACEAS SSO check failed: ${error.message}`, "warning");
    }
    
    enhancedLog("ℹ️ No active ACEAS SSO session found", "info");
    updateAuthStatus(false);
    return false;
}

async function bootstrapAuth() {
    console.log("🚀 Bootstrapping ACEAS authentication system...");
    state.isLoading.value = true;
    
    try {
        // Initialize event handlers
        setupEventHandlers();
        
        // Check for existing authentication
        const isAuthenticated = await checkSSO();
        state.isAuthenticated.value = isAuthenticated;
        
        enhancedLog("🎯 ACEAS authentication system initialized", "success");
        enhancedLog(`📊 Authentication status: ${isAuthenticated ? 'Active' : 'Inactive'}`, "info");
        
    } catch (error) {
        enhancedLog(`❌ Authentication bootstrap failed: ${error.message}`, "error");
        console.error("Bootstrap error:", error);
    } finally {
        state.isLoading.value = false;
        hideLoading();
    }
}

// Setup enhanced event handlers with animations
function setupEventHandlers() {
    // Authentication buttons
    loginBtn?.addEventListener("click", handleLogin);
    logoutBtn?.addEventListener("click", handleLogout);
    userInfoBtn?.addEventListener("click", handleUserInfo);
    callApiBtn?.addEventListener("click", handleCallApi);
    switchBtn?.addEventListener("click", handleSwitch);
    
    // Console controls
    clearConsoleBtn?.addEventListener("click", clearConsole);
    copyConsoleBtn?.addEventListener("click", copyConsoleContent);
    
    // Theme toggle
    themeToggle?.addEventListener("click", toggleTheme);
    
    // Enhanced hover effects for action cards
    document.querySelectorAll('.action-card').forEach(card => {
        card.addEventListener('mouseenter', () => {
            card.style.transition = 'all 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55)';
        });
    });
    
    // Keyboard shortcuts
    document.addEventListener('keydown', (e) => {
        if (e.ctrlKey || e.metaKey) {
            switch (e.key) {
                case 'l':
                    e.preventDefault();
                    if (!state.isAuthenticated.value && loginBtn && !loginBtn.classList.contains('hidden')) {
                        handleLogin();
                    }
                    break;
                case 'k':
                    e.preventDefault();
                    clearConsole();
                    break;
                case 'u':
                    e.preventDefault();
                    if (state.isAuthenticated.value && userInfoBtn && !userInfoBtn.classList.contains('hidden')) {
                        handleUserInfo();
                    }
                    break;
            }
        }
    });
}

// State effects with enhanced animations
effect(() => {
    const isAuthenticated = state.isAuthenticated.value;
    updateButtonVisibility(isAuthenticated);
    updateAuthStatus(isAuthenticated, state.userInfo.value);
    
    if (isAuthenticated) {
        enhancedLog("🎉 User successfully authenticated in ACEAS", "success");
        animationController.showToast("Welcome to ACEAS! Authentication successful.", "success");
    }
});

effect(() => {
    const isLoading = state.isLoading.value;
    if (isLoading) {
        enhancedLog("⏳ System processing...", "info");
    }
});

// Initialize theme from localStorage
function initializeTheme() {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
        document.body.classList.add('dark');
    }
}

// Initialize everything when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    console.log("🔧 Initializing ACEAS Enterprise Authentication Platform...");
    
    // Initialize theme
    initializeTheme();
    
    // Initialize animation system
    setTimeout(() => {
        animationController.init();
    }, 100);
    
    // Initialize authentication
    setTimeout(() => {
        bootstrapAuth();
    }, 500);
    
    // Add initial welcome message
    setTimeout(() => {
        enhancedLog("🏛️ ACEAS - Agency Corporate E-Authentication Service", "info");
        enhancedLog("🔐 Enterprise-grade authentication powered by Keycloak", "info");
        enhancedLog("🌟 Modern SaaS platform ready for interaction", "success");
        enhancedLog("💡 Tip: Use Ctrl+L to login, Ctrl+K to clear console, Ctrl+U for user info", "info");
    }, 1000);
});

// Handle page visibility changes
document.addEventListener('visibilitychange', () => {
    if (!document.hidden && state.isAuthenticated.value) {
        // Check if session is still valid when page becomes visible
        checkSSO();
    }
});

// Export for testing purposes
window.aceaAuth = {
    state,
    login: handleLogin,
    logout: handleLogout,
    userInfo: handleUserInfo,
    callApi: handleCallApi,
    clearConsole,
    animationController
};
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
console.log("🎯 ACEAS authentication system fully loaded and ready!");

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

console.log("🎯 ACEAS authentication system fully loaded and ready!");
