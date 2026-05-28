<template>
  <div
    class="relative w-full h-[35vh] min-h-[240px] rounded-xl ring-1 ring-slate-200 bg-slate-100/40 flex items-center justify-center overflow-hidden"
  >
    <template v-if="images.length">
      <img
        :src="images[safeIndex]"
        alt="product"
        class="max-h-full max-w-full object-contain cursor-zoom-in"
        @dblclick="openFullscreen"
      />
    </template>
    <template v-else>
      <div class="text-slate-400 text-sm">이미지 없음</div>
    </template>

    <button
      class="absolute left-2 top-1/2 -translate-y-1/2 rounded-full bg-white/80 ring-1 ring-slate-300 shadow p-2 hover:bg-white disabled:opacity-50"
      @click="prev"
      :disabled="!images.length"
      aria-label="prev"
    >
      ◀
    </button>
    <button
      class="absolute right-2 top-1/2 -translate-y-1/2 rounded-full bg-white/80 ring-1 ring-slate-300 shadow p-2 hover:bg-white disabled:opacity-50"
      @click="next"
      :disabled="!images.length"
      aria-label="next"
    >
      ▶
    </button>

    <div class="absolute bottom-2 left-0 right-0 flex justify-center gap-1">
      <span
        v-for="(img, i) in images"
        :key="i"
        class="w-2.5 h-2.5 rounded-full ring-1 ring-slate-300"
        :class="i === safeIndex ? 'bg-indigo-500' : 'bg-white/80'"
      ></span>
    </div>

    <div
      v-if="isFullscreen && images.length"
      class="fixed inset-0 z-[9999] bg-black/80 flex items-center justify-center"
    >
      <div
        class="relative w-full h-full max-w-5xl max-h-[90vh] flex items-center justify-center px-6"
      >
        <img
          :src="images[safeIndex]"
          alt="product large"
          class="max-h-full max-w-full object-contain cursor-zoom-out"
          @dblclick="closeFullscreen"
        />

        <button
          class="absolute right-6 top-6 rounded-full bg-white/90 ring-1 ring-slate-300 shadow px-3 py-1 text-sm font-semibold hover:bg-white"
          @click="closeFullscreen"
          aria-label="close fullscreen"
        >
          ✕
        </button>

        <button
          class="absolute left-6 top-1/2 -translate-y-1/2 rounded-full bg-white/90 ring-1 ring-slate-300 shadow p-3 hover:bg-white disabled:opacity-50"
          @click.stop="prev"
          :disabled="!images.length"
          aria-label="prev fullscreen"
        >
          ◀
        </button>
        <button
          class="absolute right-6 top-1/2 -translate-y-1/2 rounded-full bg-white/90 ring-1 ring-slate-300 shadow p-3 hover:bg-white disabled:opacity-50"
          @click.stop="next"
          :disabled="!images.length"
          aria-label="next fullscreen"
        >
          ▶
        </button>

        <div class="absolute bottom-6 left-0 right-0 flex justify-center gap-1">
          <span
            v-for="(img, i) in images"
            :key="'fs-' + i"
            class="w-2.5 h-2.5 rounded-full ring-1 ring-slate-300"
            :class="i === safeIndex ? 'bg-indigo-500' : 'bg-white/80'"
          ></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

const props = defineProps<{
  images: string[];
  index?: number;
  max?: number;
}>();

const emit = defineEmits<{
  (e: 'update:index', v: number): void;
}>();

const isFullscreen = ref(false);

const safeIndex = computed(() => {
  const len = props.images?.length ?? 0;
  const idx = props.index ?? 0;
  if (!len) return 0;
  return ((idx % len) + len) % len;
});

function prev() {
  const len = props.images.length;
  if (!len) return;
  emit('update:index', (safeIndex.value - 1 + len) % len);
}
function next() {
  const len = props.images.length;
  if (!len) return;
  emit('update:index', (safeIndex.value + 1) % len);
}

function openFullscreen() {
  if (!props.images.length) return;
  isFullscreen.value = true;
}

function closeFullscreen() {
  isFullscreen.value = false;
}
</script>
