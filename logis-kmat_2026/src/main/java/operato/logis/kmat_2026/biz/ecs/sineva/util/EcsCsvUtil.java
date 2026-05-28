package operato.logis.kmat_2026.biz.ecs.sineva.util;

import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EcsCsvUtil {

    /**
     * CSV 문자열을 List<String> 으로 변환
     */
    public static List<String> csvToStringList(String csvString) {
        if (ValueUtil.isEmpty(csvString) || csvString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(csvString.split(",")));
    }

    /**
     * CSV 문자열을 List<Integer> 으로 변환
     */
    public static List<Integer> csvToIntegerList(String csvString) {
        if (ValueUtil.isEmpty(csvString) || csvString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(
                Arrays.stream(csvString.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList()));
    }

    /**
     * List<String>을 CSV 문자열로 변환
     */
    public static String stringListToCsv(List<String> list) {
        if (ValueUtil.isEmpty(list)) {
            return "";
        }
        return String.join(",", list);
    }

    /**
     * List<Integer>를 CSV 문자열로 변환
     */
    public static String integerListToCsv(List<Integer> list) {
        if (ValueUtil.isEmpty(list)) {
            return "";
        }
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
