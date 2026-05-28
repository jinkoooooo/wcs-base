import { toastController as toast } from "@ionic/vue";
import {
  informationCircleOutline as infoIcon,
  warningOutline as warningIcon,
  alertCircleOutline as errorIcon,
  checkmarkDoneCircle as successIcon,
} from "ionicons/icons";

import { useI18n } from "@/hooks/web/useI18n";
import type { I18nGlobalTranslation } from "@/hooks/web/useI18n";
export declare type ToastPosition = "top" | "bottom" | "middle" | undefined;

export declare interface ConfirmOptions {
  header?: string;
  subHeader?: string;
  message?: string;
}

export declare interface toastType {
  present?: string;
  subHeader?: string;
  message?: string;
}

/**
 * 다국어 처리
 */

const position: ToastPosition = "bottom";

class Message {
  private instance: any;
  private t: I18nGlobalTranslation;
  constructor() {
    this.instance = null;
    this.t = useI18n().t;
  }
  public newMessage = async (
    position: ToastPosition = "bottom",
    icon: string | undefined = undefined,
    cssClass: string | string[] = "",
    message: string,
    messageArgs?: Record<string, unknown>
  ) => {
    try {
      this.instance.dismiss();
    } catch (e) {}
    let msg = messageArgs ? this.t(message, messageArgs) : this.t(message);
    this.instance = await toast.create({
      position: position,
      icon,
      cssClass,
      positionAnchor: "footer",
      message: this.t(message),
      duration: 1500,
      // animated: true,
      layout: "stacked",
      // enterAnimation, //todo : animation 적용
    });

    await this.instance.present();
  };
  public error = async (
    message: string,
    messageArgs?: Record<string, unknown>
  ) => {
    await this.newMessage(position, errorIcon, "danger", message, messageArgs);
  };
  public info = async (
    message: string,
    messageArgs?: Record<string, unknown>
  ) => {
    await this.newMessage(position, infoIcon, "info", message, messageArgs);
  };
  public warning = async (
    message: string,
    messageArgs?: Record<string, unknown>
  ) => {
    await this.newMessage(
      position,
      warningIcon,
      "warning",
      message,
      messageArgs
    );
  };
  public success = async (
    message: string,
    messageArgs?: Record<string, unknown>
  ) => {
    await this.newMessage(
      position,
      successIcon,
      "success",
      message,
      messageArgs
    );
  };
}

/**
 * @description: message
 */
export function useMessage() {
  return {
    createMessage: new Message(),
  };
}
