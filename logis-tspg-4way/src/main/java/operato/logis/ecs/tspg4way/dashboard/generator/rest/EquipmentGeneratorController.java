package operato.logis.ecs.tspg4way.dashboard.generator.rest;

import lombok.RequiredArgsConstructor;
import operato.logis.ecs.tspg4way.dashboard.generator.dto.*;
import operato.logis.ecs.tspg4way.dashboard.generator.service.EquipmentGeneratorService;
import operato.logis.ecs.tspg4way.dashboard.generator.service.InventoryLocationSyncService;
import operato.logis.ecs.tspg4way.entity.*;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
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
 * 설비 생성 REST 컨트롤러 (역매핑). 설비그룹/설비/랙셀/컨베이어/셔틀카/재고 로케이션 CRUD.
 * tb_eq_mst 가 (eqGroupId, id) 복합 unique 이므로 하위 설비는 (eqGroupId + eqId) 쌍으로 식별.
 */
@RestController
@RequestMapping(value = "/rest/equipment-generator", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "설비 생성 API (역매핑)")
@RequiredArgsConstructor
public class EquipmentGeneratorController {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentGeneratorController.class);

    private final EquipmentGeneratorService generatorService;
    private final InventoryLocationSyncService syncService;

    // 1. 설비 그룹

    @PostMapping("/eq-group")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public TbEqGroupMst createEqGroup(@RequestBody EqGroupCreateRequest request) {
        logger.info("[ Generator ][ Rest ] POST /eq-group: id={}", request.getId());
        return generatorService.createEqGroup(request);
    }

    @GetMapping("/eq-groups")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqGroupMst> getEqGroups() {
        return generatorService.getAllEqGroups();
    }

    @PutMapping("/eq-group/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public TbEqGroupMst updateEqGroup(@PathVariable String id, @RequestBody EqGroupCreateRequest request) {
        logger.info("[ Generator ][ Rest ] PUT /eq-group/{}", id);
        return generatorService.updateEqGroup(id, request);
    }

    @DeleteMapping("/eq-group/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteEqGroup(@PathVariable String id) {
        logger.info("[ Generator ][ Rest ] DELETE /eq-group/{}", id);
        generatorService.deleteEqGroup(id);
        return ValueUtil.newMap("success,deletedId", true, id);
    }

    // 2. 기본 설비 (eqGroupId 포함)

    @PostMapping("/eq-mst")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public TbEqMst createEqMst(@RequestBody EqMstCreateRequest request) {
        logger.info("[ Generator ][ Rest ] POST /eq-mst: id={}, eqGroupId={}", request.getId(), request.getEqGroupId());
        return generatorService.createEqMst(request);
    }

    @GetMapping("/eq-mst/group/{eqGroupId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqMst> getEqMstByGroup(@PathVariable String eqGroupId) {
        return generatorService.getEqMstByGroup(eqGroupId);
    }

    /**
     *(eqGroupId + id) 로 단건 상세 조회
     */
    @GetMapping("/eq-mst/group/{eqGroupId}/{id}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public EqMstDetailResponse getEqMstDetail(@PathVariable String eqGroupId, @PathVariable String id) {
        return generatorService.getEqMstDetail(eqGroupId, id);
    }

    /**
     *(eqGroupId + id) 로 수정
     */
    @PutMapping("/eq-mst/group/{eqGroupId}/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public TbEqMst updateEqMst(
            @PathVariable String eqGroupId,
            @PathVariable String id,
            @RequestBody EqMstCreateRequest request) {
        logger.info("[ Generator ][ Rest ] PUT /eq-mst/group/{}/{}", eqGroupId, id);
        return generatorService.updateEqMst(eqGroupId, id, request);
    }

    /**
     *(eqGroupId + id) 로 삭제
     */
    @DeleteMapping("/eq-mst/group/{eqGroupId}/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteEqMst(
            @PathVariable String eqGroupId,
            @PathVariable String id) {
        logger.info("[ Generator ][ Rest ] DELETE /eq-mst/group/{}/{}", eqGroupId, id);
        generatorService.deleteEqMst(eqGroupId, id);
        return ValueUtil.newMap("success,deletedId", true, id);
    }

    // 3. 랙 셀 (eqGroupId 포함)

    @PostMapping("/rack-cell/group/{eqGroupId}")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public TbEqRackMst createRackCell(
            @PathVariable String eqGroupId,
            @RequestBody RackCellCreateRequest request) {
        logger.info("[ Generator ][ Rest ] POST /rack-cell/group/{}: eqId={}, id={}",
                eqGroupId, request.getEqId(), request.getId());
        return generatorService.createRackCell(eqGroupId, request);
    }

    @PostMapping("/rack-cells/grid")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createRackCellsGrid(@RequestBody RackBulkCreateRequest request) {
        logger.info("[ Generator ][ Rest ] POST /rack-cells/grid: eqGroupId={}, eqId={}",
                request.getEqGroupId(), request.getEqId());

        List<TbEqRackMst> createdCells = generatorService.createRackCellsGrid(request);

        List<String> createdIds = createdCells.stream()
                .map(TbEqRackMst::getRackId)
                .collect(Collectors.toList());

        return ValueUtil.newMap("success,createdCount,createdIds", true, createdCells.size(), createdIds);
    }

    @GetMapping("/rack-cells/group/{eqGroupId}/eq/{eqId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqRackMst> getRackCellsByEqId(
            @PathVariable String eqGroupId,
            @PathVariable String eqId) {
        return generatorService.getRackCellsByEqId(eqGroupId, eqId);
    }

    @GetMapping("/rack-cells/group/{eqGroupId}/eq/{eqId}/level/{level}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqRackMst> getRackCellsByLevel(
            @PathVariable String eqGroupId,
            @PathVariable String eqId,
            @PathVariable int level) {
        return generatorService.getRackCellsByLevel(eqGroupId, eqId, level);
    }

    /**
     *(eqGroupId + eqId + rackId) 로 단건 랙 셀 조회
     */
    @GetMapping("/rack-cell/group/{eqGroupId}/eq/{eqId}/{id}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEqRackMst getRackCell(
            @PathVariable String eqGroupId,
            @PathVariable String eqId,
            @PathVariable String id) {
        return generatorService.getRackCell(eqGroupId, eqId, id);
    }

    /**
     *(eqGroupId + eqId + rackId) 로 단건 랙 셀 부분 수정
     */
    @PutMapping("/rack-cell/group/{eqGroupId}/eq/{eqId}/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public TbEqRackMst updateRackCell(
            @PathVariable String eqGroupId,
            @PathVariable String eqId,
            @PathVariable String id,
            @RequestBody RackCellUpdateRequest request) {
        logger.info("[ Generator ][ Rest ] PUT /rack-cell/group/{}/eq/{}/{}", eqGroupId, eqId, id);
        return generatorService.updateRackCell(eqGroupId, eqId, id, request);
    }

    @DeleteMapping("/rack-cell/group/{eqGroupId}/eq/{eqId}/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteRackCell(
            @PathVariable String eqGroupId,
            @PathVariable String eqId,
            @PathVariable String id) {
        logger.info("[ Generator ][ Rest ] DELETE /rack-cell/group/{}/eq/{}/{}", eqGroupId, eqId, id);
        generatorService.deleteRackCell(eqGroupId, eqId, id);
        return ValueUtil.newMap("success,deletedId", true, id);
    }

    @DeleteMapping("/rack-cells/group/{eqGroupId}/eq/{eqId}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteRackCellsByEqId(
            @PathVariable String eqGroupId,
            @PathVariable String eqId) {
        logger.info("[ Generator ][ Rest ] DELETE /rack-cells/group/{}/eq/{}", eqGroupId, eqId);
        generatorService.deleteRackCellsByEqId(eqGroupId, eqId);
        return ValueUtil.newMap("success,eqId", true, eqId);
    }

    /**
     * MapEditor 셀 단위 일괄 동기화 — DISABLE/ENABLE/CREATE
     */
    @PutMapping("/rack-cells/sync")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public RackCellSyncResult syncRackCells(@RequestBody RackCellSyncRequest request) {
        logger.info("[ Generator ][ Rest ] PATCH /rack-cells/sync: eqGroupId={}, rackEqId={}",
                request.getEqGroupId(), request.getRackEqId());
        return syncService.applyCellOps(request);
    }

    // 4. 컨베이어/리프터 (eqGroupId 포함)

    @PostMapping("/cv-mst/group/{eqGroupId}")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public TbEqCvMst createCvMst(
            @PathVariable String eqGroupId,
            @RequestBody CvMstCreateRequest request) {
        logger.info("[ Generator ][ Rest ] POST /cv-mst/group/{}: eqId={}, id={}",
                eqGroupId, request.getEqId(), request.getId());
        return generatorService.createCvMst(eqGroupId, request);
    }

    @GetMapping("/cv-mst/group/{eqGroupId}/eq/{eqId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqCvMst> getCvMstByEqId(
            @PathVariable String eqGroupId,
            @PathVariable String eqId) {
        return generatorService.getCvMstByEqId(eqGroupId, eqId);
    }

    /**
     *(eqGroupId + eqId + id) 로 단건 컨베이어/리프터 조회
     */
    @GetMapping("/cv-mst/group/{eqGroupId}/eq/{eqId}/{id}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEqCvMst getCvMst(
            @PathVariable String eqGroupId,
            @PathVariable String eqId,
            @PathVariable String id) {
        return generatorService.getCvMst(eqGroupId, eqId, id);
    }

    @PutMapping("/cv-mst/group/{eqGroupId}/eq/{eqId}/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public TbEqCvMst updateCvMst(
            @PathVariable String eqGroupId,
            @PathVariable String eqId,
            @PathVariable String id,
            @RequestBody CvMstCreateRequest request) {
        logger.info("[ Generator ][ Rest ] PUT /cv-mst/group/{}/eq/{}/{}", eqGroupId, eqId, id);
        return generatorService.updateCvMst(eqGroupId, eqId, id, request);
    }

    @DeleteMapping("/cv-mst/group/{eqGroupId}/eq/{eqId}/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteCvMst(
            @PathVariable String eqGroupId,
            @PathVariable String eqId,
            @PathVariable String id) {
        logger.info("[ Generator ][ Rest ] DELETE /cv-mst/group/{}/eq/{}/{}", eqGroupId, eqId, id);
        generatorService.deleteCvMst(eqGroupId, eqId, id);
        return ValueUtil.newMap("success,deletedId", true, id);
    }

    // 5. 셔틀카 (eqGroupId 포함)

    @PostMapping("/car-mst/group/{eqGroupId}")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public TbEqCarMst createCarMst(
            @PathVariable String eqGroupId,
            @RequestBody CarMstCreateRequest request) {
        logger.info("[ Generator ][ Rest ] POST /car-mst/group/{}: eqId={}, id={}, type={}",
                eqGroupId, request.getEqId(), request.getId(), request.getType());
        return generatorService.createCarMst(eqGroupId, request);
    }

    @GetMapping("/car-mst/group/{eqGroupId}/eq/{eqId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqCarMst> getCarMstByEqId(
            @PathVariable String eqGroupId,
            @PathVariable String eqId) {
        return generatorService.getCarMstByEqId(eqGroupId, eqId);
    }

    @PutMapping("/car-mst/group/{eqGroupId}/eq/{eqId}/car/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public TbEqCarMst updateCarMst(
            @PathVariable String eqGroupId,
            @PathVariable String eqId,
            @PathVariable String id,
            @RequestBody CarMstCreateRequest request) {
        logger.info("[ Generator ][ Rest ] PUT /car-mst/group/{}/eq/{}/car/{}", eqGroupId, eqId, id);
        return generatorService.updateCarMst(eqGroupId, eqId, id, request);
    }

    @DeleteMapping("/car-mst/group/{eqGroupId}/eq/{eqId}/car/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> deleteCarMst(
            @PathVariable String eqGroupId,
            @PathVariable String eqId,
            @PathVariable String id) {
        logger.info("[ Generator ][ Rest ] DELETE /car-mst/group/{}/eq/{}/car/{}", eqGroupId, eqId, id);
        generatorService.deleteCarMst(eqGroupId, eqId, id);
        return ValueUtil.newMap("success,deletedId,eqId", true, id, eqId);
    }

    @PostMapping("/dashboard-2d/generate")
    @Transactional
    public Map<String, Object> generate2dFromExistingRacks(
            @RequestParam String lcId,
            @RequestParam String eqGroupId,
            @RequestParam String eqId) {
        return generatorService.generate2dFromExistingRacks(lcId, eqGroupId, eqId);
    }

    // 6. 재고 로케이션 (tb_inventory_location) — (locGroup + rackEqId + locId) 3중 키
    // logis-inventory 모듈이 logis-tspg-4way 에 의존하지 않으므로 단건 GET/PUT 을 이 컨트롤러에서 제공(cross-module 의존 회피)

    /**
     * (locGroup + rackEqId + locId) 로 단건 로케이션 조회
     */
    @GetMapping("/inventory-location/group/{locGroup}/eq/{rackEqId}/{locId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public ExtTbInventoryLocation getInventoryLocation(
            @PathVariable String locGroup,
            @PathVariable String rackEqId,
            @PathVariable String locId) {
        return generatorService.getLocationByKey(locGroup, rackEqId, locId);
    }

    /**
     * (locGroup + rackEqId + locId) 로 단건 로케이션 부분 수정
     */
    @PutMapping("/inventory-location/group/{locGroup}/eq/{rackEqId}/{locId}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public ExtTbInventoryLocation updateInventoryLocation(
            @PathVariable String locGroup,
            @PathVariable String rackEqId,
            @PathVariable String locId,
            @RequestBody LocationUpdateRequest request) {
        logger.info("[ Generator ][ Rest ] PUT /inventory-location/group/{}/eq/{}/{}", locGroup, rackEqId, locId);
        return generatorService.updateLocationByKey(locGroup, rackEqId, locId, request);
    }
}