// vue-wcs-ui/src/views/tspg_4way/common/ResourcePopupCellRenderer.ts
import { createApp, h } from 'vue';
import { SearchOutlined } from '@ant-design/icons-vue';

/**
 * 편집 모드가 아닐 때 셀에 값 + 검색 아이콘 표시.
 * 아이콘은 Ant Design SearchOutlined 컴포넌트 사용 (상단 검색폼과 통일).
 */
export default class ResourcePopupCellRenderer {
  private el: HTMLElement;

  constructor(props: any) {
    const el = document.createElement('div');
    el.style.cssText =
      'display:flex;align-items:center;width:100%;height:100%;' +
      'padding:0 6px;box-sizing:border-box;';

    const text = document.createElement('span');
    text.style.cssText =
      'flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';
    text.textContent = String(props.value ?? '');

    const icon = document.createElement('span');
    icon.style.cssText =
      'display:flex;align-items:center;margin-left:6px;color:#bfbfbf;font-size:12px;';
    createApp(h(SearchOutlined)).mount(icon);

    el.appendChild(text);
    el.appendChild(icon);
    this.el = el;
  }

  getElement(): HTMLElement {
    return this.el;
  }

  render(props: any): void {
    const text = this.el.firstChild as HTMLElement;
    if (text) text.textContent = String(props.value ?? '');
  }
}
