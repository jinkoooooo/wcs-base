<template>
  <div
    class="equip-agv"
    :style="{
      left: `${equip.position_x}px`,
      bottom: `${equip.position_y}px`
    }"
    @click="handleEquipClick"
  >
    <img
      src="/images/posco/agv.png"
      alt="AGV"
      draggable="false"
    />
  </div>
</template>

<script setup>
const props = defineProps({
  equip: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['equipClicked']);

const handleEquipClick = () => {
  emit('equipClicked', {
    equip_type: props.equip.equip_type,
    equip_code: props.equip.equip_code
  });
};
</script>

<style scoped>
.equip-agv {
  position: absolute;
  /* 좌표점이 AGV 이미지의 정중앙에 오도록 보정 */
  transform: translate(-50%, 50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: auto; /* 클릭 이벤트 발생을 위해 추가 */
  transition: left 1s linear, bottom 1s linear;
  will-change: left, bottom;
}

.equip-agv img {
  width: 40px; /* 적절한 크기로 조절 */
  height: auto;
  user-select: none; /* 드래그 선택 방지 */
}
</style>