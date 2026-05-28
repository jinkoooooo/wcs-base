package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.util.generator.PalletBoxSeqGenerator;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireFound;
import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import static operato.logis.wcs.common.util.lang.CommonUtils.nullToEmpty;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 박스 일련번호 확정 처리.
 *
 * 입고 등록 시점에는 box_seq / box_barcode 가 모두 NULL 로 생성된다.
 * 사용자가 PalletWorkstation 에서 박스 추가·수정·폐기를 마친 뒤 [확정] 액션을 호출하면
 * 이 서비스가 (item_code, lot_no, 입고일자) 그룹 단위 일련번호를 부여한다.
 *
 * 특성:
 *   - Idempotent — 이미 확정된 박스(box_seq != null) 는 skip.
 *   - Cross-pallet — 같은 (item, lot, date) 그룹이면 파렛트가 달라도 시퀀스 이어짐
 *     (PalletBoxSeqGenerator 가 (seq_type, biz_date) 단위로 직렬화).
 *   - Pre-inbound 한정 — INBOUND 셔틀 주문이 진행 중이면 거부.
 *   - 합계 검증 — (item, lot) 그룹별 박스 total_qty 합계가 host_order_item EA 와 일치해야 함.
 */
@Service
@RequiredArgsConstructor
public class PalletBoxFinalizer {

    private static final Logger logger = LoggerFactory.getLogger(PalletBoxFinalizer.class);
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PalletBoxRepository boxRepository;
    private final HostOrderRepository hostOrderRepository;
    private final PalletBoxPreInboundGuard guard;
    private final PalletBoxSeqGenerator seqGenerator;

    /**
     * 파렛트 단위 박스 일련번호 확정.
     *
     * @return 이번 호출에서 box_seq 가 새로 부여된 박스 목록 (이미 확정된 박스는 미포함)
     */
    @Transactional(rollbackFor = Exception.class)
    public List<TbWcsPalletBox> finalize(String palletBarcode) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 비어있습니다.");

        // 입고 전 상태 + 합계 정합성 사전 검증
        guard.ensurePreInboundState(palletBarcode);
        guard.validateBoxSumMatchesHostOrder(palletBarcode);

        // 파렛트 박스 전수 로드
        List<TbWcsPalletBox> all = boxRepository.findByPalletBarcode(palletBarcode);
        requireFound(all, "BOX_NOT_FOUND", "해당 파렛트에 박스가 없습니다. (파렛트: " + palletBarcode + ")");

        // 호스트 주문 식별 — 박스에 묻혀있는 첫 hostOrderKey 사용
        String hostOrderKey = all.stream()
                .map(TbWcsPalletBox::getHostOrderKey)
                .filter(ValueUtil::isNotEmpty)
                .findFirst()
                .orElseThrow(() -> new ElidomRuntimeException("HOST_ORDER_NOT_FOUND",
                        "박스의 호스트 주문을 식별할 수 없습니다. (파렛트: " + palletBarcode + ")"));

        TbWcsHostOrder host = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        requireFound(host, "HOST_ORDER_NOT_FOUND", "호스트 주문을 찾을 수 없습니다. (host_order_key: " + hostOrderKey + ")");

        // 입고일자 결정 — receivedAt 우선, 없으면 today
        LocalDate inboundDate = resolveInboundDate(host);
        String yyyymmdd = inboundDate.format(YYYYMMDD);

        // 확정 대상 — DRAFT 박스만, 결정적 순서로 정렬
        List<TbWcsPalletBox> targets = new ArrayList<>();
        for (TbWcsPalletBox b : all) {
            if (BoxStatus.fromCode(b.getBoxStatus()) != BoxStatus.DRAFT) continue;
            targets.add(b);
        }
        targets.sort(Comparator
                .comparing((TbWcsPalletBox b) -> nullToEmpty(b.getItemCode()))
                .thenComparing(b -> nullToEmpty(b.getLotNo()))
                .thenComparing(b -> b.getCreatedAt() == null ? Long.MAX_VALUE : b.getCreatedAt().getTime())
                .thenComparing(TbWcsPalletBox::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        // 일련번호 부여 + DRAFT → PENDING 전이
        int palletBoxSeq = 1;
        for (TbWcsPalletBox b : targets) {
            int barcodeSeq = seqGenerator.next(b.getItemCode(), b.getLotNo(), inboundDate);
            String barcode = PalletBoxFactory.formatBoxBarcode(b.getItemCode(), b.getLotNo(), yyyymmdd, barcodeSeq);
            b.setBoxSeq(palletBoxSeq++);
            b.setBoxBarcode(barcode);
            PalletBoxStatusTransition.transition(b, BoxStatus.PENDING, "finalize");
        }
        // 변경 컬럼이 동일하므로 한 번의 batch update 로 묶는다 (update 235회 → 1회)
        if (!targets.isEmpty()) {
            boxRepository.updateBatch(targets, "boxSeq", "boxBarcode", "boxStatus");
        }

        logger.info("[ Pallet ][ Finalize ] completed - pallet={}, finalized={}, totalExisting={}",
                palletBarcode, targets.size(), all.size());
        return targets;
    }

    /** 입고일자 결정 — host.receivedAt 가 있으면 그 날짜, 없으면 오늘. */
    private LocalDate resolveInboundDate(TbWcsHostOrder host) {
        Date receivedAt = host.getReceivedAt();
        if (receivedAt != null) {
            return receivedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.now();
    }

}
