const STORAGE_KEY = 'latestCapsureSubscription';

export const saveLatestCapsureSubscription = (summary) => {
    if (typeof window === 'undefined') {
        return;
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(summary));
};

export const getLatestCapsureSubscription = () => {
    if (typeof window === 'undefined') {
        return null;
    }

    const rawValue = localStorage.getItem(STORAGE_KEY);
    if (!rawValue) {
        return null;
    }

    try {
        return JSON.parse(rawValue);
    } catch (error) {
        console.error('Failed to parse latest capsure subscription', error);
        return null;
    }
};
