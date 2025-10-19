class m{constructor(){this.scrollProgress=null,this.init()}init(){this.createScrollProgress(),this.initScrollAnimations(),this.initParallaxEffects(),this.initInteractiveCards(),this.initFloatingAnimations(),this.initMagneticButtons()}createScrollProgress(){this.scrollProgress=document.createElement("div"),this.scrollProgress.className="scroll-progress",document.body.appendChild(this.scrollProgress),window.addEventListener("scroll",()=>{const s=window.pageYOffset,t=document.documentElement.scrollHeight-window.innerHeight,e=s/t*100;this.scrollProgress.style.width=Math.min(e,100)+"%"})}initScrollAnimations(){const s={threshold:.1,rootMargin:"0px 0px -50px 0px"},t=new IntersectionObserver(o=>{o.forEach(a=>{a.isIntersecting&&a.target.classList.add("visible")})},s);document.querySelectorAll(".scroll-fade-in, .scroll-slide-left, .scroll-slide-right, .scroll-scale-up, .scroll-3d-flip, .scroll-3d-rotate, .scroll-3d-bounce, .scroll-3d-fold, .scroll-3d-depth, .scroll-3d-parallax").forEach(o=>t.observe(o)),this.init3DPerspectiveScaling()}init3DPerspectiveScaling(){const s=document.querySelectorAll(".scroll-3d-perspective");if(s.length===0)return;const t=()=>{const e=window.innerWidth/2,o=window.innerHeight/2;s.forEach(a=>{const n=a.getBoundingClientRect(),r=n.left+n.width/2,c=n.top+n.height/2,l=Math.abs(r-e),p=Math.abs(c-o),u=Math.sqrt(l*l+p*p),f=Math.sqrt(e*e+o*o),d=Math.max(.5,1-u/f*.5),g=(c-o)/o*15,v=(r-e)/e*15;a.style.transform=`
          perspective(1000px)
          scale3d(${d}, ${d}, 1)
          rotateX(${-g}deg)
          rotateY(${v}deg)
          translateZ(${(1-d)*-100}px)
        `,a.style.opacity=.6+d*.4})};window.addEventListener("scroll",t,{passive:!0}),window.addEventListener("resize",t,{passive:!0}),t()}initParallaxEffects(){const s=document.querySelectorAll(".parallax-element");window.addEventListener("scroll",()=>{const t=window.pageYOffset;s.forEach(e=>{const o=t*-.3;e.style.transform=`translate3d(0, ${o}px, 0)`})})}initInteractiveCards(){document.querySelectorAll(".interactive-card, .info-card, .dashboard-item").forEach(t=>{t.addEventListener("mouseenter",e=>{this.createRippleEffect(e,t)}),t.addEventListener("mousemove",e=>{this.applyTiltEffect(e,t)}),t.addEventListener("mouseleave",()=>{t.style.transform=""})})}createRippleEffect(s,t){const e=t.getBoundingClientRect(),o=s.clientX-e.left,a=s.clientY-e.top,n=document.createElement("div");n.style.cssText=`
      position: absolute;
      width: 20px;
      height: 20px;
      background: rgba(255, 255, 255, 0.3);
      border-radius: 50%;
      transform: scale(0);
      animation: ripple-expand 0.6s ease-out;
      left: ${o-10}px;
      top: ${a-10}px;
      pointer-events: none;
      z-index: 10;
    `,t.style.position="relative",t.appendChild(n),setTimeout(()=>n.remove(),600)}applyTiltEffect(s,t){const e=t.getBoundingClientRect(),o=s.clientX-e.left,a=s.clientY-e.top,n=e.width/2,r=e.height/2,c=(a-r)/r*-10,l=(o-n)/n*10;t.style.transform=`
      perspective(1000px) 
      rotateX(${c}deg) 
      rotateY(${l}deg)
      scale3d(1.02, 1.02, 1.02)
    `}initFloatingAnimations(){document.querySelectorAll(".hero-badge, .logo-3d, .dashboard-preview").forEach(t=>{t.classList.add("float-animation")})}initMagneticButtons(){document.querySelectorAll(".auth-btn, .btn-hero-primary, .btn-hero-secondary, .theme-btn-3d").forEach(t=>{t.classList.add("magnetic-btn"),t.addEventListener("mousemove",e=>{const o=t.getBoundingClientRect(),a=e.clientX-o.left-o.width/2,n=e.clientY-o.top-o.height/2,r=a*.2,c=n*.2;t.style.transform=`translate(${r}px, ${c}px)`}),t.addEventListener("mouseleave",()=>{t.style.transform=""})})}static enhanceLoadingOverlay(){const s=document.getElementById("loading-overlay");if(!s)return;const t=document.createElement("div");t.className="loading-particles",t.style.cssText=`
      position: absolute;
      inset: 0;
      pointer-events: none;
      z-index: 1;
    `;for(let e=0;e<20;e++){const o=document.createElement("div");o.style.cssText=`
        position: absolute;
        width: ${Math.random()*4+2}px;
        height: ${Math.random()*4+2}px;
        background: rgba(255, 255, 255, ${Math.random()*.5+.2});
        border-radius: 50%;
        left: ${Math.random()*100}%;
        top: ${Math.random()*100}%;
        animation: particle-float ${Math.random()*3+2}s ease-in-out infinite;
        animation-delay: ${Math.random()*2}s;
      `,t.appendChild(o)}s.insertBefore(t,s.firstChild)}static smoothScrollTo(s){const t=document.getElementById(s);t&&t.scrollIntoView({behavior:"smooth",block:"start"})}static showToast(s,t="success",e=3e3){const o=document.createElement("div");o.className=`toast-3d ${t}`,o.innerHTML=`
      <div class="toast-content-3d">
        <svg class="toast-icon-3d" fill="currentColor" viewBox="0 0 20 20">
          ${t==="success"?'<path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>':'<path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>'}
        </svg>
        <span class="toast-message-3d">${s}</span>
      </div>
    `,document.body.appendChild(o),requestAnimationFrame(()=>{o.classList.add("show")}),setTimeout(()=>{o.classList.remove("show"),setTimeout(()=>o.remove(),300)},e)}}const h=document.createElement("style");h.textContent=`
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
`;document.head.appendChild(h);document.readyState==="loading"?document.addEventListener("DOMContentLoaded",()=>{new m}):new m;typeof module<"u"&&module.exports&&(module.exports=m);function y(){const i=document.getElementById("loading-overlay");i&&(i.classList.remove("active"),setTimeout(()=>{i.classList.add("hidden")},300))}function E(i=""){const s=document.getElementById("loading-overlay");if(s){if(i){const t=s.querySelector(".loading-message-3d");t&&(t.textContent=i)}s.classList.remove("hidden"),requestAnimationFrame(()=>{s.classList.add("active")}),typeof window.InteractiveEffects<"u"&&setTimeout(()=>{window.InteractiveEffects.enhanceLoadingOverlay()},100)}}async function w(i,s,t="GET"){const e=await fetch(i,{method:t,headers:s?{Authorization:`Bearer ${s}`}:{}}),o=await e.text();return{ok:e.ok,status:e.status,body:o}}export{w as c,y as h,E as s};
//# sourceMappingURL=util-Bgifhk2q-mgxfcc79.js.map
