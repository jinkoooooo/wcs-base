package operato.logis.wcs.service.impl.order.host;

/**
 * 호스트 주문 감사 이벤트 코드.
 * HostOrderAuditLogger 에서 detail_json 의 event_type 으로 기록된다.
 */
public enum HostOrderEvents {
    CREATED,
    TEST_PASSED,
    TEST_FAILED,
    TEST_CANCELLED,
    TEST_RETRY,
    CANCELLED,
    SCHEDULE_DUE,
    COMPLETED,
    TEST_ITEM_PASSED,
    TEST_ITEM_FAILED,
    TEST_ITEM_CANCELLED,
    TEST_ITEM_RETRY,
    BCR_RELEASED,
    BOX_ADJUSTED,
    QC_RELEASED;

    public String code() {
        return name();
    }
}
