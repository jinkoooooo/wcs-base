package operato.logis.wcs.rest.inventory;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.validation.CommentValidator;
import operato.logis.wcs.dto.CellClassificationUpdateRequest;
import operato.logis.wcs.dto.CellStatusUpdateRequest;
import operato.logis.wcs.service.impl.query.inventory.CellStateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

/**
 * 셀 상태 관리 (BUSINESS302) API.
 * ZONE = eq_group_id, 적재단 = level 기반.
 *
 *  ⚠ v8.1: 재고 상세/업데이트 API 에 eq_group_id 파라미터가 필수로 추가됨.
 *  tb_inventory_location 의 동일 loc_id 가 ZONE 별로 병존하기 때문에
 *  ZONE 스코프 없이 조회/변경하면 다른 ZONE 의 레코드까지 영향을 받는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/rest/wcs/inventory/cell-state", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "셀 상태 관리 API")
public class CellStateController {

    private final CellStateService cellStateService;

    @GetMapping("/zones")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Map> getZones() {
        return this.cellStateService.getZoneGroups();
    }

    @GetMapping("/levels")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Map> getLevels(@RequestParam("eq_group_id") String eqGroupId) {
        return this.cellStateService.getLevelOptions(eqGroupId);
    }

    @GetMapping("/cells")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Map> getCells(
            @RequestParam("eq_group_id") String eqGroupId,
            @RequestParam(value = "level", required = false) Integer level) {
        return this.cellStateService.getCellsByGroup(eqGroupId, level);
    }

    /**
     * 셀 상태 일괄 변경.
     *  - 개별 선택: { "eq_group_id": "ZONE1", "cell_ids": [...], "action": "LOCK" }
     *  - 층 전체:  { "eq_group_id": "ZONE1", "level": 2, "action": "FORBID_IN_ALL" }
     *
     *  ⚠ eq_group_id 는 항상 필수 (v8.1 부터).
     */
    @PostMapping(value = "/update-status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> updateCellStatus(@RequestBody CellStatusUpdateRequest req) {
        // 자동 시스템 전환은 service/repository 를 다른 경로로 호출하므로 본 검증 대상 아님.
        CommentValidator.requireValid("comment", req == null ? null : req.getComment());
        int affected = this.cellStateService.updateCellsStatus(req);
        return Map.of("affected", affected, "success", true);
    }

    /**
     * 셀 분류·제약 일괄 변경 (item_type / item_group / max_weight / max_height).
     * 각 필드는 mode = set/clear/skip 으로 의도 분리. eq_group_id 필수.
     */
    @PostMapping(value = "/update-classification", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> updateCellClassification(@RequestBody CellClassificationUpdateRequest req) {
        int affected = this.cellStateService.updateCellsClassification(req);
        return Map.of("affected", affected, "success", true);
    }

    /**
     * ZONE 내 기존 item_type / item_group distinct 옵션 (콤보 옵션 소스).
     * 응답: { "item_types": [...], "item_groups": [...] }
     */
    @GetMapping("/classify-options")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, List<String>> getClassifyOptions(@RequestParam("eq_group_id") String eqGroupId) {
        return this.cellStateService.getClassifyOptions(eqGroupId);
    }

    /**
     * 셀 재고 상세 (단일).
     *  ⚠ eq_group_id 쿼리 파라미터 필수.
     */
    @GetMapping("/stock-detail/{cellId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Map> getStockDetail(
            @PathVariable String cellId,
            @RequestParam("eq_group_id") String eqGroupId) {
        return this.cellStateService.getCellStockDetail(cellId, eqGroupId);
    }

    /**
     * 다중 셀 재고 상세.
     *  body: { "eq_group_id": "ZONE1", "cell_ids": ["10101", "10102", ...] }
     *  ⚠ eq_group_id 필수.
     */
    @PostMapping(value = "/stock-detail-multi", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Map> getStockDetailMulti(@RequestBody CellStatusUpdateRequest req) {
        if (ValueUtil.isEmpty(req)) return List.of();
        return this.cellStateService.getCellStockDetailMulti(req.getCellIds(), req.getEqGroupId());
    }
}