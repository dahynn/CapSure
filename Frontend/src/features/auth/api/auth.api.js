import { httpClient } from '@/common/api/httpClient';

/**
 * Auth(인증) 관련 API 엔드포인트 모음
 */
export const authApi = {
    /**
     * 로그인
     * @param {Object} credentials - 로그인 정보 (예: { email, password })
     */
    login: async (credentials) => {
        // TODO: 실제 BE 서버의 로그인 API 엔드포인트 주소로 변경해야 합니다.
        const response = await httpClient.post('/auth/login', credentials);
        return response.data;
    },

    /**
     * 회원가입
     * @param {Object} userData - 가입 폼 데이터 (예: { email, password, name })
     */
    signup: async (userData) => {
        // TODO: 실제 BE 서버의 회원가입 API 엔드포인트 주소로 변경해야 합니다.
        const response = await httpClient.post('/auth/signup', userData);
        return response.data;
    },

    /**
     * 로그아웃
     */
    logout: async () => {
        // TODO: 실제 BE 서버의 로그아웃 API 엔드포인트 주소로 변경해야 합니다.
        const response = await httpClient.post('/auth/logout');
        return response.data;
    },
};
