import { useRmkStore } from './RmkStore';
import { storeToRefs } from 'pinia';

const rmkStore = useRmkStore();
const { data } = storeToRefs(rmkStore);
const { fetchData } = rmkStore;
fetchData();

/**
 * Grid Row의 코드 값을 공통코드 명칭으로 변환 (코드 → 명칭)
 * - 그리드 데이터 조회 후 gridFetched에서 호출
 * - row의 필드명이 공통코드 name(소문자)과 일치하면 dictionary에서 명칭을 찾아 치환
 *
 * 예: row.order_status = "90" → row.order_status = "완료"
 *     (ORDER_STATUS 공통코드에 { "90": "완료" }가 등록되어 있을 때)
 *
 * @param row TUI Grid row 데이터 객체
 */
export const decodeRmk = function (row: any) {
  if (!data.value || !Array.isArray(data.value)) return;

  data.value.forEach((rmk: any) => {
    const key = rmk.name?.toLowerCase();
    if (key && row.hasOwnProperty(key)) {
      const value = row[key];
      if (rmk.dictionary && rmk.dictionary.hasOwnProperty(value)) {
        row[key] = rmk.dictionary[value];
      }
    }
  });
};

/**
 * Grid Row의 명칭을 코드 값으로 역변환 (명칭 → 코드)
 * - 그리드 저장 전에 호출하여 명칭을 원래 코드로 복원
 *
 * 예: row.order_status = "완료" → row.order_status = "90"
 *
 * @param row TUI Grid row 데이터 객체
 */
export const incodeRmk = function (row: any) {
  if (!data.value || !Array.isArray(data.value)) return;

  data.value.forEach((rmk: any) => {
    const key = rmk.name?.toLowerCase();
    if (key && row.hasOwnProperty(key)) {
      const currentValue = row[key];
      if (rmk.dictionary) {
        // dictionary를 뒤집어서 value → key 매핑
        const reverseDic = Object.entries(rmk.dictionary).reduce(
          (acc: Record<string, string>, [k, v]: [string, any]) => {
            acc[String(v)] = k;
            return acc;
          },
          {},
        );
        if (reverseDic.hasOwnProperty(currentValue)) {
          row[key] = reverseDic[currentValue];
        }
      }
    }
  });
};
