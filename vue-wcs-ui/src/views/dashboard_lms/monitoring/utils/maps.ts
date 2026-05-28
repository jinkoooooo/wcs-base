import { notification } from 'ant-design-vue';
import type { CenterMetaType } from '@/views/dashboard_lms/monitoring/types';
import logoImg from '@/assets/images/logo_filled.png';

// 지도
declare global {
  interface Window {
    navermap_authFailure?: () => void;
    naver?: any;
  }
}
let loadingPromise: Promise<void> | null = null;

const OVERLAY_ZOOM_LEVEL = 13;

// 센터정보
type GeoCacheType = Record<string, { lat: number; lng: number; timestamp: number; }>;
const GEO_CACHE_KEY = 'lms:naver:geocache:v1';
const CACHE_EXPIRE_MS = 24 * 60 * 60 * 1000; // 캐시 유효기간 (24시간)

/**
 * 지도 로드
 */

export async function loadMap(): Promise<void> {
  if (window.naver?.maps) return;

  // 1. 인증 실패 콜백 등록
  if (!window.navermap_authFailure) {
    window.navermap_authFailure = () => {
      // console.error('[NaverMaps] 인증 실패: 키/허용 도메인/권한을 확인하세요');
      notification.error({
        message: '지도 인증 실패',
        description: '지도 인증에 실패했습니다. 키 또는 허용 도메인을 확인하세요',
        duration: 2,
      });
    };
  }

  // 2. 환경변수 확인
  const clientId = import.meta.env.VITE_NAVER_MAP_KEY?.trim();
  if (!clientId) {
    const hint = import.meta.env.DEV
      ? '(개발환경)'
      : '(배포환경) CI/CD Variables 설정을 확인하세요';
    // console.error('[NaverMaps] 환경변수가 비어있습니다 ' + hint);

    // 사용자 알림
    notification.warning({
      message: '지도 설정 누락',
      description: '지도를 불러올 수 없습니다',
      duration: 2,
    });
    return;
  }

  // 3. 로딩 상태 확인
  if (loadingPromise) {
    await loadingPromise;
    return;
  }

  // 4. SDK 동적 로드 - 최초 로딩
  loadingPromise = new Promise<void>((resolve, reject) => {
    const newScript = document.createElement('script');
    newScript.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${ clientId }&submodules=geocoder`;
    newScript.async = true;
    newScript.onload = () => {
      resolve();
      loadingPromise = null;
    };

    newScript.onerror = () => {
      // console.error('[NaverMaps] 스크립트 로드 실패');
      notification.error({
        message: '지도 로드 실패',
        description: '지도 스크립트를 불러오지 못했습니다.',
        duration: 2,
      });
      loadingPromise = null;
      reject(new Error('Naver Maps script load failed'));
    };
    document.head.appendChild(newScript);
  });

  await loadingPromise;
}

/**
 * 캐시 관리
 */

// 센터주소별 위도, 경도 캐시 조회 - 만료데이터 필터링 후 재 저장
function loadGeoCache(): GeoCacheType {
  try {
    const rawData = sessionStorage.getItem(GEO_CACHE_KEY);
    if (!rawData) return {};

    const cache: GeoCacheType = JSON.parse(rawData);
    const now = Date.now();
    let isChanged = false;

    for (const key in cache) {
      if (now - cache[key].timestamp > CACHE_EXPIRE_MS) {
        delete cache[key];
        isChanged = true;
      }
    }

    if (isChanged) saveGeoCache(cache);
    return cache;
  } catch {
    return {};
  }
}

// 센터주소별 위도, 경도 정보 세션캐시에 저장
function saveGeoCache(cache: GeoCacheType) {
  try {
    sessionStorage.setItem(GEO_CACHE_KEY, JSON.stringify(cache))
  } catch (e) {
    // console.warn('[saveGeoCache] 저장 실패', e);
  }
}

// 센터주소별 위도, 경도 캐시 수동 리셋
export function clearGeoCache() {
  try {
    // localStorage.removeItem(GEO_CACHE_KEY);
    sessionStorage.removeItem(GEO_CACHE_KEY);
    notification.success({
      message: '캐시 리셋 완료',
      description: '저장된 주소 좌표 정보를 모두 초기화했습니다.',
    })
  } catch (e) {
    // console.error('캐시 삭제 실패', e);
  }
}

/**
 * 주소 변환
 */

// 단일 주소의 좌표 검색
export async function getGeocodeOne(
  address: string,
): Promise<{ lat: number; lng: number } | null | undefined> {
  try {
    const { naver } = window as any;

    const query = address.trim();
    if (!query) {
      // console.log('[getGeocodeOne] 좌표 변환할 주소를 입력해주세요');
      return null;
    }

    // 1. 캐시가 존재하면 반환
    const cache = loadGeoCache();
    if (cache[query]) {
      return { lat: cache[query].lat, lng: cache[query].lng };
    }

    // 2. 좌표변환
    const result = await new Promise<{ lat: number; lng: number } | null>((resolve) => {
      const options = { query };

      function handleGeocode(status: any, response: any) {
        if (status === naver.maps.Service.Status.ERROR) {
          // console.log('[handleGeocode] FAIL: 조회 불가 query = ', query);
          resolve(null);
          return;
        }

        const addressList = response?.v2?.addresses as Array<{ x: string; y: string }> | undefined; // 필요시 jibunAddress 사용
        if (!addressList || addressList.length === 0 || response.v2.meta.totalCount === 0) {
          // console.log('[handleGeocode] FAIL: 조회 결과 없음');
          resolve(null);
          return;
        }

        const firstAddress = addressList[0];
        const lat = Number(firstAddress.y);
        const lng = Number(firstAddress.x);
        if (Number.isFinite(lat) && Number.isFinite(lng)) {
          resolve({ lat, lng });
        } else {
          // console.log('[handleGeocode] FAIL: 유효하지 않은 lat/lng 값입니다');
          resolve(null);
        }
      }

      naver.maps.Service.geocode(options, handleGeocode);
    });

    // 3. 캐시 업데이트
    if (result) {
      cache[query] = { ...result, timestamp: Date.now(), };
      saveGeoCache(cache);
    }

    // 4. 주소 변환 결과 반환
    return result;
  } catch (e) {
    // console.warn('[getGeocodeOne] error = ', e);
  }
}

// 주소 목록의 좌표 검색
export async function getGeocodeList(
  data: Array<{ lc_id: string; lc_nm: string; address: string }>,
): Promise<Array<{ lc_id: string; lc_nm: string; address: string; lat: number; lng: number }>> {
  const result: Array<{ lc_id: string; lc_nm: string; address: string; lat: number; lng: number }> =
    [];

  for (const record of data) {
    const geocode = await getGeocodeOne(record.address);
    if (geocode) result.push({ ...record, ...geocode });
  }

  if (result.length === 0) {
    notification.warning({
      message: '지오코딩 결과 없음',
      description: '주소를 좌표로 변환하지 못했습니다. 주소 형식을 확인하세요.',
      duration: 2,
    });
  }
  return result;
}

// 지도에 바로 사용할 수있도록 센터의 위/경도 좌표, 메타정보 변환
export function setLatLngList(centers: CenterMetaType[]) {
  const { naver } = window as any;
  // {raw: 센터 원시데이터, posLatLng: LatLng객체}
  return centers
    .filter((it) => Number.isFinite(it.lat) && Number.isFinite(it.lng))
    .map((it) => ({ posLatLng: new naver.maps.LatLng(it.lat, it.lng), raw: it }));
}

/**
 * UI
 */

// 마커 생성
// NOTE: 마커 생성 캐싱 방법 고안
export function createMarker(map: any, latlng: naver.Maps.LatLng, raw: CenterMetaType) {
  const { naver } = window as any;
  // 마커 생성 및 메타정보 주입

  const marker = new naver.maps.Marker({
    map,
    position: latlng,
    icon: {
      url: logoImg, // 마커 이미지 경로
      size: new naver.maps.Size(24, 24), // 아이콘 영역 크기
      scaledSize: new naver.maps.Size(24, 24), // 실제 아이콘 크기 (resize)
      anchor: new naver.maps.Point(12, 12), // 기준점 위치 (x, y)
    },
  });
  (marker as any).__meta = raw;
  return marker;
}

// 오버레이용 센터정보 UI
export function renderInfoContent(meta: CenterMetaType) {
  if (!meta) {
    return `<div style = "font-weight:bold; padding:10px 12px; background-color:white; line-height:1.45; margin:30px;">데이터 없음</div>`;
  }

  // 상태별 색상
  const status = meta.status ?? 'ERROR';
  const statusColor =
    status === 'RUN'
      ? '#16A34A' // green
      : 'WARNING'
      ? '#D97706' // amber (orange)
      : 'ERROR'
      ? '#DC2626' // red
      : 'OFF'
      ? '#6B7280' // gray
      : '#334155'; // slate (black)

  // XSS 방지용 이스케이프 처리
  const escapeChar = (data: unknown) => {
    if (data === null || data === undefined) {
      return '';
    }
    return String(data).replace(
      /[&<>"']/g,
      (replacer) =>
        ({
          '&': '&amp;',
          '<': '&lt;',
          '>': '&gt;',
          '"': '&quot;',
          "'": '&#39;',
        }[replacer] as string),
    );
  };

  const lastHeartBeat = meta.lastHeartbeat ? escapeChar(meta.lastHeartbeat) : '-';

  const alarmBadge = (label: string, val: number, borderColor: string, bgColor: string) =>
    `<span style="font-size: 11px; border: 1px solid ${borderColor}; background: ${bgColor}; padding: 2px 6px; border-radius: 9999px; margin-right: 6px;">${escapeChar(
      label,
    )} ${val}</span>`;

  return `
    <div style="padding: 10px 12px; line-height: 1.45; max-width: 300px; background-color: #fff;">
        <!-- 센터 명 & 상태 -->
        <div style="display: flex; justify-content: space-between; gap: 8px;">
            <div style="font-weight: 700; margin-bottom: 4px;">${escapeChar(meta.lc_nm)}</div>
            <div style="font-size: 12px; color: ${statusColor}">${escapeChar(status)}</div>
        </div>
        <!-- 센터ID & 주소 -->
        <div style="font-size: 12px; color: #64748b; margin-bottom: 6px;">${escapeChar(
          meta.lc_id,
        )} · ${escapeChar(meta.address)}</div>
        <!-- 센터상태 & 마지막 데이터 전송일시 -->
        <div style="display; flex; align-items: center; gap: 8px; margin: 6px 0">
            <span style="width: 8px; height: 8px; border-radius: 9999px; background: ${statusColor}; display: inline-block;"></span>
            <span style="font-size:12px; color: #475569">마지막 업데이트</span>
            <span style="font-size: 11px; color: #64748b;">HB ${lastHeartBeat}</span> 
        </div>
      <!-- 알람 건수 -->
      <div style="margin-top: 6px;">
          ${alarmBadge('INFO', meta.alarm.info, '#dbeafe', '#eff6ff')}
          ${alarmBadge('WARN', meta.alarm.warning, '#fde68a', '#fffbeb')}
          ${alarmBadge('ERR', meta.alarm.error, '#fecaca', '#fef2f2')}
      </div>
      <!-- 버튼 -->
      <button type="button" class="goto-detail-btn" data-action="goto-detail" data-center-id="${escapeChar(
        meta.lc_id,
      )}" style="margin-top: 10px; padding: 6px 10px; border: 1px solid #e5e7eb; background: #f8fafc; border-radius: 6px; cursor: pointer;">상세 보기</button>
    </div>`;
}

// 오버레이 생성 직후 1회 등록(메모리 누수 방지)
function bindOverlayClickToRouter(mapEl: HTMLElement, router: any) {
  mapEl.addEventListener('click', (e) => {
    const btn = (e.target as HTMLElement)?.closest(
      '[data-action="goto-detail"]',
    ) as HTMLElement | null;
    if (!btn) return;
    const id = btn.getAttribute('data-center-id');
    if (!id) return;
    router.push({ name: 'CenterDetail', params: { id } }); // 예: /centers/:id
  });
}

/**
 * 사용자 정의 오버레이 생성/수정/삭제
 * - 좌표 변환, 그리기 : API 처리
 * - 콘텐츠 : 사용자 정의
 * - 로직
 *    오버레이 생성 : overlay.setMap(mapRef) -> onAdd -> draw
 *    오버레이 수정 : setPosition, setCenter, ... -> draw
 *    오버레이 삭제 : overlay.setMap(null) -> onRemove
 * @param map 지도 참조 객체
 * @param latlng 오버레이 위치. {lat: number, lng: number}
 * @param html 오버레이 창 UI
 * @param opts 오버레이 위치 옵션 {xAnchor: number, yAnchor: number}
 */
export function createCustomOverlay(map: any, latlng: any, html: string, opts?: any) {
  // console.log(
  //   '[createCustomOverlay] map = ',
  //   map,
  //   '\n/latlng = ',
  //   latlng,
  //   '\n/ html = ',
  //   html,
  //   '\n/ opts = ',
  //   opts,
  // );
  const { naver } = window as any;

  // OverlayView 상속받는 사용자 정의 오버레이 생성
  function CustomOverlay(this: any, options: any) {
    // this: {map, __targets, __element, _isAdded, _position, ...} // options: {map, position, content}
    this._element = document.createElement('div');
    this._element.style.position = 'absolute';
    this._opts = {
      xAnchor: 0.5,
      yAnchor: 1.2,
      clickable: true,
      side: 'left', // 선 위치 / 'left' | 'right'
      lineColor: '#00386c', // 선 색상
      ...options.opts,
    };

    this._line = null; // polyline 속성

    this.setPosition(options.position);
    this.setContent(options.content || '');

    // 라이프사이클: onAdd() 호출
    this.setMap(options.map || null);
  }

  CustomOverlay.prototype = new naver.maps.OverlayView(); // prototype: {constructor(), onAdd(), draw(), onRemove(), getPosition(), setPosition(), setContent()}
  CustomOverlay.prototype.constructor = CustomOverlay;

  // onAdd():
  // - setMap(map) 또는 옵션의 map 속성 설정시 호출. 즉, 오버레이가 지도에 추가되었을 때 호출
  // - 라이프사이클: 이후 draw() 호출
  CustomOverlay.prototype.onAdd = function () {
    const overlayLayer = this.getPanes().overlayLayer; // getPanes()의 지도 창 MapPanes객체: {overlayLayer: HTMLElement, overlayImage: HTMLElement, floatPane: HTMLElement}
    overlayLayer.appendChild(this._element);
  };

  // draw():
  // - onAdd()이후 초기위치 지정할 때 자동 호출
  // - 지도에서 오버레이를 다시 그려야 할 때마다 호출 (확대/축소/위치 변경으로 좌표 변경 필요할 때. 드래그는 제외)
  CustomOverlay.prototype.draw = function () {
    // 1. 지도 객체가 설정되지 않았으면 그리기 불가
    if (!this.getMap()) {
      return;
    }

    // 2. projection 객체로 LatLng좌표 -> 화면좌표로 변환
    const projection = this.getProjection(); // MapSystemProejction 객체: 좌표변환 메서드 제공
    if (!projection || !this._element || !this._element.parentNode) return;
    const position = this.getPosition();
    const pixelPosition = projection.fromCoordToOffset(position); // 지정한 지도 좌표에 해당하는 픽셀 단위의 offset 좌표. Point 객체 {x: number, y: number}

    // 3. 오버레이 디자인/이벤트 지정
    const { xAnchor, yAnchor, zIndex, clickable, side, lineColor } = this._opts;
    const el = this._element;
    const apply = () => {
      // 3-1. 오버레이 좌표계산 : 마커기준 좌/우
      const width = el.offsetWidth;
      const height = el.offsetHeight;
      let left: number; // 박스 좌측 위치
      let top: number; // 박스 상측 위치

      // 줌에 따라 더 많이 밀어내기 (줌 아웃일수록 멀리)
      const zoom = map.getZoom ? map.getZoom() : 10;
      const baseGap = 50; // 마커와 박스 사이 기본 간격 (pixel)
      const extraMinGap = 60; // 마커와 박스 사이 최소 추가 간격 (pixel)
      const extraGap = 10; // 마커와 박스 사이 추가 간격 (pixel)
      const zoomFactor = Math.max(0, OVERLAY_ZOOM_LEVEL - zoom); // 기준 줌 레벨보다 작을수록 더 멀리
      const dynamicGap = extraMinGap + zoomFactor * extraGap;

      if (side === 'left') {
        // 마커 기준 왼쪽에 정보창 배치
        left = pixelPosition.x - width - baseGap - dynamicGap;
      } else {
        // 마커 기준 오른쪽에 정보창 배치
        left = pixelPosition.x + baseGap + dynamicGap;
      }
      // 마커 기준 세로 중앙에 정보창 배치
      top = pixelPosition.y - height / 2;
      el.style.left = `${left}px`;
      el.style.top = `${top}px`;
      // el.style.left = `${pixelPosition.x - el.offsetWidth * xAnchor}px`; // xAnchor: 오버레이 박스 기준점. 0: 왼, 1: 오
      // el.style.top = `${pixelPosition.y - el.offsetHeight * yAnchor}px`; // yAnchor: 오버레이 박스 기준점. 1: 위, 0: 아래

      // 3-2. 마커-오버레이 연결선 좌표 계산
      const innerX = side === 'left' ? left + width : left ; // 박스 안쪽 모서리
      const innerY = top + height / 2; // 박스 세로 중앙
      const overlayPoint = new naver.maps.Point(innerX, innerY);
      const overlayLatLng = projection.fromOffsetToCoord(overlayPoint);

      if (!this._line) {
        this._line = new naver.maps.Polyline({
          map,
          path: [position, overlayLatLng],
          strokecolor: lineColor, // 선 색상
          strokeOpacity: 1, // 선 투명도
          strokeWeight: 2, // 선 두께
        });

      } else {
        this._line.setPath([position, overlayLatLng]);
        this._line.setMap(map); // 숨겼다가 다시 보일 경우 처리
      }

      // 3-3. 스타일 지정
      if (zIndex != null) el.style.zIndex = String(zIndex);
      el.style.pointerEvents = clickable === false ? 'none' : 'auto';
    };

    if (el.offsetWidth === 0 || el.offsetHeight === 0) {
      requestAnimationFrame(apply);
    } else {
      apply();
    }
  };

  // onRemove():
  // - setMap(null) 또는 opts의 map 속성을 null로 설정 시 호출히여 오버레이 제거
  CustomOverlay.prototype.onRemove = function () {
    if (this._element?.parentNode) {
      this._element.remove(); // DOM 삭제 / this._element.parentNode.removeChild(this._element);
    }

    if (this._line) {
      this._line.setMap(null);
      this._line = null;
    }

    // NOTE: 필요 시 이벤트 핸들러 정리 추가
  };

  // setPosition():
  // - 오버레이 위치 지정
  CustomOverlay.prototype.setPosition = function (position: naver.Maps.LatLng) {
    // const lat = position.lat ?? position.y;
    // const lng = position.lng ?? position.x;
    // if (lat === undefined || lng === undefined) {
    //   console.warn('[setPosition] Fail');
    //   return;
    // }
    //
    // const normalizedPos = new naver.maps.LatLng(lat, lng);

    // this._position = normalizedPos;
    this._position = position;
    this.draw(); // 위치 변경 시 오버레이 재 렌더링
  };

  // getPosition():
  // - 오버레이 위치 조회
  CustomOverlay.prototype.getPosition = function () {
    return this._position;
  };

  // 사용자 정의 메서드 - 오버레이 내용 지정
  CustomOverlay.prototype.setContent = function (content: string | Node) {
    if (!this._element) return;
    if (typeof content === 'string') {
      this._element.innerHTML = content;
    } else {
      this._element.innerHTML = '';
      this._element.appendChild(content);
    }

    this.draw(); // 내용 변경 시 오버레이 재 렌더링
  };

  // setMap 오버라이드
  CustomOverlay.prototype.setMap = function (map: any) {
    naver.maps.OverlayView.prototype.setMap.call(this, map);

    // poly line
    if (this._line) {
      this._line.setMap(map);
    }
  };

  // 오버레이 생성
  const overlay = new CustomOverlay({ map, position: latlng, content: html, opts });

  return overlay;
}
