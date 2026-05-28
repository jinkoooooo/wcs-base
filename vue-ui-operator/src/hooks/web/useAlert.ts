//TODO: change this ionic

import { alertController as alert } from "@ionic/vue";
import type {
  AlertOptions,
  AlertButton,
  AlertInput,
} from "@ionic/core/components";

import { useI18n } from "@/hooks/web/useI18n";
export declare interface ConfirmOptions {
  title?: string;
  subTitle?: string;
  content?: string;
  onOk?: Function;
  onCancel?: Function;
  inputEls?: AlertInput[];
}

/**
 * @description input을 넣을 수 있는 confirm 기능 제공
 * @param options
 * @param onOk ok 클릭 시 처리해야는 기능
 * @param onCancel cancel 클릭 시 처리해야 하는 기능
 * @param inputEls AlertInput의 Array처리
 * @returns
 */
const createConfirm = async function (options: ConfirmOptions) {
  /**
   * 다국어 처리
   */
  const { t } = useI18n();
  let confirmBotton: AlertButton = {
    text: t("button.confirm"),
    role: "confirm",
    cssClass: "danger",
    handler: () => {
      if (options.onOk) {
        options.onOk();
      }
    },
  };

  let cancelBotton: AlertButton = {
    text: t("button.cancel"),
    role: "confirm",
    cssClass: "primary",
    handler: () => {
      if (options.onCancel) {
        options.onCancel();
      }
    },
  };

  let buttons = [];
  if (options.onOk) buttons.push(confirmBotton);
  if (options.onCancel) buttons.push(cancelBotton);

  const alertInstance = await alert.create({
    header: options.title ? options.title : "",
    subHeader: options.subTitle ? options.subTitle : "",
    message: options.content ? options.content : "",
    buttons,
  });

  await alertInstance.present();

  return alertInstance as HTMLIonAlertElement;
};

/**
 * @description: message
 */
export function useAlert() {
  return {
    createConfirm,
  };
}
