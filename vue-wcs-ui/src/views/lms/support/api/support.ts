import { defHttp } from "@/utils/http/axios";
import { getCommonGetApi, getSearchList, updateList } from "@/api/common/api";

export const SupportApi = {
  // 요청 목록 조회
  fetchList: (params: any) => getSearchList('/support-request', params),

  // 단건 상세 조회
  fetchDetail: (id: string) => getCommonGetApi(`/support-request/${ encodeURIComponent(id) }`, null),

  // 요청 저장/수정
  saveSupport: (payload: any) => defHttp.post({
    url: '/support-request/update_one',
    data: payload
  }),

  // 그리드 일괄 업데이트
  updateGrid: (patches: any[]) => updateList('/support-request/update_multiple', patches),

  // 알람 목록 조회
  fetchAlarms: () => getCommonGetApi('/lms_alarm_status_dev', null),
  // 설비 목록 조회
  fetchEquips: () => getCommonGetApi('/lms_equip_status_dev', null),
}
