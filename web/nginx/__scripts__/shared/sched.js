export const hasPTSA = typeof scheduler !== 'undefined' && typeof scheduler.postTask === 'function';

/**
 * Schedule a function with priority. Returns { signal, cancel } to abort.
 * @param {Function} fn
 * @param {('user-blocking'|'user-visible'|'background')} priority
 * @param {number} [delayMs] optional delay
 */
export function scheduleTask(fn, priority = 'background', delayMs = 0) {
    const ac = new AbortController();
    const { signal } = ac;

    if (!hasPTSA) {
        // Fallback: delay -> setTimeout, then microtask via Promise.
        const id = setTimeout(() => { if (!signal.aborted) Promise.resolve().then(fn); }, delayMs);
        return { signal, cancel: () => { ac.abort(); clearTimeout(id); } };
    }

    if (delayMs > 0) {
        const id = setTimeout(() => {
            if (signal.aborted) return;
            scheduler.postTask(fn, { priority, signal }).catch(() => { });
        }, delayMs);
        return { signal, cancel: () => { ac.abort(); clearTimeout(id); } };
    }

    const task = scheduler.postTask(fn, { priority, signal });
    return { signal, cancel: () => ac.abort() };
}

/** Cooperative yielding for long loops */
export async function cooperativeYield(priority = 'background') {
    if (hasPTSA && scheduler.yield) {
        await scheduler.yield({ priority });
    } else {
        // Fallback micro-yield
        await new Promise(requestAnimationFrame);
    }
}