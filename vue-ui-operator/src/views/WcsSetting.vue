<template>
  <PageLayout>
    <ion-content :fullscreen="true">
      <div id="container" class="h-full">
        <!-- 언어 선택 -->
        <div id="locales" class="input-container">
          <label style="text-align: left">{{ t("label.language") }}</label>
        </div>
        <div class="input-container">
          <select
            v-model="settingValue.locale"
            :size="size"
            class="selectStyle"
            @change="changeComboLocale"
          >
            <option
              v-for="locale in locales"
              :key="locale.value"
              :value="locale.value"
            >
              {{ locale.label }}
            </option>
          </select>
        </div>

        <!-- 라벨 프린터 설정 -->
        <div id="printer" class="input-container" v-if="isTabletActive">
          <label style="text-align: left">{{ t("label.label_print") }}</label>
        </div>

        <div class="input-container" v-if="isTabletActive">
          <select
            v-model="settingValue.printer"
            :size="size"
            class="selectStyle"
            @change="changePrinter"
          >
            <option
              v-for="printerOption in printerOptions"
              :key="printerOption.value"
              :value="printerOption.value"
            >
              {{ printerOption.label }}
            </option>
          </select>
        </div>

        <!-- 일반 프린터 설정 -->
        <div id="normalPrinter" class="input-container" v-if="isTabletActive">
          <label style="text-align: left">{{ t("label.normal_print") }}</label>
        </div>

        <div class="input-container" v-if="isTabletActive">
          <select
            v-model="settingValue.normalPrinter"
            :size="size"
            class="selectStyle"
            @change="changeNormalPrinter"
          >
            <option
              v-for="normalPrinterOption in normalPrinterOptions"
              :key="normalPrinterOption.value"
              :value="normalPrinterOption.value"
            >
              {{ normalPrinterOption.label }}
            </option>
          </select>
        </div>

        <!-- device 설정 -->
        <div id="device" class="input-container">
          <label style="text-align: left">{{ t("label.device") }}</label>
        </div>

        <div id="input-container">
          <!-- KIOSK, TABLET, PDA 설정 셀렉터 (개발 모드인 경우에만 활성화) -->
          <select
            v-model="settingValue.deviceType"
            :size="size"
            class="selectStyle"
            @change="deviceChanged"
          >
            <option
              v-for="device in devices"
              :key="device.value"
              :value="device.value"
            >
              {{ device.label }}
            </option>
          </select>
        </div>
        <!-- 버전 정보 -->
        <div class="version">{{ t("label.version") }} 3.0</div>
      </div>
    </ion-content>
  </PageLayout>
</template>
<script setup lang="ts">
import { IonContent } from "@ionic/vue";

import { ref, onMounted } from "vue";
import * as LOGIS_UTIL from "@/utils/logisticUtil";
import type { SelectProps } from "ant-design-vue";
import PageLayout from "@/views/common/PageLayout.vue";
import { Input, Select } from "ant-design-vue";
import { useI18n } from "@/hooks/web/useI18n";
import { getCommonGetApi } from "@/api/CommonApi";
import { useLocale } from "@/locales/useLocale";
import type { LocaleType } from "/#/config";
import { AnyObject } from "ant-design-vue/es/_util/type";
import setting from "@/settings/projectSetting";

const { t } = useI18n();

const settings = true;
const loading = ref(false);

const size = ref<SelectProps["size"]>("middle");

const printer = ref();
const { changeLocale } = useLocale();

let isTabletActive = true;
let isPdaActive = false;

const settingValue = ref({
  locale: "ko-KR",
  printer: "",
  deviceType: "",
  normalPrinter: "",
});

const locales = ref<SelectProps["options"]>([
  {
    value: "en-US",
    label: "English",
  },
  {
    value: "ko-KR",
    label: "한국어",
  },
  {
    value: "zh-CN",
    label: "中文",
  },
]);

const devices = ref<SelectProps["options"]>([
  {
    value: "KIOSK",
    label: "KIOSK",
  },
  {
    value: "PDA",
    label: "PDA",
  },
  {
    value: "TABLET",
    label: "TABLET",
  },
]);

let printerOptions = ref<SelectProps["options"]>();
let normalPrinterOptions = ref<SelectProps["options"]>();

/*****************Function Area***************************/
/**
 * @description 언어 값 변경 처리
 * @param value   -- 선택값
 */
const changeComboLocale = async (event: Event) => {
  const value = (event.target as HTMLSelectElement).value;
  settingValue.value.locale = value;
  LOGIS_UTIL.setLocalStorage("setting.locale", settingValue.value.locale);
  await changeLocale(settingValue.value.locale as LocaleType);
  location.reload();
};

/**
 * @description 일반 프린터 변경 처리
 *********************
 * @param {Object} event
 */
const changeNormalPrinter = (event: Event) => {
  const target = event.target as HTMLSelectElement;
  const value = target.value;
  const label = target.options[target.selectedIndex].text;

  settingValue.value.normalPrinter = value;
  LOGIS_UTIL.setLocalStorage(
    "setting.normalPrinter",
    settingValue.value.normalPrinter
  );

  LOGIS_UTIL.setLocalStorage("setting.normalPrinterName", label);
};

/**
 * @description 개발 모드에서 장치 콤보의 값이 변경 되었을 경우
 * 화면을 재구성 하기 위해 localStorage 값 변경 및 reload를 실시
 *********************
 * @param {Object} event
 */
const deviceChanged = async (event: Event) => {
  const target = event.target as HTMLSelectElement;
  const value = target.value;
  settingValue.value.deviceType = value;
  LOGIS_UTIL.setLocalStorage(
    "setting.deviceType",
    settingValue.value.deviceType
  );
  await getDeviceSettingList();
  location.reload();
};

/**
 * @description 현재 설정되어 있는 설정 항목의 값들 또는 초기 값을 나타내기 위함
 *********************
 */
const setDefaultValues = () => {
  let mgrSiteUrl = LOGIS_UTIL.getLocalStorage("setting.mgrSiteUrl");
  let v_locale = LOGIS_UTIL.getLocalStorage("setting.locale");
  let v_device = LOGIS_UTIL.getLocalStorage("setting.deviceType");

  let v_printer = LOGIS_UTIL.getLocalStorage("setting.printerId");
  let v_normalPrinter = LOGIS_UTIL.getLocalStorage("setting.normalPrinter");

  if (!mgrSiteUrl) {
    mgrSiteUrl = location.origin + "/rest";
    LOGIS_UTIL.setLocalStorage("setting.mgrSiteUrl", mgrSiteUrl);
  }

  let currentUrl = new URL(mgrSiteUrl);

  settingValue.value.locale = v_locale ? v_locale : "ko-KR";
  if (devices.value && devices.value.length > 0) {
    settingValue.value.deviceType = v_device
      ? v_device
      : devices.value[0].value;
  }
  // settingValue.value.printer = v_printer
  //   ? v_printer
  //   : printer.value.items[0].printer_cd;
  //
  // settingValue.value.normalPrinter = v_normalPrinter
  //   ? v_normalPrinter
  //   : printer.value.items[0].printer_cd;

  switch (v_device) {
    case "TABLET":
      isPdaActive = false;
      isTabletActive = false;
      break;
    case "PDA":
      isPdaActive = false;
      isTabletActive = false;
      break;
    case "KIOSK":
      isPdaActive = false;
      isTabletActive = false;
      break;
  }

  // 초기 세팅에 대한 LocalStorage 저장
  LOGIS_UTIL.setLocalStorage("setting.locale", settingValue.value.locale);
  LOGIS_UTIL.setLocalStorage("setting.printerId", settingValue.value.printer);

  // LOGIS_UTIL.setLocalStorage(
  //   "setting.deviceType",
  //   settingValue.value.deviceType
  // );
};

const checkDeviceType = () => {
  const width = window.innerWidth;
  if (width > 1024) {
    settingValue.value.deviceType = "KIOSK"; // 테이블 PC 또는 데스크탑
  } else if (width <= 1024 && width > 768) {
    settingValue.value.deviceType = "TABLET"; // 태블릿
  } else {
    settingValue.value.deviceType = "PDA"; // PDA 또는 모바일
  }
  /* 
  const userAgent = navigator.userAgent;
  if (/Mobile|Android|iP(hone|ad)|BlackBerry/.test(userAgent)) {
    settingValue.value.deviceType = "PDA"; // PDA 또는 모바일 기기
  } else if (/Tablet|iPad/.test(userAgent)) {
    settingValue.value.deviceType = "TABLET"; // 태블릿
  } else {
    settingValue.value.deviceType = "KIOSK"; // 데스크탑 또는 랩탑
  }*/
};

onMounted(async () => {
  // 이곳에 마운트 후 실행할 코드를 작성합니다.
  checkDeviceType();
  const param = {
    activeFlag: "true",
  };
  const optionPrint = [];
  const optionNormalPrint = [];
  // printer.value = await getCommonGetApi(`/printers`, null);
  // printer.value.items.map(function (obj) {
  //   if (obj.printer_type == "BARCODE") {
  //     optionPrint.push({
  //       value: obj.id,
  //       label: obj.printer_nm,
  //     });
  //   } else if (obj.printer_type == "NORMAL") {
  //     optionNormalPrint.push({
  //       value: obj.id,
  //       label: obj.printer_nm,
  //     });
  //   }
  // });

  printerOptions.value = optionPrint;
  normalPrinterOptions.value = optionNormalPrint;

  // LOGIS_UTIL.setLocalStorage(
  //   "setting.deviceType",
  //   settingValue.value.deviceType
  // );

  // 현재 설정되어 있는 설정 항목의 값들 또는 초기 값을 나타내기 위함
  setDefaultValues();
  getDeviceSettingList();
});

/**
 * @description 장비 설정 리스트 조회
 *******************************
 * @return
 */
const getDeviceSettingList = async () => {
  let deviceType = LOGIS_UTIL.getLocalStorage("setting.deviceType");
  let stageCd = LOGIS_UTIL.getLocalStorage("setting.stageCd");
  if (deviceType == "" || stageCd == "") return; //초기화
  LOGIS_UTIL.setLocalStorage("setting.deviceSettings", "");
  const deviceSetting = await getCommonGetApi(
    `/device_profiles/configs/${deviceType}/${stageCd}`,
    null
  );
  if (deviceSetting.length > 0) {
    LOGIS_UTIL.setLocalStorage("setting.deviceSettings", deviceSetting);
  }
};
</script>

<style>
#container {
  text-align: center;
  --offset-top: 56px;
  --offset-bottom: 0px;
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #252222;
}

#container strong {
  font-size: 20px;
  line-height: 26px;
}

#container p {
  font-size: 0.5rem;
  line-height: 22px;
  color: #8c8c8c;
  margin: 0;
}

#container a {
  text-decoration: none;
}

.input-container,
.checkbox-container,
.radio-btn-container {
  display: flex;
  margin-bottom: 0.5rem;
}

.input-container label,
.checkbox-container label,
.radio-btn-container label {
  min-width: 20%;
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.5rem;
  text-align: right;
  margin: auto 0;
  padding-right: 0.5rem;
}

.input-container input,
.input-container select {
  width: 100%;
  flex: 1;
}

input[type="checkbox"] {
  margin: auto 0 auto auto;
}

.radio-btn-container .buttons {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.radio-btn-container .buttons label {
  min-width: 0;
  padding: 0;
}

input[type="radio"] {
  margin: auto 1vw auto 3vw;
}

#ble-mode {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

#mgr-ip label {
  display: block;
}

#button-container {
  display: flex;
  margin: 5%;
}

.version {
  font: 14px arial;
  color: white;
  position: relative;
  text-align: right;
  margin-top: 5px;
  padding-top: 5px;
}

input {
  background-color: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.2);
  height: 1rem;
  color: #fff;
  font-size: 0.5rem;
  padding: 0 10px;
  border-radius: unset;
  outline: none;
}
input[type="date"] {
  border-radius: unset;
  outline: none;
}

.select2 {
  -webkit-appearance: none;
  background-image: url("../images/icon-downarrow.png");
  background-size: 0.8rem;
  background-repeat: no-repeat;
  background-position: 96% 50%;
}

.selectStyle {
  width: 100%;
  background-color: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 0.7rem;
  text-align: center;
}
</style>
