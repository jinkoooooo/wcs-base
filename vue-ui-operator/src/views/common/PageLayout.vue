<template>
  <ion-page class="animate-fade-right animate-duration-500 animate-delay-100">
    <ion-header :translucent="true">
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button color="primary"></ion-menu-button>
        </ion-buttons>
        <ion-buttons slot="end" @click="confirmLoginOut">
          <ion-icon color="primary" :icon="logoutIcon"></ion-icon>
        </ion-buttons>
        <ion-title>
          {{ t("menu." + ($route.name as string)) }} {{ rackNm }}</ion-title
        >
      </ion-toolbar>
    </ion-header>
    <slot></slot>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonButtons,
  IonHeader,
  IonMenuButton,
  IonPage,
  IonTitle,
  IonToolbar,
  IonIcon,
} from "@ionic/vue";
import { logOutOutline as logoutIcon } from "ionicons/icons";
import { useI18n } from "@/hooks/web/useI18n";
import { useUserStore } from "/@/store/modules/user";
import * as LOGIS_UTIL from "@/utils/logisticUtil";
import { ref } from "vue";
import { useRouter } from "vue-router";

const { t } = useI18n();
const { confirmLoginOut } = useUserStore();
const { currentRoute } = useRouter();
const rackNm = ref(LOGIS_UTIL.getLocalStorage("setting.equipNm"));

if (
  currentRoute.value.name == "DasSkuInput" ||
  currentRoute.value.name == "BoxLabelPrint" ||
  currentRoute.value.name == "EventView" ||
  currentRoute.value.name == "B2CBoxInput"
) {
  rackNm.value = " [ " + rackNm.value + " ]";
} else {
  rackNm.value = "";
}

console.log(currentRoute.value.name);
</script>

<style scoped>
ion-menu-button {
  font-size: 1rem;
  height: 2rem;
  width: 2rem;
}

ion-icon {
  font-size: 1rem;
  height: 2rem;
  width: 1rem;
}
ion-title {
  font-size: 0.8rem;
}
</style>
