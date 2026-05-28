import * as logisUtil from "@/utils/logisticUtil";
import { useMessage } from "@/hooks/web/useMessage";
import { $mqtt } from "@/hooks/mqtt/setupMqtt";
import { useI18n } from "@/hooks/web/useI18n";
import { nextTick, onMounted, onUnmounted } from "vue";
import { isFunction } from "/@/utils/is";
const { createMessage } = useMessage();
const { t } = useI18n();

export function useMqttSubscriber(refreshByWs: Fn) {
  let equipCd = logisUtil.getEquipCd();
  let equipType = logisUtil.getEquipType();
  let stageCd = logisUtil.getStageCd();
  let stationCd = logisUtil.getStationCd();
  let deviceType = logisUtil.getDeviceType();
  let brokerAddr = logisUtil.getBrokerAddress();
  let brokerPort = logisUtil.getBrokerPort();
  let brokerSiteCd = logisUtil.getBrokerSiteCd();
  let msgJobType = undefined;
  let rackCd = undefined;
  let action = undefined;
  let message = undefined;
  let command = undefined;

  let WebMqttInstance = $mqtt;
  let currentJobType: string;
  // let refreshByWs: Fn;

  const setCurrentJobType = (jobType: string) => {
    currentJobType = jobType;
  };

  // MQTT 연결
  WebMqttInstance.startConnectionMonitor(
    stageCd,
    equipType,
    equipCd,
    stationCd,
    deviceType,
    brokerAddr,
    brokerPort,
    brokerSiteCd
  );

  /**
   * mqtt데이터 수신 후 처리를 위한 event handler
   * @param event mqtt-message-received Event
   */
  const mqttReceiveEventHandler = (event: CustomEvent) => {
    // 실제 나타나지 않은 화면이라면 동작 없음
    if (typeof event.detail === "object") {
      const messageObj = event.detail;
      equipType = messageObj.equip_type;
      msgJobType = messageObj.job_type;
      rackCd = messageObj.rack_cd;
      action = messageObj.action;
      message = messageObj.message;
      command = messageObj.command;
    }
    messageHandler(equipType, msgJobType, rackCd, action, message);
  };

  // 화면이 loading되면 메시지 수신 이벤트 listen
  onMounted(() =>
    document.addEventListener("mqtt-message-received", mqttReceiveEventHandler)
  );

  // 화면에서 빠지만 loading되면 메시지 수신 이벤트 remove
  onUnmounted(() =>
    document.removeEventListener(
      "mqtt-message-received",
      mqttReceiveEventHandler
    )
  );

  /**
   * 메시지 수신 후 action에 따른 분기 처리
   * @param equipType 장비유형
   * @param jobType 작업유형
   * @param rackCd 랙코드/EquipCd
   * @param action WebMqtt.action
   * @param message mqtt payload string
   * @returns void
   */
  const messageHandler = (
    equipType: string,
    jobType: string,
    rackCd: string,
    action: string,
    message: string
  ) => {
    // 메시지 프로퍼티의 action 값에 따라 동작 (새로고침, 정보 메시지, 에러 메시지)
    switch (action) {
      case WebMqttInstance.ACTION.DEVICE_CMD:
        // 화면 리프레시. 이 믹스인을 사용하는 화면에서 구현
        // refreshByWs(message);
        if (!refreshByWs || !isFunction(refreshByWs)) return;
        refreshByWs(message);
        break;

      case WebMqttInstance.ACTION.REFRESH:
        // 화면 리프레시. 이 믹스인을 사용하는 화면에서 구현
        if (!refreshByWs || !isFunction(refreshByWs)) return;
        refreshByWs(message);
        break;

      case WebMqttInstance.ACTION.SHOW_INFO:
        // 기기에서 설정한 작업과 메시지의 작업이 일치하고, iron-pages에서 현재 표시되고 있는 화면인 경우
        if (jobType) {
          if (currentJobType === jobType) {
            createMessage.info(message);
          }
        } else {
          createMessage.info(message);
        }
        break;

      case WebMqttInstance.ACTION.SHOW_ERROR:
        if (jobType) {
          if (currentJobType === jobType) {
            logisUtil.showMessageWithSound("error", t("title.error"), message);
          }
        } else {
          logisUtil.showMessageWithSound("error", t("title.error"), message);
        }
        break;

      default: {
        console.log(
          equipType,
          jobType,
          rackCd,
          action,
          message,
          refreshByWs.toString()
        );
      }
    }
  };
  return {
    setCurrentJobType,
  };
}
