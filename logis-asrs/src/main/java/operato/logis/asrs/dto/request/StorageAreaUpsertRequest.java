package operato.logis.asrs.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/**
 * 아레아 등록/수정 요청 DTO.
 *
 * <p>
 * 화면 입력은 code 기준으로 받고,
 * 저장 시에는 내부에서 FK id 로 resolve 한다.
 * </p>
 */
@Data
public class StorageAreaUpsertRequest {

    @JsonAlias({"centerCode", "center_code"})
    private String centerCode;

    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    @JsonAlias({"areaName", "area_name"})
    private String areaName;

    @JsonAlias({"areaType", "area_type"})
    private String areaType;

    @JsonAlias({"operationProfileCode", "operation_profile_code"})
    private String operationProfileCode;

    @JsonAlias({"description"})
    private String description;

    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;
}