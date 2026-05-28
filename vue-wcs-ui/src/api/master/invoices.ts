// API 호출 함수
import { defHttp } from '/@/utils/http/axios';

enum Api {
  InvoicesList = '/invoices', // invoices API 엔드포인트
  UpdateInvoicesList = '/invoices/update_multiple', // invoices 업데이트 API 엔드포인트
}

export const getInvoicesList = (params?) => {
  return defHttp.get({ url: Api.InvoicesList, params });
};

export const updateInvoicesList = (params?) => {
  return defHttp.post({ url: Api.UpdateInvoicesList, data: JSON.stringify(params)  });
};


