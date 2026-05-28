package operato.logis.samsung.service.ai;

import operato.logis.samsung.consts.ai.AiIntentType;
import operato.logis.samsung.dto.ai.internal.AiParsedCommand;
import operato.logis.samsung.utils.ai.AiRegexUtils;
import org.springframework.stereotype.Component;

@Component
public class AiIntentClassifier {

    public AiIntentType classify(AiParsedCommand command) {
        String q = command.getLowerQuestion();

        if ((command.getErrorCode() != null)
                && AiRegexUtils.containsAny(q, "에러", "조치", "가이드", "원인", "의미")) {
            return AiIntentType.ERROR_CODE_GUIDE;
        }

        if ((command.getTargetDate() != null || AiRegexUtils.containsAny(q, "오늘", "어제", "금일"))
                && AiRegexUtils.containsAny(q, "에러", "장애", "현황", "미조치")) {
            return AiIntentType.DAILY_ERROR_STATUS_GUIDE;
        }

        if (command.getSkuCode() != null || AiRegexUtils.containsAny(q, "sku", "상품코드", "item code")) {
            return AiIntentType.SKU_IO_STATUS;
        }

        if (command.getSerialNo() != null || AiRegexUtils.containsAny(q, "시리얼", "serial", "상품고유번호", "진행 상황", "진행상황")) {
            return AiIntentType.SERIAL_PROGRESS_STATUS;
        }

        if (AiRegexUtils.containsAny(q, "입출고", "입고", "출고") && AiRegexUtils.containsAny(q, "현황", "상황", "상태")) {
            return AiIntentType.CURRENT_IO_STATUS;
        }

        return AiIntentType.UNKNOWN;
    }
}