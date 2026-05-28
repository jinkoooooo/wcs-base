<template>
  <ion-app>
    <common-dialog></common-dialog>
    <ion-split-pane content-id="main-content" :when="false">
      <CircleSpinner></CircleSpinner>
      <SysMenu />
      <ion-router-outlet id="main-content" />
    </ion-split-pane>
  </ion-app>
</template>

<script lang="ts" setup>
import { IonApp, IonSplitPane, IonRouterOutlet } from "@ionic/vue";
import SysMenu from "@/views/common/SysMenu.vue";
import CircleSpinner from "@/views/common/CircleSpinner.vue";
</script>
<style scoped>
.hide-menu {
  visibility: hidden;
}

ion-spinner {
  width: 100px;
  height: 100px;
}
</style>
