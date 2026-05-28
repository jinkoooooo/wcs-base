import { reactive } from 'vue';

/**
 * 공통 feedback 타입
 */
export type AsrsFeedbackType = 'info' | 'success' | 'warning' | 'error';

export interface AsrsFeedbackState {
  type: AsrsFeedbackType;
  message: string;
}

/**
 * 공통 feedback 상태 composable
 */
export function useFeedback(initialType: AsrsFeedbackType = 'info') {
  const feedback = reactive<AsrsFeedbackState>({
    type: initialType,
    message: '',
  });

  function setFeedback(type: AsrsFeedbackType, message: string) {
    feedback.type = type;
    feedback.message = message;
  }

  function clearFeedback() {
    feedback.type = initialType;
    feedback.message = '';
  }

  return {
    feedback,
    setFeedback,
    clearFeedback,
  };
}
