<template>
  <div v-if="visible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content custom-scrollbar">

      <div class="modal-header">
        <h3>📂 저장된 도면 목록</h3>
        <button class="btn-close-modal" @click="closeModal">✕</button>
      </div>

      <div class="modal-body">
        <p v-if="isLoading" class="info-text">데이터를 불러오는 중입니다...</p>
        <p v-else-if="savedLayoutList.length === 0" class="info-text">저장된 도면이 없습니다.</p>

        <ul v-else class="layout-list">
          <li v-for="item in savedLayoutList" :key="item.id" @click="selectItem(item)" class="layout-item">
            <div class="layout-info">
              <span class="layout-title">{{ item.center_id || item.centerId }}</span>
              <span class="layout-subtitle">구역: {{ item.zone_id || item.zoneId }} | 버전: {{ item.layout_version || item.layoutVersion }}</span>
            </div>
            <span class="layout-date">{{ new Date(item.created_at || item.createdAt).toLocaleDateString() }}</span>
          </li>
        </ul>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
// 🔥 회원님이 짚어주신 getCommonGetApi를 확실하게 적용!
import { getCommonGetApi } from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';

const props = defineProps({
  visible: { type: Boolean, default: false }
});
const emit = defineEmits(['close', 'select']);

const { notification } = useMessage();
const savedLayoutList = ref([]);
const isLoading = ref(false);

// 모달이 열릴 때(visible=true)마다 DB에서 리스트 조회
watch(() => props.visible, async (newVal) => {
  if (newVal) {
    isLoading.value = true;
    try {
      // getCommonGetListApi 대신 getCommonGetApi 사용!
      const response = await getCommonGetApi('/layouts', {});
      const records = response.items || response.records || response;
      savedLayoutList.value = records || [];
    } catch (error) {
      console.error("List Load Error:", error);
      notification.error({ message: '에러', description: '목록을 불러오는 중 서버 오류가 발생했습니다.' });
    } finally {
      isLoading.value = false;
    }
  }
});

const closeModal = () => emit('close');
const selectItem = (item) => emit('select', item);
</script>

<style scoped>
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(15, 23, 42, 0.6); display: flex; justify-content: center; align-items: center; z-index: 9999; backdrop-filter: blur(4px); }
.modal-content { background: #ffffff; width: 500px; max-height: 80vh; border-radius: 12px; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1); display: flex; flex-direction: column; animation: modal-pop 0.3s ease-out; }
@keyframes modal-pop { 0% { transform: scale(0.95); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; border-bottom: 1px solid #e2e8f0; background: #f8fafc; }
.modal-header h3 { margin: 0; font-size: 1.1rem; color: #0f172a; }
.btn-close-modal { background: none; border: none; font-size: 1.2rem; color: #64748b; cursor: pointer; }
.btn-close-modal:hover { color: #ef4444; }
.modal-body { padding: 16px; overflow-y: auto; flex: 1; }
.info-text { text-align: center; color: #94a3b8; padding: 20px; }
.layout-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.layout-item { display: flex; justify-content: space-between; align-items: center; padding: 16px; border: 1px solid #cbd5e1; border-radius: 8px; cursor: pointer; transition: all 0.2s; background: #fff; }
.layout-item:hover { border-color: #10b981; background: #ecfdf5; transform: translateY(-2px); }
.layout-info { display: flex; flex-direction: column; gap: 4px; }
.layout-title { font-weight: 700; font-size: 1rem; color: #1e293b; }
.layout-subtitle { font-size: 0.8rem; color: #64748b; }
.layout-date { font-size: 0.8rem; color: #94a3b8; font-weight: 600; }
</style>
