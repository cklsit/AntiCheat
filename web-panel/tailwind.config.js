/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        'bg-base': '#0D1117',
        'bg-card': '#161B22',
        'bg-hover': '#1C2333',
        'border-line': '#30363D',
        'text-primary': '#E6EDF3',
        'text-secondary': '#8B949E',
        'text-muted': '#484F58',
        'accent-cyan': '#00E5FF',
        'accent-blue': '#388BFD',
        'success': '#3FB950',
        'warning': '#D29922',
        'danger-orange': '#FF6D00',
        'danger-red': '#F85149',
        'accent-purple': '#BC8CFF'
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'Segoe UI', 'Roboto', 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', 'sans-serif'],
        mono: ['JetBrains Mono', 'SF Mono', 'Consolas', 'monospace']
      },
      fontSize: {
        'page-title': ['24px', { fontWeight: '600' }],
        'card-title': ['14px', { fontWeight: '600' }],
        'body': ['13px', { fontWeight: '400', lineHeight: '1.6' }],
        'caption': ['12px', { fontWeight: '400' }],
        'log': ['12px', { fontFamily: 'JetBrains Mono, monospace' }]
      },
      spacing: {
        '1': '8px',
        '2': '12px',
        '3': '16px',
        '4': '24px',
        '5': '32px'
      },
      borderRadius: {
        'card': '8px',
        'btn': '6px',
        'tag': '4px',
        'modal': '12px'
      },
      boxShadow: {
        'md': '0 4px 12px rgba(0,0,0,0.4)',
        'lg': '0 8px 24px rgba(0,0,0,0.5)'
      }
    }
  },
  plugins: []
}
