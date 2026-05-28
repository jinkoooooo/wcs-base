package operato.logis.asrs.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상품 일괄 저장 결과 응답 DTO.
 *
 * 부분 성공/실패를 허용하는 구조로 설계.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemMasterBulkSaveResult {

    /** 총 요청 건수 */
    private int totalCount;

    /** 성공 건수 */
    private int successCount;

    /** 실패 건수 */
    private int failCount;

    /** 행 단위 오류 목록 */
    private List<BulkErrorRow> errors = new ArrayList<BulkErrorRow>();

    /**
     * 실패 row 상세 정보.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkErrorRow {
        private int rowNo;
        private String itemCode;
        private String message;
    }
}