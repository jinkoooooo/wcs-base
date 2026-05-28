package operato.logis.asrs.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ItemInboundPolicyResult {

    private String areaId;
    private String areaCode;

    private String itemId;
    private String itemCode;
    private String itemName;

    /** Lot 입력칸 활성 여부 */
    private boolean lotEnabled;

    /** Lot 필수 여부 */
    private boolean lotRequired;

    /** 원본 정책 확인용 */
    private boolean lotControlRequired;
    private boolean expiryControlRequired;
    private boolean serialControlRequired;
}