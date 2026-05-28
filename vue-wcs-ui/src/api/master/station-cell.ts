import { defHttp } from '/@/utils/http/axios';

enum Api {
  StationList = '/stations',
  CellList = '/cells'
}

export const getStationList = (params?) => {
  return defHttp.get({ url: Api.StationList, params });
};

export const getCellList = (params?) => {
  return defHttp.get({ url: Api.CellList, params });
};


