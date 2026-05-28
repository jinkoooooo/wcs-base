package operato.logis.ecs.tspg4way.dashboard.generator.service;

import lombok.RequiredArgsConstructor;
import operato.logis.ecs.tspg4way.dashboard.entity.TbEcs2dItem;
import operato.logis.ecs.tspg4way.dashboard.entity.TbEcs2dPage;
import operato.logis.ecs.tspg4way.dashboard.generator.dto.*;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.ecs.tspg4way.entity.*;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.ValueUtil;

import java.util.*;

/**
 * 설비 생성 서비스 (역매핑). 2D 레이아웃 에디터 설정을 실운영 테이블(tb_eq_*)에 생성.
 * 하위 설비(랙/컨베이어/셔틀카)는 (eqGroupId + eqId) 쌍으로 식별. createRackCellsGrid 는 층(level) 범위를 받아 층마다 별도 페이지에 Row×Bay 셀 생성.
 * 랙 셀 ID 규칙: {level}{row:02d}{bay:02d}. 랙 셀 생성/삭제 시 tb_inventory_location 동기화는 InventoryLocationSyncService 에 위임.
 */
@Service
@RequiredArgsConstructor
public class EquipmentGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentGeneratorService.class);

    private final IQueryManager queryManager;
    private final InventoryLocationSyncService inventoryLocationSyncService;

    // 2D Dashboard 상수

    private static final int CELL_SIZE = 100;
    private static final int CANVAS_MARGIN = 100;
    private static final int DEFAULT_GRID_SIZE = 20;

    // 1. 설비 그룹 (tb_eq_group_mst)

    @Transactional
    public TbEqGroupMst createEqGroup(EqGroupCreateRequest request) {
        logger.info("[ Generator ][ Service ] Creating EqGroup: id={}, name={}", request.getId(), request.getName());

        if (queryManager.select(TbEqGroupMst.class, request.getId()) != null) {
            throw new ElidomRuntimeException("이미 존재하는 설비 그룹 ID입니다: " + request.getId());
        }

        TbEqGroupMst eqGroup = new TbEqGroupMst();
        eqGroup.setId(request.getId());
        eqGroup.setName(request.getName());
        eqGroup.setType(request.getType());

        queryManager.insert(eqGroup);
        logger.info("[ Generator ][ Service ] Created EqGroup: {}", eqGroup.getId());

        return eqGroup;
    }

    @Transactional(readOnly = true)
    public List<TbEqGroupMst> getAllEqGroups() {
        return queryManager.selectList(TbEqGroupMst.class, new Query());
    }

    @Transactional
    public TbEqGroupMst updateEqGroup(String id, EqGroupCreateRequest request) {
        TbEqGroupMst eqGroup = queryManager.select(TbEqGroupMst.class, id);
        if (eqGroup == null) {
            throw new ElidomRuntimeException("존재하지 않는 설비 그룹입니다: " + id);
        }

        List<String> updateFields = new ArrayList<>();

        if (ValueUtil.isNotEmpty(request.getName())) {
            eqGroup.setName(request.getName());
            updateFields.add("name");
        }
        if (ValueUtil.isNotEmpty(request.getType())) {
            eqGroup.setType(request.getType());
            updateFields.add("type");
        }

        if (!updateFields.isEmpty()) {
            updateFields.add("updatedAt");
            queryManager.update(eqGroup, updateFields.toArray(new String[0]));
            logger.info("[ Generator ][ Service ] Updated EqGroup: {}", id);
        }

        return eqGroup;
    }

    @Transactional
    public void deleteEqGroup(String eqGroupId) {
        Query query = new Query();
        query.addFilter("eqGroupId", eqGroupId);

        if (!queryManager.selectList(TbEqMst.class, query).isEmpty()) {
            throw new ElidomRuntimeException("하위 설비가 존재하여 삭제할 수 없습니다.");
        }

        queryManager.delete(TbEqGroupMst.class, ValueUtil.newMap("id", eqGroupId));
        logger.info("[ Generator ][ Service ] Deleted EqGroup: {}", eqGroupId);
    }

    // 2. 기본 설비 (tb_eq_mst) — (eqGroupId + eqId) 쌍 기반

    @Transactional
    public TbEqMst createEqMst(EqMstCreateRequest request) {
        logger.info("[ Generator ][ Service ] Creating EqMst + EqPlcMst: id={}, eqGroupId={}, type={}, plcId={}",
                request.getId(), request.getEqGroupId(), request.getType(), request.getPlcId());

        if (ValueUtil.isEmpty(request.getEqGroupId())) {
            throw new ElidomRuntimeException("설비 그룹이 비어있습니다.");
        }
        if (ValueUtil.isEmpty(request.getId())) {
            throw new ElidomRuntimeException("설비 ID가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(request.getName())) {
            throw new ElidomRuntimeException("설비명이 비어있습니다.");
        }
        if (ValueUtil.isEmpty(request.getType())) {
            throw new ElidomRuntimeException("설비 타입이 비어있습니다.");
        }

        Boolean usePlc = request.getType() == EcsDBConsts.EqType.CONVEYOR.getValue()
                || request.getType() == EcsDBConsts.EqType.SHUTTLE_CAR.getValue();

        if (usePlc && ValueUtil.isEmpty(request.getPlcId())) {
            throw new ElidomRuntimeException("plc id 가 비어있습니다.");
        }

        if (queryManager.select(TbEqGroupMst.class, request.getEqGroupId()) == null) {
            throw new ElidomRuntimeException("존재하지 않는 설비 그룹입니다: " + request.getEqGroupId());
        }

        if (queryManager.selectByCondition(
                TbEqMst.class,
                ValueUtil.newMap("eqGroupId,id", request.getEqGroupId(), request.getId())
        ) != null) {
            throw new ElidomRuntimeException(
                    "이미 존재하는 기본 설비 ID입니다: eqGroupId=" + request.getEqGroupId() + ", id=" + request.getId());
        }

        if (ValueUtil.isNotEmpty(request.getPlcId())
                && queryManager.select(TbEqPlcMst.class, request.getPlcId()) != null) {
            throw new ElidomRuntimeException("이미 존재하는 PLC ID입니다: " + request.getPlcId());
        }

        TbEqMst eqMst = new TbEqMst();
        eqMst.setId(request.getId());
        eqMst.setEqGroupId(request.getEqGroupId());
        eqMst.setName(request.getName());
        eqMst.setType(request.getType());
        eqMst.setPlcId(request.getPlcId());

        queryManager.insert(eqMst);
        logger.info("[ Generator ][ Service ] Created EqMst: eqGroupId={}, id={}, plcId={}",
                eqMst.getEqGroupId(), eqMst.getId(), eqMst.getPlcId());

        if (usePlc) {
            EcsDBConsts.EqType eqType = EcsDBConsts.EqType.find(request.getType());
            if (eqType == null) {
                throw new ElidomRuntimeException("설비 타입이 맞지 않습니다.");
            }

            int plcEqType;
            if (eqType.getValue() == EcsDBConsts.EqType.CONVEYOR.getValue()) {
                plcEqType = EcsDBConsts.PlcEqType.CONVEYOR_AND_LIFT.getValue();
            } else if (eqType.getValue() == EcsDBConsts.EqType.SHUTTLE_CAR.getValue()) {
                plcEqType = EcsDBConsts.PlcEqType.SHUTTLE_CAR.getValue();
            } else {
                throw new ElidomRuntimeException("Shuttle , conveyor 외에 plc 연결은 안됩니다.");
            }

            TbEqPlcMst plcMst = new TbEqPlcMst();
            plcMst.setId(request.getPlcId());
            plcMst.setName(request.getPlcName());
            plcMst.setIp(request.getPlcIp());
            plcMst.setPort(request.getPlcPort());
            plcMst.setPlcIfType(request.getPlcIfType());
            plcMst.setPlcEqType(plcEqType);
            plcMst.setConnectYn(request.isConnectYn());
            plcMst.setUseYn(request.isUseYn());

            queryManager.insert(plcMst);
            logger.info("[ Generator ][ Service ] Created EqPlcMst: {}", plcMst.getId());
        }

        return eqMst;
    }

    @Transactional(readOnly = true)
    public List<TbEqMst> getEqMstByGroup(String eqGroupId) {
        Query query = new Query();
        query.addFilter("eqGroupId", eqGroupId);
        return queryManager.selectList(TbEqMst.class, query);
    }

    @Transactional(readOnly = true)
    public EqMstDetailResponse getEqMstDetail(String eqGroupId, String id) {
        TbEqMst eqMst = findEqMstOrThrow(eqGroupId, id);

        EqMstDetailResponse response = new EqMstDetailResponse();
        response.setId(eqMst.getId());
        response.setEqGroupId(eqMst.getEqGroupId());
        response.setName(eqMst.getName());
        response.setType(eqMst.getType());
        response.setPlcId(eqMst.getPlcId());

        if (ValueUtil.isNotEmpty(eqMst.getPlcId())) {
            TbEqPlcMst plcMst = queryManager.select(TbEqPlcMst.class, eqMst.getPlcId());
            if (plcMst != null) {
                response.setPlcName(plcMst.getName());
                response.setPlcIp(plcMst.getIp());
                response.setPlcPort(plcMst.getPort());
                response.setPlcIfType(plcMst.getPlcIfType());
                response.setPlcEqType(plcMst.getPlcEqType());
                response.setConnectYn(plcMst.isConnectYn());
                response.setUseYn(plcMst.isUseYn());
            }
        }
        return response;
    }

    @Transactional
    public TbEqMst updateEqMst(String eqGroupId, String id, EqMstCreateRequest request) {
        TbEqMst eqMst = findEqMstOrThrow(eqGroupId, id);

        List<String> eqUpdateFields = new ArrayList<>();
        if (ValueUtil.isNotEmpty(request.getName())) {
            eqMst.setName(request.getName());
            eqUpdateFields.add("name");
        }

        if (!eqUpdateFields.isEmpty()) {
            eqUpdateFields.add("updatedAt");
            queryManager.update(eqMst, eqUpdateFields.toArray(new String[0]));
            logger.info("[ Generator ][ Service ] Updated EqMst: eqGroupId={}, id={}", eqGroupId, id);
        }

        if (ValueUtil.isNotEmpty(eqMst.getPlcId())) {
            TbEqPlcMst plcMst = queryManager.select(TbEqPlcMst.class, eqMst.getPlcId());
            if (plcMst != null) {
                List<String> plcFields = new ArrayList<>();

                if (ValueUtil.isNotEmpty(request.getPlcName())) {
                    plcMst.setName(request.getPlcName());
                    plcFields.add("name");
                }
                if (ValueUtil.isNotEmpty(request.getPlcIp())) {
                    plcMst.setIp(request.getPlcIp());
                    plcFields.add("ip");
                }
                if (ValueUtil.isNotEmpty(request.getPlcPort())) {
                    plcMst.setPort(request.getPlcPort());
                    plcFields.add("port");
                }
                if (ValueUtil.isNotEmpty(request.getPlcIfType())) {
                    plcMst.setPlcIfType(request.getPlcIfType());
                    plcFields.add("plcIfType");
                }

                plcMst.setConnectYn(request.isConnectYn());
                plcFields.add("connectYn");

                plcMst.setUseYn(request.isUseYn());
                plcFields.add("useYn");

                if (!plcFields.isEmpty()) {
                    plcFields.add("updatedAt");
                    queryManager.update(plcMst, plcFields.toArray(new String[0]));
                    logger.info("[ Generator ][ Service ] Updated EqPlcMst: {}", plcMst.getId());
                }
            }
        }
        return eqMst;
    }

    @Transactional
    public void deleteEqMst(String eqGroupId, String eqId) {
        TbEqMst targetEq = findEqMstOrThrow(eqGroupId, eqId);

        Query rackQuery = new Query();
        rackQuery.addFilter("eqId", eqId);
        if (!queryManager.selectList(TbEqRackMst.class, rackQuery).isEmpty()) {
            throw new IllegalStateException("하위 랙 셀이 존재합니다.");
        }

        Query cvQuery = new Query();
        cvQuery.addFilter("eqId", eqId);
        if (!queryManager.selectList(TbEqCvMst.class, cvQuery).isEmpty()) {
            throw new IllegalStateException("하위 컨베이어가 존재합니다.");
        }

        Query carQuery = new Query();
        carQuery.addFilter("eqId", eqId);
        if (!queryManager.selectList(TbEqCarMst.class, carQuery).isEmpty()) {
            throw new IllegalStateException("하위 셔틀카가 존재합니다.");
        }

        String plcId = targetEq.getPlcId();

        queryManager.delete(TbEqMst.class, ValueUtil.newMap("eqGroupId,id", eqGroupId, eqId));
        logger.info("[ Generator ][ Service ] Deleted EqMst: eqGroupId={}, id={}", eqGroupId, eqId);

        if (ValueUtil.isNotEmpty(plcId)) {
            TbEqPlcMst plc = queryManager.select(TbEqPlcMst.class, plcId);
            if (plc != null) {
                queryManager.delete(TbEqPlcMst.class, ValueUtil.newMap("id", plcId));
                logger.info("[ Generator ][ Service ] Deleted EqPlcMst: {}", plcId);
            }
        }
    }

    // 3. 랙 셀 (tb_eq_rack_mst) — (eqGroupId + eqId) 쌍 기반

    @Transactional
    public TbEqRackMst createRackCell(String eqGroupId, RackCellCreateRequest request) {
        logger.info("[ Generator ][ Service ] Creating RackCell: eqGroupId={}, eqId={}, id={}, row={}, bay={}, level={}",
                eqGroupId, request.getEqId(), request.getId(),
                request.getRow(), request.getBay(), request.getLevel());

        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("설비 그룹 ID가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(request.getEqId())) {
            throw new ElidomRuntimeException("기본 설비 ID가 비어있습니다.");
        }

        findEqMstOrThrow(eqGroupId, request.getEqId());

        if (queryManager.selectByCondition(
                TbEqRackMst.class,
                ValueUtil.newMap("eqId,rackId", request.getEqId(), request.getId())
        ) != null) {
            throw new ElidomRuntimeException(
                    "이미 존재하는 셀 ID입니다: eqId=" + request.getEqId() + ", id=" + request.getId());
        }

        TbEqRackMst rackCell = new TbEqRackMst();
        rackCell.setRackId(request.getId());
        rackCell.setEqId(request.getEqId());
        rackCell.setType(request.getType());
        rackCell.setRow(request.getRow());
        rackCell.setBay(request.getBay());
        rackCell.setLevel(request.getLevel());
        rackCell.setDriveOnlyYn(request.isDriveOnlyYn());
        rackCell.setUseYn(request.isUseYn());
        rackCell.setStatus(EcsDBConsts.EqRackStatus.READY.getValue());

        queryManager.insert(rackCell);
        logger.info("[ Generator ][ Service ] Created RackCell: {}", rackCell.getRackId());

        inventoryLocationSyncService.createLocationForRack(rackCell, eqGroupId);

        return rackCell;
    }

    @Transactional
    public List<TbEqRackMst> createRackCellsGrid(RackBulkCreateRequest request) {
        logger.info("[ Generator ][ Service ] Creating RackCells (GRID): eqGroupId={}, eqId={}, rows={}-{}, bays={}-{}, levels={}-{}, create2dItems={}",
                request.getEqGroupId(), request.getEqId(),
                request.getStartRow(), request.getEndRow(),
                request.getStartBay(), request.getEndBay(),
                request.getStartLevel(), request.getEndLevel(),
                request.isCreate2dItems());

        if (ValueUtil.isEmpty(request.getEqGroupId())) {
            throw new ElidomRuntimeException("설비 그룹 ID가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(request.getEqId())) {
            throw new ElidomRuntimeException("기본 설비가 비어있습니다.");
        }

        //층 범위 정규화 (start > end 이면 swap)
        int startLevel = Math.min(request.getStartLevel(), request.getEndLevel());
        int endLevel = Math.max(request.getStartLevel(), request.getEndLevel());
        if (startLevel < 1) {
            throw new ElidomRuntimeException("층(level)은 1 이상이어야 합니다.");
        }

        findEqMstOrThrow(request.getEqGroupId(), request.getEqId());

        List<TbEqRackMst> allCreatedCells = new ArrayList<>();

        Set<Integer> driveOnlyRows = toSet(request.getDriveOnlyRows());
        Set<Integer> driveOnlyBays = toSet(request.getDriveOnlyBays());

        Map<String, RackBulkCreateRequest.SpecialCellConfig> specialCellMap
                = buildSpecialCellMap(request.getSpecialCells());

        boolean shouldCreate2dItems = request.isCreate2dItems();
        if (shouldCreate2dItems && ValueUtil.isEmpty(request.getLcId())) {
            throw new ElidomRuntimeException("2D 아이템 생성을 위해서는 lcId가 필요합니다.");
        }

        String locEqGroupId = request.getEqGroupId();

        //층 단위 루프 — 각 층마다 독립 페이지
        for (int level = startLevel; level <= endLevel; level++) {
            logger.info("[ Generator ][ Service ] Processing level {}/{}: rows={}-{}, bays={}-{}",
                    level, endLevel, request.getStartRow(), request.getEndRow(),
                    request.getStartBay(), request.getEndBay());

            // 이 층의 페이지 확보 (create2dItems == true 일 때만)
            TbEcs2dPage page2d = null;
            if (shouldCreate2dItems) {
                page2d = findOrCreatePage2dForLevel(request, level);
            }

            // 이 층에서 실제 생성된 셀 범위 추적 (페이지 canvas 업데이트용)
            Integer createdMinRow = null, createdMaxRow = null;
            Integer createdMinBay = null, createdMaxBay = null;
            int levelCreatedCount = 0;

            for (int row = request.getStartRow(); row <= request.getEndRow(); row++) {
                for (int bay = request.getStartBay(); bay <= request.getEndBay(); bay++) {
                    String cellId = generateRackCellId(level, row, bay);

                    if (queryManager.selectByCondition(TbEqRackMst.class,
                            ValueUtil.newMap("eqId,rackId", request.getEqId(), cellId)) != null) {
                        logger.warn("[ Generator ][ Service ] RackCell already exists, skipping: eqId={}, id={}",
                                request.getEqId(), cellId);
                        continue;
                    }

                    String specialKey = row + "-" + bay;
                    RackBulkCreateRequest.SpecialCellConfig specialConfig = specialCellMap.get(specialKey);

                    boolean isDriveOnly = driveOnlyRows.contains(row) || driveOnlyBays.contains(bay);
                    if (specialConfig != null && specialConfig.getDriveOnlyYn() != null) {
                        isDriveOnly = specialConfig.getDriveOnlyYn();
                    }

                    int cellType = request.getRackType();
                    if (specialConfig != null && specialConfig.getType() > 0) {
                        cellType = specialConfig.getType();
                    }

                    TbEqRackMst rackCell = new TbEqRackMst();
                    rackCell.setRackId(cellId);
                    rackCell.setEqId(request.getEqId());
                    rackCell.setType(cellType);
                    rackCell.setRow(row);
                    rackCell.setBay(bay);
                    rackCell.setLevel(level); //현재 루프의 level
                    rackCell.setDriveOnlyYn(isDriveOnly);
                    rackCell.setUseYn(request.isUseYn());
                    rackCell.setStatus(EcsDBConsts.EqRackStatus.READY.getValue());

                    queryManager.insert(rackCell);
                    allCreatedCells.add(rackCell);
                    levelCreatedCount++;

                    inventoryLocationSyncService.createLocationForRack(rackCell, locEqGroupId);

                    createdMinRow = (createdMinRow == null) ? row : Math.min(createdMinRow, row);
                    createdMaxRow = (createdMaxRow == null) ? row : Math.max(createdMaxRow, row);
                    createdMinBay = (createdMinBay == null) ? bay : Math.min(createdMinBay, bay);
                    createdMaxBay = (createdMaxBay == null) ? bay : Math.max(createdMaxBay, bay);

                    if (page2d != null) {
                        create2dItemForRackCell(request, page2d, rackCell, row, bay, isDriveOnly);
                    }
                }
            }

            // 이 층 페이지의 canvas 크기 갱신
            if (page2d != null && levelCreatedCount > 0) {
                updatePageCanvasSizeByCreatedCells(page2d, createdMinRow, createdMaxRow, createdMinBay, createdMaxBay);
            }

            logger.info("[ Generator ][ Service ] Level {} completed: {} cells created", level, levelCreatedCount);
        }

        logger.info("[ Generator ][ Service ] Total created {} RackCells across levels {}-{}",
                allCreatedCells.size(), startLevel, endLevel);
        return allCreatedCells;
    }

    @Transactional(readOnly = true)
    public List<TbEqRackMst> getRackCellsByEqId(String eqGroupId, String eqId) {
        findEqMstOrThrow(eqGroupId, eqId);

        Query query = new Query();
        query.addFilter("eqId", eqId);
        query.addOrder("level", true);
        query.addOrder("row", true);
        query.addOrder("bay", true);
        return queryManager.selectList(TbEqRackMst.class, query);
    }

    @Transactional(readOnly = true)
    public List<TbEqRackMst> getRackCellsByLevel(String eqGroupId, String eqId, int level) {
        findEqMstOrThrow(eqGroupId, eqId);

        Query query = new Query();
        query.addFilter("eqId", eqId);
        query.addFilter("level", level);
        query.addOrder("row", true);
        query.addOrder("bay", true);
        return queryManager.selectList(TbEqRackMst.class, query);
    }

    @Transactional(readOnly = true)
    public TbEqRackMst getRackCell(String eqGroupId, String eqId, String rackId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(eqId) || ValueUtil.isEmpty(rackId)) {
            throw new ElidomRuntimeException("eqGroupId, eqId, rackId 필수입니다.");
        }
        findEqMstOrThrow(eqGroupId, eqId);

        TbEqRackMst rack = queryManager.selectByCondition(
                TbEqRackMst.class,
                ValueUtil.newMap("eqId,rackId", eqId, rackId)
        );
        if (rack == null) {
            throw new ElidomRuntimeException(
                    "Rack 셀을 찾을 수 없습니다: eqGroupId=" + eqGroupId + ", eqId=" + eqId + ", rackId=" + rackId);
        }
        return rack;
    }

    /**
     *단건 랙 셀 부분 수정 (필드 지정 업데이트)
     */
    @Transactional
    public TbEqRackMst updateRackCell(String eqGroupId, String eqId, String rackId, RackCellUpdateRequest req) {
        TbEqRackMst rack = getRackCell(eqGroupId, eqId, rackId);
        List<String> fields = new ArrayList<>();

        if (req.getType() != null) {
            rack.setType(req.getType());
            fields.add("type");
        }
        if (req.getRow() != null) {
            rack.setRow(req.getRow());
            fields.add("row");
        }
        if (req.getBay() != null) {
            rack.setBay(req.getBay());
            fields.add("bay");
        }
        if (req.getLevel() != null) {
            rack.setLevel(req.getLevel());
            fields.add("level");
        }
        if (req.getSkuId() != null) {
            rack.setSkuId(req.getSkuId());
            fields.add("skuId");
        }
        if (req.getSkuQty() != null) {
            rack.setSkuQty(req.getSkuQty());
            fields.add("skuQty");
        }
        if (req.getUseYn() != null) {
            rack.setUseYn(req.getUseYn());
            fields.add("useYn");
        }
        if (req.getCargoYn() != null) {
            rack.setCargoYn(req.getCargoYn());
            fields.add("cargoYn");
        }
        if (req.getBufferYn() != null) {
            rack.setBufferYn(req.getBufferYn());
            fields.add("bufferYn");
        }
        if (req.getDriveOnlyYn() != null) {
            rack.setDriveOnlyYn(req.getDriveOnlyYn());
            fields.add("driveOnlyYn");
        }

        if (!fields.isEmpty()) {
            fields.add("updatedAt");
            queryManager.update(rack, fields.toArray(new String[0]));
            logger.info("[ Generator ][ Service ] Updated RackCell: eqGroupId={}, eqId={}, rackId={}", eqGroupId, eqId, rackId);
        }
        return rack;
    }

    @Transactional
    public void deleteRackCell(String eqGroupId, String eqId, String id) {
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("설비 그룹 ID가 비어있습니다.");
        }

        inventoryLocationSyncService.deleteLocationForRack(eqGroupId, eqId, id);
        delete2dItemsForCells(eqGroupId, List.of(id));

        queryManager.delete(TbEqRackMst.class, ValueUtil.newMap("eq_id,rack_id", eqId, id));
        logger.info("[ Generator ][ Service ] Deleted RackCell: eqGroupId={}, eqId={}, id={}", eqGroupId, eqId, id);
    }

    @Transactional
    public void deleteRackCellsByEqId(String eqGroupId, String eqId) {
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("설비 그룹 ID가 비어있습니다.");
        }

        inventoryLocationSyncService.deleteLocationsByEqId(eqGroupId, eqId);

        Query query = new Query();
        query.addFilter("eqId", eqId);
        List<TbEqRackMst> rackList = queryManager.selectList(TbEqRackMst.class, query);

        if (ValueUtil.isNotEmpty(rackList)) {
            // 2D 아이템 동반 삭제 (고아 방지)
            delete2dItemsForCells(eqGroupId, rackList.stream().map(TbEqRackMst::getRackId).toList());
            queryManager.deleteBatch(rackList);
        }

        logger.info("[ Generator ][ Service ] Deleted RackCells: eqGroupId={}, eqId={}", eqGroupId, eqId);
    }

    // 2D 페이지 관련 (private)

    private TbEcs2dPage findOrCreatePage2dForLevel(RackBulkCreateRequest request, int level) {
        TbEcs2dPage existingPage = queryManager.selectByCondition(TbEcs2dPage.class,
                ValueUtil.newMap("lcId,eqGroupId,floorLevel",
                        request.getLcId(), request.getEqGroupId(), level));

        if (existingPage != null) {
            logger.info("[ Generator ][ Service ] Found existing 2D page for level {}: {}", level, existingPage.getId());

            int requiredWidth = calculate2dCanvasWidth(request);
            int requiredHeight = calculate2dCanvasHeight(request);

            if (existingPage.getCanvasWidth() < requiredWidth || existingPage.getCanvasHeight() < requiredHeight) {
                existingPage.setCanvasWidth(Math.max(existingPage.getCanvasWidth(), requiredWidth));
                existingPage.setCanvasHeight(Math.max(existingPage.getCanvasHeight(), requiredHeight));
                queryManager.update(existingPage);
            }
            return existingPage;
        }

        TbEcs2dPage newPage = new TbEcs2dPage();
        newPage.setId(UUID.randomUUID().toString());
        newPage.setLcId(request.getLcId());
        newPage.setEqGroupId(request.getEqGroupId());
        newPage.setFloorLevel(level);
        newPage.setPageName(level + "층");
        newPage.setCanvasWidth(calculate2dCanvasWidth(request));
        newPage.setCanvasHeight(calculate2dCanvasHeight(request));
        newPage.setGridSize(DEFAULT_GRID_SIZE);

        queryManager.insert(newPage);
        logger.info("[ Generator ][ Service ] Created 2D page for level {}: id={}, size={}x{}",
                level, newPage.getId(), newPage.getCanvasWidth(), newPage.getCanvasHeight());

        return newPage;
    }

    private int calculate2dCanvasWidth(RackBulkCreateRequest request) {
        int bayCount = request.getEndBay() - request.getStartBay() + 1;
        return bayCount * CELL_SIZE + CANVAS_MARGIN * 2;
    }

    private int calculate2dCanvasHeight(RackBulkCreateRequest request) {
        int rowCount = request.getEndRow() - request.getStartRow() + 1;
        return rowCount * CELL_SIZE + CANVAS_MARGIN * 2;
    }

    private void create2dItemForRackCell(RackBulkCreateRequest request, TbEcs2dPage page,
                                         TbEqRackMst rackCell, int row, int bay, boolean isDriveOnly) {
        TbEcs2dItem existingItem = queryManager.selectByCondition(
                TbEcs2dItem.class,
                ValueUtil.newMap("pageId,realEqId", page.getId(), rackCell.getRackId())
        );
        if (existingItem != null) return;

        double posX = CANVAS_MARGIN + (bay - request.getStartBay()) * CELL_SIZE;
        double posY = CANVAS_MARGIN + (row - request.getStartRow()) * CELL_SIZE;

        String equipmentTypeCode = determineEquipmentTypeCode(rackCell.getType(), isDriveOnly);

        TbEcs2dItem item = new TbEcs2dItem();
        item.setId(UUID.randomUUID().toString());
        item.setLcId(request.getLcId());
        item.setPageId(page.getId());
        item.setEquipmentCode(equipmentTypeCode + "_" + System.nanoTime());
        item.setEquipmentTypeCode("RACK");
        item.setRealEqId(rackCell.getRackId());
        item.setRealEqType("RACK");
        item.setPosX(posX);
        item.setPosY(posY);
        item.setWidth((double) CELL_SIZE);
        item.setHeight((double) CELL_SIZE);

        queryManager.insert(item);
    }

    // 랙 셀에 매핑된 RACK 2D 아이템 동반 삭제 — page.eq_group_id + real_eq_id 로 식별 (고아 방지)
    private void delete2dItemsForCells(String eqGroupId, List<String> cellIds) {
        if (ValueUtil.isEmpty(cellIds)) {
            return;
        }
        int deleted = queryManager.executeBySql("""
                DELETE FROM tb_ecs_2d_item
                 WHERE equipment_type_code = 'RACK'
                   AND real_eq_id IN (:cellIds)
                   AND page_id IN (SELECT id FROM tb_ecs_2d_page WHERE eq_group_id = :eqGroupId)
                """,
                ValueUtil.newMap("cellIds,eqGroupId", cellIds, eqGroupId));
        if (deleted > 0) {
            logger.info("[ Generator ][ Service ] Deleted {} 2D items for rack cells: eqGroupId={}", deleted, eqGroupId);
        }
    }

    private void updatePageCanvasSizeByCreatedCells(TbEcs2dPage page,
                                                    Integer minRow, Integer maxRow,
                                                    Integer minBay, Integer maxBay) {
        if (page == null || minRow == null || maxRow == null || minBay == null || maxBay == null) return;

        int createdRowCount = maxRow - minRow + 1;
        int createdBayCount = maxBay - minBay + 1;

        int requiredWidth = (createdBayCount * CELL_SIZE) + (CANVAS_MARGIN * 2);
        int requiredHeight = (createdRowCount * CELL_SIZE) + (CANVAS_MARGIN * 2);

        page.setCanvasWidth(requiredWidth);
        page.setCanvasHeight(requiredHeight);

        queryManager.update(page);
    }

    private String determineEquipmentTypeCode(int rackType, boolean isDriveOnly) {
        if (isDriveOnly) return "DRIVE_ONLY";

        EcsDBConsts.RackType rackDbType = EcsDBConsts.RackType.find(rackType);
        return switch (rackDbType) {
            case CELL -> "RACK_CELL";
            case INBOUND_PORT -> "INBOUND_PORT";
            case OUTBOUND_PORT -> "OUTBOUND_PORT";
            case IN_OUTBOUND_PORT -> "IN_OUTBOUND_PORT";
            case CHARGE_PORT -> "CHARGE_PORT";
            case CHARGE_ENTER_PORT -> "CHARGE_ENTER_PORT";
            default -> "RACK_CELL";
        };
    }

    private String generateRackCellId(int level, int row, int bay) {
        return String.format("%d%02d%02d", level, row, bay);
    }

    private Set<Integer> toSet(List<Integer> list) {
        return list != null ? new HashSet<>(list) : Collections.emptySet();
    }

    private Map<String, RackBulkCreateRequest.SpecialCellConfig> buildSpecialCellMap(
            List<RackBulkCreateRequest.SpecialCellConfig> specialCells) {
        Map<String, RackBulkCreateRequest.SpecialCellConfig> map = new HashMap<>();
        if (specialCells != null) {
            for (RackBulkCreateRequest.SpecialCellConfig config : specialCells) {
                String key = config.getRow() + "-" + config.getBay();
                map.put(key, config);
            }
        }
        return map;
    }

    // 4. 컨베이어/리프터 (tb_eq_cv_mst) — (eqGroupId + eqId) 쌍 기반

    @Transactional
    public TbEqCvMst createCvMst(String eqGroupId, CvMstCreateRequest request) {
        logger.info("[ Generator ][ Service ] Creating CvMst: eqGroupId={}, eqId={}, id={}, type={}, level={}",
                eqGroupId, request.getEqId(), request.getId(), request.getType(), request.getLevel());

        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("설비 그룹 ID가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(request.getEqId())) {
            throw new ElidomRuntimeException("기본 설비가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(request.getId())) {
            throw new ElidomRuntimeException("설비 ID가 비어있습니다.");
        }

        findEqMstOrThrow(eqGroupId, request.getEqId());

        if (queryManager.selectByCondition(TbEqCvMst.class,
                ValueUtil.newMap("eqId,id", request.getEqId(), request.getId())) != null) {
            throw new ElidomRuntimeException("이미 존재하는 컨베이어 ID입니다: " + request.getId());
        }

        TbEqCvMst cvMst = new TbEqCvMst();
        cvMst.setId(request.getId());
        cvMst.setEqId(request.getEqId());
        cvMst.setType(request.getType());
        cvMst.setLevel(request.getLevel());
        cvMst.setUseYn(request.isUseYn());
        cvMst.setAutoYn(request.isAutoYn());
        cvMst.setStatus(EcsDBConsts.EqConveyorStatus.READY.getValue());

        queryManager.insert(cvMst);
        logger.info("[ Generator ][ Service ] Created CvMst: {}", cvMst.getId());

        return cvMst;
    }

    @Transactional(readOnly = true)
    public List<TbEqCvMst> getCvMstByEqId(String eqGroupId, String eqId) {
        findEqMstOrThrow(eqGroupId, eqId);

        Query query = new Query();
        query.addFilter("eqId", eqId);
        query.addOrder("level", true);
        return queryManager.selectList(TbEqCvMst.class, query);
    }

    @Transactional(readOnly = true)
    public TbEqCvMst getCvMst(String eqGroupId, String eqId, String id) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(eqId) || ValueUtil.isEmpty(id)) {
            throw new ElidomRuntimeException("eqGroupId, eqId, id 필수입니다.");
        }
        findEqMstOrThrow(eqGroupId, eqId);

        TbEqCvMst cvMst = queryManager.selectByCondition(
                TbEqCvMst.class,
                ValueUtil.newMap("eqId,id", eqId, id)
        );
        if (cvMst == null) {
            throw new ElidomRuntimeException(
                    "Conveyor 마스터를 찾을 수 없습니다: eqGroupId=" + eqGroupId + ", eqId=" + eqId + ", id=" + id);
        }
        return cvMst;
    }

    /**
     *컨베이어 부분 수정
     */
    @Transactional
    public TbEqCvMst updateCvMst(String eqGroupId, String eqId, String id, CvMstCreateRequest request) {
        findEqMstOrThrow(eqGroupId, eqId);

        TbEqCvMst cvMst = queryManager.selectByCondition(TbEqCvMst.class,
                ValueUtil.newMap("eqId,id", eqId, id));
        if (cvMst == null) {
            throw new ElidomRuntimeException("존재하지 않는 컨베이어입니다: eqId=" + eqId + ", id=" + id);
        }

        List<String> fields = new ArrayList<>();

        if (ValueUtil.isNotEmpty(request.getType())) {
            cvMst.setType(request.getType());
            fields.add("type");
        }
        if (ValueUtil.isNotEmpty(request.getLevel())) {
            cvMst.setLevel(request.getLevel());
            fields.add("level");
        }

        cvMst.setAutoYn(request.isAutoYn());
        fields.add("autoYn");

        cvMst.setUseYn(request.isUseYn());
        fields.add("useYn");

        if (!fields.isEmpty()) {
            fields.add("updatedAt");
            queryManager.update(cvMst, fields.toArray(new String[0]));
            logger.info("[ Generator ][ Service ] Updated CvMst: eqGroupId={}, eqId={}, id={}", eqGroupId, eqId, id);
        }
        return cvMst;
    }

    @Transactional
    public void deleteCvMst(String eqGroupId, String eqId, String cvId) {
        findEqMstOrThrow(eqGroupId, eqId);

        queryManager.delete(TbEqCvMst.class, ValueUtil.newMap("eqId,id", eqId, cvId));
        logger.info("[ Generator ][ Service ] Deleted CvMst: eqGroupId={}, eqId={}, id={}", eqGroupId, eqId, cvId);
    }

    // 5. 셔틀카 (tb_eq_car_mst) — (eqGroupId + eqId) 쌍 기반

    @Transactional
    public TbEqCarMst createCarMst(String eqGroupId, CarMstCreateRequest request) {
        logger.info("[ Generator ][ Service ] Creating CarMst: eqGroupId={}, eqId={}, id={}, type={}, level={}, row={}, bay={}",
                eqGroupId, request.getEqId(), request.getId(), request.getType(),
                request.getLevel(), request.getRow(), request.getBay());

        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("설비 그룹 ID가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(request.getEqId())) {
            throw new ElidomRuntimeException("기본 설비가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(request.getId())) {
            throw new ElidomRuntimeException("셔틀카 ID가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(request.getType())) {
            throw new ElidomRuntimeException("셔틀카 타입이 비어있습니다.");
        }

        findEqMstOrThrow(eqGroupId, request.getEqId());

        if (queryManager.selectByCondition(
                TbEqCarMst.class,
                ValueUtil.newMap("eqId,id", request.getEqId(), request.getId())
        ) != null) {
            throw new ElidomRuntimeException("이미 존재하는 셔틀카 ID입니다: " + request.getId());
        }

        if (request.getMinRow() > 0 && request.getMaxRow() > 0 && request.getMinRow() > request.getMaxRow()) {
            throw new ElidomRuntimeException("minRow는 maxRow보다 클 수 없습니다.");
        }

        TbEqCarMst carMst = new TbEqCarMst();
        carMst.setId(request.getId());
        carMst.setEqId(request.getEqId());
        carMst.setType(request.getType());
        carMst.setRow(request.getRow());
        carMst.setBay(request.getBay());
        carMst.setLevel(request.getLevel());
        carMst.setRackId(request.getRackId());
        carMst.setRackEqId(request.getRackEqId());
        carMst.setMinRow(request.getMinRow());
        carMst.setMaxRow(request.getMaxRow());
        carMst.setAutoYn(request.isAutoYn());
        carMst.setUseYn(request.isUseYn());
        carMst.setCargoYn(request.isCargoYn());
        carMst.setCompleteYn(request.isCompleteYn());
        carMst.setPlcCmdId(request.getPlcCmdId());
        carMst.setPlcCompCmdId(request.getPlcCompCmdId());
        carMst.setBatteryStatus(request.getBatteryStatus());

        carMst.setStatus(0);
        carMst.setErrorId(null);
        carMst.setErrorDesc(null);

        queryManager.insert(carMst);
        logger.info("[ Generator ][ Service ] Created CarMst: {}", carMst.getId());

        return carMst;
    }

    @Transactional(readOnly = true)
    public List<TbEqCarMst> getCarMstByEqId(String eqGroupId, String eqId) {
        findEqMstOrThrow(eqGroupId, eqId);

        Query query = new Query();
        query.addFilter("eqId", eqId);
        query.addOrder("level", true);
        query.addOrder("row", true);
        query.addOrder("bay", true);
        return queryManager.selectList(TbEqCarMst.class, query);
    }

    /**
     *셔틀카 부분 수정
     */
    @Transactional
    public TbEqCarMst updateCarMst(String eqGroupId, String eqId, String id, CarMstCreateRequest request) {
        findEqMstOrThrow(eqGroupId, eqId);

        TbEqCarMst carMst = queryManager.selectByCondition(TbEqCarMst.class,
                ValueUtil.newMap("eqId,id", eqId, id));
        if (carMst == null) {
            throw new ElidomRuntimeException("존재하지 않는 셔틀카입니다: eqId=" + eqId + ", id=" + id);
        }

        List<String> fields = new ArrayList<>();

        if (ValueUtil.isNotEmpty(request.getType())) {
            carMst.setType(request.getType());
            fields.add("type");
        }

        carMst.setRow(request.getRow()); fields.add("row");
        carMst.setBay(request.getBay()); fields.add("bay");
        carMst.setLevel(request.getLevel()); fields.add("level");

        if (request.getRackId() != null) {
            carMst.setRackId(request.getRackId());
            fields.add("rackId");
        }
        if (request.getRackEqId() != null) {
            carMst.setRackEqId(request.getRackEqId());
            fields.add("rackEqId");
        }

        carMst.setMinRow(request.getMinRow()); fields.add("minRow");
        carMst.setMaxRow(request.getMaxRow()); fields.add("maxRow");
        carMst.setAutoYn(request.isAutoYn()); fields.add("autoYn");
        carMst.setUseYn(request.isUseYn()); fields.add("useYn");

        if (!fields.isEmpty()) {
            fields.add("updatedAt");
            queryManager.update(carMst, fields.toArray(new String[0]));
            logger.info("[ Generator ][ Service ] Updated CarMst: eqGroupId={}, eqId={}, id={}", eqGroupId, eqId, id);
        }

        return carMst;
    }

    @Transactional
    public void deleteCarMst(String eqGroupId, String eqId, String carId) {
        findEqMstOrThrow(eqGroupId, eqId);

        TbEqCarMst target = queryManager.selectByCondition(
                TbEqCarMst.class, ValueUtil.newMap("eqId,id", eqId, carId));

        if (target == null) {
            throw new ElidomRuntimeException("존재하지 않는 셔틀카입니다: " + carId);
        }

        queryManager.delete(TbEqCarMst.class, ValueUtil.newMap("eqId,id", eqId, carId));
        logger.info("[ Generator ][ Service ] Deleted CarMst: eqGroupId={}, eqId={}, id={}", eqGroupId, eqId, carId);
    }

    // 공통 유틸

    private TbEqMst findEqMstOrThrow(String eqGroupId, String eqId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(eqId)) {
            throw new ElidomRuntimeException("설비 그룹 ID와 설비 ID가 모두 필요합니다.");
        }
        TbEqMst eqMst = queryManager.selectByCondition(
                TbEqMst.class,
                ValueUtil.newMap("eqGroupId,id", eqGroupId, eqId)
        );
        if (eqMst == null) {
            throw new ElidomRuntimeException(
                    "존재하지 않는 기본 설비입니다: eqGroupId=" + eqGroupId + ", eqId=" + eqId);
        }
        return eqMst;
    }

    // 6. 재고 로케이션 (tb_inventory_location) — (locGroup + rackEqId + locId) 3중 키 기반

    @Transactional(readOnly = true)
    public ExtTbInventoryLocation getLocationByKey(String locGroup, String rackEqId, String locId) {
        if (ValueUtil.isEmpty(locGroup) || ValueUtil.isEmpty(rackEqId) || ValueUtil.isEmpty(locId)) {
            throw new ElidomRuntimeException("locGroup, rackEqId, locId 필수입니다.");
        }
        ExtTbInventoryLocation loc = inventoryLocationSyncService.findLocation(locGroup, rackEqId, locId);
        if (loc == null) {
            throw new ElidomRuntimeException(
                    "Location 을 찾을 수 없습니다: locGroup=" + locGroup + ", rackEqId=" + rackEqId + ", locId=" + locId);
        }
        return loc;
    }

    /**
     *재고 로케이션 부분 수정 (필드 지정 업데이트)
     */
    @Transactional
    public ExtTbInventoryLocation updateLocationByKey(String locGroup, String rackEqId, String locId,
                                                      LocationUpdateRequest req) {
        ExtTbInventoryLocation loc = getLocationByKey(locGroup, rackEqId, locId);
        List<String> fields = new ArrayList<>();

        if (req.getItemType() != null) {
            loc.setItemType(req.getItemType());
            fields.add("itemType");
        }
        if (req.getItemGroup() != null) {
            loc.setItemGroup(req.getItemGroup());
            fields.add("itemGroup");
        }
        if (req.getItemGrade() != null) {
            loc.setItemGrade(req.getItemGrade());
            fields.add("itemGrade");
        }
        if (req.getMaxHeight() != null) {
            loc.setMaxHeight(req.getMaxHeight());
            fields.add("maxHeight");
        }
        if (req.getMaxWeight() != null) {
            loc.setMaxWeight(req.getMaxWeight());
            fields.add("maxWeight");
        }
        if (req.getLocDeep() != null) {
            loc.setLocDeep(req.getLocDeep());
            fields.add("locDeep");
        }
        if (req.getLocSide() != null) {
            loc.setLocSide(req.getLocSide());
            fields.add("locSide");
        }
        if (req.getIsEnabled() != null) {
            loc.setIsEnabled(req.getIsEnabled());
            fields.add("isEnabled");
        }
        if (req.getIsInboundEnabled() != null) {
            loc.setIsInboundEnabled(req.getIsInboundEnabled());
            fields.add("isInboundEnabled");
        }
        if (req.getIsOutboundEnabled() != null) {
            loc.setIsOutboundEnabled(req.getIsOutboundEnabled());
            fields.add("isOutboundEnabled");
        }
        if (req.getIsPath() != null) {
            loc.setIsPath(req.getIsPath());
            fields.add("isPath");
        }
        if (req.getEquipType() != null) {
            loc.setEquipType(req.getEquipType());
            fields.add("equipType");
        }
        if (req.getEquipCode() != null) {
            loc.setEquipCode(req.getEquipCode());
            fields.add("equipCode");
        }
        if (req.getDestNodeCode() != null) {
            loc.setDestNodeCode(req.getDestNodeCode());
            fields.add("destNodeCode");
        }
        if (req.getLocType() != null) {
            loc.setLocType(req.getLocType());
            fields.add("locType");
        }

        if (!fields.isEmpty()) {
            fields.add("updatedAt");
            queryManager.update(loc, fields.toArray(new String[0]));
            logger.info("[ Generator ][ Location ] updated: locGroup={}, rackEqId={}, locId={}",
                    locGroup, rackEqId, locId);
        }

        return loc;
    }

    /**
     * 기존 tb_eq_rack_mst 데이터로 2D page/item + inventory_location 만 역생성.
     */
    @Transactional
    public Map<String, Object> generate2dFromExistingRacks(String lcId, String eqGroupId, String eqId) {
        findEqMstOrThrow(eqGroupId, eqId);

        Query q = new Query();
        q.addFilter("eqId", eqId);
        q.addOrder("level", true);
        List<TbEqRackMst> racks = queryManager.selectList(TbEqRackMst.class, q);

        if (ValueUtil.isEmpty(racks)) {
            return ValueUtil.newMap("createdItems,createdLocations", 0, 0);
        }

        Map<Integer, List<TbEqRackMst>> byLevel = new TreeMap<>();
        for (TbEqRackMst r : racks) {
            byLevel.computeIfAbsent(r.getLevel(), k -> new ArrayList<>()).add(r);
        }

        int newItems = 0;
        int newLocations = 0;

        for (Map.Entry<Integer, List<TbEqRackMst>> e : byLevel.entrySet()) {
            int level = e.getKey();
            List<TbEqRackMst> cells = e.getValue();

            int minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
            int minBay = Integer.MAX_VALUE, maxBay = Integer.MIN_VALUE;
            for (TbEqRackMst c : cells) {
                minRow = Math.min(minRow, c.getRow());
                maxRow = Math.max(maxRow, c.getRow());
                minBay = Math.min(minBay, c.getBay());
                maxBay = Math.max(maxBay, c.getBay());
            }

            TbEcs2dPage page = queryManager.selectByCondition(TbEcs2dPage.class,
                    ValueUtil.newMap("lcId,eqGroupId,floorLevel", lcId, eqGroupId, level));
            if (page == null) {
                page = new TbEcs2dPage();
                page.setId(UUID.randomUUID().toString());
                page.setLcId(lcId);
                page.setEqGroupId(eqGroupId);
                page.setFloorLevel(level);
                page.setPageName(level + "층");
                page.setCanvasWidth((maxBay - minBay + 1) * CELL_SIZE + CANVAS_MARGIN * 2);
                page.setCanvasHeight((maxRow - minRow + 1) * CELL_SIZE + CANVAS_MARGIN * 2);
                page.setGridSize(DEFAULT_GRID_SIZE);
                queryManager.insert(page);
            }

            for (TbEqRackMst rack : cells) {
                TbEcs2dItem existingItem = queryManager.selectByCondition(TbEcs2dItem.class,
                        ValueUtil.newMap("pageId,realEqId", page.getId(), rack.getRackId()));
                if (existingItem == null) {
                    TbEcs2dItem item = new TbEcs2dItem();
                    item.setId(UUID.randomUUID().toString());
                    item.setLcId(lcId);
                    item.setPageId(page.getId());
                    item.setEquipmentCode(determineEquipmentTypeCode(rack.getType(), rack.isDriveOnlyYn())
                            + "_" + System.nanoTime());
                    item.setEquipmentTypeCode("RACK");
                    item.setRealEqId(rack.getRackId());
                    item.setRealEqType("RACK");
                    double posX = CANVAS_MARGIN + (rack.getBay() - minBay) * CELL_SIZE;
                    double posY = CANVAS_MARGIN + (rack.getRow() - minRow) * CELL_SIZE;
                    item.setPosX(posX);
                    item.setPosY(posY);
                    item.setWidth((double) CELL_SIZE);
                    item.setHeight((double) CELL_SIZE);
                    queryManager.insert(item);
                    newItems++;
                }

                ExtTbInventoryLocation existingLoc = inventoryLocationSyncService
                        .findLocation(eqGroupId, rack.getEqId(), rack.getRackId());
                if (existingLoc == null) {
                    inventoryLocationSyncService.createLocationForRack(rack, eqGroupId);
                    newLocations++;
                }
            }
        }

        logger.info("[ Generator ][ Service ] reverse generation completed: items={}, locations={}", newItems, newLocations);
        return ValueUtil.newMap("createdItems,createdLocations", newItems, newLocations);
    }
}