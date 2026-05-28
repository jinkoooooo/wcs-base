// 파렛트 작업대 facade — sub-composable 들을 묶어 SFC 에서 1회 호출.

import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { palletApi } from '../api';
import {
  derivePalletStateLabel,
  parseApiError,
  pickedOf,
  remainingOf,
  totalOf,
  type BoxFilter,
} from '../shared';
import { useColumns } from '../components/box-list/useColumns';
import { useMessage } from '/@/hooks/web/useMessage';
import {
  BoxStatus,
  WcsOrderStatus,
  WcsOrderStatusLabels,
} from '/@/views/tspg_4way/constants/wcsConsts';
import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';
import type { ActionId, Scenario } from '../scenarios';
import { useBoxActions } from './useBoxActions';
import { useBoxData } from './useBoxData';
import { useBoxEditing } from './useBoxEditing';
import { useFinalizeModal } from './useFinalizeModal';
import { useLabelPrint } from './useLabelPrint';
import { useScan } from './useScan';
import { useScenario } from './useScenario';

export function usePalletWorkstation() {
  const MENU = 'PalletWorkstation';
  const { can, isAdmin } = usePermissionLocal(MENU);
  const { notification, createConfirm } = useMessage();

  // ─── 입력 / DOM refs ─────
  const pallet = ref('');
  const box = ref('');
  const palletRef = ref<any>(null);
  const boxRef = ref<any>(null);
  const qtyInputRef = ref<HTMLInputElement | null>(null);
  const boxTableWrapRef = ref<HTMLElement | null>(null);

  // ─── UI 플래그 / 메시지 ─────
  const autoRelease = ref(true);
  const loading = ref(false);
  const busy = ref(false);
  const printing = reactive({ pallet: false, boxes: false, mark: false, selected: false });
  const msg = ref('');
  const msgKind = ref<'ok' | 'err'>('ok');
  const msgClass = computed(() => (msgKind.value === 'ok' ? 'ok' : 'err'));
  const boxFilter = ref<BoxFilter>('all');

  function setMsg(text: string, kind: 'ok' | 'err' = 'ok') {
    msg.value = text;
    msgKind.value = kind;
  }

  // ─── 드로어 / 헤더 메뉴 ─────
  const auxDrawerOpen = ref(false);
  const historyDrawerOpen = ref(false);
  const headerMenuOpen = ref(false);

  // ─── 이력 모달 ─────
  const historyModal = reactive({ open: false, highlightOrderKey: '' as string });
  function openHistoryModal(orderKey?: string) {
    historyModal.highlightOrderKey = orderKey || '';
    historyModal.open = true;
  }
  function closeHistoryModal() {
    historyModal.open = false;
    historyModal.highlightOrderKey = '';
  }

  // ─── 행 선택 ─────
  const selectedBoxIds = ref<Set<string>>(new Set());
  function toggleSelectRow(id: string) {
    const next = new Set(selectedBoxIds.value);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    selectedBoxIds.value = next;
  }
  function clearSelection() {
    selectedBoxIds.value = new Set();
  }

  // ─── 컬럼 ─────
  const {
    columns,
    tableTotalWidth,
    resizingColKey,
    startColResize,
    onColResizeMove,
    onColResizeUp,
    autoFitColumn,
    resetColumnWidths,
  } = useColumns();

  // ─── 데이터 ─────
  const data = useBoxData();
  const { info, boxes, act, inboundProgress, lifecycle } = data;

  async function reloadAll() {
    if (pallet.value.trim()) await data.load(pallet.value.trim());
  }

  // ─── 박스 추가/삭제 가능 여부 (편집 composable 보다 먼저 필요) ─────
  const canAddOrDeleteBox = computed(() => {
    if (!info.value) return false;
    if (!act.value) return true;
    if (act.value.mode === 'OUTBOUND') return false;
    if (act.value.mode === 'PENDING_SAMPLE') return false;
    if (act.value.mode === 'POST_OUTBOUND') return false;
    return act.value.orderStatus === WcsOrderStatus.CREATED && act.value.mode !== 'INBOUND';
  });

  // ─── 편집 ─────
  const editing = useBoxEditing({
    palletBarcode: pallet,
    boxes,
    setMsg,
    reloadAll,
    createConfirm,
  });

  // ─── 라벨 인쇄 ─────
  const labelPrint = useLabelPrint({
    palletBarcode: pallet,
    info,
    boxes,
    selectedBoxIds,
    printing,
    setMsg,
    notification,
    refreshBoxesOnly: () => data.refreshBoxesOnly(pallet.value.trim()),
    reloadAll,
    clearSelection,
  });

  // ─── 스캔 (printBoxLabel · releaseInbound 의존) ─────
  // releaseInbound 는 아래에 정의되어 있으므로 wrapper 로 늦은 바인딩.
  let _releaseInboundImpl: () => Promise<void> = async () => {};
  const scan = useScan({
    palletBarcode: pallet,
    box,
    boxRef,
    boxTableWrapRef,
    act,
    boxes,
    inboundProgress,
    autoRelease,
    refreshBoxesOnly: () => data.refreshBoxesOnly(pallet.value.trim()),
    releaseInbound: () => _releaseInboundImpl(),
    printBoxLabel: labelPrint.printBoxLabel,
    setMsg,
    notification,
  });

  // ─── 박스 단건 액션 ─────
  const actions = useBoxActions({
    palletBarcode: pallet,
    draftEdits: editing.draftEdits,
    draftAdditions: editing.draftAdditions,
    draftDeletions: editing.draftDeletions,
    addDraftAddition: editing.addDraftAddition,
    applyDraftEditTotal: editing.applyDraftEditTotal,
    toggleDraftDeletion: editing.toggleDraftDeletion,
    canAddOrDeleteBox,
    setMsg,
    reloadAll,
  });

  // ─── 확정 미리보기 모달 ─────
  const finalizeModalApi = useFinalizeModal({
    palletBarcode: pallet,
    boxes,
    pi: scan.pi,
    setMsg,
    reloadAll,
  });

  // ─── 호스트 품목 합계 ─────
  const hostItemExpected = ref<Record<string, number>>({});
  async function loadHostItemExpected() {
    if (!pallet.value.trim()) return;
    try {
      const r: any = await palletApi.hostItems(pallet.value.trim());
      const list: any[] = Array.isArray(r) ? r : r?.data ?? [];
      const next: Record<string, number> = {};
      for (const it of list) next[`${it.itemCode ?? ''}|${it.lotNo ?? ''}`] = Number(it.qty) || 0;
      hostItemExpected.value = next;
    } catch {
      hostItemExpected.value = {};
    }
  }
  function expectedSumOf(itemCode: string, lotNo: string): number {
    return hostItemExpected.value[`${itemCode ?? ''}|${lotNo ?? ''}`] ?? 0;
  }

  // ─── 표시용 박스 / 필터 ─────
  const displayBoxes = computed(() => {
    const existing = boxes.value.map((b: any) => ({
      ...b,
      _draftEditTotal: editing.draftEdits[b.id] ?? null,
      _draftDeleted: !!editing.draftDeletions[b.id],
      _isAddition: false,
    }));
    const additions = editing.draftAdditions.map((a, idx) => ({
      id: a.tempId,
      box_seq: '+',
      box_barcode: '(저장 시 부여)',
      item_code: a.itemCode,
      lot_no: a.lotNo,
      total_qty: a.totalQty,
      remaining_qty: a.totalQty,
      picked_qty: 0,
      box_status: BoxStatus.DRAFT.code,
      print_count: 0,
      uom: a.uom ?? 'EA',
      _draftEditTotal: null,
      _draftDeleted: false,
      _isAddition: true,
      _additionIndex: idx,
    }));
    return [...existing, ...additions];
  });

  const filterCounts = computed(() => {
    const counts = { all: 0, printed: 0, scanned: 0, partial: 0, depleted: 0, pending: 0 };
    for (const b of displayBoxes.value as any[]) {
      counts.all++;
      const s = b.box_status;
      if (s === BoxStatus.DRAFT.code || s === BoxStatus.PENDING.code) counts.pending++;
      else if (s === BoxStatus.PRINTED.code) counts.printed++;
      else if (s === BoxStatus.SCANNED.code) {
        if (pickedOf(b) > 0 || remainingOf(b) < totalOf(b)) counts.partial++;
        else counts.scanned++;
      } else if (s === BoxStatus.DEPLETED.code) counts.depleted++;
    }
    return counts;
  });

  const filteredDisplayBoxes = computed(() => {
    if (boxFilter.value === 'all') return displayBoxes.value;
    return (displayBoxes.value as any[]).filter((b) => {
      const s = b.box_status;
      switch (boxFilter.value) {
        case 'pending':
          return s === BoxStatus.DRAFT.code || s === BoxStatus.PENDING.code;
        case 'printed':
          return s === BoxStatus.PRINTED.code;
        case 'scanned':
          return s === BoxStatus.SCANNED.code && !(pickedOf(b) > 0 || remainingOf(b) < totalOf(b));
        case 'partial':
          return s === BoxStatus.SCANNED.code && (pickedOf(b) > 0 || remainingOf(b) < totalOf(b));
        case 'depleted':
          return s === BoxStatus.DEPLETED.code;
      }
      return true;
    });
  });

  const selectableIds = computed(() =>
    (filteredDisplayBoxes.value as any[]).filter((b) => !b._isAddition).map((b) => b.id as string),
  );
  const allSelected = computed(
    () =>
      selectableIds.value.length > 0 &&
      selectableIds.value.every((id) => selectedBoxIds.value.has(id)),
  );
  const someSelected = computed(() =>
    selectableIds.value.some((id) => selectedBoxIds.value.has(id)),
  );
  function toggleSelectAll() {
    if (allSelected.value) {
      clearSelection();
    } else {
      const next = new Set<string>();
      for (const b of filteredDisplayBoxes.value as any[]) {
        if (b._isAddition) continue;
        next.add(b.id);
      }
      selectedBoxIds.value = next;
    }
  }

  const itemLotSummary = computed(() => {
    const map = new Map<string, { itemCode: string; lotNo: string; sum: number }>();
    for (const b of displayBoxes.value as any[]) {
      if (b.box_status === BoxStatus.VOID.code) continue;
      if (b._draftDeleted) continue;
      const key = `${b.item_code ?? ''}|${b.lot_no ?? ''}`;
      const cur = map.get(key) ?? { itemCode: b.item_code ?? '', lotNo: b.lot_no ?? '', sum: 0 };
      const eff = b._draftEditTotal != null ? Number(b._draftEditTotal) : totalOf(b);
      cur.sum += eff;
      map.set(key, cur);
    }
    return Array.from(map.values());
  });

  // ─── 박스 상태 표시 helpers ─────
  function boxStatusLabel(b: any): string {
    switch (b.box_status) {
      case BoxStatus.DRAFT.code:
        return BoxStatus.DRAFT.label;
      case BoxStatus.PENDING.code:
        return BoxStatus.PENDING.label;
      case BoxStatus.PRINTED.code:
        return BoxStatus.PRINTED.label;
      case BoxStatus.SCANNED.code:
        if (pickedOf(b) > 0) return '잡힘';
        if (remainingOf(b) < totalOf(b)) return '부분 출고';
        return BoxStatus.SCANNED.label;
      case BoxStatus.DEPLETED.code:
        return BoxStatus.DEPLETED.label;
      case BoxStatus.VOID.code:
        return BoxStatus.VOID.label;
      default:
        return String(b.box_status ?? '');
    }
  }
  function boxStatusClass(b: any): string {
    const s = b.box_status;
    switch (s) {
      case BoxStatus.DRAFT.code:
      case BoxStatus.PENDING.code:
        return 'sp-pending';
      case BoxStatus.PRINTED.code:
        return 'sp-printed';
      case BoxStatus.SCANNED.code:
        if (pickedOf(b) > 0 || remainingOf(b) < totalOf(b)) return 'sp-partial';
        return 'sp-scanned';
      case BoxStatus.DEPLETED.code:
        return 'sp-depleted';
      case BoxStatus.VOID.code:
        return 'sp-void';
      default:
        return '';
    }
  }
  function boxRowClass(b: any): string {
    const s = b.box_status;
    if (s === BoxStatus.DEPLETED.code) return 'row-depleted';
    if (s === BoxStatus.VOID.code) return 'row-void';
    if (s === BoxStatus.SCANNED.code && (pickedOf(b) > 0 || remainingOf(b) < totalOf(b)))
      return 'row-partial';
    if (s === BoxStatus.PRINTED.code) return 'row-printed';
    return '';
  }
  function canEditQty(b: any): boolean {
    return b.box_status !== BoxStatus.DEPLETED.code && b.box_status !== BoxStatus.VOID.code;
  }
  function canEditTotal(b: any): boolean {
    if (!b) return false;
    const printed = Number(b.print_count ?? b.printCount ?? 0);
    return b.box_status === BoxStatus.DRAFT.code && printed === 0;
  }
  function isDeletableBox(b: any): boolean {
    // 백엔드 voidPendingBox 와 동일 기준: DRAFT + 라벨 미인쇄
    return (
      b.box_status === BoxStatus.DRAFT.code && Number(b.print_count ?? b.printCount ?? 0) === 0
    );
  }
  function hasMoreActions(b: any): boolean {
    if (canEditTotal(b) && !b._draftDeleted) return true;
    if (!b._isAddition && canEditTotal(b) && b._draftEditTotal != null) return true;
    if (canEditTotal(b) && canAddOrDeleteBox.value) return true;
    // 확정 전 기존 박스 삭제/복구 진입 보장
    if (!b._isAddition && canAddOrDeleteBox.value && isDeletableBox(b)) return true;
    if (!b._isAddition && canEditQty(b)) return true;
    if (!b._isAddition && scan.canPartialOutbound.value && canEditQty(b)) return true;
    return false;
  }

  // 선택 박스 중 삭제 가능(DRAFT·라벨 미인쇄·아직 삭제예정 아님) 개수.
  const deletableSelectedCount = computed(() => {
    let n = 0;
    for (const id of selectedBoxIds.value) {
      const b = boxes.value.find((x: any) => x.id === id);
      if (b && isDeletableBox(b) && !editing.draftDeletions[id]) n++;
    }
    return n;
  });
  // 선택 박스 일괄 삭제 — 단건과 동일 draft 방식(저장 시 반영, 복구 가능).
  function bulkDeleteSelected() {
    let count = 0;
    for (const id of Array.from(selectedBoxIds.value)) {
      const b = boxes.value.find((x: any) => x.id === id);
      if (b && isDeletableBox(b) && !editing.draftDeletions[id]) {
        editing.toggleDraftDeletion(b);
        count++;
      }
    }
    if (count === 0) return;
    setMsg(`${count}개 박스 삭제 예정 (저장 대기).`);
    clearSelection();
  }
  function shuttleStatusLabel(status: number | null | undefined): string {
    if (status == null) return '';
    return WcsOrderStatusLabels[status] ?? `상태 ${status}`;
  }

  // ─── 시나리오 / 스텝 ─────
  const hasUnprintedActive = computed(() =>
    boxes.value.some((b: any) => {
      const s = Number(b.box_status);
      if (
        s === BoxStatus.SCANNED.code ||
        s === BoxStatus.DEPLETED.code ||
        s === BoxStatus.VOID.code
      )
        return false;
      return Number(b.print_count ?? b.printCount ?? 0) === 0;
    }),
  );
  const canUpdate = computed(() => can('update'));
  const isAdminComp = computed(() => isAdmin.value);

  const scenarioApi = useScenario({
    info,
    act,
    boxes,
    lifecycle,
    hasUnsavedChanges: editing.hasUnsavedChanges,
    hasDraft: editing.hasUnfinalizedBoxes,
    hasUnprintedActive,
    scanComplete: scan.scanComplete,
    outboundComplete: scan.outboundComplete,
    hasPartialPicked: scan.hasPartialPicked,
    anyTaken: scan.anyTaken,
    isOutboundArrived: scan.isOutboundArrived,
    unfinalizedCount: editing.unfinalizedCount,
    pickedQty: computed(() => scan.pi.value.picked),
    expectedQty: computed(() => scan.pi.value.expected),
    canUpdate,
    isAdmin: isAdminComp,
    busy,
    finalizingBoxes: editing.finalizingBoxes,
    autoRelease,
    printing,
  });

  // ─── 입출고 / 재입고 ─────
  async function releaseInbound() {
    busy.value = true;
    try {
      const bypass = !scan.scanComplete.value && isAdmin.value;
      const r: any = await palletApi.releaseInbound(pallet.value.trim(), bypass);
      if (r?.adminBypass)
        setMsg(
          `⚠ 관리자 우회 입고 완료. (미스캔 ${r.bypassedBoxCount}박스 자동 처리) host=${r.hostOrderKey}`,
        );
      else setMsg(`입고 가능 처리 완료. host=${r.hostOrderKey}`);
      await data.softReload(pallet.value.trim());
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
      nextTick(() => boxRef.value?.focus?.());
    } finally {
      busy.value = false;
    }
  }
  _releaseInboundImpl = releaseInbound;

  function onReleaseClick() {
    if (!scan.scanComplete.value && isAdmin.value) {
      createConfirm({
        iconType: 'warning',
        title: () => '관리자 우회',
        content: () =>
          `전수 스캔 미완료 상태로 입고 처리합니다 (${scan.pi.value.picked}/${scan.pi.value.expected})`,
        onOk: () => releaseInbound(),
      });
      return;
    }
    releaseInbound();
  }

  async function finalizeOutbound() {
    busy.value = true;
    try {
      const bypass = !scan.outboundComplete.value && isAdmin.value;
      const r: any = await palletApi.finalizeOutbound(pallet.value.trim(), bypass);
      if (r?.adminBypass)
        setMsg(
          `⚠ 관리자 우회 출고 확정. (미스캔 ${r.bypassedBoxCount}박스 / ${r.bypassedQty} 수량 소진)`,
        );
      else setMsg(`${r?.userMessage || '출고 확정됨.'}`);
      await data.softReload(pallet.value.trim());
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
      nextTick(() => boxRef.value?.focus?.());
    } finally {
      busy.value = false;
    }
  }
  const canBypassOutbound = computed(
    () =>
      isAdmin.value &&
      scan.isOutboundArrived.value &&
      !scan.outboundComplete.value &&
      !scan.hasPartialPicked.value,
  );
  const canShowFinalizeBtn = computed(
    () => scan.isOutboundArrived.value && (scan.outboundComplete.value || canBypassOutbound.value),
  );
  const finalizeTip = computed(() => {
    if (!can('update')) return '권한 없음';
    if (!scan.isOutboundArrived.value) return 'ECS 운송 완료(ARRIVED) 대기 중';
    if (scan.outboundComplete.value) return '';
    if (!isAdmin.value)
      return `전수 스캔 미완료 (${scan.pi.value.picked}/${scan.pi.value.expected})`;
    if (scan.hasPartialPicked.value)
      return `부분 출고 박스(${scan.partialPickedCount.value}개) 있음 — 전체 출고만 우회 가능`;
    return '관리자 우회 — 클릭 시 확인';
  });
  function onFinalizeClick() {
    if (!scan.outboundComplete.value && canBypassOutbound.value) {
      createConfirm({
        iconType: 'warning',
        title: () => '관리자 우회 — 출고 확정',
        content: () =>
          `전수 스캔 미완료 상태로 출고를 확정합니다. (${scan.pi.value.picked}/${scan.pi.value.expected})\n미스캔 박스는 모두 소진(DEPLETED) 처리됩니다.`,
        onOk: () => finalizeOutbound(),
      });
      return;
    }
    finalizeOutbound();
  }

  function doReinbound() {
    createConfirm({
      iconType: 'warning',
      title: () => '재입고',
      content: () => '시험 채취된 박스 잔량 기준으로 재입고를 등록합니다.',
      onOk: async () => {
        busy.value = true;
        try {
          const r: any = await palletApi.reinbound(pallet.value.trim());
          setMsg(r?.userMessage || `재입고 등록됨. inbound=${r?.inboundOrderKey || ''}`);
          await reloadAll();
        } catch (e: any) {
          setMsg(parseApiError(e), 'err');
        } finally {
          busy.value = false;
        }
      },
    });
  }
  function cancelReinboundReservation() {
    if (!act.value?.reinboundOrderKey) return;
    createConfirm({
      iconType: 'warning',
      title: () => '재입고 예약 취소',
      content: () => '예약된 재입고를 취소합니다.',
      onOk: async () => {
        busy.value = true;
        try {
          const r: any = await palletApi.cancelReinbound(act.value.reinboundOrderKey);
          setMsg(r?.userMessage || '재입고 예약 취소 완료.');
          await reloadAll();
        } catch (e: any) {
          setMsg(parseApiError(e), 'err');
        } finally {
          busy.value = false;
        }
      },
    });
  }
  function doRemainderReinbound() {
    createConfirm({
      iconType: 'info',
      title: () => '재입고 동기화',
      content: () =>
        '사전 발급된 잔여 재입고의 수량을 박스 현재 잔량으로 동기화합니다. (사전 발급이 없으면 신규 생성)',
      onOk: async () => {
        busy.value = true;
        try {
          const r: any = await palletApi.remainderReinbound(pallet.value.trim());
          setMsg(r?.userMessage || `재입고 동기화 완료. inbound=${r?.inboundOrderKey || ''}`);
          await reloadAll();
        } catch (e: any) {
          setMsg(parseApiError(e), 'err');
        } finally {
          busy.value = false;
        }
      },
    });
  }

  // ─── 파렛트 상태 라벨 / 안내 ─────
  const _stateView = computed(() =>
    info.value
      ? derivePalletStateLabel(act.value, lifecycle.value, scan.percent.value)
      : { label: '', cls: '' },
  );
  const stateLabel = computed(() => _stateView.value.label);
  const stateClass = computed(() => _stateView.value.cls);
  const reinboundNotice = computed(() => {
    if (!info.value || !act.value) return null;
    if (act.value.mode === 'POST_OUTBOUND') {
      if (act.value.fullyShipped)
        return {
          cls: 'notice-shipped',
          icon: '✅',
          title: '완전 출고 완료',
          msg: '파렛트를 반출하셔도 됩니다.',
        };
      if (act.value.requiresReinbound) {
        return {
          cls: 'notice-reinbound',
          icon: '⚠',
          title: '재입고 필요',
          msg: `이 파렛트는 잔량(${act.value.remainingQty})이 남아 있어 재입고가 필요합니다. [재동기화] 로 재입고를 진행하세요.`,
          actionLabel: '재동기화',
          actionType: 'primary',
          action: doRemainderReinbound,
        };
      }
    }
    if (act.value.mode === 'INBOUND' && act.value.requiresReinbound) {
      return {
        cls: 'notice-reinbound',
        icon: '⚠',
        title: '재입고 진행 중',
        msg: '부분 출고로 발생한 자동 재입고입니다. 박스를 스캔해 입고 절차를 마쳐 주세요.',
      };
    }
    return null;
  });
  const canRelease = computed(
    () =>
      can('update') &&
      !editing.hasUnsavedChanges.value &&
      (scan.scanComplete.value || isAdmin.value),
  );
  const releaseTip = computed(() => {
    if (!can('update')) return '권한 없음';
    if (!scan.scanComplete.value && !isAdmin.value)
      return `전수 스캔 미완료 (${scan.pi.value.picked}/${scan.pi.value.expected})`;
    if (!scan.scanComplete.value && isAdmin.value) return '관리자 우회 — 클릭 시 확인';
    return '';
  });
  const showSampleOutProgress = computed(() => {
    if (act.value?.mode !== 'OUTBOUND') return false;
    return !!act.value.autoFinalize;
  });
  const canDoSampleReinbound = computed(() => act.value?.mode === 'PENDING_SAMPLE');
  const hasReinboundReservation = computed(
    () => act.value?.mode === 'OUTBOUND' && !!act.value.reinboundOrderKey,
  );
  const reinboundHint = computed(() => {
    if (!act.value) return '시험 출고 완료 후 채취 작업이 끝나면 누르세요.';
    if (act.value.mode === 'PENDING_SAMPLE') return '시험 출고 완료 — 재입고 가능 상태입니다.';
    return '시험을 위한 출고에서만 사용합니다.';
  });

  // ─── 작업 단계 게이트 (영역 A/StepCard 호환용) ─────
  type WorkStep = 'finalize' | 'print' | 'scan' | 'progress';
  const finalizeGateOpen = computed(() => !act.value && editing.hasUnfinalizedBoxes.value);
  const workStep = computed<WorkStep>(() => {
    if (!info.value) return 'finalize';
    if (act.value) return 'progress';
    if (editing.hasUnfinalizedBoxes.value) return 'finalize';
    const hasUnprinted = boxes.value.some(
      (b: any) =>
        b.box_status !== BoxStatus.VOID.code && Number(b.print_count ?? b.printCount ?? 0) === 0,
    );
    if (hasUnprinted) return 'print';
    return 'scan';
  });
  const scanPanelDisabled = computed(() => finalizeGateOpen.value);
  const finalizeGateTip = computed(() => {
    if (!can('update')) return '권한 없음';
    if (editing.hasUnsavedChanges.value) return '미저장 변경사항을 먼저 저장하세요';
    return '박스 일련번호(box_seq)를 부여합니다';
  });

  // ─── 다음 데이터 로드 / 초기화 ─────
  async function load() {
    const code = pallet.value.trim();
    if (!code) return;
    loading.value = true;
    setMsg('');
    scan.lastScannedBoxId.value = null;
    scan.lastPartial.value = null;
    clearSelection();
    try {
      await data.load(code);
      loadHostItemExpected().catch(() => {});
      await nextTick();
      const hasPrintedBox = boxes.value.some((b: any) => b.box_status >= BoxStatus.PRINTED.code);
      if (hasPrintedBox && (scan.showInboundScan.value || scan.showOutboundScan.value))
        boxRef.value?.focus?.();
      else palletRef.value?.focus?.();
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
      nextTick(() => palletRef.value?.focus?.());
    } finally {
      loading.value = false;
    }
  }

  function reset() {
    data.resetData();
    historyModal.open = false;
    historyModal.highlightOrderKey = '';
    pallet.value = '';
    box.value = '';
    setMsg('');
    scan.lastPartial.value = null;
    scan.lastScannedBoxId.value = null;
    hostItemExpected.value = {};
    boxFilter.value = 'all';
    clearSelection();
    actions.closeActionMenu();
    editing.discardDraft();
    nextTick(() => palletRef.value?.focus?.());
  }

  // ─── 메뉴 액션 ─────
  function runMenu(
    action: 'editTotal' | 'cancelEdit' | 'void' | 'adjust' | 'sample' | 'partial' | 'printBoxLabel',
  ) {
    const b = actions.actionMenu.target;
    if (!b) return;
    actions.closeActionMenu();
    if (action === 'editTotal') actions.openEditTotal(b);
    else if (action === 'cancelEdit') editing.cancelDraftEdit(b.id);
    else if (action === 'void') actions.confirmVoidPending(b);
    else if (action === 'adjust') actions.openAdjust(b);
    else if (action === 'sample') actions.openSampleTaken(b);
    else if (action === 'partial') actions.openPartialOutbound(b);
    else if (action === 'printBoxLabel') labelPrint.printBoxLabel(b);
  }

  // ─── ActionId → 핸들러 라우팅 ─────
  const actionHandlers: Partial<Record<ActionId, () => void | Promise<void>>> = {
    finalizeBoxes: editing.finalizePallet,
    releaseInbound: onReleaseClick,
    finalizeOutbound: onFinalizeClick,
    doReinbound,
    doRemainderReinbound,
    cancelReinbound: cancelReinboundReservation,
    printPallet: labelPrint.printPalletLabel,
    printAllBoxes: labelPrint.printAllBoxLabels,
    printSelectedBoxes: labelPrint.printSelectedBoxLabels,
    markPalletPrinted: labelPrint.markPalletPrinted,
    saveDraft: editing.saveDraft,
    cancelDraft: editing.confirmDiscardDraft,
    reset,
    openHistory: () => openHistoryModal(),
    autoReleaseToggle: () => {
      autoRelease.value = !autoRelease.value;
    },
    confirmSampleScan: finalizeModalApi.openFinalizeModalSample,
    adminBypassRelease: finalizeModalApi.openFinalizeModalAdminRelease,
    adminBypassFinalize: finalizeModalApi.openFinalizeModalAdminFinalize,
    adminBypassSample: finalizeModalApi.openFinalizeModalAdminSample,
  };
  function runAction(id: ActionId) {
    const handler = actionHandlers[id];
    if (!handler) {
      notification.warning({
        message: '액션 실행 실패',
        description: `핸들러를 찾을 수 없습니다 (${id}).`,
      });
      return;
    }
    try {
      const result = handler();
      if (result instanceof Promise) {
        result.catch((e: any) => {
          notification.error({ message: '액션 오류', description: parseApiError(e) });
        });
      }
    } catch (e: any) {
      notification.error({ message: '액션 오류', description: parseApiError(e) });
    }
  }

  // ─── 셔틀 진행 polling (shuttleStages 가 있는 step 만) ─────
  let shuttlePollTimer: number | null = null;
  function shouldPollShuttle(s: Scenario, idx: number): boolean {
    if (s === 'INBOUND_NORMAL' && idx === 3) return true;
    if (s === 'OUTBOUND_NORMAL' && idx === 0) return true;
    if (s === 'OUTBOUND_PARTIAL_OUT' && idx === 0) return true;
    if (s === 'SAMPLE_CYCLE' && idx === 0) return true;
    if (s === 'INBOUND_REINBOUND' && idx === 0) return true;
    return false;
  }
  watch(
    [scenarioApi.scenario, scenarioApi.stepIndex],
    ([sc, idx]) => {
      const need = shouldPollShuttle(sc as Scenario, idx as number);
      if (need && shuttlePollTimer == null) {
        shuttlePollTimer = window.setInterval(() => {
          if (pallet.value.trim()) data.softReload(pallet.value.trim());
        }, 5000);
      } else if (!need && shuttlePollTimer != null) {
        window.clearInterval(shuttlePollTimer);
        shuttlePollTimer = null;
      }
    },
    { immediate: true },
  );

  // ─── 글로벌 키보드 / 모달 body lock ─────
  function onGlobalKeydown(e: KeyboardEvent) {
    if (
      e.key === '/' &&
      !actions.qtyModal.open &&
      !actions.addBoxModal.open &&
      !actions.actionMenu.open &&
      !labelPrint.labelReissueModal.open
    ) {
      const t = e.target as HTMLElement;
      if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA' || t.isContentEditable)) return;
      e.preventDefault();
      palletRef.value?.focus?.();
      return;
    }
    if (e.key !== 'Escape') return;
    if (actions.actionMenu.open) {
      actions.closeActionMenu();
      return;
    }
    if (actions.qtyModal.open && !actions.qtyModal.busy) actions.closeQtyModal();
    else if (actions.addBoxModal.open && !actions.addBoxModal.busy) actions.closeAddBoxModal();
    else if (labelPrint.labelReissueModal.open && !labelPrint.labelReissueModal.busy)
      labelPrint.closeLabelReissueModal();
  }
  onMounted(() => {
    window.addEventListener('keydown', onGlobalKeydown);
  });
  onUnmounted(() => {
    window.removeEventListener('keydown', onGlobalKeydown);
    window.removeEventListener('mousemove', onColResizeMove);
    window.removeEventListener('mouseup', onColResizeUp);
    document.body.style.overflow = '';
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
    if (shuttlePollTimer != null) {
      window.clearInterval(shuttlePollTimer);
      shuttlePollTimer = null;
    }
  });

  // 모달 열림 시 body 스크롤 잠금. 셋 중 하나라도 열려 있으면 잠금 유지.
  watch(
    () => [actions.qtyModal.open, actions.addBoxModal.open, labelPrint.labelReissueModal.open],
    ([qty, add, label]) => {
      if (qty || add || label) document.body.style.overflow = 'hidden';
      else document.body.style.overflow = '';
      if (qty) {
        nextTick(() => {
          qtyInputRef.value?.focus?.();
          qtyInputRef.value?.select?.();
        });
      }
    },
  );

  // ─── 반환 — SFC가 destructure 하는 시그니처 유지 ─────
  return {
    palletRef,
    boxRef,
    qtyInputRef,
    boxTableWrapRef,

    pallet,
    box,
    info,
    boxes,
    act,
    inboundProgress,
    lifecycle,

    historyModal,
    openHistoryModal,
    closeHistoryModal,

    autoRelease,
    loading,
    busy,
    printing,
    msg,
    msgKind,
    msgClass,
    boxFilter,

    labelReissueModal: labelPrint.labelReissueModal,

    selectedBoxIds,
    selectableIds,
    allSelected,
    someSelected,

    columns,
    tableTotalWidth,
    resizingColKey,

    lastPartial: scan.lastPartial,
    lastScannedBoxId: scan.lastScannedBoxId,

    actionMenu: actions.actionMenu,

    qtyModal: actions.qtyModal,
    addBoxModal: actions.addBoxModal,
    draftEdits: editing.draftEdits,
    draftDeletions: editing.draftDeletions,
    draftAdditions: editing.draftAdditions,
    savingDraft: editing.savingDraft,
    finalizingBoxes: editing.finalizingBoxes,

    displayBoxes,
    filteredDisplayBoxes,
    filterCounts,
    hasUnsavedChanges: editing.hasUnsavedChanges,
    hasUnfinalizedBoxes: editing.hasUnfinalizedBoxes,
    totalDraftCount: editing.totalDraftCount,
    itemLotSummary,
    pi: scan.pi,
    percent: scan.percent,
    canAddOrDeleteBox,
    deletableSelectedCount,
    bulkDeleteSelected,
    canPartialOutbound: scan.canPartialOutbound,
    isSampleCycle: computed(() => {
      if (!act.value) return false;
      if (act.value.mode === 'PENDING_SAMPLE') return true;
      if (act.value.sampleFlow) return true;
      if (act.value.inSampleCycle) return true;
      return false;
    }),
    showInboundScan: scan.showInboundScan,
    showInboundProgress: scan.showInboundProgress,
    showOutboundScan: scan.showOutboundScan,
    showOutboundWaiting: scan.showOutboundWaiting,
    isOutboundArrived: scan.isOutboundArrived,
    showSampleOutProgress,
    canDoSampleReinbound,
    hasReinboundReservation,
    reinboundHint,
    hasPartialPicked: scan.hasPartialPicked,
    partialPickedCount: scan.partialPickedCount,
    stateLabel,
    stateClass,
    reinboundNotice,
    scanComplete: scan.scanComplete,
    canRelease,
    releaseTip,
    outboundComplete: scan.outboundComplete,
    canShowFinalizeBtn,
    finalizeTip,

    workStep,
    unfinalizedCount: editing.unfinalizedCount,
    finalizeGateOpen,
    scanPanelDisabled,
    finalizeGateTip,

    can,
    isAdmin,

    load,
    reloadAll,
    refreshBoxesOnly: () => data.refreshBoxesOnly(pallet.value.trim()),
    reset,
    scan: scan.scan,
    releaseInbound,
    finalizeOutbound,
    doReinbound,
    cancelReinboundReservation,
    doRemainderReinbound,
    onReleaseClick,
    onFinalizeClick,
    setMsg,
    highlightScannedBox: scan.highlightScannedBox,
    toggleSelectRow,
    clearSelection,
    toggleSelectAll,
    startColResize,
    onColResizeMove,
    onColResizeUp,
    autoFitColumn,
    resetColumnWidths,
    toggleActionMenu: actions.toggleActionMenu,
    closeActionMenu: actions.closeActionMenu,
    runMenu,
    closeQtyModal: actions.closeQtyModal,
    closeLabelReissueModal: labelPrint.closeLabelReissueModal,
    confirmQtyModal: actions.confirmQtyModal,
    openAdjust: actions.openAdjust,
    openSampleTaken: actions.openSampleTaken,
    openPartialOutbound: actions.openPartialOutbound,
    openEditTotal: actions.openEditTotal,
    confirmVoidPending: actions.confirmVoidPending,
    openAddBox: actions.openAddBox,
    closeAddBoxModal: actions.closeAddBoxModal,
    confirmAddBox: actions.confirmAddBox,
    applyDraftEditTotal: editing.applyDraftEditTotal,
    cancelDraftEdit: editing.cancelDraftEdit,
    toggleDraftDeletion: editing.toggleDraftDeletion,
    addDraftAddition: editing.addDraftAddition,
    discardDraft: editing.discardDraft,
    confirmDiscardDraft: editing.confirmDiscardDraft,
    saveDraft: editing.saveDraft,
    finalizePallet: editing.finalizePallet,
    printPalletLabel: labelPrint.printPalletLabel,
    printAllBoxLabels: labelPrint.printAllBoxLabels,
    printBoxLabel: labelPrint.printBoxLabel,
    printSelectedBoxLabels: labelPrint.printSelectedBoxLabels,
    confirmLabelReissue: labelPrint.confirmLabelReissue,
    markPalletPrinted: labelPrint.markPalletPrinted,
    isFirstIssueBox: labelPrint.isFirstIssueBox,
    allFirstIssue: labelPrint.allFirstIssue,
    expectedSumOf,
    onQtyInputFocus: (e: FocusEvent) => (e.target as HTMLInputElement).select(),
    shuttleStatusLabel,
    boxStatusLabel,
    boxStatusClass,
    boxRowClass,
    canEditQty,
    canEditTotal,
    hasMoreActions,

    scenario: scenarioApi.scenario,
    stepIndex: scenarioApi.stepIndex,
    step: scenarioApi.step,
    stepTooltips: scenarioApi.stepTooltips,
    anyTaken: scan.anyTaken,
    auxDrawerOpen,
    historyDrawerOpen,
    headerMenuOpen,
    runAction,

    finalizeModal: finalizeModalApi.finalizeModal,
    confirmFinalizeModal: finalizeModalApi.confirmFinalizeModal,
    closeFinalizeModal: finalizeModalApi.closeFinalizeModal,
  };
}
