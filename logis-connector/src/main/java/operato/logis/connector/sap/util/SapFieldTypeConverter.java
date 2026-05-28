package operato.logis.connector.sap.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

public class SapFieldTypeConverter {

    public static Object convertValue(String type, Object value) {
        if (value == null || value.toString().isEmpty()) {
            if (type.equals("CHAR")) {
                return "";
            }
            else {
                return null;
            }
        }

        return switch (type) {
            case "CHAR", "UNIT" -> value.toString();
            case "NUMC", "INTEGER" -> (int) Double.parseDouble(value.toString());
            case "DEC", "QUAN" -> new BigDecimal(value.toString());
            case "DATS" -> convertToLocalDate(value);
            case "TIMS" -> convertToLocalTime(value);
            default -> value.toString(); // 문자열 그대로 반환
        };
    }

    private static LocalDate convertToLocalDate(Object value) {
        if (value instanceof Date date) {
            return Instant.ofEpochMilli(date.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
        return null;
    }

    private static LocalTime convertToLocalTime(Object value) {
        if (value instanceof Date date) {
            return Instant.ofEpochMilli(date.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime();
        }
        return null;
    }
}