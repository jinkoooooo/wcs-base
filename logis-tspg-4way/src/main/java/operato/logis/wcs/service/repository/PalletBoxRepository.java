package operato.logis.wcs.service.repository;

import operato.logis.wcs.entity.TbWcsPalletBox;
import org.springframework.stereotype.Repository;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.List;

/**
 * TbWcsPalletBox 영속성 전담 DAO.
 *
 * 파렛트 박스의 조회·생성·수정을 한 aggregate 단위로 캡슐화한다.
 * remaining_qty 는 일반 컬럼이며 출고 가능 박스 = box_status=SCANNED AND remaining_qty>0.
 */
@Repository
public class PalletBoxRepository extends AbstractQueryService {

    /** PK 로 단건 조회. */
    public TbWcsPalletBox findById(String id) {
        return queryManager.select(TbWcsPalletBox.class, id);
    }

    /** host_order_key 로 박스 목록 조회 (box_seq 순). */
    public List<TbWcsPalletBox> findByHostOrderKey(String key) {
        if (ValueUtil.isEmpty(key)) return Collections.emptyList();
        Query q = OrmUtil.newConditionForExecution();
        q.addFilter("host_order_key", key);
        q.addOrder("boxSeq", true);
        return queryManager.selectList(TbWcsPalletBox.class, q);
    }

    /** pallet_barcode 로 박스 목록 조회 (box_seq 순). */
    public List<TbWcsPalletBox> findByPalletBarcode(String pallet) {
        if (ValueUtil.isEmpty(pallet)) return Collections.emptyList();
        Query q = OrmUtil.newConditionForExecution();
        q.addFilter("pallet_barcode", pallet);
        q.addOrder("boxSeq", true);
        return queryManager.selectList(TbWcsPalletBox.class, q);
    }

    /** box_barcode 로 단건 조회. */
    public TbWcsPalletBox findByBoxBarcode(String boxBarcode) {
        if (ValueUtil.isEmpty(boxBarcode)) return null;
        Query q = OrmUtil.newConditionForExecution();
        q.addFilter("box_barcode", boxBarcode);
        q.setPageIndex(1);
        q.setPageSize(1);
        List<TbWcsPalletBox> rows = queryManager.selectList(TbWcsPalletBox.class, q);
        return ValueUtil.isEmpty(rows) ? null : rows.get(0);
    }

    /** box_barcode + box_status IN 으로 단건 조회. */
    public TbWcsPalletBox findByBoxBarcodeAndStatus(String boxBarcode, List<Integer> boxStatusList) {
        if (ValueUtil.isEmpty(boxBarcode) || ValueUtil.isEmpty(boxStatusList)) return null;
        Query q = OrmUtil.newConditionForExecution();
        q.addFilter("box_barcode", boxBarcode);
        q.addFilter("box_status", OrmConstants.IN, boxStatusList);
        q.setPageIndex(1);
        q.setPageSize(1);
        List<TbWcsPalletBox> rows = queryManager.selectList(TbWcsPalletBox.class, q);
        return ValueUtil.isEmpty(rows) ? null : rows.get(0);
    }

    /** outbound_order_key 로 출고된 모든 박스 조회. 확정 후에도 키 유지(이력용). */
    public List<TbWcsPalletBox> findByOutboundOrderKey(String outboundOrderKey) {
        if (ValueUtil.isEmpty(outboundOrderKey)) return Collections.emptyList();
        Query q = OrmUtil.newConditionForExecution();
        q.addFilter("outbound_order_key", outboundOrderKey);
        q.addOrder("boxSeq", true);
        return queryManager.selectList(TbWcsPalletBox.class, q);
    }

    /**
     * outbound_order_key + picked_qty>0 박스 조회 (진행률 계산용).
     * 확정 시 picked_qty 가 0 으로 리셋되므로 현재 진행 중인 출고만 잡힘.
     */
    public List<TbWcsPalletBox> findActivePickedByOutbound(String outboundOrderKey) {
        if (ValueUtil.isEmpty(outboundOrderKey)) return Collections.emptyList();
        Query q = OrmUtil.newConditionForExecution();
        q.addFilter("outbound_order_key", outboundOrderKey);
        q.addFilter("picked_qty", ">", 0);
        q.addOrder("boxSeq", true);
        return queryManager.selectList(TbWcsPalletBox.class, q);
    }

    /** 신규 박스 insert. */
    public TbWcsPalletBox insert(TbWcsPalletBox e) {
        queryManager.insert(e);
        return e;
    }

    /** 지정 필드 update. remaining_qty 는 일반 컬럼이라 fields 포함 가능. */
    public TbWcsPalletBox update(TbWcsPalletBox e, String... fields) {
        queryManager.update(e, fields);
        return e;
    }

    /** 지정 필드 일괄 update — 다건 처리 시 건건 update 대신 batch 로 묶는다. */
    public List<TbWcsPalletBox> updateBatch(List<TbWcsPalletBox> list, String... fields) {
        if (ValueUtil.isEmpty(list)) return list;
        queryManager.updateBatch(list, fields);
        return list;
    }

    /** 박스 물리 삭제 — 입고 전 DRAFT 박스 제거용(폐기 아님). */
    public void delete(TbWcsPalletBox e) {
        queryManager.delete(e);
    }
}
