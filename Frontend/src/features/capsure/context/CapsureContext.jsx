import React, { createContext, useContext, useEffect, useState } from 'react';
import { getProductSourceId, normalizeProductSource } from '../utils/productSource';
import {
    clearCapsureDraft,
    getCapsureDraft,
    saveCapsureDraft,
    saveLatestCapsureSubscription,
} from '../utils/capsuleStorage';

const CapsureContext = createContext(null);

export const useCapsure = () => {
    const context = useContext(CapsureContext);
    if (!context) {
        throw new Error('useCapsure must be used within a CapsureProvider');
    }
    return context;
};

export const CapsureProvider = ({ children }) => {
    const initialDraft = getCapsureDraft();
    const initialSelectedProducts = Array.isArray(initialDraft?.selectedProducts)
        ? initialDraft.selectedProducts.map(normalizeProductSource)
        : [];
    const initialTotalBudget = Number(initialDraft?.totalBudget);
    const initialCapsuleName = typeof initialDraft?.capsuleName === 'string'
        ? initialDraft.capsuleName
        : '';

    // Global Subscription State
    const [hasSubscription, setHasSubscription] = useState(false);
    const [checkoutSummary, setCheckoutSummary] = useState(null);
    const [capsuleName, setCapsuleName] = useState(initialCapsuleName);
    
    // Budget Page State
    const [totalBudget, setTotalBudget] = useState(Number.isFinite(initialTotalBudget) && initialTotalBudget > 0 ? initialTotalBudget : 100000);
    
    // Maker Page State
    const [selectedProducts, setSelectedProducts] = useState(initialSelectedProducts);

    useEffect(() => {
        saveCapsureDraft({
            totalBudget,
            selectedProducts,
            capsuleName,
        });
    }, [totalBudget, selectedProducts, capsuleName]);

    const handleAddItem = (product) => {
        const normalizedProduct = normalizeProductSource(product);
        const price = normalizedProduct.monthlyPrice;
        const productId = getProductSourceId(normalizedProduct);
        
        const currentAmount = selectedProducts.reduce((sum, p) => sum + p.monthlyPrice, 0);
        if (currentAmount + price > totalBudget) {
            alert('예산을 초과할 수 없습니다.');
            return false;
        }
        if (selectedProducts.find((p) => getProductSourceId(p) === productId)) return false;
        
        setSelectedProducts((previousProducts) => [...previousProducts, normalizedProduct]);
        return true;
    };

    const handleRemoveItem = (id) => {
        setSelectedProducts((previousProducts) => previousProducts.filter((p) => getProductSourceId(p) !== id));
    };

    const completeSubscription = ({ subscriptionId, products, capsuleName: nextCapsuleName }) => {
        const normalizedProducts = products.map(normalizeProductSource);
        const totalPremium = normalizedProducts.reduce((sum, product) => sum + product.monthlyPrice, 0);
        const resolvedCapsuleName = nextCapsuleName?.trim() || capsuleName;

        setHasSubscription(true);
        const summary = {
            subscriptionId,
            capsuleName: resolvedCapsuleName,
            products: normalizedProducts,
            totalPremium,
        };
        setCheckoutSummary(summary);
        saveLatestCapsureSubscription(summary);
        clearCapsureDraft();
        setSelectedProducts([]);
        setCapsuleName('');
    };

    const value = {
        hasSubscription,
        setHasSubscription,
        checkoutSummary,
        setCheckoutSummary,
        capsuleName,
        setCapsuleName,
        totalBudget,
        setTotalBudget,
        selectedProducts,
        setSelectedProducts,
        handleAddItem,
        handleRemoveItem,
        completeSubscription
    };

    return (
        <CapsureContext.Provider value={value}>
            {children}
        </CapsureContext.Provider>
    );
};
