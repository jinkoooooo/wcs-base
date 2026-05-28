<template>
  <ion-menu
    class="menu-size"
    content-id="main-content"
    type="overlay"
    swipe-gesture
  >
    <ion-content>
      <ion-list lines="none">
        <ion-menu-toggle
          :auto-hide="true"
          v-for="(page, i) in menusRef"
          :key="i"
        >
          <ion-item
            router-direction="root"
            :router-link="page.path"
            lines="none"
            :detail="false"
            class="hydrated"
            :class="{ selected: page.path === $route.path }"
            button
          >
            <div class="flex flex-col">
              <ion-img
                aria-hidden="true"
                slot="start"
                :src="page.icon"
              ></ion-img>
              <ion-label class="text-center">{{ page.name }}</ion-label>
            </div>
          </ion-item>
        </ion-menu-toggle>
      </ion-list>
    </ion-content>
  </ion-menu>
</template>

<script setup lang="ts">
import {
  IonContent,
  IonItem,
  IonLabel,
  IonList,
  IonMenu,
  IonMenuToggle,
  IonImg,
} from "@ionic/vue";
import { ref, watch } from "vue";
import { usePermissionStore } from "/@/store/modules/permission";
import { getMenus } from "/@/router/menus";
import type { Menu } from "/@/router/types";
import { useRouter } from "vue-router";
import { computed } from "vue";
import * as LOGIS_UTIL from "@/utils/logisticUtil";

const menusRef = ref<Menu[]>([]);
const permissionStore = usePermissionStore();

// 장비 유형에 따른 아이콘 크기 변환
// const equipType = ref(LOGIS_UTIL.getDeviceType());
// const imgStyle = computed(() => {
//   const size = equipType.value === "PDA" ? 100 : 140;
//   return {
//     width: `${size}px`,
//     height: `${size}px`,
//     objectFit: "contain",
//   };
// });

// const selectedIndex = ref();
const router = useRouter();

watch(
  [
    () => permissionStore.getLastBuildMenuTime,
    () => permissionStore.getBackMenuList,
  ],
  () => {
    getMenuLst();
  },
  {
    immediate: true,
  }
);
// function handleRouterChange(page) {
//   router.push(page.path);
// }
async function getMenuLst() {
  menusRef.value = await getMenus();
  return;
}
</script>

<style scoped>
ion-menu ion-content {
  --background: var(--ion-item-background, var(--ion-background-color, #fff));
  --max-width: var(--side-max-width);
}
.menu-size {
  max-width: var(--side-max-width);
}
ion-menu.md ion-content {
  --padding-start: 8px;
  --padding-end: 8px;
  --padding-top: 20px;
  --padding-bottom: 20px;
}

ion-menu.md ion-list {
  padding: 20px 0;
}

ion-menu.md ion-note {
  margin-bottom: 30px;
}

ion-menu.md ion-list-header,
ion-menu.md ion-note {
  padding-left: 10px;
}

ion-menu.md ion-list#inbox-list {
  border-bottom: 1px solid var(--ion-color-step-150, #d7d8da);
}

ion-menu.md ion-list#inbox-list ion-list-header {
  font-size: 22px;
  font-weight: 600;

  min-height: 20px;
}

ion-menu.md ion-list#labels-list ion-list-header {
  font-size: 0.8rem;

  margin-bottom: 18px;

  color: #757575;

  min-height: 26px;
}
ion-menu ion-item {
  font-size: 0.5rem;
}

ion-menu.md ion-item {
  --padding-start: 10px;
  --padding-end: 10px;
  border-radius: 4px;
}

ion-menu.md ion-item.selected {
  --background: rgba(var(--ion-color-primary-rgb), 0.14);
}

ion-menu.md ion-item.selected ion-icon {
  color: var(--ion-color-primary);
}

ion-menu.md ion-item ion-icon {
  color: #616e7e;
}

ion-menu.md ion-item ion-label {
  font-weight: 500;
}

ion-menu.ios ion-content {
  --padding-bottom: 20px;
}

ion-menu.ios ion-list {
  padding: 20px 0 0 0;
}

ion-menu.ios ion-note {
  line-height: 24px;
  margin-bottom: 20px;
}

ion-menu.ios ion-item {
  --padding-start: 16px;
  --padding-end: 16px;
  --min-height: 50px;
}

ion-menu.ios ion-item.selected ion-icon {
  color: var(--ion-color-primary);
}

ion-menu.ios ion-item ion-icon {
  font-size: 24px;
  color: #73849a;
}

ion-menu.ios ion-list#labels-list ion-list-header {
  margin-bottom: 8px;
}

ion-menu.ios ion-list-header,
ion-menu.ios ion-note {
  padding-left: 16px;
  padding-right: 16px;
}

ion-menu.ios ion-note {
  margin-bottom: 8px;
}

ion-note {
  display: inline-block;
  font-size: 0.8rem;

  color: var(--ion-color-medium-shade);
}

ion-item.selected {
  --color: var(--ion-color-primary);
}
</style>
