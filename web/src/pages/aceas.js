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

// Handler functions for authentication actions
async function handleLogin() {
    try {
        enhancedLog("🔐 Initiating ACEAS login...", 'info');
        animationController.triggerButtonAnimation(loginBtn);
        await oidc.signinRedirect();
    } catch (error) {
        enhancedLog(`❌ Login failed: ${error.message}`, 'error');
    }
}

async function handleLogout() {
    try {
        enhancedLog("🚪 Initiating logout...", 'info');
        animationController.triggerButtonAnimation(logoutBtn);
        await oidc.signoutRedirect({ 
            post_logout_redirect_uri: `${window.location.origin}/aceas/` 
        });
        await oidc.removeUser();
        state.isAuthenticated.value = false;
        enhancedLog("✅ Logout completed", 'success');
    } catch (error) {
        enhancedLog(`❌ Logout failed: ${error.message}`, 'error');
    }
}

async function handleUserInfo() {
    try {
        const u = await oidc.getUser();
        if (!u) {
            enhancedLog("❌ Not logged in!", 'error');
            return;
        }
        
        enhancedLog("👤 Fetching user information from Keycloak...", 'info');
        animationController.triggerButtonAnimation(userInfoBtn);
        
        const res = await fetch(
            "http://eservice.localhost/auth/realms/agency-realm/protocol/openid-connect/userinfo",
            { headers: { Authorization: `Bearer ${u.access_token}` } }
        );
        const info = await res.json();
        state.userInfo.value = info;
        enhancedLog("✅ Keycloak userinfo retrieved:", 'success');
        enhancedLog(`📋 User Info:\n${JSON.stringify(info, null, 2)}`, 'info');
        
    } catch (error) {
        enhancedLog(`❌ Failed to get user info: ${error.message}`, 'error');
    }
}

async function handleCallApi() {
    try {
        const u = await oidc.getUser();
        if (!u) {
            enhancedLog("❌ Login first", 'error');
            return;
        }
        
        enhancedLog("🌐 Calling ACEAS API...", 'info');
        animationController.triggerButtonAnimation(callApiBtn);
        const r = await callApi("/aceas/api/hello", u.access_token);
        enhancedLog("✅ ACEAS API response received:", 'success');
        enhancedLog(`📊 ACEAS API [${r.status}]:\n${r.body}`, 'info');
        
    } catch (error) {
        enhancedLog(`❌ ACEAS API call failed: ${error.message}`, 'error');
    }
}

function handleSwitch() {
    enhancedLog("🔄 Switching to CPDS platform...", 'info');
    animationController.triggerButtonAnimation(switchBtn);
    window.location.href = '/cpds/#switcher';
}

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
    console.log("🧹 Clear console function called");
    const consoleOutput = document.getElementById('out');
    if (consoleOutput) {
        consoleOutput.innerHTML = `
<span class="console-welcome">🔐 Welcome to ACEAS Authentication Platform</span>
<span class="console-info">🚀 Enterprise SSO ready for interaction...</span>
<span class="console-timestamp">[${new Date().toLocaleTimeString()}] Console cleared</span>
        `.trim();
        console.log("✅ Console content cleared");
        animationController.showToast('Console cleared successfully!');
    } else {
        console.error("❌ Console output element not found");
    }
}

// Copy console content with animation
function copyConsoleContent() {
    console.log("📋 Copy console function called");
    const consoleOutput = document.getElementById('out');
    if (consoleOutput) {
        const textContent = consoleOutput.textContent;
        navigator.clipboard.writeText(textContent).then(() => {
            console.log("✅ Console content copied to clipboard");
            animationController.showToast('Console content copied to clipboard!');
        }).catch((error) => {
            console.error("❌ Failed to copy content:", error);
            animationController.showToast('Failed to copy console content', 'error');
        });
    } else {
        console.error("❌ Console output element not found");
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
    if (authStatus) {
        authStatus.style.transform = 'scale(1.1)';
        setTimeout(() => {
            authStatus.style.transform = '';
        }, 200);
    }
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
            element.style.display = 'inline-flex'; // Ensure proper display
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
                element.style.display = 'none'; // Properly hide
            }, 300);
        }
    });
}

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

// Loading state helper
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
            enhancedLog(`📋 Cached User: ${JSON.stringify(cached.profile, null, 2)}`, "info");
            updateAuthStatus(true, cached.profile);
            return true;
        }

        // 2) Try silent signin (prompt=none)
        setLoadingState("Verifying SSO session...");
        const user = await oidc.signinSilent();
        if (user && !user.expired) {
            enhancedLog("✅ Silent signin successful - ACEAS SSO session active", "success");
            enhancedLog(`📋 Silent User: ${JSON.stringify(user.profile, null, 2)}`, "info");
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

// Simple theme toggle
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

async function bootstrapAuth() {
    console.log("🚀 Bootstrapping ACEAS authentication system...");
    state.isLoading.value = true;
    
    try {
        // Check for existing authentication
        const isAuthenticated = await checkSSO();
        state.isAuthenticated.value = isAuthenticated;
        
        // Handle switcher hash (from working version)
        if (!isAuthenticated && window.location.hash.includes("switcher")) {
            await oidc.signinRedirect();
            return;
        }
        
        // Token lifecycle management (from working version)
        oidc.events.addAccessTokenExpiring(async () => {
            try {
                await oidc.signinSilent();
                enhancedLog("🔄 Token expiring → silent renew successful", "success");
            } catch (error) {
                enhancedLog(`⚠️ Silent renew error: ${error.message}`, "warning");
            }
        });
        
        oidc.events.addAccessTokenExpired(async () => {
            try {
                await oidc.signinSilent();
                enhancedLog("🔄 Token expired → silent renew successful", "success");
            } catch (error) {
                enhancedLog(`❌ Silent renew error: ${error.message}`, "error");
                state.isAuthenticated.value = false;
            }
        });
        
        oidc.events.addUserSignedOut(async () => {
            await oidc.removeUser();
            state.isAuthenticated.value = false;
            enhancedLog("👋 User signed out", "info");
        });
        
        enhancedLog("🎯 ACEAS authentication system initialized", "success");
        enhancedLog(`📊 Authentication status: ${isAuthenticated ? 'Active' : 'Inactive'}`, "info");
        
    } catch (error) {
        enhancedLog(`❌ Authentication bootstrap failed: ${error.message}`, "error");
        console.error("Bootstrap error:", error);
        state.isAuthenticated.value = false;
    } finally {
        state.isLoading.value = false;
        hideLoading();
    }
}

// Setup enhanced event handlers with animations
function setupEventHandlers() {
    console.log("🔧 Setting up event handlers...");
    
    // Debug: Check if elements exist
    console.log("DOM Elements:", {
        loginBtn: !!loginBtn,
        logoutBtn: !!logoutBtn,
        userInfoBtn: !!userInfoBtn,
        callApiBtn: !!callApiBtn,
        switchBtn: !!switchBtn
    });
    
    // Authentication buttons
    if (loginBtn) {
        loginBtn.addEventListener("click", handleLogin);
        console.log("✅ Login button event handler attached");
    }
    if (logoutBtn) {
        logoutBtn.addEventListener("click", handleLogout);
        console.log("✅ Logout button event handler attached");
    }
    if (userInfoBtn) {
        userInfoBtn.addEventListener("click", handleUserInfo);
        console.log("✅ UserInfo button event handler attached");
    }
    if (callApiBtn) {
        callApiBtn.addEventListener("click", handleCallApi);
        console.log("✅ CallAPI button event handler attached");
    }
    if (switchBtn) {
        switchBtn.addEventListener("click", handleSwitch);
        console.log("✅ Switch button event handler attached");
    }
    
    // Console controls
    if (clearConsoleBtn) {
        clearConsoleBtn.addEventListener("click", clearConsole);
        console.log("✅ Clear console button event handler attached");
    } else {
        console.warn("⚠️ Clear console button not found");
    }
    
    if (copyConsoleBtn) {
        copyConsoleBtn.addEventListener("click", copyConsoleContent);
        console.log("✅ Copy console button event handler attached");
    } else {
        console.warn("⚠️ Copy console button not found");
    }
    
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
    
    console.log("🎯 All event handlers setup complete");
}

// State effects with enhanced animations
effect(() => {
    const isAuthenticated = state.isAuthenticated.value;
    console.log(`🔄 Authentication state changed: ${isAuthenticated}`);
    
    // Simple UI toggle like the working version but using CSS classes
    if (isAuthenticated) {
        loginBtn.classList.add('hidden');
        logoutBtn.classList.remove('hidden');
        userInfoBtn.classList.remove('hidden');
        callApiBtn.classList.remove('hidden');
    } else {
        loginBtn.classList.remove('hidden');
        logoutBtn.classList.add('hidden');
        userInfoBtn.classList.add('hidden');
        callApiBtn.classList.add('hidden');
    }
    
    updateAuthStatus(isAuthenticated, state.userInfo.value);
    
    if (isAuthenticated) {
        enhancedLog("🎉 User successfully authenticated in ACEAS", "success");
        animationController.showToast("Welcome to ACEAS! Authentication successful.", "success");
        
        // Handlers are now properly set up via addEventListener in setupEventHandlers
    } else {
        enhancedLog("👤 User not authenticated - showing login options", "info");
    }
});

effect(() => {
    const isLoading = state.isLoading.value;
    if (isLoading) {
        enhancedLog("⏳ System processing...", "info");
    }
});

// Initialize everything when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    console.log("🔧 Initializing ACEAS Enterprise Authentication Platform...");
    
    // Initialize theme
    initializeTheme();
    
    // Setup event handlers first
    setupEventHandlers();
    
    // Initialize animation system
    setTimeout(() => {
        animationController.init();
    }, 100);
    
    // Enhance loading overlay with particle effects
    setTimeout(() => {
        if (typeof window.InteractiveEffects !== 'undefined') {
            window.InteractiveEffects.enhanceLoadingOverlay();
        }
    }, 200);
    
    // Add test function for enhanced loading (remove in production)
    window.testEnhancedLoading = function() {
        setLoadingState('Testing Enhanced 3D Loading...');
        setTimeout(() => {
            hideLoading();
        }, 4000);
    };
    
    // Add test function for 3D scroll effects (remove in production)
    window.test3DScrollEffects = function() {
        const effects = [
            'scroll-fade-in', 'scroll-slide-left', 'scroll-slide-right', 'scroll-scale-up',
            'scroll-3d-flip', 'scroll-3d-rotate', 'scroll-3d-bounce', 'scroll-3d-fold',
            'scroll-3d-depth', 'scroll-3d-parallax', 'scroll-3d-perspective'
        ];
        
        console.log('🎭 Testing 3D Scroll Effects...');
        effects.forEach(effect => {
            const elements = document.querySelectorAll(`.${effect}`);
            console.log(`${effect}: ${elements.length} elements found`);
        });
        console.log('💡 Scroll down to see the 3D effects in action!');
    };
    
    // Test function for true 3D perspective scaling
    window.testPerspectiveScaling = function() {
        console.log('🌟 Testing True 3D Perspective Scaling...');
        
        const perspectiveElements = document.querySelectorAll('.scroll-3d-perspective');
        console.log(`Found ${perspectiveElements.length} perspective elements`);
        
        perspectiveElements.forEach((el, i) => {
            const rect = el.getBoundingClientRect();
            const centerX = window.innerWidth / 2;
            const centerY = window.innerHeight / 2;
            const elementCenterX = rect.left + rect.width / 2;
            const elementCenterY = rect.top + rect.height / 2;
            
            console.log(`Element ${i + 1}:`, {
                position: { x: Math.round(elementCenterX), y: Math.round(elementCenterY) },
                distanceFromCenter: Math.round(Math.sqrt(
                    Math.pow(elementCenterX - centerX, 2) + 
                    Math.pow(elementCenterY - centerY, 2)
                )),
                currentTransform: el.style.transform || 'none'
            });
        });
        
        console.log('✅ Perspective scaling analysis completed');
        console.log('💡 Scroll or resize to see real-time perspective changes!');
    };
    
    // Initialize authentication
    setTimeout(() => {
        bootstrapAuth();
    }, 500);
    
    // Add initial welcome message and hide loading
    setTimeout(() => {
        enhancedLog("🏛️ ACEAS - Agency Corporate E-Authentication Service", "info");
        enhancedLog("🔐 Enterprise-grade authentication powered by Keycloak", "info");
        enhancedLog("🌟 Modern SaaS platform ready for interaction", "success");
        enhancedLog("💡 Tip: Use Ctrl+L to login, Ctrl+K to clear console, Ctrl+U for user info", "info");
        
        // Hide loading overlay after initialization
        hideLoading();
    }, 2500);
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

console.log("🎯 ACEAS authentication system fully loaded and ready!");