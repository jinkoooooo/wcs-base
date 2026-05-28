<template>
  <LoginFormTitle v-show="getShow" class="enter-x" />

  <Form
    class="p-4 enter-x"
    :model="formData"
    :rules="getFormRules"
    ref="formRef"
    v-show="getShow"
    @keypress.enter="handleLogin"
  >
    <!-- 계정 -->
    <FormItem name="account" class="enter-x">
      <Input
        size="large"
        v-model:value="formData.account"
        :placeholder="t('sys.login.userName')"
        class="fix-auto-fill"
        @blur="handleDomainFetch"
      />
    </FormItem>

    <!-- 비밀번호 -->
    <FormItem name="password" class="enter-x">
      <InputPassword
        size="large"
        visibilityToggle
        v-model:value="formData.password"
        :placeholder="t('sys.login.password')"
      />
    </FormItem>

    <!-- 도메인 선택 -->
    <FormItem name="domainId" class="enter-x">
      <ASelect
        v-model:value="selectedDomainId"
        :options="domainOptions"
        :loading="isDomainLoading"
        :disabled="!domainOptions.length"
        showSearch
        optionFilterProp="label"
        allowClear
        placeholder="도메인을 선택해주세요"
        style="width: 100%"
      />
    </FormItem>

    <!-- 로그인 버튼 -->
    <FormItem class="enter-x">
      <Button type="primary" size="large" block @click="handleLogin" :loading="loading">
        {{ t('sys.login.loginButton') }}
      </Button>
    </FormItem>
  </Form>
</template>

<script lang="ts" setup>
  import { reactive, ref, unref, computed } from 'vue';
  import { Form, Input, Button, Select } from 'ant-design-vue';
  import LoginFormTitle from './LoginFormTitle.vue';

  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';

  import { useUserStore } from '/@/store/modules/user';
  import { LoginStateEnum, useLoginState, useFormRules, useFormValid } from './useLogin';
  import { useDesign } from '/@/hooks/web/useDesign';

  import { getDomain } from '/@/api/sys/user';

  const FormItem = Form.Item;
  const InputPassword = Input.Password;
  const ASelect = Select;

  interface LoginFormModel {
    account: string;
    password: string;
  }

  interface DomainApiItem {
    id: string;
    description: string;
  }

  interface DomainOption {
    label: string;
    value: string;
  }

  const { t } = useI18n();
  const { notification, createErrorModal } = useMessage();
  const { prefixCls } = useDesign('login');
  const userStore = useUserStore();

  const { getLoginState } = useLoginState();
  const { getFormRules } = useFormRules();

  const formRef = ref();
  const loading = ref(false);

  const formData = reactive<LoginFormModel>({
    account: '',
    password: '',
  });

  // 도메인 관련 상태
  const selectedDomainId = ref<string>();
  const domainOptions = ref<DomainOption[]>([]);
  const isDomainLoading = ref(false);

  const { validForm } = useFormValid(formRef);

  const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN);

  const resetDomain = () => {
    domainOptions.value = [];
    selectedDomainId.value = undefined;
  };

  // 계정 입력 후 blur 시 도메인 목록 조회
  const handleDomainFetch = async () => {
    const account = formData.account?.trim();
    if (!account) {
      resetDomain();
      return;
    }

    try {
      isDomainLoading.value = true;

      const domainList = (await getDomain({ userId: account })) as DomainApiItem[];

      if (!domainList || domainList.length === 0) {
        resetDomain();
        notification.info({
          message: 'info',
          description: t('sys.login.loginFailUser'),
          duration: 3,
        });
        return;
      }

      domainOptions.value = domainList.map<DomainOption>((item) => ({
        label: item.description,
        value: item.id,
      }));

      // 도메인 1개만 있으면 자동 선택
      if (domainOptions.value.length === 1) {
        selectedDomainId.value = domainOptions.value[0].value;
      } else {
        selectedDomainId.value = undefined;
      }
    } catch (error) {
      resetDomain();
      notification.error({
        message: t('sys.api.errorTip'),
        description: (error as Error).message || t('sys.api.networkExceptionMsg'),
        duration: 3,
      });
    } finally {
      isDomainLoading.value = false;
    }
  };

  const handleLogin = async () => {
    if (loading.value) return;

    const data = await validForm();
    if (!data) return;

    if (!selectedDomainId.value) {
      notification.info({
        message: '도메인 선택',
        description: '도메인을 선택해주세요.',
        duration: 3,
      });
      return;
    }

    try {
      loading.value = true;
      const userInfo = await userStore.login({
        password: data.password,
        username: data.account,
        domain: selectedDomainId.value,
        mode: 'none',
      });

      if (userInfo) {
        notification.success({
          message: t('sys.login.loginSuccessTitle'),
          description: `${t('sys.login.loginSuccessDesc')}: ${userInfo.realName}`,
          duration: 3,
        });
      } else {
        notification.info({
          message: '로그인 실패',
          description: 'ID/PASSWORD 확인 해주세요!',
          duration: 3,
        });
      }
    } catch (error) {
      createErrorModal({
        title: t('sys.api.errorTip'),
        content: (error as Error).message || t('sys.api.networkExceptionMsg'),
        getContainer: () => document.body.querySelector(`.${prefixCls}`) || document.body,
      });
    } finally {
      loading.value = false;
    }
  };
</script>
