package operato.logis.asrs.query.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.query.item.model.ItemMasterDetailView;
import operato.logis.asrs.query.item.model.ItemMasterListView;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 상품마스터 조회 서비스.
 *
 * 역할:
 * - 목록 조회
 * - 상세 조회
 * - 존재 여부 확인
 * - 엔티티 단건 조회
 */
@Service
public class ItemMasterQueryService extends AbstractQueryService {

    /**
     * 상품 목록 조회.
     *
     * 검색 조건:
     * - itemCode 부분일치
     * - itemName 부분일치
     * - categoryCode 정확일치
     * - storageTempType 정확일치
     * - activeYn 정확일치
     */
    public List<ItemMasterListView> findItemMasters(String itemCode,
                                                    String itemName,
                                                    String categoryCode,
                                                    String storageTempType,
                                                    String activeYn) {

        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    im.id as id, ");
        sql.append("    im.item_code as item_code, ");
        sql.append("    im.item_name as item_name, ");
        sql.append("    im.item_category_id as item_category_id, ");
        sql.append("    ic.category_code as category_code, ");
        sql.append("    ic.category_name as category_name, ");
        sql.append("    im.operation_profile_id as operation_profile_id, ");
        sql.append("    im.industry_type as industry_type, ");
        sql.append("    im.base_uom as base_uom, ");
        sql.append("    im.handling_unit_type as handling_unit_type, ");
        sql.append("    im.outbound_unit_type as outbound_unit_type, ");
        sql.append("    im.storage_temp_type as storage_temp_type, ");
        sql.append("    im.lot_control_yn as lot_control_yn, ");
        sql.append("    im.expiry_control_yn as expiry_control_yn, ");
        sql.append("    im.serial_control_yn as serial_control_yn, ");
        sql.append("    im.active_yn as active_yn, ");
        sql.append("    to_char(im.updated_at, 'YYYY-MM-DD HH24:MI:SS') as updated_at ");
        sql.append("from tb_ac_item_master im ");
        sql.append("inner join tb_ac_item_category ic on ic.id = im.item_category_id ");
        sql.append("where 1 = 1 ");

        Map<String, Object> params = new HashMap<String, Object>();

        if (itemCode != null && !itemCode.trim().isEmpty()) {
            sql.append("and upper(im.item_code) like upper(:itemCode) ");
            params.put("itemCode", "%" + itemCode.trim() + "%");
        }

        if (itemName != null && !itemName.trim().isEmpty()) {
            sql.append("and upper(im.item_name) like upper(:itemName) ");
            params.put("itemName", "%" + itemName.trim() + "%");
        }

        if (categoryCode != null && !categoryCode.trim().isEmpty()) {
            sql.append("and ic.category_code = :categoryCode ");
            params.put("categoryCode", categoryCode.trim());
        }

        if (storageTempType != null && !storageTempType.trim().isEmpty()) {
            sql.append("and im.storage_temp_type = :storageTempType ");
            params.put("storageTempType", storageTempType.trim());
        }

        if (activeYn != null && !activeYn.trim().isEmpty()) {
            sql.append("and im.active_yn = :activeYn ");
            params.put("activeYn", activeYn.trim());
        }

        sql.append("order by im.item_code asc ");

        return this.queryManager.selectListBySql(
                sql.toString(),
                params,
                ItemMasterListView.class,
                0,
                0
        );
    }

    /**
     * 상품 상세 조회.
     *
     * 규칙:
     * - 외부 조회 key는 itemCode 기준
     * - 없으면 ENTITY_NOT_FOUND 예외 반환
     */
    public ItemMasterDetailView findItemMasterDetail(String itemCode) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    im.id as id, ");
        sql.append("    im.item_code as item_code, ");
        sql.append("    im.item_name as item_name, ");
        sql.append("    im.item_category_id as item_category_id, ");
        sql.append("    ic.category_code as category_code, ");
        sql.append("    ic.category_name as category_name, ");
        sql.append("    im.operation_profile_id as operation_profile_id, ");
        sql.append("    im.industry_type as industry_type, ");
        sql.append("    im.base_uom as base_uom, ");
        sql.append("    im.handling_unit_type as handling_unit_type, ");
        sql.append("    im.outbound_unit_type as outbound_unit_type, ");
        sql.append("    im.length_mm as length_mm, ");
        sql.append("    im.width_mm as width_mm, ");
        sql.append("    im.height_mm as height_mm, ");
        sql.append("    im.weight_g as weight_g, ");
        sql.append("    im.volume_mm3 as volume_mm3, ");
        sql.append("    im.storage_temp_type as storage_temp_type, ");
        sql.append("    im.lot_control_yn as lot_control_yn, ");
        sql.append("    im.expiry_control_yn as expiry_control_yn, ");
        sql.append("    im.serial_control_yn as serial_control_yn, ");
        sql.append("    im.partial_pick_yn as partial_pick_yn, ");
        sql.append("    im.mixed_load_yn as mixed_load_yn, ");
        sql.append("    im.fragile_yn as fragile_yn, ");
        sql.append("    im.heavy_yn as heavy_yn, ");
        sql.append("    im.quarantine_required_yn as quarantine_required_yn, ");
        sql.append("    im.allocation_rule_code as allocation_rule_code, ");
        sql.append("    im.rotation_profile_code as rotation_profile_code, ");
        sql.append("    im.storage_grade_seed as storage_grade_seed, ");
        sql.append("    im.ext_attr as ext_attr, ");
        sql.append("    im.active_yn as active_yn, ");
        sql.append("    im.domain_id as domain_id, ");
        sql.append("    im.creator_id as creator_id, ");
        sql.append("    im.updater_id as updater_id, ");
        sql.append("    to_char(im.created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at, ");
        sql.append("    to_char(im.updated_at, 'YYYY-MM-DD HH24:MI:SS') as updated_at ");
        sql.append("from tb_ac_item_master im ");
        sql.append("inner join tb_ac_item_category ic on ic.id = im.item_category_id ");
        sql.append("where im.item_code = :itemCode ");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("itemCode", itemCode);

        List<ItemMasterDetailView> rows = this.queryManager.selectListBySql(
                sql.toString(),
                params,
                ItemMasterDetailView.class,
                0,
                0
        );

        if (rows == null || rows.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "상품을 찾을 수 없습니다. itemCode=" + itemCode
            );
        }

        return rows.get(0);
    }

    /**
     * itemCode 기준 존재 여부 확인.
     */
    public boolean existsByItemCode(String itemCode) {
        String sql = ""
                + "select count(*) "
                + "from tb_ac_item_master "
                + "where item_code = :itemCode";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("itemCode", itemCode);

        Number count = this.queryManager.selectBySql(sql, params, Number.class);
        return count != null && count.longValue() > 0L;
    }

    /**
     * 엔티티 단건 조회.
     *
     * 용도:
     * - update/delete/toggle 시 실제 엔티티 수정
     */
    public TbAcItemMaster findEntityByItemCodeOrThrow(String itemCode) {
        String sql = ""
                + "select * "
                + "from tb_ac_item_master "
                + "where item_code = :itemCode";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("itemCode", itemCode);

        List<TbAcItemMaster> rows = this.queryManager.selectListBySql(
                sql,
                params,
                TbAcItemMaster.class,
                0,
                0
        );

        if (rows == null || rows.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "상품 엔티티를 찾을 수 없습니다. itemCode=" + itemCode
            );
        }

        return rows.get(0);
    }
}