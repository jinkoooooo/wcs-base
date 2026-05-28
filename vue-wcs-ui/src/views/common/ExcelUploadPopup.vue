<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="titleRef"
    @ok="handleFileUpload"
    :useWrapper="false"
    :scroll="false"
    :style="computedBodyStyle"
    :bodyStyle="bodyStyleRef"
  >
    <div id="upload-container">
      <!-- 파일 업로드 영역 -->
      <div class="input-container">
        <label>{{ t('label.file_upload') }}</label>
        <input ref="fileInputRef" type="file" accept=".xlsx, .xls, .csv, .ods" />
      </div>

      <!-- 샘플 다운로드 버튼 -->
      <div class="input-container">
        <button @click="downloadSample">{{ t('button.download_sample') }}</button>
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { getCommonPostApi } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import * as XLSX from 'xlsx';
  import { sanitizeAndParseNumber, formatToIsoDate, formatFromYnToBoolean } from './utils';

  const { t } = useI18n();
  const { notification } = useMessage();
  const titleRef = ref(t('label.upload_excel'));
  const fileTypeRef = ref();
  const sampleFileRef = ref();
  const uploadUrlRef = ref();
  const columnSchemaRef = ref();

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    fileTypeRef.value = data.fileType;
    sampleFileRef.value = data.sampleFile;
    uploadUrlRef.value = data.uploadUrl;
    columnSchemaRef.value = data.columnSchema;
  });

  type ColumnType = 'string' | 'number' | 'date' | 'boolean';

  interface ColumnSchema {
    field: string; // 객체 필드명
    type: ColumnType; // 데이터 타입
    index: number; // 엑셀 열 인덱스 (0부터 시작)
  }

  const fileInputRef = ref<HTMLInputElement | null>(null);
  const computedBodyStyle = computed(() => ({
    overflow: 'visible',
    width: '600px',
  }));

  const bodyStyleRef = computed(() => ({
    height: '40vh',
    justifyContent: 'space-between',
  }));

  /**
   * 파일 업로드 핸들러
   */
  async function handleFileUpload() {
    let file = fileInputRef.value?.files?.[0];
    if (!file) {
      closeModal();
      return;
    }

    // 확장자 체크
    const fileExtension = file.name.split('.').pop()?.toLowerCase();

    if (!['xlsx', 'xls', 'csv', 'ods'].includes(fileExtension || '')) {
      notification.error({
        message: '업로드 에러',
        description: '지원하지 않는 파일 형식입니다. (xlsx, xls, csv, ods만 지원)',
        duration: 2,
      });
      return;
    }

    // 파일 읽기
    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const data = e.target?.result;
        const workbook = XLSX.read(data, { type: 'array' });
        const rows = parseExcelToJson(workbook);

        const transformedData = transformRowsToGenericObjects(rows, columnSchemaRef.value);

        await saveToDatabase(transformedData); // DB 저장 후 알림
      } catch (error) {
        notification.error({
          message: '파일 처리 에러',
          description: '파일을 처리하는 도중 문제가 발생했습니다.',
          duration: 2,
        });
      }
    };

    reader.readAsArrayBuffer(file);
  }

  /**
   * 엑셀 데이터를 JSON으로 변환
   */
  function parseExcelToJson(workbook: XLSX.WorkBook): any[] {
    const sheetName = workbook.SheetNames[0];
    const worksheet = workbook.Sheets[sheetName];

    // 첫 번째 행을 헤더로 지정하고 두 번째 행부터 데이터를 가져오도록 옵션 설정
    // 모든 행을 읽고 빈 배열이나 값이 없는 행을 제외
    return (XLSX.utils.sheet_to_json(worksheet, { header: 1 }) as any[][])
      .slice(1) // 첫 번째 행(헤더) 제외
      .filter(
        (row: any[]) =>
          row.length > 0 && row.some((cell) => cell !== null && cell !== undefined && cell !== ''),
      );
  }

  /**
   * JSON 데이터를 인터페이스에 맞게 변환
   */
  function transformRowsToGenericObjects(rows: any[], schema: ColumnSchema[]): any[] {
    return rows.map((row) => {
      const obj: Record<string, any> = {};
      schema.forEach((col) => {
        const rawValue = row[col.index];
        if (col.type === 'date') {
          obj[col.field] = formatToIsoDate(rawValue);
        } else if (col.type === 'number') {
          obj[col.field] = sanitizeAndParseNumber(rawValue);
        } else if (col.type === 'boolean') {
          obj[col.field] = formatFromYnToBoolean(rawValue);
        } else {
          obj[col.field] = rawValue ?? '';
        }
      });
      return obj;
    });
  }

  /**
   * DB 저장 로직 (가상 API)
   */
  async function saveToDatabase(data) {
    try {
      let response = await getCommonPostApi(uploadUrlRef.value, data);
      if (response === 'success') {
        notification.success({
          message: 'DB 저장 성공',
          description: '모든 데이터를 성공적으로 저장했습니다.',
          duration: 2,
        });
      }
    } catch (error) {
      notification.error({
        message: 'DB 저장 오류',
        description: 'DB 저장 도중 문제가 발생했습니다.',
        duration: 2,
      });
    } finally {
      closeModal();
    }
  }

  /**
   * 샘플 파일 다운로드
   */
  function downloadSample() {
    const fileType = fileTypeRef.value;
    const fileName = `${sampleFileRef.value}`;

    // 백엔드에서 파일을 직접 응답해주는 경로
    const serverUrl = window.location.hostname;
    const downloadUrl = `http://${serverUrl}:9500/rest/resource/get/${fileType}/${fileName}`;

    // <a> 태그를 이용한 다운로드
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.setAttribute('download', fileName);
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }
</script>

<style lang="less" scoped>
  #upload-container {
    padding: 1rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .input-container {
    display: flex;
    flex-direction: column;
  }

  input[type='file'] {
    margin-top: 0.5rem;
  }

  button {
    padding: 0.5rem 1rem;
    font-size: 1rem;
    background-color: #007bff;
    color: #ffffff;
    border: none;
    border-radius: 5px;
    cursor: pointer;
  }

  button:hover {
    background-color: #0056b3;
  }
</style>
