/**
 * 고유 UUID를 생성하는 내부 함수
 * 최신 브라우저의 내장 암호화 API를 우선 사용하고, 없을 경우 자체 생성 로직 사용
 */
const generateUUID = () => {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        return crypto.randomUUID();
    }

    // 구형 브라우저 대응 UUID v4 생성 로직
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
};

const WEB_CACHE_KEY = 'WCS_WEB_DEVICE_ID';

/**
 * 기기 고유 ID를 반환하는 함수
 * 1. 안드로이드 앱(PDA) 환경이면 ANDROID_ID 반환
 * 2. 일반 웹 환경이면 localStorage를 확인하여 기존 UUID 반환
 * 3. 캐시에 없으면 새로 생성 후 저장하여 반환
 *
 * @returns {string} 기기 고유 ID
 */
export const getDeviceId = () => {
    // 1. 안드로이드 앱 환경(WebView) 체크
    if (window.AndroidBridge && window.AndroidBridge.getDeviceId) {
        return window.AndroidBridge.getDeviceId();
    }

    // 2. 일반 웹 브라우저 환경 (로컬 스토리지 캐시 확인)
    let webDeviceId = localStorage.getItem(WEB_CACHE_KEY);

    // 3. 캐시에 저장된 ID가 없다면 새로 생성하고 저장
    if (!webDeviceId) {
        webDeviceId = `WEB_${generateUUID()}`;
        localStorage.setItem(WEB_CACHE_KEY, webDeviceId);
    }

    return webDeviceId;
};