package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 공통 코드 → 명칭 변환용 Dictionary DTO
 * - name: 공통코드 부모 이름 (예: PICKING_STATUS, ORDER_TYPE 등)
 * - dictionary: { "01": "일반입고", "02": "반품입고", ... }
 * - key/value/parentName: SQL 조회용 임시 필드
 */
@Getter
@Setter
public class RmkDictionary {

    /** 공통코드 그룹명 (예: ORDER_STATUS) */
    private String name;

    /** 코드 → 명칭 매핑 */
    private Map<String, String> dictionary;

    /** SQL 조회용 임시 필드 — 부모 코드명 */
    private String parentName;

    /** SQL 조회용 임시 필드 — 코드 값 */
    private String key;

    /** SQL 조회용 임시 필드 — 코드 명칭 */
    private String value;
}