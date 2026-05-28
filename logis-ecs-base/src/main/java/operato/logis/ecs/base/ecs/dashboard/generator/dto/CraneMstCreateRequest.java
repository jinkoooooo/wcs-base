package operato.logis.ecs.base.ecs.dashboard.generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CraneMstCreateRequest {

    /** Stacker Crane ID */
    @JsonProperty("id")
    private String id;

    /** 상위 기본 설비 ID (tb_eq_mst.id) */
    @JsonProperty("eqId")
    private String eqId;

    /**
     * 차량 타입
     * 예: CRANE, FOUR_WAY
     */
    private String type;

    /** 현재 위치 */
    private int row;
    private int bay;
    private int level;

    /** 연결 랙 정보 */
    @JsonProperty("rackId")
    private String rackId;

    @JsonProperty("rackEqId")
    private String rackEqId;

    /** 운행 가능 범위 */
    @JsonProperty("minRow")
    private int minRow;

    @JsonProperty("maxRow")
    private int maxRow;

    /** 자동 모드 여부 */
    @JsonProperty("autoYn")
    private boolean autoYn = true;

    /** 사용 여부 */
    @JsonProperty("useYn")
    private boolean useYn = true;

    /** 적재 여부 */
    @JsonProperty("cargoYn")
    private boolean cargoYn = false;

    /** 완료 여부 */
    @JsonProperty("completeYn")
    private boolean completeYn = false;

    /** PLC 명령 ID */
    @JsonProperty("plcCmdId")
    private int plcCmdId = 0;

    /** PLC 완료 명령 ID */
    @JsonProperty("plcCompCmdId")
    private int plcCompCmdId = 0;

    /** 배터리 상태 */
    @JsonProperty("batteryStatus")
    private int batteryStatus = 0;
}