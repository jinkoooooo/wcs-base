/* src/views/tspg-4way-shuttle/dashboard_2d/composables/useKeyboardShortcuts.ts */
/**
 * 키보드 단축키 Composable
 *
 * 에디터에서 사용하는 키보드 단축키를 관리합니다.
 */
import { onMounted, onUnmounted, ref } from 'vue';
import { useShuttleStore } from '../store/shuttleStore';

export interface KeyboardShortcut {
  key: string;
  ctrl?: boolean;
  shift?: boolean;
  alt?: boolean;
  description: string;
  action: () => void;
}

export function useKeyboardShortcuts() {
  const store = useShuttleStore();
  const isEnabled = ref(true);

  // 단축키 정의
  const shortcuts: KeyboardShortcut[] = [
    {
      key: 'z',
      ctrl: true,
      description: 'Undo (실행 취소)',
      action: () => store.undo(),
    },
    {
      key: 'z',
      ctrl: true,
      shift: true,
      description: 'Redo (다시 실행)',
      action: () => store.redo(),
    },
    {
      key: 'y',
      ctrl: true,
      description: 'Redo (다시 실행)',
      action: () => store.redo(),
    },
    {
      key: 's',
      ctrl: true,
      description: 'Save (저장)',
      action: async () => {
        try {
          await store.saveAllLayouts();
          console.log('Layouts saved');
        } catch (error) {
          console.error('Save failed:', error);
        }
      },
    },
    {
      key: 'Delete',
      description: 'Delete selected (선택 삭제)',
      action: () => {
        if (store.selectedObjectId) {
          store.removeLayout(store.selectedObjectId);
        }
      },
    },
    {
      key: 'Backspace',
      description: 'Delete selected (선택 삭제)',
      action: () => {
        if (store.selectedObjectId) {
          store.removeLayout(store.selectedObjectId);
        }
      },
    },
    {
      key: 'Escape',
      description: 'Deselect (선택 해제)',
      action: () => store.selectObject(null),
    },
    {
      key: 'a',
      ctrl: true,
      description: 'Select all (전체 선택)',
      action: () => {
        // 다중 선택은 현재 지원하지 않으므로 첫 번째 객체 선택
        if (store.layouts.length > 0) {
          store.selectObject(store.layouts[0].id!);
        }
      },
    },
    // 화살표 키로 객체 이동 (미세 조정)
    // bottom 좌표계: posY가 증가하면 위로 이동
    {
      key: 'ArrowUp',
      description: 'Move up (위로 이동)',
      action: () => nudgeSelected(0, 10),
    },
    {
      key: 'ArrowDown',
      description: 'Move down (아래로 이동)',
      action: () => nudgeSelected(0, -10),
    },
    {
      key: 'ArrowLeft',
      description: 'Move left (왼쪽으로 이동)',
      action: () => nudgeSelected(-10, 0),
    },
    {
      key: 'ArrowRight',
      description: 'Move right (오른쪽으로 이동)',
      action: () => nudgeSelected(10, 0),
    },
    // Shift + 화살표로 1픽셀 이동
    {
      key: 'ArrowUp',
      shift: true,
      description: 'Nudge up (위로 1px)',
      action: () => nudgeSelected(0, 1),
    },
    {
      key: 'ArrowDown',
      shift: true,
      description: 'Nudge down (아래로 1px)',
      action: () => nudgeSelected(0, -1),
    },
    {
      key: 'ArrowLeft',
      shift: true,
      description: 'Nudge left (왼쪽으로 1px)',
      action: () => nudgeSelected(-1, 0),
    },
    {
      key: 'ArrowRight',
      shift: true,
      description: 'Nudge right (오른쪽으로 1px)',
      action: () => nudgeSelected(1, 0),
    },
  ];

  /**
   * 선택된 객체 이동
   */
  function nudgeSelected(dx: number, dy: number) {
    if (store.selectedObjectId) {
      const obj = store.selectedObject;
      if (obj && !obj.isLocked) {
        store.updateLayout(store.selectedObjectId, {
          posX: obj.posX + dx,
          posY: obj.posY + dy,
        });
      }
    }
  }

  /**
   * 키보드 이벤트 핸들러
   */
  function handleKeyDown(event: KeyboardEvent) {
    if (!isEnabled.value) return;

    // 입력 필드에서는 단축키 비활성화
    const target = event.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
      return;
    }

    // 일치하는 단축키 찾기
    for (const shortcut of shortcuts) {
      const keyMatch =
        event.key === shortcut.key || event.key.toLowerCase() === shortcut.key.toLowerCase();
      const ctrlMatch = !!shortcut.ctrl === (event.ctrlKey || event.metaKey);
      const shiftMatch = !!shortcut.shift === event.shiftKey;
      const altMatch = !!shortcut.alt === event.altKey;

      if (keyMatch && ctrlMatch && shiftMatch && altMatch) {
        event.preventDefault();
        event.stopPropagation();
        shortcut.action();
        return;
      }
    }
  }

  /**
   * 단축키 활성화
   */
  function enable() {
    isEnabled.value = true;
  }

  /**
   * 단축키 비활성화
   */
  function disable() {
    isEnabled.value = false;
  }

  /**
   * 이벤트 리스너 등록
   */
  function register() {
    window.addEventListener('keydown', handleKeyDown);
  }

  /**
   * 이벤트 리스너 해제
   */
  function unregister() {
    window.removeEventListener('keydown', handleKeyDown);
  }

  // 컴포넌트 마운트 시 자동 등록
  onMounted(() => {
    register();
  });

  onUnmounted(() => {
    unregister();
  });

  return {
    shortcuts,
    isEnabled,
    enable,
    disable,
    register,
    unregister,
    nudgeSelected,
  };
}

/**
 * 단축키 표시용 헬퍼
 */
export function formatShortcut(shortcut: KeyboardShortcut): string {
  const parts: string[] = [];
  if (shortcut.ctrl) parts.push('Ctrl');
  if (shortcut.shift) parts.push('Shift');
  if (shortcut.alt) parts.push('Alt');

  // 특수 키 표시 변환
  let keyDisplay = shortcut.key;
  switch (shortcut.key) {
    case 'ArrowUp':
      keyDisplay = '↑';
      break;
    case 'ArrowDown':
      keyDisplay = '↓';
      break;
    case 'ArrowLeft':
      keyDisplay = '←';
      break;
    case 'ArrowRight':
      keyDisplay = '→';
      break;
    case 'Delete':
      keyDisplay = 'Del';
      break;
    case 'Escape':
      keyDisplay = 'Esc';
      break;
    case 'Backspace':
      keyDisplay = '⌫';
      break;
    default:
      keyDisplay = shortcut.key.toUpperCase();
  }

  parts.push(keyDisplay);
  return parts.join('+');
}
