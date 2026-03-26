import React, { createContext, useContext, useState } from 'react';

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
    
    // Budget Page State
    const [totalBudget, setTotalBudget] = useState(100000);
    
    // Maker Page State
    const [selectedProducts, setSelectedProducts] = useState([]);

    const handleAddItem = (product) => {
        const price = Number(product.monthlyPrice || product.price || 0);
        const productId = product.productSourceId || product.id;
        
        const currentAmount = selectedProducts.reduce((sum, p) => sum + Number(p.monthlyPrice || p.price || 0), 0);
        if (currentAmount + price > totalBudget) {
            alert('예산을 초과할 수 없습니다.');
            return false;
        }
        if (selectedProducts.find((p) => (p.productSourceId || p.id) === productId)) return false;
        
        setSelectedProducts([...selectedProducts, product]);
        return true;
    };

    const handleRemoveItem = (id) => {
        setSelectedProducts(selectedProducts.filter((p) => (p.productSourceId || p.id) !== id));
    };

    const value = {
        hasSubscription,
        setHasSubscription,
        totalBudget,
        setTotalBudget,
        selectedProducts,
        setSelectedProducts,
        handleAddItem,
        handleRemoveItem
    };

    return (
        <CapsureContext.Provider value={value}>
            {children}
        </CapsureContext.Provider>
    );
};
