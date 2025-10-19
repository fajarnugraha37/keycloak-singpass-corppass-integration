import"../modulepreload-polyfill-B5Qt9EMX-mgxdv7n7.js";document.addEventListener("DOMContentLoaded",function(){q(),Y(),C(),M(),D(),I(),z(),B(),A(),F()});function q(){const o=document.querySelector(".header");let t=window.scrollY,a;const n=x(()=>{const s=window.scrollY;s>100?(o.classList.add("scrolled"),s>t&&s>200?o.classList.add("hidden"):o.classList.remove("hidden")):o.classList.remove("scrolled","hidden"),t=s,T(),clearTimeout(a),a=setTimeout(()=>{document.body.classList.add("scroll-ended"),setTimeout(()=>{document.body.classList.remove("scroll-ended")},100)},150)},10);window.addEventListener("scroll",n)}function T(){const o=window.pageYOffset,t=document.documentElement.scrollHeight-window.innerHeight,a=o/t*100;let n=document.querySelector(".scroll-progress");n||(n=document.createElement("div"),n.className="scroll-progress",n.style.cssText=`
            position: fixed;
            top: 0;
            left: 0;
            width: 0%;
            height: 3px;
            background: linear-gradient(90deg, var(--primary-red), var(--secondary-blue));
            z-index: 9999;
            transition: width 0.1s ease-out;
        `,document.body.appendChild(n)),n.style.width=`${a}%`}function Y(){const o=document.querySelector(".hero-background"),t=document.querySelector(".hero-pattern");if(!o||!t)return;let a=null;function n(){const e=window.pageYOffset,r=e*-.5,c=e*-.3;t.style.transform=`translateY(${r}px) rotate(${e*.01}deg)`,o.style.transform=`translateY(${c}px)`,document.querySelectorAll(".card").forEach((d,f)=>{const m=e*(-.2-f*.1);d.style.transform=`translateY(${m}px) rotateY(-15deg)`}),a=null}function s(){a===null&&(a=requestAnimationFrame(n))}window.addEventListener("scroll",s)}function C(){const o=document.querySelectorAll(".feature-grid, .endpoints-grid"),t=document.querySelectorAll(".feature-card, .endpoint-card");function a(){const e=window.scrollY,r=window.innerHeight;if(o.forEach((d,f)=>{const m=d.offsetTop,i=d.offsetHeight,u=Math.max(0,Math.min(1,(e-m+r)/(r+i))),p=1e3+u*500,y=(u-.5)*10,h=(u-.5)*5,v=u*50;d.style.perspective=`${p}px`,d.style.transform=`
                perspective(${p}px) 
                rotateY(${y}deg) 
                rotateX(${h}deg) 
                translateZ(${v}px)
                scale3d(${.9+u*.1}, ${.9+u*.1}, 1)
            `}),t.forEach((d,f)=>{const m=d.getBoundingClientRect(),i=m.top+m.height/2,u=r/2,p=Math.abs(i-u),y=r/2,h=Math.max(0,1-p/y);if(m.top<r&&m.bottom>0){const v=(i-u)/y*8,$=(i-u)/y*4,L=h*20,S=.95+h*.05;d.style.transform=`
                    perspective(1200px) 
                    rotateY(${v}deg) 
                    rotateX(${$}deg) 
                    translateZ(${L}px) 
                    scale3d(${S}, ${S}, 1)
                `}}),document.querySelector(".hero")){const d=Math.max(0,1-e/(r*.8)),f=document.querySelector(".hero-pattern"),m=document.querySelector(".identity-cards");f&&(f.style.transform=`
                    translateZ(${d*20}px) 
                    rotateY(${e*.02}deg) 
                    scale3d(${1+d*.1}, ${1+d*.1}, 1)
                `),m&&(m.style.transform=`
                    perspective(1500px) 
                    rotateY(${-15+e*.03}deg) 
                    rotateX(${5+e*.01}deg) 
                    translateZ(${d*40}px)
                `)}const l=document.querySelector(".main-nav");if(l){const d=Math.min(1,e/200);l.style.transform=`
                perspective(1000px) 
                rotateX(${d*-2}deg) 
                translateZ(${d*10}px)
            `,l.style.backdropFilter=`blur(${d*10}px) saturate(${1+d*.5})`}}let n=!1;function s(){n||(requestAnimationFrame(()=>{a(),n=!1}),n=!0)}window.addEventListener("scroll",s),a()}function A(){const o=document.querySelector(".hero");o&&(o.addEventListener("mousemove",x(t=>{const{clientX:a,clientY:n}=t,{innerWidth:s,innerHeight:e}=window,r=(a/s-.5)*60,c=(n/e-.5)*60,l=document.querySelector(".hero-content"),d=document.querySelector(".hero-visual"),f=document.querySelector(".hero-pattern"),m=document.querySelector(".identity-cards"),i=document.querySelectorAll(".card");l&&(l.style.transform=`
                translate3d(${r*.5}px, ${c*.5}px, 20px) 
                rotateY(${r*.1}deg) 
                rotateX(${-c*.1}deg)
            `),d&&(d.style.transform=`
                translate3d(${r*-.3}px, ${c*-.3}px, 30px) 
                perspective(1500px) 
                rotateY(${r*.15}deg) 
                rotateX(${-c*.1}deg)
            `),f&&(f.style.transform=`
                translate3d(${r*-.1}px, ${c*-.1}px, 10px) 
                rotateZ(${r*.02}deg)
            `),m&&(m.style.transform=`
                perspective(1500px) 
                rotateY(${-15+r*.2}deg) 
                rotateX(${5+c*.1}deg)
                translateZ(${Math.abs(r)*.5}px)
            `),i.forEach((u,p)=>{const y=p%2===0?r*.3:r*-.2,h=c*.2,v=20+Math.abs(r)*.3;u.style.transform=`
                translateZ(${v}px) 
                rotateY(${5+y*.1}deg) 
                rotateX(${h*.05}deg)
                translateX(${y}px)
                translateY(${h}px)
            `})},16)),o.addEventListener("mouseleave",()=>{[document.querySelector(".hero-content"),document.querySelector(".hero-visual"),document.querySelector(".hero-pattern"),document.querySelector(".identity-cards")].forEach(n=>{n&&(n.style.transform="",n.style.transition="transform 0.8s cubic-bezier(0.4, 0, 0.2, 1)",setTimeout(()=>{n.style.transition=""},800))}),document.querySelectorAll(".card").forEach(n=>{n.style.transform="",n.style.transition="transform 0.8s cubic-bezier(0.4, 0, 0.2, 1)",setTimeout(()=>{n.style.transition=""},800)})}))}function M(){const o={threshold:.1,rootMargin:"0px 0px -100px 0px"},t=new IntersectionObserver(n=>{n.forEach(s=>{if(s.isIntersecting){const e=s.target;e.classList.contains("feature-card")?X(e):e.classList.contains("endpoint-card")?H(e):e.classList.contains("doc-card")?P(e):e.classList.contains("section-header")&&k(e),t.unobserve(e)}})},o);document.querySelectorAll(".feature-card, .endpoint-card, .doc-card, .section-header").forEach(n=>{t.observe(n)})}function X(o){const t=Array.from(o.parentNode.children).indexOf(o);setTimeout(()=>{o.classList.add("animate"),o.style.transform="translateY(0) scale(1)",o.style.opacity="1"},t*150)}function H(o){const t=Array.from(o.parentNode.children).indexOf(o);setTimeout(()=>{o.classList.add("animate"),o.style.transform="translateY(0) scale(1)",o.style.opacity="1"},t*200)}function P(o){const t=Array.from(o.parentNode.children).indexOf(o);setTimeout(()=>{o.classList.add("animate"),o.style.transform="translateY(0) rotateX(0)",o.style.opacity="1"},t*100)}function k(o){o.style.transform="translateY(0)",o.style.opacity="1"}function D(){document.querySelectorAll(".copy-btn").forEach(t=>{t.addEventListener("click",async a=>{a.preventDefault();const n=t.getAttribute("data-url");if(n){t.style.transform="scale(0.9) rotate(5deg)",t.style.background="var(--primary-red)",t.style.color="white",setTimeout(()=>{t.style.transform="scale(1.1)"},100),setTimeout(()=>{t.style.transform="scale(1)"},200);try{const s=window.location.origin+n;await navigator.clipboard.writeText(s),w("URL copied to clipboard! ✨","success"),t.innerHTML=`
                    <svg viewBox="0 0 20 20" fill="currentColor">
                        <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                `,setTimeout(()=>{t.innerHTML=`
                        <svg viewBox="0 0 20 20" fill="currentColor">
                            <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z"/>
                            <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z"/>
                        </svg>
                    `,t.style.background="",t.style.color=""},1500)}catch{w("Failed to copy URL ❌","error"),t.style.animation="shake 0.5s ease-in-out",setTimeout(()=>{t.style.animation="",t.style.background="",t.style.color=""},500)}}})})}function w(o,t="success"){document.querySelectorAll(".toast").forEach(r=>r.remove());const n=document.createElement("div");n.className="toast";const s={success:'<path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>',error:'<path d="M18 6L6 18M6 6l12 12"/>',info:'<path d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>'};n.innerHTML=`
        <div class="toast-content">
            <svg class="toast-icon" viewBox="0 0 20 20" fill="currentColor">
                ${s[t]||s.success}
            </svg>
            <span class="toast-message">${o}</span>
        </div>
    `;const e={success:{background:"var(--accent-green)",color:"white"},error:{background:"var(--primary-red)",color:"white"},info:{background:"var(--secondary-blue)",color:"white"}};Object.assign(n.style,e[t]||e.success),document.body.appendChild(n),n.style.transform="translateY(100px) scale(0.8)",n.style.opacity="0",requestAnimationFrame(()=>{n.style.transition="all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)",n.style.transform="translateY(0) scale(1)",n.style.opacity="1"}),setTimeout(()=>{n.style.transform="translateY(-20px) scale(0.9)",n.style.opacity="0",setTimeout(()=>{n.parentNode&&n.remove()},400)},3500)}function I(){document.querySelectorAll('a[href^="#"]:not([href="#"])').forEach(t=>{t.addEventListener("click",a=>{a.preventDefault();const n=t.getAttribute("href"),s=document.querySelector(n);if(s){const e=document.querySelector(".header").offsetHeight,r=s.offsetTop-e-30;O(r,800),t.classList.contains("nav-link")?(t.style.transform="scale(0.95)",t.style.background="rgba(255, 255, 255, 0.1)",setTimeout(()=>{t.style.transform="scale(1)",t.style.background=""},150)):t.classList.contains("btn")?(t.style.transform="scale(0.96)",t.style.filter="brightness(0.9)",setTimeout(()=>{t.style.transform="scale(1)",t.style.filter=""},200)):(t.style.opacity="0.7",setTimeout(()=>{t.style.opacity=""},150))}})})}function O(o,t){const a=window.pageYOffset,n=o-a;let s=null;function e(r){s===null&&(s=r);const c=r-s,l=Z(c,a,n,t);window.scrollTo(0,l),c<t&&requestAnimationFrame(e)}requestAnimationFrame(e)}function Z(o,t,a,n){return o/=n/2,o<1?a/2*o*o*o+t:(o-=2,a/2*(o*o*o+2)+t)}function z(){const o=document.querySelector(".header"),t=document.querySelector(".hero");if(!o||!t)return;new IntersectionObserver(n=>{n.forEach(s=>{s.isIntersecting?o.classList.remove("scrolled"):o.classList.add("scrolled")})},{threshold:.1,rootMargin:"0px 0px -50px 0px"}).observe(t)}function B(){document.querySelectorAll(".card").forEach((e,r)=>{const c=g(()=>{e.style.animationPlayState="paused",e.style.transform="translateY(-15px) scale(1.05) rotateY(5deg)",e.style.boxShadow="0 20px 40px rgba(0, 0, 0, 0.3)",e.style.filter="brightness(1.1)"},50),l=g(()=>{e.style.animationPlayState="running",e.style.transform="",e.style.boxShadow="",e.style.filter=""},50);e.addEventListener("mouseenter",c),e.addEventListener("mouseleave",l)}),document.querySelectorAll(".btn").forEach(e=>{e.addEventListener("click",function(l){N(l,this)});const r=g(()=>{const l=e.querySelector("svg");l&&(l.style.transform="translateX(5px) scale(1.1) rotate(5deg)")},30),c=g(()=>{const l=e.querySelector("svg");l&&(l.style.transform="translateX(0) scale(1) rotate(0deg)")},30);e.addEventListener("mouseenter",r),e.addEventListener("mouseleave",c)}),document.querySelectorAll(".feature-icon").forEach(e=>{const r=g(()=>{e.style.transform="scale(1.2) rotate(15deg)",e.style.boxShadow="0 10px 25px rgba(0, 0, 0, 0.2)"},50),c=g(()=>{e.style.transform="scale(1) rotate(0deg)",e.style.boxShadow=""},50);e.addEventListener("mouseenter",r),e.addEventListener("mouseleave",c)}),document.querySelectorAll(".endpoint-card").forEach((e,r)=>{let c=!1,l=null;function d(i){const u=e.getBoundingClientRect(),p=2;return i.clientX>=u.left+p&&i.clientX<=u.right-p&&i.clientY>=u.top+p&&i.clientY<=u.bottom-p}function f(){if(c)return;c=!0;const i=e.querySelector(".endpoint-icon");i&&(i.style.transition="transform 0.3s ease-out",i.style.transform="scale(1.1) rotate(-5deg)"),e.style.background="linear-gradient(45deg, white 25%, rgba(255,255,255,0.9) 50%, white 75%)",e.style.backgroundSize="200% 100%",e.style.animation="shimmer 1.5s ease-in-out infinite"}function m(){c=!1;const i=e.querySelector(".endpoint-icon");i&&(i.style.transition="transform 0.3s ease-out",i.style.transform="scale(1) rotate(0deg)"),e.style.background="white",e.style.animation=""}e.addEventListener("mouseenter",i=>{clearTimeout(l),d(i)&&(l=setTimeout(f,100))},{passive:!0}),e.addEventListener("mouseleave",i=>{clearTimeout(l),l=setTimeout(m,50)},{passive:!0}),window.addEventListener("beforeunload",()=>{clearTimeout(l)})}),document.querySelectorAll(".doc-card").forEach((e,r)=>{let c=!1,l=null;function d(i){const u=e.getBoundingClientRect(),p=2;return i.clientX>=u.left+p&&i.clientX<=u.right-p&&i.clientY>=u.top+p&&i.clientY<=u.bottom-p}function f(){if(c)return;c=!0;const i=e.querySelector(".doc-icon");i&&(i.style.transition="transform 0.3s ease-out",i.style.transform="scale(1.05) rotate(5deg)",i.style.animation="pulse3D 1.5s ease-in-out infinite")}function m(){c=!1;const i=e.querySelector(".doc-icon");i&&(i.style.transition="transform 0.3s ease-out",i.style.transform="",i.style.animation="")}e.addEventListener("mouseenter",i=>{clearTimeout(l),d(i)&&(l=setTimeout(f,100))},{passive:!0}),e.addEventListener("mouseleave",i=>{clearTimeout(l),l=setTimeout(m,50)},{passive:!0}),window.addEventListener("beforeunload",()=>{clearTimeout(l)})})}function F(){document.querySelectorAll(".hero-badge, .identity-cards").forEach((s,e)=>{s.style.animation=`float3D 8s ease-in-out infinite ${e*2}s`});const t=document.querySelector(".hero-title");if(t){const e=t.innerHTML.split(" ");t.innerHTML=e.map((r,c)=>`<span class="word" style="animation-delay: ${c*.1}s; transform-style: preserve-3d;">${r}</span>`).join(" ")}document.querySelectorAll(".hero-pattern::before, .hero-pattern::after").forEach((s,e)=>{s.style.animation=`rotate3D ${25+e*5}s linear infinite`}),document.querySelectorAll(".nav-link").forEach(s=>{s.addEventListener("mouseenter",()=>{s.style.transform="translateY(-2px) translateZ(10px) rotateX(5deg) scale(1.05)",s.style.textShadow="0 4px 8px rgba(0, 0, 0, 0.3)",s.style.transformStyle="preserve-3d"}),s.addEventListener("mouseleave",()=>{s.style.transform="translateY(0) translateZ(0) rotateX(0) scale(1)",s.style.textShadow=""})}),R()}function R(){const o=document.querySelector(".hero");if(!o)return;let t=null;o.addEventListener("touchstart",n=>{n.touches.length===1&&(t={x:n.touches[0].clientX,y:n.touches[0].clientY})},{passive:!0}),o.addEventListener("touchmove",x(n=>{if(n.touches.length===1&&t){const s=n.touches[0],e=s.clientX-t.x,r=s.clientY-t.y,c=e*.2,l=-r*.1,d=document.querySelector(".hero-content"),f=document.querySelector(".identity-cards");d&&(d.style.transform=`
                    perspective(1500px) 
                    rotateY(${c}deg) 
                    rotateX(${l}deg) 
                    translateZ(20px)
                `),f&&(f.style.transform=`
                    perspective(1500px) 
                    rotateY(${-15+c*.5}deg) 
                    rotateX(${5+l*.3}deg) 
                    translateZ(30px)
                `)}},16),{passive:!0}),o.addEventListener("touchend",()=>{t=null;const n=document.querySelector(".hero-content"),s=document.querySelector(".identity-cards");[n,s].forEach(e=>{e&&(e.style.transition="transform 0.8s cubic-bezier(0.4, 0, 0.2, 1)",e.style.transform="",setTimeout(()=>{e.style.transition=""},800))})},{passive:!0});let a=0;o.addEventListener("touchstart",n=>{n.touches.length===2&&(a=E(n.touches[0],n.touches[1]))},{passive:!0}),o.addEventListener("touchmove",x(n=>{if(n.touches.length===2){const e=E(n.touches[0],n.touches[1])/a,r=document.querySelector(".hero-visual");r&&(r.style.transform=`
                    perspective(1500px) 
                    scale3d(${e}, ${e}, ${e}) 
                    translateZ(${(e-1)*50}px)
                `)}},16),{passive:!0}),o.addEventListener("touchend",n=>{if(n.touches.length<2){const s=document.querySelector(".hero-visual");s&&(s.style.transition="transform 0.6s ease-out",s.style.transform="",setTimeout(()=>{s.style.transition=""},600))}},{passive:!0})}function E(o,t){const a=t.clientX-o.clientX,n=t.clientY-o.clientY;return Math.sqrt(a*a+n*n)}function N(o,t){const a=document.createElement("span"),n=t.getBoundingClientRect(),s=Math.max(n.width,n.height),e=o.clientX-n.left-s/2,r=o.clientY-n.top-s/2;a.style.cssText=`
        position: absolute;
        width: ${s}px;
        height: ${s}px;
        left: ${e}px;
        top: ${r}px;
        background: rgba(255, 255, 255, 0.4);
        border-radius: 50%;
        pointer-events: none;
        transform: scale(0);
        animation: ripple 0.6s linear;
        z-index: 1;
    `,t.style.position="relative",t.style.overflow="hidden",t.appendChild(a),setTimeout(()=>{a.remove()},600)}function x(o,t){let a;return function(){const n=arguments,s=this;a||(o.apply(s,n),a=!0,setTimeout(()=>a=!1,t))}}function g(o,t,a){let n;return function(){const e=this,r=arguments,c=function(){n=null,o.apply(e,r)};clearTimeout(n),n=setTimeout(c,t)}}window.innerWidth<768&&(document.documentElement.style.setProperty("--transition-normal","0.15s ease-in-out"),document.documentElement.style.setProperty("--transition-slow","0.2s ease-in-out"));if(window.matchMedia("(prefers-reduced-motion: reduce)").matches){const o=`
        *, *::before, *::after {
            animation-duration: 0.01ms !important;
            animation-iteration-count: 1 !important;
            transition-duration: 0.01ms !important;
            scroll-behavior: auto !important;
        }
    `,t=document.createElement("style");t.textContent=o,document.head.appendChild(t)}const U=`
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
`,b=document.createElement("style");b.textContent=U;document.head.appendChild(b);function V(){"performance"in window&&window.addEventListener("load",()=>{const o=performance.getEntriesByType("navigation")[0]})}V();
//# sourceMappingURL=index-UD1TRE8m-mgxdv7m4.js.map
