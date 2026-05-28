<template>
  <div ref="itemListRef" id="_item-list-container">
    <div :hidden="hideHeader">
      <li ref="headerListRef" v-show="isHeaderConfigured" class="header-list">
        <span v-if="props.checkbox" class="header-item checkbox-header" >
          <input type="checkbox" :checked="isAllChecked" @change="toggleAll" style="margin-left:5px;" />
        </span>
        <span class="header-item" v-for="item in props.header">
          {{ item.display }}
        </span>
      </li>
    </div>

    <div class="body-container">
      <div ref="dataContainerRef" class="data-container" :dialog="dialog"></div>
    </div>

    <div ref="paginatorContainerRef" class="paginator-container">
      <div class="paginator">
        <ion-icon :icon="icons.chevronBackOutline" @click="prevPage" class="prev-page"></ion-icon>
        <input ref="pageInputRef" class="page" name="page" @change="_initPage" :value="page" tabindex="-1" />
        <ion-icon :icon="icons.chevronForwardOutline" @click="nextPage" class="next-page"></ion-icon>
        <span><b>{{ page }}</b> / {{ endPage }}</span>
      </div>

      <div class="page-summary">
        <span></span>
        <!--
        <input
          class="limit"
          type="number"
          name="limit"
          :min="minLimit"
          :max="maxLimit"
          @change="_initLimit"
          :value="pageLimit"
        />

        <span>{{ beginDataNumber }}~{{ endDataNumber }} ({{ total }})</span>
        -->
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, unref, watch, watchEffect } from "vue";

import * as icons from "ionicons/icons";

import { useI18n } from "@/hooks/web/useI18n";
import * as logisUtil from "@/utils/logisticUtil";
import { IonIcon } from "@ionic/vue";

const { t } = useI18n();

const props = defineProps({
  checkbox: {
    type: Boolean,
    default: false,
  },
  /**
   * @description list column header
   ****************
   * @type {Array}
   */
  header: {
    type: Array<Column>,
  },

  /**
   * @description list data
   ****************
   * @type {Array}
   */
  data: {
    type: Array<any>,
    defualt: () => {
      return [];
    },
  },

  /**
   * @description header hidden 처리 flag
   ****************
   * @type {Boolean}
   */
  hideHeader: {
    type: Boolean,
  },

  /**
   * @description list 스타일을 dialog 타입에 맞추기 위한 flag
   ****************
   * @type {Boolean}
   */
  dialog: {
    type: Boolean,
    defualt: false,
  },

  /**
   * @description pagination 처리 여부 flag
   ****************
   * @type {Boolean}
   */
  showPaginator: {
    type: Boolean,
    defualt: false,
  },

  /**
   * @description 총 수량
   ****************
   * @type {Number}
   */
  total: {
    type: Number,
    defualt: 0,
  },

  /**
   * @description 리스트 출력 limit
   ****************
   * @type {Number}
   */
  limit: {
    type: Number,
    defualt: 50,
  },

  /**
   * @description 리스트 출력 limit minimum
   ****************
   * @type {Number}
   */
  minLimit: {
    type: Number,
    defualt: 1,
  },

  /**
   * @description 리스트 출력 limit maximum
   ****************
   * @type {Number}
   */
  maxLimit: {
    type: Number,
    defualt: 5000,
  },
  paginatorChange: {
    type: Function,
    required: true,
  },
});

const emit = defineEmits([
  "headerConfigured",
  "contentConfigured",
  "configured",
  "dataListClick",
  "btnClick",
  "openDialog",
  "paginatorChanged",
  "change",
  "update:selectedItems", // 체크박스 선택값 변경시 사용할 이벤트
]);

const itemListRef = ref();
const getItemList = (): DivElement => itemListRef.value;
const scopeId = computed(() => {
  const container = itemListRef.value;
  return "data-" + Object.keys(container.dataset)[0];
});
const headerListRef = ref();
const getHeaderList = (): ListElement => headerListRef.value;
const dataContainerRef = ref();
const getDataContainer = (): DivElement => dataContainerRef.value;
const pageInputRef = ref();
const getPageInput = (): InputElement => pageInputRef.value;
const paginatorContainerRef = ref();
const getPaginator = (): DivElement => paginatorContainerRef.value;

// 체크박스 추가
const selectedItems = ref<any[]>([]);
function toggleItem(item) {
  const idx = selectedItems.value.indexOf(item);
  if (idx > -1) {
    selectedItems.value.splice(idx, 1);
  } else {
    selectedItems.value.push(item);
  }
  emit("update:selectedItems", selectedItems.value); // 부모로 선택값 전달
}

/**
 * @description list header configured 여부 flag
 */
let isHeaderConfigured = ref(false);

/**
 * @description list content configured 여부 flag
 */
let isContentConfigured = ref(false);

let gridTemplateColumns: string = "";
let page: number;
let pageLimit: number = props.limit;
let beginDataNumber: number;
let endDataNumber: number;
let endPage: number = 1;
let rows = [] as HTMLLIElement[];
let getRows: Function = () => {
  return rows;
};

onMounted(async () => {
  _renderContents(unref(props.header), unref(props.data));
  watchEffect(() => {
    _renderContents(unref(props.header), unref(props.data));
  });

  watch(
    () => props.data,
    (data) => {
      _setupPaginator(unref(data));
      _renderContents(props.header, unref(data));
    }
  );

  watch(
    () => [page, props.limit],
    ([page, limit]) => {
      _paginatorChanged(page, limit);
    }
  );
  _showPaginatorChanged(true);
  watch(
    () => props.showPaginator,
    (showPaginator) => {
      _showPaginatorChanged(showPaginator);
    }
  );
});
/**
 * @description header, data의 값이 변경되면 list를 그림
 ****************
 * @param {Array} header,
 * @param {Array} data
 */
const _renderContents = (header: Array<any>, data: Array<any>) => {
  console.log("_renderContents 실행");
  if (header && header.length > 0) {
    _renderHeader(header);

    // render header 성공 후 flag 변경
    isHeaderConfigured.value = true;

    // header render가 완료 되면 custom event 발생
    // getHeaderList().dispatchEvent(
    //   new CustomEvent("headerConfigured", {
    //     detail: {
    //       header: header,
    //       headerElement: headerListRef.value,
    //     },
    //   })
    // );
    emit("headerConfigured", {
      detail: {
        header: header,
        headerElement: getHeaderList(),
      },
    });
  }

  if (!data || data.length === 0) {
    _clearRows();
  }

  if (header && header.length > 0 && data && data.length >= 0) {
    _renderContent(header, data);

    // render content 성공 후 flag 변경
    isContentConfigured.value = true;
    // content render가 완료 되면 custom event 발생
    // getHeaderList().dispatchEvent(
    //   new CustomEvent("contentConfigured", {
    //     detail: {
    //       data: data,
    //       contentElements: Array.from(
    //         getDataContainer().querySelectorAll("li")
    //       ),
    //     },
    //   })
    // );
    emit("contentConfigured", {
      detail: {
        data: data,
        contentElements: Array.from(getDataContainer().querySelectorAll("li")),
      },
    });
  }

  // header, content가 모두 redner 되면 list-configured custom event 발생
  if (isHeaderConfigured.value && isContentConfigured.value) {
    // getItemList().dispatchEvent(
    //   new CustomEvent("configured", {
    //     detail: {
    //       header: header,
    //       headerElement: headerListRef.value,
    //       data: data,
    //       contentElements: Array.from(
    //         getDataContainer().querySelectorAll("li")
    //       ),
    //     },
    //   })
    // );
    emit("configured", {
      detail: {
        header: header,
        headerElement: getHeaderList(),
        data: data,
        contentElements: Array.from(getDataContainer().querySelectorAll("li")),
      },
    });
  }
};

/**
 * @description _renderContents에 의해 실행되는 리스트 해더 구성 function
 *****************
 * @param {Array} header
 */
const _renderHeader = (header: Array<any>) => {
  let headerListTag = getHeaderList();
  let gridTempCols: string[] = [];
  if (props.checkbox) {
    gridTempCols.push("0.15fr"); // 체크박스 열 폭을 픽셀 단위로 고정
  }
  header.forEach((item) => {
    let colWidth = item.columnWidth ? item.columnWidth : "1fr";
    let hidden = item.hidden;
    if (hidden) {
      colWidth = "0";
    }
    gridTempCols.push(colWidth);
  });

  gridTemplateColumns = gridTempCols.join(" ");
  console.log(`ItemList.vue girdTemplateColumns : ${JSON.stringify(gridTemplateColumns, null, 2)}`)
  headerListTag.style.gridTemplateColumns = gridTemplateColumns;
};

/**
 * @description _renderContents에 의해 실행되는 리스트 데이터 구성 function
 *****************
 * @param {Array} header
 * @param {Array} data
 */
const _renderContent = (header: Array<Column>, data: Array<any>) => {
  _clearRows();

  let dataContainerTag = getDataContainer();
  rows = [];
  data.forEach((rowData) => {
    let dataList = createDataList(header.length);

    // 1. 체크박스 컬럼 추가 (props.checkbox가 true일 때만)
    if (props.checkbox) {
      const checkboxSpan = document.createElement("span");
      checkboxSpan.className = "data-item checkbox-cell";
      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      // checked 상태를 rowData.checked로 연결
      checkbox.checked = !!rowData.checked;
      // 변경 이벤트
      checkbox.addEventListener("change", (e) => {
        rowData.checked = checkbox.checked; // rowData에 직접 반영
        toggleItem(rowData); // 기존 selectedItems 관리도 계속
      });
      checkboxSpan.appendChild(checkbox);
      dataList.appendChild(checkboxSpan);
    }

    // 2. 기존 컬럼 렌더링
    header.forEach((column) => {
      let style = column.style ? column.style : null;
      let hidden = column.hidden;
      let fieldname = column.fieldname;
      let dataItem;
      if (column.button) {
        dataItem = createButtonItem(column.button, fieldname, rowData);
      } else if (column.image) {
        dataItem = createImageItem(column.image, rowData[fieldname], rowData);
      } else {
        dataItem = createDataItem(
          rowData[fieldname],
          rowData,
          column.displayCallback
        );
      }
      if (column.classCallback) {
        dataItem.classList.add(column.classCallback(rowData));
      }
      if (style && !hidden) {
        setDataStyle(dataItem, style);
      } else if (hidden) {
        hideColumn(dataItem);
      }
      dataList.appendChild(dataItem);
    });

    dataContainerTag.appendChild(dataList);
    rows.push(dataList);
  });
};


// 전체선택 체크박스 상태
const isAllChecked = computed(() => {
  return props.data.length > 0 && selectedItems.value.length === props.data.length;
});

// 전체선택 토글 함수
function toggleAll(e) {
  const checked = e.target.checked;
  // 모든 row에 checked 값 반영
  props.data.forEach(row => {
    row.checked = checked;
  });
  // selectedItems도 동기화
  if (checked) {
    selectedItems.value = [...props.data];
  } else {
    selectedItems.value = [];
  }
  emit("update:selectedItems", selectedItems.value);
  // 리스트 리렌더링 필요시 _renderContents 호출
}

/**
 * @description _renderContent에 의해 실행되는 현재 리스트의 로우를 삭제하는 function
 ******************
 */
const _clearRows = () => {
  const dataContainerEl = getDataContainer();
  while (dataContainerEl.hasChildNodes()) {
    dataContainerEl.removeChild(dataContainerEl.firstChild);
  }
};

/**
 * @description _renderContent에 의해 실행되는 리스트의 데이터 로우를 생성하는 function
 *****************
 * @param {Number} columnCount: 해더 정보에서 추출한 컬럼의 수
 */
const createDataList = (columnCount: number) => {
  // 1. 리스트 엘리먼트 생성 및 클래스, 스타일 적용
  let dataList = document.createElement("li") as ListElement;
  dataList.toggleAttribute(scopeId.value);
  dataList.className = "data-list";
  dataList.style.gridTemplateColumns = gridTemplateColumns;

  // 3. 로우의 data object를 리턴하는 getData 함수 추가
  dataList.getData = function () {
    if (props.header) {
      let fieldList = props.header.map((item: any) => item.fieldname);
      let data = {};
      // 리스트 내부의 차일드를 찾아 button-cell이 아닌 경우에
      // getValue를 통해 데이터를 추출하고 data 오브젝트에 초기화
      this.childNodes.forEach((child: InputElement, idx: string | number) => {
        let item = child as InputElement;
        if (!item.hasAttribute("button-cell")) {
          data[fieldList[idx]] = item.getValue();
        }
      });
      return data;
    }
    return null;
  };

  // 2. 리스트 엘리먼트의 click 이벤트 핸들러 등록
  dataList.addEventListener("click", (event) => {
    let target = event.currentTarget as ListElement;
    // dataList.dispatchEvent(
    //   new CustomEvent("dataListClick", {
    //     detail: {
    //       row: target,
    //       data: target?.getData(),
    //     },
    //   })
    // );
    emit("dataListClick", {
      detail: {
        row: target,
        data: target?.getData(),
      },
    });
  });

  return dataList;
};

/**
 * @description컬럼 타입이 button인 경우에 button 엘리먼트를 생성하여 return 함
 *************
 * @param {any} buttonCallback 버튼에 표시될 텍스트
 * @param {String} fieldname button이 할당 된 필드명칭 (복수의 버튼 컬럼이 있을 경우 구별하기 위해)
 * @param {Object} item 버튼컬럼과 동일한 로우의 데이터 오브젝트
 */
const createButtonItem = (
  buttonCallback: any,
  fieldname: string,
  item: object
) => {
  // 1. 버튼 엘리먼트 생성 및 속성 정의
  let button = document.createElement("button");
  button.toggleAttribute(scopeId.value);
  button.setAttribute("button-cell", "");
  button.setAttribute("fieldname", fieldname);

  // 2. 버튼 click event handler 등록
  // 버튼 클릭시 해당 버튼의 fieldname과 버튼이 속한 로우의 데이터를 전달함
  button.onclick = (event) => {
    let target = event.currentTarget as ButtonElement;
    // button.dispatchEvent(
    emit("btnClick", {
      detail: {
        fieldname: target?.getAttribute("fieldname"),
        data: item,
      },
    });
    // );
    event.stopPropagation();
  };

  // 3. 데이터 타입에 따라 버튼의 텍스트 표시 방법을 결정
  let btnType = typeof buttonCallback;
  switch (btnType) {
    case "string":
    case "number":
      button.innerText = "";
      break;
    case "function":
      let callback = buttonCallback as Function;
      button.innerText = callback(item, button);
      break;
    case "boolean":
      if (btnType) {
        button.innerText = item[fieldname];
      } else {
        button.setAttribute("hidden", "true");
      }
      break;
    default:
      button.innerText = item[fieldname];
      break;
  }
  return button;
};

/**
 * @description 이미지 그리드 아이템 생성
 *************
 * @param {any} imageObj
 * @param {String} src
 * @param {Object} item
 */
const createImageItem = (imageObj: any, src: string, item: object) => {
  const imgTag = new Image(imageObj.width, imageObj.height) as ImageElement;
  imgTag.setAttribute("src", src);
  imgTag.setAttribute("image-cell", "");
  imgTag.getValue = () => {
    return src;
  };

  if (imageObj.zoomDialog) {
    imgTag.onclick = (event) => {
      const content = new Image(
        imgTag.clientWidth * 2,
        imgTag.clientHeight * 2
      );
      content.setAttribute("src", src);

      // imgTag.dispatchEvent(
      //   new CustomEvent("openDialog", {
      //     detail: {
      //       content: content,
      //       closeByModalClick: true,
      //       width: "fit",
      //       height: "fit",
      //     },
      //   })
      // );
      emit("openDialog", {
        detail: {
          content: content,
          closeByModalClick: true,
          width: "fit",
          height: "fit",
        },
      });

      event.stopPropagation();
    };
  }

  if ("style" in imageObj) {
    setDataStyle(imgTag, imageObj.style);
  }
  return imgTag;
};

/**
 * @description 컬럼 타입이 일반인 경우에 span 엘리먼트를 생성하고 return 함
 * @param {String} value 필드의 값
 * @param {Object} rowData 현재 필드가 속해 있는 로우의 데이터 오브젝트
 * @param {Function} displayCallback value를 대신하여 화면에 출력할 값을 결정하는 함수
 */
const createDataItem = (
  value: string,
  rowData: object,
  displayCallback: Function
) => {
  // 1. span 엘리먼트 생성 및 속성 정의
  let dataItem = document.createElement("span") as SpanElement;
  dataItem.toggleAttribute(scopeId.value);
  dataItem.className = "data-item";

  // 2. span 엘리먼트(필드)의 값을 결정하고 초기화
  // dataItem.value = value;
  if (displayCallback) {
    // displayCallback이 있는 경우 해당 함수 실행 결과를 field의 값으로 결정
    dataItem.innerHTML = displayCallback(value, rowData);
  } else {
    // displayCallback이 없을 경우 그냥 value를 초기화
    dataItem.innerHTML = value;
  }

  // 3. getValue function 추가 span 엘리먼트의 innerHTML (필드의 값)을 리턴함
  dataItem.getValue = () => {
    return value;
  };

  return dataItem;
};

/**
 * @description style 오브젝트에 정의된 스타일 항목들을 loop를 돌며 적용함
 *************
 * @param {HTMLElement} dataItem 필드의 값을 나타내는 span 엘리먼트
 * @param {Object} style 적용할 스타일의 오브젝트
 */
const setDataStyle = (dataItem: HTMLElement, style: object) => {
  for (let key in style) {
    dataItem.style[key] = style[key];
  }
};

/**
 * @description 해더의 속성에서 hidden 처리가 된 컬럼의 데이터 필드들을 hidden 처리함
 *************
 * @param {Object} dataItem 필드의 값을 나타내는 span 엘리먼트
 */
const hideColumn = (dataItem: HTMLElement) => {
  dataItem.style.display = "none";
};

/**
 * @description paginator 사용 여부를 결정하는 showPaginator의 값이 변경되면 호출
 *************
 * @param {Boolean} showPaginator 페이지네이션 처리 여부를 결정하는 flag
 */
const _showPaginatorChanged = (showPaginator: boolean) => {
  const paginatorContainer = getPaginator();

  if (props.showPaginator) {
    paginatorContainer.style.display = "flex";
    page = 1;
  } else {
    paginatorContainer.style.display = "none";
  }
};

/**
 * @description 페이지의 값이 변경될 경우 validation 이후 정상적이라면
 * page에 초기화하여 _pageChanged가 호출 되도록 함
 *************
 * @param {Object} event input on-change event
 */
const _initPage = (event: Event) => {
  const pageInput = event.currentTarget as InputElement;
  const inputPage = parseInt(pageInput.value);

  if (inputPage && inputPage > 0 && !noMorePage(inputPage)) {
    page = inputPage;
  } else {
    logisUtil.showMessage(
      t("error.page_is_not_valid"),
      t("text.please_type_valid_page"),
      pageInput.select
    );
  }
};

/**
 * @description 리미트 값이 변경될 경우 validation 이후 정상적이라면
 * limit에 초기화하여 _limitChanged가 호출 되도록 함
 *************
 * @param {Event} event input on-change event
 */
const _initLimit = (event: Event) => {
  const limitInput = event.currentTarget as InputElement;
  const limit = parseInt(limitInput.value);

  if (limit <= props.maxLimit && limit >= props.minLimit) {
    pageLimit = limit;
  } else {
    var msg = `${"text.valid_range"}: ${props.minLimit} ~ ${props.maxLimit}`;
    logisUtil.showMessage(t("error.out_of_range"), msg, limitInput.select);
  }
};

/**
 * @description data 값들이 변경될 때 호출되며
 * 페이지네이터의 beginDataNumber, endDataNumber, endPage를 계산하고 초기화 함
 *************
 * @param {Array} data
 */
const _setupPaginator = (data: Array<any>) => {
  if (data && page && props.total && props.limit) {
    beginDataNumber = _getBeginDataNum(page, props.limit);
    endDataNumber = _getEndDataNum(page, props.limit);
    endPage = _getEndPage(props.total, props.limit);
  }
};

/**
 * @description 현재 페이지에서 시작되는 데이터가 몇 번째 데이터인지 표시하기 위한 값 중
 * 첫번째 번호를 계산함
 *************
 * @return {Number}
 */
const _getBeginDataNum = (page: number, limit: number): number => {
  return (page - 1) * limit + 1;
};

/**
 * @description 현재 페이지에서 끝나는 데이터가 몇 번째 데이터인지 표시하기 위한 값 중
 * 마지막 번호를 계산함
 *************
 * @return {Number}
 */
const _getEndDataNum = (page: number, limit: number): number => {
  return (page - 1) * limit + props.data.length;
};

/**
 * @description 마지막 페이지 값을 계산함
 *************
 * @return {Number}
 */
const _getEndPage = (total: number, limit: number): number => {
  return Math.ceil(total / limit);
};

/**
 * @description page or limit 변경시 호출되며
 * paginator-changed 이벤트를 발생 시킴
 *************
 * @param {Number} page 현재 리스트의 페이지
 * @param {Number} limit 현재 리스트의 제한 수량
 */
const _paginatorChanged = (page: number, limit: number) => {
  if (props.data && props.data.length > 0) {
    _setupPaginator(props.data);
  }
  // getPaginator().dispatchEvent(
  //   new CustomEvent("paginatorChanged", {
  //     detail: {
  //       page: page,
  //       limit: limit,
  //     },
  //   })
  // );
  emit("paginatorChanged", {
    detail: {
      page: page,
      limit: limit,
    },
  });
};

/**
 * @description ion-icon click event handler
 * 이전 페이지로
 * _initPage를 통해 page 값을 초기화 하기 위해
 * input change 이벤트를 발생
 */
const prevPage = () => {
  if (page > 1) {
    const pageInput = getPageInput();
    pageInput.value = String(page - 1);
    // pageInput.dispatchEvent(new InputEvent("change"));
    emit("pageChanged", {
      detail: {
        target: pageInput,
      },
    });
    page = page - 1;
    props.paginatorChange({
      detail: {
        page: pageInput.value,
      },
    });
  }
};

/**
 * @description ion-icon click event handler
 * 다음 페이지로
 * _initPage를 통해 page 값을 초기화 하기 위해
 * input change 이벤트를 발생
 */
const nextPage = () => {
  if (!noMorePage(page + 1)) {
    const pageInput = getPageInput();
    pageInput.value = String(page + 1);
    // pageInput.dispatchEvent(new InputEvent("change"));
    emit("change", {
      detail: {
        target: pageInput.value,
      },
    });
    page = page + 1;
    props.paginatorChange({
      detail: {
        page: pageInput.value,
      },
    });
  }
};

/**
 * @description 현재 페이지 이상의 페이지가 있는지 여부를 return
 *************
 * @param {Number} page 현재 페이지 값
 * @return {Boolean}
 */
const noMorePage = (page: number): boolean => {
  return page > _getEndPage(props.total, props.limit);
};

defineExpose({
  getRows,
  createDataList,
  createDataItem,
  createButtonItem,
  createImageItem,
  setDataStyle,
  hideColumn,
});
</script>

<style lang="css">
#_item-list-container {
  display: flex;
  flex-direction: column;
  flex: 1;

  .header-list {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    /* 5열로 등분 */
    color: #f6f4f4;
    background: var(--ion-color-warning-shade);
    padding: 0.3rem 0.5rem 0.2rem 0.5rem;
    border-radius: 5px;
    margin: 5px 0px;
    font-size: 0.9rem;
    text-align: center;
  }

  .header-list span {
    margin: auto;
    /* 자동 여백으로 가운데 정렬 */
    display: flex;
    /* Flexbox로 내부 텍스트 정렬 */
    align-items: center;
    /* 세로 가운데 정렬 */
    justify-content: center;
    /* 가로 가운데 정렬 */
    white-space: nowrap;
    /* 텍스트 줄바꿈 방지 */
  }

  .body-container {
    display: flex;
    flex: 1;
    overflow-y: auto;
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: var(--default-border-radius);
  }

  .data-container {
    flex: 1;
    height: 0;
  }

  .data-list {
    display: grid;
    color: #ccccce;
    font-size: 0.8rem;
    border-bottom: 1px solid rgba(0, 0, 0, 0.2);
    padding: 0.3rem 0.5rem 0.2rem 0.5rem;
    justify-content: space-around;
  }

  .data-list.complete {
    color: var(--ion-color-danger-shade);
    font-weight: bold;
  }

  .data-list.underline {
    text-decoration-line: underline;
  }

  .data-list.wait {
    color: gray;
    /* font-weight: bold; */
  }

  .data-list.selected {
    color: var(--ion-color-danger-shade);
    font-weight: bold;
    background-color: #fcf8e2;
  }

  .data-list.canceled {
    color: var(--ion-color-warning-shade);
    font-weight: bold;
  }

  li:nth-child(even) {
    background-color: rgba(83, 98, 127, 0.1);
  }

  .data-item {
    display: -webkit-box;
    /* 핵심: 줄 제한 위해 box 형태로 */
    -webkit-box-orient: vertical;
    /* 세로 방향 */
    -webkit-line-clamp: 2;
    /* 최대 2줄까지만 표시 */
    overflow: hidden;
    /* 넘치는 내용 숨김 */
    text-overflow: ellipsis;
    /* 말줄임표 */
    white-space: normal;
    /* 줄바꿈 허용 */
    text-align: center;
    /* 텍스트 중앙 정렬 */
    margin: auto 0;
    padding-left: 8px;
  }

  .data-item.red::before,
  .data-item.blue::before,
  .data-item.yellow::before,
  .data-item.green::before {
    content: "";
    background: url("/images/icon-box.png") no-repeat 50% 0 transparent;
    display: inline-block;
    position: relative;
    top: 1px;
    width: 28px;
    height: 20px;
    margin: 0 5px 0 0;
    border-radius: 4px;
  }

  .data-item.red::before {
    background-color: var(--ion-color-danger-shade);
  }

  .data-item.blue::before {
    background-color: var(--ion-color-secondary-shade);
  }

  .data-item.yellow::before {
    background-color: var(--ion-color-warning-shade);
  }

  .data-item.green::before {
    background-color: var(--ion-color-success);
  }

  button:focus {
    outline: 0;
  }

  button {
    background: var(--ion-color-success);
    background: linear-gradient(to bottom,
        var(--ion-color-success) 0%,
        var(--ion-color-success-shade) 100%);
    border-top: 0.06rem solid var(--ion-color-success-tint);
    border-bottom: 0.2rem solid var(--ion-color-success-shade);
    border-left: none;
    border-right: none;
    border-radius: 12px;
    margin: 15px 0;
    padding: 0.3rem;
    color: var(--ion-color-primary-contrast);
    text-shadow: 0px 0.03rem 0.03rem rgba(0, 0, 0, 0.4);
    font-size: 0.6rem;
  }

  button:active {
    background: var(--ion-color-success);
    background: linear-gradient(to bottom,
        var(--ion-color-success-shade) 0%,
        var(--ion-color-success) 100%);
    text-shadow: 0.03rem 0rem 0.03rem rgba(0, 0, 0, 0.4);
  }

  .data-container button {
    margin: 0px;
  }

  .data-container[dialog="true"] {
    background-color: transparent;
    height: 500px;
  }

  .data-container[dialog="true"] li {
    color: var(--dark-text-color);
    font-size: 0.6rem;
  }

  .data-container[dialog="true"] li.complete {
    color: var(--ion-color-danger);
    font-size: 0.6rem;
  }

  .data-container[dialog="true"] li.underline {
    text-decoration-line: underline;
  }

  .data-container[dialog="true"] li.selected {
    color: var(--ion-color-danger);
    font-weight: bold;
    background-color: #fcf8e2;
  }

  .data-container[dialog="true"] li.canceled {
    color: var(--ion-color-warning-shade);
    font-weight: bold;
  }

  .paginator-container {
    display: none;
    background-color: rgba(0, 0, 0, 0.2);
    border: 1px solid rgba(0, 0, 0, 0.2);
    border-radius: 10px;
    margin-top: 5px;
    padding: 5px 12px 5px 5px;
    flex-direction: row;
    height: 30px;
    justify-content: end;
    font-size: initial;
  }

  .paginator {
    display: flex;
    flex-direction: row;
    flex: 1;
  }

  .paginator ion-icon {
    fill: #fff;
    height: auto;
  }

  .paginator b {
    padding-left: 15px;
    color: #fff;
  }

  .paginator ion-icon {
    background: linear-gradient(to bottom, #5a5959 0%, #383737 100%);
    padding: 0 3px;
    color: #7f95bd;
  }

  .paginator ion-icon:active {
    background: linear-gradient(to bottom, #3c3c3c 0%, #303030 100%);
  }

  .paginator ion-icon.prev-page {
    -webkit-border-top-left-radius: 10px;
    -webkit-border-bottom-left-radius: 10px;
    -moz-border-radius-topleft: 10px;
    -moz-border-radius-bottomleft: 10px;
    border-top-left-radius: 10px;
    border-bottom-left-radius: 10px;
    border-width: 1px 0 1px 1px;
  }

  .paginator ion-icon.next-page {
    -webkit-border-top-right-radius: 10px;
    -webkit-border-bottom-right-radius: 10px;
    -moz-border-radius-topright: 10px;
    -moz-border-radius-bottomright: 10px;
    border-top-right-radius: 10px;
    border-bottom-right-radius: 10px;
    border-width: 1px 1px 1px 0;
  }

  .page-summary {
    display: flex;
    flex-direction: row;
    flex: 1;
    justify-content: flex-end;
  }

  .paginator span,
  .page-summary span {
    color: silver;
    margin-top: auto;
    margin-bottom: auto;
  }

  input.page,
  input.limit {
    width: 40px;
    height: 20px;
    background-color: rgba(0, 0, 0, 0.25);
    border: 1px solid rgba(255, 255, 255, 0.2);
    color: #fff;
    text-align: center;
    font-size: initial;
  }

  input.limit {
    margin-left: 0.2rem;
    margin-right: 0.2rem;
    border-radius: 10px;
  }

  .checkbox-header {
    min-width: 0.1fr;   /* 원하는 최소 폭 */
    max-width: 0.2fr;   /* 원하는 최대 폭 */
    width: 0.15fr;       /* 고정 폭을 원하면 이것만 써도 됨 */
    justify-content: center;
  }

  .checkbox-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 0.1fr;   /* 원하는 최소 폭 */
    max-width: 0.2fr;   /* 원하는 최대 폭 */
    width: 0.15fr;       /* 고정 폭을 원하면 이것만 써도 됨 */
  }
}
</style>
