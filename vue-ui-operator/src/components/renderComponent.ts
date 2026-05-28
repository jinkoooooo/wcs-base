import { render } from "vue";

export default function renderComponent({
  container,
  component: vnode,
  props,
  appContext,
}) {
  vnode.appContext = { ...appContext }; // must spread new object here
  render(vnode, container);
  return {
    destroy: () => {
      // destroy component
      render(null, container);
      vnode = undefined;
    },
    registerd: vnode?.component?.exposed,
  };
}
