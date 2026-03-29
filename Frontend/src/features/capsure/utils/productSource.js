import { getCapsureCategoryLabel } from '../constants/categories';

export const getProductSourceId = (product) => {
    if (!product?.productSourceId) {
        throw new Error('productSourceId is required for capsure product flows');
    }
    return product.productSourceId;
};

export const normalizeProductSource = (product) => {
    const productSourceId = getProductSourceId(product);
    const rawTermsUri = product.termsUri ?? '#';
    const normalizedTermsUri = rawTermsUri === '/terms/samsung-fire-pet/index.html'
        ? '/terms/samsung-fire-pet/full-terms-223.pdf'
        : rawTermsUri;

    return {
        ...product,
        productSourceId,
        productName: product.productName ?? '보험 상품',
        companyName: product.companyName ?? '보험사',
        monthlyPrice: Number(product.monthlyPrice ?? 0),
        coverageCategoryCode: product.coverageCategoryCode ?? 'ETC',
        categoryLabel: getCapsureCategoryLabel(product.coverageCategoryCode),
        insurerSector: product.insurerSector ?? 'NONLIFE',
        saleChannel: product.saleChannel ?? '다이렉트',
        termsUri: normalizedTermsUri,
    };
};
