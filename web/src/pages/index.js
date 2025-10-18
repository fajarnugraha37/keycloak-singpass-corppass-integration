import '../styles/style.css'
import '../styles/eservice.css'
import './eservice.js'
import '../styles/common.scss'
import 'htmx.org'

// Enhanced eService Portal JavaScript - Interactions Only
document.addEventListener('DOMContentLoaded', function() {
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
    themeToggle.addEventListener('click', function() {
      document.documentElement.classList.toggle('dark');
      const isDark = document.documentElement.classList.contains('dark');
      localStorage.setItem('theme', isDark ? 'dark' : 'light');
      
      // Update icon
      const currentIcon = this.querySelector('svg');
      if (currentIcon) {
        if (isDark) {
          currentIcon.innerHTML = '<path fill-rule="evenodd" d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z" clip-rule="evenodd"></path>';
        } else {
          currentIcon.innerHTML = '<path fill-rule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0z" clip-rule="evenodd"></path>';
        }
      }
    });
  }

  // Load saved theme on page load
  if (localStorage.getItem('theme') === 'dark') {
    document.documentElement.classList.add('dark');
    const themeIcon = document.querySelector('#theme-toggle svg');
    if (themeIcon) {
      themeIcon.innerHTML = '<path fill-rule="evenodd" d="M17.293 13.293A8 8 0 716.707 2.707a8.001 8.001 0 1010.586 10.586z" clip-rule="evenodd"></path>';
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
      btn.addEventListener('click', function(e) {
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
      btn.addEventListener('mouseenter', function() {
        this.style.transform = 'translateY(-2px) scale(1.02)';
      });

      btn.addEventListener('mouseleave', function() {
        this.style.transform = '';
      });

      // Enhanced focus states for accessibility
      btn.addEventListener('focus', function() {
        this.style.outline = '2px solid #3b82f6';
        this.style.outlineOffset = '2px';
      });

      btn.addEventListener('blur', function() {
        this.style.outline = '';
        this.style.outlineOffset = '';
      });
    });

    // Enhanced resource card interactions
    const resourceCards = document.querySelectorAll('.resource-card');
    console.log('🔧 Found resource cards:', resourceCards.length);
    
    resourceCards.forEach((card, index) => {
      card.addEventListener('mouseenter', function() {
        this.style.transform = 'translateY(-3px) scale(1.05)';
      });

      card.addEventListener('mouseleave', function() {
        this.style.transform = '';
      });
    });

  }, 200); // Increased delay to ensure DOM is ready

  // Enhanced page entrance animation
  document.body.style.opacity = '0';
  document.body.style.transform = 'translateY(10px)';
  
  setTimeout(() => {
    document.body.style.transition = 'opacity 400ms ease-out, transform 400ms ease-out';
    document.body.style.opacity = '1';
    document.body.style.transform = 'translateY(0)';
  }, 100);

  console.log('✅ Enhanced eService portal initialized successfully');
  console.log('🔐 Authentication systems ready for testing');
  console.log('📚 Learning environment active');
});

// Additional utility functions for enhanced interactions
function createRippleEffect(event, element) {
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
    z-index: 100;
  `;

  element.style.position = 'relative';
  element.style.overflow = 'hidden';
  element.appendChild(ripple);

  setTimeout(() => {
    ripple.remove();
  }, 600);
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

// Export for debugging
window.eServiceDebug = {
  buttons: () => document.querySelectorAll('.service-btn'),
  cards: () => document.querySelectorAll('.resource-card'),
  theme: () => document.documentElement.classList.contains('dark') ? 'dark' : 'light'
};