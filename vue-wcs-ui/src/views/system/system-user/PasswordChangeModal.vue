<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="title" @ok="handleSubmit">
    <BasicForm autoFocusFirstItem @register="registerForm" class="p-10 w-99" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { ref, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getCommonPostApi } from '/@/api/common/api';
  import { useI18n } from '/@/hooks/web/useI18n';

  const { t } = useI18n();

  const formSchema: FormSchema[] = [
    {
      field: 'password',
      label: t('label.password'),
      component: 'InputPassword',
      required: true,
    },
    {
      field: 'password_conf',
      label: t('label.password_confirmation'),
      component: 'InputPassword',
      required: true,
    },
  ];
  const title = ref();
  const emits = defineEmits(['success', 'register']);
  const isUpdate = ref(true);
  const { createMessage } = useMessage();
  const [registerForm, { resetFields, validate: formValidate }] = useForm({
    labelWidth: 150,
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { xxl: 24, lg: 24, md: 24, sm: 24 },
  });

  let currentUser = ref();
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    resetFields();
    if (unref(isUpdate)) {
      currentUser.value = data.record;
      title.value =
        t('label.user') + ' [' + currentUser.value.name + '] ' + t('title.change_password');
    }
  });

  async function handleSubmit() {
    let values = await formValidate();

    let isValid = passwordValidate(values);
    if (isValid) {
      let response = await getCommonPostApi(`/users/change_pass/${currentUser.value.id}`, {
        new_pass: window.btoa(values.password_conf),
      });
      if (response) {
        createMessage.success(t('text.password_changed_successfully'));
      }
      closeModal();
    }
  }
  function passwordValidate(values) {
    let id = currentUser.value.id;
    let pass = values.password;
    let passConf = values.password_conf;

    if (!pass || pass.length == 0) {
      createMessage.info(t('text.please_input_x', [t('label.password')]));
      return false;
    }

    if (!passConf || passConf.length == 0) {
      createMessage.info(t('text.please_input_x', [t('label.password_confirmation')]));
      return false;
    }

    if (pass != passConf) {
      createMessage.info(t('text.pwd_confirm_placeholder'));
      return false;
    }

    // // 1. 비밀번호가 영문자, 숫자, 특수문자가 혼용된 8자리 이상인지 확인
    // var reg = /^(?=.*[a-z])(?=.*\d)(?=.*\W).{8,}$/i;
    // if (!reg.test(pass)) {
    //   createMessage.info(t('text.password_pattern'));
    //   return false;
    // }

    // // 2. 반복 문자나 숫자가 사용하고 있는지 확인
    // reg = /([a-z0-9.*\W])\1{2,}/i;
    // if (reg.test(pass)) {
    //   createMessage.info(t('text.password_repetition'));
    //   return false;
    // }

    // // 3. ID와 유사한 패스워드 사용하고 있는지 확인
    // var chunkedIdList = [] as any;

    // for (var i = 0; i <= id.length - 4; i++) {
    //   var chunk = id.slice(i, i + 4);
    //   chunkedIdList.push(chunk);
    // }

    // for (var i = 0; i < chunkedIdList.length; i++) {
    //   if (pass.indexOf(chunkedIdList[i]) >= 0) {
    //     createMessage.info(t('text.password_similarity'));
    //     return false;
    //   }
    // }
    return true;
  }
  defineExpose({
    registerModal,
    handleSubmit,
  });
</script>
