import assert from 'node:assert/strict';
import { test } from 'node:test';
import { createRequestKeyStore } from '../src/common/utils/requestKeyStore.mjs';

const memoryStorage = () => {
    const values = new Map();
    return {
        getItem: (key) => values.get(key) ?? null,
        setItem: (key, value) => values.set(key, value),
        removeItem: (key) => values.delete(key),
    };
};

test('synchronous repeat, rerender and reload reuse the same operation key', () => {
    const storage = memoryStorage();
    let generated = 0;
    const generator = (name) => `${name}-${++generated}`;
    const first = createRequestKeyStore(storage, 'flow', generator);
    const key = first.get('claim-submit-42');
    assert.equal(first.get('claim-submit-42'), key);
    const reloaded = createRequestKeyStore(storage, 'flow', generator);
    assert.equal(reloaded.get('claim-submit-42'), key);
    assert.equal(generated, 1);
    assert.notEqual(reloaded.get('claim-submit-43'), key);
});

test('reset removes previous keys from memory and session storage', () => {
    const storage = memoryStorage();
    let generated = 0;
    const store = createRequestKeyStore(storage, 'flow', () => `key-${++generated}`);
    const old = store.get('submit');
    store.clear();
    assert.equal(storage.getItem('flow'), null);
    assert.notEqual(store.get('submit'), old);
});

test('invalid saved state and unavailable storage preserve in-memory idempotency', () => {
    for (const raw of ['null', '[', '{"submit":false}']) {
        const storage = memoryStorage();
        storage.setItem('flow', raw);
        assert.equal(createRequestKeyStore(storage, 'flow', () => 'valid').get('submit'), 'valid');
    }
    const blocked = { getItem() { throw Error(); }, setItem() { throw Error(); }, removeItem() { throw Error(); } };
    let calls = 0;
    const store = createRequestKeyStore(blocked, 'flow', () => `key-${++calls}`);
    assert.equal(store.get('submit'), store.get('submit'));
    assert.equal(calls, 1);
    store.clear();
    assert.equal(store.get('submit'), 'key-2');
});
