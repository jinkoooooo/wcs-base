package operato.logis.asrs.dto.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 전략 실행 요청 DTO.
 *
 * <p>
 * 상품등급 ↔ 로케이션등급 매칭 기반 재배치 task를 생성한다.
 * </p>
 */
@Getter
@Setter
@ToString
public class StrategyRunRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 영역 코드 */
    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    /** 실행 전략 코드(예: GRADE_MATCHING) */
    @JsonAlias({"strategyCode", "strategy_code"})
    private String strategyCode;

    /** task 최대 생성 건수 */
    @JsonAlias({"maxTaskCount", "max_task_count"})
    private Integer maxTaskCount;

    /** 미리보기 여부 */
    @JsonAlias({"previewOnly", "preview_only"})
    private Boolean previewOnly;
}