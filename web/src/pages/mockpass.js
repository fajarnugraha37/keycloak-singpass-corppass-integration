// Enhanced MockPass Interactive Features with Parallax & Animations
document.addEventListener('DOMContentLoaded', function() {
    // Initialize all enhanced features
    initScrollEffects();
    initParallaxEffects();
    initIntersectionObserver();
    initCopyToClipboard();
    initSmoothScrolling();
    initHeaderScrollEffect();
    initInteractiveAnimations();
    initMouseParallax();
    initAdvancedAnimations();
});

// Enhanced Scroll Effects and Header Behavior
function initScrollEffects() {
    const header = document.querySelector('.header');
    let lastScrollY = window.scrollY;
    let scrollTimeout;

    const throttledScroll = throttle(() => {
        const currentScrollY = window.scrollY;
        
        // Header styling based on scroll position
        if (currentScrollY > 100) {
            header.classList.add('scrolled');
            
            // Hide header when scrolling down, show when scrolling up
            if (currentScrollY > lastScrollY && currentScrollY > 200) {
                header.classList.add('hidden');
            } else {
                header.classList.remove('hidden');
            }
        } else {
            header.classList.remove('scrolled', 'hidden');
        }
        
        lastScrollY = currentScrollY;

        // Add scroll progress indicator
        updateScrollProgress();

        // Clear existing timeout and set new one for scroll end
        clearTimeout(scrollTimeout);
        scrollTimeout = setTimeout(() => {
            document.body.classList.add('scroll-ended');
            setTimeout(() => {
                document.body.classList.remove('scroll-ended');
            }, 100);
        }, 150);
    }, 10);

    window.addEventListener('scroll', throttledScroll);
}

// Add scroll progress indicator
function updateScrollProgress() {
    const scrolled = window.pageYOffset;
    const maxHeight = document.documentElement.scrollHeight - window.innerHeight;
    const progress = (scrolled / maxHeight) * 100;
    
    // Create or update progress bar
    let progressBar = document.querySelector('.scroll-progress');
    if (!progressBar) {
        progressBar = document.createElement('div');
        progressBar.className = 'scroll-progress';
        progressBar.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 0%;
            height: 3px;
            background: linear-gradient(90deg, var(--primary-red), var(--secondary-blue));
            z-index: 9999;
            transition: width 0.1s ease-out;
        `;
        document.body.appendChild(progressBar);
    }
    
    progressBar.style.width = `${progress}%`;
}

// Advanced Parallax Effects
function initParallaxEffects() {
    const heroBackground = document.querySelector('.hero-background');
    const heroPattern = document.querySelector('.hero-pattern');
    
    if (!heroBackground || !heroPattern) return;

    let rafId = null;
    
    function updateParallax() {
        const scrolled = window.pageYOffset;
        const rate = scrolled * -0.5;
        const rate2 = scrolled * -0.3;
        const rate3 = scrolled * -0.7;
        
        // Multi-layer parallax effect
        heroPattern.style.transform = `translateY(${rate}px) rotate(${scrolled * 0.01}deg)`;
        heroBackground.style.transform = `translateY(${rate2}px)`;
        
        // Apply to identity cards for depth
        const cards = document.querySelectorAll('.card');
        cards.forEach((card, index) => {
            const cardRate = scrolled * (-0.2 - index * 0.1);
            card.style.transform = `translateY(${cardRate}px) rotateY(-15deg)`;
        });
        
        rafId = null;
    }
    
    function onScroll() {
        if (rafId === null) {
            rafId = requestAnimationFrame(updateParallax);
        }
    }
    
    window.addEventListener('scroll', onScroll);
}

// Mouse Parallax Effect
function initMouseParallax() {
    const hero = document.querySelector('.hero');
    if (!hero) return;
    
    hero.addEventListener('mousemove', throttle((e) => {
        const { clientX, clientY } = e;
        const { innerWidth, innerHeight } = window;
        
        const xPos = (clientX / innerWidth - 0.5) * 30;
        const yPos = (clientY / innerHeight - 0.5) * 30;
        
        const heroContent = document.querySelector('.hero-content');
        const heroVisual = document.querySelector('.hero-visual');
        const heroPattern = document.querySelector('.hero-pattern');
        
        if (heroContent) {
            heroContent.style.transform = `translate(${xPos * 0.5}px, ${yPos * 0.5}px)`;
        }
        
        if (heroVisual) {
            heroVisual.style.transform = `translate(${xPos * -0.3}px, ${yPos * -0.3}px) perspective(1000px) rotateY(${xPos * 0.1}deg)`;
        }
        
        if (heroPattern) {
            heroPattern.style.transform = `translate(${xPos * -0.1}px, ${yPos * -0.1}px)`;
        }
    }, 16));
    
    hero.addEventListener('mouseleave', () => {
        const heroContent = document.querySelector('.hero-content');
        const heroVisual = document.querySelector('.hero-visual');
        const heroPattern = document.querySelector('.hero-pattern');
        
        [heroContent, heroVisual, heroPattern].forEach(element => {
            if (element) {
                element.style.transform = '';
                element.style.transition = 'transform 0.5s ease-out';
                setTimeout(() => {
                    element.style.transition = '';
                }, 500);
            }
        });
    });
}

// Advanced Intersection Observer with Staggered Animations
function initIntersectionObserver() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -100px 0px'
    };
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                const element = entry.target;
                
                // Determine animation type based on element
                if (element.classList.contains('feature-card')) {
                    animateFeatureCard(element);
                } else if (element.classList.contains('endpoint-card')) {
                    animateEndpointCard(element);
                } else if (element.classList.contains('doc-card')) {
                    animateDocCard(element);
                } else if (element.classList.contains('section-header')) {
                    animateSectionHeader(element);
                }
                
                observer.unobserve(element);
            }
        });
    }, observerOptions);
    
    // Observe all animatable elements
    const elements = document.querySelectorAll('.feature-card, .endpoint-card, .doc-card, .section-header');
    elements.forEach(element => {
        observer.observe(element);
    });
}

// Specific animation functions
function animateFeatureCard(card) {
    const index = Array.from(card.parentNode.children).indexOf(card);
    setTimeout(() => {
        card.classList.add('animate');
        card.style.transform = 'translateY(0) scale(1)';
        card.style.opacity = '1';
    }, index * 150);
}

function animateEndpointCard(card) {
    const index = Array.from(card.parentNode.children).indexOf(card);
    setTimeout(() => {
        card.classList.add('animate');
        card.style.transform = 'translateY(0) scale(1)';
        card.style.opacity = '1';
    }, index * 200);
}

function animateDocCard(card) {
    const index = Array.from(card.parentNode.children).indexOf(card);
    setTimeout(() => {
        card.classList.add('animate');
        card.style.transform = 'translateY(0) rotateX(0)';
        card.style.opacity = '1';
    }, index * 100);
}

function animateSectionHeader(header) {
    header.style.transform = 'translateY(0)';
    header.style.opacity = '1';
}

// Enhanced Copy to Clipboard with Animations
function initCopyToClipboard() {
    const copyButtons = document.querySelectorAll('.copy-btn');
    
    copyButtons.forEach(button => {
        button.addEventListener('click', async (e) => {
            e.preventDefault();
            
            const url = button.getAttribute('data-url');
            if (!url) return;
            
            // Enhanced click animation
            button.style.transform = 'scale(0.9) rotate(5deg)';
            button.style.background = 'var(--primary-red)';
            button.style.color = 'white';
            
            setTimeout(() => {
                button.style.transform = 'scale(1.1)';
            }, 100);
            
            setTimeout(() => {
                button.style.transform = 'scale(1)';
            }, 200);
            
            try {
                const fullUrl = window.location.origin + url;
                await navigator.clipboard.writeText(fullUrl);
                showToast('URL copied to clipboard! ✨', 'success');
                
                // Success animation
                button.innerHTML = `
                    <svg viewBox="0 0 20 20" fill="currentColor">
                        <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                `;
                
                setTimeout(() => {
                    button.innerHTML = `
                        <svg viewBox="0 0 20 20" fill="currentColor">
                            <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z"/>
                            <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z"/>
                        </svg>
                    `;
                    button.style.background = '';
                    button.style.color = '';
                }, 1500);
                
            } catch (err) {
                console.error('Failed to copy: ', err);
                showToast('Failed to copy URL ❌', 'error');
                
                // Error animation
                button.style.animation = 'shake 0.5s ease-in-out';
                setTimeout(() => {
                    button.style.animation = '';
                    button.style.background = '';
                    button.style.color = '';
                }, 500);
            }
        });
    });
}

// Advanced Toast Notifications
function showToast(message, type = 'success') {
    // Remove existing toasts
    const existingToasts = document.querySelectorAll('.toast');
    existingToasts.forEach(toast => toast.remove());
    
    // Create new toast with enhanced styling
    const toast = document.createElement('div');
    toast.className = 'toast';
    
    const iconPaths = {
        success: '<path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>',
        error: '<path d="M18 6L6 18M6 6l12 12"/>',
        info: '<path d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>'
    };
    
    toast.innerHTML = `
        <div class="toast-content">
            <svg class="toast-icon" viewBox="0 0 20 20" fill="currentColor">
                ${iconPaths[type] || iconPaths.success}
            </svg>
            <span class="toast-message">${message}</span>
        </div>
    `;
    
    // Style based on type
    const styles = {
        success: { background: 'var(--accent-green)', color: 'white' },
        error: { background: 'var(--primary-red)', color: 'white' },
        info: { background: 'var(--secondary-blue)', color: 'white' }
    };
    
    Object.assign(toast.style, styles[type] || styles.success);
    
    document.body.appendChild(toast);
    
    // Enhanced entrance animation
    toast.style.transform = 'translateY(100px) scale(0.8)';
    toast.style.opacity = '0';
    
    requestAnimationFrame(() => {
        toast.style.transition = 'all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)';
        toast.style.transform = 'translateY(0) scale(1)';
        toast.style.opacity = '1';
    });
    
    // Auto remove with exit animation
    setTimeout(() => {
        toast.style.transform = 'translateY(-20px) scale(0.9)';
        toast.style.opacity = '0';
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 400);
    }, 3500);
}

// Enhanced Smooth Scrolling
function initSmoothScrolling() {
    const navLinks = document.querySelectorAll('.nav-link[href^="#"]');
    
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            
            const targetId = link.getAttribute('href');
            const targetElement = document.querySelector(targetId);
            
            if (targetElement) {
                const headerHeight = document.querySelector('.header').offsetHeight;
                const targetPosition = targetElement.offsetTop - headerHeight - 30;
                
                // Enhanced scroll with easing
                smoothScrollTo(targetPosition, 800);
                
                // Visual feedback
                link.style.transform = 'scale(0.95)';
                link.style.background = 'rgba(255, 255, 255, 0.1)';
                
                setTimeout(() => {
                    link.style.transform = 'scale(1)';
                    link.style.background = '';
                }, 150);
            }
        });
    });
}

// Custom smooth scroll function with easing
function smoothScrollTo(target, duration) {
    const start = window.pageYOffset;
    const distance = target - start;
    let startTime = null;
    
    function animation(currentTime) {
        if (startTime === null) startTime = currentTime;
        const timeElapsed = currentTime - startTime;
        const run = easeInOutCubic(timeElapsed, start, distance, duration);
        window.scrollTo(0, run);
        if (timeElapsed < duration) requestAnimationFrame(animation);
    }
    
    requestAnimationFrame(animation);
}

// Easing function for smooth animations
function easeInOutCubic(t, b, c, d) {
    t /= d / 2;
    if (t < 1) return c / 2 * t * t * t + b;
    t -= 2;
    return c / 2 * (t * t * t + 2) + b;
}

// Header Scroll Effect with Intersection Observer
function initHeaderScrollEffect() {
    const header = document.querySelector('.header');
    const heroSection = document.querySelector('.hero');
    
    if (!header || !heroSection) return;

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                header.classList.remove('scrolled');
            } else {
                header.classList.add('scrolled');
            }
        });
    }, {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    });

    observer.observe(heroSection);
}

// Advanced Interactive Animations
function initInteractiveAnimations() {
    // Enhanced identity cards animation
    const identityCards = document.querySelectorAll('.card');
    identityCards.forEach((card, index) => {
        card.addEventListener('mouseenter', () => {
            card.style.animationPlayState = 'paused';
            card.style.transform = 'translateY(-15px) scale(1.05) rotateY(5deg)';
            card.style.boxShadow = '0 20px 40px rgba(0, 0, 0, 0.3)';
            
            // Add glow effect
            card.style.filter = 'brightness(1.1)';
        });
        
        card.addEventListener('mouseleave', () => {
            card.style.animationPlayState = 'running';
            card.style.transform = '';
            card.style.boxShadow = '';
            card.style.filter = '';
        });
    });
    
    // Enhanced button interactions with ripple effect
    const buttons = document.querySelectorAll('.btn');
    buttons.forEach(button => {
        button.addEventListener('click', function(e) {
            createRippleEffect(e, this);
        });
        
        button.addEventListener('mouseenter', () => {
            const icon = button.querySelector('svg');
            if (icon) {
                icon.style.transform = 'translateX(5px) scale(1.1) rotate(5deg)';
            }
        });
        
        button.addEventListener('mouseleave', () => {
            const icon = button.querySelector('svg');
            if (icon) {
                icon.style.transform = 'translateX(0) scale(1) rotate(0deg)';
            }
        });
    });
    
    // Interactive feature icons with magnetic effect
    const featureIcons = document.querySelectorAll('.feature-icon');
    featureIcons.forEach(icon => {
        icon.addEventListener('mouseenter', () => {
            icon.style.transform = 'scale(1.2) rotate(15deg)';
            icon.style.boxShadow = '0 10px 25px rgba(0, 0, 0, 0.2)';
        });
        
        icon.addEventListener('mouseleave', () => {
            icon.style.transform = 'scale(1) rotate(0deg)';
            icon.style.boxShadow = '';
        });
    });
    
    // Endpoint cards with advanced hover effects
    const endpointCards = document.querySelectorAll('.endpoint-card');
    endpointCards.forEach(card => {
        card.addEventListener('mouseenter', () => {
            const icon = card.querySelector('.endpoint-icon');
            if (icon) {
                icon.style.transform = 'scale(1.15) rotate(-10deg)';
            }
            
            // Add shimmer effect
            card.style.background = 'linear-gradient(45deg, white 25%, rgba(255,255,255,0.8) 50%, white 75%)';
            card.style.backgroundSize = '200% 100%';
            card.style.animation = 'shimmer 1.5s ease-in-out infinite';
        });
        
        card.addEventListener('mouseleave', () => {
            const icon = card.querySelector('.endpoint-icon');
            if (icon) {
                icon.style.transform = 'scale(1) rotate(0deg)';
            }
            
            card.style.background = 'white';
            card.style.animation = '';
        });
    });
}

// Advanced Animations
function initAdvancedAnimations() {
    // Floating elements animation
    const floatingElements = document.querySelectorAll('.hero-badge, .identity-cards');
    floatingElements.forEach((element, index) => {
        element.style.animation = `float 6s ease-in-out infinite ${index * 2}s`;
    });
    
    // Staggered text animation for hero title
    const heroTitle = document.querySelector('.hero-title');
    if (heroTitle) {
        const text = heroTitle.innerHTML;
        const words = text.split(' ');
        heroTitle.innerHTML = words.map((word, index) => 
            `<span class="word" style="animation-delay: ${index * 0.1}s">${word}</span>`
        ).join(' ');
    }
    
    // Parallax background patterns
    const patterns = document.querySelectorAll('.hero-pattern::before, .hero-pattern::after');
    patterns.forEach((pattern, index) => {
        pattern.style.animation = `parallaxFloat ${30 + index * 5}s ease-in-out infinite`;
    });
    
    // Interactive navigation links
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('mouseenter', () => {
            link.style.transform = 'translateY(-2px) scale(1.05)';
            link.style.textShadow = '0 2px 4px rgba(0, 0, 0, 0.3)';
        });
        
        link.addEventListener('mouseleave', () => {
            link.style.transform = 'translateY(0) scale(1)';
            link.style.textShadow = '';
        });
    });
}

// Ripple Effect Function
function createRippleEffect(event, element) {
    const ripple = document.createElement('span');
    const rect = element.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    const x = event.clientX - rect.left - size / 2;
    const y = event.clientY - rect.top - size / 2;
    
    ripple.style.cssText = `
        position: absolute;
        width: ${size}px;
        height: ${size}px;
        left: ${x}px;
        top: ${y}px;
        background: rgba(255, 255, 255, 0.4);
        border-radius: 50%;
        pointer-events: none;
        transform: scale(0);
        animation: ripple 0.6s linear;
        z-index: 1;
    `;
    
    element.style.position = 'relative';
    element.style.overflow = 'hidden';
    element.appendChild(ripple);
    
    setTimeout(() => {
        ripple.remove();
    }, 600);
}

// Utility Functions
function throttle(func, limit) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    }
}

function debounce(func, wait, immediate) {
    let timeout;
    return function executedFunction() {
        const context = this;
        const args = arguments;
        const later = function() {
            timeout = null;
            if (!immediate) func.apply(context, args);
        };
        const callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(context, args);
    };
}

// Performance Optimization for Mobile
if (window.innerWidth < 768) {
    document.documentElement.style.setProperty('--transition-normal', '0.15s ease-in-out');
    document.documentElement.style.setProperty('--transition-slow', '0.2s ease-in-out');
}

// Accessibility: Reduced Motion Support
if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    const reducedMotionCSS = `
        *, *::before, *::after {
            animation-duration: 0.01ms !important;
            animation-iteration-count: 1 !important;
            transition-duration: 0.01ms !important;
            scroll-behavior: auto !important;
        }
    `;
    
    const style = document.createElement('style');
    style.textContent = reducedMotionCSS;
    document.head.appendChild(style);
}

// Add enhanced CSS animations
const enhancedCSS = `
    @keyframes ripple {
        to {
            transform: scale(4);
            opacity: 0;
        }
    }
    
    @keyframes shimmer {
        0% { background-position: -200% 0; }
        100% { background-position: 200% 0; }
    }
    
    @keyframes shake {
        0%, 100% { transform: translateX(0); }
        25% { transform: translateX(-5px); }
        75% { transform: translateX(5px); }
    }
    
    .word {
        display: inline-block;
        animation: slideInUp 0.6s ease-out both;
    }
    
    .scroll-progress {
        position: fixed !important;
        top: 0 !important;
        left: 0 !important;
        height: 3px !important;
        background: linear-gradient(90deg, var(--primary-red), var(--secondary-blue)) !important;
        z-index: 9999 !important;
        transition: width 0.1s ease-out !important;
    }
`;

const styleSheet = document.createElement('style');
styleSheet.textContent = enhancedCSS;
document.head.appendChild(styleSheet);

// Analytics and Performance Tracking
function trackPerformance() {
    if ('performance' in window) {
        window.addEventListener('load', () => {
            const perfData = performance.getEntriesByType('navigation')[0];
            console.log(`MockPass loaded in ${perfData.loadEventEnd - perfData.fetchStart}ms`);
        });
    }
}

trackPerformance();