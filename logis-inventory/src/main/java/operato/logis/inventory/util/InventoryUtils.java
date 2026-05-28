package operato.logis.inventory.util;

import operato.logis.inventory.dto.ItemIdentifierDto;
import operato.logis.inventory.entity.TbInventoryLocation;

import java.util.List;
import java.util.Map;

public class InventoryUtils {

    /**
     * 조건에 맞는 전체 품목 정보 입력 시 중복되는 IN 절 생성 로직 공통 메서드
     *
     * @param sql 조건문 이전까지 작성된 SQL
     * @param param queryManager에 전달할 Parameter
     * @param itemList 조건에 맞는 전체 품목 정보
     */
    public static void appendItemInClause(StringBuilder sql, Map<String, Object> param, List<ItemIdentifierDto> itemList) {
        sql.append("WHERE (item_owner, item_code) IN (");
        for (int i = 0; i < itemList.size(); i++) {
            ItemIdentifierDto item = itemList.get(i);

            sql.append(String.format("(:owner_%d, :code_%d)", i, i));
            if (i < itemList.size() - 1) {
                sql.append(", ");
            }

            param.put("owner_" + i, item.getItemOwner());
            param.put("code_" + i, item.getItemCode());
        }
        sql.append(") ");
    }

    /**
     * TbInventoryLocation에 적합한 맨해튼 거리 계산 공식
     * 거리 = |Col차이| + |Row차이| + |Level차이|
     * 
     * @param loc 시작 Location
     * @param targetCol 목적 열
     * @param targetRow 목적 행
     * @param targetLevel 목적 단
     * @return 관련 재고의 중심점과 가장 가까운 로케이션
     */
    public static double calculateManhattanDistance(TbInventoryLocation loc, double targetCol, double targetRow, double targetLevel) {
        return Math.abs(loc.getLocCol() - targetCol) +
                Math.abs(loc.getLocRow() - targetRow) +
                Math.abs(loc.getLocLevel() - targetLevel);
    }
}