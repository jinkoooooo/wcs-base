package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 국검(NIA) 승인번호 처리 요청.
 *
 * 운영자가 dashboard2d 에서 셀(=stock) 을 선택한 뒤 2D 바코드를 스캔하여 호출.
 * 동일 stockId 에 동일 승인번호 재스캔은 멱등(no-op).
 */
@Getter
@Setter
public class NiaApprovalRequest {

    @JsonProperty("nia_approval_no")
    private String niaApprovalNo;

    @JsonProperty("comment")
    private String comment;
}
