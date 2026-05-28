export default class BooleanRenderer {
  private div;
  private input;

  public constructor(props) {
    const { grid, rowKey, columnInfo } = props;
    const div = document.createElement('div');
    const input = document.createElement('input');
    div.append(input);
    div.className = 'tui-grid-cell-boolean';
    input.type = 'checkbox';
    input.onchange = (e) => {
      grid['setValue'](rowKey, columnInfo.name, e.target?.checked);
    };
    this.div = div;
    this.input = input;
    this.render(props);
  }

  public getElement() {
    return this.div;
  }

  public getValue() {
    return this.input.checked;
  }

  public render(props) {
    this.input.checked = props.value;
  }
}
