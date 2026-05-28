package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.common.util.OrderKeyGenerator;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.CommandType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.EquipType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.ProcessStatus;
import operato.logis.kmat_2026.biz.ecs.sineva.util.EcsCsvUtil;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TbWcsOrderService extends AbstractQueryService {
    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    @Autowired
    protected OrderKeyGenerator orderKeyGenerator;

    // TbWcsOrderService 내부에 추가
    private static final Pattern TASK_A = Pattern.compile("^\\d{8}T\\d{9}$");                 // pod_no 형식
    private static final Pattern TASK_B = Pattern.compile("^\\d{8}-[0-9A-Fa-f]{4}-\\d{6}$");  // order_id 형식


    // 공통 로딩 상태 목록 (POD/PALLET 로딩 상태)
    public static final List<Integer> POD_OR_PALLET_LOADING_STATUSES =
            Collections.unmodifiableList(Arrays.asList(
                    ProcessStatus.NEXT_TASK_READY.getCode(),
                    ProcessStatus.NEXT_RELEASE_READY.getCode(),
                    ProcessStatus.MPS_ZONE_RELEASE_READY.getCode(),
                    ProcessStatus.AWAITING_FINAL_RELEASE.getCode(),
                    ProcessStatus.FINISH_FROM_SIDE_LOADING.getCode()
            ));

    /**
     * fromSide 첫번째값 반환
     */
    public String getFirstToSide(TbWcsOrder order) {
        List<String> toSideList = EcsCsvUtil.csvToStringList(order.getToSide());
        if (toSideList.isEmpty()) {
            throw new ElidomRuntimeException("toSide must contain at least one destination.");
        }
        return toSideList.get(0);
    }

    /**
     * 가장 최근 taskId 하나 가져온다.
     */
    public TbWcsOrder findOrder(String taskId) {
        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("taskId", taskId);
        condition.addOrder("accept_datetime", false); // createdAt 기준 내림차순 정렬 (최신순)
        condition.addFilter("completed",false);
        condition.setMaxResultSize(1);

        TbWcsOrder tbWcsOrder = this.queryManager.selectByCondition(TbWcsOrder.class,condition);

        return tbWcsOrder;
    }

    /**
     * 가장 최근 taskId 하나 가져온다. commandType 기반
     */
    public TbWcsOrder findOrderByLikeCommandTypeAndTaskId(String taskId, String likeCommandType) {
        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("taskId", taskId);
        condition.addOrder("accept_datetime", false); // createdAt 기준 내림차순 정렬 (최신순)
        condition.addFilter("completed",false);
        condition.addFilter("commandType",OrmConstants.LIKE,likeCommandType);
        condition.addFilter("processStatus",OrmConstants.LESS_THAN_EQUAL,ProcessStatus.COMPLETE.getCode());
        condition.setMaxResultSize(1);

        TbWcsOrder tbWcsOrder = this.queryManager.selectByCondition(TbWcsOrder.class,condition);

        return tbWcsOrder;
    }

    public TbWcsOrder selectMpsZonePendingOrder(String taskId) {
        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("taskId", taskId);
        condition.addFilter("completed", false);
        condition.addOrder("accept_datetime", false); // createdAt 기준 내림차순 정렬 (최신순)
        condition.setMaxResultSize(1);

        TbWcsOrder tbWcsOrder = this.queryManager.selectByCondition(TbWcsOrder.class,condition);

        if(ValueUtil.isEmpty(tbWcsOrder)){
            throw new ElidomRuntimeException(taskId + "가 존재하지 않습니다.");
        }

        return tbWcsOrder;
    }

    /**
     * orderId로 wcs order 조회
     */
    public TbWcsOrder findOrderByOrderId(String orderId) {
        if(ValueUtil.isEmpty(orderId)){
            return null;
        }

        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("orderId", orderId);
        condition.setMaxResultSize(1);

        TbWcsOrder tbWcsOrder = this.queryManager.selectByCondition(TbWcsOrder.class,condition);

        return tbWcsOrder;
    }

    /**
     * 특정 설비(equipCd)의 가장 최근 미완료 오더 1건 조회
     */
    public TbWcsOrder findLatestOrderByEquipCd(String equipCd, ProcessStatus status, List<String> commandTypeList) {
        Query condition = OrmUtil.newConditionForExecution(7L);
        if(ValueUtil.isNotEmpty(equipCd)){
            condition.addFilter("equipId", equipCd);// 설비코드 기준 필터
        }
        condition.addFilter("processStatus", status.getCode()); // 진행상태
        condition.addFilter("command_type",OrmConstants.IN,commandTypeList);
        condition.addOrder("accept_datetime", false); // 최신순 정렬
        condition.setMaxResultSize(1);                             // 1건만

        return this.queryManager.selectByCondition(TbWcsOrder.class, condition);
    }

    /**
     * 특정 설비(equipCd)의 가장 최근 미완료 오더 1건 조회
     */
    public TbWcsOrder findOrderByPodCd(String podCd, List<ProcessStatus> processStatusList) {
        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("podCd", podCd);

        // processStatus IN (...)
        List<Integer> statusValues = processStatusList.stream()
                .map(ProcessStatus::getCode)
                .collect(Collectors.toList());
        condition.addFilter("processStatus", OrmConstants.IN, statusValues);

        condition.addOrder("accept_datetime", false); // 최신순 정렬
        condition.setMaxResultSize(1); // 1건만

        return this.queryManager.selectByCondition(TbWcsOrder.class, condition);
    }

    public TbWcsOrder selectTbWcsOrderByTaskId(Long domainId, String orderId) {
        Query condition = OrmUtil.newConditionForExecution(domainId);
        condition.addFilter("orderId", orderId);
        return this.queryManager.selectByCondition(TbWcsOrder.class, condition);
    }

    /**
     * robot task 시작 시 update
     */
    @Transactional
    public void startRobotTask(TbWcsOrder order){
        order.setProcessStatus(ProcessStatus.STARTING.getCode());
        order.setAcceptDatetime(new Date());
        this.queryManager.update(order,"processStatus","acceptDatetime");
    }

    /**
     * WCS ORDER 상태 갱신
     */
    @Transactional
    public void updateProcessStatus(TbWcsOrder order, ProcessStatus processStatus){
        order.setProcessStatus(processStatus.getCode());
        this.queryManager.update(order,"processStatus");
    }

    /**
     * WCS ORDER 완료 처리
     */
    @Transactional
    public void completeOrder(TbWcsOrder order) {
        order.setProcessStatus(ProcessStatus.COMPLETE.getCode());
        order.setFromPositionCod(order.getToPositionCod());
        order.setToPositionCod(null);
        order.setCompDatetime(new Date());
        order.setCompleted(true);

        logger.info("[오더 상태 완료 처리] processStatus: {}, fromPositionCod: {}, toPositionCod: null, compDatetime: {}, completed: true",
                order.getProcessStatus(), order.getFromPositionCod(), order.getCompDatetime());

        this.queryManager.update(order, "processStatus", "fromPositionCod", "toPositionCod", "compDatetime", "completed");
    }

    /**
     * 오늘 날짜 기준 최대 order_id 숫자 조회
     * 예: order_id = "20250629000042" → 반환값 = 42
     */
    @Transactional(readOnly = true)
    public int getTodayMaxOrderSequence() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String sql = "SELECT MAX(split_part(order_id, '-', 3)::INTEGER) " +
                "FROM tb_wcs_order " +
                "WHERE domain_id = :domainId " +
                "AND split_part(order_id, '-', 1) = :today";

        Map<String, Object> params = ValueUtil.newMap("domainId,today", 7L, today);
        Long count = this.queryManager.selectBySql(sql, params, Long.class);

        return (count == null) ? 0 : count.intValue();
    }

    /**
     * OrderId 생성 로직
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createOrderId() {
        return orderKeyGenerator.generate("SINEVA_ORDER_KEY");
    }

    /**
     * 입력된 숫자를 기반으로 OrderId 생성
     * 예: 20250629 + 000001 → 20250629000001
     */
    public String createOrderIdBySequence(int sequence) {

        // 오늘 날짜를 YYYYMMDD 형식으로 변환
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // UUID에서 앞 4자리 추출 (중복 방지용)
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();

        // 고정된 6자리 숫자 형식으로 변환
        String formattedSeq = String.format("%06d", sequence); // 6자리 숫자 포맷
        return today + "-" + uuidPart + "-" + formattedSeq;
    }

    /**
     * AMR/AGF Task 중복 검사
     * fromSide, toSide, status=20 인 작업이 존재하면 true
     * 없으면 false
     */
    public boolean isDuplicateTask(String fromSide, String toSide) {
        String sql = "select count(*) from tb_wcs_order where from_side = :fromSide and to_side = :toSide and domain_id = :domainId and completed = false";

        Integer executeTaskCount = this.queryManager.selectBySql(
                sql,
                ValueUtil.newMap("fromSide,toSide,domainId", fromSide, toSide, 7L),
                Integer.class
        );

        return executeTaskCount != 0;
    }

    /**
     * AMR/AGF Task 중복 검사
     * fromSide, toSide, status=20 인 작업이 존재하면 true
     * 없으면 false
     */
    public boolean isDuplicateTask(CommandType commandType) {
        String sql = "select count(*) from tb_wcs_order where command_type = :commandType and domain_id = :domainId and completed = false";

        Integer executeTaskCount = this.queryManager.selectBySql(
                sql,
                ValueUtil.newMap("commandType,domainId", commandType.getCode(), 7L),
                Integer.class
        );

        return executeTaskCount != 0;
    }

    /**
     * 작업이 진행중인지 확인
     */
    public boolean isTaskInProgress(TbWcsOrder order) {

        if(ValueUtil.isEmpty(order)){
            return false;
        }

        if(ValueUtil.isEmpty(order.getProcessStatus())){
            return false;
        }

        Integer taskProcessStatus = order.getProcessStatus();

        return taskProcessStatus >= ProcessStatus.STARTING.getCode() && taskProcessStatus < ProcessStatus.COMPLETE.getCode();
    }

    public TbWcsOrder createOrder(String taskId, String fromSide, String toSide, String podCd, CommandType cmd, EquipType equipType, String equipId, Integer priority) {
        TbWcsOrder order = new TbWcsOrder();
        order.setTaskId(taskId);
        order.setEquipType(equipType.getCode());
        order.setFromSide(fromSide);
        order.setToSide(toSide);
        order.setPodCd(podCd);
        order.setCommandType(cmd.getCode());
        order.setRemark(cmd.getDesc());
        order.setEquipId(equipId);
        order.setPriority(priority);
        return order;
    }

    /**
     * equipId에 대기중인 task가 있는지 조회.
     */
    public boolean existsAmrTaskPending(String equipId){
        String sql = "SELECT count(*) FROM tb_wcs_order WHERE equip_id = :equipId AND process_status = :pendingProcessStatus and domain_id = :domainId";

        Integer readyTaskByEquipId = this.queryManager.selectBySql(sql, ValueUtil.newMap("equipId,pendingProcessStatus,domainId", equipId, ProcessStatus.READY.getCode(), 8L), Integer.class);

        return readyTaskByEquipId > 0;
    }

    /**
     * equipId에 대기중인 task가 있는지 조회.
     */
    public boolean existsCommandTaskPending(List<CommandType> commandTypeList){
        List<String> commandTypeNames = commandTypeList.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        String sql = "SELECT count(*) FROM tb_wcs_order WHERE process_status = :pendingProcessStatus and domain_id = :domainId and command_type in (:commandList)";
        Integer readyTaskByCommandType = this.queryManager.selectBySql(sql, ValueUtil.newMap("commandList,pendingProcessStatus,domainId", commandTypeNames, ProcessStatus.READY.getCode(), 8L), Integer.class);

        return readyTaskByCommandType > 0;
    }

    /**
     * ProcessStatus에 해당하는 작업 조회
     */
    public List<TbWcsOrder> findOrderByProcessStatus(ProcessStatus processStatus) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("processStatus", processStatus.getCode());

        return this.queryManager.selectList(TbWcsOrder.class, condition);
    }

    /**
     * 시스템 설정에서 AMR 활성화 여부를 반환한다.
     * @return true: AMR 사용 가능, false: AMR 비활성화
     */
    @Transactional
    public void cancelOrder(TbWcsOrder order) {
        if(ValueUtil.isEmpty(order)){
            return;
        }

        order.setProcessStatus(ProcessStatus.CANCEL.getCode());
        order.setCompleted(true);
        order.setRemark("취소");
        this.queryManager.update(order, "processStatus", "remark","completed");
    }

    @Transactional
    public void markOrderAsError(TbWcsOrder order, String errorCode) {
        markOrderAsError(order, errorCode, "");
    }

    @Transactional
    public void markOrderAsError(TbWcsOrder order, String errorCode, String errorMessage) {
        if (ValueUtil.isEmpty(order)) {
            throw new ElidomRuntimeException("Order is null. Cannot update error status.");
        }

        order.setErrorCode(ValueUtil.isNotEmpty(errorCode) ? errorCode : "UNKNOWN_ERROR");
        order.setErrorMessage(ValueUtil.isNotEmpty(errorMessage) ? errorMessage : errorCode);        order.setErrorDatetime(new Date());
        order.setErrorDatetime(new Date());

        // processStatus는 변경하지 않음 → 재개 가능성 고려
        this.queryManager.update(order, "errorCode", "errorMessage", "errorDatetime");

        logger.info("[오더 에러 상태 설정] orderId={}, errorCode={}, errorMessage={}",
                order.getOrderId(), order.getErrorCode(), order.getErrorMessage());
    }

    /**
     * 오더 에러 해제 (정상화 시)
     */
    @Transactional
    public void clearOrderError(TbWcsOrder order) {
        if (ValueUtil.isEmpty(order)) {
            throw new ElidomRuntimeException("Order is null. Cannot clear error status.");
        }

        order.setErrorCode(null);
        order.setErrorMessage(null);
        order.setErrorDatetime(null);

        // processStatus 그대로 유지
        this.queryManager.update(order, "errorCode", "errorMessage", "errorDatetime");

        logger.info("[오더 에러 해제] orderId={} → 정상화 완료", order.getOrderId());
    }

    /**
     * 오늘 날짜 모든 에러 해제.
     */
    @Transactional
    public List<TbWcsOrder> getRecoverTargetOrders() {

        // 오늘 날짜의 시작과 끝 구하기
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // Date로 변환
        Date startDate = Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());

        // batchSeqList 내 carrier 투입 완료된 상품 외 토탈 피킹 전부 조회
        Query condition = OrmUtil.newConditionForExecution(7L);

        // acceptDatetime 조건 추가 (BETWEEN)
        condition.addFilter("acceptDatetime", OrmConstants.GREATER_THAN_EQUAL, startDate);
        condition.addFilter("acceptDatetime", OrmConstants.LESS_THAN_EQUAL, endDate);
        condition.addFilter("processStatus", OrmConstants.LESS_THAN, ProcessStatus.COMPLETE.getCode());

        List<TbWcsOrder> orderList = this.queryManager.selectList(TbWcsOrder.class, condition);

        return orderList;
    }

    // 타입 enum (권장)
    public enum TaskIdTypeSimple {
        POD_NO,     // Type A
        ORDER_ID,   // Type B
        UNKNOWN
    }

    // --- 메서드: enum 반환(권장) ---
    public TaskIdTypeSimple resolveTaskIdType(String taskId) {
        if (taskId == null) return TaskIdTypeSimple.UNKNOWN;
        String s = taskId.trim();
        if (TASK_A.matcher(s).matches()) return TaskIdTypeSimple.POD_NO;
        if (TASK_B.matcher(s).matches()) return TaskIdTypeSimple.ORDER_ID;
        return TaskIdTypeSimple.UNKNOWN;
    }

    // --- 메서드: String 반환(원하면 이걸로) ---
    public String resolveTaskIdTypeString(String taskId) {
        TaskIdTypeSimple t = resolveTaskIdType(taskId);
        switch (t) {
            case POD_NO:   return "POD_NO";
            case ORDER_ID: return "ORDER_ID";
            default:       return "UNKNOWN";
        }
    }

    /**
     * CommandType과 ProcessStatus로 오더 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbWcsOrder> findByCommandTypeAndProcessStatus(String commandType, ProcessStatus processStatus) {
        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("commandType", commandType);
        condition.addFilter("processStatus", processStatus.getCode());
        condition.addFilter("completed", false);
        condition.addOrder("created_at", false);

        return this.queryManager.selectList(TbWcsOrder.class, condition);
    }

    @Transactional(readOnly = true)
    public List<TbWcsOrder> findTodayRunningPodOrders() {
        return findRunningPodOrdersByAcceptDate(LocalDate.now());
    }

    /**
     * 특정 날짜에 accept 된 작업 중,
     * 아직 로딩 상태이며 POD 를 물고 있는 오더 목록 조회.
     *
     * 예) baseDate = 오늘 → "오늘 accept된 실행 중 POD" 만 조회
     */
    @Transactional(readOnly = true)
    public List<TbWcsOrder> findRunningPodOrdersByAcceptDate(LocalDate baseDate) {
        if (baseDate == null) {
            baseDate = LocalDate.now();
        }

        LocalDateTime startOfDay = baseDate.atStartOfDay();
        LocalDateTime endOfDay = baseDate.atTime(LocalTime.MAX);

        // Date로 변환
        Date startDate = Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());

        Query condition = OrmUtil.newConditionForExecution(7L);

        // 해당 날짜에 accept 된 작업만
        condition.addFilter("acceptDatetime", OrmConstants.GREATER_THAN_EQUAL, startDate);
        condition.addFilter("acceptDatetime", OrmConstants.LESS_THAN_EQUAL, endDate);
        // 로딩 상태 (공통 상수)
        condition.addFilter("processStatus", OrmConstants.IN, POD_OR_PALLET_LOADING_STATUSES);
        // POD 를 물고 있는 작업만
        condition.addFilter("podCd", OrmConstants.IS_NOT_NULL, null);
        // 아직 완료되지 않은 작업만
        condition.addFilter("completed", false);

        return this.queryManager.selectList(TbWcsOrder.class, condition);
    }
}