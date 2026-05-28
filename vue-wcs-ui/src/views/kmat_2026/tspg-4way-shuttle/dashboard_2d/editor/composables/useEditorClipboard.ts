import { nextTick, ref, type ComputedRef, type Ref } from 'vue';
import type { TbEcs2dItem, TbEcs2dPage } from '../../api/types';
import { buildUUID } from '/@/utils/uuid';
import { clamp } from '../utils/editorGeom';

type ClipboardItem = { relX: number; relY: number; src: TbEcs2dItem };

export function useEditorClipboard(args: {
  page: ComputedRef<TbEcs2dPage | undefined>;
  objects: ComputedRef<TbEcs2dItem[]>;
  selectedIds: Ref<string[]>;
  clear: () => void;
  setMany: (ids: string[], opt: { keepExisting: boolean }) => void;
  emitAdd: (obj: TbEcs2dItem) => void;
  emitSelect: (id: string | null) => void;
  canvasWidth: ComputedRef<number>;
  canvasHeight: ComputedRef<number>;
}) {
  const clipboard = ref<{
    originPageId: string;
    originX: number;
    originY: number;
    items: ClipboardItem[];
  } | null>(null);
  const pasteCount = ref(0);

  const copySelection = () => {
    const ids = [...args.selectedIds.value];
    if (ids.length === 0) return;

    const selected = ids
      .map((id) => args.objects.value.find((o) => o.id === id))
      .filter(Boolean) as TbEcs2dItem[];

    if (selected.length === 0) return;

    const minX = Math.min(...selected.map((o) => o.posX));
    const minY = Math.min(...selected.map((o) => o.posY));

    clipboard.value = {
      originPageId: args.page.value?.id || '',
      originX: minX,
      originY: minY,
      items: selected.map((o) => ({
        relX: o.posX - minX,
        relY: o.posY - minY,
        src: { ...o },
      })),
    };

    pasteCount.value = 0;
  };

  const makeNewEquipmentCode = (typeCode: string, idx: number) => {
    const safeType = (typeCode || 'UNKNOWN').replace(/[^A-Za-z0-9_]/g, '').slice(0, 24);
    return `NEW_${safeType}_${Date.now()}_${idx + 1}`;
  };

  const pasteSelection = async () => {
    if (!clipboard.value) return;
    if (!args.page.value?.id) return;

    // 페이지 전환 시 오프셋 누적 리셋: 다른 페이지에 처음 붙여넣을 때는 원래 좌표 그대로.
    const samePage = clipboard.value.originPageId === (args.page.value?.id || '');
    if (!samePage) {
      pasteCount.value = 0;
    } else {
      pasteCount.value += 1;
    }
    const offset = samePage ? 20 * pasteCount.value : 0;

    const maxZ = args.objects.value.reduce((m, o) => Math.max(m, o.zIndex || 0), 0);

    const newIds: string[] = [];
    const baseX = clipboard.value.originX + offset;
    const baseY = clipboard.value.originY + offset;

    clipboard.value.items.forEach((item, idx) => {
      const src = item.src;

      const newId = buildUUID();
      const w = src.width || 100;
      const h = src.height || 100;

      const x = clamp(baseX + item.relX, 0, args.canvasWidth.value - w);
      const y = clamp(baseY + item.relY, 0, args.canvasHeight.value - h);

      const newObj: TbEcs2dItem = {
        ...src,
        id: newId,
        lcId: args.page.value?.lcId || '',
        pageId: args.page.value?.id || '',
        posX: x,
        posY: y,
        zIndex: maxZ + 1 + idx,

        // 붙여넣기는 새 설비 취급
        equipmentId: '',
        equipmentCode: makeNewEquipmentCode(src.equipmentTypeCode || '', idx),
      };

      args.emitAdd(newObj);
      newIds.push(newId);
    });

    await nextTick();
    requestAnimationFrame(() => {
      args.clear();
      args.setMany(newIds, { keepExisting: false });
      args.emitSelect(newIds[0] ?? null);
    });
  };

  return { copySelection, pasteSelection };
}
