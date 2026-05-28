/* src/views/tspg-4way-shuttle/dashboard_2d/composables/useCanvasSelection.vue */
import { computed, ref } from 'vue';

/**
 * 캔버스 선택(단일/다중/마퀴 선택) 유틸.
 * - store가 단일 selectedObjectId만 갖고 있어도 내부적으로 다중 선택을 유지할 수 있게 설계.
 */
export function useCanvasSelection() {
  const selectedIds = ref<string[]>([]);
  const primaryId = computed(() => (selectedIds.value.length > 0 ? selectedIds.value[0] : null));

  const isSelected = (id: string) => selectedIds.value.includes(id);

  const clear = () => {
    selectedIds.value = [];
  };

  const selectSingle = (id: string | null) => {
    selectedIds.value = id ? [id] : [];
  };

  const toggle = (id: string) => {
    if (isSelected(id)) selectedIds.value = selectedIds.value.filter((x) => x !== id);
    else selectedIds.value = [...selectedIds.value, id];
  };

  const add = (id: string) => {
    if (!isSelected(id)) selectedIds.value = [...selectedIds.value, id];
  };

  const setMany = (ids: string[], opts?: { keepExisting?: boolean }) => {
    if (opts?.keepExisting) {
      const set = new Set(selectedIds.value);
      ids.forEach((id) => set.add(id));
      selectedIds.value = Array.from(set);
      return;
    }
    selectedIds.value = [...ids];
  };

  return {
    selectedIds,
    primaryId,
    isSelected,
    clear,
    selectSingle,
    toggle,
    add,
    setMany,
  };
}
