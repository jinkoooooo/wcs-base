package operato.logis.ecs.tspg4way.dashboard.generator.service;

import lombok.RequiredArgsConstructor;
import operato.logis.ecs.tspg4way.dashboard.generator.dto.CellOp;
import operato.logis.ecs.tspg4way.dashboard.generator.dto.RackCellSyncRequest;
import operato.logis.ecs.tspg4way.dashboard.generator.dto.RackCellSyncResult;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.ecs.tspg4way.entity.TbEqRackMst;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 재고 로케이션 동기화 서비스. tb_eq_rack_mst(랙 셀) → tb_inventory_location 단방향 동기화로 정합성 유지.
 * 랙 셀은 (eqGroupId→locGroup, eqId→rackEqId, cellId→locId) 3중 키로만 유일 식별. eqGroupId 추측 조회 금지(동명 eqId 오동작).
 */
@Service
@RequiredArgsConstructor
public class InventoryLocationSyncService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryLocationSyncService.class);

    private final IQueryManager queryManager;

    // 기본값 상수 (현장 정책에 맞게 조정)

    private static final String DEFAULT_ITEM_TYPE = "PALLET";
    private static final String DEFAULT_ITEM_GROUP = "PUBLIC";
    private static final int DEFAULT_ITEM_GRADE = 0;
    private static final int DEFAULT_MAX_HEIGHT = 3000;
    private static final int DEFAULT_MAX_WEIGHT = 3000;
    private static final int DEFAULT_LOC_DEEP = 0;
    private static final String DEFAULT_LOC_SIDE = "";
    private static final int DEFAULT_ACTIVE_TASK_COUNT = 0;

    // locType 문자열 상수
    private static final String LOC_TYPE_RACK = "RACK";
    private static final String LOC_TYPE_INBOUND_PORT = "INBOUND_PORT";
    private static final String LOC_TYPE_OUTBOUND_PORT = "OUTBOUND_PORT";
    private static final String LOC_TYPE_IN_OUTBOUND_PORT = "IN_OUTBOUND_PORT";
    private static final String LOC_TYPE_CHARGE_PORT = "CHARGE_PORT";
    private static final String LOC_TYPE_CHARGE_ENTER_PORT = "CHARGE_ENTER_PORT";
    private static final String LOC_TYPE_DRIVE_ONLY = "DRIVE_ONLY";

    // 생성 (랙 → 로케이션)

    /**
     * 단일 랙 셀에 대한 로케이션 생성
     *
     *호출자가 eqGroupId 를 명시적으로 전달해야 함
     * 중복 존재 시 skip (eqGroupId + eqId + cellId 3중 키 기준)
     *
     * @param rack 랙 셀 엔티티
     * @param eqGroupId 설비 그룹 ID (호출자가 확보한 정확한 값)
     * @return 생성된 로케이션 (이미 존재하면 기존 값)
     */
    @Transactional
    public ExtTbInventoryLocation createLocationForRack(TbEqRackMst rack, String eqGroupId) {
        if (rack == null) {
            throw new ElidomRuntimeException("랙 셀이 비어있습니다.");
        }
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("설비 그룹 ID가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(rack.getEqId())) {
            throw new ElidomRuntimeException("랙의 eqId가 비어있습니다.");
        }

        // 중복 체크: locGroup + rackEqId + locId 3중 키
        ExtTbInventoryLocation existing = findLocation(eqGroupId, rack.getEqId(), rack.getRackId());
        if (existing != null) {
            logger.warn("[ Generator ][ LocationSync ] Location already exists, skipping: locGroup={}, rackEqId={}, locId={}",
                    eqGroupId, rack.getEqId(), rack.getRackId());
            return existing;
        }

        ExtTbInventoryLocation loc = buildLocationFromRack(rack, eqGroupId);
        queryManager.insert(loc);

        logger.debug("[ Generator ][ LocationSync ] Created Location: locGroup={}, rackEqId={}, locId={}, locType={}, isEnabled={}, isPath={}",
                loc.getLocGroup(), loc.getRackEqId(), loc.getLocId(), loc.getLocType(),
                loc.getIsEnabled(), loc.getIsPath());

        return loc;
    }

    /**
     * 여러 랙 셀에 대한 로케이션 일괄 생성
     *
     * @param racks 랙 셀 리스트
     * @param eqGroupId 설비 그룹 ID (전체 공통)
     * @return 실제 생성된 개수 (이미 존재하던 것은 제외)
     */
    @Transactional
    public int createLocationsForRacks(List<TbEqRackMst> racks, String eqGroupId) {
        if (ValueUtil.isEmpty(racks)) {
            return 0;
        }
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("설비 그룹 ID가 비어있습니다.");
        }

        int createdCount = 0;
        for (TbEqRackMst rack : racks) {
            ExtTbInventoryLocation before = findLocation(eqGroupId, rack.getEqId(), rack.getRackId());
            createLocationForRack(rack, eqGroupId);
            if (before == null) {
                createdCount++;
            }
        }

        logger.info("[ Generator ][ LocationSync ] Bulk created {} Locations (total racks: {}, eqGroupId: {})",
                createdCount, racks.size(), eqGroupId);

        return createdCount;
    }

    // 삭제 (랙 → 로케이션)

    /**
     * 단일 랙 셀에 대응하는 로케이션 삭제
     *
     *eqGroupId + eqId + cellId 3중 키로 안전하게 식별하여 삭제
     *
     * @param eqGroupId 설비 그룹 ID
     * @param eqId 물리 랙 장비 ID
     * @param rackCellId 랙 셀 ID (예: "10601")
     * @return 삭제 성공 여부 (없으면 false)
     */
    @Transactional
    public boolean deleteLocationForRack(String eqGroupId, String eqId, String rackCellId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(eqId) || ValueUtil.isEmpty(rackCellId)) {
            return false;
        }

        ExtTbInventoryLocation loc = findLocation(eqGroupId, eqId, rackCellId);

        if (loc == null) {
            logger.warn("[ Generator ][ LocationSync ] Location not found for delete: locGroup={}, rackEqId={}, locId={}",
                    eqGroupId, eqId, rackCellId);
            return false;
        }

        queryManager.delete(ExtTbInventoryLocation.class, ValueUtil.newMap("id", loc.getId()));
        logger.info("[ Generator ][ LocationSync ] Deleted Location: locGroup={}, rackEqId={}, locId={}",
                eqGroupId, eqId, rackCellId);

        return true;
    }

    /**
     * 특정 (eqGroupId + eqId) 조합에 속한 모든 로케이션 삭제
     *
     *두 조건을 모두 걸어서 다른 그룹의 동명 eqId 를 건드리지 않도록 보호
     *
     * @param eqGroupId 설비 그룹 ID
     * @param eqId 물리 랙 장비 ID
     * @return 삭제된 개수
     */
    @Transactional
    public int deleteLocationsByEqId(String eqGroupId, String eqId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(eqId)) {
            return 0;
        }

        Query query = new Query();
        query.addFilter("locGroup", eqGroupId);
        query.addFilter("rackEqId", eqId);
        List<ExtTbInventoryLocation> locList = queryManager.selectList(ExtTbInventoryLocation.class, query);

        if (ValueUtil.isEmpty(locList)) {
            return 0;
        }

        queryManager.deleteBatch(locList);
        logger.info("[ Generator ][ LocationSync ] Deleted {} Locations for locGroup={}, rackEqId={}",
                locList.size(), eqGroupId, eqId);

        return locList.size();
    }

    // 조회 헬퍼

    /**
     *3중 키 조회: locGroup + rackEqId + locId
     *
     * 랙 셀을 유일하게 식별하는 유일한 조회 방법.
     */
    public ExtTbInventoryLocation findLocation(String locGroup, String rackEqId, String locId) {
        if (ValueUtil.isEmpty(locGroup) || ValueUtil.isEmpty(rackEqId) || ValueUtil.isEmpty(locId)) {
            return null;
        }
        return queryManager.selectByCondition(
                ExtTbInventoryLocation.class,
                ValueUtil.newMap("locGroup,rackEqId,locId", locGroup, rackEqId, locId)
        );
    }

    // 변환 로직 (Rack → Location)

    /**
     * TbEqRackMst → ExtTbInventoryLocation 변환
     */
    private ExtTbInventoryLocation buildLocationFromRack(TbEqRackMst rack, String eqGroupId) {
        EcsDBConsts.RackType rackType = EcsDBConsts.RackType.find(rack.getType());

        ExtTbInventoryLocation loc = new ExtTbInventoryLocation();

        // 식별 / 그룹 매핑 (3중 키)
        loc.setLocId(rack.getRackId());
        loc.setRackEqId(rack.getEqId());
        loc.setLocGroup(eqGroupId);

        // 좌표 매핑 (bay → col)
        loc.setLocRow(rack.getRow());
        loc.setLocCol(rack.getBay());
        loc.setLocLevel(rack.getLevel());
        loc.setLocDeep(DEFAULT_LOC_DEEP);
        loc.setLocSide(DEFAULT_LOC_SIDE);

        // 사용 / 경로 매핑
        loc.setIsEnabled(rack.isUseYn());
        loc.setIsPath(rack.isDriveOnlyYn());

        // 입출고 가능 여부 (랙 타입별)
        loc.setIsInboundEnabled(isInboundAllowed(rackType));
        loc.setIsOutboundEnabled(isOutboundAllowed(rackType));

        // 타입 정보
        loc.setLocType(resolveLocType(rackType, rack.isDriveOnlyYn()));
        loc.setItemType(DEFAULT_ITEM_TYPE);
        loc.setItemGroup(DEFAULT_ITEM_GROUP);
        loc.setItemGrade(DEFAULT_ITEM_GRADE);

        // 필수 NOT NULL 필드 기본값
        loc.setMaxHeight(DEFAULT_MAX_HEIGHT);
        loc.setMaxWeight(DEFAULT_MAX_WEIGHT);
        loc.setDestNodeCode(rack.getRackId()); // 기본은 자기 자신

        // WCS 확장 필드 기본값
        loc.setActiveTaskCount(DEFAULT_ACTIVE_TASK_COUNT);

        return loc;
    }

    /**
     * RackType → locType 문자열 매핑
     *
     * 주행 전용 셀이면 RackType 과 무관하게 "DRIVE_ONLY" 반환
     */
    private String resolveLocType(EcsDBConsts.RackType rackType, boolean driveOnlyYn) {
        if (driveOnlyYn) return LOC_TYPE_DRIVE_ONLY;
        if (rackType == null) return LOC_TYPE_RACK;

        return switch (rackType) {
            case CELL              -> LOC_TYPE_RACK;
            case INBOUND_PORT      -> LOC_TYPE_INBOUND_PORT;
            case OUTBOUND_PORT     -> LOC_TYPE_OUTBOUND_PORT;
            case IN_OUTBOUND_PORT  -> LOC_TYPE_IN_OUTBOUND_PORT;
            case CHARGE_PORT       -> LOC_TYPE_CHARGE_PORT;
            case CHARGE_ENTER_PORT -> LOC_TYPE_CHARGE_ENTER_PORT;
            default                -> LOC_TYPE_RACK;
        };
    }

    /**
     * 입고 허용 랙 타입 판정
     * 일반 셀 / 입고포트 / 입출고포트만 입고 허용
     */
    private boolean isInboundAllowed(EcsDBConsts.RackType rackType) {
        if (rackType == null) return false;
        return switch (rackType) {
            case CELL, INBOUND_PORT, IN_OUTBOUND_PORT -> true;
            default -> false;
        };
    }

    /**
     * 출고 허용 랙 타입 판정
     * 일반 셀 / 출고포트 / 입출고포트만 출고 허용
     */
    private boolean isOutboundAllowed(EcsDBConsts.RackType rackType) {
        if (rackType == null) return false;
        return switch (rackType) {
            case CELL, OUTBOUND_PORT, IN_OUTBOUND_PORT -> true;
            default -> false;
        };
    }

    // MapEditor 셀 단위 동기화 (DISABLE / ENABLE / CREATE)

    /**
     * MapEditor에서 보낸 셀 ops를 단일 트랜잭션으로 적용.
     * 사전 검증으로 거절된 셀은 ops에서 제외되어 나머지만 반영됨.
     */
    @Transactional
    public RackCellSyncResult applyCellOps(RackCellSyncRequest req) {
        if (req == null || ValueUtil.isEmpty(req.getEqGroupId()) || ValueUtil.isEmpty(req.getRackEqId())) {
            throw new ElidomRuntimeException("eqGroupId/rackEqId가 비어있습니다.");
        }
        RackCellSyncResult result = new RackCellSyncResult();
        if (ValueUtil.isEmpty(req.getOps())) {
            return result;
        }

        String eqGroupId = req.getEqGroupId();
        String rackEqId = req.getRackEqId();

        logger.info("[ Generator ][ CellSync ] apply: eqGroupId={}, rackEqId={}, ops={}",
                eqGroupId, rackEqId, req.getOps().size());

        for (CellOp op : req.getOps()) {
            if (op == null || op.getKind() == null || ValueUtil.isEmpty(op.getCellId())) {
                reject(result, op == null ? "?" : op.getCellId(), "kind/cellId 비어있음");
                continue;
            }
            if (!isValidCellId(op)) {
                reject(result, op.getCellId(), "cellId 형식 오류");
                continue;
            }

            switch (op.getKind()) {
                case DISABLE -> handleDisable(eqGroupId, rackEqId, op, result);
                case ENABLE  -> handleEnable(eqGroupId, rackEqId, op, result);
                case CREATE  -> handleCreate(eqGroupId, rackEqId, op, result);
            }
        }

        return result;
    }

    private void handleDisable(String eqGroupId, String rackEqId, CellOp op, RackCellSyncResult result) {
        if (isCellOccupied(eqGroupId, rackEqId, op.getCellId())) {
            reject(result, op.getCellId(), "점유 셀");
            return;
        }
        int updated = queryManager.executeBySql(
                "UPDATE tb_eq_rack_mst SET use_yn = false WHERE eq_id = :eqId AND rack_id = :rackId",
                ValueUtil.newMap("eqId,rackId", rackEqId, op.getCellId())
        );
        if (updated == 0) {
            reject(result, op.getCellId(), "eq_rack 행 없음");
            return;
        }
        deleteLocationForRack(eqGroupId, rackEqId, op.getCellId());
        result.setDisabled(result.getDisabled() + 1);
    }

    private void handleEnable(String eqGroupId, String rackEqId, CellOp op, RackCellSyncResult result) {
        TbEqRackMst rack = queryManager.selectByCondition(
                TbEqRackMst.class,
                ValueUtil.newMap("eqId,rackId", rackEqId, op.getCellId())
        );
        if (rack == null) {
            reject(result, op.getCellId(), "eq_rack 행 없음");
            return;
        }
        if (!rack.isUseYn()) {
            queryManager.executeBySql(
                    "UPDATE tb_eq_rack_mst SET use_yn = true WHERE eq_id = :eqId AND rack_id = :rackId",
                    ValueUtil.newMap("eqId,rackId", rackEqId, op.getCellId())
            );
            rack.setUseYn(true);
        }
        if (findLocation(eqGroupId, rackEqId, op.getCellId()) == null) {
            createLocationForRack(rack, eqGroupId);
        }
        result.setEnabled(result.getEnabled() + 1);
    }

    private void handleCreate(String eqGroupId, String rackEqId, CellOp op, RackCellSyncResult result) {
        if (op.getRackType() == null) {
            reject(result, op.getCellId(), "rackType 필수");
            return;
        }
        TbEqRackMst existing = queryManager.selectByCondition(
                TbEqRackMst.class,
                ValueUtil.newMap("eqId,rackId", rackEqId, op.getCellId())
        );
        if (existing != null) {
            reject(result, op.getCellId(), "이미 존재");
            return;
        }

        // id 는 UUID 서러게이트 자동 생성, 셀 식별은 rack_id 로만
        TbEqRackMst rack = new TbEqRackMst();
        rack.setEqId(rackEqId);
        rack.setRackId(op.getCellId());
        rack.setLevel(op.getLevel());
        rack.setRow(op.getRow());
        rack.setBay(op.getBay());
        rack.setType(op.getRackType());
        rack.setStatus(EcsDBConsts.EqRackStatus.READY.getValue());
        rack.setUseYn(true);
        queryManager.insert(rack);

        createLocationForRack(rack, eqGroupId);
        result.setCreated(result.getCreated() + 1);
    }

    /** stock 점유 / 진행중 오더 / 적재중 여부 통합 검사 */
    private boolean isCellOccupied(String eqGroupId, String rackEqId, String cellId) {
        ExtTbInventoryLocation loc = findLocation(eqGroupId, rackEqId, cellId);
        if (loc != null) {
            if (ValueUtil.isNotEmpty(loc.getStockId())) return true;
            if (loc.getActiveTaskCount() != null && loc.getActiveTaskCount() > 0) return true;
        }
        TbEqRackMst rack = queryManager.selectByCondition(
                TbEqRackMst.class,
                ValueUtil.newMap("eqId,rackId", rackEqId, cellId)
        );
        return rack != null && rack.getSkuQty() > 0;
    }

    /** {level}{row:02d}{bay:02d} 형식 + 분해값 일치 검증 */
    private boolean isValidCellId(CellOp op) {
        String id = op.getCellId();
        if (id == null || !id.matches("^\\d{5,6}$")) return false;
        String expected = String.format("%d%02d%02d", op.getLevel(), op.getRow(), op.getBay());
        return expected.equals(id);
    }

    private void reject(RackCellSyncResult result, String cellId, String reason) {
        result.getRejectedCellIds().add(cellId);
        result.getRejectReasons().add(reason);
        logger.warn("[ Generator ][ CellSync ] rejected: cellId={}, reason={}", cellId, reason);
    }

}
