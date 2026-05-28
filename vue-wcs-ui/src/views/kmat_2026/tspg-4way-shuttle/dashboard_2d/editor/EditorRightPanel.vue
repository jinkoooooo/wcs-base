<template>
  <div class="right-panel">
    <div class="panel-header">
      <h3>속성</h3>

      <div class="header-actions">
        <button v-if="selectedObject" class="danger-btn" @click="deleteObject">삭제</button>
      </div>
    </div>

    <div v-if="selectedObject" class="tabbar">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{ active: activeTab === t.key }"
        @click="activeTab = t.key"
      >
        {{ t.label }}
      </button>
    </div>

    <div class="panel-scroll">
      <div v-if="!selectedObject">
        <div v-if="!page" class="empty-state">페이지를 선택해주세요.</div>

        <div v-else>
          <div class="property-section">
            <div class="section-title">페이지 정보</div>

            <div class="property-row readonly">
              <label>페이지 ID</label>
              <input type="text" :value="page.id" readonly />
            </div>

            <div class="property-row">
              <label>페이지명</label>
              <input
                type="text"
                :value="localPageName"
                @input="onChangePageName(($event.target as HTMLInputElement).value)"
                @blur="commitPageName()"
                @keydown.enter.prevent="commitPageName()"
              />
            </div>

            <div class="property-row" v-if="page?.floorLevel != null">
              <label>층</label>
              <input type="number" :value="page.floorLevel" readonly />
            </div>
          </div>

          <div class="property-section">
            <div class="section-title">캔버스</div>

            <div class="property-grid">
              <div class="property-row">
                <label>너비</label>
                <input
                  type="number"
                  :value="localCanvasWidth"
                  min="100"
                  @input="onChangeCanvasWidth(($event.target as HTMLInputElement).value)"
                  @blur="commitCanvas()"
                  @keydown.enter.prevent="commitCanvas()"
                />
              </div>

              <div class="property-row">
                <label>높이</label>
                <input
                  type="number"
                  :value="localCanvasHeight"
                  min="100"
                  @input="onChangeCanvasHeight(($event.target as HTMLInputElement).value)"
                  @blur="commitCanvas()"
                  @keydown.enter.prevent="commitCanvas()"
                />
              </div>
            </div>

            <div class="property-row">
              <label>배경색</label>
              <input
                type="color"
                :value="localBackgroundColor"
                @input="onChangeBackgroundColor(($event.target as HTMLInputElement).value)"
                @change="commitCanvas()"
              />
              <button class="clear-btn" @click="resetBackgroundColor()">초기화</button>
            </div>
          </div>

          <div class="property-section">
            <div class="section-title">설비그룹 매핑</div>

            <div class="property-row readonly">
              <label>현재 매핑</label>
              <input
                type="text"
                :value="currentEqGroupDisplay"
                readonly
                :class="{ 'has-mapping': page?.eqGroupId }"
              />
            </div>

            <div v-if="isEqGroupLoading" class="mapping-loading">로딩 중...</div>

            <template v-else>
              <div class="property-row">
                <label>입력 방식</label>
                <div class="radio-group">
                  <label class="radio-label">
                    <input type="radio" v-model="eqGroupInputMode" value="select" />
                    <span>목록 선택</span>
                  </label>
                  <label class="radio-label">
                    <input type="radio" v-model="eqGroupInputMode" value="manual" />
                    <span>직접 입력</span>
                  </label>
                </div>
              </div>

              <template v-if="eqGroupInputMode === 'select'">
                <div class="property-row">
                  <label>설비그룹</label>
                  <select v-model="selectedEqGroupId" class="eq-select">
                    <option value="">선택하세요</option>
                    <option v-for="group in eqGroupList" :key="group.id" :value="group.id">
                      {{ group.id }} ({{ group.name }})
                    </option>
                  </select>
                </div>

                <div v-if="selectedEqGroupInfo" class="selected-eq-info">
                  <div class="info-row">
                    <span class="info-label">그룹 ID:</span>
                    <span class="info-value">{{ selectedEqGroupInfo.id }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">그룹명:</span>
                    <span class="info-value">{{ selectedEqGroupInfo.name }}</span>
                  </div>
                </div>

                <div v-if="eqGroupList.length === 0" class="empty-hint">
                  등록된 설비그룹이 없습니다. 직접 입력하세요.
                </div>

                <button class="btn btn-secondary btn-sm" @click="loadEqGroupList">
                  목록 새로고침
                </button>
              </template>

              <template v-else>
                <div class="property-row">
                  <label>설비그룹 ID</label>
                  <input
                    type="text"
                    v-model="manualEqGroupId"
                    placeholder="예: TSPG_AMBIENT"
                    class="manual-input"
                  />
                </div>
                <div class="input-hint"> tb_eq_group_mst.eq_group_id 값 입력 </div>
              </template>

              <div class="mapping-actions">
                <button
                  class="btn btn-primary"
                  :disabled="!canSaveEqGroupMapping"
                  @click="saveEqGroupMapping"
                >
                  매핑 저장
                </button>
                <button v-if="page?.eqGroupId" class="btn btn-danger" @click="clearEqGroupMapping">
                  매핑 해제
                </button>
              </div>
            </template>
          </div>

          <div class="empty-state hint">캔버스(빈 곳)를 클릭하면 페이지 속성이 표시됩니다.</div>
        </div>
      </div>

      <div v-else class="object-tabs">
        <div v-if="stackedObjects.length > 1" class="property-section">
          <div class="section-title">겹친 객체</div>

          <div class="stacked-list">
            <button
              v-for="obj in stackedObjects"
              :key="obj.id"
              type="button"
              class="stacked-item"
              :class="{ active: obj.id === selectedObject?.id }"
              @click="selectFromStack(obj.id!)"
            >
              <div class="stacked-main">
                <span class="stacked-type">{{ obj.equipmentTypeCode }}</span>
                <span class="stacked-code">{{ obj.customLabel || obj.equipmentCode }}</span>
              </div>
              <div class="stacked-sub"> Z {{ obj.zIndex || 0 }} </div>
            </button>
          </div>

          <div class="input-hint">
            겹친 객체를 클릭해서 오른쪽 패널 편집 대상을 전환할 수 있습니다.
          </div>
        </div>

        <div v-show="activeTab === 'basic'" class="tab-pane">
          <div class="property-section">
            <div class="section-title">기본</div>

            <div class="property-row readonly">
              <label>ID</label>
              <input type="text" :value="selectedObject.id" readonly />
            </div>

            <div class="property-row">
              <label>설비 코드</label>
              <input
                type="text"
                :value="selectedObject.equipmentCode"
                @input="updateProperty('equipmentCode', ($event.target as HTMLInputElement).value)"
              />
            </div>

            <div class="property-row readonly">
              <label>설비 타입</label>
              <input type="text" :value="selectedObject.equipmentTypeCode" readonly />
            </div>
          </div>

          <div class="property-section">
            <div class="section-title">실운영 설비 매핑</div>

            <div class="property-row readonly">
              <label>설비 타입</label>
              <input type="text" :value="selectedObject.equipmentTypeCode" readonly />
            </div>

            <div class="property-row readonly">
              <label>매핑 타입</label>
              <input type="text" :value="computedRealEqType || '(자동 감지)'" readonly />
            </div>

            <div class="property-row">
              <label>현재 매핑</label>
              <input
                type="text"
                :value="selectedObject.realEqId || '(없음)'"
                readonly
                :class="{ 'has-mapping': selectedObject.realEqId }"
              />
            </div>
          </div>

          <div class="property-section">
            <div class="section-title">설비 선택</div>

            <div v-if="isMappingLoading" class="mapping-loading">로딩 중...</div>

            <div v-else-if="mappingError" class="mapping-error" v-html="mappingError"></div>

            <div v-else-if="isMappingBlocked" class="mapping-blocked">
              이 설비 타입({{ selectedObject.equipmentTypeCode }})은<br />
              실운영 설비 매핑이 설정되지 않았습니다.<br />
              <small>설비 팔레트에서 해당 타입의 <b>realEqTypeNum</b>을 설정해주세요.</small>
            </div>

            <template v-else>
              <template v-if="isRackType">
                <div class="property-row">
                  <label>층 정보</label>
                  <input type="text" :value="`${page?.floorLevel || 1}층`" readonly />
                </div>
                <div class="property-row">
                  <label>랙 셀 ({{ rackCells.length }}개)</label>
                  <select v-model="selectedRealEqId" class="eq-select">
                    <option value="">셀을 선택하세요</option>
                    <option v-for="cell in rackCells" :key="cell.rackId" :value="cell.rackId">
                      {{ cell.rackId }} (R{{ cell.row }}-C{{ cell.bay }})
                    </option>
                  </select>
                </div>

                <div v-if="selectedRackCell" class="selected-eq-info">
                  <div class="info-row">
                    <span class="info-label">셀 ID:</span>
                    <span class="info-value">{{ selectedRackCell.rackId }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">위치:</span>
                    <span class="info-value"
                      >{{ selectedRackCell.level }}층 R{{ selectedRackCell.row }}-B{{
                        selectedRackCell.bay
                      }}</span
                    >
                  </div>
                  <div class="info-row">
                    <span class="info-label">타입:</span>
                    <span class="info-value">{{ rackTypeLabel(selectedRackCell.type) }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">사용:</span>
                    <span class="info-value">{{ selectedRackCell.useYn ? '사용' : '미사용' }}</span>
                  </div>
                  <div v-if="selectedRackCell.driveOnlyYn" class="info-row">
                    <span class="info-label">주행전용:</span>
                    <span class="info-value">예</span>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="property-row">
                  <label>실운영 설비</label>
                  <select v-model="selectedRealEqId" class="eq-select">
                    <option value="">선택하세요</option>
                    <option v-for="eq in realEquipments" :key="eq.id" :value="eq.id">
                      {{ eq.name || eq.id }} ({{ eq.id }})
                    </option>
                  </select>
                </div>

                <div v-if="selectedRealEquipment" class="selected-eq-info">
                  <div class="info-row">
                    <span class="info-label">설비 ID:</span>
                    <span class="info-value">{{ selectedRealEquipment.id }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">설비명:</span>
                    <span class="info-value">{{ selectedRealEquipment.name }}</span>
                  </div>
                  <div class="info-row" v-if="selectedRealEquipment.plcId">
                    <span class="info-label">PLC ID:</span>
                    <span class="info-value">{{ selectedRealEquipment.plcId }}</span>
                  </div>
                </div>
              </template>

              <div class="mapping-actions">
                <button
                  class="btn btn-primary"
                  :disabled="!selectedRealEqId || selectedRealEqId === selectedObject.realEqId"
                  @click="saveMapping"
                >
                  매핑 저장
                </button>
                <button v-if="selectedObject.realEqId" class="btn btn-danger" @click="clearMapping">
                  매핑 해제
                </button>
              </div>
            </template>

            <button class="btn btn-secondary btn-sm" @click="loadRealEquipments"
              >목록 새로고침</button
            >
          </div>

          <div v-if="selectedObject?.realEqId" class="property-section">
            <div class="section-title">실운영 설비 속성 편집</div>

            <div v-if="isDetailLoading" class="mapping-loading">상세 정보 로딩 중...</div>
            <div v-if="detailError" class="mapping-error" v-html="detailError"></div>

            <template v-if="isRackType && rackDetail && !isDetailLoading">
              <div class="subsection-title">tb_eq_rack_mst</div>
              <div class="property-grid">
                <div class="property-row">
                  <label>타입</label>
                  <select v-model.number="rackEdits.type">
                    <option v-for="opt in rackTypeOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                </div>
                <div class="property-row">
                  <label>사용</label>
                  <input type="checkbox" v-model="rackEdits.useYn" />
                </div>
                <div class="property-row">
                  <label>버퍼 사용</label>
                  <input type="checkbox" v-model="rackEdits.bufferYn" />
                </div>
                <div class="property-row">
                  <label>주행 전용</label>
                  <input type="checkbox" v-model="rackEdits.driveOnlyYn" />
                </div>
              </div>

              <template v-if="locationDetail">
                <div class="subsection-title">tb_inventory_location</div>
                <div class="property-grid">
                  <div class="property-row">
                    <label>아이템 타입</label>
                    <input type="text" v-model="locationEdits.itemType" />
                  </div>
                  <div class="property-row">
                    <label>로케이션 타입</label>
                    <input
                      type="text"
                      v-model="locationEdits.locType"
                      placeholder="예: RACK, INBOUND_PORT"
                    />
                  </div>
                  <div class="property-row">
                    <label>최대 높이</label>
                    <input type="number" v-model.number="locationEdits.maxHeight" />
                  </div>
                  <div class="property-row">
                    <label>최대 무게</label>
                    <input type="number" v-model.number="locationEdits.maxWeight" />
                  </div>
                </div>
              </template>
              <div v-else class="empty-hint">매칭되는 로케이션 정보가 없습니다.</div>

              <div class="mapping-actions">
                <button class="btn btn-primary" :disabled="isDetailSaving" @click="saveRackDetail"
                  >속성 저장</button
                >
                <button
                  class="btn btn-secondary"
                  :disabled="isDetailSaving"
                  @click="loadRackDetailAndLocation"
                  >취소(다시 로드)</button
                >
              </div>
            </template>

            <template
              v-else-if="
                (computedRealEqType === 'CONVEYOR' || computedRealEqType === 'LIFTER') &&
                !isDetailLoading
              "
            >
              <div class="subsection-title">tb_eq_cv_mst ({{ computedRealEqType }})</div>

              <template v-if="cvMaster">
                <div class="property-grid">
                  <div class="property-row">
                    <label>타입</label>
                    <input type="number" v-model.number="cvMasterEdits.type" />
                  </div>
                  <div class="property-row">
                    <label>자동 가동</label>
                    <input type="checkbox" v-model="cvMasterEdits.autoYn" />
                  </div>
                  <div class="property-row">
                    <label>사용 여부</label>
                    <input type="checkbox" v-model="cvMasterEdits.useYn" />
                  </div>
                </div>

                <div class="mapping-actions">
                  <button class="btn btn-primary" :disabled="isDetailSaving" @click="saveCvDetail"
                    >속성 저장</button
                  >
                  <button class="btn btn-secondary" :disabled="isDetailSaving" @click="loadCvDetail"
                    >취소(다시 로드)</button
                  >
                </div>
              </template>
              <div v-else class="empty-hint">매칭되는 상세 정보(tb_eq_cv_mst)가 없습니다.</div>
            </template>
          </div>
        </div>

        <div v-show="activeTab === 'pos'" class="tab-pane">
          <div class="property-section">
            <div class="section-title">위치</div>
            <div class="property-grid">
              <div class="property-row">
                <label>X</label>
                <input
                  type="number"
                  :value="selectedObject.posX"
                  @input="
                    updateProperty(
                      'posX',
                      parseFloat(($event.target as HTMLInputElement).value) || 0,
                    )
                  "
                />
              </div>
              <div class="property-row">
                <label>Y</label>
                <input
                  type="number"
                  :value="selectedObject.posY"
                  @input="
                    updateProperty(
                      'posY',
                      parseFloat(($event.target as HTMLInputElement).value) || 0,
                    )
                  "
                />
              </div>
            </div>
          </div>

          <div class="property-section">
            <div class="section-title">크기</div>
            <div class="property-grid">
              <div class="property-row">
                <label>너비</label>
                <input
                  type="number"
                  :value="selectedObject.width"
                  min="10"
                  @input="
                    updateProperty(
                      'width',
                      parseFloat(($event.target as HTMLInputElement).value) || 100,
                    )
                  "
                />
              </div>
              <div class="property-row">
                <label>높이</label>
                <input
                  type="number"
                  :value="selectedObject.height"
                  min="10"
                  @input="
                    updateProperty(
                      'height',
                      parseFloat(($event.target as HTMLInputElement).value) || 100,
                    )
                  "
                />
              </div>
            </div>
          </div>
        </div>

        <div v-show="activeTab === 'transform'" class="tab-pane">
          <div class="property-section">
            <div class="section-title">변형</div>

            <div class="property-row">
              <label>회전</label>
              <input
                type="number"
                :value="selectedObject.rotation"
                step="15"
                @input="
                  updateProperty(
                    'rotation',
                    parseFloat(($event.target as HTMLInputElement).value) || 0,
                  )
                "
              />
            </div>

            <div class="property-grid">
              <div class="property-row">
                <label>스케일 X</label>
                <input
                  type="number"
                  :value="selectedObject.scaleX"
                  step="0.1"
                  min="0.1"
                  @input="
                    updateProperty(
                      'scaleX',
                      parseFloat(($event.target as HTMLInputElement).value) || 1,
                    )
                  "
                />
              </div>
              <div class="property-row">
                <label>스케일 Y</label>
                <input
                  type="number"
                  :value="selectedObject.scaleY"
                  step="0.1"
                  min="0.1"
                  @input="
                    updateProperty(
                      'scaleY',
                      parseFloat(($event.target as HTMLInputElement).value) || 1,
                    )
                  "
                />
              </div>
            </div>

            <div class="property-grid">
              <div class="property-row checkbox">
                <label>수평 뒤집기</label>
                <input
                  type="checkbox"
                  :checked="selectedObject.flipH"
                  @change="updateProperty('flipH', ($event.target as HTMLInputElement).checked)"
                />
              </div>
              <div class="property-row checkbox">
                <label>수직 뒤집기</label>
                <input
                  type="checkbox"
                  :checked="selectedObject.flipV"
                  @change="updateProperty('flipV', ($event.target as HTMLInputElement).checked)"
                />
              </div>
            </div>
          </div>
        </div>

        <div v-show="activeTab === 'display'" class="tab-pane">
          <div class="property-section">
            <div class="section-title">표시</div>

            <div class="property-grid">
              <div class="property-row">
                <label>Z</label>
                <input
                  type="number"
                  :value="selectedObject.zIndex"
                  @input="
                    updateProperty(
                      'zIndex',
                      parseInt(($event.target as HTMLInputElement).value) || 0,
                    )
                  "
                />
              </div>

              <div class="property-row">
                <label>투명도</label>
                <input
                  type="number"
                  min="0"
                  max="1"
                  step="0.1"
                  :value="selectedObject.opacity ?? 1"
                  @input="
                    updateProperty('opacity', parseFloat(($event.target as HTMLInputElement).value))
                  "
                />
              </div>
            </div>

            <div class="property-row checkbox">
              <label>레이블 표시</label>
              <input
                type="checkbox"
                :checked="selectedObject.showLabel"
                @change="updateProperty('showLabel', ($event.target as HTMLInputElement).checked)"
              />
            </div>

            <div class="property-row">
              <label>레이블</label>
              <input
                type="text"
                :value="selectedObject.customLabel || ''"
                @input="updateProperty('customLabel', ($event.target as HTMLInputElement).value)"
                placeholder="(설비 코드 사용)"
              />
            </div>

            <div class="property-row">
              <label>색상</label>
              <input
                type="color"
                :value="selectedObject.customColor || '#000000'"
                @input="updateProperty('customColor', ($event.target as HTMLInputElement).value)"
              />
              <button
                v-if="selectedObject.customColor"
                class="clear-btn"
                @click="updateProperty('customColor', '')"
              >
                초기화
              </button>
            </div>

            <div class="property-row checkbox">
              <label>잠금</label>
              <input
                type="checkbox"
                :checked="selectedObject.isLocked"
                @change="updateProperty('isLocked', ($event.target as HTMLInputElement).checked)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch, computed } from 'vue';
  import type { TbEcs2dItem, TbEcs2dPage } from '../api/types';
  import { useShuttleStore } from '../store/shuttleStore';
  import { realTimeApi } from '../api/shuttle';
  import { generatorApi } from '../api/generatorApi';
  import { RackType } from '../../constants/EcsDBConsts';
  import { enumLabel } from '../../constants';
  import { objectsIntersect } from './utils/editorGeom';

  /** RackType 숫자 코드 → 한글 라벨 (디스크립터에서 lookup) */
  function rackTypeLabel(typeCode: number | string | null | undefined): string {
    if (typeCode == null) return '-';
    const num = typeof typeCode === 'string' ? parseInt(typeCode, 10) : typeCode;
    return enumLabel(RackType, num, `타입 ${num}`);
  }

  const store = useShuttleStore();

  const props = defineProps<{
    selectedObject: TbEcs2dItem | null;
    selectedObjectIds: string[];
    objects: TbEcs2dItem[];
    page: TbEcs2dPage | undefined;
  }>();

  const emit = defineEmits<{
    (e: 'select-object', id: string | null): void;
    (e: 'update-property', property: string, value: any): void;
    (e: 'update-properties', ids: string[], property: string, value: any): void;
    (e: 'delete-object'): void;
    (e: 'update-page-name', payload: { id: string; pageName: string }): void;
    (e: 'update-page-canvas', width: number, height: number, backgroundColor: string): void;
    (e: 'update-real-eq-mapping', layoutId: string, realEqId: string, realEqType: string): void;
    (e: 'clear-real-eq-mapping', layoutId: string): void;
  }>();

  const tabs = [
    { key: 'basic', label: '기본' },
    { key: 'pos', label: '위치/크기' },
    { key: 'transform', label: '변형' },
    { key: 'display', label: '표시' },
  ] as const;

  type TabKey = (typeof tabs)[number]['key'];
  const activeTab = ref<TabKey>('basic');

  const deleteObject = () => {
    if (confirm('선택한 객체를 삭제하시겠습니까?')) emit('delete-object');
  };

  // ============================================
  // 설비그룹 매핑 (페이지 속성에 포함)
  // ============================================
  const selectedEqGroupId = ref<string>('');
  const manualEqGroupId = ref<string>('');
  const eqGroupInputMode = ref<'select' | 'manual'>('select');
  const isEqGroupLoading = ref(false);

  const eqGroupList = computed(() => store.eqGroups || []);

  const currentEqGroupDisplay = computed(() => {
    if (!props.page?.eqGroupId) return '(없음)';
    const group = eqGroupList.value.find((g: any) => g.eqGroupId === props.page?.eqGroupId);
    if (group) return `${group.name} (${group.id})`;
    return props.page.eqGroupId;
  });

  const selectedEqGroupInfo = computed(() => {
    if (!selectedEqGroupId.value) return null;
    return eqGroupList.value.find((g: any) => g.eqGroupId === selectedEqGroupId.value) || null;
  });

  const canSaveEqGroupMapping = computed(() => {
    if (!props.page?.id) return false;
    const targetId =
      eqGroupInputMode.value === 'select' ? selectedEqGroupId.value : manualEqGroupId.value.trim();
    if (!targetId) return false;
    return targetId !== props.page?.eqGroupId;
  });

  const loadEqGroupList = async () => {
    isEqGroupLoading.value = true;
    try {
      await store.loadAllEqGroups();
    } catch (error) {
      console.error('Failed to load eq groups:', error);
    } finally {
      isEqGroupLoading.value = false;
    }
  };

  const saveEqGroupMapping = async () => {
    const targetId =
      eqGroupInputMode.value === 'select' ? selectedEqGroupId.value : manualEqGroupId.value.trim();
    if (!props.page?.id || !targetId) return;
    isEqGroupLoading.value = true;
    try {
      await store.updatePageEqGroup(props.page.id, targetId);
      selectedEqGroupId.value = targetId;
      manualEqGroupId.value = targetId;
      alert('설비그룹 매핑이 저장되었습니다.');
    } catch (error: any) {
      alert('설비그룹 매핑 저장 실패: ' + (error?.message || error));
    } finally {
      isEqGroupLoading.value = false;
    }
  };

  const clearEqGroupMapping = async () => {
    if (!props.page?.id) return;
    if (!confirm('설비그룹 매핑을 해제하시겠습니까?')) return;
    isEqGroupLoading.value = true;
    try {
      await store.clearPageEqGroup(props.page.id);
      selectedEqGroupId.value = '';
      manualEqGroupId.value = '';
      alert('설비그룹 매핑이 해제되었습니다.');
    } catch (error: any) {
      alert('설비그룹 매핑 해제 실패: ' + (error?.message || error));
    } finally {
      isEqGroupLoading.value = false;
    }
  };

  watch(
    () => props.page?.id,
    async () => {
      selectedEqGroupId.value = props.page?.eqGroupId || '';
      manualEqGroupId.value = props.page?.eqGroupId || '';
      if (eqGroupList.value.length === 0) {
        await loadEqGroupList();
      }
    },
    { immediate: true },
  );

  // ============================================
  // 실운영설비 매핑 (기본 탭에 포함)
  // ============================================
  const selectedRealEqId = ref<string>('');
  const isMappingLoading = ref(false);
  const mappingError = ref<string>('');

  const realEquipments = computed(() => store.realEquipments);

  const computedRealEqTypeNum = computed<number | null>(() => {
    if (!props.selectedObject?.equipmentTypeCode) return null;
    const typeCode = props.selectedObject.equipmentTypeCode.toUpperCase();
    const eqTypeDef = store.equipmentTypeMap.get(typeCode);
    return eqTypeDef?.realEqTypeNum ?? null;
  });

  const isMappingBlocked = computed(() => {
    if (!props.selectedObject?.equipmentTypeCode) return false;
    const typeCode = props.selectedObject.equipmentTypeCode.toUpperCase();
    const eqTypeDef = store.equipmentTypeMap.get(typeCode);
    if (!eqTypeDef) return true;
    return eqTypeDef.realEqTypeNum == null;
  });

  const computedRealEqType = computed(() => {
    if (!props.selectedObject?.equipmentTypeCode) return '';
    const typeCode = props.selectedObject.equipmentTypeCode.toUpperCase();
    const eqTypeDef = store.equipmentTypeMap.get(typeCode);
    if (eqTypeDef?.layerType === 'dynamic') {
      if (typeCode.includes('SHUTTLE') || typeCode.includes('CAR')) return 'SHUTTLE_CAR';
      return typeCode;
    }
    if (typeCode.includes('RACK') || typeCode.includes('CELL')) return 'RACK';
    if (typeCode.includes('CONVEYOR') || typeCode.includes('CV')) return 'CONVEYOR';
    if (typeCode.includes('LIFTER') || typeCode.includes('LIFT')) return 'LIFTER';
    return typeCode;
  });

  const rackCells = ref<any[]>([]);
  const isRackType = computed(() => computedRealEqType.value === 'RACK');
  const lastLoadedType = ref<string>('');

  watch(
    () => props.selectedObject?.id,
    async () => {
      activeTab.value = 'basic';
      selectedRealEqId.value = props.selectedObject?.realEqId || '';

      if (!props.selectedObject?.equipmentTypeCode) {
        lastLoadedType.value = '';
        mappingError.value = '';
        return;
      }

      const t = computedRealEqType.value || '';
      if (t && t !== lastLoadedType.value) {
        lastLoadedType.value = t;
        await loadRealEquipments();
      }
    },
    { immediate: true },
  );

  watch(
    () => props.selectedObject?.realEqId,
    (newVal) => {
      selectedRealEqId.value = newVal || '';
    },
    { immediate: true },
  );

  const loadRealEquipments = async () => {
    if (!props.selectedObject?.equipmentTypeCode || isMappingBlocked.value) return;

    isMappingLoading.value = true;
    mappingError.value = '';
    rackCells.value = [];

    try {
      const eqTypeNum = computedRealEqTypeNum.value;
      const eqTypeName = computedRealEqType.value;
      const eqGroupId = props.page?.eqGroupId || store.selectedEqGroupId;

      if (eqTypeName === 'RACK') {
        const floor = props.page?.floorLevel || 1;
        if (eqGroupId) {
          rackCells.value = await realTimeApi.getRackCellsByGroupAndFloor(eqGroupId, floor);
        } else {
          mappingError.value = '설비그룹이 설정되지 않았습니다. 페이지 속성에서 매핑해주세요.';
        }
      } else if (eqTypeNum !== null) {
        const finalEqGroupId = eqGroupId || 'default';
        await store.loadRealEquipmentsByEqGroupIdAndEqType(finalEqGroupId, eqTypeNum);
      }
    } catch (error: any) {
      mappingError.value = error?.message || '설비 목록 로드 실패';
    } finally {
      isMappingLoading.value = false;
    }
  };

  const saveMapping = async () => {
    if (!props.selectedObject?.id || !selectedRealEqId.value) return;
    isMappingLoading.value = true;
    mappingError.value = '';

    try {
      await store.updateRealEqMapping(
        props.selectedObject.id,
        selectedRealEqId.value,
        computedRealEqType.value,
      );
    } catch (error: any) {
      mappingError.value = error?.message || '매핑 저장 실패';
    } finally {
      isMappingLoading.value = false;
    }
  };

  const clearMapping = async () => {
    if (!props.selectedObject?.id) return;
    if (!confirm('실운영 설비 매핑을 해제하시겠습니까?')) return;
    isMappingLoading.value = true;
    mappingError.value = '';

    try {
      await store.clearRealEqMapping(props.selectedObject.id);
      selectedRealEqId.value = '';
    } catch (error: any) {
      mappingError.value = error?.message || '매핑 해제 실패';
    } finally {
      isMappingLoading.value = false;
    }
  };

  const selectedRealEquipment = computed(() => {
    if (!selectedRealEqId.value) return null;
    return realEquipments.value.find((eq: any) => eq.id === selectedRealEqId.value) || null;
  });

  const selectedRackCell = computed(() => {
    if (!selectedRealEqId.value || !isRackType.value) return null;
    return rackCells.value.find((cell: any) => cell.rackId === selectedRealEqId.value) || null;
  });

  // ============================================
  // 안전 매핑 유틸 및 에러 포맷팅
  // ============================================
  const getProp = (obj: any, camel: string, snake: string, defaultVal: any) => {
    if (!obj) return defaultVal;
    if (obj[camel] !== undefined && obj[camel] !== null) return obj[camel];
    if (obj[snake] !== undefined && obj[snake] !== null) return obj[snake];
    return defaultVal;
  };

  const formatError = (e: any, prefix: string) => {
    const msg = e?.response?.data?.message || e?.message || String(e);
    return `<b>${prefix}</b><br/>${msg}`;
  };

  // ============================================
  // 실운영 설비 마스터 속성 편집 로직
  // ============================================
  const rackDetail = ref<any | null>(null);
  const locationDetail = ref<any | null>(null);
  const rackEdits = ref<any>({});
  const locationEdits = ref<any>({});

  const cvMaster = ref<any | null>(null);
  const cvMasterEdits = ref<any>({});

  const isDetailLoading = ref(false);
  const isDetailSaving = ref(false);
  const detailError = ref('');

  const rackTypeOptions = computed(() =>
    Object.values(RackType).map((entry) => ({ value: entry.code, label: entry.label })),
  );

  // -------- RACK + LOCATION --------
  async function loadRackDetailAndLocation() {
    detailError.value = '';
    rackDetail.value = null;
    locationDetail.value = null;
    rackEdits.value = {};
    locationEdits.value = {};

    if (!isRackType.value || !props.selectedObject?.realEqId) return;
    const eqGroupId = props.page?.eqGroupId || store.selectedEqGroupId;
    const rackId = props.selectedObject.realEqId;
    if (!eqGroupId) return;

    const cell = rackCells.value.find((c: any) => c.rackId === rackId || c.rack_id === rackId);
    const eqId = cell?.eqId || cell?.eq_id;

    if (!eqId) {
      detailError.value = '랙 셀의 상위 설비(eqId) 정보를 찾을 수 없습니다.';
      return;
    }

    isDetailLoading.value = true;
    try {
      const [rack, loc] = await Promise.all([
        generatorApi.getRackCell(eqGroupId, eqId, rackId),
        generatorApi.getLocation(eqGroupId, eqId, rackId).catch(() => null),
      ]);

      rackDetail.value = rack;
      rackEdits.value = {
        type: getProp(rack, 'type', 'type', 0),
        useYn: getProp(rack, 'useYn', 'use_yn', false),
        bufferYn: getProp(rack, 'bufferYn', 'buffer_yn', false),
        driveOnlyYn: getProp(rack, 'driveOnlyYn', 'drive_only_yn', false),
      };

      if (loc) {
        locationDetail.value = loc;
        locationEdits.value = {
          itemType: getProp(loc, 'itemType', 'item_type', ''),
          locType: getProp(loc, 'locType', 'loc_type', ''),
          maxHeight: getProp(loc, 'maxHeight', 'max_height', 0),
          maxWeight: getProp(loc, 'maxWeight', 'max_weight', 0),
        };
      }
    } catch (e: any) {
      detailError.value = formatError(e, '랙 속성 로드 실패');
    } finally {
      isDetailLoading.value = false;
    }
  }

  async function saveRackDetail() {
    if (!rackDetail.value) return;
    const eqGroupId = props.page?.eqGroupId || store.selectedEqGroupId;
    const rackId = props.selectedObject!.realEqId!;
    const cell = rackCells.value.find((c: any) => c.rackId === rackId || c.rack_id === rackId);
    const eqId = cell?.eqId || cell?.eq_id;
    if (!eqGroupId || !eqId) return;

    isDetailSaving.value = true;
    detailError.value = '';
    try {
      const rackPayload = {
        type: rackEdits.value.type,
        useYn: rackEdits.value.useYn,
        bufferYn: rackEdits.value.bufferYn,
        driveOnlyYn: rackEdits.value.driveOnlyYn,
      };

      const locPayload: any = {};
      if (locationDetail.value) {
        locPayload.itemType = locationEdits.value.itemType;
        locPayload.locType = locationEdits.value.locType;
        locPayload.maxHeight = locationEdits.value.maxHeight;
        locPayload.maxWeight = locationEdits.value.maxWeight;
      }

      const promises: Promise<any>[] = [];
      promises.push(generatorApi.updateRackCell(eqGroupId, eqId, rackId, rackPayload));

      if (locationDetail.value) {
        promises.push(generatorApi.updateLocation(eqGroupId, eqId, rackId, locPayload));
      }

      await Promise.all(promises);
      alert('속성이 저장되었습니다.');
      await loadRackDetailAndLocation();
    } catch (e: any) {
      detailError.value = formatError(e, '속성 저장 실패');
    } finally {
      isDetailSaving.value = false;
    }
  }

  // -------- CONVEYOR / LIFTER (동일 로직 처리) --------
  async function loadCvDetail() {
    detailError.value = '';
    cvMaster.value = null;
    cvMasterEdits.value = {};

    if (
      (computedRealEqType.value !== 'CONVEYOR' && computedRealEqType.value !== 'LIFTER') ||
      !props.selectedObject?.realEqId
    )
      return;
    const eqGroupId = props.page?.eqGroupId || store.selectedEqGroupId;
    const cvId = props.selectedObject.realEqId;
    if (!eqGroupId) return;

    // 매핑 목록에서 해당 설비를 찾아 부모 eqId 추출 (500 에러 방지 핵심)
    const eqItem = realEquipments.value.find((eq: any) => eq.id === cvId);
    const parentEqId = eqItem ? eqItem.eqId || eqItem.eq_id || cvId : cvId;

    isDetailLoading.value = true;
    try {
      // tb_eq_mst를 거치지 않고 직접 tb_eq_cv_mst 목록 호출
      const cvList = await generatorApi.getCvMstByEqId(eqGroupId, parentEqId);
      if (cvList && cvList.length > 0) {
        const cv = cvList.find((c: any) => c.id === cvId) || cvList[0];
        cvMaster.value = cv;
        cvMasterEdits.value = {
          type: getProp(cv, 'type', 'type', 0),
          autoYn: getProp(cv, 'autoYn', 'auto_yn', false),
          useYn: getProp(cv, 'useYn', 'use_yn', false),
        };
      }
    } catch (e: any) {
      detailError.value = formatError(e, `${computedRealEqType.value} 상세 정보 로드 실패`);
    } finally {
      isDetailLoading.value = false;
    }
  }

  async function saveCvDetail() {
    const eqGroupId = props.page?.eqGroupId || store.selectedEqGroupId;
    const cvId = props.selectedObject!.realEqId!;
    if (!eqGroupId || !cvMaster.value) return;

    const eqItem = realEquipments.value.find((eq: any) => eq.id === cvId);
    const parentEqId = eqItem ? eqItem.eqId || eqItem.eq_id || cvId : cvId;

    isDetailSaving.value = true;
    detailError.value = '';
    try {
      const targetCvId = cvMaster.value.id;
      await generatorApi.updateCvMst(eqGroupId, parentEqId, targetCvId, {
        type: cvMasterEdits.value.type,
        autoYn: cvMasterEdits.value.autoYn,
        useYn: cvMasterEdits.value.useYn,
      } as any);

      alert('속성이 저장되었습니다.');
      await loadCvDetail();
    } catch (e: any) {
      detailError.value = formatError(e, '속성 저장 실패');
    } finally {
      isDetailSaving.value = false;
    }
  }

  // 타입별 자동 로드 Watcher
  watch(
    () => [
      props.selectedObject?.id,
      props.selectedObject?.realEqId,
      isRackType.value,
      rackCells.value.length,
    ],
    () => {
      if (isRackType.value) loadRackDetailAndLocation();
    },
    { immediate: true },
  );

  watch(
    () => [props.selectedObject?.id, props.selectedObject?.realEqId, computedRealEqType.value],
    () => {
      if (computedRealEqType.value === 'CONVEYOR' || computedRealEqType.value === 'LIFTER') {
        loadCvDetail();
      }
    },
    { immediate: true },
  );

  // ============================================
  // 페이지 UI 로컬 상태
  // ============================================
  const localPageName = ref('');
  const localCanvasWidth = ref(1920);
  const localCanvasHeight = ref(1080);
  const localBackgroundColor = ref('#FFFFFF');

  watch(
    () => props.page,
    (p) => {
      if (!p) return;
      localPageName.value = (p as any).pageName ?? '';
      localCanvasWidth.value = (p as any).canvasWidth ?? 1920;
      localCanvasHeight.value = (p as any).canvasHeight ?? 1080;
      localBackgroundColor.value = (p as any).backgroundColor ?? '#FFFFFF';
    },
    { immediate: true },
  );

  const onChangePageName = (v: string) => (localPageName.value = v);

  const commitPageName = () => {
    const p = props.page;
    if (!p) return;
    const next = (localPageName.value || '').trim();
    if (!next) return;
    if (((p as any).pageName ?? '') === next) return;
    emit('update-page-name', { id: (p as any).id, pageName: next });
  };

  const onChangeCanvasWidth = (v: string) =>
    (localCanvasWidth.value = Math.max(100, parseInt(v || '0', 10) || 0));
  const onChangeCanvasHeight = (v: string) =>
    (localCanvasHeight.value = Math.max(100, parseInt(v || '0', 10) || 0));
  const onChangeBackgroundColor = (v: string) => (localBackgroundColor.value = v || '#FFFFFF');

  const commitCanvas = () => {
    if (!props.page) return;
    const w = Number.isFinite(localCanvasWidth.value) ? localCanvasWidth.value : 1920;
    const h = Number.isFinite(localCanvasHeight.value) ? localCanvasHeight.value : 1080;
    const bg = localBackgroundColor.value || '#FFFFFF';
    emit('update-page-canvas', w, h, bg);
  };

  const resetBackgroundColor = () => {
    localBackgroundColor.value = '#FFFFFF';
    commitCanvas();
  };

  // ============================================
  // 멀티 선택 안전 속성
  // ============================================
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

  const updateProperty = (property: string, value: any) => {
    const ids = props.selectedObjectIds || [];
    const isMulti = ids.length > 1;

    if (isMulti && BULK_SAFE_PROPS.has(property)) {
      emit('update-properties', ids, property, value);
      return;
    }
    emit('update-property', property, value);
  };

  const intersects = (a: TbEcs2dItem, b: TbEcs2dItem) => {
    const aLeft = a.posX || 0;
    const aRight = aLeft + (a.width || 0);
    const aBottom = a.posY || 0;
    const aTop = aBottom + (a.height || 0);

    const bLeft = b.posX || 0;
    const bRight = bLeft + (b.width || 0);
    const bBottom = b.posY || 0;
    const bTop = bBottom + (b.height || 0);

    return !(aRight <= bLeft || aLeft >= bRight || aTop <= bBottom || aBottom >= bTop);
  };

  const overlappedObjects = computed(() => {
    const current = props.selectedObject;
    if (!current) return [];

    return [...(props.objects || [])]
        .filter((obj) => obj.id && obj.id !== current.id)
        .filter((obj) => objectsIntersect(current, obj))   // ✅ 변경
        .sort((a, b) => (b.zIndex || 0) - (a.zIndex || 0));
  });

  const stackedObjects = computed(() => {
    const current = props.selectedObject;
    if (!current) return [];
    return [current, ...overlappedObjects.value];
  });

  const selectFromStack = (id: string) => {
    emit('select-object', id);
  };
</script>

<style scoped>
  /* 패널 안에서 padding/border 때문에 폭이 튀는 현상 방지 */
  .right-panel,
  .right-panel * {
    box-sizing: border-box;
  }

  .right-panel {
    flex: 0 0 320px;
    width: 320px;
    min-width: 320px;

    height: 100%;
    min-height: 0;
    display: flex;
    flex-direction: column;

    overflow: hidden;
    background: #fff;
    position: relative;
    z-index: 2;

    --section-gap: 14px;
    --row-gap: 30px;
    --row-gap-compact: 8px;

    --label-w: 84px;
    --input-h: 32px;
  }

  .panel-header {
    flex-shrink: 0;
    padding: 12px 16px;
    border-bottom: 1px solid #ebeef5;

    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .panel-header h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .danger-btn {
    padding: 6px 10px;
    border: none;
    border-radius: 6px;
    font-size: 12px;
    cursor: pointer;
    background: #f56c6c;
    color: #fff;
  }
  .danger-btn:hover {
    background: #f78989;
  }

  /* 탭 바 */
  .tabbar {
    flex-shrink: 0;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    padding: 10px 12px;
    border-bottom: 1px solid #ebeef5;
    background: #fff;
  }

  .tab {
    padding: 6px 10px;
    border: 1px solid #dcdfe6;
    border-radius: 999px;
    background: #fff;
    font-size: 12px;
    cursor: pointer;
    color: #606266;
  }
  .tab.active {
    border-color: #409eff;
    color: #409eff;
    background: rgba(64, 158, 255, 0.06);
  }

  .panel-scroll {
    flex: 1;
    min-height: 0;
    height: 0;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 12px;
    overscroll-behavior: contain;
    -webkit-overflow-scrolling: touch;
  }

  .object-tabs {
    min-height: 0;
  }

  .empty-state {
    padding: 16px 8px;
    text-align: center;
    color: #909399;
    font-size: 13px;
  }
  .empty-state.hint {
    padding-top: 8px;
    padding-bottom: 0;
    text-align: left;
    color: #b0b4bb;
  }

  .property-section {
    display: flex;
    flex-direction: column;
    gap: var(--row-gap);

    margin-bottom: var(--section-gap);
    padding-bottom: var(--section-gap);
    border-bottom: 1px solid #ebeef5;
  }
  .property-section:last-child {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }

  .section-title {
    font-size: 12px;
    font-weight: 600;
    color: #909399;
    text-transform: uppercase;
    margin-bottom: 2px;
  }

  .subsection-title {
    font-size: 11px;
    font-weight: 600;
    color: #475569;
    margin: 12px 0 6px;
    border-bottom: 1px dashed #e5e7eb;
    padding-bottom: 4px;
  }

  .property-row select {
    flex: 1;
    padding: 4px 6px;
    border: 1px solid #d1d5db;
    border-radius: 3px;
    font-size: 12px;
    background: #fff;
  }

  .property-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0;
  }
  .property-row label {
    flex: 0 0 var(--label-w);
    font-size: 13px;
    color: #606266;
    line-height: 1.2;
  }

  .property-row input[type='text'],
  .property-row input[type='number'] {
    flex: 1;
    height: var(--input-h);
    padding: 6px 8px;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    font-size: 13px;
  }

  .property-row input[type='color'] {
    width: 38px;
    height: var(--input-h);
    padding: 2px;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    cursor: pointer;
  }

  .property-row.checkbox {
    justify-content: space-between;
  }
  .property-row.checkbox label {
    flex: 1 1 auto;
  }
  .property-row.checkbox input[type='checkbox'] {
    width: 16px;
    height: 16px;
    cursor: pointer;
  }

  .property-row.readonly input {
    background-color: #f5f7fa;
    color: #909399;
  }

  .property-grid {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    column-gap: 12px;
    row-gap: var(--row-gap);
  }
  .property-grid .property-row {
    flex-direction: column;
    align-items: stretch;
    gap: 6px;
    min-width: 0;
  }
  .property-grid .property-row input {
    width: 100%;
    min-width: 0;
  }
  .property-grid .property-row label {
    flex: none;
    width: auto;
  }

  .clear-btn {
    margin-left: 8px;
    padding: 4px 8px;
    background-color: #f5f7fa;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    font-size: 11px;
    cursor: pointer;
  }
  .clear-btn:hover {
    background-color: #e4e7ed;
  }

  .eq-select {
    flex: 1;
    height: var(--input-h);
    padding: 6px 8px;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    font-size: 13px;
    background: #fff;
    cursor: pointer;
  }
  .eq-select:focus {
    outline: none;
    border-color: #409eff;
  }
  .has-mapping {
    color: #67c23a !important;
    font-weight: 500;
  }
  .selected-eq-info {
    background: #f5f7fa;
    border-radius: 6px;
    padding: 10px 12px;
    margin: 8px 0;
  }
  .info-row {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    margin-bottom: 4px;
  }
  .info-row:last-child {
    margin-bottom: 0;
  }
  .info-label {
    color: #909399;
  }
  .info-value {
    color: #303133;
    font-weight: 500;
  }
  .mapping-actions {
    display: flex;
    gap: 8px;
    margin-top: 12px;
  }
  .mapping-loading,
  .mapping-error {
    padding: 12px;
    text-align: center;
    font-size: 13px;
    border-radius: 6px;
  }
  .mapping-loading {
    background: #f5f7fa;
    color: #909399;
  }
  .mapping-error {
    background: #fef0f0;
    color: #f56c6c;
  }
  .mapping-blocked {
    padding: 12px;
    text-align: center;
    font-size: 12px;
    line-height: 1.6;
    background: #fdf6ec;
    color: #e6a23c;
    border-radius: 6px;
    margin: 8px 0;
  }
  .mapping-blocked small {
    display: block;
    margin-top: 6px;
    color: #909399;
    font-size: 11px;
  }
  .radio-group {
    display: flex;
    gap: 12px;
  }
  .radio-label {
    display: flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    font-size: 13px;
    color: #606266;
  }
  .radio-label input[type='radio'] {
    margin: 0;
    cursor: pointer;
  }

  .manual-input {
    width: 100%;
    padding: 8px 10px;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    font-size: 13px;
  }
  .manual-input:focus {
    border-color: #409eff;
    outline: none;
  }

  .input-hint {
    font-size: 11px;
    color: #909399;
    margin-top: 4px;
  }
  .empty-hint {
    font-size: 12px;
    color: #e6a23c;
    background: #fdf6ec;
    padding: 8px;
    border-radius: 4px;
    margin-bottom: 8px;
  }

  .btn {
    padding: 8px 16px;
    border: none;
    border-radius: 6px;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;
  }
  .btn-sm {
    padding: 6px 12px;
    font-size: 12px;
  }
  .btn-primary {
    background: #409eff;
    color: #fff;
  }
  .btn-primary:hover:not(:disabled) {
    background: #66b1ff;
  }
  .btn-primary:disabled {
    background: #a0cfff;
    cursor: not-allowed;
  }
  .btn-secondary {
    background: #f5f7fa;
    color: #606266;
    border: 1px solid #dcdfe6;
  }
  .btn-secondary:hover {
    background: #e4e7ed;
  }
  .btn-danger {
    background: #f56c6c;
    color: #fff;
  }
  .btn-danger:hover {
    background: #f78989;
  }

  @media (max-height: 760px) {
    .right-panel {
      --section-gap: 10px;
      --row-gap: var(--row-gap-compact);
      --input-h: 30px;
    }

    .panel-scroll {
      padding: 10px;
    }

    .tabbar {
      padding: 8px 10px;
    }

    .tab {
      padding: 5px 9px;
    }

    .property-row label {
      font-size: 12px;
    }

    .property-row input[type='text'],
    .property-row input[type='number'] {
      font-size: 12px;
    }
  }

  .stacked-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .stacked-item {
    width: 100%;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    background: #fff;
    padding: 10px 12px;
    text-align: left;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .stacked-item:hover {
    border-color: #409eff;
    background: #f5f9ff;
  }

  .stacked-item.active {
    border-color: #409eff;
    background: #ecf5ff;
    box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.15) inset;
  }

  .stacked-main {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    font-size: 12px;
    font-weight: 600;
  }

  .stacked-type {
    color: #409eff;
  }

  .stacked-code {
    color: #303133;
    word-break: break-all;
  }

  .stacked-sub {
    margin-top: 4px;
    font-size: 11px;
    color: #909399;
  }
</style>
