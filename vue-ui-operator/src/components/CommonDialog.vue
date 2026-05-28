<!--
  공통 다이얼로그
-->
<template>
  <div id="__modal__" ref="modalRef" @click="onDismissClick">
    <div ref="dialogRef" class="dialog">
      <div ref="titleRef" class="title">
        <span>{{ titleStr }}</span>
      </div>
      <div ref="containerRef" dialog class="container"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  onMounted,
  onUnmounted,
  getCurrentInstance,
  ref,
  Component,
} from "vue";
import * as logisUtil from "@/utils/logisticUtil";
import renderComponent from "@/components/renderComponent";
import FormListPopup from "@/components/FormListPopup.vue";
const { appContext } = getCurrentInstance();

const modalRef = ref();
const dialogRef = ref();
const titleRef = ref();
const containerRef = ref();

const getModal = () => {
  return modalRef.value;
};
const getDialog = () => {
  return dialogRef.value;
};
const getTitle = () => {
  return titleRef.value;
};
const getContainer = () => {
  return containerRef.value;
};

const props = defineProps({
  /**
   * @description 기본 팝업 너비
   ****************
   * @type {String}
   */
  basicWidth: {
    type: String,
    default: "80%",
  },

  /**
   * @description 기본 팝업 높이
   ****************
   * @type {String}
   */
  basicHeight: {
    type: String,
    value: "80%",
  },
});

let closeByModalClick: undefined | Function = undefined;
let titleStr = ref();
let openCallback: undefined | Function = undefined;
let closeCallback: undefined | Function = undefined;

/**
 * @description ready
 *******************
 */
let vnode;
onMounted(() => {
  // 1. open-dialog 이벤트 리스터
  document.addEventListener("open-dialog", (event: CustomEvent) => {
    closeByModalClick = event.detail.closeByModalClick;
    vnode = event.detail.component;
    let properties = event.detail.properties;
    let title = event.detail.title ? event.detail.title : "";
    let width = event.detail.width ? event.detail.width : props.basicWidth;
    let height = event.detail.height ? event.detail.height : props.basicHeight;
    openCallback = event.detail.openCallback ? event.detail.openCallback : null;
    closeCallback = event.detail.closeCallback
      ? event.detail.closeCallback
      : null;

    openDialog(
      vnode,
      properties,
      title,
      width,
      height,
      openCallback,
      closeCallback
    );
  });

  // 2. open-form-list-dialog 이벤트 리스너
  document.addEventListener("open-form-list-dialog", (event: CustomEvent) => {
    const options = event.detail;
    openFormListPopup(options);
  });

  // 3. close-dialog 이벤트 리스너
  document.addEventListener("close-dialog", (event: CustomEvent) => {
    closeDialog(event.detail, vnode?.component?.exposed);
  });
});
const openFormListPopup = (options) => {
  // const listPopup: any = document.createElement("form-list-popup");
  let formListProps: any = {};
  formListProps.listFields = options.listFields;
  formListProps.listData = options.listData
    ? options.listData
    : formListProps.listData;
  formListProps.listDataReqUrl = options.listDataReqUrl
    ? options.listDataReqUrl
    : formListProps.listDataReqUrl;
  formListProps.handleListDataBeforeSet = options.handleListDataBeforeSet
    ? options.handleListDataBeforeSet
    : formListProps.handleListDataBeforeSet;
  formListProps.handleListDataAfterSet = options.handleListDataAfterSet
    ? options.handleListDataAfterSet
    : formListProps.handleListDataAfterSet;
  formListProps.dataListClickHandler = options.dataListClickHandler
    ? options.dataListClickHandler
    : formListProps.dataListClickHandler;

  const openCallback =
    options.openCallback && typeof options.openCallback === "function"
      ? options.openCallback
      : null;
  const closeCallback =
    options.closeCallback && typeof options.closeCallback === "function"
      ? options.closeCallback
      : null;

  logisUtil.showPopup(
    options.title ? options.title : null,
    FormListPopup,
    formListProps,
    options.width ? options.width : "fit",
    options.height ? options.height : "fit",
    openCallback,
    closeCallback
  );
};

const throwException = (errorMessage) => {
  throw new Error(errorMessage);
};

/**
 * @description 팝업 오픈
 *******************
 * @param {Component} component
 * @param {String} title
 * @param {String} width
 * @param {String} height
 * @param {Function} openFn
 * @param {Function} closeFn
 */
const openDialog = (
  component: any,
  properties: any,
  title: string,
  width: string,
  height: string,
  openFn: Function,
  closeFn: Function
) => {
  titleStr.value = title;

  // 타이틀이 없다면 title section을 hidden 처리
  if (!titleStr.value) {
    getTitle().hidden = true;
  } else {
    getTitle().hidden = false;
  }
  const container = getContainer();
  const dialog = getDialog();
  const modal = getModal();
  const registerd = appendChild(container, component, properties);
  // 길이 값을 fit으로 주면 다이얼로그 내부의 화면 넓이를 다이얼로그에 적용함
  dialog.style.width = width === "fit" ? "" : width;
  // 높이 값을 fit으로 주면 다이얼로그 내부의 화면 높이를 다이얼로그에 적용함
  dialog.style.height = height === "fit" ? "" : height;
  modal.style.display = "flex";

  document.dispatchEvent(
    new CustomEvent("dialog-opened", {
      detail: {
        content: registerd,
      },
    })
  );

  if (openFn && typeof openFn === "function") openFn();
  if (closeFn) closeCallback = closeFn;
};
let destroyComp = null;

const appendChild = (
  container: HTMLElement,
  component: any,
  properties: any
) => {
  destroyComp?.destroy();
  destroyComp = renderComponent({
    container,
    component,
    props: { ...properties },
    appContext,
  });
  return destroyComp.registerd;
};
/**
 * @description 팝업 닫기
 *******************
 * @param {Object} closeParam
 */
const closeDialog = (closeParam?: any, innerExposed?: any) => {
  const container = getContainer();
  const modal = getModal();
  modal.style.display = "none";
  if (closeCallback && typeof closeCallback === "function") {
    closeCallback(closeParam, innerExposed);
    closeCallback = null;
  }
  destroyComp?.destroy();
};

/**
 * @description 팝업 내용 컨텐트 삭제
 *******************
 * @param {Object} container
 */
const removeContent = (container: HTMLDivElement) => {
  while (container.children.length) {
    container.removeChild(container.firstChild);
  }
};

/**
 * @description close dialog when model dismiss clicked
 *******************
 */
const onDismissClick = () => {
  if (closeByModalClick) {
    closeDialog(null, vnode?.component?.exposed);
  }
};
</script>
<style lang="css" scoped>
#__modal__ {
  display: none;
  position: fixed;
  z-index: 4;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);

  .dialog {
    display: flex;
    flex-direction: column;
    margin: auto;
    border-radius: var(--default-border-radius);
    //border: 3px solid rgba(0, 0, 0, 0.3);
    overflow: hidden;
    max-width: 80vw;
    max-height: 80vh;
  }
  .title {
    background-color: #252222;
    padding: 5px 15px 2px 15px;
    color: #fff;
    font-size: 0.8rem;
  }
  .container {
    display: flex;
    flex: 1;
    overflow-y: auto;
    background-color: #fff;
    margin: auto;
    width: 100%;
    height: 100%;
  }
  button {
    font-size: 0.7rem;
  }
}
</style>
