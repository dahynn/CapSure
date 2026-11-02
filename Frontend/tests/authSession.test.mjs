import test, { beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { clearAuthStorage, httpClient, refreshAccessToken } from '../src/common/api/httpClient.js';
import { tokenRemainingSeconds } from '../src/common/utils/tokenExpiry.mjs';

const storage = () => {
    const entries = new Map();
    return { getItem: (key) => entries.get(key) ?? null,
        setItem: (key, value) => entries.set(key, String(value)), removeItem: (key) => entries.delete(key) };
};
const response = (status, data) => new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } });
const seed = () => { localStorage.setItem('accessToken', 'old-access'); localStorage.setItem('refreshToken', 'old-refresh'); };

beforeEach(() => {
    globalThis.localStorage = storage(); globalThis.sessionStorage = storage();
    globalThis.window = { location: { pathname: '/dashboard', href: '' } };
    Object.defineProperty(globalThis, 'navigator', { configurable: true, value: {} });
    seed();
});

test('manual and automatic refresh share one in-flight rotation', async () => {
    let calls = 0;
    globalThis.fetch = async () => { calls++; return response(200, { success: true, data: { accessToken: 'new-access', refreshToken: 'new-refresh' } }); };
    assert.deepEqual(await Promise.all([refreshAccessToken(), refreshAccessToken()]), ['new-access', 'new-access']);
    assert.equal(calls, 1);
    assert.equal(localStorage.getItem('refreshToken'), 'new-refresh');
});

test('concurrent 401 requests rotate once and retry using the new access token', async () => {
    let rotations = 0;
    globalThis.fetch = async (url, options) => {
        if (url === '/auth/refresh') {
            rotations++; return response(200, { success: true, data: { accessToken: 'new-access', refreshToken: 'new-refresh' } });
        }
        return options.headers.Authorization === 'Bearer new-access'
            ? response(200, { success: true, data: 42 }) : response(401, { success: false });
    };
    const results = await Promise.all([httpClient.get('/dashboard/summary'), httpClient.get('/auth/profile')]);
    assert.equal(rotations, 1);
    assert.ok(results.every(result => result.data.data === 42));
});

test('temporary refresh outage keeps the session and does not redirect to login', async () => {
    globalThis.fetch = async (url) => url === '/auth/refresh'
        ? response(503, { success: false, message: 'temporary outage' }) : response(401, { success: false });
    await assert.rejects(httpClient.get('/dashboard/summary'), /temporary outage/);
    assert.equal(localStorage.getItem('refreshToken'), 'old-refresh');
    assert.equal(window.location.href, '');
});

test('late refresh failure does not erase a newer login', async () => {
    let finish;
    globalThis.fetch = () => new Promise(resolve => { finish = resolve; });
    const pending = refreshAccessToken();
    clearAuthStorage();
    localStorage.setItem('accessToken', 'different-user-access');
    localStorage.setItem('refreshToken', 'different-user-refresh');
    finish(response(401, { success: false }));
    await assert.rejects(pending, error => error.status === 409);
    assert.equal(localStorage.getItem('refreshToken'), 'different-user-refresh');
});

test('cross-tab lock reuses a rotation that completed while waiting', async () => {
    navigator.locks = { request: async (name, callback) => {
        assert.equal(name, 'capsure-auth-refresh');
        localStorage.setItem('refreshToken', 'other-tab-refresh');
        localStorage.setItem('accessToken', 'other-tab-access');
        return callback();
    } };
    globalThis.fetch = () => assert.fail('must not rotate again');
    assert.equal(await refreshAccessToken(), 'other-tab-access');
});

test('invalid refresh clears only CapSure session state', async () => {
    sessionStorage.setItem('unrelated-draft', 'preserve');
    sessionStorage.setItem('capsure:cancer-insurance-flow:v1', 'synthetic');
    globalThis.fetch = async () => response(401, { success: false });
    await assert.rejects(httpClient.get('/dashboard/summary'));
    assert.equal(localStorage.getItem('accessToken'), null);
    assert.equal(sessionStorage.getItem('capsure:cancer-insurance-flow:v1'), null);
    assert.equal(sessionStorage.getItem('unrelated-draft'), 'preserve');
    assert.equal(window.location.href, '/login');
});

test('session timer uses JWT expiry and handles absent, invalid and expired tokens', () => {
    const token = `header.${Buffer.from(JSON.stringify({ exp: 100 })).toString('base64url')}.signature`;
    assert.equal(tokenRemainingSeconds(token, 40_001), 60);
    assert.equal(tokenRemainingSeconds(token, 100_000), 0);
    assert.equal(tokenRemainingSeconds('invalid', 0), 0);
    assert.equal(tokenRemainingSeconds(null, 0), 0);
});
