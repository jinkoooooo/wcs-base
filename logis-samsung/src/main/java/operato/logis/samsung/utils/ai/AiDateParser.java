package operato.logis.samsung.utils.ai;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiDateParser {

    private static final Pattern YYYY_MM_DD = Pattern.compile("(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})");
    private static final Pattern MM_DD = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");

    public LocalDate parse(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }

        if (question.contains("오늘") || question.contains("금일")) {
            return LocalDate.now();
        }

        if (question.contains("어제")) {
            return LocalDate.now().minusDays(1);
        }

        Matcher ymd = YYYY_MM_DD.matcher(question);
        if (ymd.find()) {
            return LocalDate.of(
                    Integer.parseInt(ymd.group(1)),
                    Integer.parseInt(ymd.group(2)),
                    Integer.parseInt(ymd.group(3))
            );
        }

        Matcher md = MM_DD.matcher(question);
        if (md.find()) {
            return LocalDate.of(
                    LocalDate.now().getYear(),
                    Integer.parseInt(md.group(1)),
                    Integer.parseInt(md.group(2))
            );
        }

        return null;
    }
}