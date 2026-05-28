import { reactive } from 'vue';

/**
 * boolean flag 객체를 실제 boolean 타입으로 넓혀주는 유틸 타입
 */
type BooleanFlagState<T extends Record<string, boolean>> = {
  [K in keyof T]: boolean;
};

/**
 * 비동기 로딩 플래그 공통 composable
 */
export function useAsyncFlags<T extends Record<string, boolean>>(initialState: T) {
  const flags = reactive({ ...initialState }) as BooleanFlagState<T>;

  function setFlag<K extends keyof BooleanFlagState<T>>(key: K, value: boolean) {
    flags[key] = value;
  }

  function start<K extends keyof BooleanFlagState<T>>(key: K) {
    setFlag(key, true);
  }

  function stop<K extends keyof BooleanFlagState<T>>(key: K) {
    setFlag(key, false);
  }

  function reset() {
    Object.keys(initialState).forEach((key) => {
      (flags as Record<string, boolean>)[key] = initialState[key];
    });
  }

  return {
    flags,
    setFlag,
    start,
    stop,
    reset,
  };
}
