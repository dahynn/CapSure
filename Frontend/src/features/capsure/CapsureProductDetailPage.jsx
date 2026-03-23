import React from 'react';
import { useLocation, useNavigate, Navigate } from 'react-router-dom';
import CapsureProductDetail from './components/CapsureProductDetail';
import { useCapsure } from './context/CapsureContext';

const CapsureProductDetailPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const { selectedProducts, handleAddItem } = useCapsure();

    const product = location.state?.product;

    if (!product) {
        return <Navigate to="/capsure-insurance/maker" replace />;
    }

    const isAdded = !!selectedProducts.find((p) => p.id === product.id);

    const handleAdd = () => {
        if (!isAdded) {
            handleAddItem(product);
        }
        navigate(-1);
    };

    return (
        <CapsureProductDetail
            product={product}
            onBack={() => navigate(-1)}
            onAdd={handleAdd}
            isAdded={isAdded}
        />
    );
};

export default CapsureProductDetailPage;
