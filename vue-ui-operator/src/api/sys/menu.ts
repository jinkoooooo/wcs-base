import { defHttp } from "/@/utils/http/axios";
import { AppRouteRecordRaw, Component } from "@/router/types";
import { SettingRoute } from "@/router/routes";
import { RouteMeta } from "vue-router";

//typescript 3.4 버전에서 추가된 기능인 Const assertions를 사용
/** [Reference](https://steveholgado.com/typescript-types-from-arrays/) */
/** [Reference](https://talkwithcode.tistory.com/101) */
/** [PR](https://github.com/Microsoft/TypeScript/pull/29510) */
export const MenuCattegoryOptions = [
  { value: "TABLET", label: "TABLET" },
  { value: "KIOSK", label: "KIOSK" },
  { value: "PDA", label: "PDA" },
  { value: "", label: "" },
] as const;

export type MenuCategoryType = (typeof MenuCattegoryOptions)[number]["value"];
export type MenuCategoryOptionType = (typeof MenuCattegoryOptions)[number];

enum Api {
  // GetMenuList = '/vue/vue_menus',
  GetMenuList = "/menus/user_menus",
  GetParentMenuList = "/menus",
}

/**
 * @description: Get user menu based on user role
 */
export const getMenuList = async (category: MenuCategoryType = "KIOSK") => {
  if(category == null || category == "") {
    category = "KIOSK";
  }
  let menuData = await defHttp.get({ url: `${Api.GetMenuList}/${category}` });
  let routers: AppRouteRecordRaw[] = [];
  // 1. 서버에서 조회한 메뉴 정보 중에 메인 메뉴를 추출
  type Item = {
    menu_type: string;
    title: any;
    icon_path: any;
    name: any;
    routing: any;
    id: any;
    template?: Component | string;
  };
  routers = menuData
    .filter((item: Item) => {
      return item.menu_type == "SCREEN";
    })
    .map((item: Item) => {
      let meta: RouteMeta = {
        title: item.title,
        hideChildrenInMenu: false,
        icon: "/images/menu-icons/" + item.icon_path,
        // redirect: item.name,//해당 화면으로 들어 왔을때 따른 화면으로 이동해야 할 경우에만 넣는다.
      };

      let routing = item.routing.startsWith("/")
        ? item.routing
        : "/" + item.routing;

      let router = {
        ...item,
        path: routing,
        name: item.name,
        title: item.title,
        currentActiveMenu: routing,
        icon: "/images/menu-icons/" + item.icon_path,
        component: item.template, // ? "/common/index" : item.template,
        meta: meta,
        id: item.id,
      };
      return router;
    });

  let settingRouter = {
    title: "설정",
    path: "/setting",
    name: "setting",
    currentActiveMenu: "/setting",
    icon: "/images/menu-icons/menuicon-setting.png",
    component: "/WcsSetting",
    meta: {
      title: "설정",
      hideChildrenInMenu: false,
      icon: "/images/menu-icons/menuicon-setting.png",
    },
  };
  routers.splice(0, 0, settingRouter);
  return routers;
};

export const getParentMenuList = async () => {
  let params = {
    select:
      "domain_id,id,rank,name,description,template,category,menu_type,icon_path,hidden_flag,parent_id",
    sort: JSON.stringify([{ field: "rank", ascending: true }]),
    query: JSON.stringify([{ name: "parent_id", operator: "is_null" }]),
    page: 1,
    limit: 0,
  };
  const response = await defHttp.get({ url: Api.GetParentMenuList, params });
  return (response.items || []).map((item) => ({
    text: item.title,
    value: item.id,
  }));
};

export const getCommonPostApi = (
  paramUrl: any,
  params: { [x: string]: any; query: string }
) => {
  let query = "";
  for (const key in params) {
    if (key != "pageSize" && key != "page") {
      if (params[key] != undefined)
        query =
          query +
          "{ 'name': '" +
          key +
          "', 'value' : '" +
          params[key] +
          "', 'operator' : 'eq' },";
    } else {
      params["limit"] = params[key];
    }
  }

  if (query) {
    query = "[" + query.slice(0, -1) + "]";
    params.query = query;
  }

  return defHttp.post(
    {
      url: paramUrl,
      params,
    },
    {
      isTransformResponse: false,
    }
  );
};

export const getMenus = (params: { [x: string]: any; query: string }) => {
  let query = "";
  for (let key in params) {
    if (key != "pageSize" && key != "page") {
      if (params[key] != undefined)
        query =
          query +
          "{ 'name': '" +
          key +
          "', 'value' : '" +
          params[key] +
          "', 'operator' : 'eq' },";
    } else {
      params["limit"] = params[key];
    }
  }

  if (query) {
    query = "[" + query.slice(0, -1) + "]";
    params.query = query;
  }
  return defHttp.get({ url: "/menus", params });
};
