import { reactive, computed } from 'vue';
import type { CellOp } from '../../api/generatorApi';

export type CellState =
  | 'active'
  | 'disabled'
  | 'pending-disable'
  | 'pending-enable'
  | 'pending-create';
export type CellOrigin = 'persisted-active' | 'persisted-disabled' | 'new';

export interface CellDraft {
  cellId: string;
  level: number;
  row: number;
  bay: number;
  rackType: number;
  state: CellState;
  origin: CellOrigin;
}

export function useRackCellDraft() {
  const cells = reactive<Record<string, CellDraft>>({});

  function hydrate(initial: CellDraft[]) {
    Object.keys(cells).forEach((k) => delete cells[k]);
    for (const c of initial) cells[c.cellId] = c;
  }

  function toggle(cellId: string) {
    const c = cells[cellId];
    if (!c) return;
    switch (c.state) {
      case 'active':
        c.state = 'pending-disable';
        break;
      case 'disabled':
        c.state = 'pending-enable';
        break;
      case 'pending-disable':
        c.state = 'active';
        break;
      case 'pending-enable':
        c.state = 'disabled';
        break;
      case 'pending-create':
        delete cells[cellId];
        break;
    }
  }

  function toggleBatch(cellIds: string[], mode: 'auto' | 'disable' | 'enable' = 'auto') {
    let resolvedMode: 'disable' | 'enable';
    if (mode === 'auto') {
      const hasActive = cellIds.some((id) => cells[id]?.state === 'active');
      resolvedMode = hasActive ? 'disable' : 'enable';
    } else {
      resolvedMode = mode;
    }
    for (const id of cellIds) {
      const c = cells[id];
      if (!c) continue;
      if (resolvedMode === 'disable') {
        if (c.state === 'active') c.state = 'pending-disable';
        else if (c.state === 'pending-enable') c.state = 'disabled';
      } else {
        if (c.state === 'disabled') c.state = 'pending-enable';
        else if (c.state === 'pending-disable') c.state = 'active';
      }
    }
  }

  function addCreate(
    cellId: string,
    level: number,
    row: number,
    bay: number,
    rackType: number,
  ) {
    if (cells[cellId]) return;
    cells[cellId] = {
      cellId,
      level,
      row,
      bay,
      rackType,
      state: 'pending-create',
      origin: 'new',
    };
  }

  function reset() {
    for (const k of Object.keys(cells)) {
      const c = cells[k];
      if (c.origin === 'new') delete cells[k];
      else if (c.origin === 'persisted-active') c.state = 'active';
      else c.state = 'disabled';
    }
  }

  const dirty = computed(() =>
    Object.values(cells).some((c) => c.state.startsWith('pending-')),
  );

  function dirtyOps(): CellOp[] {
    const ops: CellOp[] = [];
    for (const c of Object.values(cells)) {
      if (c.state === 'pending-disable') {
        ops.push({ cellId: c.cellId, level: c.level, row: c.row, bay: c.bay, kind: 'DISABLE' });
      } else if (c.state === 'pending-enable') {
        ops.push({ cellId: c.cellId, level: c.level, row: c.row, bay: c.bay, kind: 'ENABLE' });
      } else if (c.state === 'pending-create') {
        ops.push({
          cellId: c.cellId,
          level: c.level,
          row: c.row,
          bay: c.bay,
          kind: 'CREATE',
          rackType: c.rackType,
        });
      }
    }
    return ops;
  }

  function commit(rejected: string[]) {
    for (const c of Object.values(cells)) {
      if (rejected.includes(c.cellId)) continue;
      if (c.state === 'pending-disable') {
        c.state = 'disabled';
        c.origin = 'persisted-disabled';
      } else if (c.state === 'pending-enable') {
        c.state = 'active';
        c.origin = 'persisted-active';
      } else if (c.state === 'pending-create') {
        c.state = 'active';
        c.origin = 'persisted-active';
      }
    }
  }

  return {
    cells,
    hydrate,
    toggle,
    toggleBatch,
    addCreate,
    reset,
    dirty,
    dirtyOps,
    commit,
  };
}
