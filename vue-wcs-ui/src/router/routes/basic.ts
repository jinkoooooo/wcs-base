import type { AppRouteRecordRaw } from '/@/router/types';
import { t } from '/@/hooks/web/useI18n';
import {
  REDIRECT_NAME,
  LAYOUT,
  EXCEPTION_COMPONENT,
  PAGE_NOT_FOUND_NAME,
} from '/@/router/constant';

// ✅ 대시보드 후보들 로딩 (빌드 타임에 후보 등록)
const dashboardPages = import.meta.glob('/src/views/dashboard_*/monitoring/index.vue');

// ✅ env 값으로 프로젝트 선택
const project = import.meta.env.VITE_PROJECT || 'base';
const key = `/src/views/dashboard_${project}/monitoring/index.vue`;

// ✅ 존재하면 선택, 없으면 base로 fallback
const DashboardImporter =
  (dashboardPages[key] as any) ||
  (dashboardPages['/src/views/dashboard_base/monitoring/index.vue'] as any);

// 404 on a page
export const PAGE_NOT_FOUND_ROUTE: AppRouteRecordRaw = {
  path: '/:path(.*)*',
  name: PAGE_NOT_FOUND_NAME,
  component: LAYOUT,
  meta: {
    title: 'ErrorPage',
    hideBreadcrumb: true,
    hideMenu: true,
  },
  children: [
    {
      path: '/:path(.*)*',
      name: PAGE_NOT_FOUND_NAME,
      component: EXCEPTION_COMPONENT,
      meta: {
        title: 'ErrorPage',
        hideBreadcrumb: true,
        hideMenu: true,
      },
    },
  ],
};

export const REDIRECT_ROUTE: AppRouteRecordRaw = {
  path: '/redirect',
  component: LAYOUT,
  name: 'RedirectTo',
  meta: {
    title: REDIRECT_NAME,
    hideBreadcrumb: true,
    hideMenu: true,
  },
  children: [
    {
      path: '/redirect/:path(.*)',
      name: REDIRECT_NAME,
      component: () => import('/@/views/sys/redirect/index.vue'),
      meta: {
        title: REDIRECT_NAME,
        hideBreadcrumb: true,
      },
    },
  ],
};

export const ERROR_LOG_ROUTE: AppRouteRecordRaw = {
  path: '/error-log',
  name: 'ErrorLog',
  component: LAYOUT,
  redirect: '/error-log/list',
  meta: {
    title: 'ErrorLog',
    hideBreadcrumb: true,
    hideChildrenInMenu: true,
  },
  children: [
    {
      path: 'list',
      name: 'ErrorLogList',
      component: () => import('/@/views/sys/error-log/index.vue'),
      meta: {
        title: t('routes.basic.errorLogList'),
        hideBreadcrumb: true,
        currentActiveMenu: '/error-log',
      },
    },
  ],
};

export const DASHBOARD_ROUTE: AppRouteRecordRaw = {
  path: '/',
  name: 'Dashboard',
  component: LAYOUT,
  redirect: '/dashboard',
  meta: {
    title: 'Dashboard',
    hideBreadcrumb: true,
    hideChildrenInMenu: true,
  },
  children: [
    {
      path: 'dashboard',
      name: 'dashboard',
      component: DashboardImporter,
      meta: {
        hideMenu: true,
        hideBreadcrumb: true,
        title: 'label.watch',
        currentActiveMenu: '/dashboard',
        icon: 'bx:bx-home',
      },
    },
  ],
};

export const DASHBOARDOUT_ROUTE: AppRouteRecordRaw = {
  path: '/',
  name: 'DashBoardOut',
  component: LAYOUT,
  redirect: '/dashboardout',
  meta: {
    title: 'Dashboard',
    hideBreadcrumb: true,
    hideChildrenInMenu: true,
  },
  children: [
    {
      path: 'dashboardout',
      name: 'dashboardout',
      component: DashboardImporter,
      meta: {
        hideMenu: true,
        hideBreadcrumb: true,
        title: 'label.watch',
        currentActiveMenu: '/dashboardout',
        icon: 'bx:bx-home',
      },
    },
  ],
};
