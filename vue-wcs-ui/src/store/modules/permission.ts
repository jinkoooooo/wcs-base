import type { AppRouteRecordRaw, Menu } from '/@/router/types';

import { defineStore } from 'pinia';
import { store } from '/@/store';
import { useI18n } from '/@/hooks/web/useI18n';
import { useUserStore } from './user';
import { useAppStoreWithOut } from './app';
import { toRaw } from 'vue';
import { transformObjToRoute, flatMultiLevelRoutes } from '/@/router/helper/routeHelper';
import { transformRouteToMenu } from '/@/router/helper/menuHelper';

import projectSetting from '/@/settings/projectSetting';

import { PermissionModeEnum } from '/@/enums/appEnum';

import { asyncRoutes } from '/@/router/routes';
import { ERROR_LOG_ROUTE, PAGE_NOT_FOUND_ROUTE, DASHBOARD_ROUTE } from '/@/router/routes/basic';

import { filter } from '/@/utils/helper/treeHelper';

import { getMenuList } from '/@/api/sys/menu';
import { getPermCode } from '/@/api/sys/user';

import { useMessage } from '/@/hooks/web/useMessage';
import { PageEnum } from '/@/enums/pageEnum';

interface PermissionState {
  // Permission code list
  permCodeList: string[] | number[];
  // Whether the route has been dynamically added
  isDynamicAddedRoute: boolean;
  // To trigger a menu update
  lastBuildMenuTime: number;
  // Backstage menu list
  backMenuList: Menu[];
  // 菜单列表
  frontMenuList: Menu[];
}

export const usePermissionStore = defineStore({
  id: 'app-permission',
  state: (): PermissionState => ({
    // 권한코드
    permCodeList: [],
    // Whether the route has been dynamically added
    isDynamicAddedRoute: false,
    // To trigger a menu update
    lastBuildMenuTime: 0,
    // Backstage menu list
    backMenuList: [],
    // menu List
    frontMenuList: [],
  }),
  getters: {
    getPermCodeList(state): string[] | number[] {
      return state.permCodeList;
    },
    getBackMenuList(state): Menu[] {
      return state.backMenuList;
    },
    getFrontMenuList(state): Menu[] {
      return state.frontMenuList;
    },
    getLastBuildMenuTime(state): number {
      return state.lastBuildMenuTime;
    },
    getIsDynamicAddedRoute(state): boolean {
      return state.isDynamicAddedRoute;
    },
  },
  actions: {
    setPermCodeList(codeList: string[]) {
      this.permCodeList = codeList;
    },

    setBackMenuList(list: Menu[]) {
      this.backMenuList = list;
      list?.length > 0 && this.setLastBuildMenuTime();
    },

    setFrontMenuList(list: Menu[]) {
      this.frontMenuList = list;
    },

    setLastBuildMenuTime() {
      this.lastBuildMenuTime = new Date().getTime();
    },

    setDynamicAddedRoute(added: boolean) {
      this.isDynamicAddedRoute = added;
    },
    resetState(): void {
      this.isDynamicAddedRoute = false;
      this.permCodeList = [];
      this.backMenuList = [];
      this.lastBuildMenuTime = 0;
    },
    async changePermissionCode() {
      const codeList = await getPermCode();
      this.setPermCodeList(codeList);
    },

    // 생성 라우팅
    async buildRoutesAction(): Promise<AppRouteRecordRaw[]> {
      const { t } = useI18n();
      const userStore = useUserStore();
      const appStore = useAppStoreWithOut();

      let routes: AppRouteRecordRaw[] = [];
      const roleList = toRaw(userStore.getRoleList) || [];
      const { permissionMode = projectSetting.permissionMode } = appStore.getProjectConfig;

      // 라우팅 필터는 함수 filter에서 콜백으로 전달됩니다
      const routeFilter = (route: AppRouteRecordRaw) => {
        const { meta } = route;
        // 권한추출
        const { roles } = meta || {};
        if (!roles) return true;
        // 역할 권한 판단
        return roleList.some((role) => roles.includes(role));
      };

      const routeRemoveIgnoreFilter = (route: AppRouteRecordRaw) => {
        const { meta } = route;
        // ignoreRoute가 true이면 라우팅은 메뉴 생성에만 사용되며 실제 라우팅 테이블에는 나타나지 않습니다
        const { ignoreRoute } = meta || {};
        // arr.filter가 true를 반환하면 이 요소가 테스트를 통과했음을 나타냅니다.
        return !ignoreRoute;
      };

      /**
       * @description 설정된 첫 페이지 path에 따라 routes의 affix 표시 수정 (첫 페이지 고정)
       * */
      const patchHomeAffix = (routes: AppRouteRecordRaw[]) => {
        if (!routes || routes.length === 0) return;
        let homePath: string = userStore.getUserInfo.homePath || PageEnum.BASE_HOME;

        function patcher(routes: AppRouteRecordRaw[], parentPath = '') {
          if (parentPath) parentPath = parentPath + '/';
          routes.forEach((route: AppRouteRecordRaw) => {
            const { path, children, redirect } = route;
            const currentPath = path.startsWith('/') ? path : parentPath + path;
            if (currentPath === homePath) {
              if (redirect) {
                homePath = route.redirect! as string;
              } else {
                route.meta = Object.assign({}, route.meta, { affix: true });
                throw new Error('end');
              }
            }
            children && children.length > 0 && patcher(children, currentPath);
          });
        }

        try {
          patcher(routes);
        } catch (e) {
          // 종료된 루프 건너뛰기
        }
        return;
      };

      switch (permissionMode) {
        // 역할 권한
        case PermissionModeEnum.ROLE:
          // 비일차 경로 필터링
          routes = filter(asyncRoutes, routeFilter);
          // 역할 권한에 따라 1단계 경로 필터링
          routes = routes.filter(routeFilter);
          // Convert multi-level routing to level 2 routing
          // 다단계 경로를 2단계 경로로 변환
          routes = flatMultiLevelRoutes(routes);
          break;

        // 경로 매핑, 기본값  case
        case PermissionModeEnum.ROUTE_MAPPING:
          // 비일차 경로 필터링
          routes = filter(asyncRoutes, routeFilter);
          // 역할 권한에 따라 1단계 경로 필터링
          routes = routes.filter(routeFilter);
          // 라우팅을 메뉴로 변환
          const menuList = transformRouteToMenu(routes, true);
          // ignoreRoute: true의 경로를 제거합니다. 비단계 경로
          routes = filter(routes, routeRemoveIgnoreFilter);
          // ignoreRoute: true의 경로를 제거합니다.
          routes = routes.filter(routeRemoveIgnoreFilter);
          // 메뉴 정렬
          menuList.sort((a, b) => {
            return (a.meta?.orderNo || 0) - (b.meta?.orderNo || 0);
          });

          // 메뉴 목록 설정
          this.setFrontMenuList(menuList);

          // Convert multi-level routing to level 2 routing
          routes = flatMultiLevelRoutes(routes);
          break;

        //  If you are sure that you do not need to do background dynamic permissions, please comment the entire judgment below
        case PermissionModeEnum.BACK:
          const { createMessage } = useMessage();

          createMessage.loading({
            content: t('sys.app.menuLoading'),
            duration: 1,
          });

          // !Simulate to obtain permission codes from the background,
          // 백그라운드에서 권한 코드를 가져오는 중,，
          // this function may only need to be executed once, and the actual project can be put at the right time by itself
          // 이 기능은 한 번만 실행하면 될 수 있으며 실제 프로젝트는 적절한 시기에 스스로 배치할 수 있습니다.
          let routeList: AppRouteRecordRaw[] = [];
          try {
            await this.changePermissionCode();
            routeList = (await getMenuList()) as AppRouteRecordRaw[];
          } catch (error) {
            console.error(error);
          }

          // Dynamically introduce components
          // 动态引入组件
          routeList = transformObjToRoute(routeList);

          //  Background routing to menu structure
          //  后台路由到菜单结构
          const backMenuList = transformRouteToMenu(routeList);
          this.setBackMenuList(backMenuList);

          // remove meta.ignoreRoute item
          // 删除 meta.ignoreRoute 项
          routeList = filter(routeList, routeRemoveIgnoreFilter);
          routeList = routeList.filter(routeRemoveIgnoreFilter);

          routeList = flatMultiLevelRoutes(routeList);
          routes = [DASHBOARD_ROUTE, PAGE_NOT_FOUND_ROUTE, ...routeList];
          break;
      }

      // routes.push(ERROR_LOG_ROUTE);
      patchHomeAffix(routes);
      return routes;
    },
  },
});

// Need to be used outside the setup
// 설정 외 사용 필요
export function usePermissionStoreWithOut() {
  return usePermissionStore(store);
}
