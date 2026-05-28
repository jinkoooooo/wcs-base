// 커스텀렌더러 - Password 토글
// - 아이콘 토클 선택에 따라 값 표시 변경 (실제 값 ↔ 마스킹)
import Iconify from '@purge-icons/generated';

export default class PsswordButtonRenderer {
  private div: HTMLDivElement;
  private valueSpan: HTMLSpanElement;
  private iconSpan: HTMLSpanElement;

  // private button: HTMLButtonElement;
  private value = '';
  private shown = false;
  private iconFn?: (shown: boolean) => string;

  public constructor(props: any) {
    const { grid, rowKey, columnInfo, value } = props;
    const { options } = columnInfo.renderer || {};
    const { icon } = options || {};

    this.value = String(value ?? '');
    this.iconFn = options?.icon;

    // 컨테이너
    this.div = document.createElement('div');
    this.div.style.display = 'flex';
    this.div.style.alignItems = 'center';
    this.div.style.justifyContent = 'space-between';
    this.div.style.gap = '4px';
    this.div.style.width = '100%';
    this.div.style.padding = '0 2px';

    // 값 영역
    this.valueSpan = document.createElement('span');
    this.valueSpan.style.flex = '1';

    // 아이콘 영역
    this.iconSpan = document.createElement('span');
    this.iconSpan.className = 'app-iconify anticon';
    this.iconSpan.style.display = 'inline-flex';
    this.iconSpan.style.alignItems = 'center';
    this.iconSpan.style.cursor = 'pointer';

    // 아이콘 클릭 시 토글
    this.iconSpan.addEventListener('click', () => {
      this.shown = !this.shown;
      this.render(); // 값, 아이콘 갱신
    });

    this.div.appendChild(this.valueSpan);
    this.div.appendChild(this.iconSpan);

    this.render(props);
  }

  public getElement() {
    return this.div;
  }

  public getValue() {
    return this.value;
  }

  public updateIcon() {
    // 기존 아이콘 제거
    this.iconSpan.innerHTML = '';

    // 새 아이콘 생성
    const iconName =
      typeof this.iconFn === 'function'
        ? this.iconFn(this.shown)
        : this.shown
        ? 'ant-design:eye-invisible-outlined'
        : 'ant-design:eye-outlined';

    const svg = Iconify.renderSVG(iconName, {});
    if (svg) {
      this.iconSpan.appendChild(svg);
    } else {
      // fallback : data-icon 방식
      const childSpan = document.createElement('span');
      childSpan.className = 'iconify';
      childSpan.dataset.icon = iconName;
      this.iconSpan.appendChild(childSpan);
    }
  }

  public render(props?: any) {
    if (props) {
      this.value = String(props.value ?? '');
    }

    // 값 마스킹 설정/해제
    this.valueSpan.textContent = this.shown ? this.value : '********';
    // 아이콘 갱신
    this.updateIcon();
  }
}
