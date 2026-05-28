// 파렛트/박스/셔틀/진행/lifecycle 서버 fetch 와 상태 보관.

import { ref } from 'vue';
import { palletApi } from '../api';
import type { LifecycleEntry } from '../shared';

export function useBoxData() {
  const info = ref<any>(null);
  const boxes = ref<any[]>([]);
  const act = ref<any>(null);
  const inboundProgress = ref<any>(null);
  const lifecycle = ref<LifecycleEntry[]>([]);

  // 초기 load — 모든 데이터를 병렬로 가져옴.
  // 실패 시 throw → 호출자가 catch + setMsg.
  async function load(code: string) {
    info.value = null;
    boxes.value = [];
    act.value = null;
    inboundProgress.value = null;
    lifecycle.value = [];

    const labelP = palletApi.label(code);
    const boxP = palletApi.boxes(code).catch(() => [] as any[]);
    const actP = palletApi.active(code).catch(() => null);
    const progP = palletApi.progress(code).catch(() => null);
    const lifeP = palletApi.lifecycle(code).catch(() => [] as any[]);

    const [label, boxList, active, prog, lifeList] = await Promise.all([
      labelP,
      boxP,
      actP,
      progP,
      lifeP,
    ]);
    info.value = label;
    boxes.value = Array.isArray(boxList) ? boxList : [];
    act.value = active;
    if (!active) inboundProgress.value = prog;
    lifecycle.value = Array.isArray(lifeList) ? (lifeList as LifecycleEntry[]) : [];
  }

  // 부분 갱신 — load 와 동일하나 info 는 fail-safe (null 응답이면 유지), 실패해도 throw 하지 않음.
  async function softReload(code: string) {
    const labelP = palletApi.label(code).catch(() => null);
    const boxP = palletApi.boxes(code).catch(() => [] as any[]);
    const actP = palletApi.active(code).catch(() => null);
    const progP = palletApi.progress(code).catch(() => null);
    const lifeP = palletApi.lifecycle(code).catch(() => [] as any[]);

    try {
      const [label, boxList, active, prog, lifeList] = await Promise.all([
        labelP,
        boxP,
        actP,
        progP,
        lifeP,
      ]);
      if (label) info.value = label;
      if (Array.isArray(boxList)) boxes.value = boxList;
      act.value = active;
      inboundProgress.value = active ? null : prog;
      if (Array.isArray(lifeList)) lifecycle.value = lifeList as LifecycleEntry[];
    } catch (_) {
      /* swallow */
    }
  }

  async function refreshBoxesOnly(code: string) {
    try {
      const refreshed: any = await palletApi.boxes(code);
      if (Array.isArray(refreshed)) boxes.value = refreshed;
    } catch (_) {
      /* swallow */
    }
  }

  function resetData() {
    info.value = null;
    boxes.value = [];
    act.value = null;
    inboundProgress.value = null;
    lifecycle.value = [];
  }

  return {
    info,
    boxes,
    act,
    inboundProgress,
    lifecycle,
    load,
    softReload,
    refreshBoxesOnly,
    resetData,
  };
}
