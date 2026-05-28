package operato.logis.wcs.dto.lims;

import lombok.Getter;
import lombok.Setter;

/**
 * IF01 자재마스터 동기화 요청 (LIMS → WES).
 */
@Getter
@Setter
public class If01Request extends LimsBaseRequest {

    /** 자재마스터 수정 사항 정보. */
    private If01Data data;
}