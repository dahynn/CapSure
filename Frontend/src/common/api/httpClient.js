// Native fetch-based HTTP client (axios 의존성 없음)
const BASE_URL = 'http://localhost:8080';

const getAuthHeaders = () => {
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    return {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
    };
};

const handleResponse = async (response) => {
    if (!response.ok) {
        const error = new Error(`HTTP error! status: ${response.status}`);
        error.response = response;
        throw error;
    }
    return { data: await response.json() };
};

export const httpClient = {
    get: async (url) => {
        const response = await fetch(`${BASE_URL}${url}`, {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        return handleResponse(response);
    },
    post: async (url, body) => {
        const response = await fetch(`${BASE_URL}${url}`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(body),
        });
        return handleResponse(response);
    },
    put: async (url, body) => {
        const response = await fetch(`${BASE_URL}${url}`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify(body),
        });
        return handleResponse(response);
    },
    delete: async (url) => {
        const response = await fetch(`${BASE_URL}${url}`, {
            method: 'DELETE',
            headers: getAuthHeaders(),
        });
        return handleResponse(response);
    },
};
