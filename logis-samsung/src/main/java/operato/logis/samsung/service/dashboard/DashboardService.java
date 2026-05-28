package operato.logis.samsung.service.dashboard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import operato.logis.samsung.consts.ProcessStatus;
import operato.logis.samsung.dto.dashboard.*;
import operato.logis.samsung.entity.mw.TbMwChute;
import operato.logis.samsung.entity.mw.TbMwInboundDelivery;
import operato.logis.samsung.entity.mw.TbMwItemMaster;
import operato.logis.samsung.entity.mw.TbMwXyzOrder;
import operato.logis.samsung.entity.wcs.TbMwUnitErrorLog;
import operato.logis.samsung.entity.wcs.TbMwUnitHeartbeat;
import operato.logis.samsung.service.mw.*;
import operato.logis.samsung.service.wcs.UnitErrorLogService;
import operato.logis.samsung.service.wcs.UnitHeartbeatService;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService extends AbstractQueryService {

    private final VolumeOfGoodsService volumeOfGoodsService;
    private final TbMwInboundDeliveryService tbMwInboundDeliveryService;
    private final TbMwXyzOrderService tbMwXyzOrderService;
    private final TbMwChuteManagementService tbMwChuteManagementService;
    private final TbMwItemMasterService tbMwItemMasterService;
    private final UnitHeartbeatService unitHeartbeatService;
    private final UnitErrorLogService unitErrorLogService;
    private final BoxTrackingService boxTrackingService;

    @Getter
    private static DashboardMain dashboardMain;

    private static String deliveryNo;

    private static String cntrNo;

    // @Scheduled(initialDelay = 0, fixedDelay = 5000)
    public void setDashboardMain(Date chooseDate) {
        if (ValueUtil.isEmpty(dashboardMain)) {
            dashboardMain = new DashboardMain();
        }
        setInboundDelivery(chooseDate);
        setPalletizing(chooseDate);
        setChute();
        setEquipmentStatus();
        setErrorLog(chooseDate);
        setRealTimeUPH(chooseDate);
    }

    public DashboardDate getDashboardDate(Date chooseDate, int inputData) {
        DashboardDate dashboardDate = new DashboardDate();
        dashboardDate.setDashboardVolumeOfGoods(setVolumeOfGoods(chooseDate, inputData));

        return dashboardDate;
    }

    private void setInboundDelivery(Date chooseDate) {

        // 1. 선택 날짜 기준 전체 입고 데이터 조회
        List<TbMwInboundDelivery> inboundDeliveryList =
                tbMwInboundDeliveryService.getInboundDeliveryByDate(chooseDate);

        if (ValueUtil.isEmpty(inboundDeliveryList)) {
            // 날짜 기준 컨테이너가 하나도 없으면 null 또는 빈 리스트로 내려보내기
            dashboardMain.setDashboardInboundDeliveryList(null);
            return;
        }

        // 2. BL No + CNTR No 기준으로 컨테이너 그룹핑
        //    (LinkedHashMap 으로 해서 DB 순서 그대로 유지)
        Map<String, List<TbMwInboundDelivery>> groupedByBlAndCntr =
                inboundDeliveryList.stream()
                        .collect(Collectors.groupingBy(
                                d -> d.getBlNo() + "|" + d.getCntrNo(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<DashboardInboundDelivery> containerList = new ArrayList<>();

        // 3. 컨테이너(그룹)별로 DashboardInboundDelivery 생성
        for (Map.Entry<String, List<TbMwInboundDelivery>> entry : groupedByBlAndCntr.entrySet()) {

            List<TbMwInboundDelivery> containerRows = entry.getValue();
            if (ValueUtil.isEmpty(containerRows)) {
                continue;
            }

            // 같은 컨테이너이므로 아무 행이나 하나 기준값으로 사용
            TbMwInboundDelivery first = containerRows.get(0);
            String deliveryNo = first.getBlNo();
            String cntrNo     = first.getCntrNo();

            // 3-1. 수동 / 자동 SKU 분리
            Map<Boolean, List<String>> partitionedItemCodes = containerRows.stream()
                    .collect(Collectors.partitioningBy(
                            TbMwInboundDelivery::isManualFlag,
                            Collectors.mapping(TbMwInboundDelivery::getItemCode, Collectors.toList())
                    ));

            List<String> manualItemCodeList =
                    partitionedItemCodes.getOrDefault(true, Collections.emptyList());
            List<String> automationItemCodeList =
                    partitionedItemCodes.getOrDefault(false, Collections.emptyList());

            // 3-2. SKU Summary 조회 (status 계산용 raw 데이터 포함)
            List<DashboardInboundDelivery.SkuSummary> skuSummaryList =
                    tbMwInboundDeliveryService.getSkuSummaryList(deliveryNo, cntrNo);

            // 3-3. 상태 문자열 자동 계산
            for (DashboardInboundDelivery.SkuSummary sku : skuSummaryList) {
                sku.refreshStatus();
            }

            // 3-4. DTO 조립
            DashboardInboundDelivery dashboardInboundDelivery = new DashboardInboundDelivery();
            dashboardInboundDelivery.setDeliveryNo(deliveryNo);
            dashboardInboundDelivery.setCntrNo(cntrNo);
            dashboardInboundDelivery.setAutomationItemCode(automationItemCodeList);
            dashboardInboundDelivery.setManualItemCode(manualItemCodeList);
            dashboardInboundDelivery.setSkuSummary(skuSummaryList);

            containerList.add(dashboardInboundDelivery);
        }

        // 4. 오늘 날짜 기준 컨테이너 전체 리스트로 세팅
        dashboardMain.setDashboardInboundDeliveryList(containerList);
    }

    private void setPalletizing(Date chooseDate) {
        // 선택 날짜 기준 전체 입고 데이터 조회
        List<TbMwInboundDelivery> inboundDeliveryList =
                tbMwInboundDeliveryService.getInboundDeliveryByDate(chooseDate);

        if (ValueUtil.isEmpty(inboundDeliveryList)) {
            // 날짜 기준 컨테이너가 하나도 없으면 null 또는 빈 리스트로 내려보내기
            dashboardMain.setDashboardInboundDeliveryList(null);
            return;
        }
        // ==========================
        // 컨테이너( B/L + CNTR ) 기준으로 그룹핑
        //    key 예시: "HASLS2225...|HLHU8328775"
        // ==========================
        Map<String, List<TbMwInboundDelivery>> byContainer =
                inboundDeliveryList.stream()
                        .collect(Collectors.groupingBy(
                                d -> d.getBlNo() + "|" + d.getCntrNo()
                        ));

        // 입고 예정 컨테이너 수량 = 해당 날짜 컨테이너 distinct 개수
        int inboundPlanCntrQty = byContainer.size();

        // 입고 완료 컨테이너 수량
        //     - 컨테이너에 속한 모든 row 의 inbound_status 가 3 이면 "완료 컨테이너"로 간주
        long completedCntrQtyLong = byContainer.values().stream()
                .filter(list -> list.stream()
                        .allMatch(d -> d.getInboundStatus() != null &&
                                d.getInboundStatus() == 3))
                .count();
        int completedCntrQty = (int) completedCntrQtyLong;

        // 입고 예정 총 박스 수량 = item_qty 전체 합
        int inboundPlanBoxQty = inboundDeliveryList.stream()
                .mapToInt(d -> d.getItemQty() == null ? 0 : d.getItemQty())
                .sum();

        // 메뉴얼 입고 파렛타이징 완료 박스 수량
        //     - manual_flag = true && inbound_status = 3 인 row 의 item_qty 합
        int manualCompletedQty = inboundDeliveryList.stream()
                .filter(d -> Boolean.TRUE.equals(d.isManualFlag()))
                .filter(d -> d.getInboundStatus() != null &&
                        d.getInboundStatus() == 3)
                .mapToInt(d -> d.getItemQty() == null ? 0 : d.getItemQty())
                .sum();

        // (5) 설비 입고 / NG 수량은 컨테이너별로 XYZ_ORDER 조회하면서 누적
        int autoCompletedQty = 0;
        int ngBoxQty         = 0;

        for (String key : byContainer.keySet()) {
            String[] parts = key.split("\\|", 2);
            String deliveryNo = parts[0];
            String cntrNo     = parts[1];

            // 컨테이너 1개 기준 XYZ ORDER 조회
            List<TbMwXyzOrder> orderList =
                    tbMwXyzOrderService.getOrderListByDeliveryInfo(deliveryNo, cntrNo);

            if (ValueUtil.isEmpty(orderList)) {
                continue;
            }

            for (TbMwXyzOrder order : orderList) {
                Integer procStatus = order.getProcessStatus();
                int pass = order.getPassQty() == null ? 0 : order.getPassQty();
                int ng   = order.getNgQty()   == null ? 0 : order.getNgQty();

                // procStatus가 작업중(예: PROCESSING) 이거나 완료(ORDER_COMPLETE)일 때 누적
                if (procStatus != null &&
                        (procStatus.equals(ProcessStatus.ORDER_START.value()) ||
                                procStatus.equals(ProcessStatus.ORDER_COMPLETE.value()))) {

                    autoCompletedQty += pass;
                }

                ngBoxQty += ng;

            }
        }

        // ==========================
        // 3) DashboardInboundStatus 세팅
        // ==========================
        DashboardInboundStatus status = new DashboardInboundStatus();
        status.setInboundPlanCntrQty(inboundPlanCntrQty);
        status.setCompletedCntrQty(completedCntrQty);
        status.setInboundPlanBoxQty(inboundPlanBoxQty);
        status.setAutoCompletedQty(autoCompletedQty);
        status.setManualCompletedQty(manualCompletedQty);
        status.setNgBoxQty(ngBoxQty);

        dashboardMain.setDashboardInboundStatus(status);
    }

    private void setChute() {
        // 1. 전체 Chute 정보 조회
        List<TbMwChute> chuteList = tbMwChuteManagementService.getAllChutes();
        List<DashboardChute> dashboardChuteList = new ArrayList<>();

        // 2. 전체 Chute에 대해 DashboardChute 생성
        for (TbMwChute chute : chuteList) {
            DashboardChute dashboardChute = DashboardChute.fromTbMwChute(chute);
            if (ValueUtil.isEmpty(chute.getOrderId())) {
                dashboardChuteList.add(dashboardChute);
                continue;
            }

            // 2-1. 진행 작업 정보 할당
            TbMwXyzOrder order = tbMwXyzOrderService.getOrder(chute.getOrderId());
            if (ValueUtil.isNotEmpty(order)) {
                dashboardChute.setExpectedQuantity(order.getTargetNum());
                dashboardChute.setCompletedQuantity(order.getPassQty());
                dashboardChute.setNgQuantity(order.getNgQty());
            }

            // 2-2. 진행 상품 정보 할당
            TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(chute.getItemCode());
            if (ValueUtil.isNotEmpty(itemMaster)) {
                dashboardChute.setItemCode(itemMaster.getItemCode());
                dashboardChute.setItemName(itemMaster.getItemName());
                dashboardChute.setPalletCapacity(itemMaster.getPalletCapacity());
                dashboardChute.setItemWidth((itemMaster.getItemWidth()));
                dashboardChute.setItemHeight(itemMaster.getItemHeight());
                dashboardChute.setItemLength(itemMaster.getItemLength());
            }

            dashboardChuteList.add(dashboardChute);
        }

        // 3. 대시보드 메인 객체에 설정
        dashboardMain.setDashboardChute(dashboardChuteList);
    }

    private void setRealTimeUPH(Date chooseDate) {
        // 1. 날짜 범위 구하기 (Java 8 LocalDate 사용)
        LocalDate localDate = chooseDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // 시작일: 2026-01-15 00:00:00
        Date startDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        // 종료일: 2026-01-16 00:00:00 (다음날 0시)
        Date nextDate = Date.from(localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<TbMwXyzOrder> orderList = tbMwXyzOrderService.getRealTimeUph(startDate, nextDate);

        // 3. 데이터 집계 (시간대별 누적)
        Map<String, Integer> aggregatedMap = new TreeMap<>();
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
        Calendar cal = Calendar.getInstance();

        if (ValueUtil.isNotEmpty(orderList)) {
            for (TbMwXyzOrder order : orderList) {
                Date workTime = order.getUpdatedAt();
                if (workTime == null) continue;

                // 30분 단위 버킷팅 (00분 or 30분)
                cal.setTime(workTime);
                int minute = cal.get(Calendar.MINUTE);
                cal.set(Calendar.MINUTE, (minute < 30) ? 0 : 30);

                String timeKey = sdfTime.format(cal.getTime());

                // 수량 누적
                int passQty = order.getPassQty() == null ? 0 : order.getPassQty();
                aggregatedMap.put(timeKey, aggregatedMap.getOrDefault(timeKey, 0) + passQty);
            }
        }

        // 4. 결과 리스트 생성 (차트용 데이터 변환 및 빈 시간 채우기)
        List<DashboardUPH> uphList = new ArrayList<>();

        if (aggregatedMap.isEmpty()) {
            dashboardMain.setDashboardUPHSList(uphList);
            return;
        }

        try {
            // 가장 빠른 시간부터 23:30까지 빈 구간 채우기
            String startTimeStr = ((TreeMap<String, Integer>) aggregatedMap).firstKey();
            Date chartTime = sdfTime.parse(startTimeStr);
            cal.setTime(chartTime);

            while (true) {
                String currentTimeKey = sdfTime.format(cal.getTime());

                int qty = aggregatedMap.getOrDefault(currentTimeKey, 0);
                uphList.add(new DashboardUPH(currentTimeKey, qty));

                if ("23:30".equals(currentTimeKey)) break;

                cal.add(Calendar.MINUTE, 30);
                if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0) break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        dashboardMain.setDashboardUPHSList(uphList);
    }

    private void setEquipmentStatus() {
        // 1. 전체 Unit 상태 조회
        List<TbMwUnitHeartbeat> unitList = unitHeartbeatService.getAllUnitStatus();

        // 2. Dashboard 데이터로 변환
        List<DashboardEquipmentStatus> dashboardEquipmentStatusList = new ArrayList<>();
        for (TbMwUnitHeartbeat unit : unitList) {
            DashboardEquipmentStatus dashboardEquipmentStatus = DashboardEquipmentStatus.fromEquipmentStatus(unit);
            dashboardEquipmentStatusList.add(dashboardEquipmentStatus);
        }

        // 3. 대시보드 메인 객체에 설정
        dashboardMain.setDashboardEquipmentStatus(dashboardEquipmentStatusList);
    }

    private void setBoxTestStatus(Date chooseDate) {
        List<DashboardInboundTracking> boxTrackingList = boxTrackingService.getAllBoxTracking(chooseDate);

        // 3. 대시보드 메인 객체에 설정
        dashboardMain.setDashboardInboundTrackingList(boxTrackingList);
    }

    private void setErrorLog(Date chooseDate) {
        List<TbMwUnitErrorLog> errorLogList = unitErrorLogService.getErrorLogByDate(chooseDate);

        dashboardMain.setDashboardErrorLogList(errorLogList);
    }

    private List<DashboardVolumeOfGoods> setVolumeOfGoods(Date chooseDate, int inputData) {
        return switch (inputData) {
            case 1 -> volumeOfGoodsService.getVolumeOfGoodsGroupByDay(chooseDate);
            case 2 -> volumeOfGoodsService.getVolumeOfGoodsGroupByWeek(chooseDate);
            case 3 -> volumeOfGoodsService.getVolumeOfGoodsGroupByMonth(chooseDate);
            case 4 -> volumeOfGoodsService.getVolumeOfGoodsGroupByHour(chooseDate);
            default -> null;
        };
    }

    private boolean isSameDay(Date d1, Date d2) {
        if (d1 == null || d2 == null) return false;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(d1).equals(fmt.format(d2));
    }
}