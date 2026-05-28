package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 관리자 우회 처리 요청 DTO.
 * adminBypass = true 면 미스캔/미차감분을 일괄 처리한다.
 */
@Data
public class AdminBypassRequest {

    @JsonProperty("adminBypass")
    private Boolean adminBypass;

    /** null-safe 우회 여부 판정. */
    public boolean isAdminBypass() {
        return Boolean.TRUE.equals(adminBypass);
    }
}