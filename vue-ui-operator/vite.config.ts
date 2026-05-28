import legacy from "@vitejs/plugin-legacy";
import vue from "@vitejs/plugin-vue";
import path from "path";
import { readPackageJSON } from "pkg-types";
import { defineConfig, loadEnv, mergeConfig, type UserConfig } from "vite";
import { createProxy } from "./src/utils/proxy";
import Components from "unplugin-vue-components/vite";
import { AntDesignVueResolver } from "unplugin-vue-components/resolvers";
import vueJsx from "@vitejs/plugin-vue-jsx";
// @ts-ignore: type unless
import DefineOptions from "unplugin-vue-define-options/vite";
import { createAppConfigPlugin } from "./src/createAppConfigPlugin";
import { type PluginOption } from "vite";
// const root = process.cwd();
// // const isBuild = command === 'build';
// const env = loadEnv("development", root);
// const {
//   VITE_USE_MOCK,
//   VITE_BUILD_COMPRESS,
//   VITE_ENABLE_ANALYZE,
//   VITE_PORT,
//   VITE_PROXY,
// } = env;

// https://vitejs.dev/config/
export default defineConfig(async ({ command, mode }) => {
  const root = process.cwd();
  const env = loadEnv(mode, root);
  const {
    VITE_USE_MOCK,
    VITE_BUILD_COMPRESS,
    VITE_ENABLE_ANALYZE,
    VITE_PORT,
    VITE_PROXY,
  } = env;
  const defineData = await createDefineData(root);
  const vitePlugins: (PluginOption | PluginOption[])[] = [
    vue(),
    vueJsx(),
    DefineOptions(),
    legacy({
      renderLegacyChunks: false,
    }),
    Components({
      dts: "types/components.d.ts",
      resolvers: [
        AntDesignVueResolver({
          importStyle: false, // css in js
        }),
      ],
    }),
  ];

  const isBuild = command === "build";
  const appConfigPlugin = await createAppConfigPlugin({ root, isBuild });
  vitePlugins.push(appConfigPlugin);

  const userConfig: UserConfig = {
    define: defineData,
    plugins: vitePlugins,
    optimizeDeps: {
      // 👈 optimizedeps
      esbuildOptions: {
        target: "esnext",
        // Node.js global to browser globalThis
        define: {
          global: "globalThis",
        },
        supported: {
          bigint: true,
        },
      },
    },
    build: {
      target: ["esnext"], // 👈 build.target
    },
    resolve: {
      alias: [
        {
          find: /\/@\//,
          replacement: path.resolve(__dirname, "./src") + "/",
        },
        // /#/xxxx => types/xxxx
        {
          find: /\/#\//,
          replacement: path.resolve(__dirname, "./types/") + "/",
        },
        // @/xxxx => src/xxxx
        {
          find: /@\//,
          replacement: path.resolve(__dirname, "./src/") + "/",
        },
        // #/xxxx => types/xxxx
        {
          find: /#\//,
          replacement: path.resolve(__dirname, "./types/") + "/",
        },
      ],
    },
    server: {
      host: true,
      port: Number(VITE_PORT),
      proxy: createProxy(JSON.parse(VITE_PROXY)),
    },
    esbuild: {
      supported: {
        "top-level-await": true, //browsers can handle top-level-await features
      },
    },
  };
  return userConfig;
});

async function createDefineData(root: string) {
  try {
    const pkgJson = await readPackageJSON(root);
    const { dependencies, devDependencies, name, version } = pkgJson;

    const __APP_INFO__ = {
      pkg: { dependencies, devDependencies, name, version },
      lastBuildTime: new Date().toISOString(),
    };
    return {
      __APP_INFO__: JSON.stringify(__APP_INFO__),
    };
  } catch (error) {
    return {};
  }
}
