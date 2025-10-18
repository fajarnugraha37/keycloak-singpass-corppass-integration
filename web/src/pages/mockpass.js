// Smooth scrolling for navigation links
document.addEventListener('DOMContentLoaded', function() {
    // Smooth scroll for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Copy to clipboard functionality
    document.querySelectorAll('.copy-btn').forEach(button => {
        button.addEventListener('click', async function() {
            const url = this.dataset.url;
            const fullUrl = window.location.origin + url;
            
            try {
                await navigator.clipboard.writeText(fullUrl);
                showToast('URL copied to clipboard!');
            } catch (err) {
                // Fallback for older browsers
                const textArea = document.createElement('textarea');
                textArea.value = fullUrl;
                textArea.style.position = 'fixed';
                textArea.style.opacity = '0';
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
                showToast('URL copied to clipboard!');
            }
        });
    });

    // Intersection Observer for animations
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, observerOptions);

    // Observe elements for scroll animations
    document.querySelectorAll('.feature-card, .endpoint-card, .doc-card').forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(30px)';
        el.style.transition = 'opacity 0.6s ease-out, transform 0.6s ease-out';
        observer.observe(el);
    });

    // Parallax effect for hero pattern
    let ticking = false;
    
    function updateParallax() {
        const scrolled = window.pageYOffset;
        const parallaxElement = document.querySelector('.hero-pattern');
        
        if (parallaxElement) {
            const rate = scrolled * -0.5;
            parallaxElement.style.transform = `translateY(${rate}px)`;
        }
        
        ticking = false;
    }

    function requestTick() {
        if (!ticking) {
            requestAnimationFrame(updateParallax);
            ticking = true;
        }
    }

    window.addEventListener('scroll', requestTick);

    // Dynamic navbar background on scroll
    const header = document.querySelector('.header');
    let lastScrollTop = 0;

    window.addEventListener('scroll', function() {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        
        if (scrollTop > 100) {
            header.style.backgroundColor = 'rgba(228, 0, 43, 0.95)';
            header.style.backdropFilter = 'blur(10px)';
        } else {
            header.style.backgroundColor = '';
            header.style.backdropFilter = '';
        }

        // Hide/show header on scroll
        if (scrollTop > lastScrollTop && scrollTop > 200) {
            header.style.transform = 'translateY(-100%)';
        } else {
            header.style.transform = 'translateY(0)';
        }
        
        lastScrollTop = scrollTop;
    });

    // Add transition to header
    header.style.transition = 'transform 0.3s ease-in-out, background-color 0.3s ease-in-out';

    // Animate cards on hover
    document.querySelectorAll('.card').forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-8px) scale(1.02) rotateY(-15deg)';
        });
        
        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) scale(1) rotateY(-15deg)';
        });
    });

    // Status indicator animation
    document.querySelectorAll('.endpoint-status').forEach(status => {
        if (status.textContent.trim() === 'Active') {
            setInterval(() => {
                status.style.boxShadow = '0 0 20px rgba(0, 170, 68, 0.5)';
                setTimeout(() => {
                    status.style.boxShadow = 'none';
                }, 1000);
            }, 3000);
        }
    });

    // Enhanced button interactions
    document.querySelectorAll('.btn').forEach(btn => {
        btn.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-2px)';
        });
        
        btn.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
        });
        
        btn.addEventListener('mousedown', function() {
            this.style.transform = 'translateY(0) scale(0.98)';
        });
        
        btn.addEventListener('mouseup', function() {
            this.style.transform = 'translateY(-2px) scale(1)';
        });
    });

    // Loading animation for external links
    document.querySelectorAll('.endpoint-link').forEach(link => {
        link.addEventListener('click', function(e) {
            const icon = this.querySelector('svg');
            if (icon) {
                icon.style.animation = 'spin 0.5s linear';
                setTimeout(() => {
                    icon.style.animation = '';
                }, 500);
            }
        });
    });

    // Keyboard navigation support
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            hideToast();
        }
    });

    // Performance optimization: Reduce animations on mobile
    if (window.innerWidth < 768) {
        document.documentElement.style.setProperty('--transition-normal', '0.15s ease-in-out');
        document.documentElement.style.setProperty('--transition-slow', '0.2s ease-in-out');
    }
});

// Toast notification system
function showToast(message) {
    const toast = document.getElementById('toast');
    const messageElement = toast.querySelector('.toast-message');
    
    messageElement.textContent = message;
    toast.classList.add('show');
    
    // Auto hide after 3 seconds
    setTimeout(() => {
        hideToast();
    }, 3000);
}

function hideToast() {
    const toast = document.getElementById('toast');
    toast.classList.remove('show');
}

// Utility function for debouncing scroll events
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

// Enhanced scroll effects with debouncing
const debouncedScrollHandler = debounce(function() {
    // Add any additional scroll-based animations here
    updateCardVisibility();
}, 10);

window.addEventListener('scroll', debouncedScrollHandler);

// Card visibility animation
function updateCardVisibility() {
    const cards = document.querySelectorAll('.feature-card, .endpoint-card, .doc-card');
    const windowHeight = window.innerHeight;
    
    cards.forEach(card => {
        const cardTop = card.getBoundingClientRect().top;
        const cardVisible = cardTop < windowHeight * 0.8;
        
        if (cardVisible && !card.classList.contains('visible')) {
            card.classList.add('visible');
        }
    });
}

// Add CSS animation for card visibility
const style = document.createElement('style');
style.textContent = `
    @keyframes spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
    }
    
    .visible {
        animation: slideInUp 0.6s ease-out forwards;
    }
    
    @media (prefers-reduced-motion: reduce) {
        *, *::before, *::after {
            animation-duration: 0.01ms !important;
            animation-iteration-count: 1 !important;
            transition-duration: 0.01ms !important;
        }
    }
`;
document.head.appendChild(style);

// Error handling for external links
document.querySelectorAll('a[href^="http"], a[href^="/"]').forEach(link => {
    link.addEventListener('error', function() {
        showToast('Unable to load the resource. Please try again later.');
    });
});

// Service worker registration for offline support (if available)
if ('serviceWorker' in navigator) {
    window.addEventListener('load', function() {
        // Note: Implement service worker if needed for offline functionality
        console.log('MockPass Identity Provider loaded successfully');
    });
}

// Analytics or tracking (placeholder)
function trackEvent(eventName, properties = {}) {
    // Implement analytics tracking here if needed
    console.log(`Event: ${eventName}`, properties);
}

// Track button clicks for analytics
document.querySelectorAll('.btn, .endpoint-link').forEach(element => {
    element.addEventListener('click', function() {
        trackEvent('button_click', {
            text: this.textContent.trim(),
            href: this.getAttribute('href') || 'none'
        });
    });
});