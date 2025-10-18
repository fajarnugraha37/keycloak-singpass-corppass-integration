// Enhanced MockPass Interactive Features with Parallax & Animations
document.addEventListener('DOMContentLoaded', function() {
    // Initialize all enhanced features
    initScrollEffects();
    initParallaxEffects();
    init3DScrollEffects();
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

// Advanced 3D Scroll Effects
function init3DScrollEffects() {
    const sections = document.querySelectorAll('.feature-grid, .endpoints-grid');
    const cards = document.querySelectorAll('.feature-card, .endpoint-card');
    
    function handle3DScroll() {
        const scrollY = window.scrollY;
        const windowHeight = window.innerHeight;
        
        // 3D Section transformations
        sections.forEach((section, index) => {
            const sectionTop = section.offsetTop;
            const sectionHeight = section.offsetHeight;
            const sectionProgress = Math.max(0, Math.min(1, (scrollY - sectionTop + windowHeight) / (windowHeight + sectionHeight)));
            
            const perspective = 1000 + sectionProgress * 500;
            const rotateY = (sectionProgress - 0.5) * 10;
            const rotateX = (sectionProgress - 0.5) * 5;
            const translateZ = sectionProgress * 50;
            
            section.style.perspective = `${perspective}px`;
            section.style.transform = `
                perspective(${perspective}px) 
                rotateY(${rotateY}deg) 
                rotateX(${rotateX}deg) 
                translateZ(${translateZ}px)
                scale3d(${0.9 + sectionProgress * 0.1}, ${0.9 + sectionProgress * 0.1}, 1)
            `;
        });
        
        // 3D Card animations based on scroll with better boundaries (exclude doc cards)
        cards.forEach((card, index) => {
            const cardRect = card.getBoundingClientRect();
            const cardCenter = cardRect.top + cardRect.height / 2;
            const viewportCenter = windowHeight / 2;
            const distance = Math.abs(cardCenter - viewportCenter);
            const maxDistance = windowHeight / 2;
            const progress = Math.max(0, 1 - distance / maxDistance);
            
            // Only apply transforms if card is reasonably in view
            if (cardRect.top < windowHeight && cardRect.bottom > 0) {
                const rotateY = (cardCenter - viewportCenter) / maxDistance * 8; // Reduced from 15
                const rotateX = (cardCenter - viewportCenter) / maxDistance * 4; // Reduced from 8
                const translateZ = progress * 20; // Reduced from 30
                const scale = 0.95 + progress * 0.05; // Reduced scaling
                
                card.style.transform = `
                    perspective(1200px) 
                    rotateY(${rotateY}deg) 
                    rotateX(${rotateX}deg) 
                    translateZ(${translateZ}px) 
                    scale3d(${scale}, ${scale}, 1)
                `;
            }
        });
        
        // 3D Depth layers for hero
        const hero = document.querySelector('.hero');
        if (hero) {
            const heroProgress = Math.max(0, 1 - scrollY / (windowHeight * 0.8));
            const heroPattern = document.querySelector('.hero-pattern');
            const identityCards = document.querySelector('.identity-cards');
            
            if (heroPattern) {
                heroPattern.style.transform = `
                    translateZ(${heroProgress * 20}px) 
                    rotateY(${scrollY * 0.02}deg) 
                    scale3d(${1 + heroProgress * 0.1}, ${1 + heroProgress * 0.1}, 1)
                `;
            }
            
            if (identityCards) {
                identityCards.style.transform = `
                    perspective(1500px) 
                    rotateY(${-15 + scrollY * 0.03}deg) 
                    rotateX(${5 + scrollY * 0.01}deg) 
                    translateZ(${heroProgress * 40}px)
                `;
            }
        }
        
        // 3D Navigation transformation
        const nav = document.querySelector('.main-nav');
        if (nav) {
            const navProgress = Math.min(1, scrollY / 200);
            nav.style.transform = `
                perspective(1000px) 
                rotateX(${navProgress * -2}deg) 
                translateZ(${navProgress * 10}px)
            `;
            nav.style.backdropFilter = `blur(${navProgress * 10}px) saturate(${1 + navProgress * 0.5})`;
        }
    }
    
    // Smooth scroll listener with requestAnimationFrame
    let ticking = false;
    function scrollListener() {
        if (!ticking) {
            requestAnimationFrame(() => {
                handle3DScroll();
                ticking = false;
            });
            ticking = true;
        }
    }
    
    window.addEventListener('scroll', scrollListener);
    handle3DScroll(); // Initial call
}

// Enhanced Mouse Parallax Effect with Advanced 3D
function initMouseParallax() {
    const hero = document.querySelector('.hero');
    if (!hero) return;
    
    // Advanced 3D mouse tracking
    hero.addEventListener('mousemove', throttle((e) => {
        const { clientX, clientY } = e;
        const { innerWidth, innerHeight } = window;
        
        const xPos = (clientX / innerWidth - 0.5) * 60;
        const yPos = (clientY / innerHeight - 0.5) * 60;
        
        const heroContent = document.querySelector('.hero-content');
        const heroVisual = document.querySelector('.hero-visual');
        const heroPattern = document.querySelector('.hero-pattern');
        const identityCards = document.querySelector('.identity-cards');
        const cards = document.querySelectorAll('.card');
        
        if (heroContent) {
            heroContent.style.transform = `
                translate3d(${xPos * 0.5}px, ${yPos * 0.5}px, 20px) 
                rotateY(${xPos * 0.1}deg) 
                rotateX(${-yPos * 0.1}deg)
            `;
        }
        
        if (heroVisual) {
            heroVisual.style.transform = `
                translate3d(${xPos * -0.3}px, ${yPos * -0.3}px, 30px) 
                perspective(1500px) 
                rotateY(${xPos * 0.15}deg) 
                rotateX(${-yPos * 0.1}deg)
            `;
        }
        
        if (heroPattern) {
            heroPattern.style.transform = `
                translate3d(${xPos * -0.1}px, ${yPos * -0.1}px, 10px) 
                rotateZ(${xPos * 0.02}deg)
            `;
        }
        
        if (identityCards) {
            identityCards.style.transform = `
                perspective(1500px) 
                rotateY(${-15 + xPos * 0.2}deg) 
                rotateX(${5 + yPos * 0.1}deg)
                translateZ(${Math.abs(xPos) * 0.5}px)
            `;
        }
        
        // Individual card 3D effects
        cards.forEach((card, index) => {
            const cardXOffset = (index % 2 === 0) ? xPos * 0.3 : xPos * -0.2;
            const cardYOffset = yPos * 0.2;
            const cardZOffset = 20 + Math.abs(xPos) * 0.3;
            
            card.style.transform = `
                translateZ(${cardZOffset}px) 
                rotateY(${5 + cardXOffset * 0.1}deg) 
                rotateX(${cardYOffset * 0.05}deg)
                translateX(${cardXOffset}px)
                translateY(${cardYOffset}px)
            `;
        });
        
    }, 16));
    
    hero.addEventListener('mouseleave', () => {
        const elements = [
            document.querySelector('.hero-content'),
            document.querySelector('.hero-visual'),
            document.querySelector('.hero-pattern'),
            document.querySelector('.identity-cards')
        ];
        
        elements.forEach(element => {
            if (element) {
                element.style.transform = '';
                element.style.transition = 'transform 0.8s cubic-bezier(0.4, 0, 0.2, 1)';
                setTimeout(() => {
                    element.style.transition = '';
                }, 800);
            }
        });
        
        // Reset individual cards
        const cards = document.querySelectorAll('.card');
        cards.forEach(card => {
            card.style.transform = '';
            card.style.transition = 'transform 0.8s cubic-bezier(0.4, 0, 0.2, 1)';
            setTimeout(() => {
                card.style.transition = '';
            }, 800);
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

// Enhanced Smooth Scrolling for All Internal Links
function initSmoothScrolling() {
    // Select all links that start with # (including nav links and hero buttons)
    const internalLinks = document.querySelectorAll('a[href^="#"]:not([href="#"])');
    
    internalLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            
            const targetId = link.getAttribute('href');
            const targetElement = document.querySelector(targetId);
            
            if (targetElement) {
                const headerHeight = document.querySelector('.header').offsetHeight;
                const targetPosition = targetElement.offsetTop - headerHeight - 30;
                
                // Enhanced scroll with easing
                smoothScrollTo(targetPosition, 800);
                
                // Visual feedback based on element type
                if (link.classList.contains('nav-link')) {
                    // Nav link feedback
                    link.style.transform = 'scale(0.95)';
                    link.style.background = 'rgba(255, 255, 255, 0.1)';
                    
                    setTimeout(() => {
                        link.style.transform = 'scale(1)';
                        link.style.background = '';
                    }, 150);
                } else if (link.classList.contains('btn')) {
                    // Button feedback
                    link.style.transform = 'scale(0.96)';
                    link.style.filter = 'brightness(0.9)';
                    
                    setTimeout(() => {
                        link.style.transform = 'scale(1)';
                        link.style.filter = '';
                    }, 200);
                } else {
                    // Generic link feedback
                    link.style.opacity = '0.7';
                    setTimeout(() => {
                        link.style.opacity = '';
                    }, 150);
                }
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

// Advanced Interactive Animations with Debouncing
function initInteractiveAnimations() {
    // Enhanced identity cards animation
    const identityCards = document.querySelectorAll('.card');
    identityCards.forEach((card, index) => {
        let animationTimeout = null;
        
        const debouncedMouseEnter = debounce(() => {
            card.style.animationPlayState = 'paused';
            card.style.transform = 'translateY(-15px) scale(1.05) rotateY(5deg)';
            card.style.boxShadow = '0 20px 40px rgba(0, 0, 0, 0.3)';
            card.style.filter = 'brightness(1.1)';
        }, 50);
        
        const debouncedMouseLeave = debounce(() => {
            card.style.animationPlayState = 'running';
            card.style.transform = '';
            card.style.boxShadow = '';
            card.style.filter = '';
        }, 50);
        
        card.addEventListener('mouseenter', debouncedMouseEnter);
        card.addEventListener('mouseleave', debouncedMouseLeave);
    });
    
    // Enhanced button interactions with ripple effect
    const buttons = document.querySelectorAll('.btn');
    buttons.forEach(button => {
        button.addEventListener('click', function(e) {
            createRippleEffect(e, this);
        });
        
        const debouncedButtonEnter = debounce(() => {
            const icon = button.querySelector('svg');
            if (icon) {
                icon.style.transform = 'translateX(5px) scale(1.1) rotate(5deg)';
            }
        }, 30);
        
        const debouncedButtonLeave = debounce(() => {
            const icon = button.querySelector('svg');
            if (icon) {
                icon.style.transform = 'translateX(0) scale(1) rotate(0deg)';
            }
        }, 30);
        
        button.addEventListener('mouseenter', debouncedButtonEnter);
        button.addEventListener('mouseleave', debouncedButtonLeave);
    });
    
    // Interactive feature icons with magnetic effect
    const featureIcons = document.querySelectorAll('.feature-icon');
    featureIcons.forEach(icon => {
        const debouncedIconEnter = debounce(() => {
            icon.style.transform = 'scale(1.2) rotate(15deg)';
            icon.style.boxShadow = '0 10px 25px rgba(0, 0, 0, 0.2)';
        }, 50);
        
        const debouncedIconLeave = debounce(() => {
            icon.style.transform = 'scale(1) rotate(0deg)';
            icon.style.boxShadow = '';
        }, 50);
        
        icon.addEventListener('mouseenter', debouncedIconEnter);
        icon.addEventListener('mouseleave', debouncedIconLeave);
    });
    
    // Endpoint cards with precise hover detection
    const endpointCards = document.querySelectorAll('.endpoint-card');
    endpointCards.forEach((card, index) => {
        let isHovering = false;
        let hoverTimeout = null;
        
        function isMouseInCard(e) {
            const rect = card.getBoundingClientRect();
            const margin = 2;
            
            return (
                e.clientX >= rect.left + margin &&
                e.clientX <= rect.right - margin &&
                e.clientY >= rect.top + margin &&
                e.clientY <= rect.bottom - margin
            );
        }
        
        function startAnimation() {
            if (isHovering) return;
            isHovering = true;
            
            const icon = card.querySelector('.endpoint-icon');
            if (icon) {
                icon.style.transition = 'transform 0.3s ease-out';
                icon.style.transform = 'scale(1.1) rotate(-5deg)';
            }
            
            // Subtle shimmer effect
            card.style.background = 'linear-gradient(45deg, white 25%, rgba(255,255,255,0.9) 50%, white 75%)';
            card.style.backgroundSize = '200% 100%';
            card.style.animation = 'shimmer 1.5s ease-in-out infinite';
        }
        
        function stopAnimation() {
            isHovering = false;
            
            const icon = card.querySelector('.endpoint-icon');
            if (icon) {
                icon.style.transition = 'transform 0.3s ease-out';
                icon.style.transform = 'scale(1) rotate(0deg)';
            }
            
            card.style.background = 'white';
            card.style.animation = '';
        }
        
        card.addEventListener('mouseenter', (e) => {
            clearTimeout(hoverTimeout);
            
            if (isMouseInCard(e)) {
                hoverTimeout = setTimeout(startAnimation, 100);
            }
        }, { passive: true });
        
        card.addEventListener('mouseleave', (e) => {
            clearTimeout(hoverTimeout);
            hoverTimeout = setTimeout(stopAnimation, 50);
        }, { passive: true });
        
        window.addEventListener('beforeunload', () => {
            clearTimeout(hoverTimeout);
        });
    });
    
    // Documentation cards with precise hover detection and isolation
    const docCards = document.querySelectorAll('.doc-card');
    docCards.forEach((card, index) => {
        let isHovering = false;
        let hoverTimeout = null;
        
        function isMouseInCard(e) {
            // Get fresh bounding rect to avoid stale coordinates
            const rect = card.getBoundingClientRect();
            const margin = 2; // Very small margin to account for border
            
            return (
                e.clientX >= rect.left + margin &&
                e.clientX <= rect.right - margin &&
                e.clientY >= rect.top + margin &&
                e.clientY <= rect.bottom - margin
            );
        }
        
        function startAnimation() {
            if (isHovering) return;
            isHovering = true;
            
            const icon = card.querySelector('.doc-icon');
            if (icon) {
                icon.style.transition = 'transform 0.3s ease-out';
                icon.style.transform = 'scale(1.05) rotate(5deg)';
                icon.style.animation = 'pulse3D 1.5s ease-in-out infinite';
            }
        }
        
        function stopAnimation() {
            isHovering = false;
            
            const icon = card.querySelector('.doc-icon');
            if (icon) {
                icon.style.transition = 'transform 0.3s ease-out';
                icon.style.transform = '';
                icon.style.animation = '';
            }
        }
        
        // Use only mouseenter and mouseleave for cleaner detection
        card.addEventListener('mouseenter', (e) => {
            clearTimeout(hoverTimeout);
            
            // Double-check that we're actually inside the card
            if (isMouseInCard(e)) {
                hoverTimeout = setTimeout(startAnimation, 100);
            }
        }, { passive: true });
        
        card.addEventListener('mouseleave', (e) => {
            clearTimeout(hoverTimeout);
            
            // Immediate stop when leaving
            hoverTimeout = setTimeout(stopAnimation, 50);
        }, { passive: true });
        
        // Clean up on page unload
        window.addEventListener('beforeunload', () => {
            clearTimeout(hoverTimeout);
        });
    });
}

// Advanced Animations with 3D Touch Gestures
function initAdvancedAnimations() {
    // Floating elements animation
    const floatingElements = document.querySelectorAll('.hero-badge, .identity-cards');
    floatingElements.forEach((element, index) => {
        element.style.animation = `float3D 8s ease-in-out infinite ${index * 2}s`;
    });
    
    // Staggered text animation for hero title
    const heroTitle = document.querySelector('.hero-title');
    if (heroTitle) {
        const text = heroTitle.innerHTML;
        const words = text.split(' ');
        heroTitle.innerHTML = words.map((word, index) => 
            `<span class="word" style="animation-delay: ${index * 0.1}s; transform-style: preserve-3d;">${word}</span>`
        ).join(' ');
    }
    
    // Enhanced 3D parallax background patterns
    const patterns = document.querySelectorAll('.hero-pattern::before, .hero-pattern::after');
    patterns.forEach((pattern, index) => {
        pattern.style.animation = `rotate3D ${25 + index * 5}s linear infinite`;
    });
    
    // Interactive navigation links with 3D effects
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('mouseenter', () => {
            link.style.transform = 'translateY(-2px) translateZ(10px) rotateX(5deg) scale(1.05)';
            link.style.textShadow = '0 4px 8px rgba(0, 0, 0, 0.3)';
            link.style.transformStyle = 'preserve-3d';
        });
        
        link.addEventListener('mouseleave', () => {
            link.style.transform = 'translateY(0) translateZ(0) rotateX(0) scale(1)';
            link.style.textShadow = '';
        });
    });
    
    // Touch gesture support for 3D interactions
    initTouchGestures();
}

// Touch Gestures for 3D Effects on Mobile
function initTouchGestures() {
    const hero = document.querySelector('.hero');
    if (!hero) return;
    
    let initialTouch = null;
    let currentRotationY = 0;
    let currentRotationX = 0;
    
    hero.addEventListener('touchstart', (e) => {
        if (e.touches.length === 1) {
            initialTouch = {
                x: e.touches[0].clientX,
                y: e.touches[0].clientY
            };
        }
    }, { passive: true });
    
    hero.addEventListener('touchmove', throttle((e) => {
        if (e.touches.length === 1 && initialTouch) {
            const touch = e.touches[0];
            const deltaX = touch.clientX - initialTouch.x;
            const deltaY = touch.clientY - initialTouch.y;
            
            const rotationY = deltaX * 0.2;
            const rotationX = -deltaY * 0.1;
            
            const heroContent = document.querySelector('.hero-content');
            const identityCards = document.querySelector('.identity-cards');
            
            if (heroContent) {
                heroContent.style.transform = `
                    perspective(1500px) 
                    rotateY(${rotationY}deg) 
                    rotateX(${rotationX}deg) 
                    translateZ(20px)
                `;
            }
            
            if (identityCards) {
                identityCards.style.transform = `
                    perspective(1500px) 
                    rotateY(${-15 + rotationY * 0.5}deg) 
                    rotateX(${5 + rotationX * 0.3}deg) 
                    translateZ(30px)
                `;
            }
        }
    }, 16), { passive: true });
    
    hero.addEventListener('touchend', () => {
        initialTouch = null;
        const heroContent = document.querySelector('.hero-content');
        const identityCards = document.querySelector('.identity-cards');
        
        [heroContent, identityCards].forEach(element => {
            if (element) {
                element.style.transition = 'transform 0.8s cubic-bezier(0.4, 0, 0.2, 1)';
                element.style.transform = '';
                setTimeout(() => {
                    element.style.transition = '';
                }, 800);
            }
        });
    }, { passive: true });
    
    // Pinch-to-zoom for 3D scale effect
    let initialDistance = 0;
    let currentScale = 1;
    
    hero.addEventListener('touchstart', (e) => {
        if (e.touches.length === 2) {
            initialDistance = getDistance(e.touches[0], e.touches[1]);
        }
    }, { passive: true });
    
    hero.addEventListener('touchmove', throttle((e) => {
        if (e.touches.length === 2) {
            const currentDistance = getDistance(e.touches[0], e.touches[1]);
            const scale = currentDistance / initialDistance;
            
            const heroVisual = document.querySelector('.hero-visual');
            if (heroVisual) {
                heroVisual.style.transform = `
                    perspective(1500px) 
                    scale3d(${scale}, ${scale}, ${scale}) 
                    translateZ(${(scale - 1) * 50}px)
                `;
            }
        }
    }, 16), { passive: true });
    
    hero.addEventListener('touchend', (e) => {
        if (e.touches.length < 2) {
            const heroVisual = document.querySelector('.hero-visual');
            if (heroVisual) {
                heroVisual.style.transition = 'transform 0.6s ease-out';
                heroVisual.style.transform = '';
                setTimeout(() => {
                    heroVisual.style.transition = '';
                }, 600);
            }
        }
    }, { passive: true });
}

function getDistance(touch1, touch2) {
    const dx = touch2.clientX - touch1.clientX;
    const dy = touch2.clientY - touch1.clientY;
    return Math.sqrt(dx * dx + dy * dy);
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