import React, { createContext, useContext, useState } from 'react';
import { getProductSourceId, normalizeProductSource } from '../utils/productSource';
import { saveLatestCapsureSubscription } from '../utils/capsuleStorage';

const CapsureContext = createContext(null);

export const useCapsure = () => {
    const context = useContext(CapsureContext);
    if (!context) {
        throw new Error('useCapsure must be used within a CapsureProvider');
    }
    return context;
};

export const CapsureProvider = ({ children }) => {
    // Global Subscription State
    const [hasSubscription, setHasSubscription] = useState(false);
    const [checkoutSummary, setCheckoutSummary] = useState(null);
    const [capsuleName, setCapsuleName] = useState('');
    
    // Budget Page State
    const [totalBudget, setTotalBudget] = useState(100000);
    
    // Maker Page State
    const [selectedProducts, setSelectedProducts] = useState([]);

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
        
        setSelectedProducts([...selectedProducts, normalizedProduct]);
        return true;
    };

    const handleRemoveItem = (id) => {
        setSelectedProducts(selectedProducts.filter((p) => getProductSourceId(p) !== id));
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
