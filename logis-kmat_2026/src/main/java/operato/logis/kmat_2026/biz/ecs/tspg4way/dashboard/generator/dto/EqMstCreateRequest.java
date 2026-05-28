package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 기본 설비 마스터 생성 요청 DTO
 * - tb_eq_mst
 * - tb_eq_plc_mst
 * 두 테이블 동시 생성용
 */
@Getter
@Setter
public class EqMstCreateRequest {

    /**
     * 설비 ID (tb_eq_mst.id)
     * 예: RACK_001, CV_001, CAR_001
     */
    private String id;

    /**
     * 설비 그룹 ID (FK: tb_eq_group_mst.id)
     */
    @JsonProperty("eqGroupId")
    private String eqGroupId;

    /**
     * 설비명
     */
    private String name;

    /**
     * 설비 타입
     * 11: RACK
     * 21: CONVEYOR/LIFTER
     * 22: SHUTTLE_CAR
     */
    private int type;

    /**
     * PLC ID (tb_eq_mst.plc_id / tb_eq_plc_mst.id)
     * 기본값은 설비 ID와 동일하게 사용 가능
     */
    @JsonProperty("plcId")
    private String plcId;

    /**
     * PLC 명
     * 기본값은 설비명과 동일하게 사용 가능
     */
    @JsonProperty("plcName")
    private String plcName;

    /**
     * PLC IP
     */
    @JsonProperty("plcIp")
    private String plcIp;

    /**
     * PLC Port
     */
    @JsonProperty("plcPort")
    private int plcPort;

    /**
     * PLC 인터페이스 타입
     * 예: MELSEC, S7, MODBUS, BINARY ...
     */
    @JsonProperty("plcIfType")
    private String plcIfType;

    /**
     * PLC 설비 타입
     * tb_eq_plc_mst.plc_eq_type
     */
    @JsonProperty("plcEqType")
    private int plcEqType;

    /**
     * PLC 연결 여부
     */
    @JsonProperty("connectYn")
    private boolean connectYn = false;

    /**
     * PLC 사용 여부
     */
    @JsonProperty("useYn")
    private boolean useYn = true;
}