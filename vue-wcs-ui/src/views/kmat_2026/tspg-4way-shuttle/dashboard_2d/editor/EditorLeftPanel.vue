<template>
  <div class="left-panel">
    <div class="panel-header">
      <h3>설비 팔레트</h3>
      <span v-if="isDefaultMode" class="panel-mode-badge">마스터</span>
    </div>

    <div class="panel-content">
      <div v-if="isLoading" class="loading-state">로딩 중...</div>

      <!-- DEFAULT 모드 Empty State -->
      <div v-else-if="equipmentTypes.length === 0 && isDefaultMode" class="empty-state">
        <p class="empty-title">설정된 기본 설비가 없습니다.</p>
        <p class="empty-desc">로컬 SVG 에셋을 기반으로<br />기본 설비를 일괄 등록합니다.</p>
        <button
          class="action-btn primary"
          :disabled="!props.canManageTypes"
          @click="$emit('init-from-assets')"
        >
          기본 설비 에셋 일괄 등록
        </button>
        <button
          class="action-btn secondary"
          style="margin-top: 8px"
          :disabled="!props.canManageTypes"
          @click="$emit('initialize-types')"
        >
          기본 설비만 등록 (아이콘 없이)
        </button>
      </div>

      <!-- 센터 모드 Empty State -->
      <div v-else-if="equipmentTypes.length === 0 && !isDefaultMode" class="empty-state">
        <p class="empty-title">이 센터에 등록된 설비가 없습니다.</p>
        <p class="empty-desc">시스템 마스터(DEFAULT)의 설비를<br />현재 센터로 복제합니다.</p>
        <button
          class="action-btn primary"
          :disabled="!props.canManageTypes"
          @click="$emit('sync-from-master')"
        >
          시스템 마스터에서 설정 동기화
        </button>
      </div>

      <div v-else class="equipment-scroll">
        <div class="equipment-list">
          <div
            v-for="type in equipmentTypes"
            :key="type.id"
            class="equipment-item"
            :class="{ 'edit-mode': editingTypeId === type.id }"
            draggable="true"
            @dragstart="handleDragStart($event, type)"
            @click="handleClick(type)"
          >
            <!-- 아이콘 -->
            <div class="item-icon" @click.stop>
              <!-- SVG XML 직접 렌더링 -->
              <div
                v-if="isSvgXml(type.iconData2d)"
                class="svg-render"
                v-html="type.iconData2d"
              ></div>
              <!-- Base64/URL 이미지 -->
              <img
                v-else-if="getTypeIconSrc(type)"
                :src="getTypeIconSrc(type)"
                :alt="type.typeName"
              />
              <!-- Placeholder -->
              <div v-else class="icon-placeholder">
                {{ (type.typeCode ?? '?').charAt(0) }}
              </div>
            </div>

            <!-- 설비 정보 -->
            <div class="item-info">
              <span class="item-name">{{ type.typeName }}</span>
              <span class="item-code">{{ type.typeCode }}</span>
              <span v-if="type.layerType" class="item-layer" :class="`layer-${type.layerType}`">
                {{ type.layerType }}
              </span>
            </div>

            <!-- 호버 액션 -->
            <div v-if="props.canManageTypes" class="item-actions" @click.stop>
              <button class="item-action-btn" title="편집" @click.stop="openEdit(type)">✎</button>
              <button class="item-action-btn danger" title="삭제" @click.stop="deleteType(type)"
                >✕</button
              >
            </div>
          </div>

          <div class="scroll-bottom-spacer"></div>
        </div>
      </div>
    </div>

    <!-- 편집 모달 -->
    <teleport to="body">
      <div v-if="editingType" class="edit-overlay" @click.self="closeEdit">
        <div class="edit-modal">
          <div class="edit-modal-header">
            <h4>설비 타입 편집</h4>
            <button class="close-btn" @click="closeEdit">✕</button>
          </div>

          <div class="edit-modal-body">
            <div class="edit-field">
              <label>타입 코드</label>
              <input type="text" :value="editForm.typeCode" readonly class="readonly-input" />
            </div>
            <div class="edit-field">
              <label>이름</label>
              <input v-model="editForm.typeName" type="text" class="edit-input" />
            </div>
            <div class="edit-field">
              <label>카테고리</label>
              <input
                v-model="editForm.category"
                type="text"
                class="edit-input"
                placeholder="이동설비, 보관설비..."
              />
            </div>
            <div class="edit-field">
              <label>레이어 타입</label>
              <select v-model="editForm.layerType" class="edit-select">
                <option value="">선택</option>
                <option value="static">static (정적)</option>
                <option value="dynamic">dynamic (동적/실시간)</option>
                <option value="overlay">overlay (구조물)</option>
              </select>
            </div>
            <div class="edit-field">
              <label>실운영 EqType 번호</label>
              <input
                v-model.number="editForm.realEqTypeNum"
                type="number"
                class="edit-input"
                placeholder="RACK=11, CV/LIFT=21, SHUTTLE=22"
              />
            </div>
            <div class="edit-field checkbox-field">
              <label>화물 연동</label>
              <input v-model="editForm.hasCargo" type="checkbox" />
            </div>
            <div class="edit-field checkbox-field">
              <label>재고 연동</label>
              <input v-model="editForm.hasInventory" type="checkbox" />
            </div>
            <div class="edit-field">
              <label>SVG 아이콘 업로드</label>
              <input type="file" accept=".svg" @change="onSvgFileChange" class="file-input" />
              <div
                v-if="editForm.iconData2d"
                class="svg-preview"
                v-html="editForm.iconData2d"
              ></div>
            </div>
          </div>

          <div class="edit-modal-footer">
            <button class="action-btn secondary" @click="closeEdit">취소</button>
            <button class="action-btn primary" @click="saveEdit">저장</button>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive } from 'vue';
  import type { TbEcs2dItemType } from '../api/types';
  import { resolveTypeIconUrl } from '../assets/iconUrl';

  const props = withDefaults(
    defineProps<{
      equipmentTypes: TbEcs2dItemType[];
      isLoading: boolean;
      isDefaultMode: boolean;
      canManageTypes?: boolean;
    }>(),
    {
      canManageTypes: true,
    },
  );

  const emit = defineEmits<{
    (e: 'add-equipment-type', type: TbEcs2dItemType): void;
    (e: 'initialize-types'): void;
    (e: 'init-from-assets'): void;
    (e: 'sync-from-master'): void;
    (e: 'update-type', type: TbEcs2dItemType): void;
    (e: 'delete-type', id: string): void;
  }>();

  // SVG XML 여부 판별 (< 로 시작하면 SVG 직접 렌더링)
  const isSvgXml = (data?: string | null) => {
    return !!data && data.trimStart().startsWith('<');
  };

  // 아이콘 src 결정 (data URL / URL)
  const getTypeIconSrc = (type: TbEcs2dItemType): string | null => {
    if (!type.iconData2d) return resolveTypeIconUrl(type.typeCode, type.iconFileName) || null;
    // data: 로 시작하면 img src로 사용
    if (type.iconData2d.startsWith('data:')) return type.iconData2d;
    return null;
  };

  const handleDragStart = (event: DragEvent, type: TbEcs2dItemType) => {
    if (!event.dataTransfer) return;
    event.dataTransfer.setData(
      'application/json',
      JSON.stringify({ dragType: 'equipmentType', data: type }),
    );
    event.dataTransfer.effectAllowed = 'copy';
  };

  const handleClick = (type: TbEcs2dItemType) => {
    if (editingTypeId.value) return; // 편집 중에는 클릭 추가 막기
    emit('add-equipment-type', type);
  };

  // ============================================
  // 편집 모달
  // ============================================
  const editingTypeId = ref<string | null>(null);
  const editingType = ref<TbEcs2dItemType | null>(null);
  const editForm = reactive<Partial<TbEcs2dItemType>>({});

  const openEdit = (type: TbEcs2dItemType) => {
    editingTypeId.value = type.id;
    editingType.value = type;
    Object.assign(editForm, {
      typeCode: type.typeCode,
      typeName: type.typeName,
      category: type.category ?? '',
      layerType: type.layerType ?? '',
      realEqTypeNum: type.realEqTypeNum ?? null,
      hasCargo: type.hasCargo ?? false,
      hasInventory: type.hasInventory ?? false,
      iconData2d: type.iconData2d ?? '',
    });
  };

  const closeEdit = () => {
    editingTypeId.value = null;
    editingType.value = null;
  };

  const onSvgFileChange = (event: Event) => {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
      editForm.iconData2d = e.target?.result as string;
    };
    reader.readAsText(file);
  };

  const saveEdit = () => {
    if (!editingType.value) return;
    const updated: TbEcs2dItemType = {
      ...editingType.value,
      typeName: editForm.typeName ?? editingType.value.typeName,
      category: editForm.category,
      layerType: editForm.layerType,
      realEqTypeNum: editForm.realEqTypeNum ?? null,
      hasCargo: editForm.hasCargo,
      hasInventory: editForm.hasInventory,
      iconData2d: editForm.iconData2d,
    };
    emit('update-type', updated);
    closeEdit();
  };

  const deleteType = (type: TbEcs2dItemType) => {
    if (!confirm(`'${type.typeName}' 설비 타입을 삭제하시겠습니까?`)) return;
    emit('delete-type', type.id);
  };
</script>

<style scoped>
  .left-panel,
  .left-panel * {
    box-sizing: border-box;
  }

  .left-panel {
    flex: 0 0 280px;
    width: 280px;
    min-width: 280px;
    height: 100%;
    min-height: 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    border-right: 1px solid #ebeef5;
  }

  .panel-header {
    padding: 12px 16px;
    border-bottom: 1px solid #ebeef5;
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .panel-header h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    flex: 1;
  }

  .panel-mode-badge {
    font-size: 11px;
    padding: 2px 7px;
    border-radius: 4px;
    background: #f0f9eb;
    color: #67c23a;
    border: 1px solid #b3e19d;
    font-weight: 600;
  }

  .panel-content {
    flex: 1;
    min-height: 0;
    height: 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 12px;
  }

  .loading-state {
    text-align: center;
    padding: 24px;
    color: #909399;
  }

  .empty-state {
    text-align: center;
    padding: 24px 16px;
    color: #909399;
  }

  .empty-title {
    font-size: 13px;
    font-weight: 600;
    color: #606266;
    margin: 0 0 8px;
  }

  .empty-desc {
    font-size: 12px;
    color: #909399;
    line-height: 1.6;
    margin: 0 0 16px;
  }

  .action-btn {
    width: 100%;
    padding: 9px 14px;
    border-radius: 6px;
    border: none;
    cursor: pointer;
    font-size: 13px;
    font-weight: 500;
  }

  .action-btn.primary {
    background: #409eff;
    color: #fff;
  }
  .action-btn.primary:hover {
    background: #66b1ff;
  }

  .action-btn.secondary {
    background: #f5f7fa;
    color: #606266;
    border: 1px solid #dcdfe6;
  }
  .action-btn.secondary:hover {
    background: #ecf5ff;
    border-color: #409eff;
    color: #409eff;
  }

  .equipment-scroll {
    flex: 1;
    min-height: 0;
    height: 0;
    overflow-y: auto;
    padding-right: 4px;
    overscroll-behavior: contain;
    -webkit-overflow-scrolling: touch;
  }

  .equipment-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .scroll-bottom-spacer {
    height: 12px;
    flex: 0 0 auto;
  }

  .equipment-item {
    display: flex;
    align-items: center;
    padding: 10px;
    background-color: #f5f7fa;
    border: 1px solid #e4e7ed;
    border-radius: 6px;
    cursor: grab;
    transition: all 0.2s;
    position: relative;
  }

  .equipment-item:hover {
    background-color: #ecf5ff;
    border-color: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
  }

  .equipment-item:hover .item-actions {
    opacity: 1;
  }

  .equipment-item:active {
    cursor: grabbing;
  }

  .item-icon {
    width: 48px;
    height: 48px;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 12px;
    background-color: #ffffff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    overflow: hidden;
    flex-shrink: 0;
  }

  .item-icon img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }

  /* SVG XML 직접 렌더링 */
  .svg-render {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .svg-render :deep(svg) {
    width: 100%;
    height: 100%;
    max-width: 40px;
    max-height: 40px;
  }

  .icon-placeholder {
    font-size: 20px;
    font-weight: bold;
    color: #909399;
  }

  .item-info {
    display: flex;
    flex-direction: column;
    min-width: 0;
    flex: 1;
  }

  .item-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .item-code {
    font-size: 12px;
    color: #909399;
  }

  .item-layer {
    font-size: 11px;
    padding: 1px 5px;
    border-radius: 3px;
    margin-top: 2px;
    width: fit-content;
  }

  .layer-static {
    background: #f0f9eb;
    color: #67c23a;
  }
  .layer-dynamic {
    background: #fdf6ec;
    color: #e6a23c;
  }
  .layer-overlay {
    background: #ecf5ff;
    color: #409eff;
  }

  /* 호버 액션 버튼 */
  .item-actions {
    display: flex;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.15s;
    flex-shrink: 0;
    margin-left: 6px;
  }

  .item-action-btn {
    width: 24px;
    height: 24px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    background: #fff;
    cursor: pointer;
    font-size: 12px;
    color: #606266;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
  }

  .item-action-btn:hover {
    border-color: #409eff;
    color: #409eff;
  }

  .item-action-btn.danger:hover {
    border-color: #f56c6c;
    color: #f56c6c;
  }

  /* 편집 모달 오버레이 */
  .edit-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.45);
    z-index: 2000;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .edit-modal {
    background: #fff;
    border-radius: 12px;
    width: 420px;
    max-width: 90vw;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  }

  .edit-modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-bottom: 1px solid #ebeef5;
  }

  .edit-modal-header h4 {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  .close-btn {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 16px;
    color: #909399;
    padding: 4px;
    line-height: 1;
  }
  .close-btn:hover {
    color: #606266;
  }

  .edit-modal-body {
    padding: 16px 20px;
    overflow-y: auto;
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .edit-field {
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  .edit-field label {
    font-size: 12px;
    color: #909399;
    font-weight: 500;
  }

  .edit-input,
  .edit-select {
    padding: 8px 10px;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    font-size: 13px;
    outline: none;
  }
  .edit-input:focus,
  .edit-select:focus {
    border-color: #409eff;
  }

  .readonly-input {
    padding: 8px 10px;
    border: 1px solid #ebeef5;
    border-radius: 6px;
    font-size: 13px;
    background: #f5f7fa;
    color: #909399;
  }

  .checkbox-field {
    flex-direction: row;
    align-items: center;
    gap: 10px;
  }

  .file-input {
    font-size: 13px;
  }

  .svg-preview {
    margin-top: 8px;
    width: 64px;
    height: 64px;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
  }

  .svg-preview :deep(svg) {
    width: 56px;
    height: 56px;
  }

  .edit-modal-footer {
    padding: 14px 20px;
    border-top: 1px solid #ebeef5;
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }

  .edit-modal-footer .action-btn {
    width: auto;
    padding: 8px 18px;
  }
</style>
