import React from 'react';
import { useNavigate } from 'react-router-dom';
import CapsureMakerView from './components/CapsureMakerView';
import { useCapsure } from './context/CapsureContext';

const CapsureMakerPage = () => {
    const navigate = useNavigate();
    const { totalBudget, selectedProducts, handleAddItem, handleRemoveItem } = useCapsure();

    const handleSubscribeConfirm = () => {
        if (selectedProducts.length === 0) {
            alert('담은 상품이 없습니다.');
            return;
        }
        navigate('/capsure-insurance/checkout');
    };

    const handleViewDetail = (product) => {
        // Here we pass the product ID in URL and product data in state
        navigate(`/capsure-insurance/detail/${product.id}`, { state: { product } });
    };

    return (
        <CapsureMakerView
            totalBudget={totalBudget}
            selectedProducts={selectedProducts}
            onAddItem={handleAddItem}
            onRemoveItem={handleRemoveItem}
            onConfirm={handleSubscribeConfirm}
            onViewDetail={handleViewDetail}
        />
    );
};

export default CapsureMakerPage;
