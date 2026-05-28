import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

/**
 * 화면 width 기반 반응형 분기 composable
 */
export function useResponsiveLayout() {
  const width = ref<number>(typeof window !== 'undefined' ? window.innerWidth : 1920);

  function handleResize() {
    width.value = window.innerWidth;
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize);
  });

  const isMobile = computed(() => width.value <= 960);
  const isTablet = computed(() => width.value <= 1280);
  const isDesktop = computed(() => width.value > 1280);

  return {
    width,
    isMobile,
    isTablet,
    isDesktop,
  };
}
