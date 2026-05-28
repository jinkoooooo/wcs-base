package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.util.lang.CommonUtils;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static operato.logis.wcs.common.util.check.Validator.requirePositive;

/**
 * 입고 전(pre-inbound) 단계의 박스 편집 오케스트레이터.
 *
 * 제공 작업:
 *   - editTotalQty   : DRAFT 박스의 total_qty 수정 (remaining_qty 동기화)
 *   - addBox         : 동일 host_order 의 item/lot 으로 박스 추가
 *   - voidPendingBox : DRAFT 박스 폐기 (soft-delete)
 *   - applyEditBatch : 위 작업을 한 트랜잭션으로 일괄 적용
 *
 * 책임 분리: 편집 가능 검증 PalletBoxEditGuard, 상태 변경 PalletBoxStateWriter,
 * 호스트 주문/아이템 해석 PalletHostOrderLookup, 입고 전 상태·합계 검증 PalletBoxPreInboundGuard.
 * 모든 변경은 트랜잭션 내부에서 (item, lot) 그룹 합계가 host_order_item EA 수량과 일치하는지
 * 검증한다 — 불일치 시 ElidomRuntimeException 으로 롤백.
 */
@Service
@RequiredArgsConstructor
public class PalletBoxEditor {

    private static final Logger logger = LoggerFactory.getLogger(PalletBoxEditor.class);

    private final PalletBoxRepository boxRepository;
    private final PalletBoxFactory palletBoxFactory;
    private final PalletBoxPreInboundGuard guard;
    private final PalletBoxStateWriter stateWriter;
    private final PalletHostOrderLookup hostOrderLookup;

    /**
     * DRAFT 박스의 total_qty 변경. remaining_qty 동기화, picked_qty=0.
     * 인쇄/확정된 박스는 거부.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsPalletBox editTotalQty(String boxId, int newTotalQty) {
        requirePositive(newTotalQty, "박스 수량은 1 이상이어야 합니다.");

        TbWcsPalletBox box = palletBoxFactory.requireBox(boxId);
        PalletBoxEditGuard.requireEditable(box);
        guard.ensurePreInboundState(box.getPalletBarcode());

        int before = CommonUtils.nz(box.getTotalQty());
        stateWriter.changeTotalQty(box, newTotalQty);

        logger.info("[ Pallet ][ BoxEdit ] totalQty changed - boxId={}, pallet={}, from={}, to={}",
                boxId, box.getPalletBarcode(), before, newTotalQty);

        guard.validateBoxSumMatchesHostOrder(box.getPalletBarcode());
        return box;
    }

    /**
     * 파렛트에 새 박스를 추가.
     * (item_code, lot_no) 는 동일 host_order_item 에 존재해야 함.
     * box_seq / box_barcode 는 NULL — 파렛트 [확정] 단계에서 부여된다.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsPalletBox addBox(String palletBarcode,
                                 String itemCode,
                                 String lotNo,
                                 int totalQty,
                                 Date produceDate,
                                 Date expiryDate) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 비어있습니다.");
        requireNotEmpty(itemCode, "INVALID_PARAMETER", "item_code 가 비어있습니다.");
        requirePositive(totalQty, "박스 수량은 1 이상이어야 합니다.");

        guard.ensurePreInboundState(palletBarcode);

        // 호스트 주문 식별 + 주문에 존재하는 (item, lot) 매칭
        TbWcsHostOrder host = hostOrderLookup.resolveHostOrder(palletBarcode);
        TbWcsHostOrderItem matched = PalletHostOrderLookup.matchHostItem(
                hostOrderLookup.hostItemsOf(host), itemCode, lotNo);

        TbWcsPalletBox b = addNewBox(host, matched, totalQty, produceDate, expiryDate);

        logger.info("[ Pallet ][ BoxEdit ] added - pallet={}, item={}, lot={}, totalQty={}",
                palletBarcode, itemCode, lotNo, totalQty);

        guard.validateBoxSumMatchesHostOrder(palletBarcode);
        return b;
    }

    /**
     * DRAFT 박스 폐기 (VOID).
     * 인쇄/확정된 박스는 거부.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsPalletBox voidPendingBox(String boxId, String reason) {
        TbWcsPalletBox box = palletBoxFactory.requireBox(boxId);
        PalletBoxEditGuard.requireEditable(box);
        guard.ensurePreInboundState(box.getPalletBarcode());

        stateWriter.voidBox(box, "voidPendingBox" + (ValueUtil.isEmpty(reason) ? "" : ":" + reason));

        logger.info("[ Pallet ][ BoxEdit ] voided draft - boxId={}, pallet={}, reason={}",
                boxId, box.getPalletBarcode(), ValueUtil.isEmpty(reason) ? "-" : reason);

        guard.validateBoxSumMatchesHostOrder(box.getPalletBarcode());
        return box;
    }

    /** "박스 추가" 모달용 — host_order_item 목록을 EA 수량과 함께 반환. */
    public List<Map<String, Object>> listHostItemsForPallet(String palletBarcode) {
        return hostOrderLookup.listHostItemsForPallet(palletBarcode);
    }

    /**
     * 배치 편집 — edits / additions / deletions 를 한 트랜잭션에 적용. 마지막에 한 번만 합계 검증.
     *
     * UI 에서 draft 로 모아둔 변경을 한 번에 반영하기 위한 진입점. 단일 변경마다 합계
     * 검증을 하면 정합성 깨진 중간 상태가 항상 거부되므로, 배치로만 합계 검증을 수행한다.
     *
     * @param edits     [{ "boxId", "totalQty" }, ...]
     * @param additions [{ "itemCode", "lotNo", "totalQty", "produceDate"?, "expiryDate"? }, ...]
     * @param deletions [{ "boxId" }, ...] — DRAFT 박스 물리 삭제(폐기 아님)
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyEditBatch(String palletBarcode,
                                              List<Map<String, Object>> edits,
                                              List<Map<String, Object>> additions,
                                              List<Map<String, Object>> deletions) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 비어있습니다.");
        if (ValueUtil.isEmpty(edits) && ValueUtil.isEmpty(additions) && ValueUtil.isEmpty(deletions)) {
            throw new ElidomRuntimeException("EMPTY_BATCH", "변경 내용이 없습니다.");
        }

        guard.ensurePreInboundState(palletBarcode);

        // edits → deletions → additions 순으로 적용, 합계 검증은 마지막에 한 번만
        int editedCount = applyEdits(palletBarcode, edits);
        int addedCount = applyAdditions(palletBarcode, additions);
        int deletedCount = applyDeletions(palletBarcode, deletions);

        guard.validateBoxSumMatchesHostOrder(palletBarcode);

        logger.info("[ Pallet ][ BoxEdit ] batch applied - pallet={}, edited={}, added={}, deleted={}",
                palletBarcode, editedCount, addedCount, deletedCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("editedCount", editedCount);
        result.put("addedCount", addedCount);
        result.put("deletedCount", deletedCount);
        return result;
    }

    /** edits — total_qty 일괄 변경. */
    private int applyEdits(String palletBarcode, List<Map<String, Object>> edits) {
        if (ValueUtil.isEmpty(edits)) return 0;
        int count = 0;
        for (Map<String, Object> e : edits) {
            String boxId = CommonUtils.toTrimmedString(e.get("boxId"));
            int newTotalQty = CommonUtils.toInt(e.get("totalQty"));
            requireNotEmpty(boxId, "INVALID_PARAMETER", "edits 항목에 boxId 가 없습니다.");
            requirePositive(newTotalQty, "박스 수량은 1 이상이어야 합니다. (boxId=" + boxId + ")");

            TbWcsPalletBox box = palletBoxFactory.requireBox(boxId);
            PalletBoxEditGuard.requireSamePallet(box, palletBarcode);
            PalletBoxEditGuard.requireEditable(box);
            stateWriter.changeTotalQty(box, newTotalQty);
            count++;
        }
        return count;
    }

    /** deletions — DRAFT 박스 일괄 물리 삭제 (폐기 아님 — 입고 전 박스라 행째 제거). */
    private int applyDeletions(String palletBarcode, List<Map<String, Object>> deletions) {
        if (ValueUtil.isEmpty(deletions)) return 0;
        int count = 0;
        for (Map<String, Object> d : deletions) {
            String boxId = CommonUtils.toTrimmedString(d.get("boxId"));
            requireNotEmpty(boxId, "INVALID_PARAMETER", "deletions 항목에 boxId 가 없습니다.");
            TbWcsPalletBox box = palletBoxFactory.requireBox(boxId);
            PalletBoxEditGuard.requireSamePallet(box, palletBarcode);
            PalletBoxEditGuard.requireEditable(box);
            stateWriter.deleteBox(box);
            count++;
        }
        return count;
    }

    /** additions — 신규 박스 일괄 insert. */
    private int applyAdditions(String palletBarcode, List<Map<String, Object>> additions) {
        if (ValueUtil.isEmpty(additions)) return 0;

        TbWcsHostOrder host = hostOrderLookup.resolveHostOrder(palletBarcode);
        List<TbWcsHostOrderItem> hostItems = hostOrderLookup.hostItemsOf(host);

        int count = 0;
        for (Map<String, Object> a : additions) {
            String itemCode = CommonUtils.toTrimmedString(a.get("itemCode"));
            String lotNo = CommonUtils.toTrimmedString(a.get("lotNo"));
            int totalQty = CommonUtils.toInt(a.get("totalQty"));
            requireNotEmpty(itemCode, "INVALID_PARAMETER", "additions 항목에 itemCode 가 없습니다.");
            requirePositive(totalQty, "박스 수량은 1 이상이어야 합니다. (item=" + itemCode + ")");

            TbWcsHostOrderItem matched = PalletHostOrderLookup.matchHostItem(hostItems, itemCode, lotNo);
            addNewBox(host, matched, totalQty, dateOf(a.get("produceDate")), dateOf(a.get("expiryDate")));
            count++;
        }
        return count;
    }

    /** 신규 박스 생성 + 옵션 날짜 덮어쓰기 후 insert. */
    private TbWcsPalletBox addNewBox(TbWcsHostOrder host, TbWcsHostOrderItem matched,
                                     int totalQty, Date produceDate, Date expiryDate) {
        TbWcsPalletBox b = palletBoxFactory.newBox(host, matched, totalQty);
        if (produceDate != null) b.setProduceDate(produceDate);
        if (expiryDate != null) b.setExpiryDate(expiryDate);
        boxRepository.insert(b);
        return b;
    }

    /** UI 가 보낼 수 있는 다양한 날짜 표현(Date/ISO8601/LocalDateTime/LocalDate) 을 Date 로 변환. */
    private static Date dateOf(Object v) {
        if (v == null) return null;
        if (v instanceof Date d) return d;
        if (v instanceof Number n) return new Date(n.longValue());
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try { return Date.from(java.time.OffsetDateTime.parse(s).toInstant()); }
        catch (Exception ignore) { /* fallthrough */ }
        try { return Date.from(java.time.LocalDateTime.parse(s).atZone(java.time.ZoneId.systemDefault()).toInstant()); }
        catch (Exception ignore) { /* fallthrough */ }
        try { return Date.from(java.time.LocalDate.parse(s).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()); }
        catch (Exception ignore) { return null; }
    }
}
