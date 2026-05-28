<template>
  <div class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <h2>센터 선택</h2>
        <button class="close-btn" @click="close">&times;</button>
      </div>

      <div class="modal-body">
        <div class="form-group">
          <label>센터 코드 (LC ID)</label>
          <input v-model="inputLcId" type="text" placeholder="예: LC001" @keyup.enter="submit" />
        </div>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-secondary" @click="close">취소</button>
        <button class="btn btn-primary" @click="submit" :disabled="!canSubmit">선택</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed } from 'vue';

  const emit = defineEmits<{
    (e: 'submit', data: { lcId: string }): void;
    (e: 'close'): void;
  }>();

  const props = defineProps<{
    initialLcId?: string;
  }>();

  const inputLcId = ref(props.initialLcId || '');
  const errorMessage = ref('');

  const canSubmit = computed(() => !!inputLcId.value.trim());

  const submit = () => {
    const lcId = inputLcId.value.trim();
    if (!lcId) return;

    errorMessage.value = '';
    emit('submit', { lcId });
  };

  const close = () => emit('close');
</script>

<style scoped>
  .modal-overlay {
    position: fixed;
    inset: 0;
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
    min-width: 420px;
    max-width: 520px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  }
  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
  .modal-header h2 {
    margin: 0;
    font-size: 1.2rem;
    color: #303133;
  }
  .close-btn {
    background: none;
    border: none;
    font-size: 24px;
    color: #909399;
    cursor: pointer;
    padding: 0;
    line-height: 1;
  }
  .close-btn:hover {
    color: #606266;
  }

  .modal-body {
    margin-bottom: 18px;
  }

  .form-group {
    margin-bottom: 12px;
  }
  .form-group label {
    display: block;
    margin-bottom: 8px;
    font-weight: 600;
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

  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }

  .btn {
    padding: 10px 18px;
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
  .btn-secondary {
    background-color: #f5f7fa;
    color: #606266;
    border: 1px solid #dcdfe6;
  }
  .btn-secondary:hover {
    background-color: #e4e7ed;
  }

  .error-message {
    padding: 10px 12px;
    background-color: #fef0f0;
    border: 1px solid #fbc4c4;
    border-radius: 4px;
    color: #f56c6c;
    font-size: 13px;
  }
</style>
