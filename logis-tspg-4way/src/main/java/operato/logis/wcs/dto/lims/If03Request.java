package operato.logis.wcs.dto.lims;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * IF03 판정결과 보고 요청 (LIMS → WES).
 */
@Getter
@Setter
public class If03Request extends LimsBaseRequest {

    /** 결과 List. */
    private List<If03Result> results;
}