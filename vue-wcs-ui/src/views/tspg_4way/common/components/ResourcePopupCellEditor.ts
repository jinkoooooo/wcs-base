// vue-wcs-ui/src/views/tspg_4way/common/ResourcePopupCellEditor.ts
import { createApp, h, defineAsyncComponent } from 'vue';
import { SearchOutlined } from '@ant-design/icons-vue';

const popupModules = import.meta.glob('/src/views/**/*Popup.vue');

function resolvePopupComponent(nameOrPath: string): any | null {
  if (!nameOrPath) return null;
  const keys = Object.keys(popupModules);
  let matched: string | undefined;

  if (nameOrPath.includes('/')) {
    const normalized = nameOrPath.startsWith('/') ? nameOrPath : `/${nameOrPath}`;
    const withExt = normalized.endsWith('.vue') ? normalized : `${normalized}.vue`;
    const fullPath = `/src/views${withExt}`;
    matched = keys.find((k) => k === fullPath);
  } else {
    matched = keys.find((k) => k.endsWith(`/${nameOrPath}.vue`));
  }
  if (!matched) {
    console.warn(`[ResourcePopupCellEditor] popup not found: ${nameOrPath}`);
    return null;
  }
  return defineAsyncComponent(popupModules[matched] as any);
}

/**
 * "item_code:item_code, item_name:item_name" 형식 파싱
 * → Map { 팝업결과키 → 그리드컬럼명 }
 */
function parseBindFields(bindStr: string): Map<string, string> {
  const map = new Map<string, string>();
  if (!bindStr) return map;
  bindStr.split(',').forEach((pair) => {
    const [src, dst] = pair.split(':').map((s) => s.trim());
    if (src && dst) map.set(src, dst);
  });
  return map;
}

export default class ResourcePopupCellEditor {
  private el: HTMLElement;
  private selectedValue: any = '';
  private popupApp: any = null;
  private popupContainer: HTMLElement | null = null;
  private grid: any;
  private rowKey: number | string;
  private columnName: string;
  private bindFields: Map<string, string>;
  private popupComponentName: string;
  private initialValue: any;

  constructor(props: any) {
    const { grid, rowKey, columnInfo, value } = props;
    const options = columnInfo.editor?.options || {};

    this.grid = grid;
    this.rowKey = rowKey;
    this.columnName = columnInfo.name;
    this.initialValue = value ?? '';
    this.selectedValue = this.initialValue;
    this.popupComponentName = options.popupComponent || '';
    this.bindFields = parseBindFields(options.bindFields || '');

    // ─────────────────────────────────────────
    // 셀 UI: 값 표시 + Ant Design SearchOutlined 아이콘
    // ─────────────────────────────────────────
    const wrapper = document.createElement('div');
    wrapper.style.cssText =
      'display:flex;align-items:center;width:100%;height:100%;box-sizing:border-box;';

    const input = document.createElement('input');
    input.type = 'text';
    input.readOnly = true;
    input.value = String(this.initialValue ?? '');
    input.style.cssText =
      'flex:1;border:none;outline:none;padding:0 6px;background:#fffbe6;' +
      'cursor:pointer;height:100%;font-size:inherit;';

    // ★ Ant Design SearchOutlined 와 동일한 SVG 아이콘 사용
    //    (@ant-design/icons 의 outlined 검색 아이콘 SVG path)
    const iconBtn = document.createElement('span');
    iconBtn.style.cssText =
      'display:flex;align-items:center;justify-content:center;' +
      'padding:0 8px;cursor:pointer;height:100`%;color:#1890ff;';

// Ant Design SearchOutlined 컴포넌트 직접 마운트
    createApp(h(SearchOutlined)).mount(iconBtn);

    wrapper.appendChild(input);
    wrapper.appendChild(iconBtn);
    this.el = wrapper;

    // 래퍼 어디를 눌러도 팝업 오픈
    const openHandler = (e: Event) => {
      e.stopPropagation();
      this.openPopup();
    };
    wrapper.addEventListener('mousedown', openHandler);
    wrapper.addEventListener('click', openHandler);
  }

  getElement(): HTMLElement {
    return this.el;
  }

  getValue(): any {
    return this.selectedValue;
  }

  mounted(): void {
    setTimeout(() => this.openPopup(), 0);
  }

  beforeDestroy(): void {
    this.destroyPopup();
  }

  // ─────────────────────────────────────────
  // 팝업 오픈
  // ─────────────────────────────────────────
  private openPopup(): void {
    if (this.popupApp) return;

    const PopupComp = resolvePopupComponent(this.popupComponentName);
    if (!PopupComp) {
      alert(`팝업을 찾을 수 없습니다: ${this.popupComponentName}`);
      this.finishEditingCell();
      return;
    }

    this.popupContainer = document.createElement('div');
    document.body.appendChild(this.popupContainer);

    let popupApi: any = null;

    this.popupApp = createApp({
      render: () =>
        h(PopupComp, {
          onReady: (api: any) => {
            popupApi = api;
            api?.openModal?.();
          },
          onSelect: (payload: any) => {
            this.handleSelect(payload);
            popupApi?.closeModal?.();
          },
          onClose: () => {
            this.finishEditingCell();
          },
          'onUpdate:open': (v: boolean) => {
            if (!v) this.finishEditingCell();
          },
        }),
    });
    this.popupApp.mount(this.popupContainer);
  }

  // ─────────────────────────────────────────
  // ★★★ 핵심: 선택값을 현재 셀 + 동명 컬럼 전부에 자동 채움
  // ─────────────────────────────────────────
  private handleSelect(payload: any): void {
    if (payload == null) {
      this.finishEditingCell();
      return;
    }

    const mainKey = this.columnName;

    // (a) 현재 셀(메인) 값 결정
    const mainValue =
      typeof payload === 'object'
        ? payload[mainKey] ?? payload.code ?? payload.value ?? ''
        : payload;
    this.selectedValue = mainValue;

    // (b) 자동 매칭: payload 의 키 중 그리드 컬럼명과 동명인 것을 모두 채움
    //     ref_related 가 지정된 경우 → 그 매핑을 우선 적용하고, 나머지는 자동 매칭
    if (typeof payload === 'object') {
      try {
        const gridColumns: any[] = this.grid?.getColumns?.() ?? [];
        const gridColNames = new Set<string>(gridColumns.map((c: any) => c.name));

        // 명시적 매핑 먼저 처리 (ref_related)
        const handledDstCols = new Set<string>();
        if (this.bindFields.size > 0) {
          this.bindFields.forEach((dstCol, srcKey) => {
            if (dstCol === mainKey) return;
            if (!gridColNames.has(dstCol)) return;
            const v = payload[srcKey];
            if (v !== undefined) {
              this.grid.setValue(this.rowKey, dstCol, v);
              handledDstCols.add(dstCol);
            }
          });
        }

        // 자동 매칭 — payload 키 == 그리드 컬럼명 && 아직 처리되지 않음
        Object.keys(payload).forEach((key) => {
          if (key === mainKey) return;
          if (!gridColNames.has(key)) return;
          if (handledDstCols.has(key)) return;
          const v = payload[key];
          if (v === undefined || v === null) return;
          this.grid.setValue(this.rowKey, key, v);
        });
      } catch (err) {
        console.warn('[ResourcePopupCellEditor] auto-bind failed:', err);
      }
    }

    this.finishEditingCell();
  }

  private finishEditingCell(): void {
    this.destroyPopup();
    setTimeout(
      () => this.grid.finishEditing?.(this.rowKey, this.columnName, this.selectedValue),
      0,
    );
  }

  private destroyPopup(): void {
    if (this.popupApp) {
      try { this.popupApp.unmount(); } catch (_) { /* noop */ }
      this.popupApp = null;
    }
    if (this.popupContainer) {
      this.popupContainer.remove();
      this.popupContainer = null;
    }
  }
}
