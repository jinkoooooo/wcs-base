<template>
  <div v-if="visible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content gen-modal">

      <div class="modal-header">
        <h3>🏢 로케이션 자동 생성</h3>
        <button class="btn-close" @click="closeModal">✕</button>
      </div>

      <div class="modal-body">
        <p class="help-text">
          선택한 레이아웃 도면에 동기화된 프로파일을 기반으로<br>
          백엔드(WCS Core)에서 로케이션 마스터를 일괄 생성합니다.
        </p>

        <div class="section-box">
          <h4>1. 기준 도면 선택</h4>
          <select v-model="selectedLayoutId" @change="onLayoutSelect" class="modern-select">
            <option value="" disabled>저장된 레이아웃을 선택해주세요</option>
            <option v-for="layout in savedLayoutList" :key="layout.id" :value="layout.id">
              [{{ layout.center_id || layout.centerId }}] {{ layout.zone_id || layout.zoneId }} (Ver: {{ layout.layout_version || layout.layoutVersion }})
            </option>
          </select>
        </div>

        <div v-if="selectedLayoutId" class="section-box result-box">
          <h4>2. 적용될 타겟 정보</h4>
          <ul>
            <li><strong>Area Code :</strong> <span class="highlight">{{ targetData.areaCode }}</span></li>
            <li><strong>Profile Code :</strong> <span class="highlight">{{ targetData.profileCode }}</span></li>
          </ul>
          <small>※ 위 정보는 도면 저장 시 생성된 자동 매핑 규칙을 따릅니다.</small>
        </div>

      </div>

      <div class="modal-footer">
        <button class="btn-cancel" @click="closeModal">취소</button>
        <button class="btn-confirm" @click="executeGeneration" :disabled="!selectedLayoutId || isGenerating">
          {{ isGenerating ? '⏳ WCS 코어 실행 중...' : '확인 및 생성 실행' }}
        </button>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';
import { getCommonGetApi, updateList } from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';

const props = defineProps({
  visible: { type: Boolean, default: false }
});

const emit = defineEmits(['close', 'generated']);
const { notification } = useMessage();

const savedLayoutList = ref([]);
const selectedLayoutId = ref('');
const isGenerating = ref(false);

const targetData = reactive({
  centerId: '',
  areaCode: '',
  profileCode: ''
});

watch(() => props.visible, async (newVal) => {
  if (newVal) {
    selectedLayoutId.value = '';
    targetData.centerId = '';
    targetData.areaCode = '';
    targetData.profileCode = '';
    try {
      const res = await getCommonGetApi('/layouts', {});
      savedLayoutList.value = res.items || res.records || res || [];
    } catch (error) {
      notification.error({ message: '조회 실패', description: '레이아웃 목록을 불러오지 못했습니다.' });
    }
  }
});

const closeModal = () => {
  emit('close');
};

const onLayoutSelect = () => {
  const layout = savedLayoutList.value.find(l => l.id === selectedLayoutId.value);
  if (layout) {
    targetData.centerId = layout.center_id || layout.centerId;
    targetData.areaCode = layout.zone_id || layout.zoneId;
    targetData.profileCode = layout.layout_version || layout.layoutVersion || 'DEFAULT_PROFILE';
  }
};

const executeGeneration = async () => {
  isGenerating.value = true;

  try {

    const paramAreaCode = encodeURIComponent(targetData.areaCode); // 'B/FREEZING'
    const paramProfileCode = encodeURIComponent(targetData.profileCode); // 'V1.3'

    const url = `/aislecore/location/profiles/generate?areaCode=${paramAreaCode}&profileCode=${paramProfileCode}`;

    const res = await updateList(url, {});

    const createdCount = res.created_count || 0;
    const skippedCount = res.skipped_count || 0;

    notification.success({
      message: '코어 작업 완료',
      description: `[${targetData.areaCode}] 구역에 ${createdCount}건 생성 완료 (스킵: ${skippedCount}건)`
    });

    closeModal();
    emit('generated');

  } catch (error) {
    console.error("🔥 프론트엔드 전송 에러 상세:", error);
    let errMsg = '로케이션 코어 실행 중 오류가 발생했습니다.';
    if (error instanceof Error) {
      errMsg = error.message;
    } else if (error?.response?.data?.message) {
      errMsg = error.response.data.message;
    }
    notification.error({ message: '실행 실패', description: errMsg });
  } finally {
    isGenerating.value = false;
  }
};
</script> <style scoped>
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15,23,42,0.6); display: flex; justify-content: center; align-items: center; z-index: 999; backdrop-filter: blur(4px); }
.gen-modal { width: 500px; background: #fff; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; animation: pop 0.2s ease-out; }
@keyframes pop { 0% { transform: scale(0.95); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }

.modal-header { padding: 20px; display: flex; justify-content: space-between; border-bottom: 1px solid #e2e8f0; background: #f8fafc; }
.modal-header h3 { margin: 0; font-size: 1.2rem; color: #1e293b; }
.btn-close { background: none; border: none; font-size: 1.2rem; cursor: pointer; color: #64748b; }

.modal-body { padding: 20px; }
.help-text { margin-top: 0; color: #64748b; font-size: 0.9rem; margin-bottom: 20px; line-height: 1.4; }
.section-box { border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 16px; background: #fff; }
.section-box h4 { margin: 0 0 12px 0; font-size: 1rem; color: #334155; }
.result-box { background: #f8fafc; }
.result-box ul { margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 8px; }
.result-box li { font-size: 0.95rem; color: #475569; }
.result-box .highlight { font-weight: bold; color: #4f46e5; }
.result-box small { display: block; margin-top: 12px; color: #94a3b8; }

.modern-select { width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 0.95rem; outline: none; }
.modern-select:focus { border-color: #4f46e5; }

.modal-footer { padding: 20px; border-top: 1px solid #e2e8f0; display: flex; gap: 12px; background: #fff; }
.btn-cancel, .btn-confirm { flex: 1; padding: 14px; border-radius: 6px; font-weight: bold; cursor: pointer; border: none; font-size: 1rem; transition: 0.2s; }
.btn-cancel { background: #f1f5f9; color: #475569; }
.btn-cancel:hover { background: #e2e8f0; }
.btn-confirm { background: #4f46e5; color: #fff; }
.btn-confirm:hover:not(:disabled) { background: #4338ca; }
.btn-confirm:disabled { background: #94a3b8; cursor: not-allowed; }
</style>
