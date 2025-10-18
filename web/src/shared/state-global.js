/**
 * createStateGlobal - a simple global state management utility 
 * that allows you to create a reactive global state.
 * * This can be used to manage global state in a reactive way, similar to React's useState or Vue's reactive.
 * * It provides a way to create a global state that can be subscribed to, allowing components to reactively update when the state changes.
 * * @example
 * const store = createStateGlobal({ todos: [] });
 * // // bind to DOM
 * const $list = document.getElementById('list');
 * store.subscribe((s) => {
 *    $list.innerHTML = s.todos.map(t => `<li>${t}</li>`).join('');
 * });
 * document.getElementById('addTodo').addEventListener('click', () => {
 *   store.set(s => ({ ...s, todos: [...s.todos, `Item ${s.todos.length + 1}`] }));
 * });
 *
 * @param {object} initial The initial value of the global state.
 * @returns {object} An object with `get`, `set`, and `subscribe` methods to manage the global state.
 * * - `get()`: Returns the current value of the global state.
 * * - `set(patch)`: Sets the global state to a new value. If a function is provided, it will be called with the current value and should return the new value.
 * * - `subscribe(callback)`: Subscribes to global state changes. The callback will be called with the new value whenever the global state changes. It returns a function to unsubscribe from the changes.
 */
export function createStateGlobal(initial) {
    let state = structuredClone(initial);
    const subs = new Set();
    const get = () => state;
    const set = (patch) => {
        state = typeof patch === 'function' ? patch(state) : { ...state, ...patch };
        queueMicrotask(() => subs.forEach(fn => fn(state)));
    };
    const subscribe = (fn) => (subs.add(fn), () => subs.delete(fn));
    return { get, set, subscribe };
}

/**
 * Select a part of the global state and subscribe to changes.
 * This allows you to create derived state or reactively update UI components based on a specific part of the global state.
 * * The selector function should return the part of the state you want to track.
 * * @example
 * const store = createStateGlobal({ todos: [] });
 * const $list = document.getElementById('list');
 * select(s => s.todos, (todos) => {
 *     $list.innerHTML = todos.map(t => `<li>${t}</li>`).join('');
 * });
 *
 * @param {function} selector A function that takes the global state and returns the part of the state to track.
 * @param {function} cb A callback function that will be called with the selected part of the state whenever it changes.
 * @returns {function} A function to stop the subscription. Calling this will stop thereactive updates.
 */
export function select(selector, cb) {
    let prev;
    return store.subscribe((s) => {
        const next = selector(s);
        if (JSON.stringify(next) !== JSON.stringify(prev)) { // simpel
            prev = next; cb(next);
        }
    });
}

// const store = createStateGlobal({ todos: [] });

// const $list = document.getElementById('list');
// select(s => s.todos, (todos) => {
//     $list.innerHTML = todos.map(t => `<li>${t}</li>`).join('');
// });

// document.getElementById('addTodo').onclick = () => {
//     store.set(s => ({ ...s, todos: [...s.todos, `Item ${s.todos.length + 1}`] }));
// };