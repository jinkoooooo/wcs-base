import { ref } from 'vue';
import { useMessage } from "@/hooks/web/useMessage";
import { ALLOWED_EXTENSIONS } from "@/views/lms/support/types";

export function useSupportFile() {
  const { notification } = useMessage();
  const FILE_BASE_URL = '/support-attachment'
  const selectedFiles = ref<any[]>([]);
  const deletedFileIds = ref<string[]>([]); // is_deleted === true인 파일
  const isDragging = ref(false); // 사용자의 Drag 동작 상태

  /**
   * (Util) 파일 → Base64 문자열 변환
   * @param file
   */
  const convertToBase64 = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = (error) => reject(error);
    })
  }

  /**
   * (Util) 첨부파일 리셋
   */
  const clearFiles = () => {
    selectedFiles.value = [];
    deletedFileIds.value = [];
  }

  /**
   * 파일 추가 공통로직
   * - 중복 파일 제외: 파일명, 파일크기가 동일한 파일
   * - 확장자 검증 : 허용된 확장자만 전달
   * @param files
   */
  const addFiles = (files: File[]) => {
    const validFiles: File[] = [];
    const MAX_TOTAL_SIZE = 50 * 1024 * 1024; // 50MB 제한

    let currentTotalSize = selectedFiles.value.reduce((acc, cur) => acc + cur.size, 0);

    for (const file of files) {
      const extension = file.name.split('.').pop()?.toLowerCase();
      if (!extension || !ALLOWED_EXTENSIONS.includes(extension)) {
        notification.error({
          message: "파일 형식 오류",
          description: `${ file.name }은(는) 허용되지 않는 확장자입니다.`
        })
        continue;
      }

      if (currentTotalSize + file.size > MAX_TOTAL_SIZE) {
        notification.error({
          message: "파일 용량 초과",
          description: `전체 파일 크기가 50MB를 초과하여 {$file.name}부터 추가할 수 없습니다`
        })
        break;
      }

      const isDuplicate = selectedFiles.value.some(f => f.name === file.name && f.size === file.size);
      if (isDuplicate) return;

      currentTotalSize += file.size;
      validFiles.push(file);
    }

    selectedFiles.value = [...selectedFiles.value, ...validFiles];
  }

  /**
   * 파일 목록에서 파일 삭제 (UI)
   * - 서버에 저장된 파일 : id로 관리
   * - 서버에 미저장된 파일 : selectedFiles 내 파일객체로 관리
   * @param idx
   */
  const removeFile = (idx: number) => {
    const target = selectedFiles.value[idx];
    if (target.isServerFile && target.id) {
      deletedFileIds.value.push(target.id);
    }
    selectedFiles.value.splice(idx, 1);
  }

  /**
   * Drag & Drop 파일 추가
   * @param event
   */
  const handleFileDrop = (event: DragEvent) => {
    isDragging.value = false;
    const files = event.dataTransfer?.files;
    if (files) {
      addFiles(Array.from(files));
    }
  }

  return {
    FILE_BASE_URL,
    selectedFiles,
    deletedFileIds,
    isDragging,
    convertToBase64,
    clearFiles,
    addFiles,
    removeFile,
    handleFileDrop
  }
}
