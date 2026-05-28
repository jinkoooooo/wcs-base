import { getSearchList } from "@/api/common/api";

export const MonitoringApi = {
  // 센터 목록 조회
  fetchList: (url: string, params: any) => getSearchList(url, params),
}
