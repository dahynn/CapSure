const STORAGE_KEY = 'latestCapsureSubscription';
const DRAFT_STORAGE_KEY = 'capsureFlowDraft';

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

export const saveCapsureDraft = (draft) => {
    if (typeof window === 'undefined') {
        return;
    }
    localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
};

export const getCapsureDraft = () => {
    if (typeof window === 'undefined') {
        return null;
    }

    const rawValue = localStorage.getItem(DRAFT_STORAGE_KEY);
    if (!rawValue) {
        return null;
    }

    try {
        return JSON.parse(rawValue);
    } catch (error) {
        console.error('Failed to parse capsure flow draft', error);
        return null;
    }
};

export const clearCapsureDraft = () => {
    if (typeof window === 'undefined') {
        return;
    }
    localStorage.removeItem(DRAFT_STORAGE_KEY);
};
