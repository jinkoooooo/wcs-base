package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LifterStatusDto {

    /** tb_eq_cv_mst.id */
    private String equipmentId;

    /** tb_eq_cv_mst.eq_id */
    private String eqId;

    /** 현재 페이지의 layout id */
    private String layoutId;

    /** 현재 페이지 layout 기준 좌표 */
    private Double posX;
    private Double posY;

    /** 현재 층 */
    private Integer currentLevel;

    /** 목표 층 */
    private Integer targetLevel;

    /**
     * 0: READY
     * 2: RUN
     * 8: ERROR
     */
    private Integer status;

    /** 화물 적재 여부 */
    private Boolean hasCargo;

    /** 셔틀 적재 여부 */
    private Boolean hasShuttle;

    /** 이동 중 여부 */
    private Boolean moving;

    /** stopper open 여부 */
    private Boolean stopperOpen;

    /** PLC 명령 ID */
    private Integer plcCmdId;

    /** 에러 코드 */
    private String errorCode;

    /** 에러 메시지 */
    private String errorMessage;

    /** 현재 작업 존재 여부 */
    private Boolean hasActiveJob;

    private String currentOrderKey;
    private Integer currentOrderType;
    private Integer currentOrderStatus;
    private String currentBarcode;
    private String currentFromLoc;
    private String currentToLoc;

    private Long ts;

    public boolean isGoingUp() {
        return Boolean.TRUE.equals(moving)
                && targetLevel != null
                && currentLevel != null
                && targetLevel > currentLevel;
    }

    public boolean isGoingDown() {
        return Boolean.TRUE.equals(moving)
                && targetLevel != null
                && currentLevel != null
                && targetLevel < currentLevel;
    }

    public boolean isError() {
        return status != null && status == 8;
    }

    public boolean isWorking() {
        return Boolean.TRUE.equals(hasActiveJob);
    }
}