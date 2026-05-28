import dayjs from 'dayjs';

export async function getQueryFilters(fields: any, schemas: any) {
  const filterCols: {
    name: string;
    operator: string;
    value: any;
    relation: boolean;
  }[] = [];

  for (const field in fields) {
    const schema = schemas.find((schema) => schema.field === field);
    if (!schema) continue;

    const value = fields[field];
    if (value === undefined || value === null || value === '') continue;
    if (Array.isArray(value) && value.length === 0) continue;

    const { component } = schema;

    if (component === 'RadioGroup') {
      if (value === 'all') continue;

      filterCols.push({
        name: field,
        operator: schema.operator,
        value: value === 'yes' ? true : false,
        relation: false,
      });
    } else {
      filterCols.push({
        name: field,
        operator: schema.operator,
        value,
        relation: false,
      });
    }
  }

  return filterCols;
}

/**
 * getFormattedFilters
 * ─────────────────────────────────────────────────────────────
 * 원본 searchProps / fields 를 절대 mutate 하지 않는다.
 * 복사본을 만들어서 변환 후 getQueryFilters 에 전달한다.
 */
export const getFormattedFilters = async (fields: any, searchProps: any[]) => {
  // ★ 복사 — 원본 searchProps 가 오염되면 keep-alive 재진입 시 깨진다
  const clonedFields = { ...fields };
  const clonedProps = searchProps.map((p) => ({ ...p }));

  for (const key in clonedFields) {
    const raw = clonedFields[key];
    if (raw === undefined || raw === null || raw === '') continue;

    const prop = clonedProps.find((p) => p.field === key);
    if (!prop) continue;

    // 1) RangePicker: [start,end] -> "start,end" + between
    if (prop.component === 'RangePicker' && Array.isArray(raw)) {
      const start = dayjs(raw[0]).format('YYYY-MM-DD') + ' 00:00:00';
      const end = dayjs(raw[1]).format('YYYY-MM-DD') + ' 23:59:59';
      clonedFields[key] = `${start},${end}`;
      prop.operator = 'between';
      continue;
    }

    // 2) DatePicker: date -> "start,end" + between
    if (prop.component === 'DatePicker') {
      const dateStr = dayjs(raw).format('YYYY-MM-DD');
      const start = dateStr + ' 00:00:00';
      const end = dateStr + ' 23:59:59';
      clonedFields[key] = `${start},${end}`;
      prop.operator = 'between';
      continue;
    }

    // 3) in: 배열이면 "a,b,c" 로 변환
    if (prop.operator === 'in') {
      if (Array.isArray(raw)) {
        clonedFields[key] = raw.join(',');
      } else {
        clonedFields[key] = String(raw);
      }
      continue;
    }
  }

  return await getQueryFilters(clonedFields, clonedProps);
};
