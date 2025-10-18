class h{constructor(){this.init()}init(){this.addKeyboardShortcuts(),this.addVisualEnhancements(),this.addAccessibilityFeatures(),this.trackErrors()}addKeyboardShortcuts(){document.addEventListener("keydown",e=>{(e.key==="h"||e.key==="H")&&(window.location.href="/"),(e.key==="b"||e.key==="B")&&history.back(),(e.key==="r"||e.key==="R")&&location.reload(),e.key==="Escape"&&this.showKeyboardHelp()})}addVisualEnhancements(){const e=document.querySelectorAll(".animate-float");if(e.length>0&&!window.matchMedia("(prefers-reduced-motion: reduce)").matches){let t;const s=o=>{t&&cancelAnimationFrame(t),t=requestAnimationFrame(()=>{const{clientX:r,clientY:a}=o,{innerWidth:i,innerHeight:d}=window,c=(r/i-.5)*2,l=(a/d-.5)*2;e.forEach(u=>{const m=c*10,y=l*10;u.style.transform=`translate(${m}px, ${y}px)`})})};document.addEventListener("mousemove",s),document.addEventListener("mouseleave",()=>{e.forEach(o=>{o.style.transform=""})})}}addAccessibilityFeatures(){document.querySelectorAll('a, button, [tabindex]:not([tabindex="-1"])').forEach(s=>{s.addEventListener("focus",()=>{s.style.outline="3px solid #3b82f6",s.style.outlineOffset="2px"}),s.addEventListener("blur",()=>{s.style.outline="",s.style.outlineOffset=""})});const t=document.querySelector("h1, h2");t&&(t.setAttribute("aria-live","polite"),t.setAttribute("role","alert"))}showKeyboardHelp(){const e=document.createElement("div");e.className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50",e.innerHTML=`
            <div class="bg-white rounded-2xl p-8 max-w-md mx-4 shadow-2xl">
                <h3 class="text-xl font-bold text-gray-900 mb-6">Keyboard Shortcuts</h3>
                <div class="space-y-3">
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">Go Home</span>
                        <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">H</kbd>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">Go Back</span>
                        <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">B</kbd>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">Reload Page</span>
                        <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">R</kbd>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">Show Help</span>
                        <kbd class="px-2 py-1 bg-gray-100 rounded text-sm font-mono">ESC</kbd>
                    </div>
                </div>
                <button onclick="this.closest('.fixed').remove()" class="mt-6 w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors">
                    Close
                </button>
            </div>
        `,document.body.appendChild(e),e.addEventListener("click",s=>{s.target===e&&e.remove()});const t=s=>{s.key==="Escape"&&(e.remove(),document.removeEventListener("keydown",t))};document.addEventListener("keydown",t)}trackErrors(){const e={page:window.location.pathname,referrer:document.referrer,userAgent:navigator.userAgent,timestamp:new Date().toISOString(),type:document.title.includes("404")?"404":"5xx"},t=JSON.parse(localStorage.getItem("errorLog")||"[]");t.push(e),t.length>10&&t.shift(),localStorage.setItem("errorLog",JSON.stringify(t))}}class f{constructor(){document.title.includes("Server Error")&&this.init()}init(){this.retryCount=0,this.maxRetries=5,this.baseDelay=3e4,this.startAutoRefresh(),this.addRetryButton()}startAutoRefresh(){const e=this.baseDelay*Math.pow(1.5,this.retryCount);setTimeout(()=>{this.retryCount<this.maxRetries?(this.retryCount++,location.reload()):this.showMaxRetriesMessage()},e)}addRetryButton(){const e=document.querySelector('button[onclick*="reload"]');if(e){const t=e.onclick;e.onclick=()=>{this.retryCount=0,t()}}}showMaxRetriesMessage(){const e=document.createElement("div");e.className="fixed top-4 right-4 bg-amber-100 border border-amber-400 text-amber-800 px-4 py-3 rounded-lg shadow-lg z-50",e.innerHTML=`
            <div class="flex items-center">
                <svg class="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"></path>
                </svg>
                <span>Auto-refresh disabled. Please try again manually.</span>
            </div>
        `,document.body.appendChild(e),setTimeout(()=>{e.remove()},5e3)}}document.addEventListener("DOMContentLoaded",()=>{new h,new f});
//# sourceMappingURL=error-pages-DHJnI13c-mgwvku52.js.map
