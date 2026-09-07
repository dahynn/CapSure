import { httpClient, clearAuthStorage, getAccessToken, refreshAccessToken, setAuthStorage } from '@/common/api/httpClient';

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
        if (data?.accessToken) { clearAuthStorage(); setAuthStorage(data); }
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
        try {
            const response = await httpClient.get('/auth/onboarding/categories');
            const categoryCodes = response.data?.data;
            const isCompleted = Array.isArray(categoryCodes) && categoryCodes.length >= 1;

            if (isCompleted) {
                localStorage.setItem('onboardingDone', 'true');
            } else {
                localStorage.removeItem('onboardingDone');
            }

            return { isFirstLogin: !isCompleted };
        } catch (error) {
            const onboardingDone = localStorage.getItem('onboardingDone');
            return { isFirstLogin: !onboardingDone };
        }
    },

    /**
     * 로그아웃
     */
    logout: async () => {
        const accessToken = getAccessToken();
        clearAuthStorage();
        try {
            await httpClient.post('/auth/logout', {}, { headers: { Authorization: `Bearer ${accessToken || ''}` } });
        } catch (e) {
            // 서버 로그아웃 실패해도 로컬 토큰 제거
        }
        return { success: true };
    },

    /**
     * 로그인 세션 연장
     */
    extendSession: async () => {
        try {
            await refreshAccessToken();
            return { success: true };
        } catch (error) {
            if ([401, 403].includes(error.status)) clearAuthStorage();
            throw error;
        }
    },

    sendEmailCode: async (email) => (await httpClient.post('/auth/email/send-code', { email })).data?.data,
    verifyEmailCode: async (email, authCode) => (await httpClient.post('/auth/email/verify-code', { email, authCode })).data?.data,

    saveOnboardingCategories: async (categoryCodes) => {
        const response = await httpClient.post('/auth/onboarding/categories', { categoryCodes });
        return response.data?.data;
    },

    getOnboardingCategories: async () => {
        const response = await httpClient.get('/auth/onboarding/categories');
        return response.data?.data;
    },
};
