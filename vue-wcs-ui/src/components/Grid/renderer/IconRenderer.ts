import Iconify from '@purge-icons/generated';

export default class IconRenderer {
  private element;

  public constructor(props) {
    const { grid, columnInfo, rowKey } = props;
    const { options } = columnInfo.renderer;
    const { icon } = options;
    const span = document.createElement('span');
    span.className = 'app-iconify anticon';
    let record = grid.getRow(rowKey);
    const svg = Iconify.renderSVG(icon(record), {});
    if (svg) {
      span.textContent = '';
      span.appendChild(svg);
    } else {
      const childSpan = document.createElement('span');
      childSpan.className = 'iconify';
      childSpan.dataset.icon = icon(record);
      childSpan.textContent = '';
      span.appendChild(childSpan);
    }

    this.render(span);
  }

  public getElement() {
    return this.element;
  }

  public getValue() {
    return this.element.src;
  }

  render(element) {
    this.element = element;
  }
}
