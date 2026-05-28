package operato.logis.samsung.service.mw;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.consts.InboundStatus;
import operato.logis.samsung.consts.TrackingStatus;
import operato.logis.samsung.dto.dashboard.DashboardInboundDelivery;
import operato.logis.samsung.entity.mw.TbMwInboundDelivery;
import operato.logis.samsung.entity.mw.TbMwItemMaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TbMwInboundDeliveryService extends AbstractQueryService {

    private final TbMwItemMasterService tbMwItemMasterService;
    private final TbMwInboundJobService tbMwInboundJobService;

    public TbMwInboundDelivery getRunningInboundDeliveryByItemCode(String itemCode, InboundStatus status) {

        String sql = "select * from tb_mw_inbound_delivery where item_code = :itemCode and inbound_status = :inboundStatus limit 1";
        Map<String, Object> param = ValueUtil.newMap("itemCode,inboundStatus", itemCode, status.value());
        return this.queryManager.selectBySql(sql, param, TbMwInboundDelivery.class);
    }

    public TbMwInboundDelivery getNextInboundDeliveryByItemCode(String itemCode) {
        String sql = """
                SELECT *
                FROM tb_mw_inbound_delivery
                WHERE inbound_status = :inboundStatus
                  AND item_code = :itemCode
                  AND item_priority::int <= (
                      SELECT MIN(item_priority::int) + 1
                      FROM tb_mw_inbound_delivery
                      WHERE inbound_status = :inboundStatus
                        AND manual_flag = false
                  )
                """;
        Map<String, Object> param = ValueUtil.newMap("itemCode,inboundStatus", itemCode, InboundStatus.RUNNING.value());
        return this.queryManager.selectBySql(sql, param, TbMwInboundDelivery.class);
    }

    public List<TbMwInboundDelivery> getInboundDeliveryByDate(Date targetDate) {
        String sql =
                "select * " +
                        "  from tb_mw_inbound_delivery " +
                        " where inbound_date = :inboundDate";

        Map<String, Object> param = ValueUtil.newMap(
                "inboundDate",
                new java.sql.Date(targetDate.getTime())   // date 컬럼에 맞게 변환
        );
        return this.queryManager.selectListBySql(sql, param, TbMwInboundDelivery.class, 0, 0);
    }

    public List<TbMwInboundDelivery> getInboundDeliveryByDateSdl(String startDate, String endDate) {
        String sql =
                "select * " +
                        "  from tb_mw_inbound_delivery " +
                        " where inbound_date between :startDate and :endDate";

        Map<String, Object> param = ValueUtil.newMap(
                "startDate,endDate",
                java.sql.Date.valueOf(startDate), // String을 곧바로 sql.Date로 변환!
                java.sql.Date.valueOf(endDate)    // String을 곧바로 sql.Date로 변환!
        );
        return this.queryManager.selectListBySql(sql, param, TbMwInboundDelivery.class, 0, 0);
    }

    public List<DashboardInboundDelivery.SkuSummary> getSkuSummaryList(String deliveryNo, String cntrNo) {

        String sql =
                "SELECT " +
                        "  d.item_code                             AS \"sku\", " +
                        "  d.item_type                             AS \"itemType\", " +
                        "  d.item_desc                             AS \"itemDesc\", " +
                        "  d.item_qty                              AS \"totalQty\", " +
                        "  COALESCE(SUM(o.pass_qty + o.ng_qty),0)  AS \"inboundQty\", " +
                        "  COALESCE(SUM(o.pass_qty),0)             AS \"palletQty\", " +
                        "  COALESCE(SUM(o.ng_qty),0)               AS \"ngQty\", " +
                        "  COALESCE(MAX(o.process_status),0)       AS \"processStatus\", " +
                        "  MAX(d.inbound_status)                   AS \"inboundStatus\", " +
                        "  BOOL_OR(d.manual_flag)                  AS \"manualFlag\" " +
                        "FROM tb_mw_inbound_delivery d " +
                        "LEFT JOIN tb_mw_xyz_order o " +
                        "  ON o.delivery_no = d.bl_no " +
                        " AND o.cntr_no    = d.cntr_no " +
                        " AND o.item_code  = d.inner_item_code " +
                        "WHERE d.bl_no   = :deliveryNo " +
                        "  AND d.cntr_no = :cntrNo " +
                        "  AND d.inbound_status IN (1, 2, 3) " +
                        "GROUP BY d.item_code, d.item_type, d.item_desc, d.item_qty ";

        Map<String, Object> param = ValueUtil.newMap("deliveryNo", deliveryNo);
        param.put("cntrNo", cntrNo);

        return this.queryManager.selectListBySql(
                sql,
                param,
                DashboardInboundDelivery.SkuSummary.class,
                0,
                0
        );
    }

    public List<TbMwInboundDelivery> getInboundDeliveryList(String blNo, String cntrNo) {
        String sql = "select * from tb_mw_inbound_delivery where bl_no = :blNo and cntr_no = :cntrNo";
        Map<String, Object> param = ValueUtil.newMap("blNo,cntrNo", blNo, cntrNo);
        return this.queryManager.selectListBySql(sql, param, TbMwInboundDelivery.class, 0, 0);
    }

    public void completeInboundDelivery(String blNo, String cntrNo, String itemCode, String innerItemCode) {
        String sql = "update tb_mw_inbound_delivery set inbound_status = :inboundStatus, complete_datetime = :completeDatetime where bl_no = :blNo and cntr_no = :cntrNo and item_code = :itemCode";
        Map<String, Object> param = ValueUtil.newMap("inboundStatus,completeDatetime,blNo,cntrNo,itemCode", InboundStatus.COMPLETE.value(), new Date(), blNo, cntrNo, itemCode);
        this.queryManager.executeBySql(sql, param);

        List<TbMwInboundDelivery> inboundDeliveryList = getInboundDeliveryList(blNo, cntrNo);
        if (ValueUtil.isEmpty(inboundDeliveryList)) return;
        for (TbMwInboundDelivery inboundDelivery : inboundDeliveryList) {
            if (!InboundStatus.COMPLETE.value().equals(inboundDelivery.getInboundStatus())) {
                return;
            }
        }

        // 20260430.JJG.신규 : 해당 컨테이너 동일상품 모든 Box PID "END-" 처리. 필수
        String sql2 = "update tb_mw_box set " +
                "final_at = :completeDatetime, " +
                "plc_seq_no = 'END-' || plc_seq_no " +
                "where bl_no = :blNo " +
                "and cntr_no = :cntrNo " +
                "and item_code = :innerItemCode " +
                "and plc_seq_no not like 'END%'";
        Map<String, Object> param2 = ValueUtil.newMap("completeDatetime,blNo,cntrNo,innerItemCode"
                , new Date(), blNo, cntrNo, innerItemCode);
        this.queryManager.executeBySql(sql2, param2);


        tbMwInboundJobService.getDoneDelivery(inboundDeliveryList.get(0));
    }

    public TbMwInboundDelivery getInboundDeliveryByBarcodeWithLock(String deliveryNo, String cntrNo, String barcode) {
        TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(barcode);

        String sql = "select * from tb_mw_inbound_delivery where bl_no = :deliveryNo and cntr_no = :cntrNo and item_code = :itemCode for update";
        Map<String, Object> param = ValueUtil.newMap("deliveryNo,cntrNo,itemCode", deliveryNo, cntrNo, itemMaster.getItemCode());
        return this.queryManager.selectBySql(sql, param, TbMwInboundDelivery.class);
    }

    public void updateResultQty(String blNo, String cntrNo, String barcode, int passQty, int ngQty) {
        TbMwInboundDelivery delivery = getInboundDeliveryByBarcodeWithLock(blNo, cntrNo, barcode);
        if (ValueUtil.isEmpty(delivery)) return;

        delivery.setPassQty(delivery.getPassQty() + passQty);
        delivery.setNgQty(delivery.getNgQty() + ngQty);
        this.queryManager.update(delivery, "passQty", "ngQty");

        tbMwInboundJobService.updateResultQty(blNo, cntrNo, passQty, ngQty);
    }

    @Transactional
    public int updateInboundSeq(List<TbMwInboundDelivery> items) {
        int updated = 0;

        String sql ="UPDATE tb_mw_inbound_delivery " +
                "   SET inbound_seq = CASE WHEN inbound_status = 2 THEN :inboundSeq ELSE inbound_seq END, " +
                "       manual_flag = CASE WHEN inbound_status < 3 THEN :manualFlag ELSE manual_flag END, " +
                "       updated_at = now(), " +
                "       updater_id = :updaterId " +
                " WHERE id = :id " +
                "   AND bl_no = :blNo " +
                "   AND cntr_no = :cntrNo " +
                "   AND inbound_status < 3";
        Map<String, Object> params = new HashMap<>();
        // 리스트를 반복하면서 각 FieldStatus 객체를 DB에 저장합니다.
        for (TbMwInboundDelivery data : items) {
            if (ValueUtil.isEmpty(data.getId())) continue;

            params.put("id", data.getId());
            params.put("inboundSeq", data.getInboundSeq());
            params.put("manualFlag", data.isManualFlag());
            params.put("blNo", data.getBlNo());
            params.put("cntrNo", data.getCntrNo());
            params.put("updaterId", "sys");

            int cnt = this.queryManager.executeBySql(sql,params);

            updated += cnt;
        }

        return updated;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getInboundDefectReport(Map<String, Object> param) {

        String sql =
                "SELECT d.*, " +
                        "       m.item_name AS itemName, " +
                        "       m.item_code AS itemCode, " +
                        "       b.reject_type AS finalRemark, " +
                        "       b.box_id AS boxId " +
                        "FROM samsung_mw.tb_mw_inbound_delivery d " +
                        "LEFT JOIN samsung_mw.tb_mw_item_master m " +
                        "    ON d.inner_item_code = m.inner_item_code " +
                        "LEFT JOIN samsung_mw.tb_mw_box b " +
                        "    ON d.cntr_no = b.cntr_no " +
                        "   AND d.bl_no = b.bl_no " +
                        "   AND d.inner_item_code = b.item_code " +
                        "   AND d.inbound_date = CAST(b.final_at AS DATE) " +
                        " WHERE b.final_status = 251 " +
                        "  AND d.cntr_no = :cntrNo " +
                        "  AND d.inbound_date = CAST(:inboundDate AS DATE) ";


        List<?> rawResult = this.queryManager.selectListBySql(sql, param, Map.class, 0, 0);

        return (List<Map<String, Object>>) rawResult;
    }

    public List<TbMwInboundDelivery> getInboundDeliveryDateList(String blNo, String cntrNo, Date inboundDate) {
        String sql = "SELECT * FROM tb_mw_inbound_delivery WHERE bl_no = :blNo AND cntr_no = :cntrNo AND inbound_date = :inboundDate";

        Map<String, Object> param = new HashMap<>();
        param.put("blNo", blNo);
        param.put("cntrNo", cntrNo);
        param.put("inboundDate", inboundDate);

        return this.queryManager.selectListBySql(sql, param, TbMwInboundDelivery.class, 0, 0);
    }

    public List<Map<String, Object>> getInboundDevanningRequest(Map<String, Object> param) {

        String cntrNo = (String) param.get("cntrNo");

        String sql = "SELECT A.item_code, B.item_height, B.item_length, B.item_width, B.item_weight FROM samsung_mw.tb_mw_inbound_delivery A, samsung_mw.tb_mw_item_master B\n" +
                "WHERE A.inner_item_code = B.inner_item_code \n" +
                "and A.item_code = B.item_code \n" +
                "and A.cntr_no = :cntrNo";


        List<?> rawResult = this.queryManager.selectListBySql(sql, param, Map.class, 0, 0);

        return (List<Map<String, Object>>) rawResult;
    }
}