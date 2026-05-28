package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.service.impl.RealTimeDataService;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqCarMst;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqCvMst;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqRackMst;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbWcsShuttleOrder;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

@RestController
@RequestMapping(value = "/rest/realtime", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "실시간 데이터 API")
public class RealTimeController {

    @Autowired
    private RealTimeDataService realTimeDataService;

    // ============================================
    // 설비그룹 + 타입별 설비 목록 조회
    // ============================================

    @GetMapping("/equipments/group/{eqGroupId}/type/{type}")
    @ResponseStatus(HttpStatus.OK)
    public List<?> getEquipmentsByGroupAndType(
            @PathVariable String eqGroupId,
            @PathVariable int type) {
        return realTimeDataService.getEquipmentsByGroupAndType(eqGroupId, type);
    }

    // ============================================
    // 랙 셀 목록 조회
    // ============================================

    @GetMapping("/racks/group/{eqGroupId}/floor/{floor}")
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqRackMst> getRackCellsByGroupAndFloor(
            @PathVariable String eqGroupId,
            @PathVariable int floor) {
        return realTimeDataService.getRackCellsByGroupAndFloor(eqGroupId, floor);
    }

    // ============================================
    // 컨베이어 목록 조회
    // ============================================

    @GetMapping("/conveyors/group/{eqGroupId}")
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqCvMst> getConveyorsByGroup(@PathVariable String eqGroupId) {
        return realTimeDataService.getConveyorsByGroup(eqGroupId);
    }

    @GetMapping("/conveyors/group/{eqGroupId}/floor/{floor}")
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqCvMst> getConveyorsByGroupAndFloor(
            @PathVariable String eqGroupId,
            @PathVariable int floor) {
        return realTimeDataService.getConveyorsByGroupAndFloor(eqGroupId, floor);
    }

    // ============================================
    // 셔틀 목록 조회
    // ============================================

    @GetMapping("/shuttles/group/{eqGroupId}")
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqCarMst> getShuttlesByGroup(@PathVariable String eqGroupId) {
        return realTimeDataService.getShuttlesByGroup(eqGroupId);
    }

    @GetMapping("/shuttles/group/{eqGroupId}/floor/{floor}")
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqCarMst> getShuttlesByGroupAndFloor(
            @PathVariable String eqGroupId,
            @PathVariable int floor) {
        return realTimeDataService.getShuttlesByGroupAndFloor(eqGroupId, floor);
    }

    // ============================================
    // WCS 작업 목록 조회
    // ============================================

    @GetMapping("/orders/active")
    @ResponseStatus(HttpStatus.OK)
    public List<TbWcsShuttleOrder> getActiveOrders() {
        return realTimeDataService.getActiveOrders();
    }

    @GetMapping("/orders/errors")
    @ResponseStatus(HttpStatus.OK)
    public List<TbWcsShuttleOrder> getErrorOrders() {
        return realTimeDataService.getErrorOrders();
    }

    // ============================================
    // 대시보드 초기 데이터
    // ============================================

    @GetMapping("/dashboard/initial/{lcId}/{pageId}")
    @ResponseStatus(HttpStatus.OK)
    public Map getDashboardInitialData(
            @PathVariable String lcId,
            @PathVariable String pageId) {
        return realTimeDataService.getDashboardInitialData(lcId, pageId);
    }
}