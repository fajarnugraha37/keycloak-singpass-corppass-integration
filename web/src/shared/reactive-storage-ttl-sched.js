// reactive-storage-ttl.js
// Evented localStorage wrapper with TTL, cross‑tab sync, and auto-sweeper
// "True TTL" here means: items are removed when TTL elapses without requiring a get().

export class ReactiveStorageTTLSched extends EventTarget {
  #safetyHandle = null;

  /**
   * @param {object} opts
   * @param {string} [opts.channel='lsync']
   * @param {string} [opts.namespace='__rs__'] // marker to identify our records
   * @param {number} [opts.safetySweepMs=60_000] // periodic fallback sweep
   * @param {boolean} [opts.singleSweeper=false] // use Web Locks to make one tab sweep
   */
  constructor(opts = {}) {
    super();
    this.ns = opts.namespace ?? '__rs__';
    this.bc = typeof BroadcastChannel !== 'undefined'
      ? new BroadcastChannel(opts.channel ?? 'lsync')
      : null;
    this.safetySweepMs = opts.safetySweepMs ?? 60_000;
    this.singleSweeper = !!opts.singleSweeper;
    this.nextTimer = null;

    // Cross-tab browser event
    window.addEventListener('storage', (e) => {
      if (e.storageArea !== localStorage) return;
      this.#emitChange(e.key, safeParse(e.newValue), { external: true, origin: 'storage' });
      this.#scheduleSweepSoon(250);
    });

    // BroadcastChannel
    this.bc && (this.bc.onmessage = (e) => {
      const msg = e.data || {};
      if (msg.type === 'update') {
        this.#emitChange(msg.key, msg.value, { external: true, origin: 'broadcast' });
      } else if (msg.type === 'sweepNow') {
        this.#sweepExpired({ via: 'broadcast' });
      }
    });

    // Initial + periodic sweeps
    this.#scheduleSweepSoon(300);
    this.#startSafetySweep();

    // Catch up after tab resumes
    ['visibilitychange', 'focus', 'online'].forEach(ev =>
      window.addEventListener(ev, () => this.#scheduleSweepSoon(200))
    );
  }

  // ===== Public API =====

  getItem(key, fallback = null) {
    const rec = safeParse(localStorage.getItem(key));
    const val = this.#unwrap(rec);
    return val ?? fallback;
  }

  /**
   * @param {string} key
   * @param {any} value
   * @param {object} [opts]
   * @param {number} [opts.ttlMs]  // TTL duration (ms)
   */
  setItem(key, value, opts = {}) {
    const exp = typeof opts.ttlMs === 'number' ? Date.now() + Math.max(0, opts.ttlMs) : null;
    const rec = this.#wrap(value, exp);
    localStorage.setItem(key, JSON.stringify(rec));
    this.#emitChange(key, value, { external: false, origin: 'local', exp });
    this.bc?.postMessage({ type: 'update', key, value });
    this.#scheduleNextByNearestExpiry();
  }

  removeItem(key) {
    localStorage.removeItem(key);
    this.#emitChange(key, undefined, { external: false, origin: 'local', removed: true });
    this.bc?.postMessage({ type: 'update', key, value: undefined, removed: true });
  }

  subscribe(key, cb) {
    const handler = (evt) => { if (evt.detail.key === key) cb(evt.detail.value, evt.detail.meta); };
    this.addEventListener('change', handler);
    cb(this.getItem(key), { init: true });
    return () => this.removeEventListener('change', handler);
  }

  subscribeAll(cb) {
    const handler = (evt) => cb(evt.detail.key, evt.detail.value, evt.detail.meta);
    this.addEventListener('change', handler);
    return () => this.removeEventListener('change', handler);
  }

  // ===== Internals =====

  #wrap(value, exp) {
    return { [this.ns]: 1, v: value, exp: exp ?? null };
  }
  #unwrap(rec) {
    if (!rec || rec[this.ns] !== 1) return null;
    if (rec.exp && Date.now() >= rec.exp) return null; // lazy guard
    return rec.v;
  }

  #emitChange(key, value, meta) {
    this.dispatchEvent(new CustomEvent('change', { detail: { key, value, meta } }));
  }

  // #startSafetySweep() {
  //   setInterval(() => this.#sweepExpired({ via: 'safety' }), this.safetySweepMs);
  // }
  #startSafetySweep() {
    // Recur using background tasks instead of setInterval
    const loop = () => {
      this.#sweepExpired({ via: 'safety' });
      // schedule next safety sweep
      this.#safetyHandle = scheduleTask(loop, 'background', this.safetySweepMs);
    };
    this.#safetyHandle = scheduleTask(loop, 'background', this.safetySweepMs);
  }

  // #scheduleSweepSoon(delayMs) {
  //   clearTimeout(this.nextTimer);
  //   this.nextTimer = setTimeout(() => this.#sweepExpired({ via: 'soon' }), delayMs);
  // }
  #scheduleSweepSoon(delayMs) {
    // Cancel previously scheduled task
    if (this.nextTask?.cancel) this.nextTask.cancel();
    this.nextTask = scheduleTask(() => this.#sweepExpired({ via: 'soon' }), 'background', delayMs);
  }

  // #scheduleNextByNearestExpiry() {
  //   const now = Date.now();
  //   let nearest = Infinity;

  //   for (let i = 0; i < localStorage.length; i++) {
  //     const k = localStorage.key(i);
  //     const rec = safeParse(localStorage.getItem(k));
  //     if (!rec || rec[this.ns] !== 1) continue;
  //     if (typeof rec.exp === 'number' && rec.exp > now) {
  //       nearest = Math.min(nearest, rec.exp);
  //     }
  //   }

  //   if (nearest !== Infinity) {
  //     const delay = Math.max(0, nearest - now) + 10; // tiny buffer
  //     this.#scheduleSweepSoon(delay);
  //   }
  // }
  #scheduleNextByNearestExpiry() {
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
      const delay = Math.max(0, nearest - now) + 10;
      // eviction at expiry gets slightly higher priority
      if (this.nextTask?.cancel) this.nextTask.cancel();
      this.nextTask = scheduleTask(() => this.#sweepExpired({ via: 'expiry' }), 'user-visible', delay);
    }
  }

  async #sweepExpired(meta = {}) {
    if (this.singleSweeper && 'locks' in navigator) {
      return navigator.locks.request('ls-ttl-sweep', { mode: 'exclusive' }, async () => this.#doSweep(meta));
    }
    return this.#doSweep(meta);
  }

  // async #doSweep(meta) {
  //   const now = Date.now();
  //   const expiredKeys = [];

  //   for (let i = 0; i < localStorage.length; i++) {
  //     const k = localStorage.key(i);
  //     const rec = safeParse(localStorage.getItem(k));
  //     if (!rec || rec[this.ns] !== 1) continue;
  //     if (typeof rec.exp === 'number' && rec.exp <= now) {
  //       expiredKeys.push(k);
  //     }
  //   }

  //   if (expiredKeys.length) {
  //     expiredKeys.forEach(k => localStorage.removeItem(k));
  //     expiredKeys.forEach(k => {
  //       this.#emitChange(k, undefined, { ...meta, removed: true, expired: true });
  //       this.bc?.postMessage({ type: 'update', key: k, value: undefined, removed: true, expired: true });
  //     });
  //   }

  //   this.#scheduleNextByNearestExpiry();
  //   this.bc?.postMessage({ type: 'sweepNow' });
  // }
  async #doSweep(meta) {
    const now = Date.now();
    const expiredKeys = [];

    for (let i = 0; i < localStorage.length; i) {
      const k = localStorage.key(i);
      const rec = safeParse(localStorage.getItem(k));
      if (!rec || rec[this.ns] !== 1) continue;
      if (typeof rec.exp === 'number' && rec.exp <= now) {
        expiredKeys.push(k);
      }
      // Every ~20 keys, yield to keep UI smooth
      if (i % 20 === 0) {
        const { cooperativeYield } = await import('./sched.js');
        await cooperativeYield('background');
      }
    }
  }
}