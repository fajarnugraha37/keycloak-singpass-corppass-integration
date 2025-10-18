import { load, throttle } from "./util.js";

export function createReactiveObject(key, initial = {}) {
    const target = load(key, initial);
    const bus = new EventTarget();

    const save = throttle(() => {
        localStorage.setItem(key, JSON.stringify(target));
        bus.dispatchEvent(new CustomEvent('change', { detail: { key, value: target } }));
    }, 50);

    const proxy = new Proxy(target, {
        set(obj, prop, val) {
            const ok = Reflect.set(obj, prop, val);
            save();
            return ok;
        },
        deleteProperty(obj, prop) {
            const ok = Reflect.deleteProperty(obj, prop);
            save();
            return ok;
        }
    });

    // update dari tab lain
    window.addEventListener('storage', (e) => {
        if (e.storageArea !== localStorage || e.key !== key) return;
        const next = e.newValue ? JSON.parse(e.newValue) : initial;
        Object.keys(target).forEach(k => delete target[k]);
        Object.assign(target, next);
        bus.dispatchEvent(new CustomEvent('change', { detail: { key, value: target, external: true } }));
    });

    return {
        state: proxy,
        subscribe(fn) {
            const h = (e) => fn(e.detail.value, e.detail);
            bus.addEventListener('change', h);
            // initial fire
            fn(target, { init: true });
            return () => bus.removeEventListener('change', h);
        }
    };
}

// ==== contoh ====
// const profile = createReactiveObject('profile', { name: 'Anon', age: 0 });
// profile.subscribe((v) => console.log('profile changed', v));
// profile.state.name = 'Fajar'; // auto persist + notify
