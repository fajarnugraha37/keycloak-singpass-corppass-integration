var Y=(s,e)=>()=>(e||s((e={exports:{}}).exports,e),e.exports);import"./modulepreload-polyfill-B5Qt9EMX-mgxfcc74.js";/* empty css                        */import"./vendor-htmx-hCWdS4WT-mgxfcc6u.js";var z=Y((I,f)=>{class E{constructor(){this.init()}init(){this.initScrollAnimations(),this.initServiceCardAnimations(),this.initSmoothScrolling(),this.initParallaxEffects(),this.initPerformanceObservers(),this.initAdvancedInteractions()}initScrollAnimations(){const e={root:null,rootMargin:"-10% 0px -10% 0px",threshold:[0,.1,.5,1]},o=new IntersectionObserver(t=>{t.forEach(n=>{if(n.isIntersecting){const a=n.target,r=a.dataset.delay||0;setTimeout(()=>{a.classList.add("visible"),this.triggerElementAnimation(a)},r)}})},e);document.querySelectorAll(".scroll-fade").forEach(t=>{o.observe(t)}),document.querySelectorAll(".service-card").forEach((t,n)=>{t.dataset.delay=n*100,t.classList.add("scroll-fade"),o.observe(t)}),document.querySelectorAll(".resource-card").forEach((t,n)=>{t.dataset.delay=n*50,t.classList.add("scroll-fade"),o.observe(t)})}initServiceCardAnimations(){document.querySelectorAll(".service-card").forEach(o=>{let t,n=!1;const a=c=>{if(n)return;n=!0;const i=o.getBoundingClientRect(),d=i.left+i.width/2,u=i.top+i.height/2,m=c.clientX,S=(c.clientY-u)/10,w=(d-m)/10;t=requestAnimationFrame(()=>{o.style.transform=`
                        translateY(-8px) 
                        rotateX(${Math.max(-5,Math.min(5,S))}deg) 
                        rotateY(${Math.max(-5,Math.min(5,w))}deg)
                        scale(1.02)
                    `,this.addGlowEffect(o),n=!1})},r=c=>{if(n)return;const i=o.getBoundingClientRect(),d=(c.clientX-i.left)/i.width*100,u=(c.clientY-i.top)/i.height*100;o.style.setProperty("--mouse-x",`${d}%`),o.style.setProperty("--mouse-y",`${u}%`)},l=()=>{t&&cancelAnimationFrame(t),t=requestAnimationFrame(()=>{o.style.transform="",o.style.removeProperty("--mouse-x"),o.style.removeProperty("--mouse-y"),this.removeGlowEffect(o),n=!1})};o.addEventListener("mouseenter",a,{passive:!1}),o.addEventListener("mousemove",r,{passive:!0}),o.addEventListener("mouseleave",l,{passive:!1}),o.addEventListener("touchstart",c=>{c.preventDefault(),o.classList.add("touch-active"),setTimeout(()=>{o.classList.remove("touch-active")},300)},{passive:!1})})}initSmoothScrolling(){document.querySelectorAll('a[href^="#"]').forEach(e=>{e.addEventListener("click",o=>{o.preventDefault();const t=document.querySelector(e.getAttribute("href"));if(t){const n=document.querySelector("header")?.offsetHeight||0,a=t.offsetTop-n-20;window.scrollTo({top:a,behavior:"smooth"})}})}),document.querySelectorAll(".service-btn").forEach(e=>{e.addEventListener("click",o=>{this.createRippleEffect(o,e)})})}initParallaxEffects(){let e=!1;const o=()=>{const t=window.pageYOffset,n=t*-.5,a=Math.max(.3,1-t/1e3);document.querySelector("main::before")&&(document.documentElement.style.setProperty("--bg-translate",`${n}px`),document.documentElement.style.setProperty("--bg-opacity",a)),document.querySelectorAll(".service-card").forEach((l,c)=>{if(this.isInViewport(l)){const i=.1+c*.02,d=-(t*i);l.style.setProperty("--parallax-y",`${d}px`)}}),e=!1};window.addEventListener("scroll",()=>{e||(requestAnimationFrame(o),e=!0)},{passive:!0})}initPerformanceObservers(){if("PerformanceObserver"in window){const o=new PerformanceObserver(t=>{t.getEntries().forEach(a=>{a.duration>16.67})});try{o.observe({type:"measure",buffered:!0})}catch{}}const e=new IntersectionObserver(o=>{o.forEach(t=>{if(t.isIntersecting){const n=t.target;n.classList.add("animate-in"),e.unobserve(n)}})},{rootMargin:"50px"});document.querySelectorAll("[data-animate]").forEach(o=>{e.observe(o)})}initAdvancedInteractions(){const e=document.querySelector(".alert-banner");if(e){let t;e.addEventListener("mouseenter",()=>{clearTimeout(t),e.style.animationPlayState="paused"}),e.addEventListener("mouseleave",()=>{t=setTimeout(()=>{e.style.animationPlayState="running"},1e3)})}document.querySelectorAll(".resource-card").forEach(t=>{t.addEventListener("mouseenter",()=>{this.createFloatingParticles(t)})});const o=document.querySelector("[data-theme-toggle]");o&&o.addEventListener("click",t=>{this.animateThemeTransition()}),document.querySelectorAll(".footer-links a").forEach(t=>{t.addEventListener("mouseenter",()=>{this.animateFooterLink(t)})})}triggerElementAnimation(e){const o=e.dataset.animation||"fadeInUp";e.style.animation=`${o} 0.8s cubic-bezier(0.4, 0, 0.2, 1) forwards`}addGlowEffect(e){const o=e.classList.contains("service-btn")?"glow-blue":"glow-default";e.classList.add(o)}removeGlowEffect(e){e.classList.remove("glow-blue","glow-orange","glow-default")}createRippleEffect(e,o){const t=o.getBoundingClientRect(),n=document.createElement("span"),a=Math.max(t.width,t.height),r=e.clientX-t.left-a/2,l=e.clientY-t.top-a/2;n.style.cssText=`
            position: absolute;
            width: ${a}px;
            height: ${a}px;
            left: ${r}px;
            top: ${l}px;
            background: rgba(255, 255, 255, 0.5);
            border-radius: 50%;
            transform: scale(0);
            animation: ripple 0.6s linear;
            pointer-events: none;
        `,o.style.position="relative",o.style.overflow="hidden",o.appendChild(n),setTimeout(()=>{n.remove()},600)}createFloatingParticles(e){const o=[];for(let n=0;n<5;n++){const a=document.createElement("div");a.style.cssText=`
                position: absolute;
                width: 4px;
                height: 4px;
                background: linear-gradient(45deg, #3b82f6, #10b981);
                border-radius: 50%;
                pointer-events: none;
                opacity: 0;
                animation: particleFloat 2s ease-out forwards;
                animation-delay: ${n*.1}s;
            `;const r=e.getBoundingClientRect();a.style.left=`${r.left+Math.random()*r.width}px`,a.style.top=`${r.top+Math.random()*r.height}px`,document.body.appendChild(a),o.push(a)}setTimeout(()=>{o.forEach(n=>n.remove())},2e3)}animateThemeTransition(){const e=document.createElement("div");e.style.cssText=`
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
        `,document.body.appendChild(e),requestAnimationFrame(()=>{e.style.opacity="1"}),setTimeout(()=>{e.style.opacity="0",setTimeout(()=>e.remove(),300)},150)}animateFooterLink(e){e.querySelector("::before"),e.style.transform="translateX(5px)",setTimeout(()=>{e.style.transform=""},200)}isInViewport(e){const o=e.getBoundingClientRect();return o.top>=0&&o.left>=0&&o.bottom<=(window.innerHeight||document.documentElement.clientHeight)&&o.right<=(window.innerWidth||document.documentElement.clientWidth)}debounce(e,o){let t;return function(...a){const r=()=>{clearTimeout(t),e(...a)};clearTimeout(t),t=setTimeout(r,o)}}}const A=`
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
`;document.addEventListener("DOMContentLoaded",()=>{const s=document.createElement("style");s.textContent=A,document.head.appendChild(s),new E,document.body.classList.add("fade-in-up"),document.addEventListener("visibilitychange",()=>{document.hidden?document.querySelectorAll("*").forEach(e=>{e.style.animationPlayState!==void 0&&(e.style.animationPlayState="paused")}):document.querySelectorAll("*").forEach(e=>{e.style.animationPlayState!==void 0&&(e.style.animationPlayState="running")})})});typeof f<"u"&&f.exports&&(f.exports=E);document.addEventListener("DOMContentLoaded",function(){window.htmx&&(htmx.config.defaultSwapStyle="outerHTML",htmx.config.defaultSwapDelay=100,htmx.config.defaultSettleDelay=200);const s=document.getElementById("theme-toggle");if(s&&s.addEventListener("click",function(){document.documentElement.classList.toggle("dark");const e=document.documentElement.classList.contains("dark");localStorage.setItem("theme",e?"dark":"light");const o=this.querySelector("svg");o&&(e?o.innerHTML='<path fill-rule="evenodd" d="M17.293 13.293A8 8 0 0 1 6.707 2.707a8.001 8.001 0 1 0 10.586 10.586z" clip-rule="evenodd"></path>':o.innerHTML='<path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v1a1 1 0 1 1-2 0V3a1 1 0 0 1 1-1zm4 8a4 4 0 1 1-8 0 4 4 0 0 1 8 0zm-.464 4.95l.707.707a1 1 0 0 0 1.414-1.414l-.707-.707a1 1 0 0 0-1.414 1.414zm2.12-10.607a1 1 0 0 1 0 1.414l-.706.707a1 1 0 1 1-1.414-1.414l.707-.707a1 1 0 0 1 1.414 0z" clip-rule="evenodd"></path>')}),localStorage.getItem("theme")==="dark"){document.documentElement.classList.add("dark");const e=document.querySelector("#theme-toggle svg");e&&(e.innerHTML='<path fill-rule="evenodd" d="M17.293 13.293A8 8 0 0 1 6.707 2.707a8.001 8.001 0 1 0 10.586 10.586z" clip-rule="evenodd"></path>')}setTimeout(()=>{document.querySelectorAll(".service-btn").forEach((t,n)=>{t.style.cursor="pointer",t.style.userSelect="none",t.style.position="relative",t.style.zIndex="10",t.addEventListener("click",function(a){return this.style.transform="scale(0.98)",setTimeout(()=>{this.style.transform=""},150),!0},{passive:!1}),t.addEventListener("mouseenter",function(){this.style.transform="translateY(-2px) scale(1.02)"}),t.addEventListener("mouseleave",function(){this.style.transform=""}),t.addEventListener("focus",function(){this.style.outline="2px solid #3b82f6",this.style.outlineOffset="2px"}),t.addEventListener("blur",function(){this.style.outline="",this.style.outlineOffset=""})}),document.querySelectorAll(".resource-card").forEach((t,n)=>{t.addEventListener("mousemove",function(a){const r=this.getBoundingClientRect(),l=a.clientX-r.left,c=a.clientY-r.top,i=r.width/2,d=r.height/2,u=(c-d)/d*-10,m=(l-i)/i*10;this.style.transform=`perspective(1000px) rotateX(${u}deg) rotateY(${m}deg) translateZ(20px) scale(1.05)`}),t.addEventListener("mouseleave",function(){this.style.transform="perspective(1000px) rotateX(0deg) rotateY(0deg) translateZ(0px) scale(1)"}),t.addEventListener("click",function(a){v(a,this)})})},200),document.body.style.opacity="0",document.body.style.transform="translateY(20px) rotateX(10deg)",document.body.style.transformStyle="preserve-3d",document.body.style.perspective="1000px",setTimeout(()=>{document.body.style.transition="all 800ms cubic-bezier(0.4, 0, 0.2, 1)",document.body.style.opacity="1",document.body.style.transform="translateY(0) rotateX(0deg)"},100),p(),y(),b(),L(),setTimeout(()=>{const e=document.querySelectorAll("header nav a"),o=document.querySelector("header");o&&(o.style.zIndex="9999",o.style.position="sticky",o.style.top="0"),e.forEach(n=>{n.style.pointerEvents="auto",n.style.cursor="pointer",n.style.zIndex="10000",n.style.position="relative",n.style.isolation="isolate",n.addEventListener("click",function(a){})});const t=document.querySelector(".min-h-screen");t&&(t.style.zIndex="1",t.style.position="relative")},300)});function v(s,e){const o=e.getBoundingClientRect(),t=document.createElement("span"),n=Math.max(o.width,o.height)*1.5,a=s.clientX-o.left-n/2,r=s.clientY-o.top-n/2;t.style.cssText=`
    position: absolute;
    width: ${n}px;
    height: ${n}px;
    left: ${a}px;
    top: ${r}px;
    background: radial-gradient(circle, rgba(30, 58, 138, 0.3) 0%, rgba(59, 130, 246, 0.1) 50%, transparent 100%);
    border-radius: 50%;
    transform: scale(0) rotateZ(0deg);
    animation: enhancedRipple 0.8s cubic-bezier(0.4, 0, 0.2, 1);
    pointer-events: none;
    z-index: 100;
  `,e.style.position="relative",e.style.overflow="hidden",e.appendChild(t),e.style.transform="perspective(1000px) rotateX(-2deg) rotateY(1deg) scale(1.02)",setTimeout(()=>{e.style.transform="",t.remove()},800)}const x=document.createElement("style");x.textContent=`
  @keyframes ripple {
    to {
      transform: scale(4);
      opacity: 0;
    }
  }
`;document.head.appendChild(x);function p(){const s={threshold:.1,rootMargin:"0px 0px -100px 0px"},e=new IntersectionObserver(o=>{o.forEach((t,n)=>{t.isIntersecting&&setTimeout(()=>{t.target.classList.add("visible"),t.target.style.transform="translateY(0) scale(1) rotateX(0deg) rotateZ(0deg)",t.target.style.opacity="1"},n*100)})},s);document.querySelectorAll(".service-card, .resource-card, .alert-banner, .page-title").forEach((o,t)=>{o.classList.add("scroll-animate"),o.style.transform="translateY(60px) scale(0.8) rotateX(25deg) rotateZ(2deg)",o.style.opacity="0",o.style.transition=`all 0.8s cubic-bezier(0.4, 0, 0.2, 1) ${t*.1}s`,o.style.transformStyle="preserve-3d",e.observe(o)})}function y(){let s=!1;function e(){const t=window.pageYOffset;document.querySelectorAll("[data-parallax]").forEach(r=>{const l=r.dataset.parallax||.5,c=-(t*l);r.style.transform=`translateY(${c}px) translateZ(0)`}),document.querySelectorAll(".service-card").forEach((r,l)=>{const c=r.getBoundingClientRect(),i=Math.max(0,Math.min(1,(window.innerHeight-c.top)/window.innerHeight)),d=(i-.5)*10,u=(i-.5)*5;r.style.transform=`translateY(0) rotateX(${d}deg) rotateY(${u}deg) translateZ(${i*20}px)`}),s=!1}function o(){s||(requestAnimationFrame(e),s=!0)}window.addEventListener("scroll",o),e()}function b(){let s=0,e=0;document.addEventListener("mousemove",o=>{s=o.clientX,e=o.clientY;const t=document.querySelector("header");if(t){const r=t.getBoundingClientRect().width/2,l=(s-r)/r*2;t.style.transform=`perspective(1000px) rotateY(${l}deg)`}document.querySelectorAll(".service-btn").forEach(a=>{const r=a.getBoundingClientRect(),l=r.left+r.width/2,c=r.top+r.height/2,i=Math.sqrt((s-l)**2+(e-c)**2);if(i<200){const d=(200-i)/200,u=(s-l)*d*.1,m=(e-c)*d*.1;a.style.transform=`translate(${u}px, ${m}px) scale(${1+d*.1}) perspective(1000px)`}})})}function L(){const s=document.querySelectorAll(".resource-card");s.forEach((e,o)=>{e.style.opacity="0",e.style.transform="translateY(30px) rotateX(45deg) scale(0.8)",setTimeout(()=>{e.style.transition="all 0.6s cubic-bezier(0.4, 0, 0.2, 1)",e.style.opacity="1",e.style.transform="translateY(0) rotateX(0deg) scale(1)"},1e3+o*150)}),s.forEach((e,o)=>{const t=o*.5;e.style.animation=`subtleFloat 4s ease-in-out ${t}s infinite`})}function h(){let s=0,e=0;document.addEventListener("mousemove",o=>{s=o.clientX,e=o.clientY,document.querySelectorAll(".service-btn").forEach(n=>{const a=n.getBoundingClientRect(),r=a.left+a.width/2,l=a.top+a.height/2,c=Math.sqrt((s-r)**2+(e-l)**2);if(c<150){const i=(150-c)/150,d=(s-r)*i*.1,u=(e-l)*i*.1;n.style.transform=`translate(${d}px, ${u}px) scale(${1+i*.1})`}else n.style.transform=""})})}setTimeout(()=>{h()},1e3);function g(){document.querySelectorAll(".service-card, .resource-card").forEach(s=>{s.addEventListener("mousemove",e=>{const o=s.getBoundingClientRect(),t=o.left+o.width/2,n=o.top+o.height/2,a=e.clientX-t,r=e.clientY-n,l=r/o.height*-15,c=a/o.width*15;s.style.transform=`
        perspective(1000px) 
        rotateX(${l}deg) 
        rotateY(${c}deg) 
        translateZ(20px)
        scale3d(1.03, 1.03, 1.03)
      `;const i=s.querySelector(".card-highlight")||document.createElement("div");s.querySelector(".card-highlight")||(i.className="card-highlight",i.style.cssText=`
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: radial-gradient(circle at ${(a+o.width/2)/o.width*100}% ${(r+o.height/2)/o.height*100}%, rgba(59, 130, 246, 0.1) 0%, transparent 50%);
          pointer-events: none;
          border-radius: 8px;
          opacity: 0;
          transition: opacity 0.3s ease;
        `,s.appendChild(i)),i.style.opacity="1"}),s.addEventListener("mouseleave",()=>{s.style.transform="";const e=s.querySelector(".card-highlight");e&&(e.style.opacity="0")})})}function q(){document.querySelectorAll("header nav a").forEach((e,o)=>{e.style.opacity="0",e.style.transform="translateY(-20px)",setTimeout(()=>{e.style.transition="all 0.5s cubic-bezier(0.4, 0, 0.2, 1)",e.style.opacity="1",e.style.transform="translateY(0)"},300+o*100),e.addEventListener("mouseenter",t=>{t.target.closest("header")&&(t.target.style.transform="translateY(-3px) scale(1.05)",t.target.style.boxShadow="0 4px 12px rgba(0, 0, 0, 0.2)",t.target.style.background="rgba(255, 255, 255, 0.15)")}),e.addEventListener("mouseleave",t=>{t.target.closest("header")&&(t.target.style.transform="translateY(0) scale(1)",t.target.style.boxShadow="none",t.target.style.background="transparent")}),e.addEventListener("mousedown",t=>{t.target.closest("header")&&(t.target.style.transform="translateY(-1px) scale(1.02)")}),e.addEventListener("mouseup",t=>{t.target.closest("header")&&(t.target.style.transform="translateY(-3px) scale(1.05)")})})}function T(){const s=document.getElementById("theme-toggle");s&&s.addEventListener("click",e=>{e.preventDefault(),s.style.transform="rotateY(180deg)",setTimeout(()=>{s.style.transform="rotateY(0deg)"},300),v(e,s)})}function C(){document.querySelectorAll(".service-card").forEach(e=>{e.style.opacity="0",e.style.transform="translateY(50px) rotateX(20deg)",setTimeout(()=>{e.style.transition="all 0.8s cubic-bezier(0.4, 0, 0.2, 1)",e.style.opacity="1",e.style.transform="translateY(0) rotateX(0deg)"},600),e.addEventListener("mouseenter",()=>{e.style.transform="translateY(-15px) rotateX(8deg) rotateY(-3deg) scale(1.02)"}),e.addEventListener("mouseleave",()=>{e.style.transform="translateY(0) rotateX(0deg) rotateY(0deg) scale(1)"})})}document.addEventListener("DOMContentLoaded",()=>{setTimeout(()=>{p(),y(),b(),L(),g(),h(),q(),T(),C(),document.querySelectorAll("header nav a").forEach((e,o)=>{e.style.pointerEvents="auto",e.style.zIndex="10",e.addEventListener("click",t=>{})})},500),document.querySelectorAll(".btn, .service-btn, .resource-card").forEach(s=>{s.addEventListener("click",e=>{v(e,s)})})});function X(){window.matchMedia("(prefers-reduced-motion: reduce)").matches&&document.documentElement.classList.add("reduced-motion")}window.eServiceDebug={buttons:()=>document.querySelectorAll(".service-btn"),cards:()=>document.querySelectorAll(".resource-card"),particles:()=>document.getElementById("particle-canvas"),theme:()=>document.documentElement.classList.contains("dark")?"dark":"light",animations:{scroll:()=>p(),parallax:()=>y(),tilt:()=>g(),mouse:()=>h()},reinitialize:()=>{p(),y(),g(),h()},performance:{optimize:X,reducedMotion:()=>document.documentElement.classList.contains("reduced-motion")}}});export default z();
//# sourceMappingURL=index-BhXtas7b-mgxfcc6h.js.map
