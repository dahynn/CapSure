import axios from 'axios';

// TODO: BE 서버가 준비되면 실제 서버 환경 변수나 주소로 변경해야 합니다.
const BASE_URL = 'http://localhost:8080/api'; // 임시 BE 서버 주소

export const httpClient = axios.create({
    baseURL: BASE_URL,
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 요청 인터셉터 (Request Interceptor)
httpClient.interceptors.request.use(
    (config) => {
        // TODO: 실제 토큰 관리 방식(localStorage, sessionStorage, Zustand, Cookie 등)에 맞게 수정해야 합니다.
        const accessToken = 'temp-access-token'; // 임시 Access Token

        if (accessToken) {
            config.headers.Authorization = `Bearer ${accessToken}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// 응답 인터셉터 (Response Interceptor)
httpClient.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        const originalRequest = error.config;

        // 401 Unauthorized 에러이고 이전에 재시도한 적이 없는 경우
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                // TODO: 실제 Refresh Token을 꺼내와서 Access Token 재발급 API를 호출하도록 수정해야 합니다.
                // const refreshToken = 'temp-refresh-token';
                // const response = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken });
                // const newAccessToken = response.data.accessToken;

                const newAccessToken = 'new-temp-access-token'; // 임시 발급된 토큰이라고 가정

                // TODO: 새로 발급받은 Access Token을 스토리지에 갱신하는 로직 추가

                // 새로 발급받은 토큰으로 기존 요청의 헤더를 수정
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

                // 실패했던 기존 요청을 재시도
                return httpClient(originalRequest);
            } catch (refreshError) {
                // TODO: 재발급 실패 시 로그아웃 처리 및 로그인 페이지로 리다이렉트 하는 로직 추가
                console.error('Refresh Token 만료 또는 갱신 실패', refreshError);
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);
