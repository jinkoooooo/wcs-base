package operato.logis.wcs.rest.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import operato.logis.wcs.entity.TbWcsShuttleOrder;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.util.ValueUtil;

/**
 * KMAT 2026 등 검증용 테스트 인프라(Equipment Group / Rack / Location) 세팅·정리 전용 컨트롤러.
 */
@RestController
@RequestMapping("/rest/wcs/direct-order")
public class WcsOrderTestSetupController extends AbstractRestService {

    private static final Logger logger = LoggerFactory.getLogger(WcsOrderTestSetupController.class);

    /**
     * KMAT 2026 테스트 기초 데이터 생성 (Equipment Group, Rack, Loc)
     * POST /rest/wcs/direct-order/setup-test-data
     */
    @PostMapping("/setup-test-data")
    public ResponseEntity<String> setupTestData() {
        try {
            // 1. 기존 데이터 정리 (멱등성 확보)
            this.clearTestData();

            String eqGroupId = "ZONE-TEST-01";
            String rackEqId = "RACK-TEST-01";

            // 2. Equipment Group 생성 (설비 구역)
            String groupSql = "INSERT INTO tb_eq_group_mst (id, name, type, domain_id) VALUES (:id, :name, 'ZONE', 1)";
            queryManager.executeBySql(groupSql, ValueUtil.newMap("id,name", eqGroupId, "KMAT 2026 테스트 존"));

            // 3. 물리 랙(Rack) & 로케이션(Loc) 마스터 생성

            // [A] 입출구 포트 시리즈 (drive_only=true 로 설정하여 이동 통로 확보)
            insertRackAndLoc("IN-PORT-01", eqGroupId, rackEqId, 0, 0, 0, "INBOUND_PORT", 0, true);
            insertRackAndLoc("OUT-PORT-01", eqGroupId, rackEqId, 99, 0, 0, "OUTBOUND_PORT", 0, true);
            insertRackAndLoc("IO-PORT-01", eqGroupId, rackEqId, 50, 0, 0, "IN_OUTBOUND_PORT", 0, true);

            // [B] RACK 셀 10개 (실제 적치 공간)
            for (int i = 1; i <= 10; i++) {
                String cellId = String.format("CELL-%03d", i);

                // 5번 셀만 테스트를 위해 재고가 있는 상태(OCCUPIED=1), 나머지는 EMPTY(0)
                int status = (i == 5) ? 1 : 0;

                // 일반 랙 공간 (drive_only=false)
                insertRackAndLoc(cellId, eqGroupId, rackEqId, 1, i, 1, "RACK", status, false);
            }

            // [C] 특수 주행 전용 라인 추가 (ㅁ 샌드위치 탈출 테스트용)
            insertRackAndLoc("DRIVE-WAY-01", eqGroupId, rackEqId, 2, 5, 1, "RACK", 0, true);

            return ResponseEntity.ok("KMAT 2026 Test infrastructure setup completed (14 Locations).");
        } catch (Exception e) {
            logger.error("[ Order ][ TestSetup ] setup failed", e);
            return ResponseEntity.internalServerError().body("Setup failed: " + e.getMessage());
        }
    }

    /**
     * 랙 마스터(물리)와 로케이션 마스터(논리) 통합 인서트 헬퍼
     */
    private void insertRackAndLoc(String id, String groupId, String rackEqId, int r, int b, int l, String locType, int status, boolean driveOnly) {
        String rackSql = """
                INSERT INTO tb_eq_rack_mst (id, eq_id, "row", bay, "level", use_yn, drive_only_yn, domain_id)
                VALUES (:id, :eqId, :row, :bay, :level, true, :driveOnly, 1)
                """;
        queryManager.executeBySql(rackSql, ValueUtil.newMap("id,eqId,row,bay,level,driveOnly",
                id, rackEqId, r, b, l, driveOnly));

        String locSql = """
                INSERT INTO tb_inventory_location (id, loc_id, loc_group, rack_eq_id, loc_type, is_enabled, domain_id)
                VALUES (uuid_generate_v4(), :locId, :groupId, :rackId, :locType, true, 1)
                """;
        queryManager.executeBySql(locSql, ValueUtil.newMap("locId,groupId,rackId,locType",
                id, groupId, rackEqId, locType));
    }

    /**
     * 테스트 데이터 일괄 삭제
     */
    @DeleteMapping("/clear-test-data")
    public ResponseEntity<String> clearTestDataApi() {
        try {
            clearTestData();
            return ResponseEntity.ok("Test data cleared.");
        } catch (Exception e) {
            logger.error("[ Order ][ TestSetup ] clear failed", e);
            return ResponseEntity.internalServerError().body("Clear failed: " + e.getMessage());
        }
    }

    private void clearTestData() {
        String zoneId = "ZONE-TEST-01";
        String rackEqId = "RACK-TEST-01";

        // 데이터 정합성을 위해 하위 실적 데이터부터 삭제
        queryManager.executeBySql("DELETE FROM tb_wcs_shuttle_order_item WHERE order_key IN (SELECT order_key FROM tb_wcs_shuttle_order WHERE eq_group_id = :zoneId)", ValueUtil.newMap("zoneId", zoneId));
        queryManager.executeBySql("DELETE FROM tb_wcs_shuttle_order WHERE eq_group_id = :zoneId", ValueUtil.newMap("zoneId", zoneId));
        queryManager.executeBySql("DELETE FROM tb_wcs_host_order_item WHERE host_order_key IN (SELECT host_order_key FROM tb_wcs_host_order WHERE eq_group_id = :zoneId)", ValueUtil.newMap("zoneId", zoneId));
        queryManager.executeBySql("DELETE FROM tb_wcs_host_order WHERE eq_group_id = :zoneId", ValueUtil.newMap("zoneId", zoneId));

        // 마스터 데이터 삭제
        queryManager.executeBySql("DELETE FROM tb_inventory_location WHERE eq_group_id = :zoneId", ValueUtil.newMap("zoneId", zoneId));
        queryManager.executeBySql("DELETE FROM tb_eq_rack_mst WHERE eq_id = :rackEqId", ValueUtil.newMap("rackEqId", rackEqId));
        queryManager.executeBySql("DELETE FROM tb_eq_group_mst WHERE id = :zoneId", ValueUtil.newMap("zoneId", zoneId));

        logger.info("[ Order ][ TestSetup ] cleared - zoneId=ZONE-TEST-01");
    }

    @Override
    protected Class<?> entityClass() {
        return TbWcsShuttleOrder.class;
    }
}
