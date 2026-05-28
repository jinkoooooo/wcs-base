package operato.logis.asrs.dto.request;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Access Point 저장 요청 DTO.
 */
@Getter
@Setter
@ToString
public class AccessPointSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 영역 코드 */
    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    /** 포인트 코드 */
    @JsonAlias({"pointCode", "point_code"})
    private String pointCode;

    /** 포인트 명 */
    @JsonAlias({"pointName", "point_name"})
    private String pointName;

    /** 포인트 타입: PORT / LIFT / PICK_FACE / BUFFER_POINT / WORK_POINT / CRANE_HOME */
    @JsonAlias({"pointType", "point_type"})
    private String pointType;

    /** Aisle */
    @JsonAlias({"aisleNo", "aisle_no"})
    private Integer aisleNo;

    /** Side */
    @JsonAlias({"sideCode", "side_code"})
    private String sideCode;

    /** Bay */
    @JsonAlias({"bayNo", "bay_no"})
    private Integer bayNo;

    /** Level */
    @JsonAlias({"levelNo", "level_no"})
    private Integer levelNo;

    /** Depth */
    @JsonAlias({"depthNo", "depth_no"})
    private Integer depthNo;

    /** 접근성 산정 사용 여부 */
    @JsonAlias({"useForSortYn", "use_for_sort_yn"})
    private String useForSortYn;

    /** 활성 여부 */
    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;

    /** 설명 */
    private String description;

    /** 목적 목록 */
    private List<AccessPointPurposeSaveRequest> purposes;
}