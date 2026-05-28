import { defHttp } from "@/utils/http/axios";
import axios from "axios";
/**
 * @description: Get user menu based on id
 */
export const getCommonDeleteApi = (paramUrl: any, params: any) => {
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
    } else if (key == "pageSize") {
      params["limit"] = params[key];
    }
  }

  if (query) {
    query = "[" + query.slice(0, -1) + "]";
    params.query = query;
  }

  return defHttp.delete(
    {
      url: paramUrl,
      params,
    },
    {
      isTransformResponse: false,
    }
  );
};

export const getCommonPostApi = (paramUrl: any, params: any, body?: any) => {
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
    } else if (key == "pageSize") {
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
      data: body,
    },
    {
      isTransformResponse: false,
    }
  );
};

export const getCommonGetListApi = (urlInfo: any, params: any) => {
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
    } else if (key == "pageSize") {
      params["limit"] = params[key];
    }
  }

  if (query) {
    query = "[" + query.slice(0, -1) + "]";
    params.query = query;
  }
  return defHttp.get({ url: urlInfo, params });
};

export const getCommonGetApi = (urlInfo: any, params: any) => {
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
    }
  }

  if (query) {
    query = "[" + query.slice(0, -1) + "]";
    params.query = query;
  }
  return defHttp.get({ url: urlInfo, params });
};

export const getCommonPutApi = (urlInfo: any, params: any, body?: any) => {
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
  return defHttp.put({ url: urlInfo, params, data: body });
};

export const getSearchList = (path: string, params?: any) => {
  const url = `${path.startsWith("/") ? path : `/${path}`}`;
  return defHttp.get({ url, params });
};

export const getSearchDetailList = (path: string, id: any, params?: any) => {
  const url = `${path.startsWith("/") ? path : `/${path}`}/${id}/items`;
  return defHttp.get({ url, params });
};

export const getSearchDetailListById = (
  path: string,
  id: any,
  params?: any
) => {
  const url = `${
    path.startsWith("/") ? path : `/domain_users/search_by_user`
  }/${id}`;
  return defHttp.get({ url, params });
};

export const updateCommonList = (path: string, params: any) => {
  const url = `${path.startsWith("/") ? path : `/${path}`}/update_multiple`;
  return defHttp.post({ url, data: JSON.stringify(params) });
};

export const updateList = (url: any, params: any) => {
  return defHttp.post({ url, data: JSON.stringify(params) });
};
export const updateCommonDetailList = (path: string, id: any, params: any) => {
  const url = `${
    path.startsWith("/") ? path : `/${path}`
  }/${id}/items/update_multiple`;
  return defHttp.post({ url, data: JSON.stringify(params) });
};

export const getCommonCodeByName = async (name: string) => {
  if (!name) {
    return [];
  }
  const url = `/common_codes/show_by_name?name=${name?.toUpperCase()}`;
  const response = await defHttp.get({ url });
  return (response.items || [])
    .sort((a: { rank: number }, b: { rank: number }) => a.rank - b.rank)
    .map((item: { description: any; name: any }) => ({
      text: item.description,
      value: item.name,
    }));
};

export const getEntityCodeByName = async (
  name: any,
  textField?: undefined,
  valueField?: undefined
) => {
  const url = `/entities/${name}/search_records_as_code`;
  const response = await defHttp.get({ url });
  return (response || []).map((item: { description: any; name: any }) => ({
    text: item.description,
    value: item.name,
  }));
};

export const extrPostApi = async (url, params, body?: any) => {
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
    } else if (key == "pageSize") {
      params["limit"] = params[key];
    }
  }

  if (query) {
    query = "[" + query.slice(0, -1) + "]";
    params.query = query;
  }

  return defHttp.post(
    {
      url: url,
      params,
      data: body,
    },
    {
      isTransformResponse: false,
      errorMessageMode: "none",
    }
  );
};

export const getCommonFileApi = (paramUrl, params) => {
  return defHttp.post(
    {
      url: paramUrl,
      params,
      responseType: "arraybuffer",
      transformResponse: function (data) {
        return new Blob([data], {
          type: "application/pdf",
        });
      },
    },
    {
      isTransformResponse: true,
    }
  );
};


export const getCommonFileApi2 = (paramUrl, params) => {
  return defHttp.post(
    {
      url: paramUrl,
      params,
      responseType: "arraybuffer",
      transformResponse: function (data) {
        return new Blob([data], {
          type: "application/octet-stream",
        });
      },
    },
    {
      isTransformResponse: true,
    }
  );
};
