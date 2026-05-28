import { computed, ref } from 'vue';
import { OptionType } from "@/views/lms/support/types";
import { getCommonCodeByName, getSearchList } from '/@/api/common/api';
import { SupportApi } from "@/views/lms/support/api/support";

const alarmList = ref<any[]>([]);
const equipList = ref<any[]>([]);

// Select option
const centerOptions = ref<OptionType[]>();
const assigneeOptions = ref<OptionType[]>();
const statusOptions = ref<OptionType[]>([]);
const categoryOptions = ref<OptionType[]>([]);
const alarmOptions = computed(() =>
  alarmList.value.map((item) => ({
    label: `${ item.alarm_id } | ${ item.alarm_msg ?? '' }`.trim(),
    value: item.alarm_id,
    key: item.alarm_id,
    equip_id: item.equip_id,
    lc_id: item.lc_id,
  }))
);
const equipOptions = computed(() =>
  equipList.value.map((item) => ({
    label: `${ String(item.equip_id) } | ${ String(item.line_id) }`,
    value: item.equip_id,
    key: item.equip_id,
    lc_id: item.lc_id,
  }))
);

const CENTER_SEARCH_URL = '/centers_users/current_user/detail'
const ASSIGNEE_SEARCH_URL = '/users'

// 중복 조회 방지
const centerIsLoaded = ref(false);
const assigneeIsLoaded = ref(false);
const commonCodeIsLoaded = ref(false);
const alarmIsLoaded = ref(false);
const equipIsLoaded = ref(false);

export function useRefCodes(useFilterToAdmin: boolean) {

  // (Util) 조회 데이터 리셋
  const clearOptions = () => {
    centerIsLoaded.value = false;
    assigneeIsLoaded.value = false;
    commonCodeIsLoaded.value = false;
    alarmIsLoaded.value = false;
    equipIsLoaded.value = false;
    centerOptions.value = [];
    assigneeOptions.value = [];
    statusOptions.value = [];
    categoryOptions.value = [];
    alarmList.value = [];
    equipList.value = [];
  }

  // 공통코드 조회
  async function loadCommonCodes() {
    if (commonCodeIsLoaded.value) return;

    try {
      const [categoryList, statusList] = await Promise.all([
        getCommonCodeByName('SUPPORT_CATEGORY'),
        getCommonCodeByName('SUPPORT_STATUS'),
      ]);

      categoryOptions.value = categoryList.map((item) => ({
        label: item.text,
        value: item.value,
        key: item.value,
      }));

      statusOptions.value = statusList.map((item) => ({
        label: item.text,
        value: String(item.value),
        key: String(item.value),
      }));

      commonCodeIsLoaded.value = true;
    } catch (e) {
      console.error('[loadCommonCodes] Failed :', e);
    }
  }

  // 사용자의 소속센터목록 데이터 조회
  // - 관리자: 관리자페이지에서 전체 센터 조회, 사용자페이지에서 소속 센터 조회
  // - 사용자: 소속 센터 조회
  async function loadCenterOptions() {
    if (centerIsLoaded.value) return;

    try {
      const useFilter = !useFilterToAdmin;
      const response = await getSearchList(CENTER_SEARCH_URL, useFilter);
      const list = Array.isArray(response?.items) ? response.items : response;

      centerOptions.value = list
        .map((it) => ({
          label: it?.lc_nm ? `${ it.lc_nm } (${ it.lc_id })` : String(it?.lc_id ?? ''),
          value: String(it?.lc_id ?? '').trim(),
          originalNm: it?.lc_nm
        }))
        .sort((a, b) => a.label.localeCompare(b.label, 'ko-KR', { numeric: true }))

      centerIsLoaded.value = true;
    } catch (e) {
      // console.error('[loadCenterOptions] e =',e)
    }
  }

  // 담당자 목록 데이터 조회
  async function loadAssignerOptions() {
    if (assigneeIsLoaded.value) return;

    try {
      const queryFilter = [
        {
          name: 'admin_flag',
          operator: 'eq',
          value: true,
          relation: false,
        },
      ];

      const requestParams = {
        query: JSON.stringify(queryFilter),
        sort: [],
        page: 1,
        limit: 50,
      };
      const response = await getSearchList(ASSIGNEE_SEARCH_URL, requestParams);
      const list = Array.isArray(response?.items) ? response.items : response;
      assigneeOptions.value = list.map((el) => ({ text: el.id, value: el.id, key: el.id }));

      assigneeIsLoaded.value = true;
    } catch (e) {
      // console.error('[loadAssignerOptions] e', e);
    }
  }

  // 응답에서 items 배열 추출 (직접 배열 / { items: [] } 두 포맷 대응)
  function extractItems(response: any): any[] {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.items)) return response.items;
    return [];
  }

  // 알람 목록 전체 조회 : 모달 내 lc_id select로 필터링
  async function loadAlarms() {
    if (alarmIsLoaded.value) return;

    try {
      const response = await SupportApi.fetchAlarms();
      const items = extractItems(response);

      items.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime());

      alarmList.value = items;
      alarmIsLoaded.value = true;
    } catch (e) {
      // console.error('[loadAlarms] Failed:', e);
    }
  }

  // 설비 목록 전체 조회 : 모달 내 lc_id select로 필터링
  async function loadEquips() {
    if (equipIsLoaded.value) return;

    try {
      const response = await SupportApi.fetchEquips();
      const items = extractItems(response);

      items.sort((a, b) => String(a.equip_id).localeCompare(String(b.equip_id), 'ko-KR', { numeric: true }));

      equipList.value = items;
      equipIsLoaded.value = true;
    } catch (e) {
      // console.error('[loadEquips] Failed:', e);
    }
  }

  return {
    categoryOptions, statusOptions, centerOptions, assigneeOptions,
    alarmList, alarmOptions, alarmIsLoaded,
    equipList, equipOptions, equipIsLoaded,
    commonCodeIsLoaded, centerIsLoaded, assigneeIsLoaded,
    loadCommonCodes, loadCenterOptions, loadAssignerOptions, loadAlarms, loadEquips,
    clearOptions
  }
}
