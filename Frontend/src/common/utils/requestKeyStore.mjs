// Keep one key per logical operation, including synchronous duplicate calls and reloads.
export const createRequestKeyStore = (storage, storageKey, createKey = (name) =>
    `${name}-${globalThis.crypto.randomUUID()}`) => {
    let keys = new Map();
    try {
        const stored = JSON.parse(storage.getItem(storageKey) || '{}');
        keys = new Map(Object.entries(stored).filter(([, value]) => typeof value === 'string' && value.length > 0));
    } catch {
        // Storage can be unavailable or contain an interrupted/older session.
    }

    return {
        get(name) {
            if (!keys.has(name)) {
                keys.set(name, createKey(name));
                try { storage.setItem(storageKey, JSON.stringify(Object.fromEntries(keys))); } catch { /* memory fallback */ }
            }
            return keys.get(name);
        },
        clear() {
            keys.clear();
            try { storage.removeItem(storageKey); } catch { /* memory fallback */ }
        },
    };
};
