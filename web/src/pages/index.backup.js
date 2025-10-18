import '../styles/style.css'
import '../styles/eservice.css'
import './eservice.js'
import '../styles/common.scss'
import 'htmx.org'

// Configure HTMX for smooth transitions and initialize enhanced interactions
document.addEventListener('DOMContentLoaded', function() {
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
      if (isDark) {
        currentIcon.innerHTML = '<path fill-rule="evenodd" d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z" clip-rule="evenodd"></path>';
      } else {
        currentIcon.innerHTML = '<path fill-rule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0z" clip-rule="evenodd"></path>';
      }
    });
  }

  // Load saved theme
  if (localStorage.getItem('theme') === 'dark') {
    document.documentElement.classList.add('dark');
    const themeIcon = document.querySelector('#theme-toggle svg');
    if (themeIcon) {
      themeIcon.innerHTML = '<path fill-rule="evenodd" d="M17.293 13.293A8 8 0 716.707 2.707a8.001 8.001 0 1010.586 10.586z" clip-rule="evenodd"></path>';
    }
  }

  // Fix service button interactions
  setTimeout(() => {
    const serviceButtons = document.querySelectorAll('.service-btn');
    console.log('Found service buttons:', serviceButtons.length);
    
    serviceButtons.forEach((btn, index) => {
      // Ensure buttons are properly clickable
      btn.style.cursor = 'pointer';
      btn.style.userSelect = 'none';
      btn.style.position = 'relative';
      btn.style.zIndex = '10';
      
      // Remove any conflicting event handlers
      btn.addEventListener('click', function(e) {
        e.stopPropagation();
        console.log(`Service button ${index + 1} clicked:`, this.href);
        
        // Add visual feedback
        this.style.transform = 'scale(0.98)';
        setTimeout(() => {
          this.style.transform = '';
        }, 150);
      }, { passive: false });

      // Enhanced hover effects
      btn.addEventListener('mouseenter', function() {
        this.style.transform = 'translateY(-2px)';
      });

      btn.addEventListener('mouseleave', function() {
        this.style.transform = '';
      });
    });
  }, 100);

  console.log('Enhanced eService portal initialized');
});


// Theme toggle functionality with government portal styling
const themeToggle = document.getElementById('theme-toggle')
const html = document.documentElement

// Check for saved theme preference or default to light mode
const savedTheme = localStorage.getItem('theme') || 'light'
html.classList.toggle('dark', savedTheme === 'dark')

themeToggle.addEventListener('click', () => {
  const isDark = html.classList.contains('dark')
  html.classList.toggle('dark', !isDark)
  localStorage.setItem('theme', isDark ? 'light' : 'dark')
})

// Government portal enhancements
document.addEventListener('DOMContentLoaded', () => {
  // Add subtle animations to service cards
  const serviceCards = document.querySelectorAll('.group')
  serviceCards.forEach(card => {
    card.addEventListener('mouseenter', () => {
      card.style.transform = 'translateY(-2px)'
      card.style.transition = 'all 0.2s ease-in-out'
    })
    card.addEventListener('mouseleave', () => {
      card.style.transform = 'translateY(0)'
    })
  })

  // Add focus states for accessibility
  const links = document.querySelectorAll('a')
  links.forEach(link => {
    link.addEventListener('focus', () => {
      link.style.outline = '2px solid #3b82f6'
      link.style.outlineOffset = '2px'
    })
    link.addEventListener('blur', () => {
      link.style.outline = 'none'
    })
  })

  // Add loading state simulation for service access
  const serviceButtons = document.querySelectorAll('a[href^="/"]')
  serviceButtons.forEach(button => {
    button.addEventListener('click', (e) => {
      e.preventDefault()
      const href = button.getAttribute('href')
      
      // Add smooth transition effect
      document.body.style.opacity = '0'
      document.body.style.transform = 'translateY(-10px)'
      
      setTimeout(() => {
        window.location.href = href
      }, 150)
    })
  })
})

console.log('🏛️ Singapore Government eServices Portal - SSO Playground')
console.log('🔐 Authentication systems ready for testing')
console.log('📚 Learning environment initialized')

// Add smooth page entry animation
document.addEventListener('DOMContentLoaded', () => {
  document.body.style.opacity = '0'
  document.body.style.transform = 'translateY(10px)'
  
  setTimeout(() => {
    document.body.style.transition = 'opacity 300ms ease-out, transform 300ms ease-out'
    document.body.style.opacity = '1'
    document.body.style.transform = 'translateY(0)'
  }, 50)
})
