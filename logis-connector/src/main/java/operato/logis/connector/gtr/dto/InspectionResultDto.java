package operato.logis.connector.gtr.dto;

import lombok.Data;

import java.util.List;

@Data
public class InspectionResultDto {
    private String transactionId;
    private List<String> serialNumbers;
    private String zoneId;
    private String timestamp;
    private OverallResultDto overall;
    private List<SideResultDto> sides;

    @Data
    public static class OverallResultDto {
        private String result;
        private List<String> damageClasses;
        private String reason;
        private Double confidenceScore;
    }

    @Data
    public static class SideResultDto {
        private String side;
        private String result;
        private List<FindingDto> findings;
    }

    @Data
    public static class FindingDto {
        private String damageClass;
        private Double confidenceScore;
        private List<Integer> boundingBox;
        private String reason;
    }
}