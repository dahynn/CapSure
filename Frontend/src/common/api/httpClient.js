// Vite proxy 사용 - /auth, /subscriptions 등은 vite.config.js에서 localhost:8080으로 프록시됨
const BASE_URL = '';  // 직접 호출 대신 Vite proxy 경유
let refreshPromise = null;

const getAccessToken = () => localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
const getRefreshToken = () => localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken');
const hasLocalAuthToken = () => Boolean(localStorage.getItem('accessToken') || localStorage.getItem('refreshToken'));
const hasSessionAuthToken = () => Boolean(sessionStorage.getItem('accessToken') || sessionStorage.getItem('refreshToken'));

const getAuthHeaders = () => {
    const token = getAccessToken();
    return {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
    };
};

const clearAuthStorage = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('authRole');
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
    sessionStorage.removeItem('authRole');
};

const setAuthStorage = ({ accessToken, refreshToken, role }) => {
    const useSession = !hasLocalAuthToken() && hasSessionAuthToken();
    const targetStorage = useSession ? sessionStorage : localStorage;
    const mirrorStorage = useSession ? localStorage : sessionStorage;

    if (accessToken) {
        targetStorage.setItem('accessToken', accessToken);
        mirrorStorage.removeItem('accessToken');
    }
    if (refreshToken) {
        targetStorage.setItem('refreshToken', refreshToken);
        mirrorStorage.removeItem('refreshToken');
    }
    if (role) {
        targetStorage.setItem('authRole', role);
        mirrorStorage.removeItem('authRole');
    }
};

const shouldSkipRefresh = (url) => (
    url.startsWith('/auth/login')
    || url.startsWith('/auth/signup')
    || url.startsWith('/auth/refresh')
);

const refreshAccessToken = async () => {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
        throw new Error('No refresh token');
    }

    if (!refreshPromise) {
        refreshPromise = fetch(`${BASE_URL}/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
        })
            .then(async (response) => {
                const payload = await response.json().catch(() => ({}));
                if (!response.ok || !payload?.success) {
                    throw new Error(payload?.message || 'Refresh token failed');
                }
                const data = payload?.data || {};
                if (!data.accessToken) {
                    throw new Error('Missing access token');
                }
                setAuthStorage({
                    accessToken: data.accessToken,
                    refreshToken: data.refreshToken,
                    role: data.role,
                });
                return data.accessToken;
            })
            .finally(() => {
                refreshPromise = null;
            });
    }

    return refreshPromise;
};

const handleResponse = async (response) => {
    const payload = await response.json().catch(() => ({}));

    if (!response.ok) {
        const message = payload?.message || `HTTP error! status: ${response.status}`;
        const error = new Error(message);
        error.response = response;
        error.payload = payload;
        throw error;
    }
    return { data: payload };
};

const request = async (method, url, body, canRetry = true, options = {}) => {
    const requestHeaders = {
        ...getAuthHeaders(),
        ...(options.headers || {}),
    };

    const response = await fetch(`${BASE_URL}${url}`, {
        ...options,
        method,
        headers: requestHeaders,
        ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });

    const parsedPayload = await response.clone().json().catch(() => ({}));
    const isUnauthorizedLike =
        response.status === 401
        || (response.status === 403 && parsedPayload?.errorCode === 'UNAUTHORIZED');

    if (isUnauthorizedLike && canRetry && !shouldSkipRefresh(url)) {
        try {
            await refreshAccessToken();
            return request(method, url, body, false, options);
        } catch (refreshError) {
            clearAuthStorage();
            if (window.location.pathname !== '/login') {
                window.location.href = '/login';
            }
            throw refreshError;
        }
    }

    return handleResponse(response);
};

export const httpClient = {
    get: async (url, options = {}) => {
        return request('GET', url, undefined, true, options);
    },
    post: async (url, body, options = {}) => {
        return request('POST', url, body, true, options);
    },
    put: async (url, body, options = {}) => {
        return request('PUT', url, body, true, options);
    },
    delete: async (url, options = {}) => {
        return request('DELETE', url, undefined, true, options);
    },
};
