<!--
  상단 Form과 하단 리스트로 구성되어 있는 공통 팝업 엘리먼트
-->
<template>
  <div id="__form-list-container__">
    <div ref="listContainerRef" class="list-container">
      <ItemList
        ref="listRef"
        class="list"
        dialog
        :header="listFields"
        :data="itemListDataRef"
        @headerConfigured="_onHeaderConfiguredHandler"
      />
    </div>

    <div class="button-container">
      <button class="close-btn" @click="_closeDialog">
        {{ t(closeBtnLabel) }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from "@/hooks/web/useI18n";
import { watch, ref, unref, Ref } from "vue";
import { getCommonGetApi } from "@/api/CommonApi";
import ItemList from "@/components/ItemList.vue";
import { closePopup } from "@/utils/logisticUtil";
import { watchEffect } from "vue";
import { onMounted } from "vue";
import { nextTick } from "vue";

const { t } = useI18n();

const listContainerRef = ref();
const _getListContainer = (): HTMLDivElement => {
  return listContainerRef.value;
};

const listRef = ref();
const _getList = () => {
  return listRef.value;
};

const _getBaseUrl = () => {
  return JSON.parse(localStorage.getItem(props.baseUrlStorageKey));
};

/**
 * @description 리스트에 맵핑할 데이터 배열
 */
let itemListDataRef: Ref<Array<any>> = ref();

// const properties = () => {
//   return
// };

const props = defineProps({
  /**
   * @description 서버를 통해 list 데이터를 조회하기 위한 Url
   */
  listDataReqUrl: {
    type: String,
  },
  /**
   * @description 하단에 출력되는 리스트의 컬럼
   */
  listFields: {
    type: Array<Column>,
    // defualt: () => [],
  },
  /**
   * @description 하단에 출력되는 리스트의 컬럼
   */
  listData: {
    type: Array<any>,
    defualt: () => {
      return [];
    },
  },

  /**
   * @description dialog close button label
   */
  closeBtnLabel: {
    type: String,
    default: "button.close",
  },

  /**
   * @description 리스트의 데이터를 조회하기 전 실행되는 핸들러
   * 외부로 부터 함수를 전달 받아 구현함
   */
  listDataAjaxHandler: {
    type: Function,
  },
  /**
   * @description listData가 변경되면 발생하는 event handler 없으면 기본 컴포넌트 설정으로 데이터 추출
   */
  handleListDataBeforeSet: {
    type: Function,
  },
  /**
   * @description handleListDataAfterSet이 정의되어 있다면 해당 함수를 호출하여 데이터 맵핑 후에 추가 작업을 수행함
   */
  handleListDataAfterSet: {
    type: Function,
  },
  /**
   * @description 리스트의 컬럼 구성이 완료 되었을 경우 발생하는 event handler
   * 컬럼 구성이 완료 되고 전달받은 dataListClickHandler가 존재한다면 리스트 클릭에 대한 이벤트 리스너를 등록함
   */
  dataListClickHandler: {
    type: Function,
  },
  /**
   * @description form, list의 데이터를 조회하기 위해 서버의 baseUrl을 로컬스트로지로 부터
   * 추출하는데 그때 필요한 로컬스토리지의 baseUrl 키 값
   */
  baseUrlStorageKey: {
    type: String,
    defualt: "setting.baseUrl",
  },
});

/**
 * @description 그리드 컬럼 구성 완료 여부 flag
 */
let isListHeaderConfigured = ref(false);
let isListDeactivated = ref(false);

onMounted(() => {
  watch(
    () => props.listFields,
    (listFields) => {
      _listFieldsChanged(listFields);
    }
  );

  console.log(itemListDataRef.value);
  watchEffect(() => {
    _isFormListPopupConfigured(isListHeaderConfigured.value);
  });
});
const _listFieldsChanged = (listFields) => {
  if (!listFields || listFields.length === 0) {
    _deactivateList();
    return;
  }
};

/**
 * @description 리스트의 컬럼 구성이 완료 되었을 경우 발생하는 event handler
 * 컬럼 구성이 완료 되고 전달받은 dataListClickHandler가 존재한다면 리스트 클릭에 대한 이벤트 리스너를 등록함
 */
const _onHeaderConfiguredHandler = (e) => {
  if (
    props.dataListClickHandler &&
    typeof props.dataListClickHandler === "function"
  ) {
    _getList().addEventListener("data-list-click", props.dataListClickHandler);
  }
  isListHeaderConfigured.value = true;
  // _isFormListPopupConfigured(isListHeaderConfigured.value);
};

/**
 * @description 상단의 폼, 하단의 리스트가 모두 구성 완료 되었을 경우 실행되며
 * formData, listData를 조회함
 * formData 또는 listData가 이미 존재한다면 서버를 통해 조회하지 않음
 */
const _isFormListPopupConfigured = async (isListHeaderConfigured) => {
  if (isListHeaderConfigured) {
    if (
      (!unref(itemListDataRef) || !Array.isArray(unref(itemListDataRef))) &&
      !isListDeactivated.value
    ) {
      await getListData();
    }
  }
};

/**
 * @description 서버를 통해 list 데이터를 조회하기 위한 메소드로
 * listDataReqUrl 통해 서비스 요청을 수행하고 결과를 _setListData를 호출하여 listData를 초기화함
 */
const getListData = async () => {
  if (props.listData) {
    var list = Array.isArray(props.listData) ? props.listData : null;
    _setListData(list);
  } else {
    if (!props.listDataReqUrl) return;
    if (
      props.listDataAjaxHandler &&
      typeof props.listDataAjaxHandler === "function"
    ) {
      props.listDataAjaxHandler();
    }

    const listData = await getCommonGetApi(props.listDataReqUrl, null);

    var list = Array.isArray(listData)
      ? listData
      : Array.isArray(listData.result)
      ? listData.result
      : [];

    _setListData(list);
  }
};

/**
 * @description listData가 변경되면 발생하는 event handler
 * 전달 받은 listData를 바탕으로 화면에 나타나는 폼의 값들을 결정함
 * 1. handleListDataBeforeSet이 정의되어 있다면 해당 함수를 호출하여 데이터 맵핑 전에 추가작업을 수행함
 * 2. this.listData의 값을 초기화 하여 리스트에 값을 출력함
 * 3. handleListDataAfterSet이 정의되어 있다면 해당 함수를 호출하여 데이터 맵핑 후에 추가 작업을 수행함
 */
const _setListData = (listData) => {
  if (
    props.handleListDataBeforeSet &&
    typeof props.handleListDataBeforeSet === "function"
  ) {
    props.handleListDataBeforeSet({
      listElement: _getList(),
      listData: listData,
    });
  }

  if (!listData || !Array.isArray(listData)) {
    return;
  }

  itemListDataRef.value = listData;

  nextTick(() => {
    if (
      props.handleListDataAfterSet &&
      typeof props.handleListDataAfterSet === "function"
    ) {
      props.handleListDataAfterSet({
        listElement: _getList(),
        listData: itemListDataRef.value,
      });
    }
  });
};

const _deactivateList = () => {
  _getListContainer().remove();
  isListDeactivated.value = true;
  isListHeaderConfigured.value = true;
};

/**
 * @description close-btn click event handler 다이얼로그 닫기
 */
const _closeDialog = () => {
  closePopup();
};

// /**
//  * @descirption form-list-popup 구성중 발생하는 에러에 대한 공통 핸들러
//  */
// const throwException = (errorMessage) => {
//   throw new Error(errorMessage);
// };
</script>

<style lang="css" scoped>
@import url("@/theme/shared-styles.css");
#__form-list-container__ {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 0.3rem;

  .form-container {
    display: flex;
    flex-direction: column;
    margin-bottom: 0.3rem;
  }
  .form-container.form-type {
    padding: 0.3rem;
    background-color: var(--primary-color);
    border-radius: var(--default-border-radius);
    color: #fff;
  }
  .form-container.element-type {
    display: flex;
    flex: 1;
    overflow: auto;
  }
  .form-container.multiple-columns.form-type {
    display: grid;
  }
  .field-container {
    display: grid;
    grid-template-columns: auto 1fr;
  }
  .form-container.single-column.form-type .field-container {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr 1fr;
  }
  .field-container .form-label {
    margin: auto 10px auto auto;
    width: 3rem;
    min-width: 3rem;
    max-width: 3rem;
  }
  .form-container.single-column.form-type .field-container .form-label {
    margin: auto 0.3rem;
  }
  .field-container .form-input {
    width: calc(100% - 1.2rem);
    justify-self: right;
    height: 1rem;
    margin: auto;
    font-size: 0.6rem;
  }
  .list-container {
    display: flex;
    flex: 1;
  }
  .button-container {
    display: grid;
    justify-items: right;
    margin-top: 10px;
    margin-bottom: 10px;
  }
}
</style>
