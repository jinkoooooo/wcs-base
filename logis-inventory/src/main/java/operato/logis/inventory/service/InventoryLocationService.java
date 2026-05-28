package operato.logis.inventory.service;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.InventoryConstants;
import operato.logis.inventory.entity.TbInventoryLocation;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.lang.reflect.Field;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryLocationService extends AbstractQueryService {

    private final InventorySettingService inventorySettingService;

    public List<TbInventoryLocation> getLocationListByCondition(TbInventoryLocation condition) {
        StringBuilder sql = new StringBuilder("SELECT * FROM tb_inventory_location WHERE 1=1");
        Map<String, Object> param = new HashMap<>();

        if (ValueUtil.isNotEmpty(condition)) {
            // Reflection을 사용하여 객체 내부에 선언된 모든 필드(속성)를 가져옵니다.
            Field[] fields = condition.getClass().getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true); // private 속성에 접근할 수 있도록 허용

                try {
                    Object value = field.get(condition);

                    if (ValueUtil.isNotEmpty(value)) {
                        // String 타입인 경우 빈 문자열("")이면 무시
                        if (value instanceof String && ((String) value).isEmpty()) {
                            continue;
                        }

                        String fieldName = field.getName(); // camelCase
                        String columnName = fieldName.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(); // snake_case

                        sql.append(" AND ").append(columnName).append(" = :").append(fieldName);
                        param.put(fieldName, value);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        sql.append(" ORDER BY loc_code ASC");

        return this.queryManager.selectListBySql(sql.toString(), param, TbInventoryLocation.class, 0, 0);
    }

    public void setLocationProperties(List<TbInventoryLocation> locationList) {
        this.queryManager.updateBatch(locationList, "locGroup", "itemType", "itemGroup", "itemGrade", "maxHeight", "maxWeight", "isEnabled", "equipType", "equipCode", "destNodeCode", "isPath");
    }

    public TbInventoryLocation findByLocCode(String locCode) {
        String sql = "SELECT * FROM tb_inventory_location WHERE loc_code = :locCode";
        Map<String, Object> param = ValueUtil.newMap("locCode", locCode);
        return this.queryManager.selectBySql(sql, param, TbInventoryLocation.class);
    }

    public TbInventoryLocation findByStockId(String stockId) {
        String sql = "SELECT * FROM tb_inventory_location WHERE stock_id = :stockId";
        Map<String, Object> param = ValueUtil.newMap("stockId", stockId);
        return this.queryManager.selectBySql(sql, param, TbInventoryLocation.class);
    }

    public List<TbInventoryLocation> getLocationListByStockIdList(List<String> stockIdList) {
        if (ValueUtil.isEmpty(stockIdList)) {
            return null;
        }

        String sql = "SELECT * FROM tb_inventory_location WHERE stock_id IN (:stockIdList)";
        Map<String, Object> param = ValueUtil.newMap("stockIdList", stockIdList);

        return this.queryManager.selectListBySql(sql, param, TbInventoryLocation.class, 0, 0);
    }

    public List<TbInventoryLocation> getLineAllCells(String locCode) {
        // 현재 설비 기준 방향 설정값 조회
        boolean isRowStandard = InventoryConstants.ROW_DIRECTION.equals(inventorySettingService.getOptionValue(InventoryConstants.PATH_STANDARD));

        String fixedAxis = isRowStandard ? "loc_col" : "loc_row"; // 같은 라인으로 묶을 기준 축
        String sortAxis = isRowStandard ? "loc_row" : "loc_col";  // 양옆으로 뻗어나갈 정렬 축

        String sql = String.format("""
                SELECT target.*
                FROM tb_inventory_location target
                WHERE (target.loc_group, target.loc_level, target.loc_side, target.%s) = (
                    SELECT source.loc_group, source.loc_level, source.loc_side, source.%s
                    FROM tb_inventory_location source
                    WHERE source.loc_code = :locCode
                )
                ORDER BY target.%s ASC;
                """, fixedAxis, fixedAxis, sortAxis);
        Map<String, Object> param = ValueUtil.newMap("locCode", locCode);
        return this.queryManager.selectListBySql(sql, param, TbInventoryLocation.class, 0, 0);
    }
}