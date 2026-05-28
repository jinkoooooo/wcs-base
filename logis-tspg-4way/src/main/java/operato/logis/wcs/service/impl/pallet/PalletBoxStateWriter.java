package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.springframework.stereotype.Service;

/**
 * 박스 상태/수량 변경 단일 창구 (StateWriter).
 *
 * 호출자 트랜잭션에 합류한다 — 항상 PalletBoxEditor 의 편집 트랜잭션 내부에서 호출된다.
 * 박스 필드 직접 수정 + repository.update/delete 는 이 클래스로 모은다.
 */
@Service
@RequiredArgsConstructor
public class PalletBoxStateWriter {

    private final PalletBoxRepository boxRepository;

    /** total_qty 변경 + 잔량/픽업 동기화 (remaining=total, picked=0). */
    void changeTotalQty(TbWcsPalletBox box, int newTotalQty) {
        box.setTotalQty(newTotalQty);
        box.setRemainingQty(newTotalQty);
        box.setPickedQty(0);
        boxRepository.update(box, "totalQty", "remainingQty", "pickedQty");
    }

    /** DRAFT 박스 VOID 전이 (사유 태깅). */
    void voidBox(TbWcsPalletBox box, String tag) {
        PalletBoxStatusTransition.transition(box, BoxStatus.VOID, tag);
        boxRepository.update(box, "boxStatus");
    }

    /** DRAFT 박스 물리 삭제 — 폐기(VOID)로 남기지 않고 행 자체를 제거. */
    void deleteBox(TbWcsPalletBox box) {
        boxRepository.delete(box);
    }
}
