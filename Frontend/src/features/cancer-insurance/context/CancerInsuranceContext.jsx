import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

const STORAGE_KEY = 'capsure:cancer-insurance-flow:v1';

const EMPTY_FLOW_IDS = {
    productVersionId: null,
    quoteId: null,
    applicationId: null,
    paymentOrderId: null,
    policyId: null,
    claimId: null,
    claimScenarioId: null,
};

const readStoredIds = () => {
    try {
        const stored = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || '{}');
        return { ...EMPTY_FLOW_IDS, ...stored };
    } catch {
        return EMPTY_FLOW_IDS;
    }
};

const createRequestKey = (prefix) => {
    const suffix = typeof crypto?.randomUUID === 'function'
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    return `${prefix}-${suffix}`;
};

const CancerInsuranceContext = createContext(null);

export const useCancerInsurance = () => {
    const context = useContext(CancerInsuranceContext);
    if (!context) {
        throw new Error('useCancerInsurance must be used within CancerInsuranceProvider');
    }
    return context;
};

export const CancerInsuranceProvider = ({ children }) => {
    const [flowIds, setFlowIds] = useState(readStoredIds);
    const [product, setProduct] = useState(null);
    const [terms, setTerms] = useState(null);
    const [quote, setQuote] = useState(null);
    const [application, setApplication] = useState(null);
    const [payment, setPayment] = useState(null);
    const [policy, setPolicy] = useState(null);
    const [claim, setClaim] = useState(null);
    const [requestKeys, setRequestKeys] = useState({});

    useEffect(() => {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(flowIds));
    }, [flowIds]);

    const updateFlowIds = (patch) => {
        setFlowIds((previous) => ({ ...previous, ...patch }));
    };

    const getRequestKey = (name) => {
        if (requestKeys[name]) {
            return requestKeys[name];
        }
        const key = createRequestKey(name);
        setRequestKeys((previous) => ({ ...previous, [name]: key }));
        return key;
    };

    const resetFlow = () => {
        sessionStorage.removeItem(STORAGE_KEY);
        setFlowIds(EMPTY_FLOW_IDS);
        setProduct(null);
        setTerms(null);
        setQuote(null);
        setApplication(null);
        setPayment(null);
        setPolicy(null);
        setClaim(null);
        setRequestKeys({});
    };

    const resetClaim = () => {
        setFlowIds((previous) => ({
            ...previous,
            claimId: null,
            claimScenarioId: null,
        }));
        setClaim(null);
    };

    const value = useMemo(() => ({
        flowIds,
        updateFlowIds,
        product,
        setProduct,
        terms,
        setTerms,
        quote,
        setQuote,
        application,
        setApplication,
        payment,
        setPayment,
        policy,
        setPolicy,
        claim,
        setClaim,
        getRequestKey,
        resetClaim,
        resetFlow,
    }), [flowIds, product, terms, quote, application, payment, policy, claim, requestKeys]);

    return (
        <CancerInsuranceContext.Provider value={value}>
            {children}
        </CancerInsuranceContext.Provider>
    );
};
