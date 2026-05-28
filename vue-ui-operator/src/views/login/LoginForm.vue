<template>
  <LoginFormTitle v-show="getShow" class="animate-fade-up" />
  <Form
    class="p-4 animate-fade-up"
    :model="formData"
    :rules="getFormRules"
    ref="formRef"
    v-show="getShow"
    @keydown.enter="handleLogin"
  >
    <FormItem name="account" class="animate-fade-up">
      <Input
        size="large"
        v-model:value="formData.account"
        :placeholder="t('login.userName')"
        class="fix-auto-fill"
        @blur="handleDomain"
      />
    </FormItem>
    <FormItem name="password" class="animate-fade-up">
      <InputPassword
        size="large"
        visibilityToggle
        v-model:value="formData.password"
        :placeholder="t('login.password')"
        @blur="handleDomain"
      />
    </FormItem>
    <FormItem name="domainId" class="animate-fade-up">
      <select v-model="selected">
        <option
          v-for="(item, index) in selectList"
          :key="index"
          :value="item.id"
        >
          {{ item.description }}
        </option>
      </select>
    </FormItem>

    <FormItem class="animate-fade-up">
      <Button
        type="primary"
        size="large"
        block
        @click="handleLogin"
        :loading="loading"
      >
        {{ t("login.loginButton") }}
      </Button>
    </FormItem>
  </Form>
</template>

<script lang="ts" setup>
import { reactive, ref, unref, computed, onMounted } from "vue";

import { Form, Input, Row, Col, Button } from "ant-design-vue";
import LoginFormTitle from "./LoginFormTitle.vue";

import { useI18n } from "/@/hooks/web/useI18n";
import { useMessage } from "/@/hooks/web/useMessage";
import { useAlert } from "@/hooks/web/useAlert";

import { useUserStore } from "/@/store/modules/user";
import {
  LoginStateEnum,
  useLoginState,
  useFormRules,
  useFormValid,
} from "./useLogin";

import { getDomain } from "/@/api/sys/user";
// import type { SelectProps } from "ant-design-vue";
import { Select } from "ant-design-vue";
// import { EventValue } from "ant-design-vue/lib/vc-picker/interface";

const ACol = Col;
const ARow = Row;
const FormItem = Form.Item;
const InputPassword = Input.Password;
const { t } = useI18n();
const { createMessage } = useMessage();
const { createConfirm } = useAlert();
const userStore = useUserStore();

const { setLoginState, getLoginState } = useLoginState();
const { getFormRules } = useFormRules();

const formRef = ref();
const loading = ref(false);
const rememberMe = ref(false);

const formData = reactive({
  account: "",
  password: "",
  domainId: "",
});

const { validForm } = useFormValid(formRef);

//onKeyStroke('Enter', handleLogin);

const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN);

const selectList = ref([{ id: "", description: "선택" }]);
const selected = ref("");

async function handleDomain(e?: Event) {
  const data = await validForm();
  const params = { userId: data.account };
  const domainList = await getDomain(params);
  if (domainList.length < 1) {
    createMessage.info(t("text.login_not_typed"));
  } else {
    selectList.value = [{ id: "", description: "선택" }, ...domainList];
  }
}

onMounted(async () => {
  //await handleDomain();
});

async function handleLogin() {
  const data = await validForm();
  if (!data) return;
  if (selected.value === "") {
    createMessage.info(`도메인을 선택해주세요`);
    return;
  }
  try {
    loading.value = true;
    const userInfo = await userStore.login({
      password: data.password,
      username: data.account,
      domain: selected.value,
      mode: "none",
    });
    if (userInfo) {
      createMessage.success("login.loginSuccessTitle");
    }
  } catch (error) {
    createConfirm({
      title: t("api.errorTip"),
      content:
        error.response.data.msg != null
          ? error.response.data.msg
          : (error as unknown as Error).message || t("api.networkExceptionMsg"),
    });
  } finally {
    loading.value = false;
  }
}
</script>

<style>
select {
  width: 100%;
  /* background:url(arrow.png) no-repeat 95% 9px; */

  font-family: "Noto Sansf KR", sans-serif;
  font-size: 0.5rem;
  font-weight: 400;
  line-height: 1.5;

  color: #444;
  background-color: #fff;

  padding: 0.6em 1.4em 0.5em 0.8em;
  margin: 0;

  border: 1px solid #aaa;
  border-radius: 0.1em;
  box-shadow: 0 1px 0 1px rgba(0, 0, 0, 0.04);
}

select:hover {
  border-color: #888;
}

select:focus {
  border-color: #aaa;
  box-shadow: 0 0 1px 3px var(--ion-color-primary);
  box-shadow: 0 0 0 3px -moz-mac-focusring;
  color: #222;
  outline: none;
}

select:disabled {
  opacity: 0.5;
}
</style>
