/**
 * Grid Row의 데이터 값을 WCS의 Rmk Code에 맞는 코드명칭으로 변경한다.
 *
 * @params Toast UI Grid Row
 */
export const decodeRmk = function (row, columnMetaData) {
  // readOnly이면서 공통코드가 있는 경우 공통코드의 key-value로 변환
  columnMetaData.forEach(col => {
    const key = col.name;
    const value = row[key];

    if(value !== undefined) {
      // col.meta를 배열로 변환
      const metaArray = col.meta || [];

      // metaArray가 배열인지 확인하고, find 로직 실행
      if (Array.isArray(metaArray)) {
        const matched = metaArray.find(convert => convert.value === value);


        if (matched) {
          row[key] = matched.text;
        }
      }
    }
  })
};
