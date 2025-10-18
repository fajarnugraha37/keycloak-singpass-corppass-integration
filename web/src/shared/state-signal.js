/**
 * A simple state management utility that allows you to create reactive state signals.
 * This can be used to manage state in a reactive way, similar to React's useState or Vue's reactive.
 * * It provides a way to create a signal that can be subscribed to, allowing components to reactively update when the state changes.
 * * @example
 * const count = createStateSignal(0);
 * // // bind to DOM
 * const span = document.getElementById('count');
 * count.subscribe(v => span.textContent = v);
 * * document.getElementById('inc').addEventListener('click', () => {
 *    count.set(v => v + 1);
 * * }
 * * // derived/computed example
 * const isEven = () => count.get() % 2 === 0;
 * effect(() => { document.body.dataset.even = isEven(); });
 * 
 * @param {any} initial The initial value of the state signal.
 * @returns {{
 *  get: () => any;
 *  set: (any) => void;
 *  partialSet: (object) => void;
 *  subscribe: (fn: any) => () => boolean;
 * }} An object with `get`, `set`, and `subscribe` methods to manage the state.
 * * - `get()`: Returns the current value of the state.
 * * - `set(value)`: Sets the state to a new value. If a function is provided, it will be called with the current value and should return the new value.
 * * - `subscribe(callback)`: Subscribes to state changes. The callback will be called with the new value whenever the state changes. It returns a function to unsubscribe from the changes.
 */
export function createStateSignal(initial) {
    let value = initial;
    const subs = new Set();
    const set = (next) => {
        value = typeof next === 'function' ? next(value) : next;
        queueMicrotask(() => subs.forEach(fn => fn(value))); // batch via microtask
    };
    const partialSet = (next) => {
        value = { ...value, ...next };
        queueMicrotask(() => subs.forEach(fn => fn(value))); // batch via microtask
    };
    const get = () => value;
    const subscribe = (fn) => (subs.add(fn), () => subs.delete(fn));

    return { get, set, partialSet, subscribe };
}

/**
 * Run a function reactively, re-running it whenever the state changes.
 * This is useful for creating derived state or side effects that depend on reactive state signals.
 * * The function can return a cleanup function that will be called when the effect is stopped.
 * * @example
 * const count = createStateSignal(0);
 * effect(() => {
 *   document.getElementById('count').textContent = count.get();
 *   return () => {
 *     // cleanup logic if needed
 *   };
 * });
 * 
 * @param {function} fn The function to run reactively. It should return a cleanup function if needed.
 * @returns {function} A function to stop the effect. Calling this will stop the reactive updates.
 * * The effect will automatically stop if the function returns a cleanup function.
 * * @example
 * const stop = effect(() => {
 *   console.log('Effect ran');
 * return () => console.log('Effect stopped');
 * }
 * // To stop the effect later
 * stop();
 */
export function effect(fn) {
    let stop = () => { };
    const run = () => stop = fn() || (() => { });
    run(); return () => (stop(), stop = () => { });
}

// --- usage ---
// const count = createStateSignal(0);

// // bind to DOM
// const span = document.getElementById('count');
// count.subscribe(v => span.textContent = v);

// document.getElementById('inc').addEventListener('click', () => {
//     count.set(v => v + 1);
// });

// // derived/computed example
// const isEven = () => count.get() % 2 === 0;
// effect(() => { document.body.dataset.even = isEven(); });