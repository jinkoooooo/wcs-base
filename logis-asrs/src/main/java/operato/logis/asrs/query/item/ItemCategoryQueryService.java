package operato.logis.asrs.query.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import operato.logis.asrs.query.item.model.ItemCategoryOptionView;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 상품 카테고리 조회 서비스.
 *
 * 목적:
 * - 상품 등록/수정 화면의 카테고리 선택 옵션 조회
 * - categoryCode -> categoryId resolve 지원
 *
 * 주의:
 * - AbstractQueryService import 경로는 현재 elidom 기준으로 작성
 * - 프로젝트 실제 패키지가 다르면 해당 import만 맞춰서 변경
 */
@Service
public class ItemCategoryQueryService extends AbstractQueryService {

    /**
     * 활성 카테고리 목록 조회.
     */
    public List<ItemCategoryOptionView> findActiveCategories() {
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    id as id, ");
        sql.append("    category_code as category_code, ");
        sql.append("    category_name as category_name, ");
        sql.append("    default_operation_profile_id as default_operation_profile_id, ");
        sql.append("    description as description, ");
        sql.append("    active_yn as active_yn ");
        sql.append("from tb_ac_item_category ");
        sql.append("where active_yn = 'Y' ");
        sql.append("order by category_code asc ");

        return this.queryManager.selectListBySql(
                sql.toString(),
                new HashMap<String, Object>(),
                ItemCategoryOptionView.class,
                0,
                0
        );
    }

    /**
     * categoryCode -> categoryId resolve.
     *
     * 용도:
     * - create/update/bulk upsert 시 code 기반 입력을 FK id로 해석
     */
    public String findCategoryIdByCode(String categoryCode) {
        String sql = ""
                + "select id "
                + "from tb_ac_item_category "
                + "where category_code = :categoryCode";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("categoryCode", categoryCode);

        return this.queryManager.selectBySql(sql, params, String.class);
    }
}