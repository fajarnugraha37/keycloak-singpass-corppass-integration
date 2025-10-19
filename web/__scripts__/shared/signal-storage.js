export function createSignalFromLocalStorage(key, initialValue) {
    let value = read(key, initialValue);
    const subs = new Set();

    // sinkron lintas tab
    const onStorage = (e) => {
        if (e.storageArea !== localStorage || e.key !== key) return;
        value = e.newValue == null ? initialValue : JSON.parse(e.newValue);
        subs.forEach(fn => fn(value, { external: true }));
    };
    window.addEventListener('storage', onStorage);

    function set(next) {
        value = typeof next === 'function' ? next(value) : next;
        localStorage.setItem(key, JSON.stringify(value));
        subs.forEach(fn => fn(value, { external: false }));
    }
    function get() { return value; }
    function subscribe(fn) { subs.add(fn); fn(value, { external: false, init: true }); return () => subs.delete(fn); }
    function remove() { localStorage.removeItem(key); set(initialValue); }

    return { get, set, subscribe, remove };
}

// ==== contoh ====
// const theme = createSignalFromLocalStorage('theme', 'light');
// theme.subscribe(v => console.log('theme =', v));
// theme.set('dark');
// console.log(theme.get());
