import { safeParse } from "./util";

export class ReactiveStorageTTL extends EventTarget {
  /**
   * @param {object} opts
   * @param {string} [opts.channel='lsync']
   * @param {string} [opts.namespace='__rs__'] // penanda value milik wrapper ini
   * @param {number} [opts.safetySweepMs=60_000] // sweep fallback periodik
   */
  constructor(opts = {}) {
    super();
    this.ns = opts.namespace ?? '__rs__';
    this.bc = typeof BroadcastChannel !== 'undefined'
      ? new BroadcastChannel(opts.channel ?? 'lsync')
      : null;
    this.safetySweepMs = opts.safetySweepMs ?? 60_000;
    this.nextTimer = null;

    // Event lintas-tab bawaan browser
    window.addEventListener('storage', (e) => {
      if (e.storageArea !== localStorage) return;
      this.#emitChange(e.key, safeParse(e.newValue), { external: true, origin: 'storage' });
      // Pemicu sweep cepat kalau ada perubahan
      this.#scheduleSweepSoon(250);
    });

    // Channel manual: update + perintah sweep
    this.bc && (this.bc.onmessage = (e) => {
      const msg = e.data || {};
      if (msg.type === 'update') {
        this.#emitChange(msg.key, msg.value, { external: true, origin: 'broadcast' });
      } else if (msg.type === 'sweepNow') {
        this.#sweepExpired({ via: 'broadcast' });
      }
    });

    // Jalankan sweeper awal + periodic fallback
    this.#scheduleSweepSoon(300);
    this.#startSafetySweep();

    // Saat tab jadi aktif/online—kejar TTL yang lewat ketika tab tidur
    ['visibilitychange', 'focus', 'online'].forEach(ev =>
      window.addEventListener(ev, () => this.#scheduleSweepSoon(200))
    );
  }

  // ===== API Publik =====

  getItem(key, fallback = null) {
    const rec = safeParse(localStorage.getItem(key));
    const val = this.#unwrap(rec);
    return val ?? fallback;
  }

  /**
   * Set item dengan TTL.
   * @param {string} key
   * @param {any} value
   * @param {object} [opts]
   * @param {number} [opts.ttlMs]  // durasi TTL dalam ms
   */
  setItem(key, value, opts = {}) {
    const exp = typeof opts.ttlMs === 'number' ? Date.now() + Math.max(0, opts.ttlMs) : null;
    const rec = this.#wrap(value, exp);
    localStorage.setItem(key, JSON.stringify(rec));
    this.#emitChange(key, value, { external: false, origin: 'local', exp });
    this.bc?.postMessage({ type: 'update', key, value });

    // Jadwalkan sweep berikutnya tepat di waktu expiry terdekat
    this.#scheduleNextByNearestExpiry();
  }

  removeItem(key) {
    localStorage.removeItem(key);
    this.#emitChange(key, undefined, { external: false, origin: 'local', removed: true });
    this.bc?.postMessage({ type: 'update', key, value: undefined, removed: true });
  }

  /** Subscribe ke perubahan pada satu key */
  subscribe(key, cb) {
    const handler = (evt) => { if (evt.detail.key === key) cb(evt.detail.value, evt.detail.meta); };
    this.addEventListener('change', handler);
    // initial emit (state saat ini)
    cb(this.getItem(key), { init: true });
    return () => this.removeEventListener('change', handler);
  }

  /** Subscribe ke semua perubahan */
  subscribeAll(cb) {
    const handler = (evt) => cb(evt.detail.key, evt.detail.value, evt.detail.meta);
    this.addEventListener('change', handler);
    return () => this.removeEventListener('change', handler);
  }

  // ===== Internal =====

  #wrap(value, exp) {
    return { [this.ns]: 1, v: value, exp: exp ?? null };
  }
  #unwrap(rec) {
    if (!rec || rec[this.ns] !== 1) return null; // bukan milik wrapper ini
    if (rec.exp && Date.now() >= rec.exp) return null; // sudah kedaluwarsa (lazy guard)
    return rec.v;
  }

  #emitChange(key, value, meta) {
    this.dispatchEvent(new CustomEvent('change', { detail: { key, value, meta } }));
  }

  #startSafetySweep() {
    setInterval(() => this.#sweepExpired({ via: 'safety' }), this.safetySweepMs);
  }

  #scheduleSweepSoon(delayMs) {
    clearTimeout(this.nextTimer);
    this.nextTimer = setTimeout(() => this.#sweepExpired({ via: 'soon' }), delayMs);
  }

  #scheduleNextByNearestExpiry() {
    // Cari expiry terdekat dan pasang timer tepat di sana (+ kecil buffer)
    const now = Date.now();
    let nearest = Infinity;

    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      const rec = safeParse(localStorage.getItem(k));
      if (!rec || rec[this.ns] !== 1) continue;
      if (typeof rec.exp === 'number' && rec.exp > now) {
        nearest = Math.min(nearest, rec.exp);
      }
    }

    if (nearest !== Infinity) {
      const delay = Math.max(0, nearest - now) + 10; // +10ms buffer
      this.#scheduleSweepSoon(delay);
    }
  }

  async #sweepExpired(meta = {}) {
    // Aman dieksekusi paralel di banyak tab; operasi idempotent.
    const now = Date.now();
    const expiredKeys = [];

    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      const rec = safeParse(localStorage.getItem(k));
      if (!rec || rec[this.ns] !== 1) continue;
      if (typeof rec.exp === 'number' && rec.exp <= now) {
        expiredKeys.push(k);
      }
    }

    if (expiredKeys.length) {
      expiredKeys.forEach(k => localStorage.removeItem(k));
      expiredKeys.forEach(k => {
        this.#emitChange(k, undefined, { ...meta, removed: true, expired: true });
        this.bc?.postMessage({ type: 'update', key: k, value: undefined, removed: true, expired: true });
      });
    }

    // Jadwalkan berikutnya berdasar expiry sisa (kalau ada)
    this.#scheduleNextByNearestExpiry();

    // Minta tab lain juga sweep (mempercepat konsistensi)
    this.bc?.postMessage({ type: 'sweepNow' });
  }
}

// ==== Contoh pemakaian ====
// const store = new ReactiveStorageTTL();
// store.subscribe('session', (val, meta) => console.log('session=', val, meta));
// store.setItem('session', { user: 'fajar' }, { ttlMs: 15_000 }); // akan auto-terhapus ~15 detik
