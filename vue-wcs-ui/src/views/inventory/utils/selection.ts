import { ref, computed, Ref } from 'vue'
import { Vector3 } from 'three'

export function useMultiSelection(cameraRef: Ref<any>, rackList: Ref<any[]>) {
  const selectedCodes = ref<string[]>([])

  // 드래그 상태 관리
  const isDragging = ref(false)
  const startPos = ref({ x: 0, y: 0 })
  const currentPos = ref({ x: 0, y: 0 })

  // 1. 단일 클릭 / Ctrl 클릭 처리
  const handleSingleSelect = (code: string, isCtrl: boolean) => {
    if (isCtrl) {
      if (selectedCodes.value.includes(code)) {
        selectedCodes.value = selectedCodes.value.filter(c => c !== code)
      } else {
        selectedCodes.value.push(code)
      }
    } else {
      selectedCodes.value = [code]
    }
  }

  // 2. 드래그 사각형 CSS 스타일
  const selectionBoxStyle = computed(() => {
    if (!isDragging.value) return { display: 'none' }

    const left = Math.min(startPos.value.x, currentPos.value.x)
    const top = Math.min(startPos.value.y, currentPos.value.y)
    const width = Math.abs(startPos.value.x - currentPos.value.x)
    const height = Math.abs(startPos.value.y - currentPos.value.y)

    return {
      position: 'fixed', // absolute 대신 fixed를 써야 마우스 커서와 100% 일치합니다
      left: `${left}px`,
      top: `${top}px`,
      width: `${width}px`,
      height: `${height}px`,
      backgroundColor: 'rgba(66, 184, 131, 0.2)',
      border: '1px solid #42b883',
      pointerEvents: 'none',
      zIndex: 99
    }
  })

  // 3. 마우스 드래그 이벤트 로직
  const onPointerDown = (e: PointerEvent) => {
    if (e.button !== 0) return // 좌클릭만 반응
    isDragging.value = true
    startPos.value = { x: e.clientX, y: e.clientY }
    currentPos.value = { x: e.clientX, y: e.clientY }
  }

  const onPointerMove = (e: PointerEvent) => {
    if (!isDragging.value) return
    currentPos.value = { x: e.clientX, y: e.clientY }
  }

  const onPointerUp = (e: PointerEvent) => {
    if (!isDragging.value) return
    isDragging.value = false

    // 이동 거리가 너무 짧으면 단순 '클릭'으로 간주하고 무시
    const dx = Math.abs(startPos.value.x - currentPos.value.x)
    const dy = Math.abs(startPos.value.y - currentPos.value.y)
    if (dx < 5 && dy < 5) return

    // Ctrl 안 눌렀으면 기존 선택 초기화
    const isCtrl = e.ctrlKey || e.metaKey
    if (!isCtrl) {
      selectedCodes.value = []
    }

    const minX = Math.min(startPos.value.x, currentPos.value.x)
    const maxX = Math.max(startPos.value.x, currentPos.value.x)
    const minY = Math.min(startPos.value.y, currentPos.value.y)
    const maxY = Math.max(startPos.value.y, currentPos.value.y)

    const camera = cameraRef.value
    if (!camera) return

    // 투영(Projection) 계산으로 박스 안의 객체 찾기
    const newSelections: string[] = []

    rackList.value.forEach(rack => {
      const vec = new Vector3(rack.position[0], rack.position[1], rack.position[2])
      vec.project(camera) // 3D -> 2D 변환

      if (vec.z > 1) return // 카메라 뒤에 있는 객체 무시

      const screenX = (vec.x * 0.5 + 0.5) * window.innerWidth
      const screenY = -(vec.y * 0.5 - 0.5) * window.innerHeight

      if (screenX >= minX && screenX <= maxX && screenY >= minY && screenY <= maxY) {
        if (!selectedCodes.value.includes(rack.code)) {
          newSelections.push(rack.code)
        }
      }
    })

    if (newSelections.length > 0) {
      selectedCodes.value.push(...newSelections)
    }
  }

  const clearSelection = () => {
    selectedCodes.value = []
  }

  return {
    selectedCodes,
    selectionBoxStyle,
    handleSingleSelect,
    onPointerDown,
    onPointerMove,
    onPointerUp,
    clearSelection
  }
}
