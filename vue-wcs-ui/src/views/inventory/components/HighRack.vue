<template>
  <TresGroup
    :position="position"
    :rotation="[0, rotationY, 0]"
    :scale="[0.08, 0.08, 0.08]"
    @click="onClick"
  >
    <!-- Pallet 모델 -->
    <primitive
      :object="clonePallet"
      :scale="[0.07, 0.07, 0.07]"
    />

    <!-- 박스 모델 -->
    <primitive
      v-if="isStock"
      :object="clonedBox"
      :position="[0, 5, 0]"
      :scale="[0.8, 0.8, 0.8]"
    />

    <!-- 객체 선택 시 표시 -->
    <TresMesh v-if="isSelected" :position="[0, 5, 0]">
      <TresBoxGeometry :args="[12, 12, 12]" />
      <TresMeshBasicMaterial color="#00ff00" :transparent="true" :opacity="0.4" />
    </TresMesh>
  </TresGroup>
</template>

<script setup>
import { shallowRef } from 'vue';

const props = defineProps({
  position: Array,
  rotationY: {
    type: Number,
    default: 0
  },
  code: String,
  isStock: Boolean,
  isSelected: Boolean,
  boxModel: Object,
  palletModel: Object
})

const emit = defineEmits(['select'])

// 개별 클릭 시 실행 (Ctrl 키 입력 여부도 함께 부모로 넘김)
const onClick = (e) => {
  // 1. 이벤트 전파 중지 (클릭한 랙 뒤에 있는 다른 랙이 같이 선택되는 것 방지)
  if (typeof e.stopPropagation === 'function') {
    e.stopPropagation();
  }

  // 2. TresJS 버전에 따른 Ctrl/Cmd 키 호환성 처리
  const isCtrl =
    e.ctrlKey ||
    e.metaKey ||
    (e.nativeEvent && (e.nativeEvent.ctrlKey || e.nativeEvent.metaKey)) ||
    false;

  emit('select', props.code, isCtrl);
}

const clonedBox = shallowRef(props.boxModel.clone());
const clonePallet = shallowRef(props.palletModel.clone());
</script>
