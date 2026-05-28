import { onMounted, onUnmounted, Ref } from 'vue'
import { Vector3, MOUSE } from 'three'

export function useCameraMove(
  cameraRef: Ref<any>,
  controlsRef: Ref<any>,
  moveSpeed: number = 0.5,
  rotateSpeed: number = 0.03
) {
  const keys = { w: false, a: false, s: false, d: false, q: false, e: false, r: false, f: false }
  let animationFrameId: number | null = null

  // 사용자가 입력창에 타이핑 중인지 확인하는 헬퍼 함수
  const isTypingInInput = (e: KeyboardEvent) => {
    const target = e.target as HTMLElement;
    // 태그 이름이 INPUT, TEXTAREA, SELECT 이거나 편집 가능한 영역(contenteditable)이면 true 반환
    return ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) || target.isContentEditable;
  }

  // 버튼 매핑이 한 번만 실행되도록 체크하는 플래그 변수
  let isMouseMapped = false

  const handleKeyDown = (e: KeyboardEvent) => {
    // 입력창에서 타이핑 중이면 카메라 조작 무시
    if (isTypingInInput(e)) return;

    const key = e.key.toLowerCase()
    if (key in keys) keys[key as keyof typeof keys] = true
  }

  const handleKeyUp = (e: KeyboardEvent) => {
    // 입력창에서 타이핑 중이면 카메라 조작 무시
    if (isTypingInInput(e)) return;

    const key = e.key.toLowerCase()
    if (key in keys) keys[key as keyof typeof keys] = false
  }

  const updateCamera = () => {
    animationFrameId = requestAnimationFrame(updateCamera)

    const camera = cameraRef.value
    const rawControls = controlsRef.value
    let actualControls = null

    if (rawControls) {
      if (rawControls.target) actualControls = rawControls
      else if (rawControls.value?.target) actualControls = rawControls.value
      else if (rawControls.controls?.target) actualControls = rawControls.controls
      else if (rawControls.instance?.target) actualControls = rawControls.instance
    }

    if (!camera || !actualControls || !actualControls.target) {
      if (keys.w || keys.a || keys.s || keys.d || keys.q || keys.e || keys.r || keys.f) {
        console.warn('⚠️ 3D 객체를 찾지 못해 이동이 무시되었습니다!', { camera, rawControls })
      }
      return
    }

    // 마우스 버튼 기능 강제 재할당 (최초 1회만 실행)
    if (!isMouseMapped) {
      actualControls.mouseButtons = {
        LEFT: null,          // 좌클릭: OrbitControls 개입 금지 (객체 선택용으로 비워둠)
        MIDDLE: MOUSE.DOLLY, // 휠클릭: 줌인/줌아웃 (기본값)
        RIGHT: MOUSE.ROTATE  // 우클릭: 카메라 회전으로 변경!
      }
      isMouseMapped = true
    }

    // WASD + RF 투명 캐릭터 평행 및 수직 이동 유지
    const forward = new Vector3()
    camera.getWorldDirection(forward)
    forward.y = 0

    if (forward.lengthSq() > 0.0001) {
      forward.normalize()
    } else {
      forward.set(0, 0, -1)
    }

    const right = new Vector3()
    right.crossVectors(forward, camera.up).normalize()

    const moveDelta = new Vector3(0, 0, 0)
    let isMoved = false

    if (keys.w) { moveDelta.addScaledVector(forward, moveSpeed); isMoved = true }
    if (keys.s) { moveDelta.addScaledVector(forward, -moveSpeed); isMoved = true }
    if (keys.a) { moveDelta.addScaledVector(right, -moveSpeed); isMoved = true }
    if (keys.d) { moveDelta.addScaledVector(right, moveSpeed); isMoved = true }
    if (keys.r) { moveDelta.y += moveSpeed; isMoved = true }
    if (keys.f) { moveDelta.y -= moveSpeed; isMoved = true }

    if (isMoved) {
      actualControls.target.add(moveDelta)
      camera.position.add(moveDelta)
    }

    // QE 카메라 공전 유지
    if (keys.q || keys.e) {
      const offset = camera.position.clone().sub(actualControls.target)
      const angle = keys.e ? rotateSpeed : -rotateSpeed

      offset.applyAxisAngle(new Vector3(0, 1, 0), angle)
      camera.position.copy(actualControls.target.clone().add(offset))
    }

    if (typeof actualControls.update === 'function') {
      actualControls.update()
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', handleKeyDown)
    window.addEventListener('keyup', handleKeyUp)
    updateCamera()
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeyDown)
    window.removeEventListener('keyup', handleKeyUp)
    if (animationFrameId !== null) {
      cancelAnimationFrame(animationFrameId)
    }
  })
}
