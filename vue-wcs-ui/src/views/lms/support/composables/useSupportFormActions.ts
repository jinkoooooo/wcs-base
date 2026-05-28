import { ref } from 'vue';
import { useMessage } from "@/hooks/web/useMessage";
import { FileInfo } from "@/views/lms/support/types";
import { getCommonGetApi, updateList } from "@/api/common/api";
import { defHttp } from '/@/utils/http/axios';
import { SupportApi } from "@/views/lms/support/api/support";

// TODO: 공통로직 분리
// todo: 상태변경 제한 : is_deleted true일 때, 다른 상태로 변경
export function useSupportFormActions(pageRef, selectedFiles, deletedFileIds, convertToBase64, clearFiles, isAdminPage) {
  const { notification } = useMessage();
  const isSubmitting = ref(false);
  const SAVE_FORM_URL = '/support-request/update_one';

  /**
   * 유지보수 요청 폼 저장 (생성/수정)
   * 1. 권한 제한 : 진행 상태에서는 관리자만 변경 가능
   * 2. '담당자 지정' 상태 조건 : '접수자' 필수
   * 3. 자동 상태 변경 : 알수없음(0), 신규(''), 임시저장(1) → 저장(2)
   * 4. 카테고리 미 지정시 기본값 부여
   * 5. cud_flag 부여
   * 6. 첨부파일 추가
   * @param formData 유효성 검사 진행된 폼 데이터
   * @param api 폼 제어함수
   */
  const onSubmit = async (formData, api: any) => {
    const rawData = formData || api?.getFieldsValue();
    const record = JSON.parse(JSON.stringify(rawData)); // 완전한 순수 객체화
    // const record = formData || api?.getFieldsValue(); // NOTE: 일부 컬럼만 수정할 때는 api 활용 검토

    if (!isAdminPage.value && record.status && record.status !== '1' && record.status !== '0') {
      return notification.warn({
        message: 'INFO',
        description: '등록된 요청은 관리자만 변경 가능합니다',
        duration: 2,
      });
    }

    const NEED_RECEIVER = new Set(['3', '4', '5']); //3: 담당자 지정, 4: 작업수행, 5: 완료
    if (
      NEED_RECEIVER.has(record.status) &&
      (record.receiver_id == null || record.receiver_id === '')
    ) {
      return notification.warn({
        message: 'INFO',
        description: '담당자 지정/작업 수행/완료 상태에서 접수자를 반드시 선택하세요',
        duration: 3,
      });
    }

    // TODO: 공통코드 SUPPORT_STATUS 사용
    record.status = [undefined, '', '0', '1'].includes(record.status) ? '2' : record.status ?? '2';

    // TODO: 공통코드 SUPPORT_CATEGORY 사용
    if (record.category === ' ' || record.category === '-') {
      record.category = '7';
    }

    const cudFlag = !!record.support_id ? 'u' : 'c';

    if (isSubmitting.value) {
      return notification.warn({
        message: 'INFO',
        description: '전송중입니다',
        duration: 2,
      });
    }

    const fileDataList: FileInfo[] = [];
    for (let file of selectedFiles.value) {
      if (!file.isServerFile) {
        const base64Str = await convertToBase64(file);
        fileDataList.push({
          file_name: file.name,
          file_type: file.type,
          base64_data: base64Str.split(',')[1], // remove 'data:image/png;base64,'
          size: file.size
        })
      }
    }

    const payload = {
      ...record,
      cud_flag_: cudFlag,
      files: fileDataList,
      deleted_file_ids: deletedFileIds.value
    };

    if (!payload || payload.length === 0) {
      return notification.error({
        message: 'INFO',
        description: '수정 또는 추가된 데이터가 없습니다',
        duration: 2,
      });
    }
    if (!SAVE_FORM_URL) {
      notification.error({
        message: '오류',
        description: '저장 URL을 찾을 수 없습니다.',
        duration: 2,
      });
      return;
    }

    try {
      isSubmitting.value = true;
      // const response = await getCommonPostApi(SAVE_FORM_URL, payload)
      // const response = await updateList(SAVE_FORM_URL, payload);
      const response = await SupportApi.saveSupport(payload)
      if (response) {
        if (record.status == '2') {
          notification.info({
            message: 'SUCCESS',
            description: '요청이 접수되었습니다',
            duration: 2,
          });
        } else {
          notification.info({
            message: 'SUCCESS',
            description: '요청이 업데이트 되었습니다',
            duration: 2,
          });
        }

        clearFiles();
        return response;
      }
    } catch (e) {
      // console.error('[onSubmit] e =', e);
      throw e;
    } finally {
      isSubmitting.value = false;
    }
  }

  /**
   * 유지보수 요청 폼 임시저장
   * 1. 폼 유효성 검사
   * 2. status 유효성 검사 : status가 없거나, 임시저장일 때 임시저장 가능
   * 3. 첨부파일 추가
   * 4. cud_flag 부여
   * @param form 작성 폼 객체 (데이터 및 api 포함)
   */
  const onTempSave = async (form: any) => {
    try {
      const record = await form.validate();

      // TODO: 공통코드 SUPPORT_STATUS 사용
      const currentStatus = record.status;
      if ([undefined, '', '0', '1'].includes(currentStatus)) {
        record.status = '1';
      } else {
        return notification.warn({
          message: 'INFO',
          description: '등록된 요청은 임시저장할 수 없습니다',
          duration: 2,
        });
      }

      isSubmitting.value = true; // 전송 시작
      const fileDataList: FileInfo[] = [];
      for (let file of selectedFiles.value) {
        if (!file.isServerFile) {
          const base64Str = await convertToBase64(file);
          fileDataList.push({
            file_name: file.name,
            file_type: file.type,
            base64_data: base64Str.split(',')[1], // remove 'data:image/png;base64,'
            size: file.size
          })
        }
      }

      const cudFlag = !!record.support_id ? 'u' : 'c';
      const payload = {
        ...record,
        cud_flag_: cudFlag,
        files: fileDataList,
        deleted_file_ids: deletedFileIds.value
      };

      if (!payload || payload.length === 0) {
        return notification.error({
          message: 'INFO',
          description: '수정 또는 추가된 데이터가 없습니다',
          duration: 2,
        });
      }

      // const response = await updateList(SAVE_FORM_URL, payload);
      const response = await defHttp.post({
        url: SAVE_FORM_URL,
        params: payload  // [payload] 가 아닌 payload 그대로 전송
      });

      if (response) {
        notification.info({
          message: 'SUCCESS',
          description: '요청을 임시저장하였습니다',
          duration: 2,
        });
      }
      clearFiles();
      return response;

    } catch (e) {
      // console.warn('[onTempSave] error = ', e);
      throw e;
    } finally {
      isSubmitting.value = false;
    }
  }

  // 답변 생성 후, 폼 상태 '완료(5)'로 업데이트
  const onSubmitCompletedStatus = async (formData: any, isCompleted: boolean) => {
    if (!isCompleted) return;

    let record = { ...formData };

    if (!record?.support_id)
      return notification.error({
        message: 'ERROR',
        description: 'support_id가 없습니다.',
        duration: 2,
      });

    record.status = '5'; // TODO: 공통코드 활용

    // 검증
    // 1. 진행 상태에서는 관리자만 변경 가능
    if (!isAdminPage.value && !['0', '1'].includes(record.status)) {
      return notification.warn({
        message: 'INFO',
        description: '등록된 요청은 관리자만 변경 가능합니다',
        duration: 2,
      });
    }

    const payload = { ...record, cud_flag_: 'u' };

    if (payload.length === 0) {
      return notification.warn({
        message: 'INFO',
        description: '수정 또는 추가된 데이터가 없습니다',
        duration: 2,
      });
    }

    if (!SAVE_FORM_URL) {
      return notification.error({
        message: '오류',
        description: '저장 URL을 찾을 수 없습니다.2',
        duration: 2,
      });
    }

    //TODO: 파일데이터 추가 여부 확인 필요

    try {
      isSubmitting.value = true;
      const response = await updateList(SAVE_FORM_URL, payload);

      if (response) {
        // 단건 조회 후, 상태, 접수자 업데이트
        const newRequestFormData = await getCommonGetApi(`/support-request/${ encodeURIComponent(record.support_id) }`, null,);
        notification.success({ message: '완료 처리', description: '요청이 성공적으로 완료되었습니다.' });
        return Array.isArray(newRequestFormData) ? newRequestFormData[0] : newRequestFormData;
      }
    } catch (e) {
      // console.warn('[handleCompleted] e =', e);
      notification.error({ message: 'ERROR', description: e.message || e, duration: 2 });
      return null;
    } finally {
      isSubmitting.value = false;
    }
  }

  return { isSubmitting, SAVE_FORM_URL, onSubmit, onTempSave, onSubmitCompletedStatus }
}
