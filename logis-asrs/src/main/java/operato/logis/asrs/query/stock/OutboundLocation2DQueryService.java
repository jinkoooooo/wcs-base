package operato.logis.asrs.query.stock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.query.stock.model.OutboundLocation2DAisleOptionView;
import operato.logis.asrs.query.stock.model.OutboundLocation2DMapRowView;
import operato.logis.asrs.query.stock.model.OutboundLocation2DSideOptionView;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 2D 로케이션 지정출고 화면용 조회 서비스.
 *
 * 규칙:
 * - location 테이블을 기준으로 맵 구성
 * - current_location_id 기준으로 stock / item / lot 연결
 * - 외부 입력은 areaCode / aisleNo / sideCode 기준
 */
@Service
@RequiredArgsConstructor
public class OutboundLocation2DQueryService extends AbstractQueryService {

    /**
     * area 기준 aisle 목록 조회
     */
    public List<OutboundLocation2DAisleOptionView> getAisleOptions(String areaCode) {
        if (areaCode == null || areaCode.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String sql = ""
                + "select distinct \n"
                + "    l.aisle_no as aisle_no \n"
                + "from tb_ac_location l \n"
                + "inner join tb_ac_storage_area a on a.id = l.area_id \n"
                + "where a.area_code = :areaCode \n"
                + "  and l.aisle_no is not null \n"
                + "order by l.aisle_no asc";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("areaCode", areaCode);

        return this.queryManager.selectListBySql(sql, params, OutboundLocation2DAisleOptionView.class, 0, 0);
    }

    /**
     * area + aisle 기준 side 목록 조회
     */
    public List<OutboundLocation2DSideOptionView> getSideOptions(String areaCode, Integer aisleNo) {
        if (areaCode == null || areaCode.trim().isEmpty() || aisleNo == null) {
            return Collections.emptyList();
        }

        String sql = ""
                + "select distinct \n"
                + "    l.side_code as side_code \n"
                + "from tb_ac_location l \n"
                + "inner join tb_ac_storage_area a on a.id = l.area_id \n"
                + "where a.area_code = :areaCode \n"
                + "  and l.aisle_no = :aisleNo \n"
                + "  and l.side_code is not null \n"
                + "order by l.side_code asc";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("areaCode", areaCode);
        params.put("aisleNo", aisleNo);

        return this.queryManager.selectListBySql(
                sql,
                params,
                OutboundLocation2DSideOptionView.class,
                0,
                0
        );
    }

    /**
     * area + aisle + side 기준 2D 맵 데이터 조회
     */
    public List<OutboundLocation2DMapRowView> getMapRows(String areaCode, Integer aisleNo, String sideCode) {
        if (areaCode == null || areaCode.trim().isEmpty()
                || aisleNo == null
                || sideCode == null || sideCode.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String sql = ""
                + "select \n"
                + "    l.id as location_id, \n"
                + "    l.location_code as location_code, \n"
                + "    a.area_code as area_code, \n"
                + "    l.aisle_no as aisle_no, \n"
                + "    l.side_code as side_code, \n"
                + "    l.bay_no as bay_no, \n"
                + "    l.level_no as level_no, \n"
                + "    l.depth_no as depth_no, \n"
                + "    s.id as stock_unit_id, \n"
                + "    s.stock_unit_no as stock_unit_no, \n"
                + "    im.id as item_id, \n"
                + "    im.item_code as item_code, \n"
                + "    im.item_name as item_name, \n"
                + "    coalesce(s.qty, 0) as qty, \n"
                + "    coalesce(s.reserved_qty, 0) as reserved_qty, \n"
                + "    lot.lot_no as lot_no, \n"
                + "    s.stock_status_code as stock_status_code, \n"
                + "    s.active_yn as active_yn \n"
                + "from tb_ac_location l \n"
                + "inner join tb_ac_storage_area a on a.id = l.area_id \n"
                + "left join tb_ac_stock_unit s on s.current_location_id = l.id \n"
                + "left join tb_ac_item_master im on im.id = s.item_id \n"
                + "left join tb_ac_lot lot on lot.id = s.lot_id \n"
                + "where a.area_code = :areaCode \n"
                + "  and l.aisle_no = :aisleNo \n"
                + "  and l.side_code = :sideCode \n"
                + "order by l.level_no desc, l.bay_no asc, l.depth_no asc";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("areaCode", areaCode);
        params.put("aisleNo", aisleNo);
        params.put("sideCode", sideCode);

        return this.queryManager.selectListBySql(
                sql,
                params,
                OutboundLocation2DMapRowView.class,
                0,
                0
        );
    }
}