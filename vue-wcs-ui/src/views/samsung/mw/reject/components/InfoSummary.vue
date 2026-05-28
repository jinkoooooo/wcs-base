<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
    <div
      v-for="f in infoFields"
      :key="f.key"
      class="rounded-lg ring-1 ring-slate-200 p-3 bg-slate-50/50 flex items-center justify-between"
    >
      <!-- 라벨 -->
      <div class="text-slate-500 text-sm">
        {{ f.label }}
      </div>

      <!-- 값 -->
      <div class="font-semibold text-slate-800 text-sm text-right tabular-nums">
        {{
          info[f.key] !== undefined && info[f.key] !== null && info[f.key] !== ''
            ? info[f.key]
            : '-'
        }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  defineProps<{
    info: {
      itemCode?: string;
      itemName?: string;
      serialNo?: string;
      box_length?: string;
      box_height?: string;
      box_width?: string;
      box_angle?: string;
      real_box_length?: string;
      real_box_height?: string;
      real_box_width?: string;
    };
  }>();

  const infoFields = [
    { label: '상품 바코드', key: 'itemCode' },
    { label: '상품 이름', key: 'itemName' },
    { label: '상품 시리얼', key: 'serialNo' },
    { label: '체적 각도(Angle)', key: 'box_angle' },
    { label: '체적 길이(Length)', key: 'box_length' },
    { label: '기준 길이(Length)', key: 'real_box_length' },
    { label: '체적 높이(Height)', key: 'box_height' },
    { label: '기준 높이(Height)', key: 'real_box_height' },
    { label: '체적 폭(Width)', key: 'box_width' },
    { label: '기준 폭(Width)', key: 'real_box_width' },
  ];
</script>
