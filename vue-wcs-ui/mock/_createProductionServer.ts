import { createProdMockServer } from 'vite-plugin-mock/es/createProdMockServer';

// 问题描述
// 1. `import.meta.globEager` 已被弃用, 需要升级vite版本,有兼容问题
// 2. `vite-plugin-mock` 插件问题 https://github.com/vbenjs/vite-plugin-mock/issues/56

// const modules: Record<string, any> = import.meta.glob("./**/*.ts", {
//   import: "default",
//   eager: true,
// });

// const mockModules = Object.keys(modules).reduce((pre, key) => {
//   if (!key.includes("/_")) {
//     pre.push(...modules[key]);
//   }
//   return pre;
// }, [] as any[]);

// glob은 mock에서 지원하지 못하고 있으나 운영환경에서 사용하지 않음으로 globEager -> glob으로 변경
// const modules = import.meta.globEager('./**/*.ts');
const modules = import.meta.glob('./**/*.ts', { eager: true });

const mockModules: any[] = [];
Object.keys(modules).forEach((key) => {
  if (key.includes('/_')) {
    return;
  }
  mockModules.push(...modules[key].default);
});

/**
 * Used in a production environment. Need to manually import all modules
 */
export function setupProdMockServer() {
  createProdMockServer(mockModules);
}
