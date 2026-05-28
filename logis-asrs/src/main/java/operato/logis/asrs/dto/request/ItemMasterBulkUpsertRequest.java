package operato.logis.asrs.dto.request;

import java.util.List;

import lombok.Data;

/**
 * 상품마스터 일괄 저장 요청 DTO.
 *
 * 용도:
 * - 엑셀 붙여넣기 후 파싱된 row 목록을 통째로 서버에 전달
 * - itemCode 기준으로 upsert 처리
 */
@Data
public class ItemMasterBulkUpsertRequest {

    /** 일괄 저장 대상 row 목록 */
    private List<ItemMasterUpsertRequest> rows;
}