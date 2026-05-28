package operato.logis.asrs.query.grade.model;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * SKU 일별 활동 집계 조회 전용 View DTO.
 *
 * <p>
 * tb_ac_item_activity_daily 와 센터/영역/품목 정보를 조인한 결과를 담는다.
 * 실제 DB 컬럼명 기준으로 partial_out_count, return_in_count 를 사용한다.
 * </p>
 */
@Getter
@Setter
@ToString
public class ItemActivityDailyView implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 집계 row id */
    private String itemActivityDailyId;

    /** 센터 row id */
    private String centerId;

    /** 센터 코드 */
    private String centerCode;

    /** 영역 row id */
    private String areaId;

    /** 영역 코드 */
    private String areaCode;

    /** 품목 row id */
    private String itemId;

    /** 품목 코드 */
    private String itemCode;

    /** 품목명 */
    private String itemName;

    /** 집계 일자 */
    private Date activityDate;

    /** 입고 건수 */
    private Integer inboundCount;

    /** 출고 건수 */
    private Integer outboundCount;

    /** 출고 수량 */
    private Integer outboundQty;

    /** 부분출고 건수 */
    private Integer partialOutCount;

    /** 재입고 건수 */
    private Integer returnInCount;

    /** 이동 건수 */
    private Integer moveCount;

    /** 평균 체류일수 */
    private Integer avgDwellDays;

    /** 명일 수요 수량 */
    private Integer demandTomorrowQty;

    /** 원시 점수 */
    private Integer scoreRaw;
}