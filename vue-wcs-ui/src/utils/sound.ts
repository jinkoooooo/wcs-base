// 공용 알림음 — vue-ui-operator 프레임워크와 동일한 /audio/*.wav 자산을 재사용.
// 의존성 없이 네이티브 Audio 로 재생. info / warning / error 3종.

const SOUND_SRC = {
  info: '/audio/info.wav',
  warning: '/audio/warning.wav',
  error: '/audio/error.wav',
} as const;

export type SoundType = keyof typeof SOUND_SRC;

// 타입별 Audio 인스턴스 캐시 — 중복 생성 방지, 매 호출 재사용.
const cache: Partial<Record<SoundType, HTMLAudioElement>> = {};

// 알림음 재생. 연속 호출 시 처음으로 되감아 재생.
export function playSound(type: SoundType): void {
  try {
    let audio: HTMLAudioElement | undefined = cache[type];
    if (!audio) {
      audio = new Audio(SOUND_SRC[type]);
      cache[type] = audio;
    }
    audio.currentTime = 0;
    void audio.play();
  } catch {
    /* 자동재생 차단·미지원 — 무음 */
  }
}
