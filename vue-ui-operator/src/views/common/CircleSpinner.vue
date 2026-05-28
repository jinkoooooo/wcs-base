<!-- 스피너 -->
<template>
  <div ref="modalRef">
    <ion-spinner ref="spinerRef" color="primary"></ion-spinner>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { IonSpinner } from "@ionic/vue";
const modalRef = ref();
const getModal = () => modalRef.value;
const spinerRef = ref();
const getSpinner = () => spinerRef.value as typeof IonSpinner;

onMounted(() => {
  window.circleSpinner = getSpinner();

  document.addEventListener("toggle-spinner", (event: CustomEvent) => {
    let active = event.detail.active;
    getSpinner().active = active;

    if (active) {
      getModal().setAttribute("active", "");
    } else {
      getModal().removeAttribute("active");
    }
  });
});
</script>

<style scoped>
div {
  display: none;
  position: absolute;
  height: 100%;
  width: 100%;
}
div[active] {
  display: grid;
  z-index: 100;
}
ion-spinner {
  margin: auto;
}
</style>
