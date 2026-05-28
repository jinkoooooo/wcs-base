// axios配置  可自行根据项目进行更改，只需更改该文件即可，其他文件可以不动
// The axios configuration can be changed according to the project, just change the file, other files can be left unchanged

import type { AxiosInstance, AxiosResponse, AxiosError } from "axios";
import { clone } from "lodash-es";
import type { RequestOptions, Result } from "/#/axios";
import type { AxiosTransform, CreateAxiosOptions } from "./axiosTransform";
import { VAxios } from "./Axios";
import { checkStatus } from "./checkStatus";
import { useGlobSetting } from "@/hooks/setting";
import { useMessage } from "@/hooks/web/useMessage";
import { RequestEnum, ResultEnum, ContentTypeEnum } from "@/enums/httpEnum";
import { isString, isUnDef, isNull, isEmpty } from "@/utils/is";
import { getToken } from "@/utils/auth";
import { setObjToUrlParams, deepMerge } from "@/utils";
import { useI18n } from "@/hooks/web/useI18n";
import { joinTimestamp, formatRequestDate } from "./helper";
import { useUserStoreWithOut } from "@/store/modules/user";
import { AxiosRetry } from "@/utils/http/axios/axiosRetry";
import axios from "axios";

const globSetting = useGlobSetting();
const urlPrefix = globSetting.urlPrefix;
const { createMessage } = useMessage();

/**
 * @description: 数据处理，方便区分多种处理方式
 */
const transform: AxiosTransform = {
  /**
   * @description: 응답데이터를 then/catch로 전달되기 전에 처리할 수 있게 해줌. 프로젝트의 지침에 따라 알맞게 처리한다.
   */
  transformResponseHook: (
    res: AxiosResponse<Result>,
    options: RequestOptions
  ) => {
    const { t } = useI18n();
    const { isTransformResponse, isReturnNativeResponse } = options;
    // 원래 응답 헤더를 반환할지 여부: 응답 헤더를 가져올 때 이 속성 사용하기
    if (isReturnNativeResponse) {
      return res;
    }
    // 아무런 처리 없이 바로 돌아가기
    // 페이지 코드에서 code, data, message 정보를 직접 가져와야 할 경우 켜짐
    if (!isTransformResponse) {
      return res.data;
    }
    // 오류 발생 시 되돌리기

    const { data } = res;
    if (!data) {
      // return '[HTTP] Request has no return value';
      throw new Error(t("api.apiRequestFailed"));
    }
    //  여기서 code, result, message는 백그라운드 통합 필드입니다. types.ts에서 프로젝트 자체 인터페이스로 포맷을 되돌리기
    //  const { code, result, message } = data;

    // 여기 논리는 항목에 따라 수정할 수 있습니다.
    // const hasSuccess = data && Reflect.has(data, 'code') && code === ResultEnum.SUCCESS;
    const hasSuccess = data;
    if (hasSuccess) {
      let successMsg = "OK"; //message;

      if (isNull(successMsg) || isUnDef(successMsg) || isEmpty(successMsg)) {
        successMsg = t(`api.operationSuccess`);
      }
      // createMessage.success(successMsg);
      return data;
    }
  },

  // 요청하기 전에 config 처리
  beforeRequestHook: (config, options) => {
    const {
      apiUrl,
      joinPrefix,
      joinParamsToUrl,
      formatDate,
      joinTime = true,
      urlPrefix,
    } = options;

    if (joinPrefix) {
      config.url = `${urlPrefix}${config.url}`;
    }

    if (apiUrl && isString(apiUrl)) {
      config.url = `${apiUrl}${config.url}`;
    }
    const params = config.params || {};
    const data = config.data || false;
    formatDate && data && !isString(data) && formatRequestDate(data);
    if (config.method?.toUpperCase() === RequestEnum.GET) {
      if (!isString(params)) {
        // 캐시에서 데이터를 가져오지 않도록 get 요청에 타임스탬프 매개 변수를 추가합니다.
        config.params = Object.assign(
          params || {},
          joinTimestamp(joinTime, false)
        );
      } else {
        // 호환 restful 스타일
        config.url = config.url + params + `${joinTimestamp(joinTime, true)}`;
        config.params = undefined;
      }
    } else {
      if (!isString(params)) {
        formatDate && formatRequestDate(params);
        if (
          Reflect.has(config, "data") &&
          config.data &&
          (Object.keys(config.data).length > 0 ||
            config.data instanceof FormData)
        ) {
          config.data = data;
          config.params = params;
        } else {
          // 비GET 요청은 data가 제공되지 않을 경우 params를 data로 간주합니다
          config.data = params;
          config.params = undefined;
        }
        if (joinParamsToUrl) {
          config.url = setObjToUrlParams(
            config.url as string,
            Object.assign({}, config.params, config.data)
          );
        }
      } else {
        // 호환 restful 스타일
        config.url = config.url + params;
        config.params = undefined;
      }
    }
    return config;
  },

  /**
   * @description: 응답 처리
   */
  requestInterceptors: (config, options) => {
    // 요청하기 전에 config 처리
    const token = getToken();
    if (token && (config as Recordable)?.requestOptions?.withToken !== false) {
      // jwt token
      (config as Recordable).headers.Authorization =
        options.authenticationScheme
          ? `${options.authenticationScheme} ${token}`
          : token;
    }
    return config;
  },

  /**
   * @description: 응답  처리
   */
  responseInterceptors: (res: AxiosResponse<any>) => {
    return res;
  },

  /**
   * @description: 응답 Interceptor 오류 처리
   */
  responseInterceptorsCatch: (axiosInstance: AxiosInstance, error: any) => {
    const { t } = useI18n();
    // const errorLogStore = useErrorLogStoreWithOut();
    // errorLogStore.addAjaxErrorInfo(error);
    const { response, code, message, config } = error || {};
    const errorMessageMode = config?.requestOptions?.errorMessageMode || "none";
    const msg: string = response?.data?.error?.message ?? "";
    const err: string = error?.toString?.() ?? "";
    let errMessage = response?.data?.msg;
    let errCd = response?.data?.code;
    if (axios.isCancel(error)) {
      return Promise.reject(error);
    }

    try {
      if (code === "ECONNABORTED" && message.indexOf("timeout") !== -1) {
        errMessage = t("api.apiTimeoutMessage");
      }
      if (err?.includes("Network Error")) {
        errMessage = t("api.networkExceptionMsg");
      }

      if (errMessage) {
        createMessage.error(errMessage);
        return Promise.reject(error);
      }
    } catch (error) {
      throw new Error(error as unknown as string);
    }

    checkStatus(error?.response?.status, msg, errorMessageMode);

    // GET 요청 시 자동 재시도 메커니즘 추가
    const retryRequest = new AxiosRetry();
    const { isOpenRetry } = config.requestOptions.retryRequest;
    config.method?.toUpperCase() === RequestEnum.GET &&
      isOpenRetry &&
      // @ts-ignore
      retryRequest.retry(axiosInstance, error);
    return Promise.reject(error);
  },
};

function createAxios(opt?: Partial<CreateAxiosOptions>) {
  return new VAxios(
    // 심층합병
    deepMerge(
      {
        // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication#authentication_schemes
        // authentication schemes，e.g: Bearer
        // authenticationScheme: 'Bearer',
        authenticationScheme: "",
        timeout: 10 * 1000,
        // 기본 인터페이스 주소
        // baseURL: globSetting.apiUrl,

        headers: { "Content-Type": ContentTypeEnum.JSON },
        // form-data 형식이면
        // headers: { 'Content-Type': ContentTypeEnum.FORM_URLENCODED },
        // 데이터 처리 방식
        transform: clone(transform),
        // 설정 항목, 다음 옵션을 모두 독립적인 인터페이스 요청으로 덮어쓸 수 있습니다.
        requestOptions: {
          // 기본값 url에 prefix 추가
          joinPrefix: true,
          // 원래 응답 헤더를 반환할지 여부: 응답 헤더를 가져올 때 이 속성 사용하기
          isReturnNativeResponse: false,
          // 반환된 데이터를 처리해야 함
          isTransformResponse: true,
          // post 요청 시 url에 인자 추가
          joinParamsToUrl: false,
          // 시간 포맷
          formatDate: true,
          // 메시지 큐 종류
          errorMessageMode: "message",
          // 인터페이스 주소
          apiUrl: globSetting.apiUrl,
          // 인터페이스 스플라이스 주소
          urlPrefix: urlPrefix,
          //  타임스탬프 추가 여부
          joinTime: true,
          // 중복 요청 무시하기
          ignoreCancelToken: true,
          // 휴대 token
          withToken: true,
          retryRequest: {
            isOpenRetry: false,
            count: 5,
            waitTime: 100,
          },
        },
      },
      opt || {}
    )
  );
}
export const defHttp = createAxios();

// other api url
// export const otherHttp = createAxios({
//   requestOptions: {
//     apiUrl: 'xxx',
//     urlPrefix: 'xxx',
//   },
// });
