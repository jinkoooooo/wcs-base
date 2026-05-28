import type { AppRouteRecordRaw, AppRouteModule } from "@/router/types";

// import { PAGE_NOT_FOUND_ROUTE, REDIRECT_ROUTE } from '@/router/routes/basic';

// import { mainOutRoutes } from './mainOut';
import { PageEnum } from "/@/enums/pageEnum";
import { t } from "/@/hooks/web/useI18n";

// import.meta.globEager() 모든 모듈로 직접 가져오기Vite만의 기능
// const modules = import.meta.globEager('./modules/**/*.ts');
// glob은 mock에서 지원하지 못하고 있으나 운영환경에서 사용하지 않음으로 globEager -> glob으로 변경
// const modules = import.meta.globEager('./modules/**/*.ts');
const modules = import.meta.glob("./modules/**/*.ts", { eager: true });
const routeModuleList: AppRouteModule[] = [];

// 경로 집합에 추가
Object.keys(modules).forEach((key) => {
  const mod = modules[key].default || {};
  const modList = Array.isArray(mod) ? [...mod] : [mod];
  routeModuleList.push(...modList);
});

export const asyncRoutes = [...routeModuleList];

// 루트 경로
export const RootRoute: AppRouteRecordRaw = {
  path: "/",
  name: "Root",
  // component: () => import("/@/views/WcsSetting.vue"),
  redirect: PageEnum.BASE_HOME,
  meta: {
    title: "Root",
  },
};

export const SettingRoute: AppRouteRecordRaw = {
  path: "/setting",
  name: "Setting",
  component: () => import("/@/views/WcsSetting.vue"),
  meta: {
    title: "Setting",
  },
};

export const LoginRoute: AppRouteRecordRaw = {
  path: "/login",
  name: "Login",
  component: () => import("/@/views/login/Login.vue"),
  meta: {
    title: t("routes.basic.login"),
  },
};

// Basic routing without permission
// 未经许可的基本路由
export const basicRoutes = [
  LoginRoute,
  SettingRoute,
  RootRoute,
  // ...mainOutRoutes,
  // REDIRECT_ROUTE,
  // PAGE_NOT_FOUND_ROUTE,
];
