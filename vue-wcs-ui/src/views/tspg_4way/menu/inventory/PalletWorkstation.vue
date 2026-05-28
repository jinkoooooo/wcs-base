<!--
  파렛트 작업대 — thin wrapper.
  실제 구현은 ./pallet-workstation/ 폴더 내 자산(컴포넌트 5개 + composable + 공유 모듈)에 분산되어 있다.
  메뉴 DB의 component 경로(tspg_4way/menu/inventory/PalletWorkstation)를 변경 없이 유지하기 위해
  원래 위치에 이 wrapper를 둔다.
-->
<template>
  <PalletWorkstation />
</template>

<script lang="ts" setup>
import PalletWorkstation from './pallet-workstation/PalletWorkstation.vue';
</script>
