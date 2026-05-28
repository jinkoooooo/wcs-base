package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.service.audit.AuditReason;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;

import static operato.logis.wcs.common.util.check.Validator.requireFound;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 박스 라벨 인쇄·재발행 책임 (StateWriter).
 *
 * 첫 발행과 재발행을 구분해 처리하며 감사 로그를 적재한다.
 *   - markPrinted        : PENDING && printCount=0 일 때만 첫 발행 (PENDING → PRINTED, count=1)
 *   - markReissued       : 상태 무관 + comment 필수, count += 1
 *   - markBoxesReissued  : N건 일괄 — 모두 첫 발행이면 comment 면제, 아니면 필수
 *   - markPalletPrinted  : 파렛트 단위 첫 발행
 *   - markPalletReissued : 파렛트 단위 재발행 (첫 발행/VOID 박스 제외)
 *
 * 다건 처리(markBoxesReissued/markPalletPrinted/markPalletReissued)는 박스를 건건 update 하지 않고
 * in-memory 로 상태만 바꾼 뒤 updateBatch 로 모아 실행한다. (3000건 update → 변경 컬럼별 batch 1~2회)
 */
@Service
@RequiredArgsConstructor
public class PalletBoxPrinter extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(PalletBoxPrinter.class);

    public static final String AUDIT_REASON_PRINTED = "LABEL_PRINTED";
    public static final String AUDIT_REASON_REPRINT_PREFIX = "LABEL_REPRINT: ";

    private final PalletBoxRepository boxRepository;
    private final PalletBoxFactory palletBoxFactory;

    /**
     * 첫 발행 — PENDING → PRINTED, print_count = 1, printed_at = now.
     * 이미 인쇄된 박스는 BOX_ALREADY_PRINTED — 재발행 엔드포인트를 사용해야 함.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsPalletBox markPrinted(String boxId) {
        TbWcsPalletBox box = palletBoxFactory.requireBox(boxId);
        palletBoxFactory.requireFinalized(box);

        // 이미 인쇄된 박스 거부
        int count = box.getPrintCount() == null ? 0 : box.getPrintCount();
        if (count > 0) {
            throw new ElidomRuntimeException("BOX_ALREADY_PRINTED",
                    "이미 인쇄된 박스입니다 (printCount=" + count + "). 재발행 엔드포인트를 사용하세요. boxId=" + boxId);
        }

        // 인쇄 전이 + 감사 적재 (before 는 감사 프록시가 자체 재조회)
        applyPrinted(box);
//        logger.info("[ Pallet ][ Print ] first issued - boxId={}, count=1", boxId);
        AuditReason.run(AUDIT_REASON_PRINTED,
                () -> this.queryManager.update(box, "boxStatus", "printedAt", "printCount"));
        return box;
    }

    /**
     * 재발행 — 상태 유지, print_count += 1, printed_at = now.
     * 감사 reason = "LABEL_REPRINT: {comment}". comment 는 컨트롤러가 사전 검증.
     * PRINTED/SCANNED/DEPLETED 어떤 박스도 사유만 있으면 허용.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsPalletBox markReissued(String boxId, String comment) {
        if (ValueUtil.isEmpty(comment)) {
            throw new ElidomRuntimeException("comment 는 필수입니다");
        }
        TbWcsPalletBox box = palletBoxFactory.requireBox(boxId);
        palletBoxFactory.requireFinalized(box);

        // 카운트 증분 + 시각 갱신 (before 는 감사 프록시가 자체 재조회)
        applyReissued(box);
        logger.info("[ Pallet ][ Print ] reissued - boxId={}, count={}", boxId, box.getPrintCount());
        AuditReason.run(AUDIT_REASON_REPRINT_PREFIX + comment.trim(),
                () -> this.queryManager.update(box, "printedAt", "printCount"));
        return box;
    }

    /**
     * 다수 박스 일괄 재발행 — 1개 트랜잭션으로 N건 처리.
     *
     * 정책:
     *   - 모두 첫 발행 대상(PENDING && printCount=0) 이면 comment 면제
     *   - 한 건이라도 재발행 대상이 포함되면 comment 필수
     *   - VOID 박스가 포함되면 예외
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> markBoxesReissued(List<String> boxIds, String comment) {
        requireFound(boxIds, "INVALID_PARAMETER", "박스 ID 목록이 비어있습니다.");

        // 1단계 — 박스 일괄 로드 + 첫 발행 묶음 여부 판정
        List<TbWcsPalletBox> loaded = new ArrayList<>(boxIds.size());
        boolean allFirstIssue = true;
        for (String id : boxIds) {
            TbWcsPalletBox b = palletBoxFactory.requireBox(id);
            palletBoxFactory.requireFinalized(b);
            BoxStatus s = BoxStatus.fromCode(b.getBoxStatus());

            if (s == BoxStatus.VOID) {
                throw new ElidomRuntimeException("INVALID_BOX_STATUS",
                        "폐기된 박스는 재발행할 수 없습니다. boxBarcode=" + b.getBoxBarcode());
            }

            int pc = b.getPrintCount() == null ? 0 : b.getPrintCount();
            if (!(pc == 0 && s == BoxStatus.PENDING)) {
                allFirstIssue = false;
            }
            loaded.add(b);
        }

        // 2단계 — comment 필수성 검증 (모두 첫 발행이면 면제)
        if (!allFirstIssue && ValueUtil.isEmpty(comment)) {
            throw new ElidomRuntimeException("INVALID_PARAMETER",
                    "재발행 사유(comment) 는 필수입니다. (2자 이상)");
        }

        // 3단계 — 각 박스 첫발행/재발행 분기를 in-memory 로 적용 (건건 update 호출 제거)
        // 변경 컬럼이 다르므로 첫 발행(boxStatus 포함)과 재발행을 별도 묶음으로 모은다.
        List<TbWcsPalletBox> firstIssued = new ArrayList<>();
        List<TbWcsPalletBox> reissued = new ArrayList<>();
        List<String> processed = new ArrayList<>(loaded.size());
        for (TbWcsPalletBox b : loaded) {
            int pc = b.getPrintCount() == null ? 0 : b.getPrintCount();
            BoxStatus s = BoxStatus.fromCode(b.getBoxStatus());
            if (pc == 0 && s == BoxStatus.PENDING) {
                applyPrinted(b);
                firstIssued.add(b);
            } else {
                applyReissued(b);
                reissued.add(b);
            }
            processed.add(b.getId());
        }

        // 두 묶음을 각각 batch update (총 update 호출 최대 2회, 감사 프록시는 PK 벌크로 before 1회 조회)
        if (!firstIssued.isEmpty()) {
            AuditReason.run(AUDIT_REASON_PRINTED,
                    () -> this.queryManager.updateBatch(firstIssued, "boxStatus", "printedAt", "printCount"));
        }
        if (!reissued.isEmpty()) {
            AuditReason.run(AUDIT_REASON_REPRINT_PREFIX + comment.trim(),
                    () -> this.queryManager.updateBatch(reissued, "printedAt", "printCount"));
        }

        logger.info("[ Pallet ][ Print ] batch reissued - count={}, allFirstIssue={}", processed.size(), allFirstIssue);
        return processed;
    }

    /**
     * 파렛트 단위 첫 발행 — printCount == 0 인 박스만 PRINTED 로 전이.
     * 미확정 박스 1건이라도 있으면 부분 인쇄 방지를 위해 early-fail.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markPalletPrinted(String palletBarcode) {
        List<TbWcsPalletBox> boxes = boxRepository.findByPalletBarcode(palletBarcode);

        // 사전 점검 — VOID 제외 모든 박스가 확정 상태여야 함
        for (TbWcsPalletBox b : boxes) {
            BoxStatus s = BoxStatus.fromCode(b.getBoxStatus());
            if (s == BoxStatus.VOID) continue;
            palletBoxFactory.requireFinalized(b);
        }

        // 첫 발행 대상만 in-memory 적용 후 batch 로 일괄 update (건건 markPrinted 호출 제거)
        List<TbWcsPalletBox> targets = new ArrayList<>();
        for (TbWcsPalletBox b : boxes) {
            int count = b.getPrintCount() == null ? 0 : b.getPrintCount();
            if (count == 0) {
                applyPrinted(b);
                targets.add(b);
            }
        }
        int n = targets.size();
        if (n > 0) {
            AuditReason.run(AUDIT_REASON_PRINTED,
                    () -> this.queryManager.updateBatch(targets, "boxStatus", "printedAt", "printCount"));
        }
        logger.info("[ Pallet ][ Print ] pallet printed - pallet={}, marked={}", palletBarcode, n);
        return n;
    }

    /**
     * 파렛트 단위 재발행 — 첫 발행 대상과 VOID 박스를 제외한 모든 박스에 일괄 재발행.
     * comment 필수.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markPalletReissued(String palletBarcode, String comment) {
        if (ValueUtil.isEmpty(comment)) {
            throw new ElidomRuntimeException("comment 는 필수입니다");
        }
        List<TbWcsPalletBox> boxes = boxRepository.findByPalletBarcode(palletBarcode);

        // 사전 점검 — VOID 제외 모든 박스가 확정 상태여야 함
        for (TbWcsPalletBox b : boxes) {
            BoxStatus s = BoxStatus.fromCode(b.getBoxStatus());
            if (s == BoxStatus.VOID) continue;
            palletBoxFactory.requireFinalized(b);
        }

        // 재발행 대상만 in-memory 적용 후 batch 로 일괄 update (건건 markReissued 호출 제거)
        List<TbWcsPalletBox> targets = new ArrayList<>();
        for (TbWcsPalletBox b : boxes) {
            BoxStatus s = BoxStatus.fromCode(b.getBoxStatus());
            int count = b.getPrintCount() == null ? 0 : b.getPrintCount();
            // 첫 발행 대상 — 별도 endpoint 로 처리
            if (s == BoxStatus.PENDING && count == 0) continue;
            // 폐기된 박스
            if (s == BoxStatus.VOID) continue;
            applyReissued(b);
            targets.add(b);
        }
        int n = targets.size();
        if (n > 0) {
            AuditReason.run(AUDIT_REASON_REPRINT_PREFIX + comment.trim(),
                    () -> this.queryManager.updateBatch(targets, "printedAt", "printCount"));
        }
        logger.info("[ Pallet ][ Print ] pallet reissued - pallet={}, reissued={}", palletBarcode, n);
        return n;
    }

    /** 첫 발행 in-memory 적용 — PENDING → PRINTED, printCount=1, printedAt=now. DB update 는 호출부가 모아 실행. */
    private void applyPrinted(TbWcsPalletBox box) {
        PalletBoxStatusTransition.transition(box, BoxStatus.PRINTED, "markPrinted");
        box.setPrintedAt(new Date());
        box.setPrintCount(1);
    }

    /** 재발행 in-memory 적용 — printCount += 1, printedAt=now. DB update 는 호출부가 모아 실행. */
    private void applyReissued(TbWcsPalletBox box) {
        int count = box.getPrintCount() == null ? 0 : box.getPrintCount();
        box.setPrintCount(count + 1);
        box.setPrintedAt(new Date());
    }
}