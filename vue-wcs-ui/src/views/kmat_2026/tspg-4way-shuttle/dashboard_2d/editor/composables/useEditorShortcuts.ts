import { onMounted, onUnmounted, ref } from 'vue';

interface ShortcutOptions {
  onSave?: () => void;
  onUndo?: () => void;
  onRedo?: () => void;
  onCopy?: () => void;
  onPaste?: () => void;
  onDelete?: () => void;
  onNudge?: (dx: number, dy: number) => void; // 화살표 키 이동
  onSpaceDown?: () => void; // 스페이스바 누름 (이동 툴)
  onSpaceUp?: () => void;   // 스페이스바 뗌
}

export function useEditorShortcuts(options: ShortcutOptions) {
  const spaceDown = ref(false);

  const handleKeyDown = (e: KeyboardEvent) => {
    // 💡 입력창(Input, Textarea)에서 타이핑 중일 때는 단축키 작동 방지
    const target = e.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') {
      return;
    }

    const isCmd = e.ctrlKey || e.metaKey; // Windows(Ctrl) / Mac(Cmd) 호환

    // 1. Space (화면 이동 툴)
    if (e.code === 'Space' && !spaceDown.value) {
      spaceDown.value = true;
      e.preventDefault();
      options.onSpaceDown?.();
      return;
    }

    // 2. 저장 (Ctrl + S)
    if (isCmd && (e.key === 's' || e.key === 'S')) {
      e.preventDefault();
      options.onSave?.();
      return;
    }

    // 3. 실행 취소 / 다시 실행 (Ctrl + Z / Ctrl + Shift + Z)
    if (isCmd && (e.key === 'z' || e.key === 'Z')) {
      e.preventDefault();
      if (e.shiftKey) {
        options.onRedo?.();
      } else {
        options.onUndo?.();
      }
      return;
    }

    // 4. 복사 / 붙여넣기 (Ctrl + C / Ctrl + V)
    if (isCmd && (e.key === 'c' || e.key === 'C')) {
      e.preventDefault();
      options.onCopy?.();
      return;
    }
    if (isCmd && (e.key === 'v' || e.key === 'V')) {
      e.preventDefault();
      options.onPaste?.();
      return;
    }

    // 5. 삭제 (Delete / Backspace)
    if (e.key === 'Delete' || e.key === 'Backspace') {
      e.preventDefault();
      options.onDelete?.();
      return;
    }

    // 6. 방향키 이동 (Arrow Keys + Shift)
    const step = e.shiftKey ? 10 : 1; // Shift 누르면 10칸 이동
    if (e.key === 'ArrowLeft') { e.preventDefault(); options.onNudge?.(-step, 0); return; }
    if (e.key === 'ArrowRight') { e.preventDefault(); options.onNudge?.(step, 0); return; }
    if (e.key === 'ArrowUp') { e.preventDefault(); options.onNudge?.(0, step); return; }
    if (e.key === 'ArrowDown') { e.preventDefault(); options.onNudge?.(0, -step); return; }
  };

  const handleKeyUp = (e: KeyboardEvent) => {
    if (e.code === 'Space') {
      spaceDown.value = false;
      options.onSpaceUp?.();
    }
  };

  onMounted(() => {
    window.addEventListener('keydown', handleKeyDown, { passive: false });
    window.addEventListener('keyup', handleKeyUp);
  });

  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeyDown);
    window.removeEventListener('keyup', handleKeyUp);
  });

  return { spaceDown };
}
