<template>
  <div
    style="position: absolute; bottom: 100px; left: 20px; width: 340px; max-height: 60vh; display: flex; flex-direction: column; background: rgba(30, 30, 30, 0.95); border: 1px solid #444; color: white; border-radius: 8px; z-index: 10; box-shadow: 0 4px 15px rgba(0,0,0,0.5);"
    @pointerdown.stop
    @pointermove.stop
    @pointerup.stop
  >
    <div style="padding: 12px 15px; border-bottom: 1px solid #555; display: flex; justify-content: space-between; align-items: center;">
      <h3 style="margin: 0; color: #f39c12; font-size: 16px;">입고 위치 시뮬레이션</h3>

      <div style="display: flex; gap: 8px;">
        <button
          @click="openSettingPanel"
          style="padding: 4px 10px; background: #444; color: #fff; font-size: 12px; border: 1px solid #666; border-radius: 4px; cursor: pointer;"
        >
          설정
        </button>

        <button
          @click="onReset"
          style="padding: 4px 10px; background: #555; color: #fff; font-size: 12px; border: none; border-radius: 4px; cursor: pointer;"
        >
          초기화
        </button>
      </div>

      <!-- Inventory 모듈 설정 관리 UI -->
      <InventorySettingPanel
        ref="settingPanel"
      />
    </div>

    <div style="flex: 1; overflow-y: auto; padding: 15px; font-size: 13px;">

      <div style="margin-bottom: 12px; display: flex; align-items: center;">
        <label style="width: 100px; color: #bbb;">Loc Group</label>
        <input type="text" v-model="formData.locGroup" placeholder="선택 입력" style="flex: 1; padding: 4px 8px; background: #333; color: white; border: 1px solid #555; border-radius: 4px;" />
      </div>

      <div style="margin-bottom: 12px; display: flex; align-items: center;">
        <label style="width: 100px; color: #bbb;">Item Type <span style="color:#d9534f">*</span></label>
        <input type="text" v-model="formData.itemType" placeholder="필수" style="flex: 1; padding: 4px 8px; background: #333; color: white; border: 1px solid #555; border-radius: 4px;" />
      </div>

      <div style="margin-bottom: 12px; display: flex; align-items: center;">
        <label style="width: 100px; color: #bbb;">Total Weight <span style="color:#d9534f">*</span></label>
        <input type="number" v-model.number="formData.totalWeight" placeholder="필수" style="flex: 1; padding: 4px 8px; background: #333; color: white; border: 1px solid #555; border-radius: 4px;" />
      </div>

      <div style="margin-bottom: 12px; display: flex; align-items: center;">
        <label style="width: 100px; color: #bbb;">Total Height <span style="color:#d9534f">*</span></label>
        <input type="number" v-model.number="formData.totalHeight" placeholder="필수" style="flex: 1; padding: 4px 8px; background: #333; color: white; border: 1px solid #555; border-radius: 4px;" />
      </div>

      <div style="margin-top: 20px; border-top: 1px dashed #555; padding-top: 15px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
          <label style="color: #bbb; font-weight: bold;">자재 목록 (Item List) <span style="color:#d9534f">*</span></label>
          <button @click="addItem" style="padding: 2px 8px; background: #42b883; color: #1e1e1e; font-weight: bold; border: none; border-radius: 4px; cursor: pointer;">
            + 추가
          </button>
        </div>

        <div v-for="(item, index) in formData.itemList" :key="index" style="display: flex; gap: 5px; margin-bottom: 8px;">
          <input type="text" v-model="item.itemOwner" placeholder="Owner (화주)" style="width: 40%; padding: 4px; background: #222; color: white; border: 1px solid #555; border-radius: 4px;" />
          <input type="text" v-model="item.itemCode" placeholder="Code (품번)" style="flex: 1; padding: 4px; background: #222; color: white; border: 1px solid #555; border-radius: 4px;" />
          <button v-if="formData.itemList.length > 1" @click="removeItem(index)" style="padding: 4px 8px; background: #d9534f; color: white; border: none; border-radius: 4px; cursor: pointer;">
            X
          </button>
        </div>
      </div>

    </div>

    <div style="padding: 12px 15px; border-top: 1px solid #555;">
      <button
        @click="onSimulate"
        style="width: 100%; padding: 8px; background: #f39c12; color: white; font-weight: bold; border: none; border-radius: 4px; cursor: pointer;"
      >
        할당 시뮬레이션 실행
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import InventorySettingPanel from './InventorySettingPanel.vue';

const emit = defineEmits(['simulate']);
const settingPanel = ref(null);

// DTO 구조
const getInitialData = () => ({
  locGroup: null,
  itemType: null,
  totalWeight: null,
  totalHeight: null,
  itemList: [
    { itemOwner: null, itemCode: null } // 기본 1줄은 존재하도록 세팅
  ]
});

const formData = ref(getInitialData());

const onReset = () => {
  formData.value = getInitialData();
};

const addItem = () => {
  formData.value.itemList.push({ itemOwner: null, itemCode: null });
};

const removeItem = (index) => {
  formData.value.itemList.splice(index, 1);
};

const onSimulate = () => {
  const data = formData.value;

  // 필수값 검증 (locGroup은 제외)
  if (!data.itemType || data.totalWeight === null || data.totalHeight === null) {
    alert("Item Type, Total Weight, Total Height는 필수 입력입니다.");
    return;
  }

  // itemList 검증
  const isItemListValid = data.itemList.every(item => item.itemOwner && item.itemCode);
  if (!isItemListValid) {
    alert("추가된 자재의 Owner와 Code를 모두 입력해주세요.");
    return;
  }

  // 부모에게 Payload 전달
  emit('simulate', JSON.parse(JSON.stringify(data)));
};

// Inventory 모듈 설정 관리 Panel 열기
const openSettingPanel = () => {
  if (settingPanel.value) {
    settingPanel.value.openPanel();
  }
};
</script>
