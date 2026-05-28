package operato.logis.samsung.service.mw;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.consts.InboundStatus;
import operato.logis.samsung.consts.JobStatus;
import operato.logis.samsung.entity.mw.TbMwInboundDelivery;
import operato.logis.samsung.entity.mw.TbMwInboundDeliveryHist;
import operato.logis.samsung.entity.mw.TbMwInboundJob;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.ValueUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
//@RequiredArgsConstructor // TbMwInboundDeliveryService와 무한 호출문제로 중지 // 에러수정
public class TbMwInboundJobService extends AbstractQueryService {

    private final TbMwInboundDeliveryService tbMwInboundDeliveryService;

    public TbMwInboundJobService(@Lazy TbMwInboundDeliveryService tbMwInboundDeliveryService) {
        this.tbMwInboundDeliveryService = tbMwInboundDeliveryService;
    }

    public List<TbMwInboundJob> getInboundJobByDate(Date targetDate) {
        String sql =
                "select * " +
                        "  from tb_mw_inbound_job " +
                        " where inbound_date = :inboundDate";

        Map<String, Object> param = ValueUtil.newMap(
                "inboundDate",
                new java.sql.Date(targetDate.getTime())   // date 컬럼에 맞게 변환
        );
        return this.queryManager.selectListBySql(sql, param, TbMwInboundJob.class, 0, 0);
    }

    public List<TbMwInboundJob> getInboundJobByStatus(JobStatus jobStatus) {
        String sql =
                "select * " +
                        "  from tb_mw_inbound_job " +
                        " where job_status = :jobStatus";

        Map<String, Object> param = ValueUtil.newMap("jobStatus", jobStatus.value());
        return this.queryManager.selectListBySql(sql, param, TbMwInboundJob.class, 0, 0);
    }

    public List<TbMwInboundJob> getInboundJobByDateSdl(String startDate, String endDate) {
        String sql =
                "select * " +
                        "  from tb_mw_inbound_job " +
                        " where inbound_date between :startDate and :endDate";

        Map<String, Object> param = ValueUtil.newMap(
                "startDate,endDate",
                java.sql.Date.valueOf(startDate), // String을 곧바로 sql.Date로 변환!
                java.sql.Date.valueOf(endDate)    // String을 곧바로 sql.Date로 변환!
        );


        return this.queryManager.selectListBySql(sql, param, TbMwInboundJob.class, 0, 0);
    }

    public TbMwInboundJob getInboundJobWithLock(String blNo, String cntrNo) {
        String sql = "select * from tb_mw_inbound_job where bl_no = :blNo and cntr_no = :cntrNo for update";
        Map<String, Object> param = ValueUtil.newMap("blNo,cntrNo", blNo, cntrNo);
        return this.queryManager.selectBySql(sql, param, TbMwInboundJob.class);
    }

    public TbMwInboundJob getInboundJobByBlCntr(String blNo, String cntrNo) {
        String sql = "select * from tb_mw_inbound_job where bl_no = :blNo and cntr_no = :cntrNo";
        Map<String, Object> param = ValueUtil.newMap("blNo,cntrNo", blNo, cntrNo);
        return this.queryManager.selectBySql(sql, param, TbMwInboundJob.class);
    }

    public void updateResultQty(String blNo, String cntrNo, int passQty, int ngQty) {
        TbMwInboundJob job = getInboundJobWithLock(blNo, cntrNo);
        if (ValueUtil.isEmpty(job)) return;

        job.setcompletedItemQty(job.getcompletedItemQty() + passQty);
        job.setNgItemQty(job.getNgItemQty() + ngQty);
        this.queryManager.update(job, "completedItemQty", "ngItemQty");
    }

    /**
     * 입고 작업 시작
     */
    public Map<String, Object> getStartDelivery(TbMwInboundJob data) {
        // 요청 Job 조회
        TbMwInboundJob job = getInboundJobWithLock(data.getBlNo(), data.getCntrNo());

        // Job 유효성 검사
        if (ValueUtil.isEmpty(job)) {
            return createResponse(1, "주문 정보가 존재하지 않습니다.");
        } else if (!isStartable(job.getJobStatus())) {
            return createResponse(1, "대기 중인 작업이 아닙니다.");
        }

        // 싱글 스큐 모드 사용여부
        int maxJobCnt = Integer.parseInt(SettingUtil.getValue("mw.job.cntr.max.cnt", "1"));
        List<TbMwInboundJob> jobs = hasRunningJob();

        // 진행 중인 컨테이너 작업 수 조회
        if (jobs.size() >= maxJobCnt) {
            String cntrNos = jobs.stream()
                    .map(TbMwInboundJob::getCntrNo)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("|"));

            return createResponse(2, "진행 중인 작업이 존재합니다.[" + cntrNos + "]");
        }

        // 2026.04.14.JJG. 실제 첫 BCR 입고되는 순간 Running 상태변경으로 옮김
        // 재 시작일 경우엔 Delivery 상태 원복
        updateDeliveryStatus(
                data.getBlNo(),
                data.getCntrNo(),
                null,
                InboundStatus.READY,
                List.of(InboundStatus.PAUSED),
                false);

        // 작업 시작 (Job 상태 변경)
        job.setJobStatus(JobStatus.RUNNING.value());
        job.setJobStatusDesc("실행중");
        job.setJobStartDt(new Date());
        this.queryManager.update(job, "jobStatus", "jobStartDt");

        return createResponse(0, "작업이 시작되었습니다!");
    }

    /**
     * 입고 작업 완료
     */
    public void getDoneDelivery(TbMwInboundDelivery data) {
        TbMwInboundJob job = getInboundJobWithLock(data.getBlNo(), data.getCntrNo());

        // Job 유효성 검사
        if (ValueUtil.isEmpty(job) || !JobStatus.RUNNING.value().equals(job.getJobStatus())) {
            return;
        }

        // 작업 완료
        job.setJobStatus(JobStatus.DONE.value());
        job.setJobStatusDesc("완료");
        job.setJobEndDt(new Date());
        this.queryManager.update(job, "jobStatus", "jobEndDt");
    }

    /**
     * 입고 작업 일시정지
     */
    public Map<String, Object> getPauseDelivery(TbMwInboundDelivery data) {
        // 요청 Job 조회
        TbMwInboundJob job = getInboundJobWithLock(data.getBlNo(), data.getCntrNo());

        // Job 유효성 검사
        if (ValueUtil.isEmpty(job)) {
            return createResponse(1, "주문 정보가 존재하지 않습니다.");
        } else if (!JobStatus.RUNNING.value().equals(job.getJobStatus())) {
            return createResponse(1, "진행 중인 작업이 아닙니다.");
        }

        // 작업 일시정지 (Delivery 상태 변경: 오직 inbound_status = 1 일 때만)
        int updatedData = updateDeliveryStatus(
                data.getBlNo(),
                data.getCntrNo(),
                null,
                InboundStatus.PAUSED,
                Arrays.asList(InboundStatus.RUNNING, InboundStatus.READY),
                false);

        // 변경된 작업이 없는 경우
        /*if (updatedData == 0) {
            return createResponse(2, "진행 중인 입고 정보가 존재하지 않습니다.");
        }*/

        // 작업 일시정지 (Job 상태 변경)
        job.setJobStatus(JobStatus.PAUSED.value());
        job.setJobStatusDesc("정지");
        this.queryManager.update(job, "jobStatus");

        return createResponse(0, "작업이 일시정지되었습니다!");
    }

    /**
     * 입고 작업 강제종료
     */
    public Map<String, Object> getAbortDelivery(TbMwInboundDelivery data) {
        // 요청 Job 조회
        TbMwInboundJob job = getInboundJobWithLock(data.getBlNo(), data.getCntrNo());

        // Job 유효성 검사
        if (ValueUtil.isEmpty(job)) {
            return createResponse(1, "주문 정보가 존재하지 않습니다.");
        } else if (!JobStatus.RUNNING.value().equals(job.getJobStatus())) {
            return createResponse(1, "진행 중인 작업이 아닙니다.");
        }

        // 작업 강제종료 (Job 상태 변경)
        job.setJobStatus(JobStatus.ABORTED.value());
        job.setJobStatusDesc("강제종료");
        job.setJobEndDt(new Date());
        this.queryManager.update(job, "jobStatus", "jobEndDt");

        return createResponse(0, "작업이 강제종료되었습니다!");
    }

    // --- Private Helper Methods ---

    /**
     * 작업을 시작할 수 있는 상태인지 확인 (READY 또는 PAUSED)
     */
    private boolean isStartable(Integer status) {
        return JobStatus.READY.value().equals(status) || JobStatus.PAUSED.value().equals(status);
    }

    /**
     * 현재 RUNNING 상태인 컨테이너 수 조회
     */
    private List<TbMwInboundJob> hasRunningJob() {
        String sql = "select * from tb_mw_inbound_job where job_status = :status";
        Map<String, Object> param = ValueUtil.newMap("status", JobStatus.RUNNING.value());
        return this.queryManager.selectListBySql(sql, param, TbMwInboundJob.class, 0, 0);
    }

    /**
     * Delivery 상태 및 시간 업데이트
     *
     * @param blNo          B/L No
     * @param cntrNo        컨테이너 번호
     * @param innerItemCode null 또는 빈값이면 조건 없이 업데이트, 값이 있으면 해당 inner_item_code 조건 추가
     * @param targetStatus  변경할 상태
     * @param currentStatus null이면 상태 조건 없이 업데이트, 값이 있으면 해당 상태일 때만 업데이트
     * @param updateTime    true면 시간 컬럼도 함께 업데이트
     */
    /**
     * Delivery 상태 및 시간 업데이트
     *
     * @param blNo            B/L No
     * @param cntrNo          컨테이너 번호
     * @param innerItemCode   null 또는 빈값이면 조건 없이 업데이트
     * @param targetStatus    변경할 상태
     * @param currentStatuses null 또는 비어있으면 상태 조건 없이 업데이트
     * @param updateTime      true면 시간 컬럼도 함께 업데이트
     */
    public int updateDeliveryStatus(
            String blNo,
            String cntrNo,
            String innerItemCode,
            InboundStatus targetStatus,
            List<InboundStatus> currentStatuses,
            boolean updateTime
    ) {
        StringBuilder sql = new StringBuilder(
                "update tb_mw_inbound_delivery " +
                        "set inbound_status = :targetStatus"
        );

        Map<String, Object> params = new HashMap<>();
        params.put("targetStatus", targetStatus.value());
        params.put("blNo", blNo);
        params.put("cntrNo", cntrNo);

        if (updateTime) {
            sql.append(targetStatus == InboundStatus.RUNNING
                    ? ", start_datetime = :now"
                    : ", complete_datetime = :now");
            params.put("now", new Date());
        }

        sql.append(" where bl_no = :blNo and cntr_no = :cntrNo");

        if (ValueUtil.isNotEmpty(innerItemCode)) {
            sql.append(" and inner_item_code = :innerItemCode");
            params.put("innerItemCode", innerItemCode);
        }

        if (ValueUtil.isNotEmpty(currentStatuses)) {
            sql.append(" and inbound_status in (:currentStatuses)");
            params.put(
                    "currentStatuses",
                    currentStatuses.stream()
                            .map(InboundStatus::value)
                            .collect(Collectors.toList())
            );
        }

        return this.queryManager.executeBySql(sql.toString(), params);
    }

    /**
     * 공통 응답 맵 생성
     */
    private Map<String, Object> createResponse(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        return response;
    }

    public Map<String, Object> deleteDeliveryJob(TbMwInboundJob req) {
        Map<String, Object> result = new HashMap<>();

        try {

            List<TbMwInboundDelivery> deliveryList = this.tbMwInboundDeliveryService.getInboundDeliveryDateList(
                    req.getBlNo(),
                    req.getCntrNo(),
                    req.getInboundDate()
            );

            if (deliveryList != null && !deliveryList.isEmpty()) {
                List<TbMwInboundDeliveryHist> histList = new ArrayList<>();

                for (TbMwInboundDelivery delivery : deliveryList) {
                    TbMwInboundDeliveryHist hist = new TbMwInboundDeliveryHist();

                    BeanUtils.copyProperties(delivery, hist);

                    // PK 제약조건 충돌 방지를 위해 id 값 비우기 (UUID 새로 채번)
                    hist.setId(null);
                    // 삭제된 이력임을 명시
                    hist.setRemark("DELETED");

                    histList.add(hist);
                }

                // History 테이블 일괄 Insert
                this.queryManager.insertBatch(histList);
            }

            // =======================================================
            // 3. 삭제 처리용 SQL 파라미터 세팅 및 DB 삭제
            // =======================================================
            Map<String, Object> param = new HashMap<>();
            param.put("blNo", req.getBlNo());
            param.put("cntrNo", req.getCntrNo());
            param.put("inboundDate", req.getInboundDate());

            // 3-1. 하단 상세 데이터(TbMwInboundDelivery) DB 삭제
            String deleteDeliverySql = "DELETE FROM tb_mw_inbound_delivery WHERE bl_no = :blNo AND cntr_no = :cntrNo AND inbound_date = :inboundDate";
            this.queryManager.executeBySql(deleteDeliverySql, param);

            // 3-2. 상단 작업 데이터(TbMwInboundJob) DB 삭제
            String deleteJobSql = "DELETE FROM tb_mw_inbound_job WHERE bl_no = :blNo AND cntr_no = :cntrNo AND inbound_date = :inboundDate";
            this.queryManager.executeBySql(deleteJobSql, param);

            // 성공 결과 반환
            result.put("code", 0);
            result.put("message", "정상적으로 삭제되었습니다.");

        } catch (Exception e) {
            // 실패 시 에러 결과 세팅 및 롤백 처리
            result.put("code", -1);
            result.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());

            // @Transactional 롤백을 위한 런타임 에러 던지기
            throw new RuntimeException("컨테이너 삭제 로직 수행 중 오류 발생", e);
        }

        return result;
    }
}