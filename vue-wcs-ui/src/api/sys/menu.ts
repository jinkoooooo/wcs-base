import {defHttp} from '/@/utils/http/axios';
import {AppRouteRecordRaw} from '/@/router/types';
import {RouteMeta} from 'vue-router';

enum Api {
  GetMenuList2 = '/vue/vue_menus',
  GetMenuList = '/menus/user_menus/STANDARD',
  GetParentMenuList = '/menus',
}
import XEUtils from 'xe-utils';

/**
 * @description: Get the role-based sidebar menu for the current user
 */
export const getMenuList = async () => {
  let menuData = await defHttp.get({url: Api.GetMenuList});
  let routers: AppRouteRecordRaw[] = [];
  // 1. 서버에서 조회한 메뉴 정보 중에 메인 메뉴를 추출
  let mainMenus = menuData
    .map((item) => {
      if (item.menu_type == 'MENU') {
        let meta: RouteMeta = {
          title: item.title,
          hideChildrenInMenu: false,
          icon: item.icon_path,
          // redirect: item.name,//해당 화면으로 들어 왔을때 따른 화면으로 이동해야 할 경우에만 넣는다.
        };
        let router = {
          ...item,
          title: item.title,
          name: item.name,
          meta: meta,
          path: item.routing ? item.routing : `/${XEUtils.kebabCase(item.name)}`,
          component: 'LAYOUT',
          // components?: Component;
          // redirect: item.name,
          // props?: Recordable;
          // fullPath?: string;
          children: [],
          id: item.id,
        };
        routers.push(router);
        return router;
      } else {
        return {id: '1'};
      }
    })
    .filter((i) => i.id != '1');

  // 2. 메인 메뉴 하위에 서브 메뉴를 추가
  menuData.forEach((item) => {
    if (item.menu_type == 'SCREEN') {
      let parentMenu: AppRouteRecordRaw = mainMenus.find((main) => main.id == item.parent_id);
      if (parentMenu) {
        let meta: RouteMeta = {
          title: item.title,
          hideChildrenInMenu: false,
          icon: item.icon_path,
          // redirect: item.name,//해당 화면으로 들어 왔을때 따른 화면으로 이동해야 할 경우에만 넣는다.
        };
        let child = {
          ...item,
          path: item.routing,
          name: item.name,
          title: item.title,
          currentActiveMenu: item.routing,
          icon: item.icon_path,
          component: !item.template ? '/common/index' : item.template,
          meta: meta,
        } as AppRouteRecordRaw;
        parentMenu?.children?.push(child);
      }
    }
  });
  return routers;
};

/**
 * @description: Get menus for the System Menu page
 */
export const getMenuList3 = async () => {
  let params = {
    includeHidden: false,
  };
  let menuData = await defHttp.get({url: Api.GetMenuList, params});
  let routers: AppRouteRecordRaw[] = [];
  // 1. 서버에서 조회한 메뉴 정보 중에 메인 메뉴를 추출
  let mainMenus = menuData
    .map((item) => {
      if (item.menu_type == 'MENU') {
        let meta: RouteMeta = {
          title: item.title,
          hideChildrenInMenu: false,
          icon: item.icon_path,
          // redirect: item.name,//해당 화면으로 들어 왔을때 따른 화면으로 이동해야 할 경우에만 넣는다.
        };
        let router = {
          ...item,
          title: item.title,
          name: item.name,
          meta: meta,
          path: item.routing ? item.routing : `/${XEUtils.kebabCase(item.name)}`,
          component: 'LAYOUT',
          // components?: Component;
          // redirect: item.name,
          // props?: Recordable;
          // fullPath?: string;
          children: [],
          id: item.id,
        };
        routers.push(router);
        return router;
      } else {
        return {id: '1'};
      }
    })
    .filter((i) => i.id != '1');

  // 2. 메인 메뉴 하위에 서브 메뉴를 추가
  menuData.forEach((item) => {
    if (item.menu_type == 'SCREEN') {
      let parentMenu: AppRouteRecordRaw = mainMenus.find((main) => main.id == item.parent_id);
      if (parentMenu) {
        let meta: RouteMeta = {
          title: item.title,
          hideChildrenInMenu: false,
          icon: item.icon_path,
          // redirect: item.name,//해당 화면으로 들어 왔을때 따른 화면으로 이동해야 할 경우에만 넣는다.
        };
        let child = {
          ...item,
          path: item.routing,
          name: item.name,
          title: item.title,
          currentActiveMenu: item.routing,
          icon: item.icon_path,
          component: !item.template ? '/common/index' : item.template,
          meta: meta,
        } as AppRouteRecordRaw;
        parentMenu?.children?.push(child);
      }
    }
  });
  return routers;
};

/**
 * @description: Get user menu based on user role
 */
export const getMenuList2 = async () => {
  return defHttp.post<getMenuListResultModel>({url: Api.GetMenuList2});
};

export const getParentMenuList = async () => {
  let params = {
    select:
      'domain_id,id,rank,name,description,template,category,menu_type,icon_path,hidden_flag,parent_id',
    sort: JSON.stringify([{field: 'rank', ascending: true}]),
    query: JSON.stringify([{name: 'parent_id', operator: 'is_null'}]),
    page: 1,
    limit: 0,
  };
  const response = await defHttp.get({url: Api.GetParentMenuList, params});
  return (response.items || []).map((item) => ({text: item.title, value: item.id}));
};

export const getCommonPostApi = (paramUrl, params) => {
  let query = '';
  for (const key in params) {
    if (key != 'pageSize' && key != 'page') {
      if (params[key] != undefined)
        query =
          query + "{ 'name': '" + key + "', 'value' : '" + params[key] + "', 'operator' : 'eq' },";
    } else {
      params['limit'] = params[key];
    }
  }

  if (query) {
    query = '[' + query.slice(0, -1) + ']';
    params.query = query;
  }

  return defHttp.post(
    {
      url: paramUrl,
      params,
    },
    {
      isTransformResponse: false,
    },
  );
};

export const getCommonGetApi = (params) => {
  let query = '';
  for (let key in params) {
    if (key != 'pageSize' && key != 'page') {
      if (params[key] != undefined)
        query =
          query + "{ 'name': '" + key + "', 'value' : '" + params[key] + "', 'operator' : 'eq' },";
    } else {
      params['limit'] = params[key];
    }
  }

  if (query) {
    query = '[' + query.slice(0, -1) + ']';
    params.query = query;
  }
  return defHttp.get({url: '/menus', params});
};

//
//export const getCommonGetApi2 = ( params) =>{
//    let query ="";
//    for(let key in params){
//        if(key != "pageSize" && key != "page"){
//            if(params[key] != undefined) query = query +"{ 'name': '"+ key+"', 'value' : '"+params[key]+"', 'operator' : 'eq' },";
//        }
//        else {
//        	params["limit"] = params[key];
//        }
//    }
//
//    if(query) {
//        query =  "["+query.slice(0, -1)+"]";
//        params.query = query;
//    }
//    return defHttp.get({ url: "/menus", params })
//}
//;
