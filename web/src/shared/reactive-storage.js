import { safeParse } from "./util";

export class ReactiveStorage extends EventTarget {
    constructor(channel = 'lsync') {
        super();
        this.bc = typeof BroadcastChannel !== 'undefined' ? new BroadcastChannel(channel) : null;

        // Perubahan dari tab lain (via storage event)
        window.addEventListener('storage', (e) => {
            if (e.storageArea !== localStorage) return;
            const key = e.key;
            const value = safeParse(e.newValue);
            this.#emit(key, value, { external: true, origin: 'storage' });
        });

        // Perubahan dari tab lain (via BroadcastChannel, lebih deterministik)
        if (this.bc) {
            this.bc.onmessage = (e) => {
                const { key, value } = e.data || {};
                this.#emit(key, value, { external: true, origin: 'broadcast' });
            };
        }
    }

    getItem(key, fallback = null) {
        return safeParse(localStorage.getItem(key)) ?? fallback;
    }

    setItem(key, value) {
        localStorage.setItem(key, JSON.stringify(value));
        this.#emit(key, value, { external: false, origin: 'local' });
        this.bc?.postMessage({ key, value });
    }

    removeItem(key) {
        localStorage.removeItem(key);
        this.#emit(key, undefined, { external: false, origin: 'local', removed: true });
        this.bc?.postMessage({ key, value: undefined, removed: true });
    }

    /**
     * Subscribe ke satu key tertentu. Return function untuk unsubscribe.
     */
    subscribe(key, cb) {
        const handler = (evt) => {
            if (evt.detail.key === key) cb(evt.detail.value, evt.detail.meta);
        };
        this.addEventListener('change', handler);
        return () => this.removeEventListener('change', handler);
    }

    /**
     * Subscribe ke SEMUA perubahan (apapun key-nya)
     */
    subscribeAll(cb) {
        const handler = (evt) => cb(evt.detail.key, evt.detail.value, evt.detail.meta);
        this.addEventListener('change', handler);
        return () => this.removeEventListener('change', handler);
    }

    #emit(key, value, meta) {
        this.dispatchEvent(new CustomEvent('change', { detail: { key, value, meta } }));
    }
}
// ==== contoh pemakaian ====
// const store = new ReactiveStorage();
// store.subscribe('theme', (val) => { document.documentElement.dataset.theme = val; });
// store.setItem('theme', 'dark');
