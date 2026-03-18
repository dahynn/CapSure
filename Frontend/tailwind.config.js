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
        // ✅ 브랜드 대표 색상
        brand: {
          white:  '#FCFCFC', // 거의 흰색 (배경)
          gray:   '#9D9DA4', // 중간 회색 (보조 텍스트)
          blue:   '#82D8FC', // 하늘파랑 (포인트)
          purple: '#F2BEF7', // 연보라 (강조)
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
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
