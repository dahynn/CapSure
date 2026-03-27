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
        navigate('/capsure-insurance/terms');
    };

    const handleViewDetail = (product) => {
        // Use productSourceId from backend
        const productId = product.productSourceId || product.id;
        navigate(`/capsure-insurance/detail/${productId}`, { state: { product } });
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
