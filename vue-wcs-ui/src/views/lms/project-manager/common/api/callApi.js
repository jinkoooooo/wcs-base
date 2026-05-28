import axios from 'axios';

/**
 * API 호출 공통 함수
 * - get: params로 전송
 * - 그 외: data(body)로 전송
 * - timeout / headers / withCredentials 등 옵션 확장 가능
 */
export const callApi = async (method, url, param = {}, options = {}) => {
  const httpMethod = String(method || 'get').toLowerCase();

  try {
    let response;

    if (httpMethod === 'get') {
      response = await axios.get(url, {
        params: param,
        ...options,
      });
    } else {
      response = await axios({
        method: httpMethod,
        url,
        data: param,
        ...options,
      });
    }
    return response.data;
  } catch (error) {
    console.error(`[API 응답 실패] ${url}`, error);
    throw error;
  }
};

export default callApi;
