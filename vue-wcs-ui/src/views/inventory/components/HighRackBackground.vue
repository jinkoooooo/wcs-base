<template>
  <TresGroup
    :position="position"
    :rotation="[0, rotationY, 0]"
    :scale="[0.25, 0.32, 0.3]"
  >
    <primitive
      :object="cloneBackground"
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
  highRackBackgroundModel: Object
})

const cloneBackground = shallowRef(props.highRackBackgroundModel.clone());
</script>
