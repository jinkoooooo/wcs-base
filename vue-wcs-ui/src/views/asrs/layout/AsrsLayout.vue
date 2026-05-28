<template>
  <div class="modern-wcs-container">
    <aside class="settings-sidebar">
      <!-- 💡 수정됨: 헤더 영역에 '새 도면' 버튼 배치 -->
      <div class="sidebar-header">
        <div class="header-text">
          <h2 class="title">레이아웃 빌더</h2>
          <p class="subtitle">2D to 3D Digital Twin Editor</p>
        </div>
        <button class="btn-new-file" @click="confirmReset" title="모든 내용을 지우고 새로 시작합니다.">
          📄 새 도면
        </button>
      </div>

      <div class="settings-content custom-scrollbar">
        <!-- 레이아웃 식별 정보 -->
        <div class="config-section" style="background-color: #f0f9ff; border-left: 4px solid #0ea5e9;">
          <div class="panel-header" @click="panels.meta = !panels.meta">
            <h3 class="section-title" style="color: #0284c7;">📋 레이아웃 식별 정보</h3>
            <span class="chevron" :class="{ 'is-open': panels.meta }">▼</span>
          </div>

          <div class="panel-body" v-show="panels.meta">
            <p class="help-text">저장될 센터와 해당 동(구역)을 지정합니다.</p>
            <div class="input-wrapper">
              <label>물류센터명 (Center)</label>
              <input type="text" v-model="meta.centerName" class="modern-input" placeholder="예: 하림 익산 메가센터" />
            </div>
            <div class="input-grid mt-2">
              <div class="input-wrapper"><label>동/구역명</label><input type="text" v-model="meta.buildingName" class="modern-input" /></div>
              <div class="input-wrapper"><label>버전/이름</label><input type="text" v-model="meta.layoutVersion" class="modern-input" /></div>
            </div>
          </div>
        </div>

        <!-- 도구 팔레트 -->
        <div class="config-section">
          <div class="panel-header" @click="panels.palette = !panels.palette">
            <h3 class="section-title">🛠 도구 팔레트</h3>
            <span class="chevron" :class="{ 'is-open': panels.palette }">▼</span>
          </div>

          <div class="panel-body" v-show="panels.palette">
            <p class="help-text">아래 설비를 중앙 도면으로 끌어다 놓으세요.</p>
            <div class="palette-grid">
              <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart($event, 'CV_H')">
                <div class="icon-box cv-h"></div><span>가로 컨베이어</span>
              </div>
              <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart($event, 'CV_V')">
                <div class="icon-box cv-v"></div><span>세로 컨베이어</span>
              </div>
              <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart($event, 'RGV')">
                <div class="icon-box rgv"></div><span>RGV 트랙</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 중앙 AS/RS 랙 -->
        <div class="config-section">
          <div class="panel-header" @click="panels.asrs = !panels.asrs">
            <h3 class="section-title">🏢 중앙 AS/RS 랙 (자동)</h3>
            <span class="chevron" :class="{ 'is-open': panels.asrs }">▼</span>
          </div>

          <div class="panel-body" v-show="panels.asrs">
            <p class="help-text" style="color:#ef4444;">※ 도면의 랙을 통째로 마우스로 잡아 이동할 수 있습니다.</p>
            <div class="input-grid">
              <div class="input-wrapper"><label>S/C 통로 수</label><input type="number" v-model.number="config.scCount" class="modern-input" /></div>
              <div class="input-wrapper"><label>랙 열(Col)</label><input type="number" v-model.number="config.columns" class="modern-input" /></div>
              <div class="input-wrapper full-width">
                <label>통로당 양옆 랙 행</label>
                <select v-model.number="config.rowsPerSide" class="modern-input">
                  <option value="1">1행 (Single)</option>
                  <option value="2">2행 (Double)</option>
                </select>
              </div>

              <div class="input-wrapper full-width mt-2" style="border: 2px dashed #0ea5e9; padding: 10px; border-radius: 6px; background-color: #f0f9ff; box-sizing: border-box;">
                <label style="color: #0284c7;">랙 단(Tier) 수 (3D 렌더링용)</label>
                <input type="number" v-model.number="config.tiers" class="modern-input" min="1" max="50" style="border-color: #0ea5e9; font-weight: bold;" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 💡 수정됨: 하단은 기존처럼 불러오기/저장 두 개의 메인 액션만 집중되도록 롤백 -->
      <div class="sidebar-footer action-buttons">
        <button class="btn-load" @click="openLoadModal">📂 불러오기</button>
        <button class="btn-save" @click="saveLayout">💾 레이아웃 저장</button>
      </div>
    </aside>

    <main class="canvas-stage">
      <div class="canvas-wrapper custom-scrollbar" ref="canvasWrapperRef">
        <svg
          ref="svgRef"
          :width="3000"
          :height="2000"
          class="asrs-svg"
          :class="{ 'is-dragging': draggingItemId, 'is-panning': isPanning }"
          @dragenter.prevent
          @dragover.prevent
          @drop.prevent="onCanvasDrop"
          @mousemove="onCanvasMouseMove"
          @mouseup="onCanvasMouseUp"
          @mouseleave="onCanvasMouseUp"
        >
          <defs>
            <pattern id="dotGrid" width="20" height="20" patternUnits="userSpaceOnUse"><circle cx="10" cy="10" r="1" fill="#cbd5e1" /></pattern>
            <filter id="softShadow" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="4" stdDeviation="6" flood-opacity="0.08" /></filter>
            <linearGradient id="scGradient" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#4f46e5" /><stop offset="100%" stop-color="#3730a3" /></linearGradient>
            <pattern id="roller" width="10" height="10" patternUnits="userSpaceOnUse"><line x1="5" y1="0" x2="5" y2="10" stroke="#cbd5e1" stroke-width="2" /></pattern>
            <pattern id="roller-v" width="10" height="10" patternUnits="userSpaceOnUse"><line x1="0" y1="5" x2="10" y2="5" stroke="#cbd5e1" stroke-width="2" /></pattern>
          </defs>

          <!-- 배경 -->
          <rect width="100%" height="100%" fill="url(#dotGrid)" class="bg-grid" @mousedown="onBackgroundMouseDown" />

          <!-- ASRS 랙 -->
          <g class="center-rack draggable-rack"
             :class="{ 'is-grabbing': draggingItemId && selectedItemIds.includes('ASRS_RACK') }"
             :transform="`translate(${config.rackOffsetX}, ${config.rackOffsetY})`"
             @mousedown.prevent.stop="onRackMouseDown($event)">

            <rect :x="-10" :y="-10" :width="(config.columns * cellWidth) + 20" :height="rackTotalHeight + 10" class="rack-bg-hitbox"
                  :stroke="selectedItemIds.includes('ASRS_RACK') ? '#4f46e5' : 'transparent'"
                  :stroke-width="selectedItemIds.includes('ASRS_RACK') ? 3 : 0"
                  rx="8" pointer-events="all" />

            <g v-for="(block, scIdx) in scBlocks" :key="'sc-block-' + scIdx">
              <g v-for="rowIdx in config.rowsPerSide" :key="'top-row-' + scIdx + '-' + rowIdx">
                <g v-for="col in config.columns" :key="'top-cell-' + scIdx + '-' + rowIdx + '-' + col">
                  <rect :x="(col - 1) * cellWidth" :y="block.topRackStartY + (rowIdx - 1) * cellHeight" :width="cellWidth" :height="cellHeight" class="modern-rack-cell" rx="2" />
                </g>
              </g>
              <rect :x="0" :y="block.aisleY" :width="config.columns * cellWidth" :height="aisleHeight" fill="#f1f5f9" fill-opacity="0.5" />
              <rect :x="20" :y="block.aisleY + 4" :width="40" :height="aisleHeight - 8" fill="url(#scGradient)" rx="4" filter="url(#softShadow)"/>
              <text :x="40" :y="block.aisleY + (aisleHeight/2) + 3" font-size="9" fill="#ffffff" font-weight="700" text-anchor="middle">SC{{ scIdx + 1 }}</text>
              <g v-for="rowIdx in config.rowsPerSide" :key="'bot-row-' + scIdx + '-' + rowIdx">
                <g v-for="col in config.columns" :key="'bot-cell-' + scIdx + '-' + rowIdx + '-' + col">
                  <rect :x="(col - 1) * cellWidth" :y="block.bottomRackStartY + (rowIdx - 1) * cellHeight" :width="cellWidth" :height="cellHeight" class="modern-rack-cell" rx="2" />
                </g>
              </g>
            </g>
          </g>

          <g class="dropped-items">
            <g v-for="item in placedItems" :key="item.id"
               :transform="`translate(${item.x}, ${item.y})`"
               class="draggable-item"
               :class="{ 'is-grabbing': draggingItemId && selectedItemIds.includes(item.id) }"
               @mousedown.prevent.stop="onItemMouseDown($event, item)">

              <g v-if="item.type === 'CV_H'">
                <rect :width="item.width" :height="item.height" fill="url(#roller)"
                      :stroke="selectedItemIds.includes(item.id) ? '#4f46e5' : '#94a3b8'"
                      :stroke-width="selectedItemIds.includes(item.id) ? 3 : 1.5"
                      rx="4" filter="url(#softShadow)"/>
                <rect :x="item.width/2 - 25" :y="item.height/2 - 6" width="50" height="12" fill="rgba(255,255,255,0.9)" rx="2"/>
                <text :x="item.width/2" :y="item.height/2 + 3" font-size="9" :fill="selectedItemIds.includes(item.id) ? '#4f46e5' : '#0f172a'" font-weight="bold" text-anchor="middle">{{ item.label }}</text>
              </g>

              <g v-if="item.type === 'CV_V'">
                <rect :width="item.width" :height="item.height" fill="url(#roller-v)"
                      :stroke="selectedItemIds.includes(item.id) ? '#4f46e5' : '#94a3b8'"
                      :stroke-width="selectedItemIds.includes(item.id) ? 3 : 1.5"
                      rx="4" filter="url(#softShadow)"/>
                <rect :x="item.width/2 - 20" :y="item.height/2 - 6" width="40" height="12" fill="rgba(255,255,255,0.9)" rx="2"/>
                <text :x="item.width/2" :y="item.height/2 + 3" font-size="9" :fill="selectedItemIds.includes(item.id) ? '#4f46e5' : '#0f172a'" font-weight="bold" text-anchor="middle">{{ item.label }}</text>
              </g>

              <g v-if="item.type === 'RGV'">
                <rect :width="item.width" :height="item.height" fill="#f8fafc"
                      :stroke="selectedItemIds.includes(item.id) ? '#4f46e5' : '#cbd5e1'"
                      :stroke-width="selectedItemIds.includes(item.id) ? 3 : 2"
                      rx="4" filter="url(#softShadow)"/>
                <line :x1="item.width/2" y1="10" :x2="item.width/2" :y2="item.height - 10" stroke="#94a3b8" stroke-width="3" stroke-dasharray="10 5"/>
                <rect :x="(item.width - 50) / 2" :y="(item.height - 36) / 2" width="50" height="36" fill="#ef4444" rx="4" />
                <text :x="item.width / 2" :y="(item.height / 2) + 4" font-size="10" fill="#fff" font-weight="bold" text-anchor="middle">{{ item.label }}</text>
              </g>
            </g>
          </g>

          <rect v-if="isBoxSelecting"
                :x="boxSelect.x" :y="boxSelect.y"
                :width="boxSelect.width" :height="boxSelect.height"
                fill="rgba(79, 70, 229, 0.1)" stroke="#4f46e5" stroke-width="1.5" stroke-dasharray="4" />

        </svg>
      </div>
    </main>

    <aside class="property-sidebar">
      <div class="sidebar-header">
        <h2 class="title">속성 패널</h2>
        <p class="subtitle">Properties</p>
      </div>

      <div v-if="selectedItemIds.length === 1 && selectedItem" class="property-body custom-scrollbar">
        <div class="input-wrapper">
          <label style="color: #4f46e5; font-weight: 800;">PLC 번지수 (Address ID)</label>
          <input type="text" v-model="selectedItem.label" class="modern-input highlight-input" placeholder="예: RC101" />
        </div>
        <div class="input-grid">
          <div class="input-wrapper"><label>너비 (W)</label><input type="number" v-model.number="selectedItem.width" class="modern-input" step="10" /></div>
          <div class="input-wrapper"><label>길이 (H)</label><input type="number" v-model.number="selectedItem.height" class="modern-input" step="10" /></div>
        </div>
        <div class="input-grid">
          <div class="input-wrapper"><label>X 좌표</label><input type="number" v-model.number="selectedItem.x" class="modern-input" step="10" /></div>
          <div class="input-wrapper"><label>Y 좌표</label><input type="number" v-model.number="selectedItem.y" class="modern-input" step="10" /></div>
        </div>
        <div class="input-wrapper">
          <label>설비 타입</label>
          <input type="text" :value="selectedItem.type" class="modern-input" disabled style="background:#f1f5f9; color:#94a3b8; font-weight: bold;" />
        </div>
        <button class="btn-delete" style="margin-top: 16px;" @click="removeSelectedItems">🗑️ 이 설비 삭제</button>
      </div>

      <div v-else-if="selectedItemIds.length >= 1" class="property-body custom-scrollbar">
        <div class="multi-select-box">
          <div class="multi-icon">📦</div>
          <h3 v-if="selectedItemIds.length > 1">다중 선택됨</h3>
          <h3 v-else>중앙 랙 선택됨</h3>
          <p>총 <strong>{{ selectedItemIds.length }}</strong>개의 요소가<br>선택되었습니다.</p>
        </div>
        <button class="btn-delete" @click="removeSelectedItems">🗑️ 선택된 설비 일괄 삭제</button>
      </div>

      <div v-else class="empty-state">
        <div class="empty-icon">🖱️</div>
        <p>도면에서 설비를 선택하면<br>속성을 편집할 수 있습니다.</p>
        <p style="font-size: 0.75rem; color: #cbd5e1;">(드래그하여 여러 개 동시 선택 가능)</p>
      </div>
    </aside>

    <LayoutLoadModal :visible="isModalVisible" @close="isModalVisible = false" @select="handleLayoutSelect" />
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue';
import { updateList } from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';
import LayoutLoadModal from './LayoutLoadModal.vue';

const svgRef = ref(null);
const canvasWrapperRef = ref(null);

const panels = reactive({ meta: true, palette: true, asrs: true });
const meta = reactive({ centerName: '', buildingName: '', layoutVersion: '' });
const config = reactive({ scCount: 3, columns: 30, rowsPerSide: 2, tiers: 10, rackOffsetX: 400, rackOffsetY: 100 });
const { notification } = useMessage();
const cellWidth = 16; const cellHeight = 16; const aisleHeight = 32;

const isModalVisible = ref(false);
const loadedLayoutId = ref(null);

const openLoadModal = () => { isModalVisible.value = true; };

const handleLayoutSelect = (layoutData) => {
  try {
    loadedLayoutId.value = layoutData.id;
    meta.centerName = layoutData.center_id || layoutData.centerId;
    meta.buildingName = layoutData.zone_id || layoutData.zoneId;
    meta.layoutVersion = layoutData.layout_version || layoutData.layoutVersion || '';

    if (layoutData.layout_data || layoutData.layoutData) {
      const parsedData = JSON.parse(layoutData.layout_data || layoutData.layoutData);
      Object.assign(config, parsedData.rackConfig);
      placedItems.value = parsedData.equipments || [];
    }
    isModalVisible.value = false;
    notification.success({ message: '불러오기 성공', description: `[${meta.centerName}] 도면을 그렸습니다.` });
  } catch (error) {
    notification.error({ message: '에러', description: '도면 데이터를 해독할 수 없습니다.' });
  }
};

const confirmReset = () => {
  if (placedItems.value.length > 0 || loadedLayoutId.value) {
    if (!confirm('현재 작성 중인 도면이 모두 초기화됩니다. 계속하시겠습니까?')) {
      return;
    }
  }

  meta.centerName = '';
  meta.buildingName = '';
  meta.layoutVersion = '';

  config.scCount = 3;
  config.columns = 30;
  config.rowsPerSide = 2;
  config.tiers = 10;
  config.rackOffsetX = 400;
  config.rackOffsetY = 100;

  placedItems.value = [];
  selectedItemIds.value = [];
  itemCounter = 1;
  loadedLayoutId.value = null;

  if (canvasWrapperRef.value) {
    canvasWrapperRef.value.scrollLeft = 0;
    canvasWrapperRef.value.scrollTop = 0;
  }

  notification.info({ message: '새 도면', description: '초기화되었습니다. 새 도면을 작성해주세요.' });
};

const scBlocks = computed(() => {
  const blocks = [];
  for (let i = 0; i < config.scCount; i++) {
    const racksHeightPerSide = config.rowsPerSide * cellHeight;
    const blockHeight = (racksHeightPerSide * 2) + aisleHeight + 20;
    blocks.push({ topRackStartY: i * blockHeight, aisleY: (i * blockHeight) + racksHeightPerSide, bottomRackStartY: (i * blockHeight) + racksHeightPerSide + aisleHeight });
  }
  return blocks;
});

const rackTotalHeight = computed(() => {
  if (config.scCount === 0) return 0;
  return scBlocks.value[config.scCount - 1].bottomRackStartY + (config.rowsPerSide * cellHeight);
});

const placedItems = ref([]);
let itemCounter = 1;

const selectedItemIds = ref([]);
const selectedItem = computed(() => {
  if (selectedItemIds.value.length === 1 && selectedItemIds.value[0] !== 'ASRS_RACK') {
    return placedItems.value.find(i => i.id === selectedItemIds.value[0]);
  }
  return null;
});

const draggingItemId = ref(null);
let dragStartPos = { x: 0, y: 0 };
let originalItemPositions = {};
let originalRackPos = { x: 0, y: 0 };

const isBoxSelecting = ref(false);
const boxSelect = reactive({ startX: 0, startY: 0, x: 0, y: 0, width: 0, height: 0 });

const isPanning = ref(false);
const panStart = reactive({ x: 0, y: 0, scrollX: 0, scrollY: 0 });

const getSvgPoint = (event) => {
  const svg = svgRef.value;
  const pt = svg.createSVGPoint();
  pt.x = event.clientX; pt.y = event.clientY;
  return pt.matrixTransform(svg.getScreenCTM().inverse());
};

const onPaletteDragStart = (event, type) => {
  event.dataTransfer.setData('text/plain', type);
  event.dataTransfer.setData('newItemType', type);
  event.dataTransfer.effectAllowed = 'copy';
};

const onCanvasDrop = (event) => {
  const type = event.dataTransfer.getData('text/plain') || event.dataTransfer.getData('newItemType');
  if (!type) return;

  const svgP = getSvgPoint(event);
  const snapSize = 10;

  let defaultW = 60, defaultH = 40;
  if (type === 'CV_V') { defaultW = 40; defaultH = 60; }
  if (type === 'RGV') { defaultW = 80; defaultH = 300; }

  const newItem = {
    id: Date.now(), type: type,
    x: Math.round((svgP.x - (defaultW / 2)) / snapSize) * snapSize,
    y: Math.round((svgP.y - (defaultH / 2)) / snapSize) * snapSize,
    width: defaultW, height: defaultH,
    label: `${type.split('_')[0]}_${itemCounter++}`
  };

  placedItems.value.push(newItem);
  selectedItemIds.value = [newItem.id];
};

const clearSelection = () => { selectedItemIds.value = []; };

const onBackgroundMouseDown = (event) => {
  const svgP = getSvgPoint(event);

  if (event.button === 1 || event.shiftKey) {
    isPanning.value = true;
    panStart.x = event.clientX;
    panStart.y = event.clientY;
    if (canvasWrapperRef.value) {
      panStart.scrollX = canvasWrapperRef.value.scrollLeft;
      panStart.scrollY = canvasWrapperRef.value.scrollTop;
    }
  } else {
    clearSelection();
    isBoxSelecting.value = true;
    boxSelect.startX = svgP.x; boxSelect.startY = svgP.y;
    boxSelect.x = svgP.x; boxSelect.y = svgP.y;
    boxSelect.width = 0; boxSelect.height = 0;
  }
};

const initDragPositions = (svgP, clickTargetId) => {
  draggingItemId.value = clickTargetId;
  dragStartPos = { x: svgP.x, y: svgP.y };
  originalItemPositions = {};

  selectedItemIds.value.forEach(id => {
    if (id === 'ASRS_RACK') {
      originalRackPos = { x: config.rackOffsetX, y: config.rackOffsetY };
    } else {
      const it = placedItems.value.find(i => i.id === id);
      if (it) originalItemPositions[id] = { x: it.x, y: it.y };
    }
  });
};

const onItemMouseDown = (event, item) => {
  const svgP = getSvgPoint(event);

  if (event.shiftKey) {
    if (selectedItemIds.value.includes(item.id)) selectedItemIds.value = selectedItemIds.value.filter(id => id !== item.id);
    else selectedItemIds.value.push(item.id);
  } else {
    if (!selectedItemIds.value.includes(item.id)) selectedItemIds.value = [item.id];
  }

  initDragPositions(svgP, item.id);

  const itemsToMove = placedItems.value.filter(i => selectedItemIds.value.includes(i.id));
  placedItems.value = placedItems.value.filter(i => !selectedItemIds.value.includes(i.id));
  placedItems.value.push(...itemsToMove);
};

const onRackMouseDown = (event) => {
  const svgP = getSvgPoint(event);

  if (event.shiftKey) {
    if (selectedItemIds.value.includes('ASRS_RACK')) selectedItemIds.value = selectedItemIds.value.filter(id => id !== 'ASRS_RACK');
    else selectedItemIds.value.push('ASRS_RACK');
  } else {
    if (!selectedItemIds.value.includes('ASRS_RACK')) selectedItemIds.value = ['ASRS_RACK'];
  }

  initDragPositions(svgP, 'ASRS_RACK');
};

const onCanvasMouseMove = (event) => {
  if (isPanning.value && canvasWrapperRef.value) {
    const dx = event.clientX - panStart.x;
    const dy = event.clientY - panStart.y;
    canvasWrapperRef.value.scrollLeft = panStart.scrollX - dx;
    canvasWrapperRef.value.scrollTop = panStart.scrollY - dy;
    return;
  }

  const svgP = getSvgPoint(event);
  const snapSize = 10;

  if (isBoxSelecting.value) {
    boxSelect.x = Math.min(svgP.x, boxSelect.startX);
    boxSelect.y = Math.min(svgP.y, boxSelect.startY);
    boxSelect.width = Math.abs(svgP.x - boxSelect.startX);
    boxSelect.height = Math.abs(svgP.y - boxSelect.startY);
    return;
  }

  if (draggingItemId.value) {
    const totalDx = Math.round((svgP.x - dragStartPos.x) / snapSize) * snapSize;
    const totalDy = Math.round((svgP.y - dragStartPos.y) / snapSize) * snapSize;

    selectedItemIds.value.forEach(id => {
      if (id === 'ASRS_RACK') {
        config.rackOffsetX = originalRackPos.x + totalDx;
        config.rackOffsetY = originalRackPos.y + totalDy;
      } else {
        const it = placedItems.value.find(i => i.id === id);
        if (it && originalItemPositions[id]) {
          it.x = originalItemPositions[id].x + totalDx;
          it.y = originalItemPositions[id].y + totalDy;
        }
      }
    });
  }
};

const onCanvasMouseUp = () => {
  if (isBoxSelecting.value) {
    isBoxSelecting.value = false;

    const selected = placedItems.value.filter(item => {
      return (item.x < boxSelect.x + boxSelect.width && item.x + item.width > boxSelect.x &&
        item.y < boxSelect.y + boxSelect.height && item.y + item.height > boxSelect.y);
    });
    const newSelectedIds = selected.map(i => i.id);

    const rackLeft = config.rackOffsetX - 10;
    const rackTop = config.rackOffsetY - 10;
    const rackRight = config.rackOffsetX + (config.columns * cellWidth) + 10;
    const rackBottom = config.rackOffsetY + rackTotalHeight.value + 10;

    if (rackLeft < boxSelect.x + boxSelect.width && rackRight > boxSelect.x &&
      rackTop < boxSelect.y + boxSelect.height && rackBottom > boxSelect.y) {
      newSelectedIds.push('ASRS_RACK');
    }

    selectedItemIds.value = newSelectedIds;
  }

  draggingItemId.value = null;
  isPanning.value = false;
};

const removeSelectedItems = () => {
  placedItems.value = placedItems.value.filter(item => !selectedItemIds.value.includes(item.id));

  if (selectedItemIds.value.includes('ASRS_RACK')) {
    notification.info({ message: '알림', description: '중앙 AS/RS 랙은 삭제 대상에서 제외되었습니다. (위치 이동만 가능합니다.)' });
  }

  selectedItemIds.value = [];
};

const saveLayout = async () => {
  if (!meta.centerName || !meta.buildingName) {
    notification.error({ message: '입력 오류', description: '물류센터명과 동/구역명을 입력해주세요.' });
    return;
  }
  try {
    const areaCode = meta.buildingName;
    const profileCode = meta.layoutVersion || 'DEFAULT_PROFILE';

    const layoutUrl = '/layouts/layout';
    const layoutParam = {
      id: loadedLayoutId.value, center_id: meta.centerName, zone_id: meta.buildingName,
      layout_version: profileCode, is_active: true,
      layout_data: JSON.stringify({ rackConfig: config, equipments: placedItems.value })
    };
    await updateList(layoutUrl, [layoutParam]);

    const areaUrl = '/tb_ac_storage_area/save-area';
    const areaPayload = { center_id: meta.centerName, area_code: areaCode, area_name: `${areaCode} 구역`, area_type: 'ASRS', operation_profile_id: meta.centerName +'-'+ profileCode, active_yn: 'Y' };
    await updateList(areaUrl, areaPayload);

    const profileUrl = '/tb_ac_location_profile/save-profile';
    const profilePayload = {
      area_id: areaCode, profile_code: profileCode, profile_name: `${areaCode} 도면 레이아웃 프로파일`, active_yn: 'Y',
      aisle_start: 1, aisle_end: config.scCount, bay_start: 1, bay_end: config.columns, level_start: 1, level_end: config.tiers, depth_start: 1, depth_end: config.rowsPerSide, side_codes: config.rowsPerSide === 2 ? 'L,R' : 'L',
      location_type: 'NORMAL', code_pattern: '{AREA}-A{AISLE}-S{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}', mixed_load_yn: 'N', inbound_allowed_yn: 'Y', outbound_allowed_yn: 'Y'
    };
    await updateList(profileUrl, profilePayload);

    notification.success({ message: '도면 저장 완료', description: '레이아웃 및 프로파일이 저장되었습니다.' });
  } catch (error) {
    console.error("Layout Save Workflow Error:", error);
    notification.error({ message: '저장 실패', description: '데이터 저장 중 오류가 발생했습니다.' });
  }
};
</script>

<style scoped>
.modern-wcs-container { display: flex; height: 100vh; font-family: -apple-system, BlinkMacSystemFont, sans-serif; background-color: #e2e8f0; color: #0f172a; overflow: hidden;}
.settings-sidebar { width: 320px; background: #ffffff; border-right: 1px solid #cbd5e1; display: flex; flex-direction: column; z-index: 10; box-shadow: 4px 0 15px rgba(0,0,0,0.05); }

/* 💡 헤더 영역 CSS 수정 (새 도면 버튼 우측 정렬) */
.sidebar-header { padding: 20px 24px; border-bottom: 1px solid #f1f5f9; flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; }
.header-text { display: flex; flex-direction: column; }
.sidebar-header .title { font-size: 1.2rem; font-weight: 800; margin: 0; color: #1e293b; }
.sidebar-header .subtitle { font-size: 0.75rem; color: #64748b; margin: 4px 0 0 0; text-transform: uppercase; }

/* 💡 새 도면 버튼 스타일 */
.btn-new-file { background-color: #f8fafc; border: 1px solid #cbd5e1; border-radius: 6px; padding: 8px 12px; font-size: 0.75rem; font-weight: 700; color: #475569; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.btn-new-file:hover { background-color: #e2e8f0; color: #1e293b; border-color: #94a3b8; }

.settings-content { padding: 0; overflow-y: auto; flex: 1; }
.config-section { border-bottom: 1px solid #f1f5f9; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; cursor: pointer; transition: background-color 0.2s; }
.panel-header:hover { background-color: #f8fafc; }
.section-title { font-size: 0.9rem; font-weight: 700; color: #0f172a; margin: 0; }
.chevron { font-size: 0.8rem; color: #94a3b8; transition: transform 0.3s ease; transform: rotate(-90deg); }
.chevron.is-open { transform: rotate(0deg); }
.panel-body { padding: 0 24px 20px 24px; }
.help-text { font-size: 0.75rem; color: #64748b; margin-bottom: 12px; margin-top: 0; display: block; line-height: 1.3;}
.palette-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.palette-item { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 12px; background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 8px; cursor: grab; transition: all 0.2s; }
.palette-item:hover { background: #e0e7ff; border-color: #6366f1; transform: translateY(-2px); }
.palette-item:active { cursor: grabbing; }
.icon-box { width: 30px; height: 30px; border-radius: 4px; background: #cbd5e1; border: 2px solid #94a3b8;}
.cv-h { width: 40px; height: 20px; background: repeating-linear-gradient(90deg, #cbd5e1, #cbd5e1 2px, #f8fafc 2px, #f8fafc 6px); }
.cv-v { width: 20px; height: 40px; background: repeating-linear-gradient(180deg, #cbd5e1, #cbd5e1 2px, #f8fafc 2px, #f8fafc 6px); }
.rgv { background: #ef4444; border-color: #b91c1c; }
.palette-item span { font-size: 0.75rem; font-weight: 600; color: #475569; text-align: center;}
.input-wrapper { display: flex; flex-direction: column; gap: 6px; }
.input-wrapper label { font-size: 0.8rem; font-weight: 600; color: #475569; }
.modern-input { width: 100%; box-sizing: border-box; padding: 10px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 0.85rem; color: #334155; }
.modern-input:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,0.1); }
.input-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.full-width { grid-column: 1 / -1; }
.mt-2 { margin-top: 10px; }

/* 💡 하단 버튼 CSS 원복 (불러오기 / 저장 2개) */
.sidebar-footer { padding: 20px 24px; border-top: 1px solid #e2e8f0; background: #fff;}
.action-buttons { display: flex; gap: 8px; padding: 16px 20px; background: #fff; border-top: 1px solid #e2e8f0; }
.btn-load, .btn-save { flex: 1; padding: 14px 4px; font-size: 0.9rem; font-weight: 700; color: white; border: none; border-radius: 6px; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.btn-load { background-color: #10b981; }
.btn-load:hover { background-color: #059669; }
.btn-save { background-color: #4f46e5; box-shadow: 0 4px 6px rgba(79, 70, 229, 0.2); }
.btn-save:hover { background-color: #4338ca; }

.canvas-stage { flex: 1; padding: 24px; display: flex; flex-direction: column; overflow: hidden; }
.canvas-wrapper { flex: 1; background: #ffffff; border-radius: 12px; box-shadow: 0 10px 20px rgba(0,0,0,0.05); overflow: auto; }
.asrs-svg { display: block; }

.bg-grid { cursor: crosshair; }
.asrs-svg.is-panning .bg-grid { cursor: grabbing; }

.rack-bg-hitbox { fill: transparent; transition: all 0.2s ease; }
.draggable-rack { cursor: grab; }
.draggable-rack:hover .rack-bg-hitbox { fill: rgba(99, 102, 241, 0.05); }
.draggable-rack.is-grabbing { cursor: grabbing; filter: drop-shadow(0px 10px 20px rgba(0,0,0,0.15)); }
.modern-rack-cell { fill: #ffffff; stroke: #e2e8f0; stroke-width: 1.2; }
.draggable-item { cursor: grab; transition: filter 0.2s; }
.draggable-item:hover { filter: brightness(0.95); }
.draggable-item.is-grabbing { cursor: grabbing; filter: brightness(0.85) drop-shadow(0px 8px 12px rgba(0,0,0,0.15)); }

.property-sidebar { width: 300px; background: #ffffff; border-left: 1px solid #cbd5e1; display: flex; flex-direction: column; z-index: 10; box-shadow: -4px 0 15px rgba(0,0,0,0.05); }
.property-body { padding: 24px; display: flex; flex-direction: column; gap: 20px; overflow-y: auto; flex: 1; }
.highlight-input { border: 2px solid #4f46e5 !important; font-weight: bold; background: #eef2ff;}
.btn-delete { width: 100%; padding: 12px; background-color: #fef2f2; color: #dc2626; border: 1px solid #f87171; border-radius: 6px; font-weight: 600; font-size: 0.85rem; cursor: pointer; transition: all 0.2s; }
.btn-delete:hover { background-color: #fee2e2; color: #b91c1c; }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #94a3b8; text-align: center; gap: 16px; padding: 20px;}
.empty-icon { font-size: 3rem; opacity: 0.3; }

.multi-select-box { background: #eef2ff; border: 1px dashed #6366f1; border-radius: 8px; padding: 20px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 10px;}
.multi-icon { font-size: 2.5rem; margin-bottom: 5px; }
.multi-select-box h3 { margin: 0; color: #4f46e5; font-size: 1.1rem; }
.multi-select-box p { margin: 0; font-size: 0.85rem; color: #475569; }

.custom-scrollbar::-webkit-scrollbar { width: 8px; height: 8px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #94a3b8; }
</style>
