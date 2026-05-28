package operato.logis.ecs.tspg4way.dashboard.rest;

import operato.logis.ecs.tspg4way.dashboard.dto.DashboardControlInfo;
import operato.logis.ecs.tspg4way.dashboard.dto.DashboardControlResponse;
import operato.logis.ecs.tspg4way.dashboard.service.impl.WcsDashboardControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * WCS 대시보드 팝업 수동 제어 Controller. 운영자가 팝업에서 수동 실행하는 액션만 담당(자동화/스케줄러/ECS 콜백과 분리).
 * URL: /rack(로케이션 제어), /equipment(설비 마스터 제어), /inventory(재고 정합성), /orders(작업 흐름).
 */
@RestController
@RequestMapping(value = "/rest/wcs/tspg-4way/dashboard-control", produces = MediaType.APPLICATION_JSON_VALUE)
public class WcsDashboardControlController {

    private static final Logger logger = LoggerFactory.getLogger(WcsDashboardControlController.class);

    @Autowired
    private WcsDashboardControlService controlService;

    // [1] 제어 정보 조회 API (팝업 오픈 시 호출)

    /**
     * 랙 셀 제어 정보 조회
     * GET /rest/wcs/tspg4way/dashboard-control/rack/{rackCellId}/info
     *
     * @param rackCellId TbEcs2dItem.realEqId (RACK 타입)
     */
    @GetMapping("/rack/{eqGroupId}/{rackCellId}/info")
    public ResponseEntity<DashboardControlInfo> getRackControlInfo(
            @PathVariable String eqGroupId,
            @PathVariable String rackCellId) {
        DashboardControlInfo info = controlService.getRackControlInfo(eqGroupId, rackCellId);
        return ResponseEntity.ok(info);
    }

    /**
     * 설비 제어 정보 조회 (CONVEYOR / LIFTER / SHUTTLE)
     * GET /rest/wcs/tspg4way/dashboard-control/equipment/{eqType}/{eqId}/info
     */
    @GetMapping("/equipment/{eqType}/{eqId}/info")
    public ResponseEntity<DashboardControlInfo> getEquipmentControlInfo(
            @PathVariable String eqType,
            @PathVariable String eqId) {
        DashboardControlInfo info = controlService.getEquipmentControlInfo(eqType, eqId);
        return ResponseEntity.ok(info);
    }

    // [2] Use/Disable 제어 API

    /**
     * 랙 로케이션 사용 여부 토글
     * POST /rest/wcs/tspg4way/dashboard-control/loc/{eqGroupId}/{locId}/toggle-use
     */
    @PostMapping("/loc/{eqGroupId}/{locId}/toggle-use")
    public ResponseEntity<DashboardControlResponse> toggleLocUse(
            @PathVariable String eqGroupId,
            @PathVariable String locId) {
        logger.warn("[ Dashboard ][ Control ] Toggle loc use: eqGroupId={}, locId={}", eqGroupId, locId);
        DashboardControlResponse resp = controlService.toggleLocUse(eqGroupId, locId);
        return resp.isSuccess() ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    /**
     * 설비 사용 여부 토글 (CONVEYOR / LIFTER / SHUTTLE)
     * POST /rest/wcs/tspg4way/dashboard-control/equipment/{eqType}/{eqId}/toggle-use
     */
    @PostMapping("/equipment/{eqType}/{eqId}/toggle-use")
    public ResponseEntity<DashboardControlResponse> toggleEquipmentUse(
            @PathVariable String eqType,
            @PathVariable String eqId) {
        logger.warn("[ Dashboard ][ Control ] Toggle equipment use: eqType={}, eqId={}", eqType, eqId);
        DashboardControlResponse resp = controlService.toggleEquipmentUse(eqType, eqId);
        return resp.isSuccess() ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    // [3] 수동 Lock/Unlock API

    /**
     * 로케이션 수동 잠금
     * POST /rest/wcs/tspg4way/dashboard-control/loc/{eqGroupId}/{locId}/lock
     */
    @PostMapping("/loc/{eqGroupId}/{locId}/lock")
    public ResponseEntity<DashboardControlResponse> manualLock(
            @PathVariable String eqGroupId,
            @PathVariable String locId) {
        logger.warn("[ Dashboard ][ Control ] Manual lock: eqGroupId={}, locId={}", eqGroupId, locId);
        DashboardControlResponse resp = controlService.manualLock(eqGroupId, locId);
        return resp.isSuccess() ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    /**
     * 로케이션 수동 잠금 해제
     * POST /rest/wcs/tspg4way/dashboard-control/loc/{eqGroupId}/{locId}/unlock
     */
    @PostMapping("/loc/{eqGroupId}/{locId}/unlock")
    public ResponseEntity<DashboardControlResponse> manualUnlock(
            @PathVariable String eqGroupId,
            @PathVariable String locId) {
        logger.warn("[ Dashboard ][ Control ] Manual unlock: eqGroupId={}, locId={}", eqGroupId, locId);
        DashboardControlResponse resp = controlService.manualUnlock(eqGroupId, locId);
        return resp.isSuccess() ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    // [4] 재고 정합성 제어 API

    /**
     * 수동 재고 삭제 — 공출고(Empty Pick) 복구
     * DELETE /rest/wcs/tspg4way/dashboard-control/inventory/{eqGroupId}/{locId}
     */
    @DeleteMapping("/inventory/{eqGroupId}/{locId}")
    public ResponseEntity<DashboardControlResponse> deleteInventory(
            @PathVariable String eqGroupId,
            @PathVariable String locId) {
        logger.warn("[ Dashboard ][ Control ] Delete inventory (Empty Pick fix): eqGroupId={}, locId={}", eqGroupId, locId);
        DashboardControlResponse resp = controlService.deleteInventory(eqGroupId, locId);
        return resp.isSuccess() ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    /**
     * 수동 재고 생성 — 이중입고(Double Entry) 복구
     * POST /rest/wcs/tspg4way/dashboard-control/inventory/{eqGroupId}/{locId}
     *
     * Request body: { skuCode, palletId, qty, ownerCode? }
     */
    @PostMapping("/inventory/{eqGroupId}/{locId}")
    public ResponseEntity<DashboardControlResponse> createInventory(
            @PathVariable String eqGroupId,
            @PathVariable String locId,
            @RequestBody Map<String, Object> body) {

        String skuCode = (String) body.get("skuCode");
        String palletId = (String) body.getOrDefault("palletId", "");
        int qty = body.containsKey("qty") ? ((Number) body.get("qty")).intValue() : 0;
        String ownerCode = (String) body.getOrDefault("ownerCode", "");

        logger.warn("[ Dashboard ][ Control ] Create inventory (Double Entry fix): eqGroupId={}, locId={}, skuCode={}, palletId={}, qty={}",
                eqGroupId, locId, skuCode, palletId, qty);

        DashboardControlResponse resp = controlService.createInventory(eqGroupId, locId, skuCode, palletId, qty, ownerCode);
        return resp.isSuccess() ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    /**
     * 라인 단위 수량 보정 — 채취/박스출고 오기입 보정 (newQty=0 → 비활성 + EMPTY 복원)
     * POST /rest/wcs/tspg-4way/dashboard-control/inventory/{eqGroupId}/{locId}/stock/{stockRowId}/adjust-qty
     *
     * Request body: { newQty, comment }
     */
    @PostMapping("/inventory/{eqGroupId}/{locId}/stock/{stockRowId}/adjust-qty")
    public ResponseEntity<DashboardControlResponse> adjustInventoryQty(
            @PathVariable String eqGroupId,
            @PathVariable String locId,
            @PathVariable String stockRowId,
            @RequestBody Map<String, Object> body) {

        int newQty = body.containsKey("newQty") ? ((Number) body.get("newQty")).intValue() : -1;
        String comment = (String) body.getOrDefault("comment", "");

        logger.warn("[ Dashboard ][ Control ] Adjust qty: eqGroupId={}, locId={}, stockRowId={}, newQty={}",
                eqGroupId, locId, stockRowId, newQty);

        DashboardControlResponse resp = controlService.adjustInventoryQty(eqGroupId, locId, stockRowId, newQty, comment);
        return resp.isSuccess() ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    // [5] 작업 흐름 제어 API

    /**
     * 작업 강제 완료 (Force Complete)
     * POST /rest/wcs/tspg4way/dashboard-control/orders/{orderKey}/force-complete
     */
    @PostMapping("/orders/{orderKey}/force-complete")
    public ResponseEntity<DashboardControlResponse> forceCompleteOrder(@PathVariable String orderKey) {
        logger.warn("[ Dashboard ][ Control ] Force complete order: orderKey={}", orderKey);
        DashboardControlResponse resp = controlService.forceCompleteOrder(orderKey);
        return resp.isSuccess() ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    /**
     * 작업 취소 (Cancel)
     * POST /rest/wcs/tspg4way/dashboard-control/orders/{orderKey}/cancel
     *
     * Request body: { reason? }
     */
    @PostMapping("/orders/{orderKey}/cancel")
    public ResponseEntity<DashboardControlResponse> cancelOrder(
            @PathVariable String orderKey,
            @RequestBody(required = false) Map<String, Object> body) {

        String reason = (body != null) ? (String) body.getOrDefault("reason", "") : "";
        logger.warn("[ Dashboard ][ Control ] Cancel order: orderKey={}, reason={}", orderKey, reason);
        DashboardControlResponse resp = controlService.cancelOrder(orderKey, reason);
        return resp.isSuccess() ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    /**
     * 작업 재개 (Resume — 에러 복구 후 재전송 트리거)
     * POST /rest/wcs/tspg4way/dashboard-control/orders/{orderKey}/resume
     */
    @PostMapping("/orders/{orderKey}/resume")
    public ResponseEntity<DashboardControlResponse> resumeOrder(@PathVariable String orderKey) {
        logger.warn("[ Dashboard ][ Control ] Resume order: orderKey={}", orderKey);
        DashboardControlResponse resp = controlService.resumeOrder(orderKey);
        return resp.isSuccess() ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }
}
