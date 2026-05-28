<template>
  <Button
    v-for="button in props.buttonlist"
    :key="button.text"
    @click="btnHandler(button.listener)"
    :type="button.auth === 'delete' ? 'dashed' : 'primary'"
    :danger="button.auth === 'delete'"
    :hidden="button.hidden === 1"
    :shape="getButtonShape(button.auth)"
  >
    <template #icon v-if="button.auth === 'export'">
      <DownloadOutlined />
    </template>
    <template #icon v-if="button.auth === 'import'">
      <UploadOutlined />
    </template>

    {{ t(button.text) }}
  </Button>
</template>

<script lang="ts" setup>
  import { Button } from 'ant-design-vue';
  import { DownloadOutlined, UploadOutlined } from '@ant-design/icons-vue';
  import { useI18n } from '/@/hooks/web/useI18n';

  const { t } = useI18n();

  const props = defineProps({
    buttonlist: {
      type: Array,
      required: true,
    },
    emits: ['btnHandler'],
  });

  /**
   * Helper functions
   */
  const getButtonType = (auth: string) => {
    return auth === 'delete' ? 'dashed' : 'primary';
  };

  const getButtonShape = (auth: string) => {
    return auth === 'export' ? 'round' : 'default';
  };
  const emit = defineEmits(['btnHandler']);

  function btnHandler(e) {
    emit('btnHandler', e);
  }
</script>
