/**
 * TSPG-WCS 메인 대시보드 라우팅 설정.
 *
 * 진입 URL:
 *   /tspg-wcs                → /tspg-wcs/dashboard 으로 redirect
 *   /tspg-wcs/dashboard      → TSPG WCS 메인 대시보드 (시뮬레이터는 모달 안에)
 *
 * 하위 호환을 위해 기존 /wcs-simulator 경로도 동일 컴포넌트로 라우팅한다.
 */

import type { AppRouteModule } from '/@/router/types';
import { LAYOUT } from '/@/router/constant';

const tspgWcsRoutes: AppRouteModule = {
  path: '/tspg-wcs',
  name: 'TspgWcs',
  component: LAYOUT,
  redirect: '/tspg-wcs/dashboard',
  meta: {
    orderNo: 10,
    icon: 'ant-design:dashboard-outlined',
    title: 'TSPG WCS 대시보드',
  },
  children: [
    {
      path: 'dashboard',
      name: 'TspgWcsDashboard',
      component: () => import('./index.vue'),
      meta: {
        title: 'TSPG WCS 대시보드',
        icon: 'ant-design:dashboard-outlined',
      },
    },
    // 하위 호환: 이전 경로
    {
      path: '/wcs-simulator/monitoring',
      name: 'WcsSimulatorMonitoring',
      component: () => import('./index.vue'),
      meta: {
        title: 'TSPG WCS 대시보드',
        hideMenu: true,
      },
    },
  ],
};

export default tspgWcsRoutes;
