import { createApp } from "vue";
import App from "./App.vue";

import { IonicVue } from "@ionic/vue";

/* Core CSS required for Ionic components to work properly */
import "@ionic/vue/css/core.css";

/* Basic CSS for apps built with Ionic */
import "@ionic/vue/css/normalize.css";
import "@ionic/vue/css/structure.css";
import "@ionic/vue/css/typography.css";

/* Optional CSS utils that can be commented out */
import "@ionic/vue/css/padding.css";
import "@ionic/vue/css/float-elements.css";
import "@ionic/vue/css/text-alignment.css";
import "@ionic/vue/css/text-transformation.css";
import "@ionic/vue/css/flex-utils.css";
import "@ionic/vue/css/display.css";

/* Theme variables */
import "ant-design-vue/dist/reset.css";
import "./theme/variables.css";
import "./theme/custom.css";
import "@ionic/core/css/core.css";
import "@ionic/core/css/ionic.bundle.css";
import "./theme/tailwind.css";

/* setups */
import { setupStore } from "@/store";
import { setupI18n } from "@/locales/setupI18n";

import { initAppConfigStore } from "@/initAppConfig";
import { router, setupRouter } from "@/router";
import { setupRouterGuard } from "@/router/guard";
import { defineCustomElements } from "@ionic/pwa-elements/loader";
import { setupMqtt } from "@/hooks/mqtt/setupMqtt";
import { registerGlobComp } from "@/components/registerCustomElement";
import { setupVBarcodeFocus } from "./directives/vBarcodeFocus";

async function bootstrap() {
  defineCustomElements(window);
  registerGlobComp();
  const app = createApp(App);

  // Configure store
  // 설정 store
  setupStore(app);
  initAppConfigStore();
  app.use(IonicVue);

  setupRouter(app);
  setupRouterGuard(router);
  await setupI18n(app);
  app.use(setupMqtt());
  setupVBarcodeFocus(app);
  router.isReady().then(() => {
    app.mount("#app");
  });
}
bootstrap();
