import { defHttp } from '/@/utils/http/axios';

enum Api {
  DailyProdSummaryList = '/daily_prod_summary',
}

export const getDailyProdSummaryList = (params?) => {
  return defHttp.get({ url: Api.DailyProdSummaryList, params });
};

