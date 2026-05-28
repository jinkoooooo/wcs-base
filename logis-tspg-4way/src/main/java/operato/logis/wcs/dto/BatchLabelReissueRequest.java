package operato.logis.wcs.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 박스 라벨 일괄 재발행 요청 DTO.
 *
 * boxIds  : 재발행 대상 박스 ID 목록 (필수, 최소 1건)
 * comment : 재발행 사유. 모두 첫 발행(PENDING && printCount=0) 묶음이면 면제,
 *           재발행 대상(SCANNED/PRINTED/DEPLETED)이 하나라도 섞이면 필수 (2~500자)
 */
@Getter
@Setter
public class BatchLabelReissueRequest {
    private List<String> boxIds;
    private String comment;
}