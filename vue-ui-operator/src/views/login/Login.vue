<template>
  <div class="login relative w-full h-full px-4">
    <div class="">
      <img
        :alt="title"
        src="/images/logo-login.png"
        class="w-56 h-25 mt-4 animate-fade-right"
      />
    </div>
    <div class="container relative h-full py-2 mx-auto sm:px-10">
      <div class="flex h-full">
        <div class="hidden min-h-full pl-4 mr-4 xl:flex xl:flex-col xl:w-6/12">
          <div class="my-calc">
            <img
              :alt="title"
              src="/images/mobile-brand.png"
              class="w-full animate-fade-right"
            />
            <div
              class="font-medium text-gray-700 text-center animate-fade-right"
            >
              <span class="inline-block mt-4 text-lg">
                {{ t("login.signInTitle") }}</span
              >
            </div>
            <div
              class="mt-5 font-normal text-white dark:text-gray-500 animate-fade-right"
            >
              {{ t("login.signInDesc") }}
            </div>
          </div>
        </div>
        <div
          class="flex w-full h-full py-5 xl:h-auto xl:py-0 xl:my-0 xl:w-6/12"
        >
          <div
            class="bg-opacity-80 bg-white relative w-full px-5 py-8 mx-auto my-calc rounded-md shadow-md xl:ml-16 xl:bg-transparent sm:px-8 xl:p-4 xl:shadow-none sm:w-3/4 lg:w-2/4 xl:w-auto animate-fade-up"
          >
            <LoginForm />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { computed } from "vue";
// import { AppLogo } from "/@/components/Application";
// import { AppLocalePicker } from "/@/components/Application";
import LoginForm from "./LoginForm.vue";
import ForgetPasswordForm from "./ForgetPasswordForm.vue";
import RegisterForm from "./RegisterForm.vue";
// import MobileForm from './MobileForm.vue';
// import QrCodeForm from './QrCodeForm.vue';
import { useGlobSetting } from "/@/hooks/setting";
import { useI18n } from "/@/hooks/web/useI18n";
import { useLocaleStore } from "/@/store/modules/locale";

defineProps({
  sessionTimeout: {
    type: Boolean,
  },
});

const prefixCls = "login";
const globSetting = useGlobSetting();
const { t } = useI18n();
const localeStore = useLocaleStore();
const showLocale = localeStore.getShowPicker;
const title = computed(() => globSetting?.title ?? "");
</script>
<style scoped>
.login {
  min-height: 100%;
  overflow: hidden;
  background-color: var(--white-background-color);

  &::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-position: top;
    background-repeat: no-repeat;
    background-size: cover;
    @media (max-width: 1024px) {
      background-image: url("/images/mobile-brand.png");
      background-color: var(--ion-color-primary);
    }
  }
  &-sign-in-way {
    .anticon {
      color: #888;
      font-size: 22px;
      cursor: pointer;

      &:hover {
        color: var(--ion-color-primary);
      }
    }
  }
  .my-calc {
    margin-top: calc(50% - 150px);
    margin-bottom: auto;
  }

  input:not([type="checkbox"]) {
    min-width: 360px;
  }

  .ant-divider-inner-text {
    color: var(--ion-color-secondary-contrast);
    font-size: 12px;
  }
}
</style>
