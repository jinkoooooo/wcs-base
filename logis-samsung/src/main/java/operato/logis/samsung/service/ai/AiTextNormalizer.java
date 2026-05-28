package operato.logis.samsung.service.ai;

import org.springframework.stereotype.Component;

@Component
public class AiTextNormalizer {

    public String normalize(String question) {
        if (question == null) {
            return "";
        }

        return question
                .replaceAll("[\\t\\n\\r]+", " ")
                .replaceAll("\\s{2,}", " ")
                .replace("입고/출고", "입출고")
                .replace("입고 출고", "입출고")
                .replace("알람", "에러")
                .trim();
    }
}