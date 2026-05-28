import { defineStore } from 'pinia';
import { getSearchList } from '/@/api/common/api';

/**
 * 공통 코드 Dictionary Store
 * - 서버에서 공통코드 전체를 가져와서 캐싱
 * - gridRmkMeta.ts의 decodeRmk/incodeRmk에서 사용
 *
 * data 구조:
 * [
 *   { name: "ORDER_STATUS", dictionary: { "90": "완료", "10": "대기", ... } },
 *   { name: "ORDER_TYPE",   dictionary: { "INBOUND": "입고", "OUTBOUND": "출고", ... } },
 * ]
 */
export const useRmkStore = defineStore('rmkStore', {
  state: () => ({
    data: [] as any[],
    loading: false,
    error: null as string | null,
  }),

  actions: {
    async fetchData() {
      // 이미 로드된 경우 스킵
      if (this.data && this.data.length > 0) return;

      this.loading = true;
      this.error = null;
      try {
        const url = '/rmkdictionary/getRmkDictionary';
        const response = await getSearchList(url);
        this.data = Array.isArray(response) ? response : [];
      } catch (err: any) {
        this.error = err?.message || 'Failed to fetch RMK dictionary';
        console.error('[rmkStore] fetchData error:', err);
      } finally {
        this.loading = false;
      }
    },
  },
});
