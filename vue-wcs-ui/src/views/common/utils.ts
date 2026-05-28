import * as XLSX from 'xlsx';
import dayjs from 'dayjs';

export async function getQueryFilters(fields: any, schemas: any) {
  const filterCols: {
    name: string;
    operator: string;
    value: string | boolean;
    relation: boolean;
  }[] = [];
  for (const field in fields) {
    const schema = schemas.find((schema) => schema.field === field);
    if (fields[field]) {
      const { component } = schema;
      if (component === 'RadioGroup') {
        if (fields[field] === 'all') {
          continue;
        }
        filterCols.push({
          name: field,
          operator: schema.operator,
          value: fields[field] === 'yes' ? true : false,
          relation: false,
        });
      } else {
        filterCols.push({
          name: field,
          operator: schema.operator,
          value: fields[field],
          relation: false,
        });
      }
    }
  }

  return new Promise((resolve) => resolve(filterCols));
}

export const hasKeyWithFormat = (
  obj: { [x: string]: string | number | Date | dayjs.Dayjs | null | undefined },
  substring: string,
  searchProps: any,
) => {
  let betWeenValue: any;
  let inValue: any;

  // 날짜 포맷 변경
  for (let key in obj) {
    if (
      key.includes(substring) ||
      key.includes('created_at') ||
      key.includes('updated_at') ||
      key.includes('date') ||
      key.includes('time')
    ) {
      if (Array.isArray(obj[key])) {
        // 해당 키의 값이 배열인 경우 각 요소를 변환하여 새로운 배열을 생성
        obj[key] = obj[key].map(
          (item: string | number | Date | dayjs.Dayjs | null | undefined, index) => {
            if (index == 0) {
              return dayjs(item).format('YYYY-MM-DD');
            } else {
              return dayjs(item)
                .hour(23)
                .minute(59)
                .second(59)
                .millisecond(999)
                .format('YYYY-MM-DD HH:mm:ss.SSS');
            }
          },
        );
      } else {
        // 해당 키의 값이 배열이 아닌 경우 바로 날짜 형식으로 변환
        if (obj[key]) obj[key] = dayjs(obj[key]).format('YYYY-MM-DD');
      }
    }
  }

  //between 데이터 포맷 변경
  searchProps.forEach((item: { component: string; field: any; operator?: string }) => {
    if (item.component === 'RangePicker') {
      // RangePicker 컴포넌트인 경우
      if (Array.isArray(obj[item.field])) {
        // 해당 필드의 값이 배열인 경우
        betWeenValue = obj[item.field].join(',');
        obj[item.field] = betWeenValue;
      }
    }
    if (item.operator === 'in') {
      // operator가 'in'인 경우
      if (Array.isArray(obj[item.field])) {
        // 해당 필드의 값이 배열인 경우
        inValue = obj[item.field].join(',');
      } else {
        // 해당 필드의 값이 배열이 아닌 경우
        inValue = obj[item.field];
      }
      obj[item.field] = inValue;
    }
  });
};

// 숫자 변환 헬퍼 함수
export function sanitizeAndParseNumber(value: any): number {
  if (typeof value === 'string') {
    const cleanedValue = value.replace(/,/g, '').trim(); // 쉼표 제거 및 공백 트림
    const parsedValue = parseFloat(cleanedValue);
    return isNaN(parsedValue) ? 0 : parsedValue; // 변환 실패 시 0 반환
  }
  return typeof value === 'number' ? value : 0; // 숫자면 그대로 반환, 아니면 0
}

/**
 * 날짜 값을 ISO 8601 문자열로 변환하는 함수
 */
export function formatToIsoDate(value: any): string {
  if (typeof value === 'number') {
    // Excel 날짜 코드인 경우 처리
    const date = XLSX.SSF.parse_date_code(value);

    // 월과 일을 두 자리로 패딩 (예: 9 -> 09, 1 -> 01)
    const year = date.y.toString();
    const month = date.m.toString().padStart(2, '0');
    const day = date.d.toString().padStart(2, '0');

    // 최종 결과: "yyyy-MM-dd" 형식 문자열
    return `${year}-${month}-${day}`;
  } else if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    // 이미 'yyyy-MM-dd' 형식의 문자열인 경우 그대로 반환
    return value;
  }
  return '';
}

/**
 * 문자열 Y/N -> bool 형식으로 변환하는 함수
 */
export function formatFromYnToBoolean(value: any): boolean {
  if (value === 'Y') {
    return true;
  } else {
    return false;
  }
}
