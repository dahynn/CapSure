// import { httpClient } from '@/common/api/httpClient';

/**
 * Auth(인증) 관련 API 엔드포인트 모음
 */
export const authApi = {
    /**
     * 로그인
     * @param {Object} credentials - 로그인 정보 (예: { email, password })
     */
    login: async (credentials) => {
        // 실제 API 연동 시 주석 해제
        // const response = await httpClient.post('/auth/login', credentials);
        // return response.data;
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({ success: true, message: '로그인 성공', data: { email: credentials.email, name: '테스트 유저' } });
            }, 500);
        });
    },

    /**
     * 회원가입
     * @param {Object} userData - 가입 폼 데이터 (예: { email, password, name })
     */
    signup: async (userData) => {
        // 실제 API 연동 시 주석 해제
        // const response = await httpClient.post('/auth/signup', userData);
        // return response.data;
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({ success: true, message: '회원가입 성공' });
            }, 1000);
        });
    },

    /**
     * 이메일 중복 확인 (무조건 승인)
     * @param {string} email
     */
    checkEmail: async (email) => {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({ success: true, message: '사용 가능한 이메일입니다.' });
            }, 300);
        });
    },

    /**
     * 로그아웃
     */
    logout: async () => {
        // 실제 API 연동 시 주석 해제
        // const response = await httpClient.post('/auth/logout');
        // return response.data;
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({ success: true });
            }, 300);
        });
    },
};
