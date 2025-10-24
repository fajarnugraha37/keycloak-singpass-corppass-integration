import './__styles__/style.css'
import './__styles__/common.scss'
import './index.animation.js'
import 'htmx.org'

// Enhanced eService Portal JavaScript - Interactions Only
document.addEventListener('DOMContentLoaded', function () {
  console.log('🏛️ Enhanced eService Portal Loading...');

  // Configure HTMX settings
  if (window.htmx) {
    htmx.config.defaultSwapStyle = 'outerHTML';
    htmx.config.defaultSwapDelay = 100;
    htmx.config.defaultSettleDelay = 200;
  }

  // Initialize dark mode toggle
  const themeToggle = document.getElementById('theme-toggle');
  if (themeToggle) {
    themeToggle.addEventListener('click', function () {
      document.documentElement.classList.toggle('dark');
      const isDark = document.documentElement.classList.contains('dark');
      localStorage.setItem('theme', isDark ? 'dark' : 'light');

      // Update icon
      const currentIcon = this.querySelector('svg');
      if (currentIcon) {
        if (isDark) {
          currentIcon.innerHTML = '<path fill-rule="evenodd" d="M17.293 13.293A8 8 0 0 1 6.707 2.707a8.001 8.001 0 1 0 10.586 10.586z" clip-rule="evenodd"></path>';
        } else {
          currentIcon.innerHTML = '<path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v1a1 1 0 1 1-2 0V3a1 1 0 0 1 1-1zm4 8a4 4 0 1 1-8 0 4 4 0 0 1 8 0zm-.464 4.95l.707.707a1 1 0 0 0 1.414-1.414l-.707-.707a1 1 0 0 0-1.414 1.414zm2.12-10.607a1 1 0 0 1 0 1.414l-.706.707a1 1 0 1 1-1.414-1.414l.707-.707a1 1 0 0 1 1.414 0z" clip-rule="evenodd"></path>';
        }
      }
    });
  }

  // Load saved theme on page load
  if (localStorage.getItem('theme') === 'dark') {
    document.documentElement.classList.add('dark');
    const themeIcon = document.querySelector('#theme-toggle svg');
    if (themeIcon) {
      themeIcon.innerHTML = '<path fill-rule="evenodd" d="M17.293 13.293A8 8 0 0 1 6.707 2.707a8.001 8.001 0 1 0 10.586 10.586z" clip-rule="evenodd"></path>';
    }
  }

  // Enhanced service button interactions (fix click issues)
  setTimeout(() => {
    const serviceButtons = document.querySelectorAll('.service-btn');
    console.log('🔧 Found service buttons:', serviceButtons.length);

    serviceButtons.forEach((btn, index) => {
      // Ensure buttons are properly clickable
      btn.style.cursor = 'pointer';
      btn.style.userSelect = 'none';
      btn.style.position = 'relative';
      btn.style.zIndex = '10';

      // Add click handler for enhanced feedback
      btn.addEventListener('click', function (e) {
        console.log(`🎯 Service button ${index + 1} clicked:`, this.href);

        // Add visual click feedback
        this.style.transform = 'scale(0.98)';
        setTimeout(() => {
          this.style.transform = '';
        }, 150);

        // Let the default navigation proceed
        return true;
      }, { passive: false });

      // Enhanced hover effects
      btn.addEventListener('mouseenter', function () {
        this.style.transform = 'translateY(-2px) scale(1.02)';
      });

      btn.addEventListener('mouseleave', function () {
        this.style.transform = '';
      });

      // Enhanced focus states for accessibility
      btn.addEventListener('focus', function () {
        this.style.outline = '2px solid #3b82f6';
        this.style.outlineOffset = '2px';
      });

      btn.addEventListener('blur', function () {
        this.style.outline = '';
        this.style.outlineOffset = '';
      });
    });

    // Enhanced 3D resource card interactions
    const resourceCards = document.querySelectorAll('.resource-card');
    console.log('🔧 Found resource cards:', resourceCards.length);

    resourceCards.forEach((card, index) => {
      // Add 3D tilt effect based on mouse position
      card.addEventListener('mousemove', function (e) {
        const rect = this.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const centerX = rect.width / 2;
        const centerY = rect.height / 2;
        const rotateX = (y - centerY) / centerY * -10;
        const rotateY = (x - centerX) / centerX * 10;

        this.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateZ(20px) scale(1.05)`;
      });

      card.addEventListener('mouseleave', function () {
        this.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateZ(0px) scale(1)';
      });

      // Add click ripple effect
      card.addEventListener('click', function (e) {
        createRippleEffect(e, this);
      });
    });

  }, 200); // Increased delay to ensure DOM is ready

  // Enhanced 3D Page Entrance Animation
  document.body.style.opacity = '0';
  document.body.style.transform = 'translateY(20px) rotateX(10deg)';
  document.body.style.transformStyle = 'preserve-3d';
  document.body.style.perspective = '1000px';

  setTimeout(() => {
    document.body.style.transition = 'all 800ms cubic-bezier(0.4, 0, 0.2, 1)';
    document.body.style.opacity = '1';
    document.body.style.transform = 'translateY(0) rotateX(0deg)';
  }, 100);

  // Advanced Scroll-based 3D Animations
  initializeScrollAnimations();

  // Parallax Background Effects
  initializeParallaxEffects();

  // Interactive 3D Mouse Tracking
  initializeMouseTracking();

  // Enhanced Resource Grid Animation
  initializeResourceGridAnimation();

  // Fix navigation link clickability
  setTimeout(() => {
    const navLinks = document.querySelectorAll('header nav a');
    const header = document.querySelector('header');

    // Force header to be above everything
    if (header) {
      header.style.zIndex = '9999';
      header.style.position = 'sticky';
      header.style.top = '0';
    }

    navLinks.forEach(link => {
      // Ensure links are clickable
      link.style.pointerEvents = 'auto';
      link.style.cursor = 'pointer';
      link.style.zIndex = '10000';
      link.style.position = 'relative';

      // Remove any potential overlays
      link.style.isolation = 'isolate';

      // Add click handler to ensure functionality
      link.addEventListener('click', function (e) {
        // Allow default navigation behavior
        console.log('Navigation click:', this.textContent.trim());
      });
    });

    // Fix main container z-index
    const mainContainer = document.querySelector('.min-h-screen');
    if (mainContainer) {
      mainContainer.style.zIndex = '1';
      mainContainer.style.position = 'relative';
    }

  }, 300);

  console.log('✅ Enhanced eService portal with 3D effects initialized');
  console.log('🌌 Blue sky theme and animations loaded');
  console.log('🔐 Authentication systems ready for testing');
  console.log('📚 Learning environment active');
  console.log('🔗 Navigation links enhanced for proper functionality');
});

// Enhanced 3D interaction functions
function createRippleEffect(event, element) {
  const rect = element.getBoundingClientRect();
  const ripple = document.createElement('span');
  const size = Math.max(rect.width, rect.height) * 1.5;
  const x = event.clientX - rect.left - size / 2;
  const y = event.clientY - rect.top - size / 2;

  ripple.style.cssText = `
    position: absolute;
    width: ${size}px;
    height: ${size}px;
    left: ${x}px;
    top: ${y}px;
    background: radial-gradient(circle, rgba(30, 58, 138, 0.3) 0%, rgba(59, 130, 246, 0.1) 50%, transparent 100%);
    border-radius: 50%;
    transform: scale(0) rotateZ(0deg);
    animation: enhancedRipple 0.8s cubic-bezier(0.4, 0, 0.2, 1);
    pointer-events: none;
    z-index: 100;
  `;

  element.style.position = 'relative';
  element.style.overflow = 'hidden';
  element.appendChild(ripple);

  // Add 3D tilt effect during ripple
  element.style.transform = 'perspective(1000px) rotateX(-2deg) rotateY(1deg) scale(1.02)';

  setTimeout(() => {
    element.style.transform = '';
    ripple.remove();
  }, 800);
}

// Add CSS for ripple animation
const rippleStyle = document.createElement('style');
rippleStyle.textContent = `
  @keyframes ripple {
    to {
      transform: scale(4);
      opacity: 0;
    }
  }
`;
document.head.appendChild(rippleStyle);

// Advanced 3D Animation System Functions
function initializeScrollAnimations() {
  const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -100px 0px'
  };

  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry, index) => {
      if (entry.isIntersecting) {
        setTimeout(() => {
          entry.target.classList.add('visible');
          entry.target.style.transform = 'translateY(0) scale(1) rotateX(0deg) rotateZ(0deg)';
          entry.target.style.opacity = '1';
        }, index * 100); // Staggered animation
      }
    });
  }, observerOptions);

  // Observe all elements for scroll animation
  document.querySelectorAll('.service-card, .resource-card, .alert-banner, .page-title').forEach((el, index) => {
    el.classList.add('scroll-animate');
    el.style.transform = 'translateY(60px) scale(0.8) rotateX(25deg) rotateZ(2deg)';
    el.style.opacity = '0';
    el.style.transition = `all 0.8s cubic-bezier(0.4, 0, 0.2, 1) ${index * 0.1}s`;
    el.style.transformStyle = 'preserve-3d';
    observer.observe(el);
  });
}

function initializeParallaxEffects() {
  let ticking = false;

  function updateParallax() {
    const scrolled = window.pageYOffset;
    const parallaxElements = document.querySelectorAll('[data-parallax]');

    parallaxElements.forEach(element => {
      const speed = element.dataset.parallax || 0.5;
      const yPos = -(scrolled * speed);
      element.style.transform = `translateY(${yPos}px) translateZ(0)`;
    });

    // 3D rotation effect on scroll for service cards
    const serviceCards = document.querySelectorAll('.service-card');
    serviceCards.forEach((card, index) => {
      const rect = card.getBoundingClientRect();
      const scrollPercent = Math.max(0, Math.min(1, (window.innerHeight - rect.top) / window.innerHeight));
      const rotateX = (scrollPercent - 0.5) * 10;
      const rotateY = (scrollPercent - 0.5) * 5;

      card.style.transform = `translateY(0) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateZ(${scrollPercent * 20}px)`;
    });

    ticking = false;
  }

  function requestTick() {
    if (!ticking) {
      requestAnimationFrame(updateParallax);
      ticking = true;
    }
  }

  window.addEventListener('scroll', requestTick);
  updateParallax(); // Initial call
}

function initializeMouseTracking() {
  let mouseX = 0;
  let mouseY = 0;

  document.addEventListener('mousemove', (e) => {
    mouseX = e.clientX;
    mouseY = e.clientY;

    // Add subtle 3D tilt to header based on mouse
    const header = document.querySelector('header');
    if (header) {
      const rect = header.getBoundingClientRect();
      const centerX = rect.width / 2;
      const tiltX = (mouseX - centerX) / centerX * 2;

      header.style.transform = `perspective(1000px) rotateY(${tiltX}deg)`;
    }

    // Add magnetic effect to service buttons
    const serviceButtons = document.querySelectorAll('.service-btn');
    serviceButtons.forEach(btn => {
      const rect = btn.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const distance = Math.sqrt((mouseX - centerX) ** 2 + (mouseY - centerY) ** 2);

      if (distance < 200) {
        const force = (200 - distance) / 200;
        const deltaX = (mouseX - centerX) * force * 0.1;
        const deltaY = (mouseY - centerY) * force * 0.1;

        btn.style.transform = `translate(${deltaX}px, ${deltaY}px) scale(${1 + force * 0.1}) perspective(1000px)`;
      }
    });
  });
}

function initializeResourceGridAnimation() {
  const resourceCards = document.querySelectorAll('.resource-card');

  // Add staggered entrance animation
  resourceCards.forEach((card, index) => {
    card.style.opacity = '0';
    card.style.transform = 'translateY(30px) rotateX(45deg) scale(0.8)';

    setTimeout(() => {
      card.style.transition = 'all 0.6s cubic-bezier(0.4, 0, 0.2, 1)';
      card.style.opacity = '1';
      card.style.transform = 'translateY(0) rotateX(0deg) scale(1)';
    }, 1000 + (index * 150));
  });

  // Add floating animation
  resourceCards.forEach((card, index) => {
    const delay = index * 0.5;
    card.style.animation = `subtleFloat 4s ease-in-out ${delay}s infinite`;
  });
}

// Enhanced Mouse Interaction Effects
function addMouseTrackingEffects() {
  let mouseX = 0;
  let mouseY = 0;

  document.addEventListener('mousemove', (e) => {
    mouseX = e.clientX;
    mouseY = e.clientY;

    // Add magnetic effect to service buttons
    const serviceButtons = document.querySelectorAll('.service-btn');
    serviceButtons.forEach(btn => {
      const rect = btn.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const distance = Math.sqrt((mouseX - centerX) ** 2 + (mouseY - centerY) ** 2);

      if (distance < 150) {
        const force = (150 - distance) / 150;
        const deltaX = (mouseX - centerX) * force * 0.1;
        const deltaY = (mouseY - centerY) * force * 0.1;

        btn.style.transform = `translate(${deltaX}px, ${deltaY}px) scale(${1 + force * 0.1})`;
      } else {
        btn.style.transform = '';
      }
    });
  });
}

// Initialize advanced effects
setTimeout(() => {
  addMouseTrackingEffects();
}, 1000);

// Advanced 3D Card Tilt System
function initializeAdvancedTilt() {
  document.querySelectorAll('.service-card, .resource-card').forEach(card => {
    card.addEventListener('mousemove', (e) => {
      const rect = card.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const mouseX = e.clientX - centerX;
      const mouseY = e.clientY - centerY;

      const rotateX = (mouseY / rect.height) * -15;
      const rotateY = (mouseX / rect.width) * 15;

      card.style.transform = `
        perspective(1000px) 
        rotateX(${rotateX}deg) 
        rotateY(${rotateY}deg) 
        translateZ(20px)
        scale3d(1.03, 1.03, 1.03)
      `;

      // Add inner highlight effect
      const highlight = card.querySelector('.card-highlight') || document.createElement('div');
      if (!card.querySelector('.card-highlight')) {
        highlight.className = 'card-highlight';
        highlight.style.cssText = `
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: radial-gradient(circle at ${((mouseX + rect.width / 2) / rect.width) * 100}% ${((mouseY + rect.height / 2) / rect.height) * 100}%, rgba(59, 130, 246, 0.1) 0%, transparent 50%);
          pointer-events: none;
          border-radius: 8px;
          opacity: 0;
          transition: opacity 0.3s ease;
        `;
        card.appendChild(highlight);
      }
      highlight.style.opacity = '1';
    });

    card.addEventListener('mouseleave', () => {
      card.style.transform = '';
      const highlight = card.querySelector('.card-highlight');
      if (highlight) {
        highlight.style.opacity = '0';
      }
    });
  });
}

// Enhanced Navigation Animation System - Target Main Header Navigation Only
function initializeNavigationAnimations() {
  const navLinks = document.querySelectorAll('header nav a');

  console.log('🎯 Found navigation links:', navLinks.length);

  navLinks.forEach((link, index) => {
    // Add staggered entrance animation
    link.style.opacity = '0';
    link.style.transform = 'translateY(-20px)';

    setTimeout(() => {
      link.style.transition = 'all 0.5s cubic-bezier(0.4, 0, 0.2, 1)';
      link.style.opacity = '1';
      link.style.transform = 'translateY(0)';
    }, 300 + (index * 100));

    // Enhanced hover effects with 3D animations
    link.addEventListener('mouseenter', (e) => {
      if (e.target.closest('header')) {
        e.target.style.transform = 'translateY(-3px) scale(1.05)';
        e.target.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.2)';
        e.target.style.background = 'rgba(255, 255, 255, 0.15)';
      }
    });

    link.addEventListener('mouseleave', (e) => {
      if (e.target.closest('header')) {
        e.target.style.transform = 'translateY(0) scale(1)';
        e.target.style.boxShadow = 'none';
        e.target.style.background = 'transparent';
      }
    });

    // Active state animation
    link.addEventListener('mousedown', (e) => {
      if (e.target.closest('header')) {
        e.target.style.transform = 'translateY(-1px) scale(1.02)';
      }
    });

    link.addEventListener('mouseup', (e) => {
      if (e.target.closest('header')) {
        e.target.style.transform = 'translateY(-3px) scale(1.05)';
      }
    });
  });
}

// Enhanced Theme Toggle Animation
function initializeThemeToggle() {
  const themeToggle = document.getElementById('theme-toggle');
  if (themeToggle) {
    themeToggle.addEventListener('click', (e) => {
      e.preventDefault();

      // Add rotation animation
      themeToggle.style.transform = 'rotateY(180deg)';

      setTimeout(() => {
        themeToggle.style.transform = 'rotateY(0deg)';
      }, 300);

      // Add ripple effect
      createRippleEffect(e, themeToggle);
    });
  }
}

// Service Card Enhanced Interactions
function initializeServiceCards() {
  const serviceCards = document.querySelectorAll('.service-card');

  serviceCards.forEach((card) => {
    // Add entrance animation
    card.style.opacity = '0';
    card.style.transform = 'translateY(50px) rotateX(20deg)';

    setTimeout(() => {
      card.style.transition = 'all 0.8s cubic-bezier(0.4, 0, 0.2, 1)';
      card.style.opacity = '1';
      card.style.transform = 'translateY(0) rotateX(0deg)';
    }, 600);

    // Enhanced hover interactions
    card.addEventListener('mouseenter', () => {
      card.style.transform = 'translateY(-15px) rotateX(8deg) rotateY(-3deg) scale(1.02)';
    });

    card.addEventListener('mouseleave', () => {
      card.style.transform = 'translateY(0) rotateX(0deg) rotateY(0deg) scale(1)';
    });
  });
}

// Initialize complete 3D animation system
document.addEventListener('DOMContentLoaded', () => {
  console.log('🚀 Enhanced 3D SSO Playground initialized');

  // Initialize all animation systems
  setTimeout(() => {
    initializeScrollAnimations();
    initializeParallaxEffects();
    initializeMouseTracking();
    initializeResourceGridAnimation();
    initializeAdvancedTilt();
    addMouseTrackingEffects();
    initializeNavigationAnimations();
    initializeThemeToggle();
    initializeServiceCards();

    console.log('✨ All 3D animation systems active');
    console.log('🎯 Navigation animations loaded');
    console.log('🎨 Enhanced interactions ready');

    // Debug navigation links
    const headerNavLinks = document.querySelectorAll('header nav a');
    console.log('🔍 Header nav links found:', headerNavLinks);
    headerNavLinks.forEach((link, i) => {
      console.log(`Link ${i}:`, link.textContent, link.href);

      // Ensure links are clickable
      link.style.pointerEvents = 'auto';
      link.style.zIndex = '10';

      // Add debug click handler
      link.addEventListener('click', (e) => {
        console.log('🖱️ Navigation link clicked:', link.textContent);
        // Don't prevent default - let the link work normally
      });
    });
  }, 500);

  // Add enhanced ripple effect to all interactive elements
  document.querySelectorAll('.btn, .service-btn, .resource-card').forEach(element => {
    element.addEventListener('click', (e) => {
      createRippleEffect(e, element);
    });
  });
});

// Performance monitoring and cleanup
let animationFrameId;
function optimizeAnimations() {
  const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
  if (mediaQuery.matches) {
    // Disable intensive animations for accessibility
    document.documentElement.classList.add('reduced-motion');
  }
}

// Export for debugging and development
window.eServiceDebug = {
  buttons: () => document.querySelectorAll('.service-btn'),
  cards: () => document.querySelectorAll('.resource-card'),
  particles: () => document.getElementById('particle-canvas'),
  theme: () => document.documentElement.classList.contains('dark') ? 'dark' : 'light',
  animations: {
    scroll: () => initializeScrollAnimations(),
    parallax: () => initializeParallaxEffects(),
    tilt: () => initializeAdvancedTilt(),
    mouse: () => addMouseTrackingEffects()
  },
  reinitialize: () => {
    console.log('🔄 Reinitializing 3D animation systems...');
    initializeScrollAnimations();
    initializeParallaxEffects();
    initializeAdvancedTilt();
    addMouseTrackingEffects();
  },
  performance: {
    optimize: optimizeAnimations,
    reducedMotion: () => document.documentElement.classList.contains('reduced-motion')
  }
};

console.log('🎮 Enhanced 3D Animation System Ready');
console.log('📱 Mobile-optimized effects loaded');
console.log('🎯 Advanced interaction system online');