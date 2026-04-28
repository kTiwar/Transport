/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  corePlugins: {
    preflight: false,
  },
  theme: {
    extend: {
      colors: {
        erp: {
          canvas: '#edebe9',
          panel: '#ffffff',
          hairline: '#edebe9',
          rule: '#c8c6c4',
          text: '#323130',
          muted: '#605e5c',
          accent: '#0078d4',
          success: '#107c10',
          field: '#8a8886',
        },
      },
      fontFamily: {
        erp: [
          '"Segoe UI"',
          '"Segoe UI Web (West European)"',
          'system-ui',
          '-apple-system',
          'sans-serif',
        ],
      },
      boxShadow: {
        erp: '0 0.3px 1.3px rgba(0,0,0,0.06)',
      },
    },
  },
  plugins: [],
};
