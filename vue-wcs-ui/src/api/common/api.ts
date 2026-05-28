import { defHttp } from '/@/utils/http/axios';
import axios from 'axios';

/**
 * @description: Get user menu based on id
 */
export const getCommonDeleteApi = (paramUrl, params) => {
  let query = '';
  for (const key in params) {
    if (key != 'pageSize' && key != 'page') {
      if (params[key] != undefined)
        query =
          query + "{ 'name': '" + key + "', 'value' : '" + params[key] + "', 'operator' : 'eq' },";
    } else if (key == 'pageSize') {
      params['limit'] = params[key];
    }
  }

  if (query) {
    query = '[' + query.slice(0, -1) + ']';
    params.query = query;
  }

  return defHttp.delete(
    {
      url: paramUrl,
      params,
    },
    {
      isTransformResponse: false,
    },
  );
};

export const getCommonPostApi = (paramUrl, params) => {
  let query = '';
  for (const key in params) {
    if (key != 'pageSize' && key != 'page') {
      if (params[key] != undefined)
        query =
          query + "{ 'name': '" + key + "', 'value' : '" + params[key] + "', 'operator' : 'eq' },";
    } else if (key == 'pageSize') {
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

export const getCommonGetListApi = (urlInfo, params) => {
  let query = '';
  for (const key in params) {
    if (key != 'pageSize' && key != 'page') {
      if (params[key] != undefined)
        query =
          query + "{ 'name': '" + key + "', 'value' : '" + params[key] + "', 'operator' : 'eq' },";
    } else if (key == 'pageSize') {
      params['limit'] = params[key];
    }
  }

  if (query) {
    query = '[' + query.slice(0, -1) + ']';
    params.query = query;
  }
  return defHttp.get({ url: urlInfo, params });
};

export const getCommonGetApi = (urlInfo, params) => {
  let query = '';
  for (const key in params) {
    if (key != 'pageSize' && key != 'page') {
      if (params[key] != undefined)
        query =
          query + "{ 'name': '" + key + "', 'value' : '" + params[key] + "', 'operator' : 'eq' },";
    }
  }

  if (query) {
    query = '[' + query.slice(0, -1) + ']';
    params.query = query;
  }
  return defHttp.get({ url: urlInfo, params });
};

export const getCommonPutApi = (urlInfo, params) => {
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
  return defHttp.put({ url: urlInfo, params });
};

export const getSearchList = (path, params?) => {
  if (!path){
    return Promise.reject('No Path')
  }

  const url = `${path.startsWith('/') ? path : `/${path}`}`;
  return defHttp.get({ url, params });
};

export const getSearchDetailList = (path, id, params?) => {
  const url = `${path.startsWith('/') ? path : `/${path}`}/${id}/items`;
  return defHttp.get({ url, params });
};

export const getSearchDetailListById = (path, id, params?) => {
  const url = `${path.startsWith('/') ? path : `/domain_users/search_by_user`}/${id}`;
  return defHttp.get({ url, params });
};

export const updateCommonList = (path, params) => {
  const url = `${path.startsWith('/') ? path : `/${path}`}/update_multiple`;
  return defHttp.post({ url, data: JSON.stringify(params) });
};

export const updateList = (url, params) => {
  return defHttp.post({ url, data: JSON.stringify(params) });
};
export const updateCommonDetailList = (path, id, params) => {
  const url = `${path.startsWith('/') ? path : `/${path}`}/${id}/items/update_multiple`;
  return defHttp.post({ url, data: JSON.stringify(params) });
};

export const getCommonCodeByName = async (name) => {
  if (!name) {
    return [];
  }
  const url = `/common_codes/show_by_name?name=${name?.toUpperCase()}`;
  const response = await defHttp.get({ url });
  return (response.items || [])
    .sort((a, b) => a.rank - b.rank)
    .map((item) => ({ text: item.description, value: item.name }));
};

export const getEntityCodeByName = async (name, textField?, valueField?) => {
  const url = `/entities/${name}/search_records_as_code`;
  const response = await defHttp.get({ url });
  return (response || []).map((item) => ({ text: item.description, value: item.name }));
};

export const getCommonFileApi = (paramUrl, params) => {
  return defHttp.post(
    {
      url: paramUrl,
      params,
      data: JSON.stringify(params),
      responseType : 'arraybuffer',
      transformResponse: function(data) {
        return new Blob([data], {
          type: 'application/pdf'
        })
      },
    },
    {
      isTransformResponse: true, 
    },
  );
};

export const getExcelDownload = (paramUrl, params) => {
  return defHttp.get(
    {
      url: paramUrl,
      params,
      responseType: 'blob',
      transformResponse: function (data) {
        return new Blob([data], {
          type: 'application/excel',
        });
      },
    },
    {
      isTransformResponse: true, // transformResponse 비활성화
    },
  );
};

export const getFileDownloadApi = (paramUrl: string) => {
  return fileDownloadClient.get(paramUrl, {
    responseType: 'blob',
  })
}

export const apiClient = axios.create({
  baseURL: `${ window.location.protocol }//${ window.location.hostname }:2026/rest`, // 현재 접속 중인 서버의 origin을 기본 URL로 사용
  headers: {
    'Content-Type': 'application/json',
  }
});

// 파일 다운로드 인스턴스
const fileDownloadClient = axios.create({
  baseURL: `${ window.location.protocol }//${ window.location.hostname }:9500/rest`, // NOTE: 개발ver.
  // baseURL: `${ window.location.protocol }//${ window.location.hostname }`, // NOTE: 배포ver.
  headers: {
    'Content-Type': 'application/json',
  }
});
