package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.impl.order.lookup.OrderLookupUtils;
import operato.logis.wcs.service.repository.HostOrderItemRepository;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.List;

import static operato.logis.wcs.common.util.check.Validator.requireFound;
import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;

/**
 * 박스 레코드 생성 책임.
 *
 * 박스 바코드 정책 — B-{itemCode}-{lotNo}-{YYYYMMDD}-{seq4} 형식.
 * 입고 등록 시점에는 box_seq / box_barcode 모두 NULL 로 생성되며, 사용자가
 * PalletBoxFinalizer.finalize 를 호출(또는 [확정] 버튼을 누른) 시점에
 * (item_code, lot_no, 입고일자) 그룹 단위 일련번호로 채워진다. 확정 후 불변.
 *
 * 인쇄/재발행은 PalletBoxPrinter, 소진은 PalletBoxDepleter 가 담당.
 * 외부 I/O 없음 — repository 호출만 수행.
 */
@Service
@RequiredArgsConstructor
public class PalletBoxFactory {

    private static final Logger logger = LoggerFactory.getLogger(PalletBoxFactory.class);

    public static final String BOX_BARCODE_PREFIX = "B-";
    public static final String BOX_BARCODE_DELIMITER = "-";

    private final PalletBoxRepository boxRepository;
    private final HostOrderItemRepository hostOrderItemRepository;
    private final InventoryItemMasterRepository itemMasterRepository;
    private final OrderLookupUtils orderLookup;

    /**
     * 박스 바코드 생성 — (itemCode, lotNo, 입고일자, 일련번호) 를 조합한 불변 식별자.
     *
     * 형식: B-{itemCode}-{lotNo}-{YYYYMMDD}-{4자리 zero-padding boxSeq}
     * 예시: B-SKU001-LOT-A-20260522-0003
     *
     * itemCode / lotNo 는 하이픈을 포함할 수 있으나 파싱 로직은 제공하지 않는다.
     * 박스 바코드는 opaque identifier 로만 사용하고 식별이 필요하면 DB 조회를 사용한다.
     */
    public static String formatBoxBarcode(String itemCode, String lotNo, String yyyymmdd, int boxSeq) {
        requireNotEmpty(itemCode, "INVALID_PARAMETER", "박스 바코드 생성 실패 — itemCode 가 비어있습니다.");
        requireNotEmpty(yyyymmdd, "INVALID_PARAMETER", "박스 바코드 생성 실패 — 입고일자(yyyymmdd) 가 비어있습니다.");
        return BOX_BARCODE_PREFIX + String.join(BOX_BARCODE_DELIMITER,
                itemCode, lotNo == null ? "" : lotNo, yyyymmdd, String.format("%04d", boxSeq));
    }

    /**
     * host_order 의 모든 host_order_item 에 대해 박스 행만 생성.
     * box_seq / box_barcode 는 NULL — 사용자가 PalletBoxFinalizer 로 확정해야 채워짐.
     */
    @Transactional(rollbackFor = Exception.class)
    public int generateBoxes(String hostOrderKey) {
        TbWcsHostOrder host = requireHost(hostOrderKey);
        List<TbWcsHostOrderItem> items = hostOrderItemRepository.findByHostOrderKey(
                host.getHostSystemCode(), hostOrderKey);
        if (ValueUtil.isEmpty(items)) return 0;

        int total = 0;
        for (TbWcsHostOrderItem item : items) {
            // 마스터 + EA 환산 결정
            ExtTbInventoryItemMaster master = itemMasterRepository.findByOwnerAndCode(
                    host.getOwnerCode(), item.getItemCode());
            int eaQty = toEaQty(master, item);
            if (eaQty <= 0) continue;

            // 박스당 수량 결정 — master.boxQty 우선
            int perBox = ValueUtil.isNotEmpty(master) && ValueUtil.isNotEmpty(master.getBoxQty()) && master.getBoxQty() > 0
                    ? master.getBoxQty() : eaQty;

            // 잔량을 박스당 수량씩 나눠 N건 insert
            int remaining = eaQty;
            while (remaining > 0) {
                int q = Math.min(perBox, remaining);
                boxRepository.insert(newBox(host, item, q));
                total++;
                remaining -= q;
            }
        }
        logger.info("[ Pallet ][ Generate ] boxes created - host={}, count={}", hostOrderKey, total);
        return total;
    }

    /** 박스 단건 조회 — 컨트롤러가 첫 발행/재발행 분기에 사용. */
    public TbWcsPalletBox findById(String boxId) {
        return boxRepository.findById(boxId);
    }

    /**
     * 신규 박스 생성 — box_seq / box_barcode 는 NULL 로 두며 확정 단계에서 채워진다.
     */
    public TbWcsPalletBox newBox(TbWcsHostOrder h, TbWcsHostOrderItem i, int qty) {
        TbWcsPalletBox b = new TbWcsPalletBox();
        b.setEqGroupId(h.getEqGroupId());
        b.setHostOrderKey(h.getHostOrderKey());
        b.setPalletBarcode(h.getBarcode());
        b.setItemCode(i.getItemCode());
        b.setLotNo(i.getLotNo());
        b.setTotalQty(qty);
        b.setPickedQty(0);
        b.setRemainingQty(qty);
        b.setUom("EA");
        b.setBoxStatus(BoxStatus.DRAFT.code());
        b.setPrintCount(0);
        b.setTestRequestNo(i.getTestRequestNo());
        b.setTestNo(i.getTestNo());
        b.setProduceDate(i.getProduceDate());
        b.setExpiryDate(i.getExpiryDate());
        return b;
    }

    /**
     * UOM 수량을 EA 로 환산.
     * EA 면 그대로, 아니면 마스터의 boxQty/palletQty 적용. 환산 실패는 원본 qty fallback.
     */
    int toEaQty(ExtTbInventoryItemMaster master, TbWcsHostOrderItem item) {
        int qty = item.getQty();
        if (qty <= 0) return 0;
        String uom = item.getUom();
        if (ValueUtil.isEmpty(uom) || "EA".equalsIgnoreCase(uom) || ValueUtil.isEmpty(master)) return qty;
        try { return master.toEaQty(uom, qty); } catch (Exception e) { return qty; }
    }

    /** 박스 단건 조회 후 미존재 시 BOX_NOT_FOUND. 인쇄/편집 진입 시 사용. */
    TbWcsPalletBox requireBox(String id) {
        TbWcsPalletBox b = boxRepository.findById(id);
        requireFound(b, "BOX_NOT_FOUND", "해당 박스를 찾을 수 없습니다. (박스 ID: " + id + ")");
        return b;
    }

    /**
     * 박스가 확정되었는지 검증 — box_seq 와 box_barcode 가 모두 채워져 있어야 한다.
     */
    void requireFinalized(TbWcsPalletBox box) {
        if (box.getBoxSeq() == null || ValueUtil.isEmpty(box.getBoxBarcode())) {
            throw new ElidomRuntimeException("BOX_NOT_FINALIZED",
                    "박스 일련번호가 확정되지 않았습니다. [확정] 후 인쇄해주세요. (boxId=%s)"
                            .formatted(box.getId()));
        }
    }

    private TbWcsHostOrder requireHost(String k) {
        return orderLookup.getHostOrderOrThrow(k);
    }
}
