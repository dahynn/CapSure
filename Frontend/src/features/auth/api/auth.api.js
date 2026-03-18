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
     * 최초 로그인 여부 확인 모의 API
     * @returns {Promise<{ isFirstLogin: boolean }>}
     */
    checkFirstLogin: async () => {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    isFirstLogin: true // 테스트를 위해 항상 true 반환
                });
            }, 500);
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

    /**
     * 로그인 세션 연장
     */
    extendSession: async () => {
        // 실제 API 연동 시 주석 해제
        // const response = await httpClient.post('/auth/extend-session');
        // return response.data;
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({ success: true, message: '세션이 연장되었습니다.' });
            }, 500);
        });
    },
};
