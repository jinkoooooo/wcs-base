import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import type { AreaCodeOption } from '@/views/asrs/shared/components/form/AsrsAreaCodeField.vue';
import {
  getOutboundLocation2DAisles,
  getOutboundLocation2DAreas,
  getOutboundLocation2DMap,
  getOutboundLocation2DSides,
  postOutboundLocation2DExecute,
} from '@/api/asrs/stock';
import {
  normalizeAisleOptions,
  normalizeLocation2DMap,
  normalizeSideOptions,
} from '../mappers/outboundLocation2D.mapper';
import type {
  OutboundLocation2DCell,
  OutboundLocation2DDepth,
  OutboundLocation2DExecuteForm,
  OutboundLocation2DFilter,
  OutboundLocation2DLoadingState,
  OutboundLocation2DMap,
  OutboundLocation2DViewState,
} from '../types';

function createInitialFilter(): OutboundLocation2DFilter {
  return {
    areaCode: '',
    aisleNo: '',
    sideCode: '',
  };
}

function createInitialViewState(): OutboundLocation2DViewState {
  return {
    zoom: 1,
    offsetX: 0,
    offsetY: 0,
  };
}

function createInitialExecuteForm(): OutboundLocation2DExecuteForm {
  return {
    refDocType: 'ORDER',
    refDocNo: '',
    refLineNo: '',
    reasonCode: '',
    remark: '',
  };
}

export function useOutboundLocation2D() {
  const filter = reactive<OutboundLocation2DFilter>(createInitialFilter());
  const viewState = reactive<OutboundLocation2DViewState>(createInitialViewState());
  const executeForm = reactive<OutboundLocation2DExecuteForm>(createInitialExecuteForm());

  const { flags: loading } = useAsyncFlags<OutboundLocation2DLoadingState>({
    areas: false,
    aisles: false,
    sides: false,
    map: false,
    execute: false,
  } as OutboundLocation2DLoadingState);

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  const areaOptions = ref<AreaCodeOption[]>([]);
  const aisleOptions = ref<number[]>([]);
  const sideOptions = ref<string[]>([]);
  const rackMap = ref<OutboundLocation2DMap | null>(null);

  const selectedCell = ref<OutboundLocation2DCell | null>(null);
  const selectedDepthNo = ref<number>(1);

  const selectedDepth = computed<OutboundLocation2DDepth | null>(() => {
    if (!selectedCell.value) return null;
    return selectedCell.value.depths.find((d) => d.depthNo === selectedDepthNo.value) || null;
  });

  const groupedCells = computed(() => {
    if (!rackMap.value) return [];

    return rackMap.value.cells.map((cell) => ({
      ...cell,
      key: `${cell.bayNo}-${cell.levelNo}`,
    }));
  });

  const canExecute = computed(() => {
    return !!selectedDepth.value?.locationId && !!selectedDepth.value?.stockUnitNo;
  });

  async function loadAreas() {
    loading.areas = true;
    try {
      const payload = await getOutboundLocation2DAreas();

      areaOptions.value = Array.isArray(payload)
        ? payload
          .map((area: any) => {
            const areaCode = area.areaCode || area.area_code || '';
            const areaName = area.areaName || area.area_name || '';
            if (!areaCode) return null;
            return { areaCode, areaName };
          })
          .filter(Boolean) as AreaCodeOption[]
        : [];
    } catch (error) {
      console.error(error);
      areaOptions.value = [];
      setFeedback('error', 'Area 목록을 불러오지 못했습니다.');
    } finally {
      loading.areas = false;
    }
  }

  async function loadAisles() {
    if (!filter.areaCode) {
      aisleOptions.value = [];
      filter.aisleNo = '';
      return;
    }

    loading.aisles = true;
    try {
      const payload = await getOutboundLocation2DAisles(filter.areaCode);
      aisleOptions.value = normalizeAisleOptions(payload).map((item) => item.aisleNo);
    } catch (error) {
      console.error(error);
      aisleOptions.value = [];
      setFeedback('error', 'Aisle 목록을 불러오지 못했습니다.');
    } finally {
      loading.aisles = false;
    }
  }

  async function loadSides() {
    if (!filter.areaCode || !filter.aisleNo) {
      sideOptions.value = [];
      filter.sideCode = '';
      return;
    }

    loading.sides = true;
    try {
      const payload = await getOutboundLocation2DSides(filter.areaCode, filter.aisleNo);
      sideOptions.value = normalizeSideOptions(payload).map((item) => item.sideCode);
    } catch (error) {
      console.error(error);
      sideOptions.value = [];
      setFeedback('error', 'Side 목록을 불러오지 못했습니다.');
    } finally {
      loading.sides = false;
    }
  }

  async function loadMap() {
    if (!filter.areaCode || !filter.aisleNo || !filter.sideCode) {
      rackMap.value = null;
      selectedCell.value = null;
      return;
    }

    loading.map = true;
    try {
      const payload = await getOutboundLocation2DMap(
        filter.areaCode,
        filter.aisleNo,
        filter.sideCode,
      );
      rackMap.value = normalizeLocation2DMap(payload);

      if (rackMap.value.cells.length > 0) {
        selectedCell.value = rackMap.value.cells[0];
        selectedDepthNo.value = selectedCell.value.depths[0]?.depthNo || 1;
      } else {
        selectedCell.value = null;
      }

      clearFeedback();
    } catch (error) {
      console.error(error);
      rackMap.value = null;
      selectedCell.value = null;
      setFeedback('error', '2D 맵을 불러오지 못했습니다.');
    } finally {
      loading.map = false;
    }
  }

  function selectCell(cell: OutboundLocation2DCell) {
    selectedCell.value = cell;
    selectedDepthNo.value = cell.depths[0]?.depthNo || 1;
  }

  function selectDepth(depthNo: number) {
    selectedDepthNo.value = depthNo;
  }

  function zoomIn() {
    viewState.zoom = Math.min(2.5, Number((viewState.zoom + 0.1).toFixed(2)));
  }

  function zoomOut() {
    viewState.zoom = Math.max(0.5, Number((viewState.zoom - 0.1).toFixed(2)));
  }

  function resetZoom() {
    Object.assign(viewState, createInitialViewState());
  }

  function setPan(payload: { offsetX: number; offsetY: number }) {
    viewState.offsetX = payload.offsetX;
    viewState.offsetY = payload.offsetY;
  }

  async function executeOutbound() {
    if (!selectedDepth.value?.locationId || !selectedDepth.value?.stockUnitNo) {
      setFeedback('warning', '출고할 depth 재고를 선택해주세요.');
      return;
    }

    loading.execute = true;
    try {
      await postOutboundLocation2DExecute({
        locationId: selectedDepth.value.locationId,
        stockUnitNo: selectedDepth.value.stockUnitNo,
        refDocType: executeForm.refDocType || undefined,
        refDocNo: executeForm.refDocNo || undefined,
        refLineNo: executeForm.refLineNo || undefined,
        reasonCode: executeForm.reasonCode || undefined,
        remark: executeForm.remark || undefined,
      });

      setFeedback('success', '지정출고가 완료되었습니다.');
      await loadMap();
    } catch (error: any) {
      console.error(error);
      setFeedback('error', '지정출고 중 오류가 발생했습니다.');
    } finally {
      loading.execute = false;
    }
  }

  watch(
    () => filter.areaCode,
    async () => {
      filter.aisleNo = '';
      filter.sideCode = '';
      aisleOptions.value = [];
      sideOptions.value = [];
      rackMap.value = null;
      selectedCell.value = null;
      await loadAisles();
    },
  );

  watch(
    () => filter.aisleNo,
    async () => {
      filter.sideCode = '';
      sideOptions.value = [];
      rackMap.value = null;
      selectedCell.value = null;
      await loadSides();
    },
  );

  watch(
    () => filter.sideCode,
    async () => {
      await loadMap();
    },
  );

  onMounted(async () => {
    await loadAreas();
  });

  return {
    filter,
    executeForm,
    loading,
    feedback,
    areaOptions,
    aisleOptions,
    sideOptions,
    rackMap,
    groupedCells,
    selectedCell,
    selectedDepthNo,
    selectedDepth,
    canExecute,
    loadAreas,
    loadAisles,
    loadSides,
    loadMap,
    selectCell,
    selectDepth,
    viewState,
    zoomIn,
    zoomOut,
    resetZoom,
    executeOutbound,
    setPan,
  };
}
