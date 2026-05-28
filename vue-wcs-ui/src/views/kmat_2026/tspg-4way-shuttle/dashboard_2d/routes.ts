/**
 * 4-Way Shuttle WCS 라우팅 설정
 *
 * 사용 방법:
 * 1. 기존 라우터 모듈(예: src/router/routes/modules/)에 이 라우트를 추가
 * 2. 또는 동적 라우트로 등록
 *
 * 예시 (src/router/routes/modules/shuttle.ts):
 * import shuttleRoutes from '/@/views/tspg-4way-shuttle/routes';
 * export default shuttleRoutes;
 */

import type { RouteRecordRaw } from 'vue-router';
import { LAYOUT } from '/@/router/constant';

const shuttleRoutes: RouteRecordRaw = {
  path: '/shuttle',
  name: 'Shuttle',
  component: LAYOUT,
  redirect: '/shuttle/dashboard',
  meta: {
    orderNo: 100,
    icon: 'ant-design:dashboard-outlined',
    title: '4-Way Shuttle WCS',
  },
  children: [
    {
      path: 'dashboard',
      name: 'ShuttleDashboard',
      component: () => import('./dashboard/Dashboard2D.vue'),
      meta: {
        title: '운영 대시보드',
        icon: 'ant-design:fund-projection-screen-outlined',
      },
    },
    {
      path: 'dashboard/:lcId',
      name: 'ShuttleDashboardWithLcId',
      component: () => import('./dashboard/Dashboard2D.vue'),
      meta: {
        title: '운영 대시보드',
        hideMenu: true,
      },
    },
    {
      path: 'editor',
      name: 'ShuttleMapEditor',
      component: () => import('./editor/MapEditor.vue'),
      meta: {
        title: '맵 에디터',
        icon: 'ant-design:edit-outlined',
        requiredRoles: ['super'],
      },
    },
    {
      path: 'editor/:lcId',
      name: 'ShuttleMapEditorWithLcId',
      component: () => import('./editor/MapEditor.vue'),
      meta: {
        title: '맵 에디터',
        hideMenu: true,
        requiredRoles: ['super'],
      },
    },
    {
      path: 'cell-state-2d',
      name: 'ShuttleCellState2D',
      component: () => import('../cell_state_2d/CellState2D.vue'),
      meta: {
        title: '2D 셀 상태 관리',
        icon: 'ant-design:appstore-outlined',
      },
    },
    {
      path: 'cell-state-2d/:lcId',
      name: 'ShuttleCellState2DWithLcId',
      component: () => import('../cell_state_2d/CellState2D.vue'),
      meta: {
        title: '2D 셀 상태 관리',
        hideMenu: true,
      },
    },
  ],
};

export default shuttleRoutes;

/**
 * 독립 실행 라우트 (레이아웃 없이 전체 화면으로 표시)
 * 별도의 URL로 대시보드만 표시하고 싶을 때 사용
 */
export const shuttleStandaloneRoutes: RouteRecordRaw[] = [
  {
    path: '/shuttle-view/dashboard/:lcId',
    name: 'ShuttleDashboardStandalone',
    component: () => import('./dashboard/Dashboard2D.vue'),
    meta: {
      title: 'Shuttle Dashboard',
      ignoreAuth: false, // 인증 필요 여부 설정
    },
  },
  {
    path: '/shuttle-view/editor/:lcId',
    name: 'ShuttleEditorStandalone',
    component: () => import('./editor/MapEditor.vue'),
    meta: {
      title: 'Shuttle Map Editor',
      ignoreAuth: false,
    },
  },
  {
    path: '/kmat2026/tspg4way/cell-state-2d/:lcId?',
    name: 'ShuttleCellState2DStandalone',
    component: () => import('../cell_state_2d/CellState2D.vue'),
    meta: {
      title: '2D 셀 상태 관리',
      ignoreAuth: false,
    },
  },
];
