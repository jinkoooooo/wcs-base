<template>
  <section class="asrs-page-shell">
    <!--
      공통 상단 shell.
      headerMode 에 따라
      1) default       : 제목 + toolbar + actions
      2) toolbarOnly   : 제목 없이 toolbar + actions
      두 가지 레이아웃을 지원
    -->
    <header
      class="asrs-shell__hero"
      :class="{
        'asrs-shell__hero--default': headerMode === 'default',
        'asrs-shell__hero--toolbar-only': headerMode === 'toolbarOnly',
      }"
    >
      <template v-if="headerMode === 'default'">
        <div v-if="eyebrow || title" class="asrs-shell__hero-left">
          <p v-if="eyebrow" class="asrs-shell__eyebrow">{{ eyebrow }}</p>
          <h1 v-if="title" class="asrs-shell__title">{{ title }}</h1>
        </div>

        <div class="asrs-shell__hero-center">
          <slot name="toolbar" />
        </div>

        <div v-if="$slots.actions" class="asrs-shell__hero-right">
          <slot name="actions" />
        </div>
      </template>

      <template v-else>
        <div class="asrs-shell__hero-center asrs-shell__hero-center--full">
          <slot name="toolbar" />
        </div>

        <div v-if="$slots.actions" class="asrs-shell__hero-right asrs-shell__hero-right--toolbar-only">
          <slot name="actions" />
        </div>
      </template>
    </header>

    <!--
      본문 영역.
      각 페이지는 여기 아래에 summary / grid / panel 등을 배치
    -->
    <section class="asrs-shell__body">
      <slot />
    </section>
  </section>
</template>

<script setup lang="ts">
  /**
   * ASRS 화면 공통 shell.
   *
   * headerMode:
   * - default: 제목 영역 사용
   * - toolbarOnly: 제목 없이 toolbar 중심
   */
  withDefaults(
    defineProps<{
      eyebrow?: string;
      title?: string;
      headerMode?: 'default' | 'toolbarOnly';
    }>(),
    {
      eyebrow: '',
      title: '',
      headerMode: 'default',
    },
  );
</script>

<style scoped>
  .asrs-page-shell {
    display: flex;
    flex-direction: column;
    gap: 20px;
    min-height: 100%;
    padding: 24px;
    background: #eef4fb;
    box-sizing: border-box;
  }

  .asrs-shell__hero {
    display: grid;
    gap: 20px;
    padding: 24px 28px;
    border-radius: 28px;
    background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
    border: 1px solid rgba(148, 163, 184, 0.16);
    box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
  }

  .asrs-shell__hero--default {
    grid-template-columns: 220px minmax(0, 1fr) auto;
    align-items: start;
  }

  .asrs-shell__hero--toolbar-only {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: start;
  }

  .asrs-shell__hero-left {
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-width: 0;
    padding-top: 18px;
  }

  .asrs-shell__eyebrow {
    margin: 0 0 10px 0;
    font-size: 14px;
    font-weight: 800;
    letter-spacing: 0.04em;
    color: #2563eb;
  }

  .asrs-shell__title {
    margin: 0;
    font-size: 28px;
    font-weight: 900;
    line-height: 1.2;
    color: #0f172a;
  }

  .asrs-shell__hero-center {
    min-width: 0;
  }

  .asrs-shell__hero-center--full {
    width: 100%;
  }

  .asrs-shell__hero-right {
    display: flex;
    align-items: flex-start;
    justify-content: flex-end;
    gap: 10px;
    flex-wrap: wrap;
  }

  .asrs-shell__hero-right--toolbar-only {
    align-self: start;
  }

  .asrs-shell__body {
    display: flex;
    flex-direction: column;
    gap: 20px;
    min-height: 0;
    flex: 1;
  }

  @media (max-width: 1440px) {
    .asrs-shell__hero--default,
    .asrs-shell__hero--toolbar-only {
      grid-template-columns: 1fr;
    }

    .asrs-shell__hero-left {
      padding-top: 0;
    }

    .asrs-shell__hero-right,
    .asrs-shell__hero-right--toolbar-only {
      justify-content: flex-start;
    }
  }
</style>
