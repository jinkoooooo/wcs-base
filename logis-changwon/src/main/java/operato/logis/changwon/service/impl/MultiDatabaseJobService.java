package operato.logis.changwon.service.impl;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.WcsConstants;
import operato.logis.changwon.dto.LmsRackDto;
import operato.logis.changwon.entity.LMS.LmsStock;
import operato.logis.changwon.entity.LMS.LmsTask;
import operato.logis.changwon.entity.MFC.*;
import operato.logis.changwon.entity.WCS.WcsStockInfo;
import operato.logis.changwon.entity.WCS.WcsTask;
import org.springframework.stereotype.Service;
import xyz.elidom.orm.IDataSourceManager;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MultiDatabaseJobService extends AbstractQueryService {

    private final IDataSourceManager dataSourceManager;

    public void receiveErrorLogFromMfc() {
        WcsConstants.setupDomainContext();
        // 1. MFC 데이터베이스에서 전송할 데이터 조회 SQL
        String selectSql = "SELECT * FROM c_err_log WHERE flag = 0";
        IQueryManager mfcQueryManager = dataSourceManager.getQueryManager("mfc");
        List<ErrLog> mfcErrLogList = mfcQueryManager.selectListBySql(selectSql, null, ErrLog.class, 0, 0);

        if (mfcErrLogList == null || mfcErrLogList.isEmpty()) {
            // 처리할 데이터가 없으면 종료
            return;
        }

        logger.info("MFC -> WCS로 전송할 c_err_log 데이터 {}건을 조회했습니다.", mfcErrLogList.size());

        // 2. 리스트를 순회하며 WCS 데이터베이스에 INSERT 실행
        for (ErrLog mfcErrLog : mfcErrLogList) {
            ErrLog wcsErrLog = new ErrLog();

            // MFC에서 조회한 데이터를 WCS 엔티티로 복사
            wcsErrLog.setOrderId(mfcErrLog.getOrderId());
            wcsErrLog.setJobNo(mfcErrLog.getJobNo());
            wcsErrLog.setErrorDatetime(mfcErrLog.getErrorDatetime());
            wcsErrLog.setErrorMachine(mfcErrLog.getErrorMachine());
            wcsErrLog.setErrorCode(mfcErrLog.getErrorCode());
            wcsErrLog.setErrorTier(mfcErrLog.getErrorTier());
            wcsErrLog.setErrorBay(mfcErrLog.getErrorBay());
            wcsErrLog.setErrorBank(mfcErrLog.getErrorBank());
            wcsErrLog.setResetDatetime(mfcErrLog.getResetDatetime());
            wcsErrLog.setRackNo(mfcErrLog.getRackNo());

            // WCS 테이블 설정
            wcsErrLog.setFlag(0);
            wcsErrLog.setIsChecked(false);

            // 3-1. WCS DB에 INSERT 실행
            this.queryManager.insert(wcsErrLog);

            // 3-2. 원본 MFC 데이터의 flag를 9로 업데이트
            String updateSql = "UPDATE c_err_log SET flag = 9 WHERE order_id = :orderId AND job_no = :jobNo AND error_datetime = :errorDatetime";

            Map<String, Object> params = new HashMap<>();
            params.put("orderId", mfcErrLog.getOrderId());
            params.put("jobNo", mfcErrLog.getJobNo());
            params.put("errorDatetime", mfcErrLog.getErrorDatetime());

            mfcQueryManager.executeBySql(updateSql, params);

            logger.info("성공적으로 Order ID [{}] / Job No [{}]의 에러 로그를 WCS로 전송했습니다.", mfcErrLog.getOrderId(), mfcErrLog.getJobNo());
        }
    }

    public void receiveResultFromMfc() {
        WcsConstants.setupDomainContext();
        // 1. MFC 데이터베이스에서 전송할 데이터 조회 (flag가 0인 데이터)
        String selectSql = "SELECT * FROM c_job_ret WHERE flag = 0";
        IQueryManager mfcQueryManager = dataSourceManager.getQueryManager("mfc");
        List<JobRet> mfcJobRetList = mfcQueryManager.selectListBySql(selectSql, null, JobRet.class, 0, 0);

        if (mfcJobRetList == null || mfcJobRetList.isEmpty()) {
            // 처리할 데이터가 없으면 종료
            return;
        }

        logger.info("MFC -> WCS로 전송할 c_job_ret 데이터 {}건을 조회했습니다.", mfcJobRetList.size());

        // 2. 리스트를 순회하며 WCS 데이터베이스에 INSERT 실행
        for (JobRet mfcJob : mfcJobRetList) {
            JobRet wcsJob = new JobRet();

            // MFC에서 조회한 데이터를 WCS 엔티티로 복사
            wcsJob.setOrderId(mfcJob.getOrderId());
            wcsJob.setJobNo(mfcJob.getJobNo());
            wcsJob.setWmsOrdNo(mfcJob.getWmsOrdNo());
            wcsJob.setUpdateDatetime(mfcJob.getUpdateDatetime());
            wcsJob.setOrderKind(mfcJob.getOrderKind());
            wcsJob.setResultType(mfcJob.getResultType());
            wcsJob.setCompleteType(mfcJob.getCompleteType());
            wcsJob.setErrorMachine(mfcJob.getErrorMachine());
            wcsJob.setErrorCode(mfcJob.getErrorCode());
            wcsJob.setErrorZ(mfcJob.getErrorZ());
            wcsJob.setErrorX(mfcJob.getErrorX());
            wcsJob.setErrorY(mfcJob.getErrorY());

            // WCS 테이블에는 flag를 0으로 입력
            wcsJob.setFlag(0);

            // 3-2. WCS DB에 INSERT 실행
            this.queryManager.insert(wcsJob);

            // 3-3. 원본 MFC 데이터의 flag를 9로 업데이트하여 중복 전송 방지
            // 조건: 키 값들이 NULL일 경우까지 고려한 SQL
            String updateSql = "UPDATE c_job_ret SET flag = 9 " +
                    "WHERE (:wmsOrdNo IS NULL AND wms_ord_no IS NULL OR wms_ord_no = :wmsOrdNo) " +
                    "  AND (:orderId IS NULL AND order_id IS NULL OR order_id = :orderId) " +
                    "  AND (:jobNo IS NULL AND job_no IS NULL OR job_no = :jobNo)";

            Map<String, Object> params = new HashMap<>();
            params.put("wmsOrdNo", mfcJob.getWmsOrdNo());
            params.put("orderId", mfcJob.getOrderId());
            params.put("jobNo", mfcJob.getJobNo());

            mfcQueryManager.executeBySql(updateSql, params);

            logger.info("성공적으로 WMS Order No [{}] / Order ID [{}]의 작업 결과를 WCS로 전송했습니다.",
                    mfcJob.getWmsOrdNo(), mfcJob.getOrderId());
        }
    }

    public void sendTaskToMfc() {
        WcsConstants.setupDomainContext();
        // 1. 원본 데이터베이스에서 전송할 데이터 조회
        String selectSql = "SELECT * FROM c_job_odr WHERE data_transmit_status = 0 ORDER BY order_priority ASC";
        IQueryManager mfcQueryManager = dataSourceManager.getQueryManager("mfc");
        List<JobOdr> jobOdrList = this.queryManager.selectListBySql(selectSql, null, JobOdr.class, 0, 0);

        if (jobOdrList == null || jobOdrList.isEmpty()) {
            // 처리할 데이터가 없으면 종료
            return;
        }

        // 2. 대상 데이터베이스에 INSERT 하기 위한 SQL 정의
        String insertSql = """
            INSERT INTO c_job_odr (
                order_id, wms_ord_no, job_no, am_kbn, order_status, order_type, order_kind, order_phase,
                order_trans_status, mfc_result, sn, sz, sx, sy, ez, ex, ey,
                current_machine, current_position, error_count, order_priority,
                order_receive_datetime, order_mfc_datetime, update_datetime,
                storage_id, source_id, rack_no, lugg_info, pallet_id, userdata
            ) VALUES (
                :order_id, :wms_ord_no, :job_no, :am_kbn, :order_status, :order_type, :order_kind, :order_phase,
                :order_trans_status, :mfc_result, :sn, :sz, :sx, :sy, :ez, :ex, :ey,
                :current_machine, :current_position, :error_count, :order_priority,
                :order_receive_datetime, :order_mfc_datetime, :update_datetime,
                :storage_id, :source_id, :rack_no, :lugg_info, :pallet_id, :userdata
            )
            """;

        for (JobOdr job : jobOdrList) {
            try {
                // 3. 파라미터 바인딩
                Map<String, Object> param = new HashMap<>();
                param.put("order_id", job.getOrderId());
                param.put("wms_ord_no", job.getWmsOrdNo());
                param.put("job_no", job.getJobNo());
                param.put("am_kbn", job.getAmKbn());
                param.put("order_status", job.getOrderStatus());
                param.put("order_type", job.getOrderType());
                param.put("order_kind", job.getOrderKind());
                param.put("order_phase", job.getOrderPhase());
                param.put("order_trans_status", job.getOrderTransStatus());
                param.put("mfc_result", job.getMfcResult());
                param.put("sn", job.getSn());
                param.put("sz", job.getSz());
                param.put("sx", job.getSx());
                param.put("sy", job.getSy());
                param.put("ez", job.getEz());
                param.put("ex", job.getEx());
                param.put("ey", job.getEy());
                param.put("current_machine", job.getCurrentMachine());
                param.put("current_position", job.getCurrentPosition());
                param.put("error_count", job.getErrorCount());
                param.put("order_priority", job.getOrderPriority());
                param.put("order_receive_datetime", job.getOrderReceiveDatetime());
                param.put("order_mfc_datetime", job.getOrderMfcDatetime());
                param.put("update_datetime", job.getUpdateDatetime());
                param.put("storage_id", job.getStorageId());
                param.put("source_id", job.getSourceId());
                param.put("rack_no", job.getRackNo());
                param.put("lugg_info", job.getLuggInfo());
                param.put("pallet_id", job.getPalletId());
                param.put("userdata", job.getUserdata());

                // 4. INSERT 실행
                mfcQueryManager.executeBySql(insertSql, param);

                // 5. 원본 데이터 상태 업데이트
                job.setDataTransmitStatus(1);
                this.queryManager.update(job, "dataTransmitStatus");

                logger.info("성공적으로 Order ID [{}]를 전송했습니다.", job.getOrderId());
            } catch (Exception e) {
                // 원본 데이터 에러 상태 업데이트
                job.setDataTransmitStatus(9);
                this.queryManager.update(job, "dataTransmitStatus");

                logger.error("Order ID [{}] 전송 중 오류 발생", job.getOrderId(), e);
            }
        }
    }

    public void syncMfcErrorDef() {
        WcsConstants.setupDomainContext();
        // 1. MFC 데이터베이스에서 전송할 데이터 조회
        String selectSql = "SELECT * FROM c_err_def";
        IQueryManager mfcQueryManager = dataSourceManager.getQueryManager("mfc");
        List<ErrDef> mfcErrDefList = mfcQueryManager.selectListBySql(selectSql, null, ErrDef.class, 0, 0);

        if (mfcErrDefList == null || mfcErrDefList.isEmpty()) {
            logger.info("MFC 데이터베이스에 조회된 에러 정의 데이터가 없습니다.");
            return;
        }

        logger.info("MFC -> WCS로 전송할 c_err_def 데이터 {}건을 조회했습니다.", mfcErrDefList.size());

        try {
            // 2. WCS 데이터베이스에서 기존 에러 정의 삭제
            String deleteSql = "DELETE FROM c_err_def";
            this.queryManager.executeBySql(deleteSql, null);

            // 3. WCS 데이터베이스 동기화 (Batch Insert 실행)
            this.queryManager.insertBatch(mfcErrDefList);

            logger.info("성공적으로 {}건의 에러 정의 데이터를 WCS로 동기화했습니다.", mfcErrDefList.size());
        } catch (Exception e) {
            logger.error("에러 정의 데이터 동기화 중 오류 발생: ", e);
        }
    }

    public void syncMfcPrsJobSts() {
        WcsConstants.setupDomainContext();
        // 1. MFC 데이터베이스에서 조회할 SQL 정의
        String selectSql = "SELECT " +
                           "  UpdateTime  AS update_time, " +
                           "  MachineID   AS machine_id, " +
                           "  JobNo       AS job_no, " +
                           "  JobPhase    AS job_phase, " +
                           "  Tier        AS tier, " +
                           "  Bay         AS bay, " +
                           "  Bank        AS bank, " +
                           "  XActPos     AS x_act_pos, " +
                           "  ModeStatus  AS mode_status, " +
                           "  ObstacleChk AS obstacle_chk, " +
                           "  LoadChk     AS load_chk, " +
                           "  JobStatus   AS job_status, " +
                           "  StatusSend  AS status_send, " +
                           "  ErrorCode   AS error_code, " +
                           "  Battery     AS battery, " +
                           "  PalletID    AS pallet_id, " +
                           "  OrderId     AS order_id " +
                           "FROM PRS_JOB_STS";

        // 2. MFC 데이터베이스에서 데이터 조회
        IQueryManager mfcQueryManager = dataSourceManager.getQueryManager("mfc");
        List<PrsJobSts> mfcPrsJobStsList = mfcQueryManager.selectListBySql(selectSql, null, PrsJobSts.class, 0, 0);

        if (mfcPrsJobStsList == null || mfcPrsJobStsList.isEmpty()) {
            logger.debug("MFC에서 조회된 PRS 작업 상태 데이터가 없습니다.");
            return;
        }

        try {
            // 3. WCS 데이터베이스의 기존 데이터 삭제
            String deleteSql = "DELETE FROM prs_job_sts";
            this.queryManager.executeBySql(deleteSql, null);

            // 4. WCS 데이터베이스에 Batch Insert 실행
            this.queryManager.insertBatch(mfcPrsJobStsList);
        } catch (Exception e) {
            logger.error("PRS 작업 상태 동기화 중 오류 발생: ", e);
        }
    }

    public void syncWcsToLmsStatusBoard() {
        // 1. LMS DB 접근을 위한 QueryManager 획득
        WcsConstants.setupDomainContext();
        IQueryManager lmsQueryManager;
        try {
            lmsQueryManager = dataSourceManager.getQueryManager("lms");
        } catch (Exception e) {
            logger.error("Failed to get LMS DataSource: {}", e.getMessage());
            return;
        }

        // 2. WCS 데이터 조회
        String selectWcsSql = """
            SELECT loc_cd,
                   task_id,
                   stock_id,
                   COALESCE(rack_disabled, 0) AS rack_disabled,
                   COALESCE(rack_locked, 0)   AS rack_locked
            FROM wcs_stock_auto
            WHERE loc_cd IS NOT NULL
            """;
        List<LmsRackDto> wcsDataList = this.queryManager.selectListBySql(selectWcsSql, null, LmsRackDto.class, 0, 0);
        if (ValueUtil.isEmpty(wcsDataList)) {
            return;
        }

        // 3. LMS 업데이트 쿼리
        String updateLmsSql = """
            UPDATE status_board_dmi
            SET task_id = :taskId,
                stock_id = :stockId,
                box_is_use = :boxIsUse,
                instance_status = :instanceStatus
            WHERE model_code = :locCd
              AND lc_id = 'GNM001'
            """;

        // 4. 데이터 가공 및 LMS 업데이트 실행
        for (LmsRackDto wcsRow : wcsDataList) {
            Map<String, Object> params = new HashMap<>();

            // 데이터 추출
            String locCd = wcsRow.getLocCd();
            String taskId = wcsRow.getTaskId();
            String stockId = wcsRow.getStockId();

            // 숫자형 데이터 안전하게 처리
            int rackDisabled = wcsRow.getRackDisabled();
            int rackLocked = wcsRow.getRackLocked();

            // 로직 1: box_is_use (stock_id가 null이나 공백이 아니면 true)
            boolean isStockIdPresent = stockId != null && !stockId.trim().isEmpty();
            params.put("boxIsUse", isStockIdPresent);

            // 로직 2: instance_status 설정
            Integer instanceStatus = null;
            if (rackDisabled == 1) {
                instanceStatus = 9;
            } else if (rackLocked != 0) {
                instanceStatus = 2;
            }
            // 그 외(else)는 초기값 null 유지

            // 파라미터 세팅
            params.put("taskId", taskId);
            params.put("stockId", stockId);
            params.put("instanceStatus", instanceStatus);
            params.put("locCd", locCd);

            // LMS DB 업데이트 실행
            try {
                lmsQueryManager.executeBySql(updateLmsSql, params);
            } catch (Exception e) {
                logger.error("Failed to update LMS for loc_cd: {} - {}", locCd, e.getMessage());
            }
        }
    }

    public void syncWcsToLmsShuttleAndRunner() {
        // 1. LMS DB 접근을 위한 QueryManager 획득
        WcsConstants.setupDomainContext();
        IQueryManager lmsQueryManager;
        try {
            lmsQueryManager = dataSourceManager.getQueryManager("lms");
        } catch (Exception e) {
            logger.error("Failed to get LMS DataSource: {}", e.getMessage());
            return;
        }

        // 2. WCS 데이터 조회
        String selectWcsSql = """
        SELECT *
        FROM prs_job_sts
        """;

        List<PrsJobSts> wcsDataList = this.queryManager.selectListBySql(selectWcsSql, null, PrsJobSts.class, 0, 0);
        if (ValueUtil.isEmpty(wcsDataList)) {
            return;
        }

        // 3. LMS 업데이트 쿼리
        String updateLmsSql = """
        UPDATE status_board_dmi
        SET task_id = :taskId,
            stock_id = :stockId,
            box_is_use = :boxIsUse,
            instance_status = :instanceStatus,
            error_code = :errorCode,
            error_message = :errorMessage,
            position_x_2d = :posX,
            position_y_2d = :posY
        WHERE model_code = :machineId
          AND lc_id = 'GNM001'
        """;

        // 4. 데이터 가공 및 업데이트 실행
        for (PrsJobSts jobSts : wcsDataList) {
            Map<String, Object> params = new HashMap<>();

            // 기본 정보 추출
            Integer machineId = jobSts.getMachineId();
            Integer loadChk = jobSts.getLoadChk();
            String wmsOrdNo = jobSts.getWmsOrdNo();
            Integer errorCode = jobSts.getErrorCode();
            int tier = jobSts.getTier();
            int bank = jobSts.getBank();
            int bay = jobSts.getBay();

            // [로직 1] stock_id 조회 (wcs_task 테이블)
            String stockId = null;
            if (ValueUtil.isNotEmpty(wmsOrdNo)) {
                String stockSql = """
                SELECT stock_id
                FROM wcs_task
                WHERE task_id = :taskId
                ORDER BY accept_datetime DESC
                LIMIT 1
                """;
                Map<String, Object> stockParam = new HashMap<>();
                stockParam.put("taskId", wmsOrdNo);
                stockId = this.queryManager.selectBySql(stockSql, stockParam, String.class);
            }

            // [로직 2] error_message 조회 (c_err_def 테이블)
            String errorMessage = null;
            if (ValueUtil.isNotEmpty(errorCode) && errorCode != 0) {
                String errorMachinePrefix = "";
                if (machineId >= 201 && machineId <= 206) errorMachinePrefix = "STT";
                else if (machineId >= 401 && machineId <= 406) errorMachinePrefix = "RUN";

                if (!errorMachinePrefix.isEmpty()) {
                    String errorMachineCode = errorMachinePrefix + machineId;
                    String errorSql = """
                    SELECT error_name
                    FROM c_err_def
                    WHERE error_machine = :errorMachine
                      AND error_code = :errorCode
                    LIMIT 1
                    """;
                    Map<String, Object> errorParam = new HashMap<>();
                    errorParam.put("errorMachine", errorMachineCode);
                    errorParam.put("errorCode", errorCode);

                    errorMessage = this.queryManager.selectBySql(errorSql, errorParam, String.class);
                }
            }

            // [로직 3] box_is_use 설정
            // machine_id가 201 ~ 206이고 load_chk가 3이면 true, 아니면 false
            boolean boxIsUse = false;
            if (machineId >= 201 && machineId <= 206) {
                boxIsUse = (loadChk != null && loadChk == 3);
            } else if (machineId >= 401 && machineId <= 406) {
                boxIsUse = (loadChk != null && loadChk == 1);
            }
            params.put("boxIsUse", boxIsUse);

            // [로직 4] instance_status 설정
            // 1순위: error_code != 0 -> 9
            // 2순위: wms_ord_no != null -> 2
            // 3순위: 그 외 -> 1
            int instanceStatus;
            if (ValueUtil.isNotEmpty(errorCode) && errorCode != 0) {
                instanceStatus = 9;
            } else if (ValueUtil.isNotEmpty(wmsOrdNo)) {
                instanceStatus = 2;
            } else {
                instanceStatus = 1;
            }
            params.put("instanceStatus", instanceStatus);

            // [로직 5] Position 계산
            int xOffset = 0;
            switch (machineId) {
                case 201: case 401: xOffset = 150; break;
                case 202: case 402: xOffset = 510; break;
                case 203: case 403: xOffset = 975; break;
                case 204: case 404: xOffset = 1335; break;
                case 205: case 405: xOffset = 1780; break;
                case 206: case 406: xOffset = 2140; break;
            }

            int posX = xOffset;
            int posY = bay * 30 + 270;

            // 파라미터 매핑
            params.put("machineId", String.valueOf(machineId));
            params.put("taskId", wmsOrdNo);
            params.put("stockId", stockId);
            params.put("boxIsUse", boxIsUse);
            params.put("instanceStatus", instanceStatus);
            params.put("errorCode", String.valueOf(errorCode));
            params.put("errorMessage", errorMessage);
            params.put("posX", posX);
            params.put("posY", posY);

            // LMS DB 업데이트 실행
            try {
                lmsQueryManager.executeBySql(updateLmsSql, params);
            } catch (Exception e) {
                logger.error("Failed to update LMS (Shuttle/Runner) for machine_id: {} - {}", machineId, e.getMessage());
            }
        }
    }

    public void syncWcsToLmsTask(WcsTask task) {
        // 1. LMS DB 접근을 위한 QueryManager 획득
        WcsConstants.setupDomainContext();
        IQueryManager lmsQueryManager;
        try {
            lmsQueryManager = dataSourceManager.getQueryManager("lms");
        } catch (Exception e) {
            logger.error("Failed to get LMS DataSource: {}", e.getMessage());
            return;
        }

        // 2. 데이터 가공 및 LMS DB 업데이트 실행
        if (ValueUtil.isNotEmpty(task)) {
            String commandType = switch (task.getOrderKind()) {
                case "1" -> "입고";
                case "2" -> "출고";
                default -> "재고이동";
            };

            LmsTask lmsTask = new LmsTask();
            lmsTask.setLcId("GNM001");
            lmsTask.setTaskId(task.getTaskId());
            lmsTask.setCommandType(commandType);
            lmsTask.setStartPointCd(task.getStartPointCd());
            lmsTask.setEndPointCd(task.getEndPointCd());

            lmsQueryManager.insert(lmsTask);
        }
    }

    public void syncWcsToLmsStock(WcsStockInfo stock) {
        // 1. LMS DB 접근을 위한 QueryManager 획득
        WcsConstants.setupDomainContext();
        IQueryManager lmsQueryManager;
        try {
            lmsQueryManager = dataSourceManager.getQueryManager("lms");
        } catch (Exception e) {
            logger.error("Failed to get LMS DataSource: {}", e.getMessage());
            return;
        }

        // 2. 데이터 가공 및 LMS DB 업데이트 실행
        if (ValueUtil.isNotEmpty(stock)) {
            LmsStock lmsStock = new LmsStock();
            lmsStock.setLcId("GNM001");
            lmsStock.setStockId(stock.getStockId());
            lmsStock.setItemCode(stock.getItemCode());
            lmsStock.setItemName(stock.getItemName());
            lmsStock.setItemQty(stock.getBoxQty());

            lmsQueryManager.insert(lmsStock);
        }
    }
}