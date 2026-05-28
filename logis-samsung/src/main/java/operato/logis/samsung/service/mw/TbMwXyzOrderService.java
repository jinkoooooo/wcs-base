package operato.logis.samsung.service.mw;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.WcsUtils;
import operato.logis.samsung.consts.InboundStatus;
import operato.logis.samsung.consts.JobStatus;
import operato.logis.samsung.consts.ProcessStatus;
import operato.logis.samsung.consts.TrackingStatus;
import operato.logis.samsung.entity.mw.*;
import operato.logis.samsung.entity.xyz.TbMwIfXyzOrder;
import operato.logis.samsung.event.BoxTrackingEvent;
import operato.logis.samsung.service.xyz.XyzPalletService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TbMwXyzOrderService extends AbstractQueryService {

    private final TbMwChuteManagementService tbMwChuteManagementService;
    private final TbMwItemMasterService tbMwItemMasterService;
    private final TbMwInboundDeliveryService tbMwInboundDeliveryService;
    private final TbMwInboundJobService tbMwInboundJobService;
    private final XyzPalletService xyzPalletService;

    /**
     * BCR Box 스캔 시 호출되어 TbMwXyzOrder를 생성하는 메소드
     *
     * @param event itemCode와 bcrNo를 담은 이벤트 객체
     */
    @Transactional
    @Async
    @EventListener(classes = BoxTrackingEvent.class, condition = "#event.bcrNo == 'BCR-01'")
    public void handleBcrScanning(BoxTrackingEvent event) {
        // 이벤트 수신 후 도메인 설정 (안하면 도메인 Null 에러 발생)
        WcsUtils.setupDomainContext();

        // 1. 박스 품번 확인
        String innerItemCode = event.getItemCode();

        // 아이템코드 유효성 확인
        TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(innerItemCode);
        if (ValueUtil.isEmpty(itemMaster) || ValueUtil.isEmpty(itemMaster.getItemCode())) {
            logger.warn("[XYZ] Barcode : {} 에 대한 ItemMaster Is Empty", innerItemCode);
            return;
        }

        // 시작 컨테이너 여부 확인
        TbMwInboundDelivery delivery = isItemIncludedInCurrentRunningJob(innerItemCode);

        if (ValueUtil.isNotEmpty(delivery)) {
            TbMwInboundJob runningJob = tbMwInboundJobService.getInboundJobByBlCntr(delivery.getBlNo(), delivery.getCntrNo());

            // Job 유효성 검사
            if (ValueUtil.isEmpty(runningJob) || !JobStatus.RUNNING.value().equals(runningJob.getJobStatus())) {
                logger.error("[XYZ] 시작 컨테이너 여부 확인 {}/{} RUNNING job 정보가 없습니다.", runningJob.getBlNo(), runningJob.getCntrNo());
                return;
            }

            // 싱글 스큐 모드 사용여부
            String isSingleSku = SettingUtil.getValue("mw.order.single.sku", "false");

            // 20260423.JJG : 컨테이너 내 단일 SKU(총 박스 수 1 Pallet 이상)의 경우 로직 구분
            if(runningJob.getSkuQty() == 1
                    && isSingleSku.equals("true")
                    && delivery.getItemQty() > Integer.parseInt(itemMaster.getPalletCapacity())){

                // 기존 주문 확인
                TbMwXyzOrder order = getActiveAndPendingOrder(innerItemCode);

                // 이미 계산되고 할당되어 실행중인 경우 걸러내기.
                if (ValueUtil.isNotEmpty(order)) {
                    processExistingOrder(order);
                }
                // 3-b. 신규 주문 생성
                else {
                    // 20260423.JJG.신규로직(현업요청) : 1컨테이너 1SKU 처리로직 수행
                    processNewOrderOneSku(delivery, itemMaster);
                }

            }else{
                // 단일이 아닐 경우 기존 로직.
                // 기존 주문 확인
                TbMwXyzOrder order = getActiveAndPendingOrder(innerItemCode);

                // 기존 주문 처리
                if (ValueUtil.isNotEmpty(order)) {
                    processExistingOrder(order);
                }
                // 3-b. 신규 주문 생성
                else {
                    processNewOrder(delivery, innerItemCode);
                }
            }

        } else {
            logger.info("[XYZ] Barcode : {} 는 현재 RUNNING Job 대상 품목이 아니므로 주문 생성하지 않습니다.", innerItemCode);
        }
    }

    /**
     * 현재 RUNNING 상태인 Job의 inboundDelivery 목록 안에
     * 해당 itemCode가 포함되어 있는지 확인합니다.
     *
     * 주의:
     * - event의 itemCode는 스캔값일 수 있으므로 itemMaster 기준 itemCode로 정규화해서 비교합니다.
     * - Job 조회는 getInboundJobByDate(new Date()) 기준, 즉 "오늘 날짜" 기준입니다.
     */
    private TbMwInboundDelivery isItemIncludedInCurrentRunningJob(String scannedItemCode) {
        if (ValueUtil.isEmpty(scannedItemCode)) {
            return null;
        }

        // 1. 스캔값 -> 마스터 itemCode 정규화
        TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(scannedItemCode);
        if (ValueUtil.isEmpty(itemMaster) || ValueUtil.isEmpty(itemMaster.getItemCode())) {
            logger.warn("[XYZ] Barcode : {} 에 대한 itemMaster가 없어 RUNNING Job 포함 여부 확인 불가", scannedItemCode);
            return null;
        }

        String targetItemCode = itemMaster.getItemCode();

        // 현재 실행중인 Job 조회.
        List<TbMwInboundJob> jobList = tbMwInboundJobService.getInboundJobByStatus(JobStatus.RUNNING);
        if (ValueUtil.isEmpty(jobList)) {
            logger.info("[XYZ] RUNNING Inbound Job 이 존재하지 않습니다. Barcode : {}", scannedItemCode);
            return null;
        }

        // 3. RUNNING Job만 대상으로 해당 Job의 delivery 목록에서 itemCode 존재 여부 확인
        for (TbMwInboundJob job : jobList) {
            if (ValueUtil.isEmpty(job) || !JobStatus.RUNNING.value().equals(job.getJobStatus())) {
                continue;
            }

            List<TbMwInboundDelivery> deliveryList =
                    tbMwInboundDeliveryService.getInboundDeliveryList(job.getBlNo(), job.getCntrNo());

            if (ValueUtil.isEmpty(deliveryList)) {
                continue;
            }

            for (TbMwInboundDelivery delivery : deliveryList) {
                if (ValueUtil.isEmpty(delivery)) {
                    continue;
                }

                if (targetItemCode.equals(delivery.getItemCode())) {
                    logger.info("[XYZ] Barcode : {} / ItemCode : {} 는 RUNNING Job 대상입니다. B/L NO : {}, CNTR NO : {}",
                            scannedItemCode, targetItemCode, job.getBlNo(), job.getCntrNo());
                    return delivery;
                }
            }
        }

        logger.info("[XYZ] Barcode : {} / ItemCode : {} 는 현재 RUNNING Job delivery 대상이 아닙니다.",
                scannedItemCode, targetItemCode);
        return null;
    }

    /**
     * 이미 존재하는 주문을 처리합니다.
     * (대기 상태일 경우에만 슈트 할당을 시도합니다.)
     *
     * @param order 기존 주문 엔티티
     */
    private void processExistingOrder(TbMwXyzOrder order) {
        // 대기 상태가 아니면 (이미 진행 중이거나 다른 상태) 로직을 종료합니다.
        if (!order.getProcessStatus().equals(ProcessStatus.ORDER_READY.value())) {
            logger.info("[XYZ] 바코드 : {}에 대한 작업이 진행 중입니다. Order ID : {}", order.getItemCode(), order.getOrderId());
            return;
        }

        // 가용 슈트가 있으면 작업을 시작(업데이트)합니다.
        boolean started = tryStartOrderWithAvailableChute(order);
        if (started) {
            this.queryManager.update(order, "startPointCd", "endPointCd", "startDatetime", "processStatus");
        }
        // 슈트가 없으면 'ORDER_READY' 상태 그대로 유지되며, 별도 update는 불필요합니다.
    }

    /**
     * 신규 스캔된 품목의 주문을 생성하고 처리합니다.
     *
     * @param delivery 입고 상품 상세 주문
     * @param innerItemCode 실제 스캔 상품 88 코드
     */
    private void processNewOrder(TbMwInboundDelivery delivery, String innerItemCode) {

        // 1. 신규 주문 엔티티 생성 (상태: ORDER_READY)
        TbMwXyzOrder newOrder = buildNewOrder(delivery);
        if (ValueUtil.isEmpty(newOrder)) {
            return;
        }

        // 2. 가용 슈트가 있으면 즉시 작업을 시작합니다. (newOrder 객체의 상태가 ORDER_START로 변경됨)
        tryStartOrderWithAvailableChute(newOrder);

        // 3. 주문 정보를 등록합니다. (상태: ORDER_READY 또는 ORDER_START)
        // 2025-12-10 수정 : 다른 작업 중 Manual 재고로 변경 가능하므로 가용 Chute가 없다면 작업 대기 상태로 등록하지 않고 즉시 종료
        if (ProcessStatus.ORDER_START.value().equals(newOrder.getProcessStatus())) {
            logger.info("[XYZ] 바코드 : {}에 대한 작업을 등록합니다. Order ID : {}", newOrder.getItemCode(), newOrder.getOrderId());
            this.queryManager.insert(newOrder);

            // 작업 시작 (Delivery 상태 변경)
            tbMwInboundJobService.updateDeliveryStatus(
                    delivery.getBlNo(),
                    delivery.getCntrNo(),
                    innerItemCode,
                    InboundStatus.RUNNING,
                    Arrays.asList(InboundStatus.READY, InboundStatus.PAUSED),
                    true);
        }
    }

    /**
     * 신규 주문(TbMwXyzOrder) 엔티티를 'ORDER_READY' 상태로 생성합니다.
     *
     * @param delivery 상품 상세 주문정보
     * @return TbMwXyzOrder 신규 주문 엔티티
     */
    private TbMwXyzOrder buildNewOrder(TbMwInboundDelivery delivery) {

        if (ValueUtil.isEmpty(delivery)) {
            logger.error("[XYZ] Barcode : {}에 해당하는 InboundDelivery 정보가 존재하지 않습니다.", delivery.getItemCode());
            return null;
        } else if (delivery.isManualFlag()) {
            logger.info("[XYZ] Barcode : {}는 매뉴얼 이송 주문입니다. B/L NO : {}, CNTR NO : {}", delivery.getItemCode(), delivery.getBlNo(),  delivery.getCntrNo());
            return null;
        }

        int orderNo = getNextOrderNo();

        TbMwXyzOrder newOrder = new TbMwXyzOrder();
        newOrder.setOrderId(generateOrderId(orderNo));
        newOrder.setTaskNo(0);
        newOrder.setTargetNum(delivery.getItemQty() - delivery.getPassQty() - delivery.getNgQty());
        newOrder.setPassQty(0);
        newOrder.setNgQty(0);
        newOrder.setItemCode(delivery.getInnerItemCode());
        newOrder.setPriority(orderNo); // 선입선출
        newOrder.setAcceptDatetime(new Date());
        newOrder.setProcessStatus(ProcessStatus.ORDER_READY.value()); // 작업 대기
        newOrder.setDeliveryNo(delivery.getBlNo());
        newOrder.setCntrNo(delivery.getCntrNo());

        return newOrder;
    }


    /**
     * 신규 스캔된 품목의 주문을 생성하고 처리합니다.
     * 1컨테이너 1SKU 버전
     *
     * @param delivery 입고 상품 상세 주문
     * @param item 실제 스캔 상품 마스터
     */
    private void processNewOrderOneSku(TbMwInboundDelivery delivery, TbMwItemMaster item) {

        // 가용 가능한 슈트 전체 조회
        List<TbMwChute> chuteList = tbMwChuteManagementService.getAvailableChuteList();
        if (ValueUtil.isEmpty(chuteList)) {
            return;
        }

        // 신규 주문 목록 생성
        List<TbMwXyzOrder> newOrders = buildNewOrdersOneSku(
                delivery,
                chuteList.size(),
                Integer.parseInt(item.getPalletCapacity())
        );
        if (ValueUtil.isEmpty(newOrders)) {
            return;
        }

        boolean started = false;
        int assignCount = Math.min(newOrders.size(), chuteList.size());

        for (int i = 0; i < assignCount; i++) {
            TbMwXyzOrder order = newOrders.get(i);
            TbMwChute chute = chuteList.get(i);

            // targetNum 0 이하 오더는 실제 작업 의미가 없으므로 제외
            if (order.getTargetNum() <= 0) {
                continue;
            }

            // Order 정보 추가 (작업 시작 상태로 변경)
            order.setStartPointCd(chute.getStartPointCd());
            order.setEndPointCd(chute.getEndPointCd());
            order.setStartDatetime(new Date());
            order.setProcessStatus(ProcessStatus.ORDER_START.value()); // 작업 진행 중

            // Chute 할당
            tbMwChuteManagementService.assignOrder(
                    chute.getEndPointCd(),
                    order.getOrderId(),
                    order.getItemCode()
            );

            logger.info("[XYZ][1SKU] Barcode : {}에 대한 작업을 Chute {}에 시작합니다. Order ID : {}",
                    order.getItemCode(), chute.getEndPointCd(), order.getOrderId());

            // 주문 정보 등록
            logger.info("[XYZ][1SKU] 바코드 : {}에 대한 작업을 등록합니다. Order ID : {}",
                    order.getItemCode(), order.getOrderId());

            this.queryManager.insert(order);
            started = true;
        }

        // 실제 시작된 주문이 있는 경우에만 Delivery 상태 변경
        if (started) {
            tbMwInboundJobService.updateDeliveryStatus(
                    delivery.getBlNo(),
                    delivery.getCntrNo(),
                    delivery.getInnerItemCode(),
                    InboundStatus.RUNNING,
                    Arrays.asList(InboundStatus.READY, InboundStatus.PAUSED),
                    true
            );
        }
    }

    /**
     * 파렛트 만재수량 단위로 분할하여 신규 주문 목록을 생성합니다.
     *
     * 규칙
     * 1. targetNum 은 파렛트 만재수량 및 가용 적재포인트 단위로 할당
     * 2. 마지막 주문만 잔량 처리
     * 3. 생성된 모든 주문의 targetNum 합계는 딜리버리 대상 수량과 동일
     * 4. 필요한 주문 수가 빈 포인트 수를 초과하면 생성하지 않음
     *
     * @param delivery         상품 상세 주문정보
     * @param emptyPointCount  빈 파렛트 포인트 수
     * @param palletCapacity    상품 1파렛트 만재수량
     * @return 신규 주문 목록
     */
    private List<TbMwXyzOrder> buildNewOrdersOneSku(TbMwInboundDelivery delivery, int emptyPointCount, int palletCapacity) {

        // TODO : 검증 필요
        if (ValueUtil.isEmpty(delivery)) {
            logger.error("[XYZ][1SKU] Barcode : {}에 해당하는 InboundDelivery 정보가 존재하지 않습니다.", delivery.getItemCode());
            return null;
        } else if (delivery.isManualFlag()) {
            logger.info("[XYZ][1SKU] Barcode : {}는 매뉴얼 이송 주문입니다. B/L NO : {}, CNTR NO : {}", delivery.getItemCode(), delivery.getBlNo(),  delivery.getCntrNo());
            return null;
        }

        List<TbMwXyzOrder> orders = new ArrayList<TbMwXyzOrder>();

        // 기존 buildNewOrder 기준과 동일하게 '미처리 잔량' 기준
        int totalTargetQty = delivery.getItemQty() - delivery.getPassQty() - delivery.getNgQty();

        if (totalTargetQty <= 0) {
            logger.info("[XYZ][1SKU] 생성할 신규 오더 수량이 없습니다. itemCode : {}, totalTargetQty : {}",
                    delivery.getItemCode(), totalTargetQty);
            return orders;
        }

        int fullPalletCount = totalTargetQty / palletCapacity;   // 만재수량 단위 파렛트 수
        int remainQty = totalTargetQty % palletCapacity;         // 잔량

        int baseFullPalletPerPoint = fullPalletCount / emptyPointCount;
        int extraFullPalletCount = fullPalletCount % emptyPointCount;

        for (int i = 0; i < emptyPointCount; i++) {
            int fullPalletForThisPoint = baseFullPalletPerPoint;

            // 앞쪽 포인트부터 만재 단위 1개씩 추가 분배
            if (i < extraFullPalletCount) {
                fullPalletForThisPoint++;
            }

            int targetNum = fullPalletForThisPoint * palletCapacity;

            // 마지막 오더에 잔량 추가
            if (i == emptyPointCount - 1) {
                targetNum += remainQty;
            }

            int orderNo = getNextOrderNo();

            TbMwXyzOrder newOrder = new TbMwXyzOrder();
            newOrder.setOrderId(generateOrderId(orderNo));
            newOrder.setTaskNo(0);
            newOrder.setTargetNum(targetNum);
            newOrder.setPassQty(0);
            newOrder.setNgQty(0);
            newOrder.setItemCode(delivery.getInnerItemCode());
            newOrder.setPriority(orderNo); // 선입선출
            newOrder.setAcceptDatetime(new Date());
            newOrder.setProcessStatus(ProcessStatus.ORDER_READY.value());
            newOrder.setDeliveryNo(delivery.getBlNo());
            newOrder.setCntrNo(delivery.getCntrNo());

            orders.add(newOrder);
        }

        return orders;
    }

    /**
     * 가용한 슈트를 찾아 주문에 할당하고, 주문 상태를 '시작'으로 변경합니다.
     * (기존 주문/신규 주문 공통 로직)
     *
     * @param order 주문 엔티티 (상태 변경이 일어날 수 있음)
     * @return 슈트 할당 및 작업 시작 여부 (true/false)
     */
    private boolean tryStartOrderWithAvailableChute(TbMwXyzOrder order) {
        TbMwChute chute = tbMwChuteManagementService.getAvailableChute();

        if (ValueUtil.isEmpty(chute)) {
            logger.info("[XYZ] Barcode : {}에 대한 가용 슈트가 존재하지 않습니다.", order.getItemCode());
            return false; // 가용 슈트 없음
        }

        // Order 정보 추가 (작업 시작 상태로 변경)
        order.setStartPointCd(chute.getStartPointCd());
        order.setEndPointCd(chute.getEndPointCd());
        order.setStartDatetime(new Date());
        order.setProcessStatus(ProcessStatus.ORDER_START.value()); // 작업 진행 중

        // Chute 할당
        tbMwChuteManagementService.assignOrder(chute.getEndPointCd(), order.getOrderId(), order.getItemCode());

        logger.info("[XYZ] Barcode : {}에 대한 작업을 Chute {}에 시작합니다. Order ID : {}", order.getItemCode(), order.getEndPointCd(), order.getOrderId());
        return true; // 작업 시작됨
    }

    /**
     * Order 중단 시 Order 및 Chute 상태 변경
     *
     * @param param Order 정보 객체
     */
    public Map<String, Object> cancelOrder(TbMwXyzOrder param) {
        Map<String, Object> result = new HashMap<>();
        TbMwXyzOrder order = abortOrder(param);
        if (ValueUtil.isEmpty(order)) {
            result.put("code", 1);
            result.put("message", "존재하지 않거나 이미 완료된 작업입니다.");
            return result;
        }

        // InboundDelivery 완료 처리
        TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(order.getItemCode());
        tbMwInboundDeliveryService.completeInboundDelivery(order.getDeliveryNo(), order.getCntrNo(), itemMaster.getItemCode(), itemMaster.getInnerItemCode());

        result.put("code", 0);
        result.put("message", "작업이 성공적으로 중단되었습니다.");
        return result;
    }

    public TbMwXyzOrder abortOrder(TbMwXyzOrder param) {
        // 작업 정보 조회
        TbMwXyzOrder order = getOrderWithLock(param.getOrderId());

        // 이미 완료된 작업
        if (ValueUtil.isEmpty(order) || order.getProcessStatus() > ProcessStatus.ORDER_START.value()) {
            return null;
        }

        // 진행 중인 작업에 대해서 강제 종료
        logger.info("[XYZ] 작업자 요청에 의해 Order ID : {}, Barcode : {}에 대한 작업을 강제 종료합니다.", order.getOrderId(), order.getItemCode());
        if (order.getProcessStatus().equals(ProcessStatus.ORDER_START.value())) {
            // Chute 완료 처리
            tbMwChuteManagementService.updateOrderResult(order.getEndPointCd());

            // Pallet 배출 요청
            xyzPalletService.sendPalletExchange(order.getEndPointCd());
        }

        // 작업 상태 변경
        order.setCompleteDatetime(new Date());
        order.setProcessStatus(ProcessStatus.ORDER_ERROR.value());
        this.queryManager.update(order, "completeDatetime", "processStatus");

        return order;
    }

    /**
     * Order 완료 시 Order 및 Chute 상태 변경
     *
     * @param order 변경할 Order 정보 객체
     */
    public void completeOrder(TbMwXyzOrder order) {
        order.setCompleteDatetime(new Date());
        order.setProcessStatus(ProcessStatus.ORDER_COMPLETE.value());
        this.queryManager.update(order, "completeDatetime", "processStatus");

        // Chute 완료 처리
        tbMwChuteManagementService.updateOrderResult(order.getEndPointCd());

        // InboundDelivery 완료 처리
        TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(order.getItemCode());
        tbMwInboundDeliveryService.completeInboundDelivery(order.getDeliveryNo(), order.getCntrNo(), itemMaster.getItemCode(), itemMaster.getInnerItemCode());
    }

    /**
     * 분할 오더용 개별 완료 처리
     *
     * 기존 completeOrder() 와 차이점
     * - Chute 완료 처리까지는 동일
     * - Delivery 완료 처리는 하지 않음
     */
    private void completeOrderOnly(TbMwXyzOrder order) {
        order.setCompleteDatetime(new Date());
        order.setProcessStatus(ProcessStatus.ORDER_COMPLETE.value());
        this.queryManager.update(order, "completeDatetime", "processStatus");

        // Chute 완료 처리
        tbMwChuteManagementService.updateOrderResult(order.getEndPointCd());
    }

    /**
     * Order 진행 수량 변경
     *
     * @param order 변경할 Order 정보 객체
     * @param passQty XYZ에서 송신한 적재된 Box 수
     * @param ngQty 작업자가 입력한 Reject Box 수
     */
    /*public void updateResultQty(TbMwXyzOrder order, int passQty, int ngQty) {
        // 완료 수량 정보 반영
        logger.info("[XYZ] 기존 작업 수량 Order ID : {}, Plan : {}, Pass : {}, NG : {}", order.getOrderId(), order.getTargetNum(), order.getPassQty(), order.getNgQty());
        logger.info("[XYZ] 수신 작업 수량 Order ID : {}, Pass : {}, NG : {}", order.getOrderId(), passQty, ngQty);
        order.setPassQty(order.getPassQty() + passQty);
        order.setNgQty(order.getNgQty() + ngQty);
        this.queryManager.update(order, "passQty", "ngQty");

        // Delivery 정보 완료 수량 반영
        tbMwInboundDeliveryService.updateResultQty(order.getDeliveryNo(), order.getCntrNo(), order.getItemCode(), passQty, ngQty);

        // 해당 Order가 끝났는지 판단 여부 반환
        boolean isOrderFinished = order.getPassQty() + order.getNgQty() >= order.getTargetNum();
        if (isOrderFinished) {
            logger.info("[XYZ] 전체 작업 완료! Order ID : {}, Plan : {}, Pass : {}, NG : {}", order.getOrderId(), order.getTargetNum(), order.getPassQty(), order.getNgQty());
            // Order 완료 처리
            completeOrder(order);

            // Pallet 교체 요청
            xyzPalletService.sendPalletExchange(order.getEndPointCd());
        }
    }*/
    public void updateResultQty(TbMwXyzOrder order, int passQty, int ngQty) {
        // 완료 수량 정보 반영
        logger.info("[XYZ] 기존 작업 수량 Order ID : {}, Plan : {}, Pass : {}, NG : {}",
                order.getOrderId(), order.getTargetNum(), order.getPassQty(), order.getNgQty());
        logger.info("[XYZ] 수신 작업 수량 Order ID : {}, Pass : {}, NG : {}",
                order.getOrderId(), passQty, ngQty);

        order.setPassQty(order.getPassQty() + passQty);
        order.setNgQty(order.getNgQty() + ngQty);
        this.queryManager.update(order, "passQty", "ngQty");

        // Delivery 정보 완료 수량 반영
        tbMwInboundDeliveryService.updateResultQty(
                order.getDeliveryNo(),
                order.getCntrNo(),
                order.getItemCode(),
                passQty,
                ngQty
        );

        // 현재 Order 완료 여부 확인
        boolean isOrderFinished = order.getPassQty() + order.getNgQty() >= order.getTargetNum();
        if (!isOrderFinished) {
            return;
        }

        // 최소 수정 분기:
        // 실제 1SKU 컨테이너 + single sku 모드 + 같은 컨테이너 내 오더가 2건 이상일 때만 분할 완료 로직 수행
        String isSingleSku = SettingUtil.getValue("mw.order.single.sku", "false");
        logger.info("[XYZ][1SKU] order.getDeliveryNo[{}], order.getCntrNo()[{}]", order.getDeliveryNo(), order.getCntrNo());
        TbMwInboundJob job = tbMwInboundJobService.getInboundJobByBlCntr(order.getDeliveryNo(), order.getCntrNo());
        logger.info("[XYZ][1SKU] getInboundJobByBlCntr(), isNotNull [{}]", ValueUtil.isNotEmpty(job));
        List<TbMwXyzOrder> orderList = getOrderListByDeliveryInfo(order.getDeliveryNo(), order.getCntrNo());

        boolean isSplitSingleSku =
                ValueUtil.isNotEmpty(job)
                        && job.getSkuQty() == 1
                        && "true".equals(isSingleSku)
                        && ValueUtil.isNotEmpty(orderList)
                        && orderList.size() > 1;

        if (isSplitSingleSku) {
            logger.info("[XYZ][1SKU] 분할 오더 완료. Order ID : {}, Plan : {}, Pass : {}, NG : {}",
                    order.getOrderId(), order.getTargetNum(), order.getPassQty(), order.getNgQty());

            // 현재 오더만 완료 처리 (기존 completeOrder 사용 X)
            // 이유: completeOrder() 는 Delivery 완료까지 같이 태워서 첫 오더 완료 시 전체 완료로 오인될 수 있음
            completeOrderOnly(order);

            // 전체 분할 오더가 모두 끝났는지 확인
            List<TbMwXyzOrder> refreshedOrderList = getOrderListByDeliveryInfo(order.getDeliveryNo(), order.getCntrNo());
            if (isAllOrdersFinished(refreshedOrderList)) {
                logger.info("[XYZ][1SKU] 전체 작업 완료! DeliveryNo : {}, CntrNo : {}, ItemCode : {}",
                        order.getDeliveryNo(), order.getCntrNo(), order.getItemCode());

                TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(order.getItemCode());

                // Delivery 최종 완료 처리
                tbMwInboundDeliveryService.completeInboundDelivery(
                        order.getDeliveryNo(),
                        order.getCntrNo(),
                        itemMaster.getItemCode(),
                        itemMaster.getInnerItemCode()
                );

                // 최종 완료 시점에 남아있는 완료 오더 포인트들만 pallet exchange 요청
                sendPalletExchangeForFinishedOrders(refreshedOrderList, order);
            }

            return;
        } else{
            // 기존 로직 그대로 유지
            logger.info("[XYZ] 전체 작업 완료! Order ID : {}, Plan : {}, Pass : {}, NG : {}",
                    order.getOrderId(), order.getTargetNum(), order.getPassQty(), order.getNgQty());

            // Order 완료 처리
            completeOrder(order);

            // Pallet 교체 요청
            xyzPalletService.sendPalletExchange(order.getEndPointCd());
        }


    }

    /**
     * 최종 완료 시점에 완료된 오더들의 포인트만 pallet exchange 요청
     *
     * 예)
     * - 2개 오더 모두 완료 -> 2개 포인트 모두 요청
     * - 실제 완료 오더가 1개만 남음 -> 1개만 요청
     */
    private void sendPalletExchangeForFinishedOrders(List<TbMwXyzOrder> orderList, TbMwXyzOrder order) {
        if (ValueUtil.isEmpty(orderList)) {
            return;
        }

        for (TbMwXyzOrder targetOrder : orderList) {
            boolean isFinished = targetOrder.getPassQty() + targetOrder.getNgQty() >= targetOrder.getTargetNum();
            if (!isFinished) {
                continue;
            }

            if (ValueUtil.isEmpty(targetOrder.getEndPointCd())) {
                continue;
            }

            if(targetOrder.getOrderId().equals(order.getOrderId())){
                logger.info("[XYZ][1SKU] Last Box Comp & pallet Change order");
                xyzPalletService.sendPalletExchange(targetOrder.getEndPointCd());

            }
        }
    }

    /**
     * 같은 컨테이너의 모든 오더가 완료되었는지 확인
     */
    private boolean isAllOrdersFinished(List<TbMwXyzOrder> orderList) {
        if (ValueUtil.isEmpty(orderList)) {
            return false;
        }

        for (TbMwXyzOrder targetOrder : orderList) {
            if (targetOrder.getPassQty() + targetOrder.getNgQty() < targetOrder.getTargetNum()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 품번 기준으로 대기 혹은 진행 중인 Order가 존재하는지 조회
     *
     * @param itemCode 조회 기준 품번
     * @return 대기 혹은 진행 중인 Order 정보 객체
     */
    public TbMwXyzOrder getActiveAndPendingOrder(String itemCode) {
        String sql = "select * from tb_mw_xyz_order where process_status < :processStatus and item_code = :itemCode limit 1 for update";
        Map<String, Object> param = ValueUtil.newMap("itemCode,processStatus", itemCode, ProcessStatus.ORDER_COMPLETE.value());
        return this.queryManager.selectBySql(sql, param, TbMwXyzOrder.class);
    }
    /**
     * orderId 기준으로 Task 송신 이력 조회
     *
     * @param orderId 조회 기준 오더 아이디
     * @return Task 송신 이력 조회
     */
    public List<TbMwIfXyzOrder> getSendedOrderList(String orderId) {
        String sql = "select * from tb_mw_if_xyz_order where task_id like :orderId";
        Map<String, Object> param = ValueUtil.newMap(
                "orderId",
                orderId + "%"
        );
        return this.queryManager.selectListBySql(sql, param, TbMwIfXyzOrder.class, 0, 0);
    }

    /**
     * orderId 기준으로 이미 송신한 Box 총 수량 조회
     *
     * 주의:
     * - getSendedOrderList().size() 는 Task 건수이므로 2pick/3pick 을 반영하지 못함
     * - 1SKU 라우팅에서는 실제 보낸 박스 수량 합계를 사용해야 함
     */
    public int getSendedBoxQty(String orderId) {
        String sql = "select coalesce(sum(target_num), 0) " +
                "from tb_mw_if_xyz_order " +
                "where task_id like :orderId";

        Map<String, Object> param = ValueUtil.newMap(
                "orderId",
                orderId + "%"
        );

        Integer result = this.queryManager.selectBySql(sql, param, Integer.class);
        return result == null ? 0 : result;
    }

    /**
     * 품번 기준으로 대기 혹은 진행 중인 OrderList 조회
     *
     * @param itemCode 조회 기준 품번
     * @return 대기 혹은 진행 중인 Order 정보 객체
     */
    public List<TbMwXyzOrder> getActiveAndPendingOrderList(String itemCode) {
        String sql = "select * from tb_mw_xyz_order where process_status < :processStatus and item_code = :itemCode order by end_point_cd for update";
        Map<String, Object> param = ValueUtil.newMap("itemCode,processStatus", itemCode, ProcessStatus.ORDER_COMPLETE.value());
        return this.queryManager.selectListBySql(sql, param, TbMwXyzOrder.class, 0, 0);
    }

    /**
     * TaskNo 1 증가
     *
     * @param order 대상 Order 객체
     */
    public void incrementTaskNo(TbMwXyzOrder order) {
        order.setTaskNo(order.getTaskNo() + 1);
        this.queryManager.update(order, "taskNo");
    }

    /**
     * Order ID -> Order 정보 객체
     *
     * @param orderId 조회할 Order의 ID
     * @return 입력한 Order ID의 Order 정보 객체
     */
    public TbMwXyzOrder getOrderWithLock(String orderId) {
        String sql = "select * from tb_mw_xyz_order where order_id = :orderId for update";
        Map<String, Object> param = ValueUtil.newMap("orderId", orderId);
        return this.queryManager.selectBySql(sql, param, TbMwXyzOrder.class);
    }

    public TbMwXyzOrder getOrder(String orderId) {
        String sql = "select * from tb_mw_xyz_order where order_id = :orderId";
        Map<String, Object> param = ValueUtil.newMap("orderId", orderId);
        return this.queryManager.selectBySql(sql, param, TbMwXyzOrder.class);
    }

    /**
     * 다음 Order ID 생성에 필요한 Order No 생성
     *
     * @return 날짜별 고유한 Order No 생성
     */
    private Integer getNextOrderNo() {
        String sql = """
                INSERT INTO tb_mw_xyz_order_id_counter (biz_date, last_no)
                VALUES (CURRENT_DATE, 1)
                ON CONFLICT (biz_date) DO UPDATE SET last_no = tb_mw_xyz_order_id_counter.last_no + 1
                RETURNING last_no
                """;
        return this.queryManager.selectBySql(sql, null, Integer.class);
    }

    /**
     * MW Order ID 생성
     *
     * @param orderNo getNextOrderNo 메소드에서 생성한 Order No
     * @return yyyyMMdd-00000 형식의 Order ID
     */
    private String generateOrderId(int orderNo) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = today.format(dateFormatter);
        String orderNoPart = String.format("%05d", orderNo);
        return datePart + "-" + orderNoPart;
    }

    /**
     * 특정 컨테이너에 대한 전체 작업 정보 조회
     * @param deliveryNo InboundDelivery 주문 번호
     * @param cntrNo 컨테이너 번호
     *
     * @return 특정 컨테이너에 대한 전체 작업 정보
     */
    public List<TbMwXyzOrder> getOrderListByDeliveryInfo(String deliveryNo, String cntrNo) {
        String sql = "select * from tb_mw_xyz_order where delivery_no = :deliveryNo and cntr_no = :cntrNo";
        Map<String, Object> param = ValueUtil.newMap("deliveryNo,cntrNo", deliveryNo, cntrNo);
        return this.queryManager.selectListBySql(sql, param, TbMwXyzOrder.class, 0, 0);
    }

    /**
     * 시간단위로 조회하여 실시간 물동량 확인
     *
     * @return 시간단위로 조회하여 실시간 물동량 확인
     */
    public List<TbMwXyzOrder> getRealTimeUph(Date startDate, Date nextDate) {
        String sql = "SELECT * FROM samsung_mw.tb_mw_xyz_order " +
                "WHERE complete_datetime >= :startDate " +
                "  AND complete_datetime < :nextDate " + // 23:59:59 대신 < nextDate 사용이 더 안전함
                "  AND pass_qty > 0" +
                "  AND process_status = '33'" +
                "  OR process_status = '32'";
        Map<String, Object> param = ValueUtil.newMap("startDate,nextDate", startDate, nextDate);
        return this.queryManager.selectListBySql(sql, param, TbMwXyzOrder.class, 0, 0);
    }

    /**
     * tb_mw_xyz_order 목록 조회
     * - ng_qty: tb_mw_box에서 final_status가 0 인 box 수 + tb_mw_xyz_order의 ng_qty의 합
     */
    public List<TbMwXyzOrder> getRejectDeliveryInfo(String processStatus, String startPointCd, String endPointCd, String itemCode, String[] acceptDateTime) {

        Map<String, Object> params = ValueUtil.newMap("finalStatus", TrackingStatus.FINAL_VALID_NG.getValue());

        String query = """
                SELECT
                    xyz.*,
                    COALESCE(b.ng_qty, 0) AS ng_qty
                FROM tb_mw_xyz_order xyz
                    LEFT JOIN (
                        SELECT cntr_no, bl_no, item_code, COUNT(*) AS ng_qty
                        FROM tb_mw_box
                        WHERE final_status = :finalStatus
                        GROUP BY cntr_no, bl_no, item_code
                    ) b ON xyz.cntr_no = b.cntr_no
                            AND xyz.delivery_no = b.bl_no
                            AND xyz.item_code = b.item_code
                WHERE 1=1
                """;
        if (processStatus != null) {
            query += " AND xyz.process_status = :processStatus";
            params.put("processStatus", Integer.parseInt(processStatus));
        }
        if (startPointCd != null) {
            query += " AND xyz.start_point_cd LIKE CONCAT('%', :startPointCd, '%')";
            params.put("startPointCd", startPointCd.toUpperCase());
        }
        if (endPointCd != null) {
            query += " AND xyz.end_point_cd LIKE CONCAT('%', :endPointCd, '%')";
            params.put("endPointCd", endPointCd.toUpperCase());
        }
        if (itemCode != null) {
            query += " AND xyz.item_code LIKE CONCAT('%', :itemCode, '%')";
            params.put("itemCode", itemCode);
        }
        if (acceptDateTime != null) {
            if (acceptDateTime.length > 0 && acceptDateTime[0] != null && !acceptDateTime[0].isEmpty()) {
                query += " AND xyz.accept_datetime >= :fromAcceptDate";
                params.put("fromAcceptDate", LocalDate.parse(acceptDateTime[0].substring(0, 10)));
            }

            if (acceptDateTime.length > 1 && acceptDateTime[1] != null && !acceptDateTime[1].isEmpty()) {
                query += " AND xyz.accept_datetime < :toAcceptDate";
                params.put("toAcceptDate", LocalDate.parse(acceptDateTime[1].substring(0, 10)).plusDays(1));
            }
        }

        query += " ORDER BY xyz.process_status;";

        List<TbMwXyzOrder> result = this.queryManager.selectListBySql(query, params, TbMwXyzOrder.class, 0, 0);

        if (result == null || result.isEmpty()) {
            return new ArrayList<>();
        }
        return result;
    }
}