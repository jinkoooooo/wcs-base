package operato.logis.samsung.dto.mw;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BoxInfoRequest {
    private String dateMode;   // day | range
    private String date;       // 하루치 조회
    private String startDate;  // 기간 조회 시작일
    private String endDate;    // 기간 조회 종료일
    private String barcode;
    private String serial;
    private String rejectType;
}