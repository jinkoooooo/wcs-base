import { defineApplicationConfig } from '@vben/vite-config';

export default defineApplicationConfig({
  overrides: {
    optimizeDeps: {
      include: [
        'echarts/core',
        'echarts/charts',
        'echarts/components',
        'echarts/renderers',
        'qrcode',
        '@iconify/iconify',
        'ant-design-vue/es/locale/zh_CN',
        'ant-design-vue/es/locale/en_US',
        'ant-design-vue/es/locale/ko_KR',
      ],
    },
    assetsInclude: ['**/*.glb'],  // 추가된 부분: .glb 파일을 에셋으로 포함
  },
});
