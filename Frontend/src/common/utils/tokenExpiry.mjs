export const tokenRemainingSeconds = (token, now = Date.now()) => {
    if (!token) return 0;
    try {
        const segment = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const { exp } = JSON.parse(atob(segment));
        return Number.isFinite(exp) ? Math.max(0, Math.ceil((exp * 1000 - now) / 1000)) : 0;
    } catch { return 0; }
};
