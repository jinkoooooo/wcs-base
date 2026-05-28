package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.dto.*;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.service.EquipmentGeneratorService;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 설비 생성 REST 컨트롤러 (역매핑)
 *
 * 2D 레이아웃 에디터에서 설정한 정보를 기반으로
 * 실운영 테이블에 데이터를 생성하는 API 제공
 *
 * [API 목록]
 * 1. 설비 그룹 (tb_eq_group_mst)
 *    - POST   /rest/equipment-generator/eq-group
 *    - GET    /rest/equipment-generator/eq-groups
 *    - PUT    /rest/equipment-generator/eq-group/{id}
 *    - DELETE /rest/equipment-generator/eq-group/{id}
 *
 * 2. 기본 설비 (tb_eq_mst)
 *    - POST   /rest/equipment-generator/eq-mst
 *    - GET    /rest/equipment-generator/eq-mst/group/{eqGroupId}
 *    - GET    /rest/equipment-generator/eq-mst/{id}
 *    - PUT    /rest/equipment-generator/eq-mst/{id}
 *    - DELETE /rest/equipment-generator/eq-mst/{id}
 *
 * 3. 랙 셀 (tb_eq_rack_mst)
 *    - POST   /rest/equipment-generator/rack-cell
 *    - POST   /rest/equipment-generator/rack-cells/grid
 *    - GET    /rest/equipment-generator/rack-cells/eq/{eqId}
 *    - GET    /rest/equipment-generator/rack-cells/eq/{eqId}/level/{level}
 *    - DELETE /rest/equipment-generator/rack-cell/{id}
 *    - DELETE /rest/equipment-generator/rack-cells/eq/{eqId}
 *
 * 4. 컨베이어/리프터 (tb_eq_cv_mst)
 *    - POST   /rest/equipment-generator/cv-mst
 *    - GET    /rest/equipment-generator/cv-mst/eq/{eqId}
 *    - PUT    /rest/equipment-generator/cv-mst/{eqId}/{id}
 *    - DELETE /rest/equipment-generator/cv-mst/{eqId}/{id}
 *
 * 5. 셔틀카 (tb_eq_car_mst)
 *    - POST   /rest/equipment-generator/car-mst
 *    - GET    /rest/equipment-generator/car-mst/eq/{eqId}
 *    - PUT    /rest/equipment-generator/car-mst/eq/{eqId}/car/{id}
 *    - DELETE /rest/equipment-generator/car-mst/eq/{eqId}/car/{id}
 *
 * @author WCS Development Team
 * @since 2026-03-27
 */
@RestController
@RequestMapping(value = "/rest/equipment-generator", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "설비 생성 API (역매핑)")
@RequiredArgsConstructor
public class EquipmentGeneratorController {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentGeneratorController.class);

    private final EquipmentGeneratorService generatorService;

    // ============================================
    // 1. 설비 그룹 (tb_eq_group_mst)
    // ============================================

    /**
     * 설비 그룹 생성
     */
    @PostMapping("/eq-group")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public TbEqGroupMst createEqGroup(@RequestBody EqGroupCreateRequest request) {
        logger.info("[Generator] POST /eq-group: id={}", request.getId());
        return generatorService.createEqGroup(request);
    }

    /**
     * 설비 그룹 목록 조회
     */
    @GetMapping("/eq-groups")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqGroupMst> getEqGroups() {
        return generatorService.getAllEqGroups();
    }

    /**
     * 설비 그룹 수정 (name, type) — EqGroupCreateRequest 재사용
     */
    @PutMapping("/eq-group/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public TbEqGroupMst updateEqGroup(@PathVariable String id, @RequestBody EqGroupCreateRequest request) {
        logger.info("[Generator] PUT /eq-group/{}", id);
        return generatorService.updateEqGroup(id, request);
    }

    /**
     * 설비 그룹 삭제
     */
    @DeleteMapping("/eq-group/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteEqGroup(@PathVariable String id) {
        logger.info("[Generator] DELETE /eq-group/{}", id);
        generatorService.deleteEqGroup(id);
        return ValueUtil.newMap("success,deletedId", true, id);
    }

    // ============================================
    // 2. 기본 설비 (tb_eq_mst)
    // ============================================

    /**
     * 기본 설비 생성
     */
    @PostMapping("/eq-mst")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public TbEqMst createEqMst(@RequestBody EqMstCreateRequest request) {
        logger.info("[Generator] POST /eq-mst: id={}, eqGroupId={}", request.getId(), request.getEqGroupId());
        return generatorService.createEqMst(request);
    }

    /**
     * 그룹별 기본 설비 목록 조회
     */
    @GetMapping("/eq-mst/group/{eqGroupId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqMst> getEqMstByGroup(@PathVariable String eqGroupId) {
        return generatorService.getEqMstByGroup(eqGroupId);
    }

    /**
     * 기본 설비 단건 상세 조회 (PLC 정보 포함)
     */
    @GetMapping("/eq-mst/{id}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public EqMstDetailResponse getEqMstDetail(@PathVariable String id) {
        return generatorService.getEqMstDetail(id);
    }

    /**
     * 기본 설비 수정 (name, PLC 정보) — EqMstCreateRequest 재사용
     */
    @PutMapping("/eq-mst/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public TbEqMst updateEqMst(@PathVariable String id, @RequestBody EqMstCreateRequest request) {
        logger.info("[Generator] PUT /eq-mst/{}", id);
        return generatorService.updateEqMst(id, request);
    }

    /**
     * 기본 설비 삭제
     */
    @DeleteMapping("/eq-mst/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteEqMst(@PathVariable String id) {
        logger.info("[Generator] DELETE /eq-mst/{}", id);
        generatorService.deleteEqMst(id);
        return ValueUtil.newMap("success,deletedId", true, id);
    }

    // ============================================
    // 3. 랙 셀 (tb_eq_rack_mst)
    // ============================================

    /**
     * 단일 랙 셀 생성
     */
    @PostMapping("/rack-cell")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public TbEqRackMst createRackCell(@RequestBody RackCellCreateRequest request) {
        logger.info("[Generator] POST /rack-cell: id={}", request.getId());
        return generatorService.createRackCell(request);
    }

    /**
     * 랙 셀 일괄 생성 (GRID 모드)
     *
     * ID 생성 규칙: {level}{row:02d}{bay:02d}
     * 예: level=1, row=6, bay=1 → 10601
     */
    @PostMapping("/rack-cells/grid")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createRackCellsGrid(@RequestBody RackBulkCreateRequest request) {
        logger.info("[Generator] POST /rack-cells/grid: eqId={}, rows={}-{}, bays={}-{}, level={}",
                request.getEqId(), request.getStartRow(), request.getEndRow(),
                request.getStartBay(), request.getEndBay(), request.getFloorLevel());

        List<TbEqRackMst> createdCells = generatorService.createRackCellsGrid(request);

        // 생성된 ID 목록 추출
        List<String> createdIds = createdCells.stream()
                .map(TbEqRackMst::getId)
                .collect(Collectors.toList());

        return ValueUtil.newMap("success,createdCount,createdIds", true, createdCells.size(), createdIds);
    }

    /**
     * 설비별 랙 셀 목록 조회
     */
    @GetMapping("/rack-cells/eq/{eqId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqRackMst> getRackCellsByEqId(@PathVariable String eqId) {
        return generatorService.getRackCellsByEqId(eqId);
    }

    /**
     * 설비+층별 랙 셀 목록 조회
     */
    @GetMapping("/rack-cells/eq/{eqId}/level/{level}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqRackMst> getRackCellsByLevel(@PathVariable String eqId, @PathVariable int level) {
        return generatorService.getRackCellsByLevel(eqId, level);
    }

    /**
     * 랙 셀 삭제
     */
    @DeleteMapping("/rack-cell/{eqId}/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteRackCell(@PathVariable String eqId,@PathVariable String id) {
        logger.info("[Generator] DELETE /rack-cell/{}/{}", eqId,id);
        generatorService.deleteRackCell(eqId,id);
        return ValueUtil.newMap("success,deletedId", true, id);
    }

    /**
     * 설비별 랙 셀 전체 삭제
     */
    @DeleteMapping("/rack-cells/eq/{eqId}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteRackCellsByEqId(@PathVariable String eqId) {
        logger.info("[Generator] DELETE /rack-cells/eq/{}", eqId);
        generatorService.deleteRackCellsByEqId(eqId);
        return ValueUtil.newMap("success,eqId", true, eqId);
    }

    // ============================================
    // 4. 컨베이어/리프터 (tb_eq_cv_mst)
    // ============================================

    /**
     * 컨베이어/리프터 생성
     */
    @PostMapping("/cv-mst")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public TbEqCvMst createCvMst(@RequestBody CvMstCreateRequest request) {
        logger.info("[Generator] POST /cv-mst: id={}", request.getId());
        return generatorService.createCvMst(request);
    }

    /**
     * 설비별 컨베이어 목록 조회
     */
    @GetMapping("/cv-mst/eq/{eqId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqCvMst> getCvMstByEqId(@PathVariable String eqId) {
        return generatorService.getCvMstByEqId(eqId);
    }

    /**
     * 컨베이어 수정 (type, level, autoYn, useYn) — CvMstCreateRequest 재사용
     */
    @PutMapping("/cv-mst/{eqId}/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public TbEqCvMst updateCvMst(@PathVariable String eqId, @PathVariable String id,
                                  @RequestBody CvMstCreateRequest request) {
        logger.info("[Generator] PUT /cv-mst/{}/{}", eqId, id);
        return generatorService.updateCvMst(eqId, id, request);
    }

    /**
     * 컨베이어 삭제
     */
    @DeleteMapping("/cv-mst/{eqId}/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteCvMst(@PathVariable String eqId, @PathVariable String id) {
        logger.info("[Generator] DELETE /cv-mst/{}/{}", eqId, id);
        generatorService.deleteCvMst(eqId, id);
        return ValueUtil.newMap("success,deletedId", true, id);
    }

    // ============================================
    // 5. 셔틀카 (tb_eq_car_mst)
    // ============================================

    /**
     * 셔틀카 생성
     */
    @PostMapping("/car-mst")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public TbEqCarMst createCarMst(@RequestBody CarMstCreateRequest request) {
        logger.info("[Generator] POST /car-mst: id={}, eqId={}, type={}",
                request.getId(), request.getEqId(), request.getType());
        return generatorService.createCarMst(request);
    }

    /**
     * 설비별 셔틀카 목록 조회
     */
    @GetMapping("/car-mst/eq/{eqId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqCarMst> getCarMstByEqId(@PathVariable String eqId) {
        return generatorService.getCarMstByEqId(eqId);
    }

    /**
     * 셔틀카 수정 (위치/범위/타입 등) — CarMstCreateRequest 재사용
     */
    @PutMapping("/car-mst/eq/{eqId}/car/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public TbEqCarMst updateCarMst(@PathVariable String eqId, @PathVariable String id,
                                    @RequestBody CarMstCreateRequest request) {
        logger.info("[Generator] PUT /car-mst/eq/{}/car/{}", eqId, id);
        return generatorService.updateCarMst(eqId, id, request);
    }

    /**
     * 셔틀카 삭제
     */
    @DeleteMapping("/car-mst/eq/{eqId}/car/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteCarMst(@PathVariable String eqId, @PathVariable String id) {
        logger.info("[Generator] DELETE /car-mst/eq/{}/car/{}", eqId, id);
        generatorService.deleteCarMst(eqId, id);
        return ValueUtil.newMap("success,deletedId,eqId", true, id, eqId);
    }
}
