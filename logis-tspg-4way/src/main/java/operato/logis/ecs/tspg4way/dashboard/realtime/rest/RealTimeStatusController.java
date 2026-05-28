package operato.logis.ecs.tspg4way.dashboard.realtime.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.ecs.tspg4way.dashboard.realtime.registry.RealTimeProviderRegistry;
import operato.logis.ecs.tspg4way.dashboard.realtime.scheduler.RealTimeBroadcastScheduler;
import operato.logis.ecs.tspg4way.dashboard.realtime.service.ActiveSubscriptionManager;
import operato.logis.ecs.tspg4way.dashboard.realtime.service.LayoutCoordinateService;
import operato.logis.ecs.tspg4way.dashboard.realtime.service.RealTimeStatusService;
import operato.logis.ecs.tspg4way.entity.TbEqCarMst;
import operato.logis.ecs.tspg4way.entity.TbEqRackMst;
import operato.logis.ecs.tspg4way.entity.TbWcsShuttleOrder;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 실시간 데이터 모니터링 REST 컨트롤러. 브로드캐스트/구독/좌표 캐시 현황 및 셔틀·랙·오더 상태 조회/제어.
 */
@RestController
@RequestMapping("/rest/realtime")
public class RealTimeStatusController {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeStatusController.class);

    @Autowired
    private RealTimeProviderRegistry providerRegistry;

    @Autowired
    private RealTimeBroadcastScheduler broadcastScheduler;

    @Autowired
    private ActiveSubscriptionManager subscriptionManager;

    @Autowired
    private LayoutCoordinateService coordinateService;

    @Autowired
    private RealTimeStatusService realTimeStatusService;

    /**
     * 전체 실시간 시스템 상태 조회
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return ValueUtil.newMap(
                "activeBroadcasts,totalSubscriptions,runningTasks,enabledProviders,cachedCoordinates,timestamp",
                broadcastScheduler.getActiveBroadcasts(), // eqGroupId:pageId 형태의 키 목록
                subscriptionManager.getTotalSubscriptionCount(),
                broadcastScheduler.getRunningTaskCount(),
                getProviderInfo(),
                coordinateService.getCachedCoordinateCount(),
                System.currentTimeMillis()
        );
    }

    /**
     * 등록된 Provider 목록 조회
     */
    @GetMapping("/providers")
    public List<Map<String, Object>> getProviders() {
        return getProviderInfo();
    }

    /**
     * 브로드캐스트 수동 시작 (테스트용)
     */
    @PostMapping("/broadcast/start")
    public Map<String, Object> startBroadcast(
            @RequestParam String eqGroupId,
            @RequestParam String lcId,
            @RequestParam String pageId) {

        logger.warn("[ Realtime ][ Status ] manual broadcast start: eqGroupId={}, lcId={}, pageId={}", eqGroupId, lcId, pageId);
        broadcastScheduler.startBroadcast(eqGroupId, lcId, pageId);

        return ValueUtil.newMap(
                "success,message,timestamp",
                true, "Broadcast started for " + eqGroupId + ":" + pageId, System.currentTimeMillis()
        );
    }

    /**
     * 브로드캐스트 수동 중지 (테스트용). pageId 로 특정 페이지만 중지.
     */
    @PostMapping("/broadcast/stop")
    public Map<String, Object> stopBroadcast(
            @RequestParam String eqGroupId,
            @RequestParam String pageId) {

        logger.warn("[ Realtime ][ Status ] manual broadcast stop: eqGroupId={}, pageId={}", eqGroupId, pageId);
        broadcastScheduler.stopBroadcast(eqGroupId, pageId);

        return ValueUtil.newMap(
                "success,message,timestamp",
                true, "Broadcast stopped for " + eqGroupId + ":" + pageId, System.currentTimeMillis()
        );
    }

    /**
     * 모든 브로드캐스트 중지 (관리용)
     */
    @PostMapping("/broadcast/stop-all")
    public Map<String, Object> stopAllBroadcasts() {
        logger.warn("[ Realtime ][ Status ] stopping all broadcasts");
        broadcastScheduler.stopAllBroadcasts();

        return ValueUtil.newMap(
                "success,message,timestamp",
                true, "All broadcasts stopped", System.currentTimeMillis()
        );
    }

    /**
     * 좌표 캐시 리로드
     */
    @PostMapping("/coordinates/reload")
    public Map<String, Object> reloadCoordinates(
            @RequestParam String lcId,
            @RequestParam String pageId) {

        logger.info("[ Realtime ][ Status ] coordinates reloading: lcId={}, pageId={}", lcId, pageId);
        coordinateService.loadLayoutCoordinates(lcId, pageId);

        return ValueUtil.newMap(
                "success,cachedCount,currentPageId,timestamp",
                true, coordinateService.getCachedCoordinateCount(), coordinateService.getCurrentPageId(), System.currentTimeMillis()
        );
    }

    /**
     * 좌표 캐시 클리어
     */
    @PostMapping("/coordinates/clear")
    public Map<String, Object> clearCoordinates() {
        logger.info("[ Realtime ][ Status ] coordinate cache clearing");
        coordinateService.clearCache();

        return ValueUtil.newMap(
                "success,message,timestamp",
                true, "Coordinate cache cleared", System.currentTimeMillis()
        );
    }

    /**
     * 구독자 정보 상세 조회. eqGroupId:pageId 복합 키 기반 층별 구독 현황.
     */
    @GetMapping("/subscriptions")
    public Map<String, Object> getSubscriptions() {
        Set<String> activeBroadcasts = broadcastScheduler.getActiveBroadcasts();
        List<Map<String, Object>> subscriptions = new ArrayList<>();

        for (String broadcastKey : activeBroadcasts) {
            String[] parts = broadcastKey.split(":");
            String eqGroupId = parts[0];
            String pageId = parts.length > 1 ? parts[1] : "";

            subscriptions.add(ValueUtil.newMap(
                    "broadcastKey,eqGroupId,pageId,subscriberCount,isBroadcasting",
                    broadcastKey,
                    eqGroupId,
                    pageId,
                    broadcastScheduler.getSubscriberCount(eqGroupId, pageId),
                    true
            ));
        }

        return ValueUtil.newMap(
                "totalSubscriptions,subscriptions,timestamp",
                subscriptionManager.getTotalSubscriptionCount(), subscriptions, System.currentTimeMillis()
        );
    }

    /**
     * Provider 정보 목록 생성
     */
    private List<Map<String, Object>> getProviderInfo() {
        List<Map<String, Object>> providers = new ArrayList<>();
        for (RealTimeDataProvider<?> provider : providerRegistry.getAllProviders()) {
            providers.add(ValueUtil.newMap(
                    "type,topic,intervalMs,enabled,retryOnError",
                    provider.getProviderType(),
                    provider.getTopicPattern(),
                    provider.getIntervalMs(),
                    provider.isEnabled(),
                    provider.retryOnError()
            ));
        }
        return providers;
    }

    /**
     * 전체 셔틀카 상태 목록 조회
     */
    @GetMapping("/cars")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqCarMst> getAllCarStatus() {
        return this.realTimeStatusService.getAllCarStatus();
    }

    /**
     * 설비 그룹별 셔틀카 상태 목록 조회
     */
    @GetMapping("/cars/group/{eqGroupId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqCarMst> getCarStatusByGroup(@PathVariable String eqGroupId) {
        return this.realTimeStatusService.getCarStatusByGroup(eqGroupId);
    }

    /**
     * 단일 셔틀카 상태 조회
     */
    @GetMapping("/cars/{carId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEqCarMst getCarStatus(@PathVariable String carId) {
        return this.realTimeStatusService.getCarStatus(carId);
    }

    // 랙 재고 상태 조회

    @GetMapping("/racks")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqRackMst> getAllRackStatus() {
        return this.realTimeStatusService.getAllRackStatus();
    }

    @GetMapping("/racks/floor/{floor}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqRackMst> getRackStatusByFloor(@PathVariable Integer floor) {
        return this.realTimeStatusService.getRackStatusByFloor(floor);
    }

    @GetMapping("/racks/rack/{rackId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqRackMst> getRackStatusByRackId(@PathVariable String rackId) {
        return this.realTimeStatusService.getRackStatusByRackId(rackId);
    }

    @GetMapping("/racks/cell/{cellId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEqRackMst getCellStatus(@PathVariable String cellId) {
        return this.realTimeStatusService.getCellStatus(cellId);
    }

    @GetMapping("/racks/occupied")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqRackMst> getOccupiedCells() {
        return this.realTimeStatusService.getOccupiedCells();
    }

    @GetMapping("/racks/group/{eqGroupId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqRackMst> getRackCellsByGroup(@PathVariable String eqGroupId) {
        return this.realTimeStatusService.getRackCellsByGroup(eqGroupId);
    }

    // WCS 작업 상태 조회

    @GetMapping("/orders/status/{orderStatus}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbWcsShuttleOrder> getOrdersByStatus(@PathVariable Integer orderStatus) {
        return this.realTimeStatusService.getOrdersByStatus(orderStatus);
    }

    @GetMapping("/orders/shuttle/{shuttleId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbWcsShuttleOrder> getOrdersByShuttle(@PathVariable String shuttleId) {
        return this.realTimeStatusService.getOrdersByShuttle(shuttleId);
    }

    @GetMapping("/orders/{orderKey}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbWcsShuttleOrder getOrder(@PathVariable String orderKey) {
        return this.realTimeStatusService.getOrder(orderKey);
    }

    @GetMapping("/orders/completed/recent")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbWcsShuttleOrder> getRecentCompletedOrders(
            @RequestParam(defaultValue = "50") int limit) {
        return this.realTimeStatusService.getRecentCompletedOrders(limit);
    }

    // 대시보드 초기 데이터

    @GetMapping("/dashboard/shuttles/{lcId}/{pageId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, Object>> getDashboardShuttles(
            @PathVariable String lcId,
            @PathVariable String pageId) {
        return this.realTimeStatusService.getDashboardShuttles(lcId, pageId);
    }

    @GetMapping("/dashboard/cargos/{lcId}/{pageId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, Object>> getDashboardCargos(
            @PathVariable String lcId,
            @PathVariable String pageId) {
        return this.realTimeStatusService.getDashboardCargos(lcId, pageId);
    }

    // 작업 제어

    @PostMapping("/orders/{orderKey}/cancel")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> cancelOrder(
            @PathVariable String orderKey,
            @RequestBody(required = false) Map<String, String> params) {
        String reason = params != null ? params.get("reason") : null;
        return this.realTimeStatusService.requestCancelOrder(orderKey, reason);
    }

    @PostMapping("/orders/{orderKey}/resume")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> resumeOrder(@PathVariable String orderKey) {
        return this.realTimeStatusService.requestResumeOrder(orderKey);
    }

    @PostMapping("/orders/{orderKey}/priority")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> changePriority(
            @PathVariable String orderKey,
            @RequestBody Map<String, Object> params) {
        Integer newPriority = params.get("priority") != null
                ? ((Number) params.get("priority")).intValue()
                : null;
        return this.realTimeStatusService.requestChangePriority(orderKey, newPriority);
    }
}