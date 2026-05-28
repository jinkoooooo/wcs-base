import { useI18n } from '/@/hooks/web/useI18n';
import { formatToDate } from '/@/utils/dateUtil';
import { getCommonCodeByName, getEntityCodeByName } from '/@/api/common/api';
import dayjs from 'dayjs';

const { t } = useI18n();
// import type { FormActionType, FormProps, FormSchema } from '/@/components/Form';
/**
 * meta data로 부터 select fields를 파싱한다.
 *********
 * @param {Array} metaDataList
 * @return {String} selectFields
 */
export const parseSelectFields = function (metaDataList) {
  let listMetaList = metaDataList.filter(function (metaData) {
    return (
      metaData.name == 'id' ||
      (metaData.grid_rank &&
        metaData.grid_rank > 0 &&
        (!metaData.virtual_field || metaData.virtual_field == false))
    );
  });

  let selectColumns = listMetaList.map(function (metaData) {
    if (
      metaData.grid_editor &&
      (metaData.grid_editor.indexOf('resource-selector') >= 0 ||
        metaData.grid_editor.indexOf('resource-column') >= 0 ||
        metaData.grid_editor.indexOf('file-selector') >= 0)
    ) {
      return metaData.name.substr(0, metaData.name.indexOf('_id'));
    } else {
      return metaData.name;
    }
  });

  return selectColumns.join(',');
};

/**
 * meta data로 부터 SortFields로 파싱한다.
 *********
 * @param {Array} metaDataList
 * @return {Array} sortFields
 */
export const parseSortFields = function (metaDataList) {
  let orderMetaList = metaDataList.filter(function (metaData) {
    return metaData.sort_rank && metaData.sort_rank > 0;
  });

  if (!orderMetaList || orderMetaList.length == 0) return;

  sortMetaData(orderMetaList, 'sort_rank');

  return orderMetaList.map(function (metaData) {
    let asc = true;
    if (metaData.reverse_sort && metaData.reverse_sort === true) asc = false;
    return { field: metaData.name, ascending: asc };
  });
};

/**
 * meta data로 부터 searchFormFields로 파싱한다.
 *********
 * @param {Array} metaDataList
 * @return {Array} searchFormFields
 */
export const parseSearchFormFields = function (metaDataList) {
  let searchMetaList = metaDataList.filter(function (metaData) {
    return metaData.search_rank && metaData.search_rank > 0;
  });
  sortMetaData(searchMetaList, 'search_rank');

  return searchMetaList.map(function (metaData) {
    if (!metaData.search_editor) metaData.search_editor = 'Input';

    let field: any = {
      // Field name
      field: metaData.name,
      // Label name
      label: t(metaData.term),
      // render component
      component: metaData.search_editor,
      // Component parameters
      componentProps: {},
      operator: metaData.search_oper ? metaData.search_oper : 'eq',
      colProps: { span: 8 },
    };
    if (metaData.search_editor) parseSearchFormField(field, metaData);
    if (metaData.search_init_val) {
      if (metaData.search_editor == 'date-picker') {
        field.default = calcDate(metaData.search_init_val);
      } else if (metaData.search_editor == 'ranged-date-picker') {
        field.defaultValue = [
          dayjs(calcDate(metaData.search_init_val), 'YYYY-MM-DD'),
          dayjs(calcDate(metaData.search_init_val), 'YYYY-MM-DD'),
        ];
      } else {
        field.default = metaData.search_init_val;
      }
    }

    return field;
  });
};

/**
 * form field를 파싱한다.
 *********
 * @param {Object} field
 * @param {Object} metaData
 * @param {Boolean} resourceForm resourceForm인지 아닌지 여부
 */

export const parseSearchFormField = function (field, metaData) {
  if (field.component) {
    const searchEditor = field.component;
    if (searchEditor === 'text') {
      field.component = 'Input';
      if (metaData.search_init_val) {
        field.defaultValue = metaData.search_init_val;
      }
      // if (metaData.col_size <= 0) {
      //   field.dynamicDisabled = true;
      // }
    } else if (searchEditor === 'resource-selector') {
      //TODO change it as popup modal
      field.component = 'Input';
      if (metaData.search_init_val) {
        field.defaultValue = metaData.search_init_val;
      }

      // if (metaData.col_size <= 0) {
      //   field.dynamicDisabled = true;
      // }
    } else if (searchEditor === 'resource-popup') {
      // 돋보기 팝업 연동 필드 — CommonPage 가 메타의 팝업 컴포넌트 이름/경로를 보고
      // 자동으로 .vue 를 import 해 Modal 을 띄운다. 부모 페이지에서 팝업을 배치하거나
      // resourcePopupClick 핸들러를 작성할 필요가 없다.
      //   - popup_component (우선) : 띄울 팝업 이름 또는 경로 (예: 'ItemSearchPopup')
      //   - ref_name      (fallback): 팝업 컴포넌트 이름 겸 조회 타겟 키
      field.component = 'Input';
      field.slot = `popup_${metaData.name}`;   // ★ BasicForm slot 이름
      field.resourcePopup = true;
      field.resourcePopupTarget = metaData.ref_name;
      field.resourcePopupComponent = metaData.popup_component || metaData.ref_name;

      console.log(`field.resourcePopupTarget : ${field.resourcePopupTarget}`);
      if (metaData.search_init_val) {
        field.defaultValue = metaData.search_init_val;
      }
    } else if (searchEditor === 'code-combo') {
      field.component = 'ApiSelect';
      field.componentProps = {
        api: getCommonCodeByName,
        params: metaData.ref_name,
        labelField: 'text',
        valueField: 'value',
        immediate: true,
      };

      //code-combo 검색그리드에서 조건이 in일경우 다중선택
      if (metaData.search_oper == 'in') {
        field.componentProps.mode = 'multiple';
      }

      if (metaData.search_init_val) {
        field.defaultValue = metaData.search_init_val;
      }

      // if (metaData.col_size <= 0) {
      //   field.dynamicDisabled = true;
      // }
    } else if (searchEditor === 'resource-code') {
      field.component = 'ApiSelect';
      field.componentProps = {
        api: getEntityCodeByName,
        params: metaData.ref_name,
        labelField: 'text',
        valueField: 'value',
      };

      //resource-combo 검색그리드에서 조건이 in일경우 다중선택
      if (metaData.search_oper == 'in') {
        field.componentProps.mode = 'multiple';
      }

      if (metaData.search_init_val) {
        field.defaultValue = metaData.search_init_val;
      }

      // if (metaData.col_size <= 0) {
      //   field.dynamicDisabled = true;
      // }
    } else if (searchEditor === 'date-picker') {
      field.component = 'DatePicker';
      field.componentProps = {
        valueFormat: 'YYYY-MM-DD',
      };

      if (metaData.search_init_val) {
        field.defaultValue = calcDate(metaData.search_init_val);
      }

      // if (metaData.col_size <= 0) {
      //   field.dynamicDisabled = true;
      // }
    } else if (searchEditor === 'ranged-date-picker') {
      field.component = 'RangePicker';
      field.componentProps = {
        format: 'YYYY-MM-DD',
      };

      // if (metaData.col_size <= 0) {
      //   field.dynamicDisabled = true;
      // }
    } else if (searchEditor === 'switch') {
      field.component = 'Switch';
      if (metaData.search_init_val) {
        field.defaultValue = metaData.search_init_val;
      }

      // if (metaData.col_size <= 0) {
      //   field.dynamicDisabled = true;
      // }
    } else if (searchEditor === 'tristate-radio') {
      field.component = 'RadioGroup';
      // field.componentProps = {
      //   api: getEntityCodeByName(metaData.ref_name),
      //   labelField: 'name',
      //   valueField: 'description',
      // };
      field.componentProps = {
        options: [
          { label: 'No', value: 'no' },
          { label: 'Yes', value: 'yes' },
          { label: 'All', value: 'all' },
        ],
      };
      if (metaData.search_init_val) {
        field.defaultValue = metaData.search_init_val;
      }

      // if (metaData.col_size <= 0) {
      //   field.dynamicDisabled = true;
      // }
    } else if (searchEditor === 'readonly') {
      field.component = 'Input';
      field.dynamicDisabled = true;
      field.defaultValue = metaData.search_init_val;
    } else {
      field.component = 'Input';
      if (metaData.search_init_val) {
        field.defaultValue = metaData.search_init_val;
      }
    }
  }
  // return field;
};

/**
 * list sorting
 *********
 * @param {Array} list
 * @param {String} sortField
 * @return {Array} sorted list
 */
export const sortMetaData = function (list, sortField) {
  list.sort(function (a, b) {
    return a[sortField] > b[sortField] ? 1 : b[sortField] > a[sortField] ? -1 : 0;
  });
};

/**
 * 범위 값 정보를 파싱
 * 상세 폼, 그리드의 값의 범위를 제한하기 위한 필드
 * 예) 숫자형의 경우 : 0,100 or min:0 or max:100
 * 예) 날짜형의 경우 : 2016-01-01,today or min:today or max:today + 1
 *********
 * @param {Object} field
 * @param {String} rangeVal
 * @param {String} type
 */
export const parseSearchValueRange = function (field, rangeVal, type) {
  if (!rangeVal || rangeVal.length < 1) return;
  if (type != 'number' && type != 'date') return;

  let rangeType =
    rangeVal.indexOf(',') > 0 ? 'min-max' : rangeVal.indexOf(',') == 0 ? 'max' : 'min';
  if (rangeVal.indexOf('min') == 0) rangeType = 'min';
  if (rangeVal.indexOf('max') == 0) rangeType = 'max';

  if (rangeVal.indexOf(',') == 0) {
    rangeVal = rangeVal.substr(1).trim();
  }

  let min: any = null,
    max: any = null;
  if (rangeType == 'min-max') {
    let rangeValArr = rangeVal.split(',');
    min = rangeValArr[0].trim();
    max = rangeValArr[1].trim();
  } else if (rangeType == 'min') {
    min = rangeVal.trim();
  } else if (rangeType == 'max') {
    max = rangeVal.trim();
  }

  if (min) min = min.indexOf('min') >= 0 ? min.substring(4).trim() : min;
  if (max) max = max.indexOf('max') >= 0 ? max.substring(4).trim() : max;

  // 날짜형인 경우
  if (type == 'date') {
    min = calcDate(min);
    max = calcDate(max);
  }

  field.userData = field.userData ? field.userData : {};
  if (min) field.userData.min = min;
  if (max) field.userData.max = max;
};

/**
 * Datetime 범위 값 정보를 파싱
 * 검색 폼 날짜 컴포넌트의 범위를 제한하기 위한 설정 값 파싱
 * ex1) 2016-01-01,today -> 2016-01-01 00:00:00 ~ 오늘 날짜 0시 0분 0초
 * ex2) currentTime - 3,currentTime -> 현재 시간 3시간 전 ~ 현재 시간
 * ex3) today - 3, currentTime -> 어제 0시 0분 0초 ~ 현재 시간
 * ex4) today - 1 -> 어제 0시 0분 0초 ~
 * ex5) ,today -> 									 ~ 오늘 0시 0분 0초
 * ex6) ,today -> 									 ~ 현재 시간까지
 *********
 * @param {Object} field
 * @param {String} initVal
 */
export const parseDatetimeInitValue = function (field, initVal) {
  if (!initVal || initVal.length < 1) return;

  let rangeType = initVal.indexOf(',') > 0 ? 'from-to' : initVal.indexOf(',') == 0 ? 'to' : 'from';
  if (initVal.indexOf('min') == 0) rangeType = 'from';
  if (initVal.indexOf('max') == 0) rangeType = 'to';
  if (initVal.indexOf(',') == 0) {
    initVal = initVal.substr(1).trim();
  }

  let from: any = null,
    to: any = null;
  if (rangeType == 'from-to') {
    let initValArr = initVal.split(',');
    from = initValArr[0].trim();
    to = initValArr[1].trim();
  } else if (rangeType == 'from') {
    from = initVal.trim();
  } else if (rangeType == 'to') {
    to = initVal.trim();
  }

  if (from) from = from.indexOf('min') >= 0 ? from.substr(4).trim() : from;
  if (to) to = to.indexOf('max') >= 0 ? to.substr(4).trim() : to;

  if (from) from = clacDateTime(from);
  if (to) to = clacDateTime(to);

  field.userData = field.userData ? field.userData : {};
  if (from) field.userData.from = from;
  if (to) field.userData.to = to;
};

/**
 * 문자형 데이터를 객체를 파싱하여 리턴한다.
 *********
 * @params {String} dateStr
 */
export const calcDate = function (dateStr) {
  if (!dateStr) return null;

  // 1. dateStr이 today가 들어가 있다면
  if (dateStr.indexOf('today') >= 0) {
    var today = new Date();

    // 년, 월, 일을 가져오기
    var year = today.getFullYear(); // 연도(네 자리)
    var month = today.getMonth() + 1; // 월 (0부터 시작하므로 1을 더해줌)
    var day = today.getDate(); // 일

    // 월이나 일이 한 자리 숫자인 경우 앞에 0을 붙여 두 자리로 만들기
    if (month < 10) {
      month = '0' + month;
    }
    if (day < 10) {
      day = '0' + day;
    }

    // 오늘 날짜를 yyyy-mm-dd 형식의 문자열로 표시
    var formattedDate = year + '-' + month + '-' + day;
    return formattedDate;
    // 2. dateStr이 2016-10-21 처럼 날짜형이라면 ...
  } else {
    return new Date(dateStr);
  }
};

/**
 * 문자형 데이터를 객체를 파싱하여 Date 객체로 리턴한다.
 *********
 * @params {String} datetimeStr
 */
export const clacDateTime = function (datetimeStr) {
  if (datetimeStr.indexOf('today') >= 0) {
    datetimeStr =
      datetimeStr.replace('today', 'let dt = new Date(); dt.setDate(dt.getDate()') + ')';
    let date = new Date(() => {
      let dt = new Date();
      dt.setDate(dt.getDate());
      return dt;
    });
    date.setHours(0);
    date.setMinutes(0);
    date.setSeconds(0);
    return date;
  } else if (datetimeStr.indexOf('currentTime') >= 0) {
    if (datetimeStr.indexOf('-') >= 0) {
      let minusHour = Number(datetimeStr.split('-')[1].trim());
      let date = new Date();
      date.setHours(date.getHours() - minusHour);
      date.setSeconds(0);
      return date;
    } else {
      return new Date();
    }
  } else {
    return formatToDate(datetimeStr);
  }
};
