package operato.logis.samsung.service.xyz;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.connector.api.service.ExternalApiService;
import operato.logis.samsung.WcsConstants;
import operato.logis.samsung.WcsUtils;
import operato.logis.samsung.consts.ProcessStatus;
import operato.logis.samsung.dto.xyz.XyzOrderRequest;
import operato.logis.samsung.entity.mw.*;
import operato.logis.samsung.entity.xyz.TbMwIfXyzOrder;
import operato.logis.samsung.event.BoxArrivedOnConveyorEvent;
import operato.logis.samsung.service.mw.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class XyzOrderSendService extends AbstractQueryService {

    private final ExternalApiService externalApiService;
    private final TbMwXyzOrderService tbMwXyzOrderService;
    private final TbMwItemMasterService tbMwItemMasterService;
    private final TbMwBoxConveyorInfoService tbMwBoxConveyorInfoService;
    private final TbMwInboundJobService tbMwInboundJobService;
    private final BoxTrackingService boxTrackingService;
    private final TbMwChuteManagementService tbMwChuteManagementService;

    /**
     * MW -> XYZ 적재 지시 송신
     *
     * 처리 개요
     * 1. 컨베이어 도착 이벤트에서 박스 목록 확인
     * 2. 첫 박스 기준으로 Box / Job / ItemMaster 조회
     * 3. 단일 SKU 모드 여부 확인
     * 4. 1SKU 모드면 포인트별 분할 오더 라우팅 로직 수행
     * 5. 그 외는 기존 단일 활성오더 기반 로직 수행
     *
     * @param event BoxConveyor에 Box가 도착했음을 알리는 이벤트 객체
     */
    @Transactional
    @Async
    @EventListener(classes = BoxArrivedOnConveyorEvent.class)
    public void sendOrder(BoxArrivedOnConveyorEvent event) {
        // 이벤트 수신 후 도메인 컨텍스트 설정
        // 비동기 이벤트 처리 시 도메인 정보가 없으면 DB 처리 중 Null 관련 오류가 날 수 있음
        WcsUtils.setupDomainContext();

        // 이벤트에 박스 정보가 없으면 처리 불가
        List<TbMwBoxConveyorInfo> boxList = event.getBoxList();
        if (ValueUtil.isEmpty(boxList)) {
            logger.error("[XYZ] BoxConveyor {} 도착 Event에 BoxList 정보가 없습니다.", event.getBoxConveyorCd());
            return;
        }

        // 첫 번째 박스를 기준으로 BL / CNTR / ITEM 추적
        // 현재 이벤트는 동일 SKU 흐름을 전제로 처리하므로 첫 박스 기준으로 조회
        TbMwBox boxInfo = boxTrackingService.findBoxByBoxId(boxList.get(0).getSerialNo());
        logger.info("[XYZ][BOXINFO] {}", boxInfo);
        if (ValueUtil.isEmpty(boxInfo)) {
            logger.error("[XYZ] BoxConveyor {} 도착 Event에 box 정보가 없습니다.", boxList.get(0).getSerialNo());
            return;
        }
        // 품목 마스터 조회
        String itemCode = boxList.get(0).getItemCode();
        TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(itemCode);
        if (ValueUtil.isEmpty(itemMaster)) {
            logger.error("[XYZ] BoxConveyor {} 도착 Event에 대한 itemMaster 정보가 없습니다.", itemCode);
            return;
        }

        // BCR/VISION 평균 치수 조회
        TbMwBcrItemDimensionAvg itemDimensionAvg = tbMwItemMasterService.getItemDimension(itemCode);

        // 설정값으로 1SKU 모드 여부 제어
        String isSingleSku = SettingUtil.getValue("mw.order.single.sku", "false");


        /**
         * 1SKU 모드 로직
         *
         * 핵심 개념
         * - 1컨테이너 1SKU 인 경우, 해당 SKU 는 여러 chute/point 에 분할 오더로 배정됨
         * - 현재 라운드에서 먼저 시작한 파렛트는 만재까지 우선 적재
         * - 한 포인트가 현재 파렛트 만재되면, 다음 포인트의 현재 파렛트를 채움
         * - 박스가 2개 들어와도 1개씩 독립 판단하여 분할 전송 가능하도록 1Box = 1Task 방식 사용
         */
        // 해당 박스가 속한 Inbound Job 조회
        TbMwInboundJob job = tbMwInboundJobService.getInboundJobByBlCntr(boxInfo.getBlNo(), boxInfo.getCntrNo());

        boolean isSplitSingleSku =
                ValueUtil.isNotEmpty(job)
                        && job.getSkuQty() == 1
                        && "true".equals(isSingleSku);

        /*if (isSplitSingleSku) {
            // 해당 아이템이 현재 할당된 chute 목록 조회
            List<TbMwChute> chuteList = tbMwChuteManagementService.getChuteListByItemCode(itemMaster.getInnerItemCode());

            // 해당 아이템에 대해 이미 생성/진행 중인 적재 오더 목록 조회
            List<TbMwXyzOrder> activeOrders = tbMwXyzOrderService.getActiveAndPendingOrderList(itemMaster.getInnerItemCode());

            // chute 또는 오더가 없으면 라우팅 불가
            if (ValueUtil.isEmpty(chuteList) || ValueUtil.isEmpty(activeOrders)) {
                logger.error("[XYZ][1SKU] chute 또는 활성 오더가 없습니다. itemCode : {}", itemCode);
                return;
            }

            // 1파렛트 만재 수량
            int palletCapacity = Integer.parseInt(itemMaster.getPalletCapacity());

            // chute.order_id -> chute 매핑
            // 실제로 어떤 오더가 어떤 포인트에 물려있는지 확인하기 위한 맵
            Map<String, TbMwChute> chuteByOrderId = chuteList.stream()
                    .filter(chute -> ValueUtil.isNotEmpty(chute.getOrderId()))
                    .collect(Collectors.toMap(
                            TbMwChute::getOrderId,
                            Function.identity(),
                            (a, b) -> a
                    ));

            // 오더별 현재 적재 진행 상태(RouteState) 구성
            // sentQty 는 해당 오더로 이미 내려간 Task 수량(= 1SKU 모드에서는 박스 수량과 동일) 기준
            List<RouteState> routeStates = buildRouteStates(activeOrders, chuteByOrderId, palletCapacity);

            // 실제 chute 에 매핑된 활성 오더가 하나도 없으면 처리 불가
            if (ValueUtil.isEmpty(routeStates)) {
                logger.error("[XYZ][1SKU] itemCode : {} 에 대한 chute 매핑 활성 오더가 없습니다.", itemCode);
                return;
            }

            *//**
             *
             *  *박스를 1 개씩만 무조건 보내는 것이 아니라,
             *  *현재 선택된 포인트가 이번 턴에 같은 파렛트로 몇 개까지 받을 수 있는지 계산해서 sendQty 를 결정한다.
             *  *
             *  *예)
             *  *-현재 포인트가 4 / 5 상태이고 boxList 가 2 개이면 -> sendQty = 1
                    * * =>현재 포인트 1 개 적재 후 만재, 다음 루프에서 다른 포인트 선택
             *  *
             *  *-현재 포인트가 2 / 5 상태이고 boxList 가 2 개이면 -> sendQty = 2
                    * * =>두 박스를 한 번에 같은 포인트로 전송
                    *
             **//*
            for (TbMwBoxConveyorInfo box : boxList) {
                // 현재 시점 기준으로 가장 우선순위가 높은 RouteState 선택
                RouteState selected = selectRouteState(routeStates);

                // 더 이상 배정 가능한 오더가 없으면 미할당 로그 후 다음 박스 진행
                if (selected == null) {
                    logger.warn("[XYZ][1SKU] itemCode : {} / 미할당 박스 발생. serialNo : {}",
                            itemCode, box.getSerialNo());
                    continue;
                }

                TbMwXyzOrder order = selected.getOrder();
                TbMwChute chute = selected.getChute();

                // 실제 어느 박스가 어느 오더/포인트로 가는지 로그 기록
                logger.info("[XYZ][1SKU] 작업 박스 정보 Conveyor : {}, Index : {}, Serial : {}, Barcode : {}, Pid : {}, OrderId : {}, Chute : {}",
                        event.getBoxConveyorCd(),
                        box.getIndex(),
                        box.getSerialNo(),
                        box.getItemCode(),
                        box.getPid(),
                        order.getOrderId(),
                        chute.getEndPointCd());

                // 1SKU 모드는 1Box = 1Task
                // 박스 단위로 쪼개서 라우팅해야 특이 케이스(4/5 상태에서 2박스 유입 등)를 자연스럽게 처리 가능
                TbMwIfXyzOrder task = TbMwIfXyzOrder.setXyzOrderByDimension(order, itemDimensionAvg, itemMaster, 1);
                XyzOrderRequest request = XyzOrderRequest.fromXyzOrder(task, 1);

                String url = WcsConstants.XYZ_SERVER_URL + "/adaptor/api/MW/order";

                try {
                    logger.info("[XYZ][1SKU] Order Send API Body : {}", WcsUtils.logRequestBody(request));
                    Mono<CommonApiResponse> responseMono = externalApiService.post(url, request, CommonApiResponse.class);
                    CommonApiResponse result = responseMono.block();
                    logger.info("[XYZ][1SKU] Order Send API Result : {}", WcsUtils.logRequestBody(result));

                    // XYZ 송신 성공 후, 실제 배정된 end_point_cd 반영
                    TbMwBox targetBox = boxTrackingService.findBoxByBoxId(box.getSerialNo());
                    if (ValueUtil.isNotEmpty(targetBox)) {
                        targetBox.setEndPointCd(chute.getEndPointCd());
                        boxTrackingService.updateTbMwBox(targetBox);
                    } else {
                        logger.warn("[XYZ][1SKU] end_point_cd 업데이트 대상 box 가 없습니다. serialNo : {}", box.getSerialNo());
                    }
                }
                catch (ElidomRuntimeException e) {
                    logger.error("[XYZ][1SKU] Order {} 생성 중 에러 발생 : {}", order.getOrderId(), e.getMessage());
                    return;
                }

                // XYZ 지시 이력 저장
                this.queryManager.insert(task);

                // 오더의 TaskNo 증가
                tbMwXyzOrderService.incrementTaskNo(order);

                // 이번 박스와 TaskId 매핑 이력 저장
                tbMwBoxConveyorInfoService.createBoxConveyorInfo(
                        Collections.singletonList(box),
                        task.getTaskId()
                );

                // 메모리 상 RouteState 에도 즉시 반영
                // 다음 박스 라우팅 시 "방금 1개 보낸 상태" 기준으로 우선순위를 다시 계산해야 함
                selected.addSentQty(1);
            }

            return;
        }*/
        if (isSplitSingleSku) {
            // 해당 아이템이 현재 할당된 chute 목록 조회
            List<TbMwChute> chuteList = tbMwChuteManagementService.getChuteListByItemCode(itemMaster.getInnerItemCode());

            // 해당 아이템에 대해 이미 생성/진행 중인 적재 오더 목록 조회
            List<TbMwXyzOrder> activeOrders = tbMwXyzOrderService.getActiveAndPendingOrderList(itemMaster.getInnerItemCode());

            // chute 또는 오더가 없으면 라우팅 불가
            if (ValueUtil.isEmpty(chuteList) || ValueUtil.isEmpty(activeOrders)) {
                logger.error("[XYZ][1SKU] chute 또는 활성 오더가 없습니다. itemCode : {}", itemCode);
                return;
            }

            // 1파렛트 만재 수량
            int palletCapacity = Integer.parseInt(itemMaster.getPalletCapacity());

            // chute.order_id -> chute 매핑
            // 실제로 어떤 오더가 어떤 포인트에 물려있는지 확인하기 위한 맵
            Map<String, TbMwChute> chuteByOrderId = chuteList.stream()
                    .filter(chute -> ValueUtil.isNotEmpty(chute.getOrderId()))
                    .collect(Collectors.toMap(
                            TbMwChute::getOrderId,
                            Function.identity(),
                            (a, b) -> a
                    ));

            // 오더별 현재 적재 진행 상태(RouteState) 구성
            // sentQty 는 해당 오더로 이미 내려간 Task 수량(= 1SKU 모드에서는 박스 수량과 동일) 기준
            List<RouteState> routeStates = buildRouteStates(activeOrders, chuteByOrderId, palletCapacity);

            // 실제 chute 에 매핑된 활성 오더가 하나도 없으면 처리 불가
            if (ValueUtil.isEmpty(routeStates)) {
                logger.error("[XYZ][1SKU] itemCode : {} 에 대한 chute 매핑 활성 오더가 없습니다.", itemCode);
                return;
            }

            /*
             * 박스를 무조건 1개씩만 보내는 것이 아니라,
             * 현재 선택된 포인트가 이번 턴에 같은 파렛트로 몇 개까지 받을 수 있는지 계산해서 sendQty 를 결정한다.
             *
             * 예)
             * - 현재 포인트가 4/5 상태이고 boxList 가 2개이면 -> sendQty = 1
             *   => 현재 포인트 1개 적재 후 만재, 다음 루프에서 다른 포인트 선택
             *
             * - 현재 포인트가 2/5 상태이고 boxList 가 2개이면 -> sendQty = 2
             *   => 두 박스를 한 번에 같은 포인트로 전송
             */
            int fromIndex = 0;

            while (fromIndex < boxList.size()) {
                // 현재 시점 기준으로 가장 우선순위가 높은 RouteState 선택
                RouteState selected = selectRouteState(routeStates);

                // 더 이상 배정 가능한 오더가 없으면 미할당 로그 후 종료
                if (selected == null) {
                    logger.warn("[XYZ][1SKU] itemCode : {} / 미할당 박스 발생. remainBoxCount : {}",
                            itemCode, boxList.size() - fromIndex);
                    break;
                }

                TbMwXyzOrder order = selected.getOrder();
                TbMwChute chute = selected.getChute();

                // 이번 턴에 아직 보내지 않은 박스 수
                int remainBoxCount = boxList.size() - fromIndex;

                // 현재 선택된 포인트가 이번 턴에 같은 파렛트로 받을 수 있는 최대 수량
                int sendQty = Math.min(remainBoxCount, selected.getCurrentPalletSendableQty());

                if (sendQty <= 0) {
                    logger.warn("[XYZ][1SKU] itemCode : {} / sendQty 계산 결과가 0 이하입니다. orderId : {}",
                            itemCode, order.getOrderId());
                    break;
                }

                List<TbMwBoxConveyorInfo> targetBoxes = boxList.subList(fromIndex, fromIndex + sendQty);

                // 실제 어느 박스가 어느 오더/포인트로 가는지 로그 기록
                for (TbMwBoxConveyorInfo box : targetBoxes) {
                    logger.info("[XYZ][1SKU] 작업 박스 정보 Conveyor : {}, Index : {}, Serial : {}, Barcode : {}, Pid : {}, OrderId : {}, Chute : {}, sendQty : {}",
                            event.getBoxConveyorCd(),
                            box.getIndex(),
                            box.getSerialNo(),
                            box.getItemCode(),
                            box.getPid(),
                            order.getOrderId(),
                            chute.getEndPointCd(),
                            sendQty);
                }

                // 현재 포인트로 한 번에 보낼 수 있는 수량만큼 Task 생성
                TbMwIfXyzOrder task = TbMwIfXyzOrder.setXyzOrderByDimension(order, itemDimensionAvg, itemMaster, sendQty);
                XyzOrderRequest request = XyzOrderRequest.fromXyzOrder(task, 1);

                String url = WcsConstants.XYZ_SERVER_URL + "/adaptor/api/MW/order";

                try {
                    logger.info("[XYZ][1SKU] Order Send API Body : {}", WcsUtils.logRequestBody(request));
                    Mono<CommonApiResponse> responseMono = externalApiService.post(url, request, CommonApiResponse.class);
                    CommonApiResponse result = responseMono.block();
                    logger.info("[XYZ][1SKU] Order Send API Result : {}", WcsUtils.logRequestBody(result));

                    // XYZ 송신 성공 후, 실제 배정된 end_point_cd 반영
                    for (TbMwBoxConveyorInfo box : targetBoxes) {
                        TbMwBox targetBox = boxTrackingService.findBoxByBoxId(box.getSerialNo());
                        if (ValueUtil.isNotEmpty(targetBox)) {
                            targetBox.setEndPointCd(chute.getEndPointCd());
                            boxTrackingService.updateTbMwBox(targetBox);
                        } else {
                            logger.warn("[XYZ][1SKU] end_point_cd 업데이트 대상 box 가 없습니다. serialNo : {}", box.getSerialNo());
                        }
                    }
                }
                catch (ElidomRuntimeException e) {
                    logger.error("[XYZ][1SKU] Order {} 생성 중 에러 발생 : {}", order.getOrderId(), e.getMessage());
                    return;
                }

                // XYZ 지시 이력 저장
                this.queryManager.insert(task);

                // 오더의 TaskNo 증가
                tbMwXyzOrderService.incrementTaskNo(order);

                // 이번 박스들과 TaskId 매핑 이력 저장
                tbMwBoxConveyorInfoService.createBoxConveyorInfo(targetBoxes, task.getTaskId());

                // 메모리 상 RouteState 즉시 반영
                selected.addSentQty(sendQty);

                // 다음 미처리 박스 위치 갱신
                fromIndex += sendQty;
            }

            return;
        }
        else { // 기존 다SKU 로직 유지
            TbMwXyzOrder order = tbMwXyzOrderService.getActiveAndPendingOrder(itemCode);

            // MW 주문 정보가 없는 경우
            if (ValueUtil.isEmpty(order) || !order.getProcessStatus().equals(ProcessStatus.ORDER_START.value())) {
                logger.error("[XYZ] 박스 바코드 {}에 해당하는 MW 주문 정보가 존재하지 않습니다.", itemCode);
                return;
            }

            // Box 정보 로그 출력
            for (TbMwBoxConveyorInfo box : boxList) {
                logger.info("[XYZ] 작업 박스 정보 Conveyor : {}, Index : {}, Serial : {}, Barcode : {}, Pid : {}",
                        event.getBoxConveyorCd(), box.getIndex(), box.getSerialNo(), box.getItemCode(), box.getPid());
            }

            // 다SKU 는 기존처럼 이벤트 단위 전체 boxList 를 하나의 Task 로 전송
            TbMwIfXyzOrder task = TbMwIfXyzOrder.setXyzOrderByDimension(order, itemDimensionAvg, itemMaster, boxList.size());
            XyzOrderRequest request = XyzOrderRequest.fromXyzOrder(task, 0);

            // API URL 설정
            String xyzServerIP = WcsConstants.XYZ_SERVER_URL;
            String endPoint = "/adaptor/api/MW/order";
            String url = xyzServerIP + endPoint;

            // XYZ 응답 수신
            try {
                logger.info("[XYZ] Order Send API Body : {}", WcsUtils.logRequestBody(request));
                Mono<CommonApiResponse> responseMono = externalApiService.post(url, request, CommonApiResponse.class);
                CommonApiResponse result = responseMono.block();
                logger.info("[XYZ] Order Send API Result : {}", WcsUtils.logRequestBody(result));
            }
            catch (ElidomRuntimeException e) {
                logger.error("[XYZ] Order {} 생성 중 에러 발생 : {}", order.getOrderId(), e.getMessage());
            }

            // XYZ API 이력 생성
            this.queryManager.insert(task);

            // TaskNo 1 증가
            tbMwXyzOrderService.incrementTaskNo(order);

            // BoxConveyor Event 정보 저장
            tbMwBoxConveyorInfoService.createBoxConveyorInfo(boxList, task.getTaskId());
        }
    }

    /**
     * 활성 오더 목록을 RouteState 목록으로 변환
     *
     * RouteState 에는 아래 정보가 들어감
     * - 실제 오더
     * - 해당 오더가 할당된 chute
     * - 파렛트 만재수량
     * - 이미 송신된 수량(sentQty)
     *
     * sentQty 계산 기준
     * - 1SKU 모드에서는 1Box = 1Task 로 운영한다는 전제
     * - 따라서 getSendedOrderList(orderId).size() 를 곧바로 보낸 박스 수로 사용
     *
     * 주의
     * - 과거에 2Box 이상을 1Task 로 송신한 이력이 섞여 있으면 이 부분은 보정 필요
     */
    private List<RouteState> buildRouteStates(
            List<TbMwXyzOrder> activeOrders,
            Map<String, TbMwChute> chuteByOrderId,
            int palletCapacity
    ) {
        return activeOrders.stream()
                .filter(order -> ProcessStatus.ORDER_START.value().equals(order.getProcessStatus()))
                .filter(order -> chuteByOrderId.containsKey(order.getOrderId()))
                .map(order -> {
                    TbMwChute chute = chuteByOrderId.get(order.getOrderId());

                    // 1SKU 모드 기준: 1Task == 1Box
                    int sentBoxQty = tbMwXyzOrderService.getSendedBoxQty(order.getOrderId());

                    return new RouteState(order, chute, palletCapacity, sentBoxQty);
                })
                .collect(Collectors.toList());
    }

    /**
     * 현재 시점에서 다음 박스를 어느 오더/포인트로 보낼지 선택
     *
     * 내부 compareRouteState() 비교 로직을 통해 가장 우선순위가 높은 상태를 선정
     * totalRemainQty <= 0 인 오더는 이미 종료 상태로 보고 제외
     */
    private RouteState selectRouteState(List<RouteState> routeStates) {
        RouteState selected = null;

        for (RouteState state : routeStates) {
            if (state.getTotalRemainQty() <= 0) {
                continue;
            }

            if (selected == null || compareRouteState(state, selected) < 0) {
                selected = state;
            }
        }

        return selected;
    }

    /**
     * RouteState 우선순위 비교
     *
     * 정렬 기준
     * 1. 완료한 풀파렛트 수가 적은 오더 우선
     *    - 라운드 균형 유지 목적
     *
     * 2. 같은 라운드라면 이미 시작된 파렛트가 있는 오더 우선
     *    - 한번 시작한 파렛트는 우선 만재 처리
     *
     * 3. 열린 파렛트가 있다면, 만재까지 남은 수량이 적은 오더 우선
     *    - 4/5, 3/5 상태면 먼저 닫아줌
     *
     * 4. 둘 다 새 파렛트 시작 상태면 전체 남은 적재수량이 큰 오더 우선
     *    - 예: PC02 11개, PC01 10개면 PC02 먼저 시작
     *
     * 5. 같으면 포인트 번호 작은 순
     *    - PC01, PC02 같은 tie-breaker
     *
     * 6. 마지막은 order priority
     */
    private int compareRouteState(RouteState s1, RouteState s2) {
        int cmp;

        // 1. 완료한 풀파렛트 수가 적은 오더 우선
        cmp = Integer.compare(s1.getFinishedFullPalletCount(), s2.getFinishedFullPalletCount());
        if (cmp != 0) {
            return cmp;
        }

        // 2. 같은 라운드에서는 이미 시작한 파렛트가 있으면 그 오더 우선
        cmp = Integer.compare(s1.hasOpenPallet() ? 0 : 1, s2.hasOpenPallet() ? 0 : 1);
        if (cmp != 0) {
            return cmp;
        }

        // 3. 열린 파렛트가 있으면 만재까지 남은 수량이 적은 오더 우선
        cmp = Integer.compare(s1.getCurrentPalletRemainQtyForSort(), s2.getCurrentPalletRemainQtyForSort());
        if (cmp != 0) {
            return cmp;
        }

        // 4. 둘 다 새 파렛트 시작 상태면 전체 남은 적재수량이 큰 오더 우선
        cmp = Integer.compare(s2.getTotalRemainQty(), s1.getTotalRemainQty());
        if (cmp != 0) {
            return cmp;
        }

        // 5. 같으면 포인트 번호 작은 순
        cmp = Integer.compare(s1.getPointNo(), s2.getPointNo());
        if (cmp != 0) {
            return cmp;
        }

        // 6. 마지막은 priority
        return Integer.compare(s1.getOrder().getPriority(), s2.getOrder().getPriority());
    }

    /**
     * 1SKU 모드 라우팅 상태 클래스
     *
     * 의미
     * - TbMwXyzOrder 1건 + 연결된 chute + 현재까지 보낸 박스 수량을 묶어 관리
     *
     * 왜 필요한가
     * - DB 오더 정보만으로는 "현재 라운드에서 어느 파렛트가 얼마나 찼는지" 계산이 번거로움
     * - 한 이벤트 안에서도 박스 1개를 보낸 뒤 다음 박스는 우선순위가 달라질 수 있으므로
     *   메모리 상에서 즉시 상태를 갱신할 객체가 필요함
     */
    private static class RouteState {
        private final TbMwXyzOrder order;
        private final TbMwChute chute;
        private final int palletCapacity;
        private int sentQty;

        private RouteState(TbMwXyzOrder order, TbMwChute chute, int palletCapacity, int sentQty) {
            this.order = order;
            this.chute = chute;
            this.palletCapacity = palletCapacity;
            this.sentQty = sentQty;
        }

        public TbMwXyzOrder getOrder() {
            return order;
        }

        public TbMwChute getChute() {
            return chute;
        }

        /**
         * 실제 적재 대상 수량
         *
         * targetNum 에서 NG 수량을 제외한 값
         * NG 는 실제 적재되지 않는 수량이므로 파렛트 적재 목표에서 제외
         */
        public int getLoadTargetQty() {
            return Math.max(order.getTargetNum() - order.getNgQty(), 0);
        }

        /**
         * 유효한 누적 송신 수량
         *
         * sentQty 가 음수가 될 일은 없지만 방어적으로 0 보정
         * 또한 load target 을 초과하지 않도록 clamp 처리
         */
        public int getEffectiveSentQty() {
            int targetQty = getLoadTargetQty();
            if (sentQty < 0) {
                return 0;
            }
            return Math.min(sentQty, targetQty);
        }

        /**
         * 전체 남은 적재 수량
         *
         * 이 오더가 아직 얼마나 더 적재되어야 하는지 계산
         */
        public int getTotalRemainQty() {
            return Math.max(getLoadTargetQty() - getEffectiveSentQty(), 0);
        }

        /**
         * 지금까지 완료한 풀파렛트 수
         *
         * 예: 만재수량 5, sentQty 12 이면
         * 완료한 풀파렛트는 2개
         */
        public int getFinishedFullPalletCount() {
            return getEffectiveSentQty() / palletCapacity;
        }

        /**
         * 현재 진행 중인 파렛트가 열려 있는지 여부
         *
         * 예)
         * - sentQty % palletCapacity > 0 이면 현재 파렛트에 일부 적재가 시작된 상태
         * - 동시에 totalRemainQty > 0 이어야 아직 진행 중인 파렛트라고 볼 수 있음
         */
        public boolean hasOpenPallet() {
            return getTotalRemainQty() > 0 && (getEffectiveSentQty() % palletCapacity) > 0;
        }

        /**
         * 현재 열려 있는 파렛트가 만재되기까지 남은 수량
         *
         * 정렬용 값
         * - 열린 파렛트가 없다면 Integer.MAX_VALUE 반환
         * - 즉, 이미 시작한 파렛트가 있는 오더가 항상 새 파렛트 시작 상태보다 우선
         */
        public int getCurrentPalletRemainQtyForSort() {
            if (!hasOpenPallet()) {
                return Integer.MAX_VALUE;
            }

            int currentProgress = getEffectiveSentQty() % palletCapacity;
            return palletCapacity - currentProgress;
        }

        /**
         * chute 포인트 번호 추출
         *
         * 예)
         * - PC01 -> 1
         * - PC02 -> 2
         *
         * tie-breaker 용도
         */
        public int getPointNo() {
            String pointCd = chute.getEndPointCd();
            if (ValueUtil.isEmpty(pointCd)) {
                return Integer.MAX_VALUE;
            }

            String numberOnly = pointCd.replaceAll("[^0-9]", "");
            if (ValueUtil.isEmpty(numberOnly)) {
                return Integer.MAX_VALUE;
            }

            return Integer.parseInt(numberOnly);
        }

        /**
         * 박스 1개 전송 후 sentQty 즉시 반영
         *
         * 같은 이벤트 안에서 다음 박스를 선택할 때
         * 방금 전송한 결과를 반영한 상태로 우선순위를 다시 계산하기 위함
         */
        public void addSentQty(int qty) {
            this.sentQty += qty;
        }

        public int getCurrentPalletSendableQty() {
            int totalRemainQty = getTotalRemainQty();
            if (totalRemainQty <= 0) {
                return 0;
            }

            int currentProgress = getEffectiveSentQty() % palletCapacity;

            // 새 파렛트 시작 상태
            if (currentProgress == 0) {
                return Math.min(totalRemainQty, palletCapacity);
            }

            // 현재 진행 중인 파렛트에 추가 적재 가능한 수량
            return Math.min(totalRemainQty, palletCapacity - currentProgress);
        }
    }
}