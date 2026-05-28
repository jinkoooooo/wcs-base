package operato.logis.samsung.dto.report.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 일별 리포트 원본 데이터 조회 요청 DTO
 */
@Data
public class DailyReportRawRequest extends DailyReportSearchRequest {

    /** BCR / SORTER / PALLETIZED */
    @JsonAlias({"processType", "process_type"})
    private String processType;
}