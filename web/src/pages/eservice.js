/**
 * Enhanced eService Landing Page JavaScript
 * Advanced animations, smooth scrolling, and interactive effects
 */

// Enhanced Animation System
class EServiceAnimations {
    constructor() {
        this.init();
    }

    init() {
        this.initScrollAnimations();
        this.initServiceCardAnimations();
        this.initSmoothScrolling();
        this.initParallaxEffects();
        this.initPerformanceObservers();
        this.initAdvancedInteractions();
    }

    // Advanced Scroll-based Animations
    initScrollAnimations() {
        const observerOptions = {
            root: null,
            rootMargin: '-10% 0px -10% 0px',
            threshold: [0, 0.1, 0.5, 1]
        };

        const scrollObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const element = entry.target;
                    const delay = element.dataset.delay || 0;
                    
                    setTimeout(() => {
                        element.classList.add('visible');
                        this.triggerElementAnimation(element);
                    }, delay);
                }
            });
        }, observerOptions);

        // Observe elements with scroll-fade class
        document.querySelectorAll('.scroll-fade').forEach(el => {
            scrollObserver.observe(el);
        });

        // Observe service cards
        document.querySelectorAll('.service-card').forEach((el, index) => {
            el.dataset.delay = index * 100;
            el.classList.add('scroll-fade');
            scrollObserver.observe(el);
        });

        // Observe resource cards
        document.querySelectorAll('.resource-card').forEach((el, index) => {
            el.dataset.delay = index * 50;
            el.classList.add('scroll-fade');
            scrollObserver.observe(el);
        });
    }

    // Enhanced Service Card Animations
    initServiceCardAnimations() {
        const cards = document.querySelectorAll('.service-card');
        
        cards.forEach(card => {
            let animationFrame;
            let isAnimating = false;

            // Enhanced hover detection with precise boundaries
            const handleMouseEnter = (e) => {
                if (isAnimating) return;
                isAnimating = true;

                const rect = card.getBoundingClientRect();
                const centerX = rect.left + rect.width / 2;
                const centerY = rect.top + rect.height / 2;
                
                const mouseX = e.clientX;
                const mouseY = e.clientY;
                
                const rotateX = (mouseY - centerY) / 10;
                const rotateY = (centerX - mouseX) / 10;

                animationFrame = requestAnimationFrame(() => {
                    card.style.transform = `
                        translateY(-8px) 
                        rotateX(${Math.max(-5, Math.min(5, rotateX))}deg) 
                        rotateY(${Math.max(-5, Math.min(5, rotateY))}deg)
                        scale(1.02)
                    `;
                    
                    this.addGlowEffect(card);
                    isAnimating = false;
                });
            };

            const handleMouseMove = (e) => {
                if (isAnimating) return;
                
                const rect = card.getBoundingClientRect();
                const x = ((e.clientX - rect.left) / rect.width) * 100;
                const y = ((e.clientY - rect.top) / rect.height) * 100;
                
                // Update CSS custom properties for dynamic effects
                card.style.setProperty('--mouse-x', `${x}%`);
                card.style.setProperty('--mouse-y', `${y}%`);
            };

            const handleMouseLeave = () => {
                if (animationFrame) {
                    cancelAnimationFrame(animationFrame);
                }
                
                animationFrame = requestAnimationFrame(() => {
                    card.style.transform = '';
                    card.style.removeProperty('--mouse-x');
                    card.style.removeProperty('--mouse-y');
                    this.removeGlowEffect(card);
                    isAnimating = false;
                });
            };

            // Precise event binding with boundary checking
            card.addEventListener('mouseenter', handleMouseEnter, { passive: false });
            card.addEventListener('mousemove', handleMouseMove, { passive: true });
            card.addEventListener('mouseleave', handleMouseLeave, { passive: false });

            // Touch support for mobile devices
            card.addEventListener('touchstart', (e) => {
                e.preventDefault();
                card.classList.add('touch-active');
                setTimeout(() => {
                    card.classList.remove('touch-active');
                }, 300);
            }, { passive: false });
        });
    }

    // Enhanced Smooth Scrolling
    initSmoothScrolling() {
        // Internal link smooth scrolling
        document.querySelectorAll('a[href^="#"]').forEach(anchor => {
            anchor.addEventListener('click', (e) => {
                e.preventDefault();
                const target = document.querySelector(anchor.getAttribute('href'));
                
                if (target) {
                    const headerHeight = document.querySelector('header')?.offsetHeight || 0;
                    const targetPosition = target.offsetTop - headerHeight - 20;
                    
                    window.scrollTo({
                        top: targetPosition,
                        behavior: 'smooth'
                    });
                }
            });
        });

        // Enhanced service button interactions
        document.querySelectorAll('.service-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.createRippleEffect(e, btn);
            });
        });
    }

    // Advanced Parallax Effects
    initParallaxEffects() {
        let ticking = false;

        const updateParallax = () => {
            const scrolled = window.pageYOffset;
            const rate = scrolled * -0.5;
            const opacity = Math.max(0.3, 1 - scrolled / 1000);

            // Update background patterns
            const bgPattern = document.querySelector('main::before');
            if (bgPattern) {
                document.documentElement.style.setProperty('--bg-translate', `${rate}px`);
                document.documentElement.style.setProperty('--bg-opacity', opacity);
            }

            // Parallax for service cards
            document.querySelectorAll('.service-card').forEach((card, index) => {
                if (this.isInViewport(card)) {
                    const speed = 0.1 + (index * 0.02);
                    const yPos = -(scrolled * speed);
                    card.style.setProperty('--parallax-y', `${yPos}px`);
                }
            });

            ticking = false;
        };

        window.addEventListener('scroll', () => {
            if (!ticking) {
                requestAnimationFrame(updateParallax);
                ticking = true;
            }
        }, { passive: true });
    }

    // Performance Observers
    initPerformanceObservers() {
        // Performance observer for animations
        if ('PerformanceObserver' in window) {
            const perfObserver = new PerformanceObserver((list) => {
                const entries = list.getEntries();
                entries.forEach(entry => {
                    if (entry.duration > 16.67) {
                        console.warn('Long animation detected:', entry.name);
                    }
                });
            });

            try {
                perfObserver.observe({ type: 'measure', buffered: true });
            } catch (e) {
                // Fallback for browsers that don't support this
                console.log('Performance observer not supported');
            }
        }

        // Intersection observer for lazy loading animations
        const lazyAnimationObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const element = entry.target;
                    element.classList.add('animate-in');
                    lazyAnimationObserver.unobserve(element);
                }
            });
        }, { rootMargin: '50px' });

        document.querySelectorAll('[data-animate]').forEach(el => {
            lazyAnimationObserver.observe(el);
        });
    }

    // Advanced Interactive Effects
    initAdvancedInteractions() {
        // Enhanced alert banner interactions
        const alertBanner = document.querySelector('.alert-banner');
        if (alertBanner) {
            let shimmerTimeout;
            
            alertBanner.addEventListener('mouseenter', () => {
                clearTimeout(shimmerTimeout);
                alertBanner.style.animationPlayState = 'paused';
            });

            alertBanner.addEventListener('mouseleave', () => {
                shimmerTimeout = setTimeout(() => {
                    alertBanner.style.animationPlayState = 'running';
                }, 1000);
            });
        }

        // Enhanced resource card interactions
        document.querySelectorAll('.resource-card').forEach(card => {
            card.addEventListener('mouseenter', () => {
                this.createFloatingParticles(card);
            });
        });

        // Advanced theme toggle animations
        const themeToggle = document.querySelector('[data-theme-toggle]');
        if (themeToggle) {
            themeToggle.addEventListener('click', (e) => {
                this.animateThemeTransition();
            });
        }

        // Enhanced footer link animations
        document.querySelectorAll('.footer-links a').forEach(link => {
            link.addEventListener('mouseenter', () => {
                this.animateFooterLink(link);
            });
        });
    }

    // Helper Methods
    triggerElementAnimation(element) {
        const animationType = element.dataset.animation || 'fadeInUp';
        element.style.animation = `${animationType} 0.8s cubic-bezier(0.4, 0, 0.2, 1) forwards`;
    }

    addGlowEffect(element) {
        const glowClass = element.classList.contains('service-btn') ? 'glow-blue' : 'glow-default';
        element.classList.add(glowClass);
    }

    removeGlowEffect(element) {
        element.classList.remove('glow-blue', 'glow-orange', 'glow-default');
    }

    createRippleEffect(event, element) {
        const rect = element.getBoundingClientRect();
        const ripple = document.createElement('span');
        const size = Math.max(rect.width, rect.height);
        const x = event.clientX - rect.left - size / 2;
        const y = event.clientY - rect.top - size / 2;

        ripple.style.cssText = `
            position: absolute;
            width: ${size}px;
            height: ${size}px;
            left: ${x}px;
            top: ${y}px;
            background: rgba(255, 255, 255, 0.5);
            border-radius: 50%;
            transform: scale(0);
            animation: ripple 0.6s linear;
            pointer-events: none;
        `;

        element.style.position = 'relative';
        element.style.overflow = 'hidden';
        element.appendChild(ripple);

        setTimeout(() => {
            ripple.remove();
        }, 600);
    }

    createFloatingParticles(element) {
        const particles = [];
        const particleCount = 5;

        for (let i = 0; i < particleCount; i++) {
            const particle = document.createElement('div');
            particle.style.cssText = `
                position: absolute;
                width: 4px;
                height: 4px;
                background: linear-gradient(45deg, #3b82f6, #10b981);
                border-radius: 50%;
                pointer-events: none;
                opacity: 0;
                animation: particleFloat 2s ease-out forwards;
                animation-delay: ${i * 0.1}s;
            `;

            const rect = element.getBoundingClientRect();
            particle.style.left = `${rect.left + Math.random() * rect.width}px`;
            particle.style.top = `${rect.top + Math.random() * rect.height}px`;

            document.body.appendChild(particle);
            particles.push(particle);
        }

        setTimeout(() => {
            particles.forEach(particle => particle.remove());
        }, 2000);
    }

    animateThemeTransition() {
        const overlay = document.createElement('div');
        overlay.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: radial-gradient(circle at center, rgba(0,0,0,0.8) 0%, transparent 70%);
            opacity: 0;
            transition: opacity 0.3s ease-out;
            pointer-events: none;
            z-index: 9999;
        `;

        document.body.appendChild(overlay);
        
        requestAnimationFrame(() => {
            overlay.style.opacity = '1';
        });

        setTimeout(() => {
            overlay.style.opacity = '0';
            setTimeout(() => overlay.remove(), 300);
        }, 150);
    }

    animateFooterLink(link) {
        const icon = link.querySelector('::before');
        link.style.transform = 'translateX(5px)';
        
        setTimeout(() => {
            link.style.transform = '';
        }, 200);
    }

    isInViewport(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    }

    // Debounced event handler
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
}

// Additional CSS animations to be injected
const additionalStyles = `
    @keyframes ripple {
        to {
            transform: scale(4);
            opacity: 0;
        }
    }

    @keyframes particleFloat {
        0% {
            opacity: 0;
            transform: translateY(0) scale(0);
        }
        50% {
            opacity: 1;
            transform: translateY(-20px) scale(1);
        }
        100% {
            opacity: 0;
            transform: translateY(-40px) scale(0);
        }
    }

    .glow-blue {
        box-shadow: 0 0 20px rgba(59, 130, 246, 0.4) !important;
    }

    .glow-orange {
        box-shadow: 0 0 20px rgba(234, 88, 12, 0.4) !important;
    }

    .glow-default {
        box-shadow: 0 0 15px rgba(255, 255, 255, 0.2) !important;
    }

    .touch-active {
        transform: scale(0.98) !important;
    }

    .animate-in {
        animation: fadeInUp 0.8s cubic-bezier(0.4, 0, 0.2, 1) forwards !important;
    }
`;

// Initialize Enhanced eService Animations
document.addEventListener('DOMContentLoaded', () => {
    // Inject additional styles
    const style = document.createElement('style');
    style.textContent = additionalStyles;
    document.head.appendChild(style);

    // Initialize animation system
    new EServiceAnimations();

    // Add enhanced loading animation
    document.body.classList.add('fade-in-up');

    // Enhanced page visibility handling
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            // Pause animations when page is hidden
            document.querySelectorAll('*').forEach(el => {
                if (el.style.animationPlayState !== undefined) {
                    el.style.animationPlayState = 'paused';
                }
            });
        } else {
            // Resume animations when page is visible
            document.querySelectorAll('*').forEach(el => {
                if (el.style.animationPlayState !== undefined) {
                    el.style.animationPlayState = 'running';
                }
            });
        }
    });
});

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = EServiceAnimations;
}