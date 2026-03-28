import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate, useParams, Navigate } from 'react-router-dom';
import CapsureProductDetail from './components/CapsureProductDetail';
import { useCapsure } from './context/CapsureContext';
import { httpClient } from '@/common/api/httpClient';
import { getProductSourceId, normalizeProductSource } from './utils/productSource';

const CapsureProductDetailPage = () => {
    const { id } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { selectedProducts, handleAddItem } = useCapsure();
    
    // Fallback to location state but we prefer fresh fetch
    const [product, setProduct] = useState(
        location.state?.product ? normalizeProductSource(location.state.product) : null
    );
    const [isLoading, setIsLoading] = useState(!product);

    useEffect(() => {
        const fetchDetail = async () => {
            setIsLoading(true);
            try {
                const response = await httpClient.get(`/insurers/products/${id}`);
                const data = response.data;
                if (data.success) {
                    setProduct(normalizeProductSource(data.data));
                }
            } catch (error) {
                console.error("Failed to fetch product detail:", error);
            } finally {
                setIsLoading(false);
            }
        };

        if (id) {
            fetchDetail();
        }
    }, [id]);

    if (!id && !product) {
        return <Navigate to="/capsure-insurance/maker" replace />;
    }

    if (isLoading) {
        return <div className="min-h-screen bg-[#020715] flex items-center justify-center text-white">상세 정보를 불러오는 중...</div>;
    }

    if (!product) {
        return <div className="min-h-screen bg-[#020715] flex items-center justify-center text-white">정보를 찾을 수 없습니다.</div>;
    }

    const productId = getProductSourceId(product);
    const isAdded = !!selectedProducts.find((p) => getProductSourceId(p) === productId);

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
