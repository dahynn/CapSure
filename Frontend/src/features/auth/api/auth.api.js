import { httpClient } from '@/common/api/httpClient';

/**
 * Auth(인증) 관련 API 엔드포인트 모음
 */
export const authApi = {
    /**
     * 로그인 - 실제 백엔드 API 연동 + JWT 토큰 저장
     */
    login: async (credentials) => {
        const response = await httpClient.post('/auth/login', credentials);
        const data = response.data?.data;
        if (data?.accessToken) {
            localStorage.setItem('accessToken', data.accessToken);
        }
        if (data?.refreshToken) {
            localStorage.setItem('refreshToken', data.refreshToken);
        }
        return data;
    },

    /**
     * 회원가입
     */
    signup: async (userData) => {
        const response = await httpClient.post('/auth/signup', userData);
        return response.data?.data;
    },

    /**
     * 이메일 중복 확인
     */
    checkEmail: async (email) => {
        const response = await httpClient.get(`/auth/check-email?email=${encodeURIComponent(email)}`);
        return response.data?.data;
    },

    /**
     * 최초 로그인 여부 확인
     */
    checkFirstLogin: async () => {
        // 회원가입 후 온보딩 여부는 서버에서 받거나, 로그인 응답에 포함된 정보로 판단
        // 임시: 로컬스토리지에 온보딩 완료 여부 저장
        const onboardingDone = localStorage.getItem('onboardingDone');
        return { isFirstLogin: !onboardingDone };
    },

    /**
     * 로그아웃
     */
    logout: async () => {
        try {
            await httpClient.post('/auth/logout', {});
        } catch (e) {
            // 서버 로그아웃 실패해도 로컬 토큰 제거
        }
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        return { success: true };
    },

    /**
     * 로그인 세션 연장
     */
    extendSession: async () => {
        const response = await httpClient.post('/auth/extend-session', {});
        return response.data?.data;
    },
};
