/**
 * Simple DOM binding utility to create reactive UI elements.
 * This allows you to bind state properties to DOM elements
 * using data attributes like `data-bind="text:name"`.
 * 
 * @example
 * const state = createStateDOM(document, { name: 'Anon' });
 * // // 2‑way binding manual kecil
 * document.getElementById('name').addEventListener('input', (e) => {
 *     state.name = e.target.value;  // trigger re-render
 * });
 * document.getElementById('reset').addEventListener('click', () => {
 *    state.name = 'Anon';
 * });
 *  
 * @param {HTMLElement} $root
 * @param {object} state
 */
export function createStateDOM($root, state) {
    // render sekali
    const render = () => {
        $root.querySelectorAll('[data-bind]').forEach(el => {
            const [type, key] = el.dataset.bind.split(':'); // "text:name"
            const val = state[key];
            if (type === 'text') el.textContent = val ?? '';
            if (type === 'value') el.value = val ?? '';
            if (type === 'class') el.className = val ?? '';
        });
    };
    // reaktif pake Proxy
    const proxy = new Proxy(state, {
        set(obj, prop, val) {
            const ok = Reflect.set(obj, prop, val);
            queueMicrotask(render);
            return ok;
        }
    });
    render(); // initial
    return proxy;
}

// const state = createStateDOM(document, { name: 'Anon' });

// // 2‑way binding manual kecil
// document.getElementById('name').addEventListener('input', (e) => {
//     state.name = e.target.value;  // trigger re-render
// });
// document.getElementById('reset').addEventListener('click', () => {
//     state.name = 'Anon';
// });