import { defHttp } from '/@/utils/http/axios';

enum Api {
  SkuList = '/sku',
  UpdateSkuList = '/sku/update_multiple',
}

export const getSkuList = (params?) => {
  return defHttp.get({ url: Api.SkuList, params });
};

export const updateSkuList = (params?) => {
  return defHttp.post({ url: Api.UpdateSkuList, data: JSON.stringify(params) });
};
