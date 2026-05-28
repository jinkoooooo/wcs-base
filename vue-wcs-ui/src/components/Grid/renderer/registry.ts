import BooleanRenderer from './BooleanRenderer';
import IconRenderer from './IconRenderer';
import PasswordRenderer from './PasswordButtonRenderer'

const RENDERERS = {
  boolean: BooleanRenderer,
  icon: IconRenderer,
  password: PasswordRenderer,
};

export default function registerRenderer(columns) {
  columns.forEach((column) => {
    const type = column.renderer?.type;
    if (type && RENDERERS[type]) {
      column.renderer.type = RENDERERS[type];
    }
  });
}
