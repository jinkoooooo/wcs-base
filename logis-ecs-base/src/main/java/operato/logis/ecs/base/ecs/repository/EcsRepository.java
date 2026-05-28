package operato.logis.ecs.base.ecs.repository;

import org.springframework.stereotype.Repository;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Repository
public class EcsRepository extends AbstractQueryService {

    public <T> List<T> findAll(Class<T> clazz, String tableName) {

        String sql = """
                SELECT *
                FROM %s
                """.formatted(tableName);
        return this.queryManager.selectListBySql(sql, null, clazz, 0, 0);
    }

    public List<Integer> findDriveOnlyLineByEqIdAndLevel(String eqId, int level) {
        String sql = """
                SELECT row
                FROM tb_eq_rack_mst
                WHERE 1=1
                AND eq_id = :eqId
                AND level = :level
                AND drive_only_yn = 'true'
                GROUP BY row;
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId,level", eqId, level);
        return this.queryManager.selectListBySql(sql, params, Integer.class, 0, 0);
    }
}