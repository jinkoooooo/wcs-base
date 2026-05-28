<template>
  <div class="setting-modal-overlay" v-if="isVisible" @click.self="closePanel">
    <div class="setting-modal-content">
      <div class="modal-header">
        <h3>⚙️ 로케이션 할당 설정</h3>
      </div>

      <div class="modal-body">
        <table class="setting-table">
          <colgroup>
            <col width="60%" />
            <col width="40%" />
          </colgroup>
          <thead>
          <tr>
            <th>설정 항목 (Description)</th>
            <th>설정 값 (Value)</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="setting in settings" :key="setting.id">
            <td>{{ setting.optionDescription }}</td>
            <td>
              <select
                v-if="setting.optionName === 'DISTANCE_STANDARD'"
                v-model="setting.optionValue"
                class="setting-select"
              >
                <option value="EUCLID">EUCLID (직선 거리)</option>
                <option value="MANHATTAN">MANHATTAN (격자 거리)</option>
                <option value="CHEBYSHEV">CHEBYSHEV (동시 구동)</option>
              </select>

              <select
                v-else
                v-model="setting.optionValue"
                class="setting-select"
              >
                <option value="true">True</option>
                <option value="false">False</option>
              </select>
            </td>
          </tr>
          <tr v-if="!settings || settings.length === 0">
            <td colspan="2" style="text-align: center; color: #888; padding: 20px;">
              설정 데이터를 불러오는 중이거나 데이터가 없습니다.
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="modal-footer">
        <button class="btn save-btn" @click="saveSettings">저장</button>
        <button class="btn close-btn" @click="closePanel">닫기</button>
      </div>
    </div>
  </div>
</template>

<script>
import { getCommonGetApi, getCommonPostApi } from '/@/api/common/api';

export default {
  name: 'InventorySettingPanel',
  data() {
    return {
      isVisible: false,
      settings: [],
      originalSettings: []
    };
  },
  methods: {
    async openPanel() {
      this.isVisible = true;
      await this.fetchSettings();
    },

    closePanel() {
      this.isVisible = false;
      this.settings = [];
    },

    async fetchSettings() {
      try {
        // 설정 목록 조회
        const response = await getCommonGetApi('/inventory_setting/get_total_setting', null);

        // snake_case -> camelCase로 객체 키 변환 후 할당
        const mappedData = response.map(item => ({
          id: item.id,
          optionName: item.option_name,
          optionValue: item.option_value,
          optionDescription: item.option_description
        }));

        // 설정 목록 할당
        this.settings = mappedData;

        // 원본 데이터를 복사하여 보관 (추후 변경점 비교용)
        this.originalSettings = JSON.parse(JSON.stringify(mappedData));

      } catch (error) {
        console.error('❌ 설정 정보를 불러오는데 실패했습니다.', error);
        alert('설정 정보를 불러오는데 실패했습니다. 콘솔을 확인해주세요.');
      }
    },

    async saveSettings() {
      // 원본 데이터와 현재 데이터를 비교하여 변경된 항목만 추출
      const changedSettings = this.settings.filter(setting => {
        const original = this.originalSettings.find(orig => orig.id === setting.id);
        return original && original.optionValue !== setting.optionValue;
      });

      // 변경된 항목이 없으면 API 호출 없이 종료
      if (changedSettings.length === 0) {
        alert('변경된 설정이 없습니다.');
        return;
      }

      if (!confirm('설정을 저장하시겠습니까?')) return;

      try {
        // 변경된 설정만 camelCase -> snake_case로 객체 키 변환 후 할당
        const param = changedSettings.map(item => ({
          option_name: item.optionName,
          option_value: item.optionValue
        }))

        await getCommonPostApi('/inventory_setting/set_option_value', param);

        alert('설정이 성공적으로 저장되었습니다.');
        this.closePanel();

      } catch (error) {
        console.error('❌ 설정 저장 중 오류가 발생했습니다.', error);
        alert('설정 저장 중 오류가 발생했습니다.');
      }
    }
  }
};
</script>

<style scoped>
/* 모달 배경 오버레이 */
.setting-modal-overlay {
  position: fixed;
  top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

/* 모달 컨텐츠 박스 (다크 테마 적용) */
.setting-modal-content {
  background: rgba(30, 30, 30, 0.95);
  border: 1px solid #444;
  color: white;
  width: 550px;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 15px 20px;
  border-bottom: 1px solid #555;
  border-radius: 8px 8px 0 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  color: #f39c12;
}

.modal-body {
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
}

.setting-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.setting-table th, .setting-table td {
  border: 1px solid #555;
  padding: 10px 12px;
  text-align: left;
}

.setting-table th {
  background-color: #2a2a2a;
  color: #bbb;
  font-weight: bold;
}

.setting-table td {
  background-color: #1e1e1e;
}

.setting-select {
  width: 100%;
  padding: 6px 8px;
  background: #333;
  color: white;
  border: 1px solid #555;
  border-radius: 4px;
  outline: none;
}

.setting-select:focus {
  border-color: #f39c12;
}

.modal-footer {
  padding: 12px 20px;
  border-top: 1px solid #555;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  font-size: 13px;
  transition: all 0.2s;
}

.save-btn {
  background-color: #f39c12;
  color: white;
}
.save-btn:hover { background-color: #d68910; }

.close-btn {
  background-color: #555;
  color: white;
}
.close-btn:hover { background-color: #666; }

.modal-body::-webkit-scrollbar {
  width: 8px;
}
.modal-body::-webkit-scrollbar-track {
  background: #1e1e1e;
}
.modal-body::-webkit-scrollbar-thumb {
  background: #555;
  border-radius: 4px;
}
.modal-body::-webkit-scrollbar-thumb:hover {
  background: #777;
}
</style>
