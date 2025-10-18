import 'htmx.org';
import './common.js';
import { callApi, log, showLoading, hideLoading } from "../shared";

// Authentication check function
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

// Custom log function for the new console design
function logToConsole(elementId, message) {
    const timestamp = new Date().toLocaleTimeString();
    const logLine = `[${timestamp}] ${message}`;
    console.log(logLine);
    
    const consoleElement = document.getElementById(elementId);
    if (consoleElement) {
        // Check if console has welcome message and clear it
        const hasWelcome = consoleElement.querySelector('.console-welcome');
        if (hasWelcome) {
            consoleElement.innerHTML = '';
        }
        
        // Add the message with proper formatting
        const logDiv = document.createElement('div');
        logDiv.className = 'console-log-line';
        logDiv.innerHTML = `<span class="console-timestamp">${timestamp}</span> ${message}`;
        consoleElement.appendChild(logDiv);
        consoleElement.scrollTop = consoleElement.scrollHeight;
    }
}

// Update button visibility and auth status
function updateButtonVisibility(isAuthenticated) {
    const loginBtn = document.getElementById('login');
    const logoutBtn = document.getElementById('logout');
    const userinfoBtn = document.getElementById('userinfo');
    const callapiBtn = document.getElementById('callapi');
    const switchBtn = document.getElementById('switch');

    if (isAuthenticated) {
        loginBtn?.classList.add('hidden');
        logoutBtn?.classList.remove('hidden');
        userinfoBtn?.classList.remove('hidden');
        callapiBtn?.classList.remove('hidden');
    } else {
        loginBtn?.classList.remove('hidden');
        logoutBtn?.classList.add('hidden');
        userinfoBtn?.classList.add('hidden');
        callapiBtn?.classList.add('hidden');
    }
    
    // Switch button is always visible
    switchBtn?.classList.remove('hidden');
    
    // Update auth status indicators
    const authStatus3d = document.getElementById('auth-status-3d');
    const statusDot = document.querySelector('.status-dot');
    const statusText = document.querySelector('.status-text');
    
    if (authStatus3d && statusText) {
        if (isAuthenticated) {
            authStatus3d.classList.remove('status-offline');
            authStatus3d.classList.add('status-online');
            statusText.textContent = 'Authenticated';
        } else {
            authStatus3d.classList.remove('status-online');
            authStatus3d.classList.add('status-offline');
            statusText.textContent = 'Not Authenticated';
        }
    }
    
    // Update the simple status dot too
    if (statusDot) {
        if (isAuthenticated) {
            statusDot.classList.add('online');
        } else {
            statusDot.classList.remove('online');
        }
    }
}

// Initialize application - main entry point
window.onload = async () => {
    // Ensure loading overlay is visible while we decide
    showLoading();
    let isNeedToHide = false;
    
    try {
        const isAuth = await authCheck();
        if (!isAuth) {
            logToConsole("out", "State: not authenticated, please login");
            updateButtonVisibility(false);
            
            if (window.location.hash.includes("switcher")) {
                console.log("Switching from ACEAS to CPDS");
                // Keep overlay visible while redirecting
                location.href = '/ids/auth/login';
            } else {
                isNeedToHide = true;
            }
            return;
        }

        logToConsole("out", "Authenticated successfully");
        updateButtonVisibility(true);
        
        // Set up periodic auth check
        var interval = setInterval(async () => {
            const isAuth = await authCheck();
            if (!isAuth) {
                logToConsole("out", "State: not authenticated, clearing interval");
                clearInterval(interval);
                updateButtonVisibility(false);
                return;
            }
        }, 60000); // Check every minute 
        
        isNeedToHide = true;
    } catch (e) {
        console.error(e);
        logToConsole("out", "Init error: " + e);
    } finally {
        // Hide loading overlay after auth check
        if (isNeedToHide) {
            hideLoading();
        }
    }
}

// Button event handlers
document.addEventListener('DOMContentLoaded', function() {
    // Enhance loading overlay with particle effects
    setTimeout(() => {
        if (typeof window.InteractiveEffects !== 'undefined') {
            window.InteractiveEffects.enhanceLoadingOverlay();
        }
    }, 200);

    // Login button handler
    const loginBtn = document.getElementById("login");
    if (loginBtn) {
        loginBtn.onclick = () => {
            showLoading();
            location.href = '/ids/auth/login';
        };
    }

    // Logout button handler
    const logoutBtn = document.getElementById("logout");
    if (logoutBtn) {
        logoutBtn.onclick = () => {
            showLoading();
            location.href = '/ids/auth/logout';
        };
    }

    // User info button handler
    const userinfoBtn = document.getElementById("userinfo");
    if (userinfoBtn) {
        userinfoBtn.onclick = async () => {
            const isAuthenticated = window.localStorage.getItem("is_authenticated") === "true" && window.localStorage.getItem("access_token");
            if (!isAuthenticated) return logToConsole("out", "Not logged in");

            const res = await fetch('/ids/me', {
                credentials: 'include',
                headers: { Authorization: "Bearer " + window.localStorage.getItem("access_token") }
            });

            logToConsole("out", "userinfo: " + JSON.stringify(await res.json(), null, 2));
        };
    }

    // Call API button handler
    const callapiBtn = document.getElementById("callapi");
    if (callapiBtn) {
        callapiBtn.onclick = async () => {
            const isAuthenticated = window.localStorage.getItem("is_authenticated") === "true" && window.localStorage.getItem("access_token");
            if (!isAuthenticated) return logToConsole("out", "Not logged in");
            
            const r = await callApi("/cpds/api/hello", window.localStorage.getItem("access_token"));
            logToConsole("out", `CPDS API [${r.status}]:\n${r.body}`);
        };
    }

    // Switch to ACEAS button handler
    const switchBtn = document.getElementById("switch");
    if (switchBtn) {
        switchBtn.onclick = () => {
            window.location.href = `/aceas/#switcher`;
        };
    }

    // Clear console button handler
    const clearBtn = document.getElementById("clear-console");
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            const consoleOutput = document.getElementById('out');
            if (consoleOutput) {
                consoleOutput.innerHTML = `
                    <span class="console-welcome">👥 Welcome to CPDS Personnel Management Platform</span><br>
                    <span class="console-info">🚀 Government SSO ready for interaction...</span><br>
                    <span class="console-timestamp">[${new Date().toLocaleTimeString()}] Console cleared</span>
                `;
            }
        });
    }

    // Copy console button handler
    const copyBtn = document.getElementById("copy-console");
    if (copyBtn) {
        copyBtn.addEventListener('click', async () => {
            const consoleOutput = document.getElementById('out');
            if (consoleOutput) {
                try {
                    const text = consoleOutput.textContent || consoleOutput.innerText;
                    await navigator.clipboard.writeText(text);
                    logToConsole("out", "Console content copied to clipboard");
                } catch (error) {
                    console.error('Failed to copy console content:', error);
                    logToConsole("out", "Failed to copy console content: " + error.message);
                }
            }
        });
    }
});

// Utility functions
window.cpdsAuth = {
    clearConsole: () => {
        const consoleOutput = document.getElementById('out');
        if (consoleOutput) {
            consoleOutput.innerHTML = `
                <span class="console-welcome">👥 Welcome to CPDS Personnel Management Platform</span><br>
                <span class="console-info">🚀 Government SSO ready for interaction...</span><br>
                <span class="console-timestamp">[${new Date().toLocaleTimeString()}] Console cleared</span>
            `;
        }
    }
};

// Smooth scroll for navigation links
document.addEventListener('DOMContentLoaded', function() {
    // Setup smooth scrolling for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth'
                });
            }
        });
    });
    
    // Enhance loading overlay with particle effects
    setTimeout(() => {
        if (typeof window.InteractiveEffects !== 'undefined') {
            window.InteractiveEffects.enhanceLoadingOverlay();
        }
    }, 200);
    
    // Add test function for enhanced loading (remove in production)
    window.testEnhancedLoading = function() {
        import('../shared/util.js').then(({ showLoading, hideLoading }) => {
            showLoading('Testing Enhanced 3D Loading...');
            setTimeout(() => {
                hideLoading();
            }, 4000);
        });
    };
    
    // Add test function for 3D scroll effects (remove in production)
    window.test3DScrollEffects = function() {
        const effects = [
            'scroll-fade-in', 'scroll-slide-left', 'scroll-slide-right', 'scroll-scale-up',
            'scroll-3d-flip', 'scroll-3d-rotate', 'scroll-3d-bounce', 'scroll-3d-fold',
            'scroll-3d-depth', 'scroll-3d-parallax'
        ];
        
        console.log('🎭 Testing 3D Scroll Effects...');
        effects.forEach(effect => {
            const elements = document.querySelectorAll(`.${effect}`);
            console.log(`${effect}: ${elements.length} elements found`);
        });
        console.log('💡 Scroll down to see the 3D effects in action!');
    };
    
    console.log('🏗️ CPDS initialized successfully');
    console.log('💡 Try: window.testEnhancedLoading() to test the enhanced loading overlay');
    
    // Hide loading overlay after initialization
    setTimeout(() => {
        import('../shared/util.js').then(({ hideLoading }) => {
            hideLoading();
        });
    }, 2500);
});