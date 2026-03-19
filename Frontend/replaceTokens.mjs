import fs from 'fs';
import path from 'path';

const files = [
    'src/features/capsure/components/CapsureMakerView.jsx',
    'src/features/capsure/components/CapsureBudgetView.jsx',
    'src/features/capsure/components/CapsureProductDetail.jsx',
    'src/features/capsure/CapsureInsurancePage.jsx'
];

const replacements = {
    // Colors
    'bg-[#161B26]': 'bg-capsure-card',
    'text-[#82D8FC]': 'text-brand-blue',
    'bg-[#82D8FC]': 'bg-brand-blue',
    'border-[#82D8FC]': 'border-brand-blue',
    'text-[#F2BEF7]': 'text-brand-purple',
    'bg-[#F2BEF7]': 'bg-brand-purple',
    'text-[#F6CD3C]': 'text-brand-yellow',
    'bg-[#F6CD3C]': 'bg-brand-yellow',
    'text-[#E2BFEA]': 'text-brand-light-purple',
    'border-[#E2BFEA]': 'border-brand-light-purple',
    'bg-[#E2BFEA]': 'bg-brand-light-purple',
    
    // Typography
    'text-[10px]': 'text-micro',
    'text-[11px]': 'text-micro',
    'text-[12px]': 'text-capsure-sm',
    'text-[13px]': 'text-capsure-base',
    'text-[14px]': 'text-sm', // Standard tailwind
    'text-[15px]': 'text-capsure-lg',
    'text-[16px]': 'text-base', // Standard tailwind
    'text-[17px]': 'text-capsure-title',
    'text-[18px]': 'text-lg', // Standard tailwind
    'text-[20px]': 'text-xl', // Standard tailwind
    'text-[24px]': 'text-capsure-price',
    'text-[28px]': 'text-2xl', // Standard tailwind
    'text-[30px]': 'text-3xl' // Standard tailwind
};

for (const relPath of files) {
    const fullPath = path.resolve(relPath);
    if (fs.existsSync(fullPath)) {
        let content = fs.readFileSync(fullPath, 'utf-8');
        for (const [key, value] of Object.entries(replacements)) {
            const regex = new RegExp(key.replace(/\[/g, '\\[').replace(/\]/g, '\\]'), 'g');
            content = content.replace(regex, value);
        }
        fs.writeFileSync(fullPath, content);
        console.log(`Updated ${relPath}`);
    } else {
        console.log(`File missing: ${relPath}`);
    }
}
