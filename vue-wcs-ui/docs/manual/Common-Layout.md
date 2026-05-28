# 공통 레이아웃 가이드

## 목차
<!-- TOC -->
* [1. 설명](#1-설명)
* [2. 레이아웃 구조](#2-레이아웃-구조)
* [3. 레이아웃 종류 별 사용방법](#3-레이아웃-종류-별-사용방법)
  * [사용 우선순위](#사용-우선순위)
  * [index.vue](#span-stylecolor007bffindexvue-span)
    * [사용방법](#사용방법)
    * [세부구조](#세부구조)
  * [CommonPage.vue](#span-stylecolor007bffcommonpagevue-span)
    * [사용방법](#사용방법-1)
  * [CommonPage.vue + ButtonGroup.vue](#span-stylecolor007bffcommonpagevue--buttongroupvuespan)
    * [사용방법](#사용방법-2)
    * [세부 구조](#세부-구조)
  * [CodeEditorModal.vue](#span-stylecolor007bffcodeeditormodalvuespan)
    * [사용방법](#사용방법-3)
    * [세부구조](#세부구조-1)
<!-- TOC -->
---

## 1. 설명

공통 레이아웃 구성 및 사용지침을 정리한 문서이며 표는 Tui Grid, 차트는 ECharts 컴포넌트를 사용합니다. 

## 2. 레이아웃 디렉토리 구조

- src > views > common

## 3. 레이아웃 종류 별 사용방법

#### 사용 우선순위

1. index.vue 
    - CRUD, 커스텀 불가 
2. CommomPage.vue + ButtonGroup.vue
    - CRUD, 커스텀 가능 
3. CommonPage.vue
    - Read only, 커스텀 불가

---

### <span style="color:#007BFF">index.vue </span>
#### 사용방법

- **[시스템]-[메뉴]-[컬럼셋업]-[메뉴]-[경로]** 에 `/common/index` 입력
- **[시스템]-[메뉴]-[컬럼셋업]-[버튼 셋업]** 에 버튼 추가

#### 세부구조

<details style="background-color:#E6F2FF; padding: 2px 4px; font-weight: bold;"><summary>참조 및 하위 전달 Props [클릭]</summary>

  > `ref="commPageRef"`  
  > → CommonPage.vue 컴포넌트의 메서드, 속성에 접근하기 위한 ref  
  > → CommonPage.vue의 defineExpose로 지정된 메서드/속성에 접근 가능
  
  > `:limit="limit"`  
  > → 페이지 당 최대 레코드 수 (기본값 100)
  
  > `:fetchHandler="fetchHandler"`  
  > → 데이터 조회함수 설정
  
  > `@gridFetched="handleGridCreated"`  
  > → 데이터 조회 후 fetchStore 조회 완료 처리
  
  > `:buttonlist="buttonlist"`  
  > → [시스템]-[메뉴]-[컬럼셋업]-[버튼 셋업] 에 작성한 버튼 리스트 사용
  
  > `@btnHandler="btnHandler"`  
  > → [시스템]-[메뉴]-[컬럼셋업]-[버튼 셋업]에 작성한 label.text 이름으로 버튼 핸들러 생성  
  > → label.text에 `save` 입력하면 `saveBtnHandler` 생성
  
</details>

---

### <span style="color:#007BFF">CommonPage.vue </span>

#### 사용방법

- **[시스템]-[메뉴]-[컬럼셋업]-[메뉴] 설정**
  - **경로** : `/common/CommonPage`
  - **리소스 URL** : 조회 url
  - **라우팅** : 빈값이 아닌 값

---

### <span style="color:#007BFF">CommonPage.vue + ButtonGroup.vue</span>

#### 사용방법

- **[시스템]-[메뉴]-[컬럼셋업]-[메뉴]-[경로]** 에 `/[Vue 트리 내 CustomPage.vue가 위치한 경로]` 입력
- 커스텀 페이지 생성(PascalCase)

  ```vue
  <!-- 예) CustomPage.vue -->

  <template>
    <!-- case1: 한 번에 입력 -->
    <CommonPage
      ref="commPageRef"
      v-bind="totalProps"
      @gridClicked="onGridClicked"
      @gridChecked="onGridChecked"
      @gridDbClicked="onGridDbClicked"
      @gridUnChecked="onGridUnChecked"
      @gridFetched="onGridFetched"
    >
      <ButtonGroup :buttonlist="buttonlist" @btnHandler="btnHandler" />
    </CommonPage>

    <!-- case2: 분할해서 입력 -->
    <CommonPage
      ref="commPageRef"
      :limit="limit"
      :menuName="menuName"
      :menuMetaProp="menuMetaProp"
      :metas="metas"
      :rowHeaders="rowHeaders"
      :optionCheck="optionCheck"
      :resourceId="resourceId"
      :actionColumns="actionColumns"
      :fetchHandler="fetchHandler"
      :baseColProps="baseColProps"
      :showSearchForm="true"
      :showPagination="true"
      :showButtons="true"
      @gridClicked="onGridClicked"
      @gridChecked="onGridChecked"
      @gridDbClicked="onGridDbClicked"
      @gridUnChecked="onGridUnChecked"
      @gridFetched="onGridFetched"
    />
  </template>

  <script lang="ts" setup>
    import { ref, computed } from 'vue';
    import { getQueryFilters, hasKeyWithFormat } from '/@/views/common/utils';
    import { getSearchList, updateList } from '/@/api/common/api';
    import CommonPage from '/@/views/common/CommonPage.vue';
    import ButtonGroup from '/@/views/common/ButtonGroup.vue';
    // --------------------------------------------------------+
    /* CommonPage.vue 참조 */
    const commPageRef = ref();
    // --------------------------------------------------------+
    /* CommonPage.vue에서 관리하는 데이터 */
    let buttonList = computed(() => commPageRef.value?.buttons);              // 메뉴UI에서 설정한 버튼 정보

    /* (Optional) CommonPage.vue에서 관리하는 데이터
    * 필요한 데이터만 선언하여 사용
    * 주로 gridRef, getFormFields, gridSaveUrl 사용
    */
    let gridRef = computed(() => commPageRef.value?.grid);                    // Grid.vue 참조 / 그리드 제어에 사용
    let columns = computed(() => commPageRef.value?.columns);                 // 메뉴UI에서 설정한 컬럼정보 및 props로 전달한 actionColumns / 동일 column은 메뉴UI 설정 우선
    let form = computed(() => commPageRef.value?.form);                       // BasicForm.vue 참조 / 검색 폼 제어에 사용
    let getFormFields = computed(() => commPageRef.value?.getFormFields);     // 검색 데이터 조회 함수
    let validate = computed(() => commPageRef.value?.formValidate);           // 검색 데이터 유효성 검사 함수
    let originMetas = computed(() => commPageRef.value?.getMenuMetas);        // 현재 페이지 메타정보
    let originSearchProps = computed(() => commPageRef.value?.searchProps);   // 현재 검색 조건
    let resourceUrl = computed(() => commPageRef.value?.resourceUrl);         // 최종 조회 url
    let gridSaveUrl = computed(() => commPageRef.value?.gridSaveUrl);         // 최종 저장 url
    let downExcel = computed(() => commPageRef.value?.downExcel);             // 엑셀 다운로드 함수
    let resourceName = computed(() => commPageRef.value?.resourceName);       // 메뉴UI에서 설정한 리소스명
    // --------------------------------------------------------+
    /* (Optional) CommonPage.vue로 전달할 props
    * 필요한 데이터만 선언하여 사용
    * 주로 actionColumns 사용
    */
    const limit = 50;           // 페이지 당 최대 레코드 수 / default : 50
    const menuName = '';        // 메타데이터 조회 url 일부 / default : 메뉴UI에서 설정한 route.name / metas, metaUrl과 동시에 전달 할 경우 무시됨
    const metaUrl = '';         // 조회 url / metas와 동시에 전달할 경우 무시됨
    const metas = {'menu': {}, 'buttons': [], 'columns': [], 'menu_params': {}};      // 덮어쓸 메뉴 메타데이터 / menuMetaProp과 동시에 전달 할 경우 menuMetaProp 데이터 반영
    const menuMetaProp = ['menu':{}, 'buttons':[], 'columns':[], 'menu_params': {}];  // 덮어쓸 메뉴 메타데이터 / default: 메뉴UI에서 설정한 정보
    const resourceId = '';      // 조회 파라미터 id / ex) 메뉴UI 내 resource_url에 '/test/:id' 입력 → props.resourceId = 'id1'  전달 → '/test/id1'
    const baseColProps = {};    // default { xxl: 7, lg: 7, md: 7, sm: 28 }
    // showSearchForm           // 검색 필드 표시/숨김 여부 / default : true
    // showPagination           // 그리드 페이지네이션 사용 여부 / default : true
    // showButtons              // 버튼 표시/숨김 여부 / default : true
    // --------------------------------------------------------+
    /** ButtonGroup.vue 컴포넌트 emit 이벤트 수신 후, 액션 핸들러 중계 */
    async function btnHandler(param: any) {
        switch (param) {
          case 'addBtnHandler': addRow(); break;
          case 'saveBtnHandler': saveRows(); break;
          case 'deleteBtnHandler': deleteRows(); break;
          case 'exportBtnHandler': downExcel(); break;
          case 'clear-cacheBtnHandler': clearCash(); break;
          case 'exceldownBtnHandler': exceldown(); break;
          default: break;
        }
    }

    /** ✔ 버튼 액션 구현부 - 필요 시 수정 / 세부구현은 /common/index.vue 참고 */
    const addRow () => {};
    const saveRows = async () => {};
    const deleteRows = () => {};
    async function downExcel() {};
    async function clearCash() {};
    // --------------------------------------------------------+
    /** (Optiona) 화면에서만 사용하는 컬럼 추가
    * default : []
    */
    const actionColumns = [
        {
            name: 'detail_btn',           // 필드명
            header: t('label.detail'),    // 용어 적용
            align: 'center',              // 필드 정렬
            width: 40,                    // 필드 너비
            minWidth: 40,                 // 필드 너비
            renderer: {                   // 커스텀 렌더링 (아이콘 표시). 아이콘 외 다른 타입 렌더링 가능
            type: 'icon',
                options: {
                    icon: (record) => {
                        return 'ant-design:search-outlined'
                    }
                },
            },
        },
    ];  
    // --------------------------------------------------------+
    /** (Optional) Grid.vue 컴포넌트 emit 이벤트 수신 및 액션 구현 */
    const onGridClicked = (clickedRow) => {};
    const onGridChecked = (checkedRow) => {};
    const onGridDbClicked = (clickedRow) => {};
    const onGridUnChecked =(unCheckedRow) => {};
    const onGridFetched = (records) => {};
    // --------------------------------------------------------+
    /** (Optional) 데이터 조회 커스텀 함수
    * default : CommonPage.vue 내 fetcher() 사용
    *
    * @param page         page번호
    * @param limit        페이지 당 레코드 수 제한
    * @param sorters      정렬 조건 목록
    * @param searchProps  검색 조건
    */
    async function fetchHandler(page: any, limit: any, sorters: any, searchProps: any,) {
        // ✔ 필요시 여기에 정렬 기준 추가
        sorters = [{ field: '컬럼명(snake_case)', ascending: false }],

        // 검색 폼 데이터 - 유효성 검사, 조회, 날짜 범위 검색 필드를 유효한 형식으로 변환, 파라미터 형식으로 변경
        await validate.value();
        const fields = getFormFields.value();
        hasKeyWithFormat(fields, 'Custom String', searchProps);
        const queryFilters = await getQueryFilters(fields, searchProps);

        // ✔ 필요시 여기에 쿼리조건 추가
        const customFilters = [
          {
            name: '컬럼명(snake_case)',
            operator: 'in',
            value: '값',
            relation: false,
          }
        ];
        const mergedQueryFilters = [...customFilters, ...queryFilters];

        const requestParams = {
          page,
          limit,
          sort: JSON.stringify(sorters),
          query: JSON.stringify(mergedQueryFilters),
        };

        const response = await getSearchList(resourceUrl.value, requestParams);

        // 페이지네이션이 true면 Array응답
        if (Array.isArray(response)) {
            return {
                total: response.length,
                records: response,
            };
        } else {
            return {
                total: response.total,
                records: response.items,
            };
        }
    }
    // --------------------------------------------------------+
    /** (Optional) 검색 폼 제어
    * BasicForm.vue 내 function 사용방법
    * [form.value?.함수명] 형식
    */
    function func2() {
      form.value?.getSchema();
    }

    /** (Optional) 그리드 제어
    * Grid.vue 내 function 사용방법
    * [gridRef.value?.함수명] 형식
    */
    function func1() {
      gridRef.value?.addRow();
    }
  </script>
  ```

#### 세부 구조

<details style="background-color:#E6F2FF; padding: 2px 6px; font-weight: bold;"><summary>참조 및 하위전달 Props [클릭]</summary>

  > `ref="commPageRef"`  
  > → CommonPage.vue 컴포넌트의 메서드, 속성에 접근하기 위한 ref  
  > → CommonPage.vue의 defineExpose로 지정된 메서드/속성에 접근 가능

  > `@btnHandler="btnHandler"`  
  > → [시스템]-[메뉴]-[컬럼셋업]-[버튼 셋업]에 작성한 label.text 이름으로 버튼 핸들러 생성  
  > → label.text에 `save` 입력하면 `saveBtnHandler` 생성
  
</details>

### <span style="color:#007BFF">CodeEditorModal.vue</span>
#### 사용방법
- 업데이트 예정

#### 세부구조
- 업데이트 예정

---
