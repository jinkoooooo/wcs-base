package operato.logis.wcs.service.impl.pallet;

import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.entity.TbWcsPalletBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

/**
 * 박스 상태 전이 헬퍼.
 *
 * box_status 필드를 in-place 갱신하며 forward/backward 전이를 분리해 제공.
 * autoDepleteIfEmpty 는 remaining_qty 기준으로 판정 (출고 확정 시점에 호출).
 */
public final class PalletBoxStatusTransition {

    private static final Logger logger = LoggerFactory.getLogger(PalletBoxStatusTransition.class);

    private PalletBoxStatusTransition() {}

    /**
     * forward 전이.
     * 현재 상태가 next 로 전이 가능한지 BoxStatus 룰로 검증 후 변경.
     */
    public static void transition(TbWcsPalletBox box, BoxStatus next, String reason) {
        BoxStatus cur = BoxStatus.fromCode(box.getBoxStatus());

        // 전이 불가 케이스 — 이미 처리된 박스 등
        if (ValueUtil.isNotEmpty(cur) && !cur.canTransitionTo(next)) {
            logger.warn("[ Pallet ][ Box ] transition denied - boxId={}, from={}, to={}, reason={}",
                    box.getId(), cur.name(), next.name(), reason);
            throw new ElidomRuntimeException("INVALID_BOX_STATUS_TRANSITION",
                    String.format("이미 처리된 박스입니다. 박스(%s)의 현재 상태는 '%s' 이며, '%s' 상태로 전이할 수 없습니다.",
                            describeBoxBarcode(box), statusLabel(cur), statusLabel(next)));
        }

        // 동일 상태 재전이는 로그만 생략
        if (cur != next) {
            logger.info("[ Pallet ][ Box ] transition - boxId={}, from={}, to={}, reason={}",
                    box.getId(), ValueUtil.isEmpty(cur) ? "null" : cur.name(), next.name(), reason);
        }
        box.setBoxStatus(next.code());
    }

    /**
     * remaining_qty 가 0 이면 DEPLETED 자동 전이.
     * 이미 DEPLETED 인 박스는 noop.
     */
    public static void autoDepleteIfEmpty(TbWcsPalletBox box, String reason) {
        if (ValueUtil.isEmpty(box)) return;

        // remaining 0 이하면 소진 처리
        int remaining = ValueUtil.isEmpty(box.getRemainingQty()) ? 0 : box.getRemainingQty();
        BoxStatus cur = BoxStatus.fromCode(box.getBoxStatus());
        if (remaining <= 0 && cur != BoxStatus.DEPLETED) {
            transition(box, BoxStatus.DEPLETED, reason + "/auto-deplete");
        }
    }

    /**
     * backward 전이 — 검증 우회.
     * 재입고/시험 회수 흐름에서 SCANNED → PRINTED 복원 등에 사용.
     */
    public static void restore(TbWcsPalletBox box, BoxStatus next, String reason) {
        BoxStatus cur = BoxStatus.fromCode(box.getBoxStatus());
        if (cur != next) {
            logger.info("[ Pallet ][ Box ] restore - boxId={}, from={}, to={}, reason={}",
                    box.getId(), ValueUtil.isEmpty(cur) ? "null" : cur.name(), next.name(), reason);
        }
        box.setBoxStatus(next.code());
    }

    /** 박스 식별자 — 바코드 우선, 없으면 ID. */
    private static String describeBoxBarcode(TbWcsPalletBox b) {
        if (ValueUtil.isEmpty(b)) return "알 수 없음";
        return ValueUtil.isNotEmpty(b.getBoxBarcode()) ? b.getBoxBarcode() : b.getId();
    }

    /** 상태 코드의 사용자 표시 라벨. */
    private static String statusLabel(BoxStatus s) {
        if (ValueUtil.isEmpty(s)) return "알 수 없음";
        return switch (s) {
            case DRAFT    -> "미확정 (일련번호 미발번)";
            case PENDING  -> "확정됨 (라벨 인쇄 대기)";
            case PRINTED  -> "라벨 인쇄됨";
            case SCANNED  -> "입고 스캔 완료 (재고 있음)";
            case DEPLETED -> "출고 완료 (박스 비움)";
            case VOID     -> "폐기";
        };
    }
}
