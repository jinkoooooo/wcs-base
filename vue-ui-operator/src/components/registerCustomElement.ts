import { defineCustomElement, VueElement } from "vue";
import { kebabCase } from "lodash-es";
import CommonDialog from "@/components/CommonDialog.vue";
import ItemList from "@/components/ItemList.vue";
import FormListPopup from "@/components/FormListPopup.vue";

/**
 * @description global custom element로 등록할 component를 일괄 등록, 본 파일에 사전 등록되어 있어야 함
 * @param components import된 component list
 */
export const registerGlobComp = () => {
  const components = [CommonDialog, FormListPopup, ItemList];
  registerComponents(components);
};

/**
 * @description components Array
 * @example [CommonDialog, FormListPopup, ItemList]
 * @param components import된 component list
 */
export const registerComponents = (components: any[]) => {
  components.forEach(async (component) => {
    registerComponent(component);
  });
};

/**
 * @description component
 * @example ItemList
 * @param component import된 component
 */
export const registerComponent = (component) => {
  try {
    const ComponentEl = defineCustomElement(component);
    customElements.define(kebabCase(component.__name), ComponentEl);
  } catch (error) {
    console.log("element not need to register component");
  } finally {
  }
};
