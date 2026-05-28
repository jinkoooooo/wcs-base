<template>
  <div class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <h2>센터 선택</h2>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label for="lcId">센터 코드 (LC ID)</label>
          <input
            id="lcId"
            v-model="inputLcId"
            type="text"
            placeholder="센터 코드를 입력하세요"
            @keyup.enter="submit"
          />
        </div>
        <div class="hint-text"> 예: LC001, CENTER_A, WAREHOUSE_1 </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-primary" @click="submit" :disabled="!inputLcId.trim()">
          확인
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';

  const emit = defineEmits<{
    (e: 'submit', lcId: string): void;
    (e: 'close'): void;
  }>();

  const inputLcId = ref('');

  const submit = () => {
    const trimmed = inputLcId.value.trim();
    if (trimmed) {
      emit('submit', trimmed);
    }
  };

  const close = () => {
    emit('close');
  };
</script>

<style scoped>
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
  }

  .modal-content {
    background: white;
    border-radius: 8px;
    padding: 24px;
    min-width: 400px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  }

  .modal-header {
    margin-bottom: 20px;
  }

  .modal-header h2 {
    margin: 0;
    font-size: 1.25rem;
    color: #303133;
  }

  .modal-body {
    margin-bottom: 20px;
  }

  .form-group {
    margin-bottom: 12px;
  }

  .form-group label {
    display: block;
    margin-bottom: 8px;
    font-weight: 500;
    color: #606266;
  }

  .form-group input {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 14px;
    box-sizing: border-box;
  }

  .form-group input:focus {
    outline: none;
    border-color: #409eff;
  }

  .hint-text {
    font-size: 12px;
    color: #909399;
  }

  .modal-footer {
    display: flex;
    justify-content: flex-end;
  }

  .btn {
    padding: 10px 20px;
    border: none;
    border-radius: 4px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-primary {
    background-color: #409eff;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background-color: #66b1ff;
  }

  .btn-primary:disabled {
    background-color: #a0cfff;
    cursor: not-allowed;
  }
</style>
