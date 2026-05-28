import { defHttp } from '/@/utils/http/axios';
import { LoginParams, LoginResultModel, GetUserInfoModel } from './model/userModel';

import { ErrorMessageMode } from '/#/axios';
import { useLocaleStore } from '/@/store/modules/locale';

enum Api {
  Login = '/vue/vue_login',
  Logout = '/vue/logout',
  GetUserInfo = '/vue/getUserInfo',
  GetPermCode = '/vue/getPermCode',
  TestRetry = '/testRetry',
  GetDomainList = '/vue/site_list',
}

/**
 * @description: user login api
 */
export function loginApi(params: LoginParams, mode: ErrorMessageMode = 'modal') {
  const localeStore = useLocaleStore();
  const currentLocale = localeStore.getLocale;

  defHttp.setHeader({ 'x-locale': currentLocale });
  return defHttp.post<LoginResultModel>(
    {
      url: Api.Login,
      params,
    },
    {
      errorMessageMode: mode,
    },
  );
}

/**
 * @description: getUserInfo
 */
export function getUserInfo() {
  const localeStore = useLocaleStore();
  const currentLocale = localeStore.getLocale;

  defHttp.setHeader({ 'x-locale': currentLocale });
  return defHttp.get<GetUserInfoModel>({ url: Api.GetUserInfo }, { errorMessageMode: 'none' });
}

export function getPermCode() {
  const localeStore = useLocaleStore();
  const currentLocale = localeStore.getLocale;

  defHttp.setHeader({ 'x-locale': currentLocale });
  return defHttp.post({ url: Api.GetPermCode });
}

export function doLogout() {
  const localeStore = useLocaleStore();
  const currentLocale = localeStore.getLocale;

  defHttp.setHeader({ 'x-locale': currentLocale });
  return defHttp.post({ url: Api.Logout });
}

/**
 * @description: Domain List
 */
export function getDomain(params) {
  const localeStore = useLocaleStore();
  const currentLocale = localeStore.getLocale;

  defHttp.setHeader({ 'x-locale': currentLocale });
  return defHttp.post(
    {
      url: Api.GetDomainList,
      params,
    },
    {
      errorMessageMode: 'none',
    },
  );
}

export function testRetry() {
  return defHttp.get(
    { url: Api.TestRetry },
    {
      retryRequest: {
        isOpenRetry: true,
        count: 5,
        waitTime: 1000,
      },
    },
  );
}
