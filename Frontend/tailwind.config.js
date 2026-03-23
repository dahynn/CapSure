/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // ✅ 메인 배경
        bg: '#020715',
        'capsure-card': '#161B26',
        // ✅ 브랜드 대표 색상
        brand: {
          white:  '#FCFCFC', // 거의 흰색 (배경)
          gray:   '#9D9DA4', // 중간 회색 (보조 텍스트)
          blue:   '#82D8FC', // 하늘파랑 (포인트)
          purple: '#F2BEF7', // 연보라 (강조)
          'light-purple': '#E2BFEA', // 연한 연보라
          yellow: '#F6CD3C', // 노랑 (CTA)
        },
        primary: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          200: '#bae6fd',
          300: '#7dd3fc',
          400: '#38bdf8',
          500: '#0ea5e9',
          600: '#0284c7',
          700: '#0369a1',
          800: '#075985',
          900: '#0c4a6e',
          950: '#082f49',
        },
        success: {
          500: '#10b981',
          600: '#059669',
        },
      },
      fontFamily: {
        sans: ['Pretendard', 'Inter', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        /* Legacy Capsure Sizes for Compatibility */
        'micro': ['10px', '14px'],
        'capsure-sm': ['12px', '16px'],
        'capsure-base': ['13px', '18px'],
        'capsure-lg': ['15px', '22px'],
        'capsure-title': ['17px', '24px'],
        'capsure-price': ['24px', '32px'],
        
        /* New Standardized Typography */
        'display': ['36px', { lineHeight: '1.1', letterSpacing: '-0.02em', fontWeight: '900' }],
        'h1': ['28px', { lineHeight: '1.2', letterSpacing: '-0.02em', fontWeight: '700' }],
        'h2': ['22px', { lineHeight: '1.3', letterSpacing: '-0.02em', fontWeight: '700' }],
        'h3': ['20px', { lineHeight: '1.3', letterSpacing: '-0.02em', fontWeight: '700' }],
        'h4': ['17px', { lineHeight: '1.4', letterSpacing: '-0.01em', fontWeight: '700' }],
        'body-lg': ['15px', { lineHeight: '1.5', letterSpacing: '-0.01em', fontWeight: '400' }],
        'body': ['14px', { lineHeight: '1.5', letterSpacing: '-0.01em', fontWeight: '400' }],
        'body-sm': ['13px', { lineHeight: '1.5', letterSpacing: '0', fontWeight: '400' }],
        'caption': ['11px', { lineHeight: '1.4', letterSpacing: '0.15em', fontWeight: '700' }],
      },
    },
  },
  plugins: [],
}
