/**
 * Enhanced Interactive Effects & Scroll Animations
 * Shared across ACEAS and CPDS platforms
 */

class InteractiveEffects {
  constructor() {
    this.scrollProgress = null;
    this.init();
  }

  init() {
    this.createScrollProgress();
    this.initScrollAnimations();
    this.initParallaxEffects();
    this.initInteractiveCards();
    this.initFloatingAnimations();
    this.initMagneticButtons();
  }

  /**
   * Create and manage scroll progress bar
   */
  createScrollProgress() {
    // Create scroll progress bar
    this.scrollProgress = document.createElement('div');
    this.scrollProgress.className = 'scroll-progress';
    document.body.appendChild(this.scrollProgress);

    // Update progress on scroll
    window.addEventListener('scroll', () => {
      const scrollTop = window.pageYOffset;
      const documentHeight = document.documentElement.scrollHeight - window.innerHeight;
      const scrollPercent = (scrollTop / documentHeight) * 100;
      this.scrollProgress.style.width = Math.min(scrollPercent, 100) + '%';
    });
  }

  /**
   * Initialize scroll-triggered animations
   */
  initScrollAnimations() {
    const observerOptions = {
      threshold: 0.1,
      rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
        }
      });
    }, observerOptions);

    // Observe elements with scroll animation classes
    const animatedElements = document.querySelectorAll(
      '.scroll-fade-in, .scroll-slide-left, .scroll-slide-right, .scroll-scale-up'
    );
    
    animatedElements.forEach(el => observer.observe(el));
  }

  /**
   * Initialize parallax effects
   */
  initParallaxEffects() {
    const parallaxElements = document.querySelectorAll('.parallax-element');
    
    window.addEventListener('scroll', () => {
      const scrollTop = window.pageYOffset;
      
      parallaxElements.forEach(element => {
        const rate = scrollTop * -0.3;
        element.style.transform = `translate3d(0, ${rate}px, 0)`;
      });
    });
  }

  /**
   * Initialize interactive card effects
   */
  initInteractiveCards() {
    const cards = document.querySelectorAll('.interactive-card, .info-card, .dashboard-item');
    
    cards.forEach(card => {
      card.addEventListener('mouseenter', (e) => {
        this.createRippleEffect(e, card);
      });

      // Add tilt effect on mouse move
      card.addEventListener('mousemove', (e) => {
        this.applyTiltEffect(e, card);
      });

      card.addEventListener('mouseleave', () => {
        card.style.transform = '';
      });
    });
  }

  /**
   * Create ripple effect on card hover
   */
  createRippleEffect(event, element) {
    const rect = element.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    
    const ripple = document.createElement('div');
    ripple.style.cssText = `
      position: absolute;
      width: 20px;
      height: 20px;
      background: rgba(255, 255, 255, 0.3);
      border-radius: 50%;
      transform: scale(0);
      animation: ripple-expand 0.6s ease-out;
      left: ${x - 10}px;
      top: ${y - 10}px;
      pointer-events: none;
      z-index: 10;
    `;
    
    element.style.position = 'relative';
    element.appendChild(ripple);
    
    setTimeout(() => ripple.remove(), 600);
  }

  /**
   * Apply tilt effect based on mouse position
   */
  applyTiltEffect(event, element) {
    const rect = element.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    const centerX = rect.width / 2;
    const centerY = rect.height / 2;
    
    const rotateX = (y - centerY) / centerY * -10;
    const rotateY = (x - centerX) / centerX * 10;
    
    element.style.transform = `
      perspective(1000px) 
      rotateX(${rotateX}deg) 
      rotateY(${rotateY}deg)
      scale3d(1.02, 1.02, 1.02)
    `;
  }

  /**
   * Initialize floating animations for specific elements
   */
  initFloatingAnimations() {
    // Add floating animation to hero elements
    const floatingElements = document.querySelectorAll(
      '.hero-badge, .logo-3d, .dashboard-preview'
    );
    
    floatingElements.forEach(element => {
      element.classList.add('float-animation');
    });
  }

  /**
   * Initialize magnetic button effects
   */
  initMagneticButtons() {
    const magneticButtons = document.querySelectorAll(
      '.auth-btn, .btn-hero-primary, .btn-hero-secondary, .theme-btn-3d'
    );
    
    magneticButtons.forEach(button => {
      button.classList.add('magnetic-btn');
      
      button.addEventListener('mousemove', (e) => {
        const rect = button.getBoundingClientRect();
        const x = e.clientX - rect.left - rect.width / 2;
        const y = e.clientY - rect.top - rect.height / 2;
        
        const moveX = x * 0.2;
        const moveY = y * 0.2;
        
        button.style.transform = `translate(${moveX}px, ${moveY}px)`;
      });
      
      button.addEventListener('mouseleave', () => {
        button.style.transform = '';
      });
    });
  }

  /**
   * Add enhanced loading overlay effects
   */
  static enhanceLoadingOverlay() {
    const overlay = document.getElementById('loading-overlay');
    if (!overlay) return;

    // Add particle effects to loading overlay
    const particleContainer = document.createElement('div');
    particleContainer.className = 'loading-particles';
    particleContainer.style.cssText = `
      position: absolute;
      inset: 0;
      pointer-events: none;
      z-index: 1;
    `;

    // Create floating particles
    for (let i = 0; i < 20; i++) {
      const particle = document.createElement('div');
      particle.style.cssText = `
        position: absolute;
        width: ${Math.random() * 4 + 2}px;
        height: ${Math.random() * 4 + 2}px;
        background: rgba(255, 255, 255, ${Math.random() * 0.5 + 0.2});
        border-radius: 50%;
        left: ${Math.random() * 100}%;
        top: ${Math.random() * 100}%;
        animation: particle-float ${Math.random() * 3 + 2}s ease-in-out infinite;
        animation-delay: ${Math.random() * 2}s;
      `;
      particleContainer.appendChild(particle);
    }

    overlay.insertBefore(particleContainer, overlay.firstChild);
  }

  /**
   * Smooth scroll to element
   */
  static smoothScrollTo(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
      element.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
    }
  }

  /**
   * Create toast notification with enhanced animations
   */
  static showToast(message, type = 'success', duration = 3000) {
    const toast = document.createElement('div');
    toast.className = `toast-3d ${type}`;
    toast.innerHTML = `
      <div class="toast-content-3d">
        <svg class="toast-icon-3d" fill="currentColor" viewBox="0 0 20 20">
          ${type === 'success' 
            ? '<path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>'
            : '<path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>'
          }
        </svg>
        <span class="toast-message-3d">${message}</span>
      </div>
    `;

    document.body.appendChild(toast);
    
    // Trigger show animation
    requestAnimationFrame(() => {
      toast.classList.add('show');
    });

    // Auto remove
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }
}

// Add CSS for dynamic animations
const dynamicStyles = document.createElement('style');
dynamicStyles.textContent = `
  @keyframes ripple-expand {
    to {
      transform: scale(4);
      opacity: 0;
    }
  }

  @keyframes particle-float {
    0%, 100% {
      transform: translateY(0px) rotate(0deg);
      opacity: 0.3;
    }
    50% {
      transform: translateY(-20px) rotate(180deg);
      opacity: 0.8;
    }
  }

  .loading-particles {
    overflow: hidden;
  }

  .magnetic-btn {
    transition: transform 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  }

  .toast-3d.success .toast-icon-3d {
    color: var(--success, #10B981);
  }

  .toast-3d.error .toast-icon-3d {
    color: var(--error, #EF4444);
  }
`;
document.head.appendChild(dynamicStyles);

// Auto-initialize when DOM is loaded
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    new InteractiveEffects();
  });
} else {
  new InteractiveEffects();
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
  module.exports = InteractiveEffects;
}