<template>
  <TresGroup
    :position="position"
    :rotation="[0, rotationY, 0]"
    :scale="[0.01, 0.01, 0.01]"
  >
    <primitive
      :object="cloneAgf"
    />
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
  agfModel: Object
})

const cloneAgf = shallowRef(props.agfModel.clone());
</script>
