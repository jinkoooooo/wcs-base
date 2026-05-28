import type { RouteRecordRaw } from 'vue-router';
import type { App } from 'vue';

import { createRouter, createWebHashHistory } from 'vue-router';
import { basicRoutes } from './routes';

// 화이트리스트에는 기본 정적 경로가 포함되어 있어야 합니다
const WHITE_NAME_LIST: string[] = [];
const getRouteNames = (array: any[]) =>
  array.forEach((item) => {
    WHITE_NAME_LIST.push(item.name);
    getRouteNames(item.children || []);
  });
getRouteNames(basicRoutes);

// app router
// Vue 프로그램에서 사용할 수 있는 라우팅 인스턴스를 만듭니다.
export const router = createRouter({
  // 해시 기록을 작성합니다.
  history: createWebHashHistory(import.meta.env.VITE_PUBLIC_PATH),
  // 라우팅의 초기 라우팅 목록에 추가해야 합니다.
  routes: basicRoutes as unknown as RouteRecordRaw[],
  // 테일 슬래시를 금지해야 합니까?암묵적으로 거짓으로 여기다.
  strict: true,
  scrollBehavior: () => ({ left: 0, top: 0 }),
});

// reset router
export function resetRouter() {
  router.getRoutes().forEach((route) => {
    const { name } = route;
    if (name && !WHITE_NAME_LIST.includes(name as string)) {
      router.hasRoute(name) && router.removeRoute(name);
    }
  });
}

// config router
// 配置路由器
export function setupRouter(app: App<Element>) {
  app.use(router);
}
