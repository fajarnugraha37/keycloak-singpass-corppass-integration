// Error page interactive features
class ErrorPageEnhancer {
    constructor() {
        this.init();
    }

    init() {
        this.addKeyboardShortcuts();
        this.addVisualEnhancements();
        this.addAccessibilityFeatures();
        this.trackErrors();
    }

    addKeyboardShortcuts() {
        document.addEventListener('keydown', (e) => {
            // Press 'H' to go home
            if (e.key === 'h' || e.key === 'H') {
                window.location.href = '/';
            }
            
            // Press 'B' to go back
            if (e.key === 'b' || e.key === 'B') {
                history.back();
            }
            
            // Press 'R' to reload
            if (e.key === 'r' || e.key === 'R') {
                location.reload();
            }
            
            // Press Escape to show help
            if (e.key === 'Escape') {
                this.showKeyboardHelp();
            }
        });
    }

    addVisualEnhancements() {
        // Add subtle parallax effect to floating elements
        const floatingElements = document.querySelectorAll('.animate-float');
        
        if (floatingElements.length > 0 && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            let animationId;
            
            const handleMouseMove = (e) => {
                if (animationId) cancelAnimationFrame(animationId);
                
                animationId = requestAnimationFrame(() => {
                    const { clientX, clientY } = e;
                    const { innerWidth, innerHeight } = window;
                    
                    const xPercent = (clientX / innerWidth - 0.5) * 2;
                    const yPercent = (clientY / innerHeight - 0.5) * 2;
                    
                    floatingElements.forEach((element) => {
                        const intensity = 10;
                        const x = xPercent * intensity;
                        const y = yPercent * intensity;
                        
                        element.style.transform = `translate(${x}px, ${y}px)`;
                    });
                });
            };
            
            document.addEventListener('mousemove', handleMouseMove);
            
            // Reset on mouse leave
            document.addEventListener('mouseleave', () => {
                floatingElements.forEach((element) => {
                    element.style.transform = '';
                });
            });
        }
    }

    addAccessibilityFeatures() {
        // Add focus indicators for better keyboard navigation
        const focusableElements = document.querySelectorAll('a, button, [tabindex]:not([tabindex="-1"])');
        
        focusableElements.forEach((element) => {
            element.addEventListener('focus', () => {
                element.style.outline = '3px solid #3b82f6';
                element.style.outlineOffset = '2px';
            });
            
            element.addEventListener('blur', () => {
                element.style.outline = '';
                element.style.outlineOffset = '';
            });
        });

        // Announce error state to screen readers
        const errorTitle = document.querySelector('h1, h2');
        if (errorTitle) {
            errorTitle.setAttribute('aria-live', 'polite');
            errorTitle.setAttribute('role', 'alert');
        }
    }

    showKeyboardHelp() {
        // Create and show keyboard shortcuts modal
        const modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50';
        modal.innerHTML = `
            <div class="bg-white rounded-2xl p-8 max-w-md mx-4 shadow-2xl">
                <h3 class="text-xl font-bold text-gray-900 mb-6">Keyboard Shortcuts</h3>
                <div class="space-y-3">
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">Go Home</span>
                        <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">H</kbd>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">Go Back</span>
                        <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">B</kbd>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">Reload Page</span>
                        <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">R</kbd>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">Show Help</span>
                        <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">ESC</kbd>
                    </div>
                </div>
                <button onclick="this.closest('.fixed').remove()" class="mt-6 w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors">
                    Close
                </button>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        // Close on click outside
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        });
        
        // Close on Escape
        const closeOnEscape = (e) => {
            if (e.key === 'Escape') {
                modal.remove();
                document.removeEventListener('keydown', closeOnEscape);
            }
        };
        document.addEventListener('keydown', closeOnEscape);
    }

    trackErrors() {
        // Simple error tracking (can be extended with analytics)
        const errorData = {
            page: window.location.pathname,
            referrer: document.referrer,
            userAgent: navigator.userAgent,
            timestamp: new Date().toISOString(),
            type: document.title.includes('404') ? '404' : '5xx'
        };

        // Store in localStorage for debugging
        const errorLog = JSON.parse(localStorage.getItem('errorLog') || '[]');
        errorLog.push(errorData);
        
        // Keep only last 10 errors
        if (errorLog.length > 10) {
            errorLog.shift();
        }
        
        localStorage.setItem('errorLog', JSON.stringify(errorLog));

        // Console log for development
        console.group('🚨 Error Page Loaded');
        console.table(errorData);
        console.groupEnd();
    }
}

// Auto-refresh functionality for server errors
class ServerErrorManager {
    constructor() {
        if (document.title.includes('Server Error')) {
            this.init();
        }
    }

    init() {
        this.retryCount = 0;
        this.maxRetries = 5;
        this.baseDelay = 30000; // 30 seconds
        
        this.startAutoRefresh();
        this.addRetryButton();
    }

    startAutoRefresh() {
        const delay = this.baseDelay * Math.pow(1.5, this.retryCount); // Exponential backoff
        
        setTimeout(() => {
            if (this.retryCount < this.maxRetries) {
                this.retryCount++;
                console.log(`Auto-refresh attempt ${this.retryCount}/${this.maxRetries}`);
                location.reload();
            } else {
                this.showMaxRetriesMessage();
            }
        }, delay);
    }

    addRetryButton() {
        const tryAgainButton = document.querySelector('button[onclick*="reload"]');
        if (tryAgainButton) {
            const originalOnClick = tryAgainButton.onclick;
            tryAgainButton.onclick = () => {
                this.retryCount = 0; // Reset retry count on manual retry
                originalOnClick();
            };
        }
    }

    showMaxRetriesMessage() {
        const notification = document.createElement('div');
        notification.className = 'fixed top-4 right-4 bg-amber-100 border border-amber-400 text-amber-800 px-4 py-3 rounded-lg shadow-lg z-50';
        notification.innerHTML = `
            <div class="flex items-center">
                <svg class="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"></path>
                </svg>
                <span>Auto-refresh disabled. Please try again manually.</span>
            </div>
        `;
        
        document.body.appendChild(notification);
        
        setTimeout(() => {
            notification.remove();
        }, 5000);
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new ErrorPageEnhancer();
    new ServerErrorManager();
});

export { ErrorPageEnhancer, ServerErrorManager };