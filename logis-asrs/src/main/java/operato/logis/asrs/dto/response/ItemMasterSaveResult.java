package operato.logis.asrs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상품 저장 결과 응답 DTO.
 *
 * create / update / active toggle 에서 공통 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemMasterSaveResult {

    /** 저장 대상 상품 코드 */
    private String itemCode;

    /** 내부 PK */
    private String id;

    /** 처리 유형: CREATED / UPDATED / ACTIVE_CHANGED */
    private String action;

    /** 처리 메시지 */
    private String message;
}