import { useI18n } from '/@/hooks/web/useI18n';
import { getCommonCodeByName, getEntityCodeByName } from '/@/api/common/api';

const { t } = useI18n();

export const parseSelectColumns = function (metaDataList) {
  // 메뉴 메타에 컬럼명 trailing/leading whitespace 가 섞이는 케이스가 실제로 관측됨
  // (예: "report_pdf_id " — 끝에 공백). tui-grid 컬럼 등록과 page 의 필드 접근키가
  // 불일치하면서 "수정 표시는 뜨지만 저장 안됨" 같은 미묘한 실패를 유발한다.
  // 입력 단계에서 정규화한다 (이후 모든 경로가 trim 된 name 을 본다).
  metaDataList.forEach(function (m) {
    if (m && typeof m.name === 'string') m.name = m.name.trim();
  });

  const gridColumnMetaList = metaDataList.filter(function (metaData) {
    if (metaData.name == 'id' && !metaData.grid_rank) metaData.grid_rank = -10;
    // file-upload 컬럼은 grid_rank 와 무관하게 항상 포함 — 셀 값(=file_id) 이
    // 서버 SELECT 절에 반드시 들어와야 fetch 후 row 데이터에 동기화됨.
    if (metaData.grid_editor === 'file-upload') return true;
    return metaData.grid_rank && metaData.grid_rank != 0;
  });

  let selectColumns = '';

  gridColumnMetaList.map(function (metaData) {
    if (!metaData.virtual_field || metaData.virtual_field == false) {
      selectColumns += metaData.name + ',';
    }
  });

  let selectFields: String | null = null;
  if (selectColumns.length > 2) {
    selectFields = selectColumns.substring(0, selectColumns.length - 1);
  }

  return selectFields;
};

/**
 * 서버로 부터 조회한 그리드 컬럼 관련 메타 데이터를 gridColumns로 변환한다.
 *
 * @param {Array} metaDataList 서버로 부터 조회한 그리드 컬럼 관련 메타 데이터 리스트
 * @return {Array} gridColumns 그리드 구성에 필요한 그리드 컬럼 데이터
 */
export const parseGridColumns = async function (metaDataList) {
  // 컬럼명 whitespace 정규화 — parseSelectColumns 와 동일 사유.
  // 보통은 parseSelectColumns 가 먼저 호출되어 이미 trim 되어 있지만,
  // 본 함수만 단독 호출되는 경로도 있어 방어적으로 한 번 더 trim.
  metaDataList.forEach(function (m) {
    if (m && typeof m.name === 'string') m.name = m.name.trim();
  });

  const gridColumnMetaList = metaDataList.filter(function (metaData) {
    if (metaData.name == 'id' && !metaData.grid_rank) metaData.grid_rank = -10;
    // file-upload 컬럼은 grid_rank 와 무관하게 항상 등록 — tui-grid 에 미등록이면
    // setValue 가 no-op 이 되어 file_id 가 row 에 박히지 않는다.
    if (metaData.grid_editor === 'file-upload') return true;
    return metaData.grid_rank && metaData.grid_rank != 0;
  });

  sortMetaData(gridColumnMetaList, 'grid_rank');

  return await parseToastGrid(gridColumnMetaList);
  // return parseForDataludiGrid(gridColumnMetaList);
};

const parseToastGrid = async function (gridColumnMetaList) {
  const toastCols = await Promise.all(
    gridColumnMetaList.map(async function (metaData) {
      const field: any = {
        name: metaData.name,
        align: metaData.grid_align,
        minWidth: metaData.grid_width,
        hidden: metaData.grid_editor == 'hidden' || metaData.grid_rank == -10,
        sortable: true,
      };
      const title = t(metaData.term);
      field.header = title;
      const gridEditor = metaData.grid_editor;

      if (gridEditor === 'resource-popup') {
        const { default: ResourcePopupCellEditor } = await import(
          './components/ResourcePopupCellEditor'
        );
        const { default: ResourcePopupCellRenderer } = await import(
          './components/ResourcePopupCellRenderer'
        );

        field.editor = {
          type: ResourcePopupCellEditor,
          options: {
            // ref_name: 팝업 컴포넌트 경로 또는 이름
            //   예) '/tspg_4way/menu/inbound/popup/ItemSearchPopup'
            //   예) 'ItemSearchPopup'
            popupComponent: metaData.ref_name,
            // ref_related: 선택 시 같이 채울 필드 매핑 (선택사항)
            //   예) 'item_code:item_code,item_name:item_name,item_unit:item_unit'
            //   형식: "팝업결과키:셀컬럼명, ..."
            bindFields: metaData.ref_related || '',
          },
        };
        // 셀에 돋보기 아이콘이 보이도록 renderer 추가 (선택사항이지만 UX 좋음)
        field.renderer = {
          type: ResourcePopupCellRenderer,
        };
      } else if (gridEditor === 'text') {
        field.editor = 'text';
      } else if (gridEditor === 'code-combo') {
        const options = await getCommonCodeByName(metaData.ref_name);
        options.unshift({ text: ' ', value: ' ' });
        const listItems = { listItems: options };
        const editor = { type: 'select', options: listItems };
        // ★ 편집 전에도 명칭 표시되도록 커스텀 formatter 적용
        const dict = new Map(options.map((o) => [String(o.value), o.text]));
        field.formatter = ({ value }) => dict.get(String(value)) ?? value;
        field.editor = editor;
      } else if (gridEditor === 'resource-code') {
        const options = await getEntityCodeByName(metaData.ref_name);
        options.unshift({ text: ' ', value: ' ' });
        const listItems = { listItems: options };
        const editor = { type: 'select', options: listItems };
        field.formatter = 'listItemText';
        field.editor = editor;
      } else if (gridEditor === 'date-picker') {
        // ant-design-vue DatePicker 기반 커스텀 셀 에디터
        // (검색 폼 formGenerate.ts 의 date-picker 와 동일한 'YYYY-MM-DD' 포맷)
        const { default: DatePickerCellEditor } = await import('./components/DatePickerCellEditor');
        field.editor = {
          type: DatePickerCellEditor,
        };
        // 셀 표시 포맷 — 'YYYY-MM-DD HH:mm:ss' 로 와도 날짜 부분만 표시
        field.formatter = ({ value }) => {
          if (!value) return '';
          if (typeof value === 'string') return value.substring(0, 10);
          if (value instanceof Date) {
            const y = value.getFullYear();
            const m = String(value.getMonth() + 1).padStart(2, '0');
            const d = String(value.getDate()).padStart(2, '0');
            return `${y}-${m}-${d}`;
          }
          return String(value);
        };
      } else if (gridEditor === 'file-upload') {
        const { default: FileUploadCellRenderer } = await import(
          './components/FileUploadCellRenderer'
        );

        // ref_type → accept 매핑
        //   'pdf'   → '.pdf'
        //   'excel' → '.xlsx,.xls'
        //   'image' → 'image/*'
        //   'any' / 빈값 → '*'
        //   그 외 직접 문자열은 그대로 사용 (예: '.pdf,.xlsx')
        const refType = (metaData.ref_type || '').toLowerCase();
        let accept = '*';
        if (refType === 'pdf') accept = '.pdf';
        else if (refType === 'excel') accept = '.xlsx,.xls';
        else if (refType === 'image') accept = 'image/*';
        else if (refType && refType !== 'any') accept = metaData.ref_type;

        // ref_related 로 추가 옵션 전달 (key=value;key=value ...)
        //   예) 'category=qc_test'
        const extraOpts: Record<string, string> = {};
        if (metaData.ref_related) {
          String(metaData.ref_related)
            .split(';')
            .map((s) => s.trim())
            .filter(Boolean)
            .forEach((pair) => {
              const [k, v] = pair.split('=');
              if (k && v) extraOpts[k.trim()] = v.trim();
            });
        }

        field.renderer = {
          type: FileUploadCellRenderer,
          options: {
            accept,
            // category: 공통 파일 저장소 디렉토리 분기용. 미지정 시 컬럼명을 fallback.
            category: extraOpts.category || metaData.name,
          },
        };
      } else if (gridEditor === 'readonly') {
        // readOnly가 모두 매핑되는 값이 있는 것은 아니다.
        // 참조 이름이 있는지 확인
        if (metaData.ref_name !== undefined && metaData.ref_name?.trim() !== '') {
          const options = await getCommonCodeByName(metaData.ref_name);
          field.meta = options; // 공통코드 변환 값을 추가한다.
          const optionDict = new Map(options.map((option) => [String(option.value), option.text]));
          field.formatter = ({ value }) => optionDict.get(String(value)) ?? value;
        }
      } else if (gridEditor === 'checkbox') {
        const editor = { type: 'boolean' };
        field.renderer = editor;
      } else if (gridEditor === 'image-circe') {
        field.renderer = {
          type: 'icon',
          options: {
            icon: (record) => {
              let icon: any = '';
              const value: any = record[metaData.name];
              if (value === '0') icon = 'noto:green-circle';
              else if (value === '1') icon = 'noto:orange-circle';
              else if (value === '2') icon = 'noto:red-circle';
              else if (value === '4') icon = 'noto:brown-circle';
              else if (value === '5') icon = 'noto:yellow-circle';
              else if (value === '6') icon = 'noto:purple-circle';
              else if (value === '7') icon = 'noto:hollow-red-circle';
              else if (value === '8') icon = 'noto:yellow-circle';
              else if (value === 0) icon = 'noto:green-circle';
              else if (value === 1) icon = 'noto:orange-circle';
              else if (value === 2) icon = 'noto:red-circle';
              else icon = 'noto:white-circle';

              return icon;
            },
          },
        };
      } else if (gridEditor === 'password') {
        console.log('[parseToastGrid] gridEditor password');
        field.editor = 'password';
        field.renderer = {
          type: 'password',
          options: {
            icon: (shown) => {
              let icon: any = '';
              if (shown) icon = 'ant-design:eye-invisible-outlined';
              else icon = 'ant-design:eye-outlined';
              return icon;
            },
          },
        };
      } else {
        field.editor = 'text';
      }
      return field;
    }),
  );
  return toastCols;
};
/**
 * Resource Column 구성
 *
 * @param {Object} metaData 서버로 부터 조회한 서버측 메타 데이터
 * @param {Object} field 그리드 필드 데이터
 */
export const newResourceColumn = function (metaData, field) {
  const titleField = metaData.grid_format ? metaData.grid_format : 'name';
  field.userData = {
    titleField: titleField,
  };
  field.name = field.name.substr(0, field.name.indexOf('_id'));
  field.fieldName = field.name;

  // 1. Object 형식의 데이터를 화면에 표시하기 위한 콜백 함수
  field.displayCallback = function (index, value) {
    const rowObj = index.getRow().getRowObject();
    const fieldName = index.column.dataFieldName();
    const userData = index.column.userData();
    const columnObj = rowObj[fieldName];
    let retVal = columnObj ? columnObj[userData.titleField] : '';

    if (columnObj && columnObj.description) {
      retVal = retVal + ' (' + columnObj.description + ')';
    }

    return retVal;
  };

  // 2. Object 형식의 데이터를 Text 형식으로 클립보드에 전달
  (field.copyCallback = function (row, field, value) {
    return value ? JSON.stringify(value) : value;
  }),
    // 3. Text 형식으로 전달된 클립보드 데이터를 전달받아 Paste Logic 수행
    (field.pasteCallback = function (row, field, text) {
      return text ? JSON.parse(text) : text;
    });
};

/**
 * 범위 값 정보를 파싱
 * 상세 폼, 그리드의 값의 범위를 제한하기 위한 필드
 * 예) 숫자형의 경우 : 0,100 or min:0 or max:100
 * 예) 날짜형의 경우 : 2016-01-01,today or min:today or max:today + 1
 *
 * @param {String} rangeVal 범위값
 * @param {String} type 범위값의 타입
 */
export const parseGridValueRange = function (rangeVal, type) {
  if (!rangeVal || rangeVal.length < 1) return null;
  if (type != 'number' && type != 'date') return null;

  const rangeType =
    rangeVal.indexOf(',') > 0
      ? 'min-max'
      : rangeVal.indexOf('min') >= 0
      ? 'min'
      : rangeVal.indexOf('max') >= 0
      ? 'max'
      : 'none';
  if (rangeType == 'none') return null;
  let min: any = null,
    max: any = null;

  if (rangeType == 'min-max') {
    min = rangeVal.split(',')[0];
    max = rangeVal.split(',')[1];
  } else if (rangeType == 'min') {
    min = rangeVal.substr(4).trim();
  } else if (rangeType == 'max') {
    max = rangeVal.substr(4).trim();
  }

  // 날짜형인 경우
  if (type == 'date') {
    min = calcGridRangeDate(min);
    max = calcGridRangeDate(max);
  }

  return [min, max];
};

/**
 * 문자형 데이터를 Date 객체로 파싱하여 리턴한다.
 *
 * @params {String} dateStr 문자로 된 날짜 데이터
 * @return {Date} dateStr을 파싱한 Date 객체
 */
export const calcGridRangeDate = function (dateStr) {
  if (!dateStr) return null;

  // 1. dateStr이 today가 들어가 있다면
  if (dateStr.indexOf('today') >= 0) {
    dateStr = dateStr.replace('today', 'var dt = new Date(); dt.setDate(dt.getDate()') + ')';
    const time = eval(dateStr);
    return new Date(time);

    // 2. dateStr이 2016-10-21 처럼 날짜형이라면 ...
  } else {
    return new Date(dateStr);
  }
};

/**
 * 배열 객체인 list를 sortField를 중심으로 소팅한다.
 *
 * @params {Array} list 배열 객체
 * @params {String} sortField 배열 객체 중 소트에 활용할 필드 명
 */
export const sortMetaData = function (list, sortField) {
  list.sort(function (a, b) {
    return a[sortField] > b[sortField] ? 1 : b[sortField] > a[sortField] ? -1 : 0;
  });
};
