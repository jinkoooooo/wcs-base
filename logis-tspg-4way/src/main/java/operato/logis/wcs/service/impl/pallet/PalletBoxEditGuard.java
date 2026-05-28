package operato.logis.wcs.service.impl.pallet;

import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.entity.TbWcsPalletBox;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.Objects;

/**
 * 입고 전 박스 편집 가드 (Validator).
 *
 * DRAFT + 미인쇄 + 미확정(box_seq 미부여) 박스만 수정/삭제 가능하다는 규칙을 검증.
 * 모든 메서드는 위반 시 ElidomRuntimeException 으로 트랜잭션 롤백을 유도한다.
 */
public final class PalletBoxEditGuard {

    private PalletBoxEditGuard() {}

    /** DRAFT + 미인쇄 + 미확정 박스만 편집 가능. */
    static void requireEditable(TbWcsPalletBox box) {
        requireDraft(box);
        requireNotPrinted(box);
        requireNotFinalized(box);
    }

    /** 박스가 요청 파렛트에 속하는지 검증 (배치 편집의 교차 파렛트 차단). */
    static void requireSamePallet(TbWcsPalletBox box, String palletBarcode) {
        if (!Objects.equals(box.getPalletBarcode(), palletBarcode)) {
            throw new ElidomRuntimeException("PALLET_MISMATCH",
                    "박스의 파렛트가 일치하지 않습니다. (요청: %s, 박스: %s, boxId=%s)"
                            .formatted(palletBarcode, box.getPalletBarcode(), box.getId()));
        }
    }

    /** DRAFT 상태 검증. */
    private static void requireDraft(TbWcsPalletBox box) {
        if (BoxStatus.fromCode(box.getBoxStatus()) != BoxStatus.DRAFT) {
            throw new ElidomRuntimeException("BOX_NOT_DRAFT",
                    "확정 전(DRAFT) 박스만 수정/삭제할 수 있습니다. (박스: %s)".formatted(box.getBoxBarcode()));
        }
    }

    /** 미인쇄 검증 — print_count > 0 이면 거부. */
    private static void requireNotPrinted(TbWcsPalletBox box) {
        int pc = box.getPrintCount() == null ? 0 : box.getPrintCount();
        if (pc > 0) {
            throw new ElidomRuntimeException("BOX_ALREADY_PRINTED",
                    "이미 인쇄된 박스는 수정/삭제할 수 없습니다. (박스: " + box.getBoxBarcode() + ", printCount=" + pc + ")");
        }
    }

    /** 미확정 검증 — box_seq 가 부여됐으면 거부. */
    private static void requireNotFinalized(TbWcsPalletBox box) {
        if (ValueUtil.isNotEmpty(box.getBoxSeq()) && box.getBoxSeq() != 0) {
            throw new ElidomRuntimeException("BOX_ALREADY_FINALIZED",
                    "이미 확정된 박스는 수정/삭제할 수 없습니다. (boxBarcode=%s, boxSeq=%s)"
                            .formatted(box.getBoxBarcode(), box.getBoxSeq()));
        }
    }
}
