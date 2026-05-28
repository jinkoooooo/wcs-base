import { useI18n } from '/@/hooks/web/useI18n';
import { getCommonCodeByName, getEntityCodeByName } from '/@/api/common/api';

const { t } = useI18n();

export const parseSelectColumns = function (metaDataList) {
  var gridColumnMetaList = metaDataList.filter(function (metaData) {
    if (metaData.name == 'id' && !metaData.grid_rank) metaData.grid_rank = -10;
    return metaData.grid_rank && metaData.grid_rank != 0;
  });

  var selectColumns = '';

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
  var gridColumnMetaList = metaDataList.filter(function (metaData) {
    if (metaData.name == 'id' && !metaData.grid_rank) metaData.grid_rank = -10;
    return metaData.grid_rank && metaData.grid_rank != 0;
  });

  sortMetaData(gridColumnMetaList, 'grid_rank');

  return await parseToastGrid(gridColumnMetaList);
  // return parseForDataludiGrid(gridColumnMetaList);
};

const parseToastGrid = async function (gridColumnMetaList) {
  let toastCols = await Promise.all(
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
      if (gridEditor === 'text') {
        field.editor = 'text';
      } else if (gridEditor === 'code-combo') {
        const options = await getCommonCodeByName(metaData.ref_name);
        options.unshift({ text: ' ', value: ' ' });
        const listItems = { listItems: options };
        const editor = { type: 'select', options: listItems };
        field.formatter = 'listItemText';
        field.editor = editor;
      } else if (gridEditor === 'resource-code') {
        const options = await getEntityCodeByName(metaData.ref_name);
        options.unshift({ text: ' ', value: ' ' });
        const listItems = { listItems: options };
        const editor = { type: 'select', options: listItems };
        field.formatter = 'listItemText';
        field.editor = editor;
      } else if (gridEditor === 'date-picker') {
        const editor = {
          type: 'datePicker',
          options: { format: 'yyyy/MM/dd' },
        };
        field.editor = editor;
      } else if (gridEditor === 'readonly') {
        // readOnly가 모두 매핑되는 값이 있는 것은 아니다.        
        // 참조 이름이 있는지 확인
        if (metaData.ref_name !== undefined && metaData.ref_name?.trim() !== "") {
          const options = await getCommonCodeByName(metaData.ref_name);
          field.meta = options; // 공통코드 변환 값을 추가한다.
          const optionDict = new Map(options.map(option => [String(option.value), option.text]));
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

const parseForDataludiGrid = function (gridColumnMetaList) {
  var gColumns = gridColumnMetaList.map(function (metaData) {
    var field: any = {
      name: metaData.name,
      fieldName: metaData.name,
      width: metaData.grid_width ? metaData.grid_width : 100,
      header: {
        text: t(metaData.term),
      },
      styles: {} as any,
      validations: [],
    };

    // id 필드는 기본으로 grid_width를 0으로 하고 숨기지만 표시한다고 설정이 되어 있으면 설정한다. 어쨋든 그리드 상에는 존재해야 한다.
    if (metaData.name == 'id') {
      field.width = metaData.grid_rank > 0 ? metaData.grid_width : 0;
    }
    var editor = metaData.grid_editor;
    // align 정보가 있으면 설정
    if (metaData.grid_align) field.styles.textAlignment = metaData.grid_align;

    if (editor) {
      if ('code-combo' == editor || 'code-column' == editor || 'resource-code' == editor) {
        field.editor = {
          type: 'list',
          domainOnly: true,
          editable: editor != 'code-column',
        };
        field.lookupDisplay = true;
        field.editable = editor != 'code-column';
        if (!field.editable) {
          field.styleCallback = function (index, styles) {
            styles.setBackground('#edebeb');
          };
        }
        field.userData = {
          resourceType: 'code',
          resourceName: metaData.ref_name,
        };
        if ('resource-code' == editor) {
          field.userData.resourceType = 'entity';
        }

        if (metaData.code_list && metaData.code_list.length > 0) {
          field.lookupValues = metaData.code_list.map(function (code) {
            return code.name;
          });
          field.lookupLabels = metaData.code_list.map(function (code) {
            return code.description;
          });
          field.lookupValues.unshift('');
          field.lookupLabels.unshift('');
        } else {
          field.lookupValues = [];
          field.lookupLabels = [];
        }
      } else if (editor == 'checkbox') {
        field.editable = false;
        field.renderer = {
          type: 'check',
          editable: true,
          threeStates: false,
          trueValues: 'true',
          falseValues: 'false',
        };
      } else if (editor == 'rank') {
        field.editor = {
          type: 'number',
          minValue: 0,
        };
      } else if (editor == 'positive-number') {
        field.editor = {
          type: 'number',
          minValue: 0,
        };
        field.styles.numberFormat = metaData.grid_format ? metaData.grid_format : '0,000';
      } else if (editor == 'negative-number') {
        field.editor = {
          type: 'number',
          maxValue: 0,
        };
        field.styles.numberFormat = metaData.grid_format ? metaData.grid_format : '0,000';
      } else if (editor == 'number') {
        field.editor = {
          type: 'number',
        };
        field.styles.numberFormat = metaData.grid_format ? metaData.grid_format : '0,000';
        var minMax = parseGridValueRange(metaData.range_val, 'number');

        if (minMax && minMax.length == 2) {
          if (minMax[0]) field.editor.minValue = Number(minMax[0]);
          if (minMax[1]) field.editor.maxValue = Number(minMax[1]);
        }
      } else if (editor.indexOf('resource-column') >= 0) {
        newResourceColumn(metaData, field);
        field.editable = false;
        field.styleCallback = function (index, styles) {
          styles.setBackground('#edebeb');
        };
      } else if (editor.indexOf('resource-selector') >= 0) {
        var originalField = field.name;
        newResourceColumn(metaData, field);
        field.button = 'action';
        field.buttonVisibility = 'always';
        field.editable = false;
        field.styles.iconIndex = 'resource-selector';
        field.userData.originalField = originalField;
        field.userData.resourceType = metaData.ref_type;
        field.userData.resourceName = metaData.ref_name;
        field.userData.initialParams = metaData.ref_params;
        field.userData.ownerField = field.name;
        field.userData.bindFields = metaData.ref_related;
      } else if (editor.indexOf('popup-editor') >= 0) {
        field.styles.iconIndex = 'popup-editor';
        field.customButtons = [
          {
            visibility: 'always',
            imageUrl: 'images/grid/popup_up.png',
            hoverUrl: 'images/grid/popup_up.png',
            downUrl: 'images/grid/popup_down.png',
          },
        ];

        field.userData = {
          editorMode: metaData.grid_format,
        };
      } else if (editor.indexOf('resource-format-selector') >= 0) {
        field.editable = false;
        field.styles.iconIndex = 'resource-format-selector';
        field.userData = {
          resourceType: metaData.ref_type,
          resourceName: metaData.ref_name,
          initialParams: metaData.ref_params,
          ownerField: field.name,
          bindFields: metaData.ref_related,
          valueField: metaData.grid_format,
        };
      } else if (editor == 'date') {
        field.styles = {
          datetimeFormat: 'yyyy-MM-dd',
        };
        field.editor = {
          type: 'mask',
          mask: '0000-00-00',
          datetimeFormat: 'yyyy-MM-dd',
        };
      } else if (editor == 'time') {
        field.styles = {
          datetimeFormat: 'hh:mm:ss',
        };
        field.editor = {
          type: 'mask',
          mask: '00:00:00',
          datetimeFormat: 'hh:mm:ss',
        };
      } else if (editor == 'datetime') {
        field.styles = {
          datetimeFormat: 'yyyy-MM-dd hh:mm:ss',
        };
        field.editor = {
          type: 'mask',
          mask: '0000-00-00 00:00:00',
          datetimeFormat: 'yyyy-MM-dd hh:mm:ss',
          includeLiterals: true,
        };
      } else if (editor == 'ranged-date-picker') {
        field.editable = false;
        field.styles.iconIndex = 'date-picker';
        field.styles.datetimeFormat = metaData.grid_format ? metaData.grid_format : 'yyyy-MM-dd';
        field.styles.textAlignment = 'center';
        var minMax = parseGridValueRange(metaData.range_val, 'date');

        if (minMax && minMax.length == 2) {
          field.userData = field.userData ? field.userData : {};
          if (minMax[0]) field.userData.minDate = minMax[0];
          if (minMax[1]) field.userData.maxDate = minMax[1];
        }
      } else if (editor == 'date-picker') {
        field.editable = false;
        field.styles.iconIndex = 'date-picker';
        field.styles.datetimeFormat = metaData.grid_format ? metaData.grid_format : 'yyyy-MM-dd';
        field.styles.textAlignment = 'center';
      } else if (editor.indexOf('image-selector') >= 0) {
        field.editable = false;
        field.styles.iconIndex = 'image-selector';
      } else if (editor.indexOf('file-selector') >= 0) {
        field.editable = false;
        field.styles.iconIndex = 'file-selector';
        newResourceColumn(metaData, field);
        field.userData.defaultStorage = metaData.grid_format;
        field.userData.titleField = 'name';
      }
      // else if (editor.indexOf('image-renderer') >= 0) {
      //   field.editable = false;
      //   field.styles.iconIndex = 'image-renderer';
      //   field.type = 'calced';
      //   field.renderer = {
      //     type: 'image',
      //     imageDisplay: 'auto'
      //   };

      //   var baseUrl = me.globals.baseUrl;
      //   field.valueCallback = function(column, row) {
      //     var imgId = row.getValue(field.name);
      //     return imgId ? baseUrl + '/download/' + imgId : '';
      //   };

      // } else if (editor == 'download-link') {
      //   field.renderer = {
      //     type: 'link',
      //     requiredFields: 'id',
      //     showUrl: true,
      //     url: (metaData.grid_format ? metaData.grid_format : '') + "/${id}"
      //   }

      // }
      else if (editor == 'textarea') {
        field.editor = {
          type: 'multiline',
          maxLength: metaData.col_size ? metaData.col_size : 255,
        };
      } else if (editor == 'hidden') {
        field.width = 0;
      } else if (editor == 'readonly') {
        field.editable = false;
        field.styleCallback = function (index, styles) {
          styles.setBackground('#edebeb');
        };
        if (metaData.grid_format) {
          if (
            metaData.col_type == 'integer' ||
            metaData.col_type == 'int' ||
            metaData.col_type == 'long' ||
            metaData.col_type == 'double' ||
            metaData.col_type == 'float'
          ) {
            field.styles.numberFormat = metaData.grid_format;
          }
        }
      } else if (editor == 'password') {
        field.displayCallback = function (index, value) {
          var hiddenString = '';

          if (value) {
            for (var i = 0; i < value.length; i++) {
              hiddenString += '*';
            }
          }

          return hiddenString;
        };
      } else if (editor == 'page-link') {
        field.editable = false;
        field.styles.iconIndex = 'page-link';
        field.userData = field.userData || {};
        field.userData.pageLink = metaData.def_val;
      } else {
        if (metaData.col_size && metaData.col_size > 0 && metaData.col_type == 'string') {
          field.editor = {
            maxLength: metaData.col_size,
          };
        }
      }
    }

    if (metaData.name != 'id') {
      field.nullable = typeof metaData.nullable == 'undefined' ? false : metaData.nullable;
    }

    if (metaData.col_size && metaData.col_size > 0 && metaData.col_type == 'string') {
      field.maxValueLength = metaData.col_size;
    }

    if (typeof metaData.ext_field != 'undefined' && metaData.ext_field == true) {
      field.userData = field.userData || {};
      field.userData.extField = metaData.ext_field;
    }

    return field;
  });
  return gColumns;
};
/**
 * Resource Column 구성
 *
 * @param {Object} metaData 서버로 부터 조회한 서버측 메타 데이터
 * @param {Object} field 그리드 필드 데이터
 */
export const newResourceColumn = function (metaData, field) {
  var titleField = metaData.grid_format ? metaData.grid_format : 'name';
  field.userData = {
    titleField: titleField,
  };
  field.name = field.name.substr(0, field.name.indexOf('_id'));
  field.fieldName = field.name;

  // 1. Object 형식의 데이터를 화면에 표시하기 위한 콜백 함수
  field.displayCallback = function (index, value) {
    var rowObj = index.getRow().getRowObject();
    var fieldName = index.column.dataFieldName();
    var userData = index.column.userData();
    var columnObj = rowObj[fieldName];
    var retVal = columnObj ? columnObj[userData.titleField] : '';

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

  var rangeType =
    rangeVal.indexOf(',') > 0
      ? 'min-max'
      : rangeVal.indexOf('min') >= 0
      ? 'min'
      : rangeVal.indexOf('max') >= 0
      ? 'max'
      : 'none';
  if (rangeType == 'none') return null;
  var min: any = null,
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
    var time = eval(dateStr);
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
