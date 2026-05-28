import type {
  VNode,
  VNodeChild,
  ComponentPublicInstance,
  FunctionalComponent,
  PropType as VuePropType,
} from "vue";

import { IonSpinner } from "@ionic/vue";

declare global {
  interface Window {
    circleSpinner: typeof IonSpinner;
  }

  type Column = {
    fieldname: string;
    columnWidth?: string;
    hidden?: boolean;
    style?: Record<string, Object>;
    displayCallback?: (record: any, rowData?: any) => string;
    image?: Record<string, Object>;
    button?: string | ((record: any, rowData?: any) => string);
    display?: string;
    checkBox?: string;
    classCallback?: (record: any) => string;
    buttonCallback?: string | ((record: any, button: any) => string);
  };

  interface ImageElement extends HTMLImageElement {
    getValue?: () => {};
  }
  interface InputElement extends HTMLInputElement {
    getValue?: () => {};
  }

  interface ButtonElement extends HTMLButtonElement {
    getValue?: () => {};
  }
  interface SpanElement extends HTMLSpanElement {
    value?: string;
    getValue?: () => {};
  }

  interface DivElement extends HTMLDivElement {
    getData?: () => {};
  }

  interface ListElement extends HTMLLIElement {
    getData?: () => {};
  }

  const __APP_INFO__: {
    pkg: {
      name: string;
      version: string;
      dependencies: Recordable<string>;
      devDependencies: Recordable<string>;
    };
    lastBuildTime: string;
  };
  // declare interface Window {
  //   // Global vue app instance
  //   __APP__: App<Element>;
  // }

  // vue
  type PropType<T> = VuePropType<T>;
  type VueNode = VNodeChild | JSX.Element;

  export type Writable<T> = {
    -readonly [P in keyof T]: T[P];
  };

  type Nullable<T> = T | null;
  // type NonNullable<T> = T extends null | undefined ? never : T;
  type Recordable<T = any> = Record<string, T>;
  type ReadonlyRecordable<T = any> = {
    readonly [key: string]: T;
  };
  type Indexable<T = any> = {
    [key: string]: T;
  };
  type DeepPartial<T> = {
    [P in keyof T]?: DeepPartial<T[P]>;
  };
  type TimeoutHandle = ReturnType<typeof setTimeout>;
  type IntervalHandle = ReturnType<typeof setInterval>;

  interface ChangeEvent extends Event {
    target: HTMLInputElement;
  }

  interface WheelEvent {
    path?: EventTarget[];
  }
  interface ImportMetaEnv extends ViteEnv {
    __: unknown;
  }

  interface ViteEnv {
    VITE_USE_MOCK: boolean;
    VITE_PUBLIC_PATH: string;
    VITE_GLOB_APP_TITLE: string;
    VITE_BUILD_COMPRESS: "gzip" | "brotli" | "none";
  }

  function parseInt(s: string | number, radix?: number): number;

  function parseFloat(string: string | number): number;
}

declare module "vue" {
  export type JSXComponent<Props = any> =
    | { new (): ComponentPublicInstance<Props> }
    | FunctionalComponent<Props>;
}
