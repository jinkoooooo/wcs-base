const originalWarn = console.warn;
console.warn = (...args: any[]) => {
  const isCustomElementWarning = args.some(
    (arg) => typeof arg === 'string' && (
      arg.includes('Failed to resolve component: Tres') ||
      arg.includes('Failed to resolve component: primitive')
    )
  );

  if (isCustomElementWarning) return; // Tres 및 primitive 경고면 무시
  originalWarn(...args);              // 나머지는 정상 출력
};

import 'uno.css';
import '@/design/index.less';
import '@/components/VxeTable/src/css/index.scss';
import 'ant-design-vue/dist/antd.less';
import './assets/styles/variables.css'
// Register icon sprite
import 'virtual:svg-icons-register';

import { createApp } from 'vue';

import { registerGlobComp } from '@/components/registerGlobComp';
import { setupGlobDirectives } from '@/directives';
import { setupI18n } from '@/locales/setupI18n';
import { setupErrorHandle } from '@/logics/error-handle';
import { initAppConfigStore } from '@/logics/initAppConfig';
import { router, setupRouter } from '@/router';
import { setupRouterGuard } from '@/router/guard';
import { setupStore } from '@/store';

import App from './App.vue';

async function bootstrap() {
  const app = createApp(App);

  // Configure store
  // 설정 store
  setupStore(app);

  // Initialize internal system configuration
  // 초기화 내부 시스템 설정
  initAppConfigStore();

  // Register global components
  // 등록 글로벌 컴퍼넌트
  registerGlobComp(app);

  // Multilingual configuration
  // 다국어 설정
  // Asynchronous case: language files may be obtained from the server side
  // 异步案例：语言文件可能从服务器端获取
  await setupI18n(app);

  // Configure routing
  // 配置路由
  setupRouter(app);

  // router-guard
  // 路由守卫
  setupRouterGuard(router);

  // Register global directive
  // 注册全局指令
  setupGlobDirectives(app);

  // Configure global error handling
  // 配置全局错误处理
  setupErrorHandle(app);

  // https://next.router.vuejs.org/api/#isready
  // await router.isReady();

  app.mount('#app');
}

bootstrap();
