import "./index.css";

// ============ Space Background Canvas ============
const canvas = document.getElementById('spaceCanvas');
const ctx = canvas.getContext('2d');

function resizeCanvas() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
}
resizeCanvas();
window.addEventListener('resize', resizeCanvas);

// Create stars
const stars = [];
for (let i = 0; i < 200; i++) {
    stars.push({
        x: Math.random() * canvas.width,
        y: Math.random() * canvas.height,
        radius: Math.random() * 1.5,
        opacity: Math.random() * 0.5 + 0.5,
        twinkleSpeed: Math.random() * 0.02 + 0.01,
        twinklePhase: Math.random() * Math.PI * 2,
    });
}

// Create nebula particles
const nebulas = [];
for (let i = 0; i < 50; i++) {
    nebulas.push({
        x: Math.random() * canvas.width,
        y: Math.random() * canvas.height,
        vx: (Math.random() - 0.5) * 0.3,
        vy: (Math.random() - 0.5) * 0.3,
        radius: Math.random() * 100 + 50,
        color: ['rgba(100, 150, 255, 0.1)', 'rgba(80, 120, 255, 0.08)', 'rgba(120, 160, 255, 0.12)'][
            Math.floor(Math.random() * 3)
        ],
    });
}

let time = 0;

function animateSpace() {
    // Clear canvas
    ctx.fillStyle = 'rgba(13, 20, 40, 0.95)';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Draw nebula
    nebulas.forEach((nebula) => {
        nebula.x += nebula.vx;
        nebula.y += nebula.vy;

        if (nebula.x < -nebula.radius) nebula.x = canvas.width + nebula.radius;
        if (nebula.x > canvas.width + nebula.radius) nebula.x = -nebula.radius;
        if (nebula.y < -nebula.radius) nebula.y = canvas.height + nebula.radius;
        if (nebula.y > canvas.height + nebula.radius) nebula.y = -nebula.radius;

        const gradient = ctx.createRadialGradient(nebula.x, nebula.y, 0, nebula.x, nebula.y, nebula.radius);
        gradient.addColorStop(0, nebula.color);
        gradient.addColorStop(1, 'rgba(100, 150, 255, 0)');

        ctx.fillStyle = gradient;
        ctx.fillRect(nebula.x - nebula.radius, nebula.y - nebula.radius, nebula.radius * 2, nebula.radius * 2);
    });

    // Draw stars with twinkling
    stars.forEach((star) => {
        star.twinklePhase += star.twinkleSpeed;
        const twinkle = Math.sin(star.twinklePhase) * 0.3 + 0.7;

        ctx.fillStyle = `rgba(255, 255, 255, ${star.opacity * twinkle})`;
        ctx.beginPath();
        ctx.arc(star.x, star.y, star.radius, 0, Math.PI * 2);
        ctx.fill();

        if (star.radius > 1) {
            ctx.strokeStyle = `rgba(100, 150, 255, ${star.opacity * twinkle * 0.5})`;
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.arc(star.x, star.y, star.radius + 2, 0, Math.PI * 2);
            ctx.stroke();
        }
    });

    // Draw floating particles
    time += 0.01;
    for (let i = 0; i < 30; i++) {
        const x = (Math.sin(time * 0.5 + i) * canvas.width) / 2 + canvas.width / 2;
        const y = (Math.cos(time * 0.3 + i * 0.5) * canvas.height) / 2 + canvas.height / 2;
        const size = Math.sin(time + i) * 1 + 1.5;

        ctx.fillStyle = `rgba(100, 150, 255, ${Math.sin(time + i) * 0.3 + 0.3})`;
        ctx.beginPath();
        ctx.arc(x, y, size, 0, Math.PI * 2);
        ctx.fill();
    }

    requestAnimationFrame(animateSpace);
}

animateSpace();

// ============ 3D Card Tilt ============
const cardWrapper = document.getElementById('cardWrapper');
let mouseX = 0;
let mouseY = 0;

document.addEventListener('mousemove', (e) => {
    const rect = cardWrapper.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;

    const rotateX = (e.clientY - centerY) / 25;
    const rotateY = (e.clientX - centerX) / 25;

    cardWrapper.style.transform = `rotateX(${-rotateX}deg) rotateY(${rotateY}deg)`;
});

document.addEventListener('mouseleave', () => {
    cardWrapper.style.transform = 'rotateX(0deg) rotateY(0deg)';
});

// ============ Form Interactions ============
const passwordInput = document.getElementById('password');
const passwordToggle = document.getElementById('passwordToggle');

passwordToggle.addEventListener('click', (e) => {
    e.preventDefault();
    const isPassword = passwordInput.type === 'password';
    passwordInput.type = isPassword ? 'text' : 'password';
    
    // Update the toggle button icon based on password visibility
    const svg = passwordToggle.querySelector('svg');
    if (isPassword) {
        // Show the "hide password" icon when password is visible
        svg.innerHTML = `
            <path d="M2 2l20 20M5.3 5.3C1.5 8 1 12 1 12s4-8 11-8c1.9 0 3.5.4 5 1l-1.7 1.7"></path>
            <path d="M9.3 9.3C8.5 10 8 11 8 12c0 2.2 1.8 4 4 4 1 0 2-.5 2.7-1.3"></path>
            <path d="M15 15.5c-2.1.7-4.3 1-6.5.5-5.5-1-8.5-7-8.5-7s1.1-2.2 3.3-4.3"></path>
        `;
    } else {
        // Show the "show password" icon when password is hidden
        svg.innerHTML = `
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
            <circle cx="12" cy="12" r="3"></circle>
        `;
    }
});

// ============ Submit Form ============
const loginForm = document.getElementById('kc-form-login');
const submitBtn = document.getElementById('kc-login');

loginForm.addEventListener('submit', async (e) => {
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner"></span>Signing in...';

    await new Promise((resolve) => setTimeout(resolve, 1500));

    submitBtn.disabled = false;
    submitBtn.innerHTML = 'Sign In';
});

// ============ Singpass Button Ripple Effect ============
function addRippleEffect(button) {
    button.addEventListener('click', (e) => {
        const rect = button.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        const x = e.clientX - rect.left - size / 2;
        const y = e.clientY - rect.top - size / 2;

        const ripple = document.createElement('div');
        ripple.className = 'ripple';
        ripple.style.width = ripple.style.height = size + 'px';
        ripple.style.left = x + 'px';
        ripple.style.top = y + 'px';

        button.appendChild(ripple);

        setTimeout(() => ripple.remove(), 600);
    });
}

const individualBtn = document.getElementById('social-mockpass-singpass');
const corporateBtn = document.getElementById('social-mockpass-corppass');

addRippleEffect(individualBtn);
addRippleEffect(corporateBtn);

// Add click handlers
individualBtn.addEventListener('click', () => {
    console.log('Individual Singpass clicked');
    // Add your redirect or action here
});

corporateBtn.addEventListener('click', () => {
    console.log('Corporate Singpass clicked');
    // Add your redirect or action here
});