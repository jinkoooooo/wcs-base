// snake_case <-> camelCase key 변환 유틸

const toCamel = (s: string) => s.replace(/_([a-z0-9])/g, (_, c) => String(c).toUpperCase());
const toSnake = (s: string) =>
  s
    .replace(/([A-Z])/g, '_$1')
    .replace(/__/g, '_')
    .toLowerCase();

export function keysToCamel<T = any>(input: any): T {
  if (Array.isArray(input)) {
    return input.map((v) => keysToCamel(v)) as any;
  }
  if (input && typeof input === 'object' && !(input instanceof Date)) {
    const out: any = {};
    Object.keys(input).forEach((k) => {
      out[toCamel(k)] = keysToCamel(input[k]);
    });
    return out;
  }
  return input as T;
}

export function keysToSnake<T = any>(input: any): T {
  console.log(`keysToSnake before convert : ${JSON.stringify(input,null,2)}`);
  if (Array.isArray(input)) {
    return input.map((v) => keysToSnake(v)) as any;
  }
  if (input && typeof input === 'object' && !(input instanceof Date)) {
    const out: any = {};
    Object.keys(input).forEach((k) => {
      out[toSnake(k)] = keysToSnake(input[k]);
    });
    console.log(`keysToSnake after convert : ${JSON.stringify(out,null,2)}`);
    return out;
  }
  return input as T;
}
