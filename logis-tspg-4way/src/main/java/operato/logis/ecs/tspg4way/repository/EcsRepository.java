package operato.logis.ecs.tspg4way.repository;

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
/**
 *
 *
 *
 *  this.getQueryManager().update(layout);
 *  layout.setRealEqId(realEqId);
 *                 layout.setRealEqType(realEqType);
 *                 this.getQueryManager().update(layout, "realEqId,realEqType");
 *
 *
 *  String updateSql = "UPDATE tb_eq_cv_mst SET cargo_yn = :cargoYn WHERE eq_id = :eqId";
 *             this.getQueryManager().executeBySql(updateSql, ValueUtil.newMap("cargoYn,eqId", hasCargo, cvEqId));
 * String updateSql = """
 *                 UPDATE tb_eq_car_mst
 *                    SET row = :row,
 *                        bay = :bay,
 *                        level = :level,
 *                        status = :status,
 *                        cargo_yn = :cargoYn,
 *                        battery_status = :battery
 *                  WHERE eq_id = :eqId
 *                 """;
 *             Map<String, Object> updateParams = ValueUtil.newMap(
 *                     "row,bay,level,status,cargoYn,battery,eqId",
 *                     vs.row, vs.bay, vs.floor, vs.statusCode, vs.hasCargo, vs.battery, vs.eqId
 *             );
 *             this.getQueryManager().executeBySql(updateSql, updateParams);
 *
 insert into tb_eq_rack_mst
 values
 ('10101', 'RACK_1', 1, 1, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10102', 'RACK_1', 1, 2, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10103', 'RACK_1', 1, 3, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10104', 'RACK_1', 1, 4, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10201', 'RACK_1', 2, 1, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10202', 'RACK_1', 2, 2, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10203', 'RACK_1', 2, 3, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10204', 'RACK_1', 2, 4, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10301', 'RACK_1', 3, 1, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10302', 'RACK_1', 3, 2, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10303', 'RACK_1', 3, 3, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10304', 'RACK_1', 3, 4, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10401', 'RACK_1', 4, 1, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10402', 'RACK_1', 4, 2, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10403', 'RACK_1', 4, 3, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10404', 'RACK_1', 4, 4, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10500', 'RACK_1', 5, 0, 0, null, null, 0, null, null, 'false', 7,  null, null, now(), now() ,31 ),
 ('10501', 'RACK_1', 5, 1, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,32 ),
 ('10502', 'RACK_1', 5, 2, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10503', 'RACK_1', 5, 3, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10504', 'RACK_1', 5, 4, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10601', 'RACK_1', 6, 1, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,22 ),
 ('10602', 'RACK_1', 6, 2, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,22 ),
 ('10603', 'RACK_1', 6, 3, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10604', 'RACK_1', 6, 4, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('10703', 'RACK_1', 7, 3, 1, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),

 ('20101', 'RACK_1', 1, 1, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20102', 'RACK_1', 1, 2, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20103', 'RACK_1', 1, 3, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20104', 'RACK_1', 1, 4, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20201', 'RACK_1', 2, 1, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20202', 'RACK_1', 2, 2, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20203', 'RACK_1', 2, 3, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20204', 'RACK_1', 2, 4, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20301', 'RACK_1', 3, 1, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20302', 'RACK_1', 3, 2, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20303', 'RACK_1', 3, 3, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20304', 'RACK_1', 3, 4, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20401', 'RACK_1', 4, 1, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20402', 'RACK_1', 4, 2, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20403', 'RACK_1', 4, 3, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20404', 'RACK_1', 4, 4, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20500', 'RACK_1', 5, 0, 0, null, null, 0, null, null, 'false', 7,  null, null, now(), now() ,31 ),
 ('20501', 'RACK_1', 5, 1, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,32 ),
 ('20502', 'RACK_1', 5, 2, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20503', 'RACK_1', 5, 3, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20504', 'RACK_1', 5, 4, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20601', 'RACK_1', 6, 1, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20602', 'RACK_1', 6, 2, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20603', 'RACK_1', 6, 3, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20604', 'RACK_1', 6, 4, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 ),
 ('20703', 'RACK_1', 7, 3, 2, null, null, 0, null, null, 'true', 7,  null, null, now(), now() ,11 );

 insert into tb_eq_cv_mst(id, eq_id, type, status, use_yn)
 values
 ('101', 'CV_1', 2, 0, 'true'),
 ('102', 'CV_1', 11, 0, 'true'),
 ('103', 'CV_1', 12, 0, 'true'),
 ('104', 'CV_1', 12, 0, 'true');

 */