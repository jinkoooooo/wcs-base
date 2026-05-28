import { ref } from 'vue';
import { MonitoringApi } from "@/views/lms/monitoring-status/api/monitoringApi";
import { CenterDataType } from "@/views/lms/monitoring-status/types";

const centerData = ref<CenterDataType[]>([]);

// 중복 조회 방지
const centerIsLoaded = ref(false);

export function useCenters(baseUrl: string) {

  // (Util) 조회 데이터 리셋
  const clearCenter = () => {
    centerIsLoaded.value = false;
    centerData.value = [];
  }

  // 센터목록 데이터 조회
  async function loadCenter() {
    if (centerIsLoaded.value) return;

    try {
      const response = await MonitoringApi.fetchList(baseUrl, null)
      centerData.value = Array.isArray(response?.items) ? response.items : response;
      centerIsLoaded.value = true;
    } catch (e) {
      // console.error('[loadCenter] e =', e)
    }
  }

  return { clearCenter, loadCenter, centerData, centerIsLoaded }
}
