/*******************************************************************************
 *  Source Name:    logis-util.js
 *  Description:    유틸리티
 *  Author:         박용환
 *  Update History:
 *                  2023. 12. 4  박용환 최초 작성
 *
 ******************************************************************************/

import { Component, createVNode } from "vue";

import errorSfx from "/audio/error.wav";
import infoSfx from "/audio/info.wav";
import warningSfx from "/audio/warning.wav";
import { AnyObject } from "ant-design-vue/es/_util/type";

/**
 * @description component
 * @example ItemList
 * @param component import된 component
 */
import { registerComponent } from "@/components/registerCustomElement";
import { useAlert } from "@/hooks/web/useAlert";
import { useI18n } from "@/hooks/web/useI18n";
import { useMessage } from "@/hooks/web/useMessage";
import { useSound } from "@vueuse/sound";

const { t } = useI18n();

const createConfirm = useAlert().createConfirm;
const toastMessage = useMessage().createMessage;

export const registerComp = registerComponent;
/**
 * @description key로 로컬 스토리지에서 뽑아냄
 *******************************
 * @param {String} key
 * @return 로컬 스토리지 값
 */
export const getLocalStorage = (key: string) => {
  let value = localStorage.getItem(key);
  if (value !== "undefined" && value) {
    return JSON.parse(value);
  } else {
    return "";
  }
};

/**
 * @description 로컬 스토리지에서 key, value로 저장
 *******************************
 * @param {String} key
 * @param {String} value
 */
export const setLocalStorage = (key: string, value: any) => {
  localStorage.setItem(key, JSON.stringify(value));
};

/*******************************************************************************
 *                        장비에서 설정한 내용을 조회
 ******************************************************************************/

/**
 * @description 디바이스 타입 조회
 *******************
 * @return 디바이스 타입
 */
export const getDeviceType = () => {
  return getLocalStorage("setting.deviceType");
};

/**
 * @description 현재 설정에서 선택한 설비 유형 리턴
 ********************
 * @return 설비 유형
 */
export const getEquipType = () => {
  return getLocalStorage("setting.equipType");
};

/**
 * @description 현재 설정에서 선택한 설비 코드를 리턴
 ********************
 * @return 설비 코드
 */
export const getEquipCd = () => {
  return getLocalStorage("setting.equipCd");
};

/**
 * @description 현재 설정에서 선택한 설비 이름을 리턴
 ********************
 * @return 설비 이름
 */
export const getEquipNm = () => {
  return getLocalStorage("setting.equipNm");
};

/**
 * @description 현재 설정에서 선택한 스테이지 코드를 리턴
 ********************
 * @return 스테이지 코드
 */
export const getStageCd = () => {
  return getLocalStorage("setting.stageCd");
};

/**
 * @description 현재 설정에서 선택한 작업 스테이션을 리턴
 ********************
 * @return 스테이션 코드
 */
export const getStationCd = () => {
  return getLocalStorage("setting.stationCd");
};

/**
 * @description 현재 설정에서 선택한 작업 사이드를 리턴
 ********************
 * @return 작업 사이드 코드
 */
export const getWorkSideCd = () => {
  return getLocalStorage("setting.workSideCd");
};

/**
 * @description 작업 유형 리턴
 ********************
 * @return 작업 유형
 */
export const getJobType = () => {
  return getLocalStorage("setting.jobType");
};

/**
 * @description 사용할 프린터 아이디 리턴
 ********************
 * @return 사용할 프린터 아이디
 */
export const getPrinterId = () => {
  return getLocalStorage("setting.printerId");
};

/*******************************************************************************
 *                        서버에서 장비에 대해서 설정한 내용을 조회
 ******************************************************************************/

/**
 * @description 디바이스 설정 리스트 조회
 *******************
 * @return 디바이스 설정 리스트
 */
export const getDeviceSettings = () => {
  return getLocalStorage("setting.deviceSettings");
};

/**
 * @description 디바이스 설정 리스트에서 설정 값 조회
 ********************
 * @param {String} key 설정 명
 */
export const getDeviceSettingValue = (key: string) => {
  let settings = getDeviceSettings();

  if (settings) {
    let setting = settings.find((setting: { name: any }) => {
      return setting.name === key;
    });
    return setting ? setting.value : null;
  } else {
    return null;
  }
};

/**
 * @description 디바이스 설정 리스트에서 Boolean 타입의 설정 값 조회
 ********************
 * @param {String} key 설정 명
 */
export const getBooleanDeviceSetting = (key: string) => {
  let value = getDeviceSettingValue(key);
  value = value ? value.toLowerCase() : "false";
  return value == "true" || value == "t" || value == "on" ? true : false;
};

/**
 * @description 디바이스 설정 리스트에서 Number 타입의 설정 값 조회
 ********************
 * @param {String} key 설정 명
 */
export const getNumberDeviceSetting = (key: string) => {
  let value = getDeviceSettingValue(key);
  return value && !isNaN(value) ? Number(value) : 0;
};

/**
 * @description 장비 소프트웨어 버전 (공통 설정)
 ********************
 * @return 작업 스테이션 내 대상 외 주문 정보 표시 여부
 */
export const getDeviceSwVersion = () => {
  return getDeviceSettingValue("software.version");
};

/**
 * @description 메시지 브로커의 사이트 코드 조회
 ********************
 * @return 메시지 브로커의 사이트 코드
 */
export const getBrokerSiteCd = () => {
  return getDeviceSettingValue("mw.rabbitmq.site.code");
};

/**
 * @description 메시지 브로커 주소 조회
 ********************
 * @return 메시지 브로커의 주소
 */
export const getBrokerAddress = () => {
  return getDeviceSettingValue("mw.rabbitmq.broker.address");
};

/**
 * @description 메시지 브로커 포트 조회
 ********************
 * @return 메시지 브로커 포트
 */
export const getBrokerPort = () => {
  return getDeviceSettingValue("mw.rabbitmq.web.mqtt.port");
};

/**
 * @description 상품 코드 최대 길이
 ********************
 * @return 상품 코드 최대 길이 리턴
 */
export const getMaxSkuCdLength = () => {
  return getNumberDeviceSetting("job.sku.barcode.max.length");
};

/**
 * @description 작업 스테이션 내 대상 외 주문 정보 표시 여부 리턴 (DPS 유형 설정)
 ********************
 * @return 작업 스테이션 내 대상 외 주문 정보 표시 여부
 */
export const isShowOthersOrder = () => {
  return getBooleanDeviceSetting("job.show.other.orders");
};

/**
 * @description 투입 박스 유형 (DPS 유형 설정) (box / tray)
 ********************
 * @return 투입 박스 유형
 */
export const getInputBoxType = () => {
  return getDeviceSettingValue("job.inputbox.type");
};

/**
 * @description 투입 박스 유형이 박스 유형인지 여부
 ********************
 * @return 박스 유형인 경우
 */
export const isBoxTypeBoxInput = () => {
  return getInputBoxType() == "box";
};

/**
 * @description 투입 박스 유형이 트레이 유형인지 여부
 ********************
 * @return 트레이 유형인 경우
 */
export const isTrayTypeBoxInput = () => {
  return getInputBoxType() == "tray";
};

/**
 * @description 연속 스캔 허용 여부 조회 (반품 설정)
 ********************
 * @return 연속 스캔 허용 여부
 */
export const isContinousScanAllowed = () => {
  return getBooleanDeviceSetting("scanner.continuous.scan.enabled");
};

/**
 * @description 화면에서 데이터 리프레쉬 주기
 ********************
 * @return 데이터 리프레쉬 주기
 */
export const getRefreshInterval = () => {
  let interval = getNumberDeviceSetting("screen.refresh.interval");
  return interval == 0 ? 30 * 1000 : interval * 1000;
};

/**
 * @description 태블릿에서 현재 주문을 처리하고 난 후 다음 주문에 대한 선택을 어떻게 할 지에 대한 모드 (auto: 자동 처리 모드, scan: 박스 ID를 스캔하여 다음 주문 처리, touch: 태블릿 터치하여 주문 처리)
 ********************
 * @return 다음 주문 선택 모드
 */
export const getNextOrderSelectionMode = () => {
  return getDeviceSettingValue("job.next.order.selection.mode");
};

/**
 * @description 현재 태블릿의 자동피킹 여부를 리턴 (DPS 유형 설정)
 ********************
 * @return 자동 피킹
 */
export const isAutoSelectionNextOrder = () => {
  return getNextOrderSelectionMode() === "auto";
};

/**
 * @description 현재 태블릿의 상품 코드를 전부 표시할 지 여부를 리턴 (상품 코드가 길기 때문에 전부 보여줄 지 / 일부만 보여줄 지 여부)
 ********************
 * @return 상품 코드를 전부 표시할 지 여부
 */
export const isShowFullSkuCode = () => {
  return !getBooleanDeviceSetting("display.sku.shorter.enabled");
};

/**
 * @description 현재 태블릿의 상품 코드를 일부 표시할 지 여부를 리턴 (상품 코드가 긴 경우 전부 보여줄 지 / 일부만 보여줄 지 여부)
 ********************
 * @return 상품 코드를 일부 표시할 지 여부
 */
export const isShowShortSkuCode = () => {
  return getBooleanDeviceSetting("display.sku.shorter.enabled");
};

/**
 * @description 표시할 상품 코드의 시작 인덱스를 리턴
 ********************
 * @return 표시할 상품 코드의 시작 인덱스
 */
export const getSkuCdStartIndex = () => {
  return getNumberDeviceSetting("display.sku.shorter.start.index");
};

/**
 * @description 상품 코드를 표시할 옵션에 따라 가공하여 리턴
 ********************
 * @param {String} skuCd
 * @return 표시할 상품 코드의 시작 인덱스
 */
export const getSkuCdForDisplay = (skuCd: string) => {
  if (!skuCd || !isShowShortSkuCode()) {
    return skuCd;
  } else {
    let startIdx = getSkuCdStartIndex();
    return skuCd.length > startIdx ? skuCd.substring(startIdx) : skuCd;
  }
};

/**
 * @description 박스 ID를 일부 표시할 지 여부를 리턴 (박스 ID가 긴 경우 전부 보여줄 지 / 일부만 보여줄 지 여부)
 ********************
 * @return 상품 코드를 일부 표시할 지 여부
 */
export const isShowShortBoxId = () => {
  return getBooleanDeviceSetting("display.box_id.shorter.enabled");
};

/**
 * @description 표시할 박스 ID 문자열의 시작 인덱스를 리턴
 ********************
 * @return 표시할 박스 ID 문자열의 시작 인덱스
 */
export const getBoxIdStartIndex = () => {
  return getNumberDeviceSetting("display.box_id.shorter.start.index");
};

/**
 * @description 박스 ID를 표시할 옵션에 따라 가공하여 리턴
 ********************
 * @param {String} boxId
 * @return 표시할 박스 ID의 시작 인덱스
 */
export const getBoxIdForDisplay = (boxId: string) => {
  if (!boxId || !isShowShortBoxId()) {
    return boxId;
  } else {
    let startIdx = getBoxIdStartIndex();
    return boxId.length > startIdx ? boxId.substring(startIdx) : boxId;
  }
};

/**
 * @description 현재 태블릿의 송장번호를 일부 표시할 지 여부를 리턴 (송장번호가 긴 경우 전부 보여줄 지 / 일부만 보여줄 지 여부)
 ********************
 * @return 송장번호를 일부 표시할 지 여부
 */
export const isShowShortInvoiceNo = () => {
  return getBooleanDeviceSetting("display.invoice.shorter.enabled");
};

/**
 * @description 표시할 송장 번호 문자열의 시작 인덱스를 리턴
 ********************
 * @return 표시할 송장 번호 문자열의 시작 인덱스
 */
export const getInvoiceNoStartIndex = () => {
  return getNumberDeviceSetting("display.invoice.shorter.start.index");
};

/**
 * @description 송장 번호를 표시할 옵션에 따라 가공하여 리턴
 ********************
 * @param {String} invoiceId
 * @return 표시할 송장 번호의 시작 인덱스
 */
export const getInvoiceIdForDisplay = (invoiceId: string) => {
  if (!invoiceId || !isShowShortInvoiceNo()) {
    return invoiceId;
  } else {
    let startIdx = getInvoiceNoStartIndex();
    return invoiceId.length > startIdx
      ? invoiceId.substring(startIdx)
      : invoiceId;
  }
};

/**
 * @description 셀 번호를 표시할 옵션에 따라 가공하여 리턴
 ********************
 * @param {String} cellCd
 * @return 표시할 셀 번호의 시작 인덱스
 */
export const getCellCdForDisplay = (cellCd: string) => {
  return cellCd.replace("-", "");
};

/**
 * @description 페이지네이션 처리를 위한 페이지 Limit
 ********************
 * @return 페이지네이션 처리를 위한 페이지 Limit
 */
export const getPageLimit = () => {
  let limit = getNumberDeviceSetting("pagination.page.limit");
  return limit > 0 ? limit : 50;
};

/**
 * @description 태블릿에서 블루투스 스캐너를 사용할 지 여부
 ********************
 * @return 태블릿에서 블루투스 스캐너를 사용할 지 여부
 */
export const isUseBloothScanner = () => {
  return getBooleanDeviceSetting("scanner.bluetooth.enabled");
};

/**
 * @description 작업 위치 (앞/뒤/전체) 선택 기능 활성화 여부
 ********************
 * @return 작업 위치 (앞/뒤/전체) 선택 기능 활성화 여부
 */
export const isWorkSideEnabled = () => {
  return getBooleanDeviceSetting("work.side.selection.enabled");
};

/**
 * @description 분할 Fullbox 기능 활성화 여부 (반품)
 ********************
 * @return 분할 Fullbox 기능 활성화 여부
 */
export const isSplitFullboxEnabled = () => {
  return getBooleanDeviceSetting("job.split.fullbox.enabled");
};

/**
 * @description 작업 화면에서 작업 옵션 선택 기능 활성화 여부
 ********************
 * @return 작업 화면에서 작업 옵션 선택 기능 활성화 여부
 */
export const isJobTransactionPopupEnabled = () => {
  return getBooleanDeviceSetting("job.transaction.enabled");
};

/**
 * @description 작업 화면에서 분류 처리 할 수 있는 처리 옵션 - [확정(P), 취소(C), 확정 취소(U), 수량 조절(S), Fullbox(F), Fullbox 취소(FC)
 ********************
 * @return 작업 화면에서 분류 처리 할 수 있는 처리 옵션
 */
export const getJobTransactionList = () => {
  let functions = getDeviceSettingValue("job.transaction.functions");
  return functions.split(",");
};

/**
 * @description DAS 반품 처리시 셀 - 박스 매핑을 디바이스에서 처리할 지 여부
 ********************
 * @param {String} jobType 작업 유형 (DAS, DPC, PDAS ....)
 * @param {String} inout in (반품) / out (출고)
 * @return DAS 반품 처리시 셀 - 박스 매핑을 디바이스에서 처리할 지 여부
 */
export const isCellBoxMappingEnabled = (jobType: string, inout: string) => {
  let configKey = "job." + jobType.toLowerCase() + ".";
  configKey += inout ? inout.toLowerCase() : "";
  configKey += "mapping.cell_box.enabled";
  return getBooleanDeviceSetting(configKey);
};

/**
 * @description 태블릿 피킹 화면에서 하단 투입 리스트의 방향을 설정하기 위한 작업 동선 방향 정보
 ********************
 * @return 작업 화면에서 분류 처리 할 수 있는 처리 옵션
 */
export const getWorkFlowDirection = () => {
  return getDeviceSettingValue("job.work.flow.direction");
};

/**
 * @description 테스트를 위해서 QR Code를 생성하기 위한 유형을 리턴
 ********************
 * @return 바코드 유형
 */
export const getQrGenerateSourceType = () => {
  return getDeviceSettingValue("qrcode.generate.source.type");
};

/**
 * @description 박스 당 최대 몇 개를 넣을 수 있는지 설정 리턴
 ********************
 * @return 개수 리턴
 */
export const getMaxPcsPerBox = () => {
  return getNumberDeviceSetting("max.pcs.per.box");
};

/**
 * @description 검수 & 피킹 모드 사용 여부
 ********************
 * @return 검수 & 피킹 모드 사용 여부
 */
export const isUsePickWithInspection = () => {
  return getBooleanDeviceSetting("pick.with.inspection.enabled");
};

/**
 * @description 수기 검수 시 검수가 완료되었다면 자동 완료 기능 사용 여부
 ********************
 * @return 자동 완료 기능 사용 여부
 */
export const isUseInspectionAutoFinish = () => {
  return getBooleanDeviceSetting("inspection.auto.finish.enabled");
};

/**
 * @description 박스 바코드 Validation을 화면에서 처리할 지 여부
 ********************
 * @return
 */
export const isBoxIdValidationEnabled = () => {
  return getBooleanDeviceSetting("validation.box_barcd.screen.enabled");
};

/**
 * @description 박스 바코드 Validation Rule
 ********************
 * @return
 */
export const getBoxIdValidationRule = () => {
  return getDeviceSettingValue("validation.box_barcd.rule");
};

/**
 * @deprecated 박스 ID 유효성 체크, isBoxIdValid로 변경
 ************************
 * @param {String} boxId
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isBoxIdValidate = (
  boxId: any,
  showInvalidMsg: any,
  invalidCallback: any
): boolean => {
  return isBoxIdValid(boxId, showInvalidMsg, invalidCallback);
};

/**
 * @description 박스 ID 유효성 체크
 ************************
 * @param {String} boxId
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isBoxIdValid = (
  boxId: string,
  showInvalidMsg?: boolean,
  invalidCallback?: () => void
): boolean => {
  if (!isBoxIdValidationEnabled()) {
    return true;
  } else {
    let rule = new RegExp(getBoxIdValidationRule());
    let isValid = rule.test(boxId);

    if (!isValid && showInvalidMsg && showInvalidMsg === true) {
      showMessageWithSound(
        "error",
        t("error.VALIDATION_ERROR"),
        t("error.box_id_invalid"),
        invalidCallback
      );
    }

    return isValid;
  }
};

/**
 * @description 셀 코드 Validation을 화면에서 처리할 지 여부
 ********************
 * @return
 */
export const isCellCdValidationEnabled = () => {
  return getBooleanDeviceSetting("validation.cell_cd.screen.enabled");
};

/**
 * @description 셀 코드 Validation Rule
 ********************
 * @return
 */
export const getCellCdValidationRule = () => {
  return getDeviceSettingValue("validation.cell_cd.rule");
};

/**
 * @description 셀 코드 유효성 체크
 ************************
 * @param {String} cellCd
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isCellCdValid = (
  cellCd: string,
  showInvalidMsg?: boolean,
  invalidCallback?: () => void
): boolean => {
  if (!isCellCdValidationEnabled()) {
    return true;
  } else {
    let rule = new RegExp(getCellCdValidationRule());
    let isValid = rule.test(cellCd);

    if (!isValid && showInvalidMsg && showInvalidMsg === true) {
      showMessageWithSound(
        "error",
        t("error.VALIDATION_ERROR"),
        t("error.loc_cd_invalid"),
        invalidCallback
      );
    }

    return isValid;
  }
};

/**
 * @description 작업 스테이션 코드 Validation을 화면에서 처리할 지 여부
 ********************
 * @return
 */
export const isStationCdValidationEnabled = () => {
  return getBooleanDeviceSetting("validation.station_cd.screen.enabled");
};

/**
 * @description 작업 스테이션 Validation Rule
 ********************
 * @return
 */
export const getStationCdValidationRule = () => {
  return getDeviceSettingValue("validation.station_cd.rule");
};

/**
 * @description 작업 스테이션 코드 유효성 체크
 ************************
 * @param {String} stationCd
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isStationCdValid = (
  stationCd: string,
  showInvalidMsg?: boolean,
  invalidCallback?: () => void
): boolean => {
  if (!isStationCdValidationEnabled()) {
    return true;
  } else {
    let rule = new RegExp(getStationCdValidationRule());
    let isValid = rule.test(stationCd);

    if (!isValid && showInvalidMsg && showInvalidMsg === true) {
      showMessageWithSound(
        "error",
        t("error.VALIDATION_ERROR"),
        t("error.station_cd_invalid"),
        invalidCallback
      );
    }

    return isValid;
  }
};

/**
 * @description 슈트 코드 Validation을 화면에서 처리할 지 여부
 ********************
 * @return
 */
export const isChuteCdValidationEnabled = () => {
  return getBooleanDeviceSetting("validation.chute_cd.screen.enabled");
};

/**
 * @description 슈트 코드 Validation Rule
 ********************
 * @return
 */
export const getChuteCdValidationRule = () => {
  return getDeviceSettingValue("validation.chute_cd.rule");
};

/**
 * @description 슈트 코드 유효성 체크
 ************************
 * @param {String} chuteCd
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isChuteCdValid = (
  chuteCd: string,
  showInvalidMsg: boolean,
  invalidCallback: any
): boolean => {
  if (!isChuteCdValidationEnabled()) {
    return true;
  } else {
    let rule = new RegExp(getChuteCdValidationRule());
    let isValid = rule.test(chuteCd);

    if (!isValid && showInvalidMsg && showInvalidMsg === true) {
      showMessageWithSound(
        "error",
        t("error.VALIDATION_ERROR"),
        t("error.chute_cd_invalid"),
        invalidCallback
      );
    }

    return isValid;
  }
};

/**
 * @description 표시기 코드 Validation을 화면에서 처리할 지 여부
 ********************
 * @return
 */
export const isIndCdValidationEnabled = () => {
  return getBooleanDeviceSetting("validation.ind_cd.screen.enabled");
};

/**
 * @description 표시기 코드 Validation Rule
 ********************
 * @return
 */
export const getIndCdValidationRule = () => {
  return getDeviceSettingValue("validation.ind_cd.rule");
};

/**
 * @description 표시기 코드 유효성 체크
 ************************
 * @param {String} indCd
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isIndCdValid = (
  indCd: string,
  showInvalidMsg: boolean,
  invalidCallback: any
): boolean => {
  if (!isIndCdValidationEnabled()) {
    return true;
  } else {
    let rule = new RegExp(getIndCdValidationRule());
    let isValid = rule.test(indCd);

    if (!isValid && showInvalidMsg && showInvalidMsg === true) {
      showMessageWithSound(
        "error",
        t("error.VALIDATION_ERROR"),
        t("error.ind_cd_invalid"),
        invalidCallback
      );
    }

    return isValid;
  }
};

/**
 * @description 표시기 QR 코드 Validation을 화면에서 처리할 지 여부
 ********************
 * @return
 */
export const isIndQRCdValidationEnabled = () => {
  return getBooleanDeviceSetting("validation.ind_qr_cd.screen.enabled");
};

/**
 * @description 표시기 QR 코드 Validation Rule
 ********************
 * @return
 */
export const getIndQRCdValidationRule = () => {
  return getDeviceSettingValue("validation.ind_qr_cd.rule");
};

/**
 * @description 표시기 코드 유효성 체크
 ************************
 * @param {String} indQrCd
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isIndQRCdValid = (
  indQrCd: string,
  showInvalidMsg?: boolean,
  invalidCallback?: () => void
): boolean => {
  if (!isIndQRCdValidationEnabled()) {
    return true;
  } else {
    let rule = new RegExp(getIndQRCdValidationRule());
    let isValid = rule.test(indQrCd);

    if (!isValid && showInvalidMsg && showInvalidMsg === true) {
      showMessageWithSound(
        "error",
        t("error.VALIDATION_ERROR"),
        t("error.ind_cd_invalid"),
        invalidCallback
      );
    }

    return isValid;
  }
};

/**
 * @description 표시기 QR 코드로 부터 표시기 코드 추출 룰
 ********************
 * @return
 */
export const getIndCdFromIndQrRule = () => {
  return getDeviceSettingValue("ind_cd.from.ind_qr.rule");
};

/**
 * @description 표시기 QR 코드에서 표시기 코드 추출
 ************************
 * @param {String} indQrValue 표시기 QR 코드 값
 * @return {String} 표시기 코드 값
 */
export const getIndCdByIndQr = (indQrValue: string): string => {
  let rule = getIndCdFromIndQrRule();

  if (!rule) {
    showMessageWithSound(
      "error",
      t("error.VALIDATION_ERROR"),
      t("error.ind_cd_from_ind_qr_rule_not_found")
    );
    return;
  }

  let rules = rule.split(",");
  let startIdx = rules[0];
  let endIdx = rules[1];

  return indQrValue.substring(startIdx, endIdx);
};

/**
 * @description 랙 코드 Validation을 화면에서 처리할 지 여부
 ********************
 * @return
 */
export const isRackCdValidationEnabled = () => {
  return getBooleanDeviceSetting("validation.rack_cd.screen.enabled");
};

/**
 * @description 랙 코드 Validation Rule
 ********************
 * @return
 */
export const getRackCdValidationRule = () => {
  return getDeviceSettingValue("validation.rack_cd.rule");
};

/**
 * @description 랙 코드 유효성 체크
 ************************
 * @param {String} rackCd
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isRackCdValid = (
  rackCd: string,
  showInvalidMsg: boolean,
  invalidCallback: any
): boolean => {
  if (!isRackCdValidationEnabled()) {
    return true;
  } else {
    let rule = new RegExp(getRackCdValidationRule());
    let isValid = rule.test(rackCd);

    if (!isValid && showInvalidMsg && showInvalidMsg === true) {
      showMessageWithSound(
        "error",
        t("error.VALIDATION_ERROR"),
        t("error.rack_cd_invalid"),
        invalidCallback
      );
    }

    return isValid;
  }
};

/**
 * @description 상품 바코드 Validation을 화면에서 처리할 지 여부
 ********************
 * @return
 */
export const isSkuBarcdValidationEnabled = () => {
  return getBooleanDeviceSetting("validation.sku_barcd.screen.enabled");
};

/**
 * @description 상품 바코드 Validation Rule
 ********************
 * @return
 */
export const getSkuBarcdValidationRule = () => {
  return getDeviceSettingValue("validation.sku_barcd.rule");
};

/**
 * @description 상품 바코드 유효성 체크
 ************************
 * @param {String} skuBarcd
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isSkuBarcdValid = (
  skuBarcd: string,
  showInvalidMsg?: boolean,
  invalidCallback?: () => void
): boolean => {
  if (!isSkuBarcdValidationEnabled()) {
    return true;
  } else {
    let rule = new RegExp(getSkuBarcdValidationRule());
    let isValid = rule.test(skuBarcd);

    if (!isValid && showInvalidMsg && showInvalidMsg === true) {
      showMessageWithSound(
        "error",
        t("error.VALIDATION_ERROR"),
        t("error.sku_barcd_invalid"),
        invalidCallback
      );
    }

    return isValid;
  }
};

/**
 * @description 상품 코드 Validation을 화면에서 처리할 지 여부
 ********************
 * @return
 */
export const isSkuCdValidationEnabled = () => {
  return getBooleanDeviceSetting("validation.sku_cd.screen.enabled");
};

/**
 * @description 상품 코드 Validation Rule
 ********************
 * @return
 */
export const getSkuCdValidationRule = () => {
  return getDeviceSettingValue("validation.sku_cd.rule");
};

/**
 * @description 상품 코드 유효성 체크
 ************************
 * @param {String} skuCd
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isSkuCdValid = (
  skuCd: string,
  showInvalidMsg?: boolean,
  invalidCallback?: () => void
): boolean => {
  if (!isSkuCdValidationEnabled()) {
    return true;
  } else {
    let rule = new RegExp(getSkuCdValidationRule());
    let isValid = rule.test(skuCd);

    if (!isValid && showInvalidMsg && showInvalidMsg === true) {
      showMessageWithSound(
        "error",
        t("error.VALIDATION_ERROR"),
        t("error.sku_cd_invalid"),
        invalidCallback
      );
    }

    return isValid;
  }
};

/**
 * @description 각종 바코드 validation 체크 후 알수 없는 바코드 일때 custom 서비스로 호출 할지 여부
 ********************
 * @return
 */
export const isBarcdValidationFailToCustom = () => {
  return getBooleanDeviceSetting("validation.barcd.fail.to.custom");
};

/**
 * @description 시리얼 번호 Validation을 화면에서 처리할 지 여부
 ********************
 * @return
 */
export const isSerialNoValidationEnabled = () => {
  return getBooleanDeviceSetting("validation.serial_no.screen.enabled");
};

/**
 * @description 시리얼 번호 Validation Rule
 ********************
 * @return
 */
export const getSerialNoValidationRule = () => {
  return getDeviceSettingValue("validation.serial_no.rule");
};

/**
 * @description 시리얼 번호 유효성 체크
 ************************
 * @param {String} serialNo
 * @param {Boolean} showInvalidMsg
 * @param {Object} invalidCallback
 * @return {Boolean}
 */
export const isSerialNoValid = (
  serialNo: string,
  showInvalidMsg?: boolean,
  invalidCallback?: any
): boolean => {
  if (!isSerialNoValidationEnabled()) {
    return true;
  } else {
    let rule = new RegExp(getSerialNoValidationRule());
    let isValid = rule.test(serialNo);

    if (!isValid && showInvalidMsg && showInvalidMsg === true) {
      showMessageWithSound(
        "error",
        t("error.VALIDATION_ERROR"),
        t("error.serial_no_invalid"),
        invalidCallback
      );
    }

    return isValid;
  }
};

/**
 * @description 시리얼 번호를 서버에서 체크해야 하는지 여부
 ********************
 * @return
 */
export const isSerialCheckUnique = () => {
  return getBooleanDeviceSetting("pick.serial.check.unique");
};

/**
 * @description 시리얼 번호로 부터 상품 바코드 추출 룰
 ********************
 * @return
 */
export const getSkuBarcodeFromSerialNoRule = () => {
  return getDeviceSettingValue("sku_barcd.from.serial_no.rule");
};

/**
 * @description 시리얼 번호로 부터 시리얼 추출 룰
 ********************
 * @return
 */
export const getSerialFromSerialNoRule = () => {
  return getDeviceSettingValue("serial.from.serial_no.rule");
};

/**
 * @description DPS 박스 투입시 화면에서 박스 타입을 선택 할지 여부
 ********************
 * @returns
 */
export const getBoxInputBySelectBoxType = () => {
  return getBooleanDeviceSetting("input.box_type.select.enabled");
};

/**
 * @description DPS 박스 투입시 박스 바코드에서 추출할 박스 타입 룰(정규식)
 ********************
 * @returns
 */
export const getBoxTypeRule = () => {
  return getDeviceSettingValue("validation.box_type.rule");
};

/**
 * @description DPS 박스 투입시 박스 바코드에서 박스 타입 추출
 ********************
 * @returns
 */
export const getBoxTypeByBoxId = (boxId: string) => {
  // 박스 타입 룰
  var boxTypeRegExp = getBoxTypeRule();

  // 박스 타입 룰이 지정되어 있지 않으면 return
  if (!boxTypeRegExp) return undefined;

  // 정규식 추출
  var boxTypeMatch = boxId.match(boxTypeRegExp);

  // 추출 결과가 없으면 return
  if (!boxTypeMatch) return undefined;

  // 추출 결과 return
  return boxTypeMatch.toString();
};

/**
 * @description 시리얼 번호에서 상품 바코드 추출
 ************************
 * @param {String} serialNo
 * @return {String}
 */
export const getSkuBarcodeFromSerialNo = (serialNo: string): string => {
  let rule = getSkuBarcodeFromSerialNoRule();

  if (!rule) {
    showMessageWithSound(
      "error",
      t("error.VALIDATION_ERROR"),
      t("error.sku_barcd_from_serial_no_rule_not_found")
    );
    return;
  }

  let rules = rule.split(",");
  let startIdx = rules[0];
  let endIdx = rules[1];

  return serialNo.substring(startIdx, endIdx);
};

/**
 * @description 시리얼 번호에서 시리얼 추출
 ************************
 * @param {String} serialNo
 * @return {String}
 */
export const getSerialFromSerialNo = (serialNo: string): string => {
  let rule = getSerialFromSerialNoRule();

  if (!rule) {
    showMessageWithSound(
      "error",
      t("error.VALIDATION_ERROR"),
      t("error.serial_from_serial_no_rule_not_found")
    );
    return;
  }

  let rules = rule.split(",");
  let startIdx = rules[0];
  let endIdx = rules[1];

  if (startIdx == 0 && endIdx == 0) {
    return serialNo;
  }

  return serialNo.substring(startIdx, endIdx);
};

/**
 * 커스텀 사운드 파일 기본 경로 URL
 *****************************
 */
export const getCustomSoundFileBaseUrl = () => {
  return getDeviceSettingValue("custom.sound.file.url");
};

/**
 * @description 반품 중분류 범위 설정 ( rack / station )
 ********************
 * @return 다음 주문 선택 모드
 */
export const getJobRtnMiddelAssortMode = () => {
  var mode = getDeviceSettingValue("job.rtn.middle.assort.mode");

  if (!mode || mode == null) {
    mode = "rack";
  }

  return mode.toLowerCase();
};

/**
 * @description Das 박스 라벨 재출력 사용 여부
 ********************
 * @return true / false
 */
export const isDasReprintBoxLabelEnabled = () => {
  return getBooleanDeviceSetting("das.reprint.box_label.enabled");
};
/**
 * @description Das 박스 라벨 재출력 사용 여부
 ********************
 * @return true / false
 */
export const isRtnReprintBoxLabelEnabled = () => {
  return getBooleanDeviceSetting("rtn.reprint.box_label.enabled");
};

/*******************************************************************************
 *                                  유틸리티 함수
 ******************************************************************************/
export const showMessage = (
  title: string,
  message: string,
  confirmCallback?: () => void
) => {
  createConfirm({
    title: title,
    content: message,
    onOk: confirmCallback,
  });
};

/**
 * @description Warning 팝업 표시
 ******************
 * @param {String} title
 * @param {String} message
 * @param {Function} cancelCallback
 * @param {Function} confirmCallback
 */
export const showConfirm = (
  title: any,
  message: any,
  cancelCallback?: any,
  confirmCallback?: any
) => {
  createConfirm({
    title: title,
    content: message,
    onCancel: cancelCallback,
    onOk: confirmCallback,
  });
};

/**
 * @description 설비 설정이 비어있는 경우 핸들러
 ********************
 */
export const handleRequiredSettingEmpty = () => {
  showMessage(t("text.selecting_equipment"), t("text.select_equipment"), () => {
    location.hash = "/logis_setting";
  });
};

/**
 * @description 설정에 설비 설정이 비어있는 지 체크
 ********************
 */
export const checkRequiredSettingEmpty = () => {
  if (!getEquipCd()) {
    handleRequiredSettingEmpty();
    return false;
  } else {
    return true;
  }
};

/**
 * @description 팝업 표시
 ******************
 * @param {String} title 팝업 타이틀
 * @param {Component} component 팝업
 * @param {any} properties component 속성 정보
 * @param {String} width 60%
 * @param {String} height 60%
 * @param {Function} openCallback 팝업 오픈 때 실행 될 콜백 함수
 * @param {Function} closeCallback 팝업 닫을 때 실행 될 콜백 함수
 */
export const showPopup = (
  title: any,
  component: Component,
  properties: any,
  width: any,
  height: any,
  openCallback?: (arg0: any, arg1: any) => void,
  closeCallback?: (arg0: any, arg1: any) => void
) => {
  let vnode = createVNode(component, properties);
  document.dispatchEvent(
    new CustomEvent("open-dialog", {
      detail: {
        component: vnode,
        properties: { ...properties },
        title: title,
        width: width,
        height: height,
        // openCallback: (event: CustomEvent) => {
        // if (openCallback && typeof openCallback == "function") {
        openCallback,
        //   }
        // },
        closeCallback,
      },
    })
  );
  return vnode?.component?.exposed;
};

/**
 * @description 팝업 닫기
 ******************
 */
export const closePopup = (event?: CustomEventInit<unknown>) => {
  document.dispatchEvent(new CustomEvent("close-dialog", event ? event : {}));
};

/**
 * @description 현재 작업 위치 변경시
 ******************
 * @param {String} workSideCd
 * @param {String} screen
 */
export const setWorkSide = (
  workSideCd: string,
  screen: { showFront: boolean; showRear: boolean; showTotal: boolean }
) => {
  if (!workSideCd) {
    workSideCd = "F";
  }

  screen.showFront = false;
  screen.showRear = false;
  screen.showTotal = false;

  if (
    workSideCd === "F" ||
    workSideCd === "ALL" ||
    workSideCd === "f" ||
    workSideCd === "all"
  ) {
    screen.showFront = true;
  }

  if (
    workSideCd === "R" ||
    workSideCd === "ALL" ||
    workSideCd === "r" ||
    workSideCd === "all"
  ) {
    screen.showRear = true;
  }

  if (workSideCd === "T") {
    screen.showTotal = true;
  }
};

export const showToastMessage = function (type: string, message: string) {
  toastMessage[type](message);
};

/**
 * @description 알림성 토스트 메시지 출력
 ******************
 * @param message
 */
export const showToastInfoMessage = (message: string) => {
  toastMessage.info(message);
};

/**
 * @description 경고성 토스트 메시지 출력
 ******************
 * @param message
 */
export const showToastWarnMessage = (message: string) => {
  toastMessage.warning(message);
};

/**
 * @description 오류성 토스트 메시지 출력
 ******************
 * @param message
 */
export const showToastErrorMessage = (message: string) => {
  toastMessage.error(message);
};

/**
 * @description 정보 / 경고 팝업 표시
 ******************
 * @param {String} type 메시지 타입 - info / error
 * @param {String} title 메시지 타이틀
 * @param {String} message 팝업 메시지
 * @param {Function} callback 콜백 함수
 */
export const showMessageWithSound = (
  type: "info" | "error",
  title: string,
  message: string,
  callback?: () => void
) => {
  playSound(type);
  showMessage(title, message, callback);
};

let errorSound: AnyObject;
let warnSound: AnyObject;
let infoSound: AnyObject;
export const useMsgSound = () => {
  errorSound = useSound(errorSfx);
  warnSound = useSound(warningSfx);
  infoSound = useSound(infoSfx);
};
/**
 * @description 음성 play
 ******************
 * @param {String} type 메시지 타입 - info / error
 * @return {Object} 커스텀 이벤트 (CustomEvent) 객체
 */
export const playSound = (type: "info" | "error" | "warning"): void => {
  if (type && type === "error") {
    errorSound.play();
  } else if (type && type === "warning") {
    warnSound.play();
  } else {
    infoSound.play();
  }
};

/**
 * @description items의 내용 중에 left_qty가 0인 항목이 아래로 가도록 소팅
 ******************
 * @param {Array} items
 * @return {Array}
 */
export const sortByLeftQty = (items: any[]): Array<any> => {
  items.sort((a, b) => {
    if (a.left_qty == b.left_qty) {
      return 0;
    } else if (a.left_qty > 0 && b.left_qty == 0) {
      return -1;
    } else {
      return 1;
    }
  });

  return items;
};

/**
 * @description 콤보 박스의 선택된 값을 리턴
 ******************
 * @param {Object} combo
 * @param {String} defaultValue
 * @return {String}
 */
export const getComboSelectValue = (
  combo: { selectedOptions: { value: any }[] },
  defaultValue: any
): string => {
  defaultValue = defaultValue ? defaultValue : null;
  if (!combo) {
    return defaultValue;
  } else {
    return combo.selectedOptions[0]
      ? combo.selectedOptions[0].value
      : defaultValue;
  }
};

/**
 * @description 콤보 박스의 선택된 텍스트를 리턴
 ******************
 * @param {Object} combo
 * @param {String} defaultValue
 * @return {String}
 */
export const getComboSelectText = (
  combo: { selectedOptions: { innerText: any }[] },
  defaultValue: any
): string => {
  defaultValue = defaultValue ? defaultValue : null;
  if (!combo) {
    return defaultValue;
  } else {
    return combo.selectedOptions[0]
      ? combo.selectedOptions[0].innerText
      : defaultValue;
  }
};

/**
 * @description 커스텀 이벤트 객체를 생성
 ******************
 * @param {Object} eventName
 * @param {String} paramObj
 * @return {Object} 커스텀 이벤트 (CustomEvent) 객체
 */
export const newCustomEvent = (eventName: string, paramObj: any): object => {
  return new CustomEvent(eventName, { detail: paramObj });
};


/**
 * @description 데이터 빈 값 여부 검증 맟 경고 메시지 표현
 ******************
 * @param {Object[]} input 데이터 , 해당 데이터의 라벨(경고메시지 동적으로 표시하기 위함)
    파라미터 예시
    const columnValueArray = [
          {value:palletScanFromRef.value, label:"pallet_scan_from"},
          {value:skuIdScanRef.value, label:"pallet_scan_to"},
          {value:skuCodeRef.value, label:"sku_scan"},
          {value:skuQtyRef.value, label:"sku_qty"},
        ]
  @returns {Object} null 데이터 여부(있으면 true 반환),없는 데이터에 대한 라벨  
 */

 export function dataValidation(columnArray:any[]){
  let result = {
                bool:false,
                label:null
               }
  for(let item of columnArray){
    if(!item.value){
      result.bool = true;
      result.label = item.label;
      break;}
  }
  return result;
}

