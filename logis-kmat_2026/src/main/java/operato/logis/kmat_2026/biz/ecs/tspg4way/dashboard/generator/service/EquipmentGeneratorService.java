package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.service;

import lombok.RequiredArgsConstructor;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.entity.TbEcs2dItem;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.entity.TbEcs2dPage;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.dto.*;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

/**
 * 설비 생성 서비스 (역매핑)
 *
 * 2D 레이아웃 에디터에서 설정한 정보를 기반으로
 * 실운영 테이블에 데이터를 생성하는 서비스
 *
 * [생성 순서]
 * 1. tb_eq_group_mst (설비 그룹)
 * 2. tb_eq_mst (기본 설비)
 * 3. tb_eq_rack_mst / tb_eq_cv_mst (설비 타입별)
 *
 * [ID 생성 규칙]
 * - 랙 셀: {level}{row:02d}{bay:02d} (예: level=1, row=6, bay=1 → 10601)
 * - 컨베이어: 직접 지정
 *
 * @author WCS Development Team
 * @since 2026-03-27
 */
@Service
@RequiredArgsConstructor
public class EquipmentGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentGeneratorService.class);

    private final IQueryManager queryManager;

    // ============================================
    // 2D Dashboard 상수
    // ============================================

    /** 랙 셀 픽셀 크기 */
    private static final int CELL_SIZE = 100;

    /** 캔버스 여백 */
    private static final int CANVAS_MARGIN = 100;

    /** 기본 그리드 크기 */
    private static final int DEFAULT_GRID_SIZE = 20;

    // ============================================
    // 1. 설비 그룹 (tb_eq_group_mst)
    // ============================================

    /**
     * 설비 그룹 생성
     *
     * @param request 생성 요청
     * @return 생성된 설비 그룹
     * @throws ElidomRuntimeException 중복 ID인 경우
     */
    @Transactional
    public TbEqGroupMst createEqGroup(EqGroupCreateRequest request) {
        logger.info("[Generator] Creating EqGroup: id={}, name={}", request.getId(), request.getName());

        // 중복 체크
        if (queryManager.select(TbEqGroupMst.class, request.getId()) != null) {
            throw new ElidomRuntimeException("이미 존재하는 설비 그룹 ID입니다: " + request.getId());
        }

        TbEqGroupMst eqGroup = new TbEqGroupMst();
        eqGroup.setId(request.getId());
        eqGroup.setName(request.getName());
        eqGroup.setType(request.getType());

        queryManager.insert(eqGroup);
        logger.info("[Generator] Created EqGroup: {}", eqGroup.getId());

        return eqGroup;
    }

    /**
     * 설비 그룹 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqGroupMst> getAllEqGroups() {
        return queryManager.selectList(TbEqGroupMst.class, new Query());
    }

    /**
     * 설비 그룹 수정 (name, type) — EqGroupCreateRequest 재사용
     */
    @Transactional
    public TbEqGroupMst updateEqGroup(String id, EqGroupCreateRequest request) {
        TbEqGroupMst eqGroup = queryManager.select(TbEqGroupMst.class, id);
        if (eqGroup == null) {
            throw new ElidomRuntimeException("존재하지 않는 설비 그룹입니다: " + id);
        }
        if (ValueUtil.isNotEmpty(request.getName())) eqGroup.setName(request.getName());
        if (ValueUtil.isNotEmpty(request.getType())) eqGroup.setType(request.getType());
        queryManager.update(eqGroup);
        logger.info("[Generator] Updated EqGroup: {}", id);
        return eqGroup;
    }

    /**
     * 설비 그룹 삭제
     *
     * @param eqGroupId 삭제할 그룹 ID
     * @throws IllegalStateException 하위 설비가 있는 경우
     */
    @Transactional
    public void deleteEqGroup(String eqGroupId) {
        Query query = new Query();
        query.addFilter("eqGroupId", eqGroupId);

        if (!queryManager.selectList(TbEqMst.class, query).isEmpty()) {
            throw new IllegalStateException("하위 설비가 존재하여 삭제할 수 없습니다.");
        }

        queryManager.delete(TbEqGroupMst.class, ValueUtil.newMap("id", eqGroupId));
        logger.info("[Generator] Deleted EqGroup: {}", eqGroupId);
    }

    // ============================================
    // 2. 기본 설비 (tb_eq_mst)
    // ============================================

    /**
     * 기본 설비 생성
     *
     * - tb_eq_mst insert
     * - tb_eq_plc_mst insert
     * 를 하나의 트랜잭션으로 처리
     *
     * @param request 생성 요청
     * @return 생성된 기본 설비
     */
    @Transactional
    public TbEqMst createEqMst(EqMstCreateRequest request) {
        logger.info("[Generator] Creating EqMst + EqPlcMst: id={}, eqGroupId={}, type={}, plcId={}",
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

        if(usePlc && ValueUtil.isEmpty(request.getPlcId())) {
            throw new ElidomRuntimeException("plc id 가 비어있습니다.");
        }

        // 설비 그룹 존재 여부 확인
        if (queryManager.select(TbEqGroupMst.class, request.getEqGroupId()) == null) {
            throw new ElidomRuntimeException("존재하지 않는 설비 그룹입니다: " + request.getEqGroupId());
        }

        // tb_eq_mst 중복 체크
        if (queryManager.selectByCondition(
                TbEqMst.class,
                ValueUtil.newMap("eqGroupId,id", request.getEqGroupId(), request.getId())
        ) != null) {
            throw new ElidomRuntimeException("이미 존재하는 기본 설비 ID입니다: " + request.getId());
        }

        // tb_eq_mst 중복 체크
        if (ValueUtil.isNotEmpty(request.getPlcId()) && queryManager.selectByCondition(
                TbEqPlcMst.class,
                ValueUtil.newMap("id", request.getId())
        ) != null) {
            throw new ElidomRuntimeException("이미 존재하는 기본 설비 ID입니다: " + request.getId());
        }

        // tb_eq_plc_mst 중복 체크
        if (queryManager.select(TbEqPlcMst.class, request.getPlcId()) != null) {
            throw new ElidomRuntimeException("이미 존재하는 PLC ID입니다: " + request.getPlcId());
        }

        // 1. tb_eq_mst 생성
        TbEqMst eqMst = new TbEqMst();
        eqMst.setId(request.getId());
        eqMst.setEqGroupId(request.getEqGroupId());
        eqMst.setName(request.getName());
        eqMst.setType(request.getType());
        eqMst.setPlcId(request.getPlcId());

        queryManager.insert(eqMst);

        logger.info("[Generator] Created EqMst: {}, plcId={}", eqMst.getId(), eqMst.getPlcId());

        // plc 사용 설비의 경우
        if(usePlc) {
            EcsDBConsts.EqType eqType = EcsDBConsts.EqType.find(request.getType());

            if(eqType == null) {
                throw new ElidomRuntimeException("설비 타입이 맞지 않습니다.");
            }

            int plcEqType;

            if(eqType.getValue() == EcsDBConsts.EqType.CONVEYOR.getValue()) {
                plcEqType = EcsDBConsts.PlcEqType.CONVEYOR_AND_LIFT.getValue();
            }else if(eqType.getValue() == EcsDBConsts.EqType.SHUTTLE_CAR.getValue()){
                plcEqType = EcsDBConsts.PlcEqType.SHUTTLE_CAR.getValue();
            }else{
                throw new ElidomRuntimeException("Shuttle , conveyor 외에 plc 연결은 안됩니다.");
            }

            // 2. tb_eq_plc_mst 생성
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

            logger.info("[Generator] Created EqPlcMst: {}", plcMst.getId());
        }

        return eqMst;
    }

    /**
     * 그룹별 기본 설비 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqMst> getEqMstByGroup(String eqGroupId) {
        Query query = new Query();
        query.addFilter("eqGroupId", eqGroupId);
        return queryManager.selectList(TbEqMst.class, query);
    }

    /**
     * 기본 설비 단건 상세 조회 (PLC 정보 포함)
     */
    @Transactional(readOnly = true)
    public EqMstDetailResponse getEqMstDetail(String id) {
        TbEqMst eqMst = queryManager.select(TbEqMst.class, id);
        if (eqMst == null) {
            throw new ElidomRuntimeException("존재하지 않는 기본 설비입니다: " + id);
        }

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
            }else{
                logger.info("[Generator] getEqMstDetail PLC ID is empty");
            }
        }
        return response;
    }

    /**
     * 기본 설비 수정 (name, PLC 정보) — EqMstCreateRequest 재사용
     * - id / eqGroupId / type 은 path variable 기준이므로 변경 불가
     * - PLC 타입 설비의 경우 연결된 TbEqPlcMst도 함께 수정
     */
    @Transactional
    public TbEqMst updateEqMst(String id, EqMstCreateRequest request) {
        TbEqMst eqMst = queryManager.select(TbEqMst.class, ValueUtil.newMap("id",id));
        if (eqMst == null) {
            throw new ElidomRuntimeException("존재하지 않는 기본 설비입니다: " + id);
        }

        if (ValueUtil.isNotEmpty(request.getName())) eqMst.setName(request.getName());
        queryManager.update(eqMst);
        logger.info("[Generator] Updated EqMst: {}", id);

        if (ValueUtil.isNotEmpty(eqMst.getPlcId())) {
            TbEqPlcMst plcMst = queryManager.select(TbEqPlcMst.class, ValueUtil.newMap("id", eqMst.getPlcId()));
            if (plcMst != null) {
                if (ValueUtil.isNotEmpty(request.getPlcName())) plcMst.setName(request.getPlcName());
                if (ValueUtil.isNotEmpty(request.getPlcIp())) plcMst.setIp(request.getPlcIp());
                plcMst.setPort(request.getPlcPort());
                if (ValueUtil.isNotEmpty(request.getPlcIfType())) plcMst.setPlcIfType(request.getPlcIfType());
                plcMst.setConnectYn(request.isConnectYn());
                plcMst.setUseYn(request.isUseYn());
                queryManager.update(plcMst);
                logger.info("[Generator] Updated EqPlcMst: {}", plcMst.getId());
            }
        }else{
            logger.info("[Generator] PLC ID is empty");
        }
        return eqMst;
    }

    /**
     * 기본 설비 삭제
     *
     * - 하위 랙/컨베이어/셔틀카가 있으면 삭제 불가
     * - tb_eq_mst 삭제
     * - 연결된 tb_eq_plc_mst도 함께 삭제
     */
    @Transactional
    public void deleteEqMst(String eqId) {
        TbEqMst targetEq = queryManager.select(TbEqMst.class, eqId);
        if (targetEq == null) {
            throw new ElidomRuntimeException("존재하지 않는 기본 설비입니다: " + eqId);
        }

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

        queryManager.delete(TbEqMst.class, ValueUtil.newMap("id",eqId));
        logger.info("[Generator] Deleted EqMst: {}", eqId);

        if (ValueUtil.isNotEmpty(plcId)) {
            TbEqPlcMst plc = queryManager.select(TbEqPlcMst.class, ValueUtil.newMap("id",plcId));
            if (plc != null) {
                queryManager.delete(TbEqPlcMst.class,  ValueUtil.newMap("id",plcId));
                logger.info("[Generator] Deleted EqPlcMst: {}", plcId);
            }
        }
    }

    // ============================================
    // 3. 랙 셀 (tb_eq_rack_mst)
    // ============================================

    /**
     * 단일 랙 셀 생성
     *
     * @param request 생성 요청
     * @return 생성된 랙 셀
     * @throws ElidomRuntimeException 중복 ID인 경우
     */
    @Transactional
    public TbEqRackMst createRackCell(RackCellCreateRequest request) {
        logger.info("[Generator] Creating RackCell: id={}, row={}, bay={}, level={}",
                request.getId(), request.getRow(), request.getBay(), request.getLevel());

        if (queryManager.select(TbEqRackMst.class, request.getId()) != null) {
            throw new ElidomRuntimeException("이미 존재하는 셀 ID입니다: " + request.getId());
        }

        TbEqRackMst rackCell = new TbEqRackMst();
        rackCell.setId(request.getId());
        rackCell.setEqId(request.getEqId());
        rackCell.setType(request.getType());
        rackCell.setRow(request.getRow());
        rackCell.setBay(request.getBay());
        rackCell.setLevel(request.getLevel());
        rackCell.setDriveOnlyYn(request.isDriveOnlyYn());
        rackCell.setUseYn(request.isUseYn());
        rackCell.setStatus(EcsDBConsts.EqRackStatus.READY.getValue());

        queryManager.insert(rackCell);
        logger.info("[Generator] Created RackCell: {}", rackCell.getId());

        return rackCell;
    }

    /**
     * 랙 셀 일괄 생성 (GRID 모드)
     *
     * 격자 범위(startRow~endRow, startBay~endBay)를 기반으로 셀 자동 생성
     * create2dItems=true이면 2D Dashboard 아이템도 동시 생성
     *
     * [ID 생성 규칙]
     * {level}{row:02d}{bay:02d}
     * 예: level=1, row=6, bay=1 → 10601
     *
     * [좌표계]
     * 원점 (Row=0, Bay=0): 왼쪽 하단
     * Bay (X축): 오른쪽으로 증가 →
     * Row (Y축): 위쪽으로 증가 ↑
     *
     * [지원 기능]
     * - driveOnlyRows: 주행 전용 Row 목록
     * - driveOnlyBays: 주행 전용 Bay 목록
     * - specialCells: 특수 셀 설정 (충전포트, 입출고포트 등)
     *
     * @param request 일괄 생성 요청
     * @return 생성된 랙 셀 목록
     */
    @Transactional
    public List<TbEqRackMst> createRackCellsGrid(RackBulkCreateRequest request) {
        logger.info("[Generator] Creating RackCells (GRID): eqId={}, rows={}-{}, bays={}-{}, level={}, create2dItems={}",
                request.getEqId(), request.getStartRow(), request.getEndRow(),
                request.getStartBay(), request.getEndBay(), request.getFloorLevel(), request.isCreate2dItems());

        if (ValueUtil.isEmpty(request.getEqId())) {
            logger.info("설비 그룹 id 가 비어있습니다.");
            throw new ElidomRuntimeException("기본 설비가 비어있습니다.");
        }

        List<TbEqRackMst> createdCells = new ArrayList<>();

        Integer createdMinRow = null;
        Integer createdMaxRow = null;
        Integer createdMinBay = null;
        Integer createdMaxBay = null;

        // 주행 전용 Row/Bay 설정
        Set<Integer> driveOnlyRows = toSet(request.getDriveOnlyRows());
        Set<Integer> driveOnlyBays = toSet(request.getDriveOnlyBays());

        // 특수 셀 맵 생성 (row-bay -> config)
        Map<String, RackBulkCreateRequest.SpecialCellConfig> specialCellMap = buildSpecialCellMap(request.getSpecialCells());

        // 2D Dashboard 아이템 생성용 페이지 (create2dItems=true인 경우에만)
        TbEcs2dPage page2d = null;
        if (request.isCreate2dItems()) {
            if (ValueUtil.isEmpty(request.getLcId()) || ValueUtil.isEmpty(request.getEqGroupId())) {
                throw new ElidomRuntimeException("2D 아이템 생성을 위해서는 lcId와 eqGroupId가 필요합니다.");
            }
            page2d = findOrCreatePage2d(request);
        }

        for (int row = request.getStartRow(); row <= request.getEndRow(); row++) {
            for (int bay = request.getStartBay(); bay <= request.getEndBay(); bay++) {
                // ID 생성: {level}{row:02d}{bay:02d}
                String cellId = generateRackCellId(request.getFloorLevel(), row, bay);

                // 중복 체크
                if (queryManager.selectByCondition(TbEqRackMst.class, ValueUtil.newMap("eqId,id", request.getEqId(), cellId)) != null) {
                    logger.warn("[Generator] RackCell already exists, skipping: {}", cellId);
                    continue;
                }

                // 특수 셀 설정 확인
                String specialKey = row + "-" + bay;
                RackBulkCreateRequest.SpecialCellConfig specialConfig = specialCellMap.get(specialKey);

                // 주행 전용 여부 결정
                boolean isDriveOnly = driveOnlyRows.contains(row) || driveOnlyBays.contains(bay);
                if (specialConfig != null && specialConfig.getDriveOnlyYn() != null) {
                    isDriveOnly = specialConfig.getDriveOnlyYn();
                }

                // 랙 타입 결정
                int cellType = request.getRackType();
                if (specialConfig != null && specialConfig.getType() > 0) {
                    cellType = specialConfig.getType();
                }

                TbEqRackMst rackCell = new TbEqRackMst();
                rackCell.setId(cellId);
                rackCell.setEqId(request.getEqId());
                rackCell.setType(cellType);
                rackCell.setRow(row);
                rackCell.setBay(bay);
                rackCell.setLevel(request.getFloorLevel());
                rackCell.setDriveOnlyYn(isDriveOnly);
                rackCell.setUseYn(request.isUseYn());
                rackCell.setStatus(EcsDBConsts.EqRackStatus.READY.getValue());

                queryManager.insert(rackCell);
                createdCells.add(rackCell);

                createdMinRow = (createdMinRow == null) ? row : Math.min(createdMinRow, row);
                createdMaxRow = (createdMaxRow == null) ? row : Math.max(createdMaxRow, row);
                createdMinBay = (createdMinBay == null) ? bay : Math.min(createdMinBay, bay);
                createdMaxBay = (createdMaxBay == null) ? bay : Math.max(createdMaxBay, bay);

                // 2D Dashboard 아이템 생성
                if (page2d != null) {
                    create2dItemForRackCell(request, page2d, rackCell, row, bay, isDriveOnly);
                }
            }
        }

        if (page2d != null && !createdCells.isEmpty()) {
            updatePageCanvasSizeByCreatedCells(
                    page2d,
                    createdMinRow,
                    createdMaxRow,
                    createdMinBay,
                    createdMaxBay
            );
        }

        logger.info("[Generator] Created {} RackCells", createdCells.size());
        return createdCells;
    }

    /**
     * 2D 페이지 조회 또는 생성
     *
     * 동일 lcId, eqGroupId, floorLevel 조합이 있으면 재사용
     * 없으면 새로 생성하고 캔버스 크기 설정
     */
    private TbEcs2dPage findOrCreatePage2d(RackBulkCreateRequest request) {
        // 기존 페이지 조회
        Query query = new Query();
        query.addFilter("lcId", request.getLcId());
        query.addFilter("eqGroupId", request.getEqGroupId());
        query.addFilter("floorLevel", request.getFloorLevel());

        TbEcs2dPage existingPage = queryManager.selectByCondition(TbEcs2dPage.class,
                ValueUtil.newMap("lcId,eqGroupId,floorLevel", request.getLcId(), request.getEqGroupId(), request.getFloorLevel()));

        if (existingPage != null) {
            logger.info("[Generator] Found existing 2D page: {}", existingPage.getId());

            // 기존 페이지의 캔버스 크기 확장 (필요 시)
            int requiredWidth = calculate2dCanvasWidth(request);
            int requiredHeight = calculate2dCanvasHeight(request);

            if (existingPage.getCanvasWidth() < requiredWidth || existingPage.getCanvasHeight() < requiredHeight) {
                existingPage.setCanvasWidth(Math.max(existingPage.getCanvasWidth(), requiredWidth));
                existingPage.setCanvasHeight(Math.max(existingPage.getCanvasHeight(), requiredHeight));
                queryManager.update(existingPage);
                logger.info("[Generator] Updated 2D page canvas size: {}x{}", existingPage.getCanvasWidth(), existingPage.getCanvasHeight());
            }

            return existingPage;
        }

        // 새 페이지 생성
        TbEcs2dPage newPage = new TbEcs2dPage();
        newPage.setId(UUID.randomUUID().toString());
        newPage.setLcId(request.getLcId());
        newPage.setEqGroupId(request.getEqGroupId());
        newPage.setFloorLevel(request.getFloorLevel());
        newPage.setPageName(request.getFloorLevel() + "층");
        newPage.setCanvasWidth(calculate2dCanvasWidth(request));
        newPage.setCanvasHeight(calculate2dCanvasHeight(request));
        newPage.setGridSize(DEFAULT_GRID_SIZE);

        queryManager.insert(newPage);
        logger.info("[Generator] Created 2D page: id={}, floorLevel={}, size={}x{}",
                newPage.getId(), newPage.getFloorLevel(), newPage.getCanvasWidth(), newPage.getCanvasHeight());

        return newPage;
    }

    /**
     * 2D 캔버스 너비 계산
     *
     * (endBay - startBay + 1) * CELL_SIZE + MARGIN * 2
     */
    private int calculate2dCanvasWidth(RackBulkCreateRequest request) {
        int bayCount = request.getEndBay() - request.getStartBay() + 1;
        return bayCount * CELL_SIZE + CANVAS_MARGIN * 2;
    }

    /**
     * 2D 캔버스 높이 계산
     *
     * (endRow - startRow + 1) * CELL_SIZE + MARGIN * 2
     */
    private int calculate2dCanvasHeight(RackBulkCreateRequest request) {
        int rowCount = request.getEndRow() - request.getStartRow() + 1;
        return rowCount * CELL_SIZE + CANVAS_MARGIN * 2;
    }

    /**
     * 랙 셀에 대한 2D 아이템 생성
     *
     * [좌표 변환]
     * 원점 (Row=0, Bay=0): 화면 좌하단
     * - posX = MARGIN + (bay - startBay) * CELL_SIZE
     * - posY = MARGIN + (endRow - row) * CELL_SIZE (Y축 반전: 화면은 위에서 아래로 증가)
     */
    private void create2dItemForRackCell(RackBulkCreateRequest request, TbEcs2dPage page,
                                         TbEqRackMst rackCell, int row, int bay, boolean isDriveOnly) {
        TbEcs2dItem existingItem = queryManager.selectByCondition(
                TbEcs2dItem.class,
                ValueUtil.newMap("pageId,realEqId", page.getId(), rackCell.getId())
        );
        if (existingItem != null) {
            logger.warn("[Generator] 2D Item already exists for rackCell: {}", rackCell.getId());
            return;
        }

        // 하단 좌측(10101)을 기준으로
        // 오른쪽으로 갈수록 X 증가
        // 위로 갈수록 Y 증가
        double posX = CANVAS_MARGIN + (bay - request.getStartBay()) * CELL_SIZE;
        double posY = CANVAS_MARGIN + (row - request.getStartRow()) * CELL_SIZE;

        String equipmentTypeCode = determineEquipmentTypeCode(rackCell.getType(), isDriveOnly);

        TbEcs2dItem item = new TbEcs2dItem();
        item.setId(UUID.randomUUID().toString());
        item.setLcId(request.getLcId());
        item.setPageId(page.getId());
        item.setEquipmentCode(equipmentTypeCode + "_" + System.nanoTime());
        item.setEquipmentTypeCode("RACK");
        item.setRealEqId(rackCell.getId());
        item.setRealEqType("RACK");
        item.setPosX(posX);
        item.setPosY(posY);
        item.setWidth((double) CELL_SIZE);
        item.setHeight((double) CELL_SIZE);

        queryManager.insert(item);

        logger.debug("[Generator] Created 2D Item: id={}, rackCellId={}, pos=({},{})",
                item.getId(), rackCell.getId(), posX, posY);
    }

    /**
     * 이번 요청에서 실제 생성된 전체 cell 범위를 기준으로
     * page canvas width / height 를 업데이트
     *
     * 기준:
     * - 셀 1개의 width = CELL_SIZE(100)
     * - 셀 1개의 height = CELL_SIZE(100)
     * - 전체 width = bay 개수 * CELL_SIZE + 좌우 margin
     * - 전체 height = row 개수 * CELL_SIZE + 상하 margin
     */
    private void updatePageCanvasSizeByCreatedCells(TbEcs2dPage page,
                                                    Integer minRow,
                                                    Integer maxRow,
                                                    Integer minBay,
                                                    Integer maxBay) {
        if (page == null || minRow == null || maxRow == null || minBay == null || maxBay == null) {
            return;
        }

        int createdRowCount = maxRow - minRow + 1;
        int createdBayCount = maxBay - minBay + 1;

        int requiredWidth = (createdBayCount * CELL_SIZE) + (CANVAS_MARGIN * 2);
        int requiredHeight = (createdRowCount * CELL_SIZE) + (CANVAS_MARGIN * 2);

        page.setCanvasWidth(requiredWidth);
        page.setCanvasHeight(requiredHeight);

        queryManager.update(page);

        logger.info(
                "[Generator] Updated page canvas by created cells: pageId={}, rowCount={}, bayCount={}, cellSize={}, margin={}, width={}, height={}",
                page.getId(), createdRowCount, createdBayCount, CELL_SIZE, CANVAS_MARGIN, requiredWidth, requiredHeight
        );
    }

    /**
     * 랙 타입에 따른 2D 아이템 타입 코드 결정
     */
    private String determineEquipmentTypeCode(int rackType, boolean isDriveOnly) {
        // 주행 전용 셀
        if (isDriveOnly) {
            return "DRIVE_ONLY";
        }

        EcsDBConsts.RackType rackDbType = EcsDBConsts.RackType.find(rackType);

        // 랙 타입에 따른 분류 (EcsDBConsts.RackType 참조)
        // 11: CELL, 21: INBOUND_PORT, 22: OUTBOUND_PORT, 23: IN_OUTBOUND_PORT
        // 31: CHARGE_PORT, 32: CHARGE_ENTER_PORT
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

    /**
     * 랙 셀 ID 생성
     *
     * 규칙: {level}{row:02d}{bay:02d}
     * 예: level=1, row=6, bay=1 → 10601
     */
    private String generateRackCellId(int level, int row, int bay) {
        return String.format("%d%02d%02d", level, row, bay);
    }

    /**
     * 리스트를 Set으로 변환 (null-safe)
     */
    private Set<Integer> toSet(List<Integer> list) {
        return list != null ? new HashSet<>(list) : Collections.emptySet();
    }

    /**
     * 특수 셀 맵 생성
     */
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

    /**
     * 설비별 랙 셀 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqRackMst> getRackCellsByEqId(String eqId) {
        Query query = new Query();
        query.addFilter("eqId", eqId);
        query.addOrder("level", true);
        query.addOrder("row", true);
        query.addOrder("bay", true);
        return queryManager.selectList(TbEqRackMst.class, query);
    }

    /**
     * 층별 랙 셀 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqRackMst> getRackCellsByLevel(String eqId, int level) {
        Query query = new Query();
        query.addFilter("eqId", eqId);
        query.addFilter("level", level);
        query.addOrder("row", true);
        query.addOrder("bay", true);
        return queryManager.selectList(TbEqRackMst.class, query);
    }

    /**
     * 랙 셀 삭제
     */
    @Transactional
    public void deleteRackCell(String eqId, String id) {
        queryManager.delete(TbEqRackMst.class, ValueUtil.newMap("eq_id,id", eqId,id));
        logger.info("[Generator] Deleted eqId : {} RackCell: {}", eqId,id);
    }

    /**
     * 설비별 랙 셀 전체 삭제
     *
     * @param eqId 설비 ID
     * @return 삭제된 셀 개수
     */
    @Transactional
    public void deleteRackCellsByEqId(String eqId) {
        List<TbEqRackMst> rackList = queryManager.selectList(TbEqRackMst.class, SysValueUtil.newMap("eqId", eqId));

        if(ValueUtil.isNotEmpty(rackList)) {
            queryManager.deleteBatch(rackList);
        }

        logger.info("[Generator] Deleted RackCells for eqId: {}", eqId);
    }

    // ============================================
    // 4. 컨베이어/리프터 (tb_eq_cv_mst)
    // ============================================

    /**
     * 컨베이어/리프터 생성
     *
     * @param request 생성 요청
     * @return 생성된 컨베이어
     * @throws ElidomRuntimeException 중복 ID인 경우
     */
    @Transactional
    public TbEqCvMst createCvMst(CvMstCreateRequest request) {
        logger.info("[Generator] Creating CvMst: id={}, eqId={}, type={}, level={}",
                request.getId(), request.getEqId(), request.getType(), request.getLevel());

        if(ValueUtil.isEmpty(request.getEqId())) {
            throw new ElidomRuntimeException("기본 그룹이 비어있습니다.");
        }

        if(ValueUtil.isEmpty(request.getId())) {
            throw new ElidomRuntimeException("설비 ID가 비어있습니다.");
        }

        // 중복 체크
        if (queryManager.selectByCondition(TbEqCvMst.class, ValueUtil.newMap("eqId,id",request.getEqId(),request.getId())) != null) {
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
        logger.info("[Generator] Created CvMst: {}", cvMst.getId());

        return cvMst;
    }

    /**
     * 설비별 컨베이어 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqCvMst> getCvMstByEqId(String eqId) {
        Query query = new Query();
        query.addFilter("eqId", eqId);
        query.addOrder("level", true);
        return queryManager.selectList(TbEqCvMst.class, query);
    }

    /**
     * 컨베이어 수정 (type, level, autoYn, useYn) — CvMstCreateRequest 재사용
     */
    @Transactional
    public TbEqCvMst updateCvMst(String eqId, String id, CvMstCreateRequest request) {
        TbEqCvMst cvMst = queryManager.selectByCondition(TbEqCvMst.class, ValueUtil.newMap("eqId,id", eqId, id));
        if (cvMst == null) {
            throw new ElidomRuntimeException("존재하지 않는 컨베이어입니다: " + id);
        }
        cvMst.setType(request.getType());
        cvMst.setLevel(request.getLevel());
        cvMst.setAutoYn(request.isAutoYn());
        cvMst.setUseYn(request.isUseYn());
        queryManager.update(cvMst);
        logger.info("[Generator] Updated CvMst: {}", id);
        return cvMst;
    }

    /**
     * 컨베이어 삭제
     */
    @Transactional
    public void deleteCvMst(String eqId, String cvId) {
        queryManager.delete(TbEqCvMst.class, ValueUtil.newMap("eqId,id",eqId,cvId));
        logger.info("[Generator] Deleted CvMst: {}", cvId);
    }

    // ============================================
    // 5. 셔틀카 (tb_eq_car_mst)
    // ============================================

    /**
     * 셔틀카 생성
     *
     * @param request 생성 요청
     * @return 생성된 셔틀카
     */
    @Transactional
    public TbEqCarMst createCarMst(CarMstCreateRequest request) {
        logger.info("[Generator] Creating CarMst: id={}, eqId={}, type={}, level={}, row={}, bay={}",
                request.getId(), request.getEqId(), request.getType(),
                request.getLevel(), request.getRow(), request.getBay());

        if (ValueUtil.isEmpty(request.getEqId())) {
            throw new ElidomRuntimeException("기본 설비가 비어있습니다.");
        }

        if (ValueUtil.isEmpty(request.getId())) {
            throw new ElidomRuntimeException("셔틀카 ID가 비어있습니다.");
        }

        if (ValueUtil.isEmpty(request.getType())) {
            throw new ElidomRuntimeException("셔틀카 타입이 비어있습니다.");
        }

        // 상위 기본 설비 존재 여부 확인
        Query eqQuery = new Query();
        eqQuery.addFilter("id", request.getEqId());
        List<TbEqMst> eqList = queryManager.selectList(TbEqMst.class, eqQuery);
        if (eqList.isEmpty()) {
            throw new ElidomRuntimeException("존재하지 않는 기본 설비입니다: " + request.getEqId());
        }

        // 중복 체크 (복합 unique: eqId, id)
        if (queryManager.selectByCondition(
                TbEqCarMst.class,
                ValueUtil.newMap("eqId,id", request.getEqId(), request.getId())
        ) != null) {
            throw new ElidomRuntimeException("이미 존재하는 셔틀카 ID입니다: " + request.getId());
        }

        // row 범위 검증
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

        // 기본 상태값
        carMst.setStatus(0);   // 필요하면 EcsDBConsts 셔틀/카 상태 enum으로 교체
        carMst.setErrorId(null);
        carMst.setErrorDesc(null);

        queryManager.insert(carMst);
        logger.info("[Generator] Created CarMst: {}", carMst.getId());

        return carMst;
    }

    /**
     * 기본 설비별 셔틀카 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqCarMst> getCarMstByEqId(String eqId) {
        Query query = new Query();
        query.addFilter("eqId", eqId);
        query.addOrder("level", true);
        query.addOrder("row", true);
        query.addOrder("bay", true);
        return queryManager.selectList(TbEqCarMst.class, query);
    }

    /**
     * 셔틀카 수정 (위치/범위/타입 등) — CarMstCreateRequest 재사용
     */
    @Transactional
    public TbEqCarMst updateCarMst(String eqId, String id, CarMstCreateRequest request) {
        TbEqCarMst carMst = queryManager.selectByCondition(TbEqCarMst.class, ValueUtil.newMap("eqId,id", eqId, id));
        if (carMst == null) {
            throw new ElidomRuntimeException("존재하지 않는 셔틀카입니다: " + id);
        }
        if (ValueUtil.isNotEmpty(request.getType())) carMst.setType(request.getType());
        carMst.setRow(request.getRow());
        carMst.setBay(request.getBay());
        carMst.setLevel(request.getLevel());
        if (request.getRackId() != null) carMst.setRackId(request.getRackId());
        if (request.getRackEqId() != null) carMst.setRackEqId(request.getRackEqId());
        carMst.setMinRow(request.getMinRow());
        carMst.setMaxRow(request.getMaxRow());
        carMst.setAutoYn(request.isAutoYn());
        carMst.setUseYn(request.isUseYn());
        queryManager.update(carMst);
        logger.info("[Generator] Updated CarMst: {}", id);
        return carMst;
    }

    /**
     * 셔틀카 삭제
     */
    @Transactional
    public void deleteCarMst(String eqId, String carId) {
        TbEqCarMst target = queryManager.selectByCondition(
                TbEqCarMst.class,
                ValueUtil.newMap("eqId,id", eqId, carId)
        );

        if (target == null) {
            throw new ElidomRuntimeException("존재하지 않는 셔틀카입니다: " + carId);
        }

        queryManager.delete(TbEqCarMst.class, ValueUtil.newMap("eqId,id", eqId,carId));
        logger.info("[Generator] Deleted CarMst: eqId={}, carId={}", eqId, carId);
    }
}
