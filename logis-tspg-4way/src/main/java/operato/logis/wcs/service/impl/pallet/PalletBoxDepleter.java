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
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 박스 소진(DEPLETED) 책임 (StateWriter).
 *
 * 시험 사이클의 [확정 미리보기] 에서 사용자가 명시한 미스캔 박스 + 전량 채취 박스를
 * 한 트랜잭션으로 DEPLETED 전이한다. ReinboundIssuer 가 시험 회수 후 호출.
 *
 * 전이 규칙:
 *   - SCANNED / PRINTED → DEPLETED (전이 허용)
 *   - 이미 DEPLETED / VOID → skip (idempotent)
 *   - PENDING / DRAFT 등 인쇄 전 상태 → 예외 (비정상 흐름)
 */
@Service
@RequiredArgsConstructor
public class PalletBoxDepleter extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(PalletBoxDepleter.class);

    public static final String AUDIT_REASON_DEPLETED_PREFIX = "DEPLETED: ";

    private final PalletBoxRepository boxRepository;
    private final PalletBoxFactory palletBoxFactory;

    /**
     * 다수 박스 일괄 소진 처리.
     *
     * @return 실제로 DEPLETED 전이된 박스 ID 목록.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> markBoxesDepleted(List<String> boxIds, String reason) {
        if (ValueUtil.isEmpty(boxIds)) return new ArrayList<>();

        List<String> processed = new ArrayList<>(boxIds.size());
        String auditReason = AUDIT_REASON_DEPLETED_PREFIX + (ValueUtil.isEmpty(reason) ? "manual" : reason);

        for (String id : boxIds) {
            TbWcsPalletBox box = palletBoxFactory.requireBox(id);
            BoxStatus s = BoxStatus.fromCode(box.getBoxStatus());

            // idempotent skip
            if (s == BoxStatus.DEPLETED || s == BoxStatus.VOID) continue;

            // 인쇄 전 박스는 비정상 흐름 — 거부
            if (s != BoxStatus.SCANNED && s != BoxStatus.PRINTED) {
                throw new ElidomRuntimeException("INVALID_BOX_STATUS",
                        "DEPLETED 전이는 SCANNED 또는 PRINTED 상태에서만 가능합니다. boxId=%s, status=%s"
                                .formatted(id, s));
            }

            // DEPLETED 전이 + 잔량/픽업 0 정리 (before 는 감사 프록시가 자체 재조회)
            PalletBoxStatusTransition.transition(box, BoxStatus.DEPLETED, "markBoxesDepleted");
            box.setRemainingQty(0);
            box.setPickedQty(0);
            AuditReason.run(auditReason,
                    () -> this.queryManager.update(box, "boxStatus", "remainingQty", "pickedQty"));
            processed.add(id);
        }
        logger.info("[ Pallet ][ Deplete ] bulk depleted - count={}, reason={}", processed.size(), reason);
        return processed;
    }
}
