<!--vue-wcs-ui/src/views/kmat_2026/tspg-4way-shuttle/dashboard_2d/editor/MapEditor.vue-->
<template>
  <div class="map-editor-container">
    <div class="editor-main">
      <EditorTopBar
        :eq-groups="store.eqGroups"
        :active-eq-group-id="store.selectedEqGroupId"
        :pages="store.pages"
        :active-page-id="store.activePageId"
        :lc-id="store.lcId"
        :is-default-mode="store.isDefaultMode"
        :can-save="perm.canSave.value"
        :can-manage-pages="perm.canManagePages.value"
        :rack-cell-edit-mode="rackCellEditMode"
        @select-eq-group="handleSelectEqGroup"
        @select-page="handleSelectPage"
        @add-floor="handleAddFloor"
        @remove-page="handleRemovePage"
        @update-page-name="handleUpdatePageName"
        @copy-floor="handleCopyFloor"
        @save="handleSave"
        @undo="handleUndo"
        @redo="handleRedo"
        @open-generator="showGeneratorPanel = true"
        @change-center="handleChangeCenter"
        @delete-eq-group="handleDeleteEqGroup"
        @toggle-rack-cell-edit="handleToggleRackCellEdit"
      />


      <!-- 설비 생성 패널 (역매핑) -->
      <teleport to="body">
        <div
          v-if="showGeneratorPanel"
          class="generator-overlay"
          @click.self="showGeneratorPanel = false"
        >
          <EquipmentGeneratorPanel
            :lc-id="store.lcId"
            @close="showGeneratorPanel = false"
            @refresh="handleGeneratorRefresh"
          />
        </div>
      </teleport>

      <div class="editor-body">
        <EditorLeftPanel
          :equipment-types="store.equipmentTypes"
          :is-loading="store.isLoading"
          :is-default-mode="store.isDefaultMode"
          :can-manage-types="perm.canManageTypes.value"
          @add-equipment-type="handleAddEquipmentType"
          @initialize-types="handleInitializeTypes"
          @init-from-assets="handleInitFromAssets"
          @sync-from-master="handleSyncFromMaster"
          @update-type="handleUpdateType"
          @delete-type="handleDeleteType"
        />

        <EditorCenterPanel
          :page="store.activePage"
          :objects="store.layouts"
          :selected-object-id="store.selectedObjectId"
          :equipment-type-map="store.equipmentTypeMap"
          :rack-cell-edit-mode="rackCellEditMode"
          :rack-cells-draft="rackDraft.cells"
          :rack-cell-rejected-ids="rejectedIds"
          :rack-cell-rejected-reasons="rejectedReasons"
          @add-object="handleAddObject"
          @update-object="handleUpdateObject"
          @select-object="handleSelectObject"
          @selection-changed="handleSelectionChanged"
          @update-canvas="handleUpdateCanvas"
          @delete-objects="handleDeleteObjects"
          @save="handleSave"
          @undo="handleUndo"
          @redo="handleRedo"
          @rack-cell-drag-end="handleCellDragEnd"
        />

        <EditorRightPanel
          :selected-object="store.selectedObject"
          :selected-object-ids="store.selectedObjectIds"
          :objects="store.layouts"
          :page="store.activePage"
          @select-object="handleSelectObject"
          @update-property="handleUpdateProperty"
          @update-properties="handleUpdateProperties"
          @bulk-resize="handleBulkResize"
          @delete-object="handleDeleteObject"
          @update-page-name="handleUpdatePageName"
          @update-page-canvas="handleUpdateCanvas"
        />
      </div>
    </div>
  </div>
</template>


<script setup lang="ts">
  import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';
  import { useShuttleStore } from '../store/shuttleStore';
  import { buildUUID } from '/@/utils/uuid';
  import type { TbEcs2dItem, TbEcs2dItemType } from '../api/types';
  import { equipmentTypeApi } from '../api/shuttle';

  import EditorTopBar from './EditorTopBar.vue';
  import EditorLeftPanel from './EditorLeftPanel.vue';
  import EditorCenterPanel from './EditorCenterPanel.vue';
  import EditorRightPanel from './EditorRightPanel.vue';
  import EquipmentGeneratorPanel from './EquipmentGeneratorPanel.vue';
  import { useEditorPermissions } from './composables/useEditorPermissions';
  import { useRackCellDraft, type CellDraft } from './composables/useRackCellDraft';
  import { generatorApi, type CellOp } from '../api/generatorApi';

  const store = useShuttleStore();
  const perm = useEditorPermissions();
  const showGeneratorPanel = ref(false);

  // 랙 셀 편집 모드
  const rackCellEditMode = ref(false);
  const rackDraft = useRackCellDraft();
  const rejectedIds = ref<string[]>([]);
  const rejectedReasons = ref<Record<string, string>>({});
  const editorRackContext = ref<{ eqGroupId: string; rackEqId: string; level: number } | null>(null);

  onUnmounted(() => store.reset());

  // 초기 레이아웃 로드
  const booting = ref(false);

  const bootstrapInitialLayouts = async () => {
    if (booting.value) return;
    booting.value = true;
    try {
      await nextTick();
      if (store.pages.length > 0) {
        const firstPage = store.pages[0];
        store.selectedEqGroupId = firstPage.eqGroupId || '';
        if (!store.activePageId) {
          store.selectPage(firstPage.id);
          await nextTick();
        }
      }
      if (store.activePageId) {
        await store.loadLayouts();
      }
    } finally {
      booting.value = false;
    }
  };

  // 진입 시 DEFAULT 모드로 자동 초기화
  onMounted(async () => {
    await store.initializeLcOnly('DEFAULT');
    await bootstrapInitialLayouts();
  });

  const loadedPageIds = ref(new Set<string>());

  watch(
    () => store.activePageId,
    async (pageId) => {
      if (!pageId) return;
      if (booting.value) return;
      if (loadedPageIds.value.has(String(pageId))) return;
      await store.loadLayouts();
      loadedPageIds.value.add(String(pageId));
    },
    { immediate: false },
  );

  // LC 바뀌면 로드 기록 초기화
  watch(
    () => store.lcId,
    () => loadedPageIds.value.clear(),
  );

  // 센터 변경 핸들러 (EditorTopBar에서 emit)
  const handleChangeCenter = async (lcId: string) => {
    await store.initializeLcOnly(lcId || 'DEFAULT');
    loadedPageIds.value.clear();
    await bootstrapInitialLayouts();
  };

  const handleSelectEqGroup = async (eqGroupId: string) => {
    // shuttle_layout_pages그룹 선택 (store에서 해당 그룹의 첫 페이지도 자동 선택)
    store.selectEqGroup(eqGroupId);
    await nextTick();

    // 레이아웃 로드
    if (store.activePageId) {
      await store.loadLayouts();
    }
  };

  const handleSelectPage = async (pageId: string) => {
    store.selectPage(pageId);
    await nextTick();
    await store.loadLayouts();
  };

  const handleAddFloor = async () => {
    const newFloor = await store.createFloorAuto();
    alert(`새로운 ${newFloor.floorLevel || ''}층이 추가되었습니다!`);
    store.selectPage(newFloor.id);
    await nextTick();
    await store.loadLayouts();
  };

  const handleRemovePage = async (pageId: string) => {
    await store.deletePage(pageId);
    if (store.activePageId) await store.loadLayouts();
  };

  /**
   * 그룹 탭 × 클릭: 해당 eq_group 의 페이지들을 모두 삭제하고 eq_group 자체 삭제.
   *
   * 주의: 이 그룹에 매핑된 tb_eq_mst (설비)가 남아있으면 백엔드 deleteEqGroup 이
   *       IllegalStateException 으로 거부한다. 그 경우 사용자가 '설비 생성' 패널에서
   *       먼저 해당 그룹의 설비를 정리한 뒤 재시도해야 한다.
   */
  async function handleDeleteEqGroup(eqGroupId: string) {
    if (!eqGroupId) return;
    const groupPages = store.pages.filter((p: any) => (p.eqGroupId || '') === eqGroupId);

    const msg = [
      `이 설비 그룹과 그 안의 모든 페이지를 삭제하시겠습니까?`,
      ``,
      `  - 페이지: ${groupPages.length}개`,
      ``,
      `※ 그룹에 매핑된 설비(tb_eq_mst)가 남아있으면 그룹 삭제는 거부됩니다.`,
      `   설비 정리 후 재시도하세요.`,
    ].join('\n');
    if (!confirm(msg)) return;

    try {
      // 1) 페이지 삭제 (백엔드 cascade로 tb_ecs_2d_item 정리)
      for (const p of groupPages) {
        try {
          await store.deletePage(p.id);
        } catch (e: any) {
          console.warn(`[deleteEqGroup] deletePage(${p.id}) failed:`, e?.message || e);
        }
      }

      // 2) eq_group 자체 삭제 (하위 tb_eq_mst 없을 때만 성공)
      const { generatorApi } = await import('../api/generatorApi');
      await generatorApi.deleteEqGroup(eqGroupId);

      // 3) 로컬 상태 갱신
      if (typeof (store as any).loadEqGroups === 'function') {
        await (store as any).loadEqGroups();
      }
      if (typeof (store as any).loadPagesByLcId === 'function') {
        await (store as any).loadPagesByLcId();
      }
      if (store.selectedEqGroupId === eqGroupId) {
        const next = store.eqGroups[0];
        store.selectedEqGroupId = next ? next.id : '';
      }
      alert('설비 그룹이 삭제되었습니다.');
    } catch (e: any) {
      const reason = e?.response?.data?.message || e?.message || e;
      alert(`설비 그룹 삭제 실패: ${reason}\n\n그룹에 속한 설비(tb_eq_mst)가 있는지 확인 후 정리하고 다시 시도해주세요.`);
    }
  }

  const handleUpdatePageName = async ({ id, pageName }: { id: string; pageName: string }) => {
    await store.updatePageName(id, pageName);
  };

  const handleCopyFloor = async (payload: { sourcePageId: string; newFloorLevel: number }) => {
    const newPage = await store.copyPage(
      payload.sourcePageId,
      `${payload.newFloorLevel}층`,
      payload.newFloorLevel,
    );
    store.selectPage(newPage.id);
    await nextTick();
    await store.loadLayouts();
    alert('층이 복사되었습니다!');
  };

  const handleUpdateCanvas = async (width: number, height: number, backgroundColor: string) => {
    if (store.activePageId) {
      await store.updatePageCanvas(store.activePageId, width, height, backgroundColor);
    }
  };

  const handleSave = async () => {
    if (!perm.canSave.value) {
      alert('저장 권한이 없습니다.');
      return;
    }

    // 1) 랙 셀 동기화 먼저 — dirty 시에만 호출
    if (rackDraft.dirty.value && editorRackContext.value) {
      const ops: CellOp[] = rackDraft.dirtyOps();
      try {
        const res = await generatorApi.syncRackCells({
          eqGroupId: editorRackContext.value.eqGroupId,
          rackEqId: editorRackContext.value.rackEqId,
          ops,
        });
        rejectedIds.value = res.rejectedCellIds || [];
        rejectedReasons.value = {};
        (res.rejectedCellIds || []).forEach((id, i) => {
          rejectedReasons.value[id] = res.rejectReasons?.[i] || '거절';
        });
        rackDraft.commit(rejectedIds.value);
        if (rejectedIds.value.length > 0) {
          alert(
            `일부 셀이 거절되었습니다 (${rejectedIds.value.length}개):\n` +
              rejectedIds.value
                .map((id) => `${id} — ${rejectedReasons.value[id]}`)
                .join('\n'),
          );
          return; // 거절 있으면 2D 아이템 저장 중단
        }
      } catch (e: any) {
        alert(`랙 셀 동기화 실패: ${e?.message || e}`);
        return;
      }
    }

    // 2) 기존 2D 아이템 저장
    await store.saveAllLayouts();
    alert('저장되었습니다!');
  };

  // ============================================
  // 랙 셀 편집 모드
  // ============================================

  // EcsDBConsts.EqType.RACK = 11
  const EQ_TYPE_RACK = 11;

  async function resolveRackEqId(eqGroupId: string): Promise<string | null> {
    // 그룹 내 RACK 장비 조회 (현재 페이지의 2D 아이템 realEqId는 셀 ID라 직접 사용 불가)
    const eqs = await generatorApi.getEqMstByGroup(eqGroupId);
    const racks = (eqs || []).filter((e: any) => e.type === EQ_TYPE_RACK);
    if (racks.length === 0) return null;
    if (racks.length === 1) return racks[0].id;
    // 여러 RACK이면 첫 번째 사용 + 안내. 정교화 필요 시 선택 다이얼로그 추가
    console.warn('[MapEditor] 그룹에 RACK이 2개 이상. 첫 번째 사용:', racks.map((r: any) => r.id));
    return racks[0].id;
  }

  async function handleToggleRackCellEdit() {
    if (rackCellEditMode.value) {
      if (
        rackDraft.dirty.value &&
        !confirm('저장하지 않은 셀 변경이 있습니다. 변경을 버리시겠습니까?')
      ) {
        return;
      }
      rackDraft.reset();
      rejectedIds.value = [];
      rejectedReasons.value = {};
      editorRackContext.value = null;
      rackCellEditMode.value = false;
      return;
    }

    const eqGroupId = store.selectedEqGroupId || (store.activePage as any)?.eqGroupId;
    if (!eqGroupId) {
      alert('활성 설비 그룹을 확인할 수 없습니다.');
      return;
    }
    const rackEqId = await resolveRackEqId(eqGroupId);
    if (!rackEqId) {
      alert(`그룹 ${eqGroupId}에 RACK 설비가 없습니다. 먼저 Generator로 랙을 생성하세요.`);
      return;
    }
    const pageLevel = (store.activePage as any)?.floorLevel || 1;

    try {
      const cells = await generatorApi.getRackCellsByLevel(eqGroupId, rackEqId, pageLevel);
      if (!cells || cells.length === 0) {
        alert(`해당 랙(${rackEqId}) L${pageLevel}에 셀이 없습니다.`);
        return;
      }
      editorRackContext.value = { eqGroupId, rackEqId, level: pageLevel };

      const drafts: CellDraft[] = cells.map((c: any) => ({
        cellId: c.rackId,
        level: c.level,
        row: c.row,
        bay: c.bay,
        rackType: c.type,
        state: c.useYn ? 'active' : 'disabled',
        origin: c.useYn ? 'persisted-active' : 'persisted-disabled',
      }));
      rackDraft.hydrate(drafts);
      rejectedIds.value = [];
      rejectedReasons.value = {};
      rackCellEditMode.value = true;
    } catch (e: any) {
      alert(`랙 셀 로드 실패: ${e?.message || e}`);
    }
  }

  function handleCellDragEnd(cellIds: string[]) {
    if (cellIds.length === 0) return;
    // 새 토글이 시작되면 이전 거절 표시는 지움
    if (rejectedIds.value.length > 0) {
      rejectedIds.value = [];
      rejectedReasons.value = {};
    }
    rackDraft.toggleBatch(cellIds);
  }

  const handleUndo = () => store.undo();
  const handleRedo = () => store.redo();

  const handleInitializeTypes = async () => {
    await store.initializeDefaultTypes();
  };

  // 로컬 SVG 에셋으로 DEFAULT 마스터 일괄 등록
  const handleInitFromAssets = async () => {
    try {
      await store.initMasterFromLocalAssets();
      alert('기본 설비 에셋이 등록되었습니다.');
    } catch (e: any) {
      alert('등록 실패: ' + (e?.message || e));
    }
  };

  // DEFAULT 마스터 → 현재 센터 동기화
  const handleSyncFromMaster = async () => {
    try {
      await store.syncMasterToCenter();
      alert('마스터 설비가 현재 센터로 동기화되었습니다.');
    } catch (e: any) {
      alert('동기화 실패: ' + (e?.message || e));
    }
  };

  // 개별 설비 타입 수정
  const handleUpdateType = async (type: TbEcs2dItemType) => {
    try {
      await equipmentTypeApi.updateType(type);
      await store.loadEquipmentTypes();
    } catch (e: any) {
      alert('수정 실패: ' + (e?.message || e));
    }
  };

  // 개별 설비 타입 삭제
  const handleDeleteType = async (id: string) => {
    try {
      await equipmentTypeApi.deleteType(id);
      await store.loadEquipmentTypes();
    } catch (e: any) {
      alert('삭제 실패: ' + (e?.message || e));
    }
  };

  const handleAddEquipmentType = (type: TbEcs2dItemType) => {
    if (!store.activePage) return;

    const newObject: TbEcs2dItem = {
      id: buildUUID(),
      lcId: store.lcId,
      pageId: store.activePageId,
      equipmentCode: `${type.typeCode}_${Date.now()}`,
      equipmentTypeCode: type.typeCode,
      posX: 100,
      posY: 100,
      width: type.defaultWidth || 100,
      height: type.defaultHeight || 100,
      rotation: 0,
      scaleX: 1,
      scaleY: 1,
      flipH: false,
      flipV: false,
      zIndex: store.layouts.length,
      opacity: 1,
      showLabel: true,
      isVisible: true,
      isLocked: false,
    };

    store.addLayout(newObject);
    store.selectObject(newObject.id!);
  };

  const handleAddObject = (obj: TbEcs2dItem) => store.addLayout(obj);

  const handleUpdateObject = (id: string, updates: Partial<TbEcs2dItem>) =>
    store.updateLayout(id, updates);

  const handleSelectObject = (id: string | null) => store.selectObject(id);

  const handleSelectionChanged = (payload: { ids: string[]; primaryId: string | null }) => {
    store.setSelection(payload.ids, payload.primaryId);
  };

  const BULK_SAFE_PROPS = new Set([
    'isLocked',
    'isVisible',
    'showLabel',
    'opacity',
    'rotation',
    'scaleX',
    'scaleY',
    'flipH',
    'flipV',
    'customColor',
  ]);

  const handleUpdateProperties = (ids: string[], property: string, value: any) => {
    if (!BULK_SAFE_PROPS.has(property)) return;
    ids.forEach((id) => store.updateLayout(id, { [property]: value }));
  };

  const handleUpdateProperty = (property: string, value: any) => {
    if (store.selectedObjectId) store.updateLayout(store.selectedObjectId, { [property]: value });
  };

  function handleBulkResize(ids: string[], updates: { width?: number; height?: number }) {
    if (!ids || ids.length === 0) return;
    for (const id of ids) {
      store.updateLayout(id, updates);
    }
  }

  function handleDeleteObjects(ids: string[] | string) {
    if (!perm.canDeleteEquipment.value) {
      alert('삭제 권한이 없습니다.');
      return;
    }
    const arr = Array.isArray(ids) ? ids : [ids];
    if (arr.length === 0) return;
    store.removeLayouts(arr);
    store.selectObject(null);
  }

  const handleDeleteObject = () => {
    if (store.selectedObjectId) handleDeleteObjects(store.selectedObjectId);
  };

  // 설비 생성 패널에서 생성 완료 후 새로고침
  const handleGeneratorRefresh = async () => {
    // 레이아웃 다시 로드
    if (store.activePageId) {
      await store.loadLayouts();
    }
    // 설비 그룹 목록 다시 로드
    await store.loadAllEqGroups();
  };
</script>

<style scoped>
  .map-editor-container {
    position: absolute;
    inset: 0;

    display: flex;
    flex-direction: column;

    overflow: hidden;
    background: #fff;
  }

  .editor-main {
    flex: 1;
    min-height: 0;
    height: 0;

    display: flex;
    flex-direction: column;

    overflow: hidden;
  }

  .editor-body {
    flex: 1;
    min-height: 0;
    height: 0;

    display: flex;
    align-items: stretch;

    overflow: hidden;
  }

  .generator-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 999;
    display: flex;
    align-items: center;
    justify-content: center;
  }
</style>
