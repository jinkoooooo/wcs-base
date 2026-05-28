package operato.logis.lms.service.impl.center;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.lms.dto.center.LmsAlarmRequestDto;
import operato.logis.lms.dto.center.LmsStatusRequestDto;
import operato.logis.lms.entity.monitoring.LmsAlarmStatusDev;
import operato.logis.lms.entity.monitoring.LmsEquipStatusDev;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LmsTotalService extends AbstractQueryService {

    // =========================================================
    // 1. 알람 처리 (Alarm)
    // =========================================================
    @Transactional
    public void saveAlarmList(List<LmsAlarmRequestDto> list) {
        Domain.setCurrentDomain(new Domain("7"));

        for (LmsAlarmRequestDto dto : list) {
            this.processAlarmItem(dto);
        }
    }

    private void processAlarmItem(LmsAlarmRequestDto dto) {
        LmsAlarmStatusDev existAlarm = this.findAlarmByAlarmId(dto.getAlarmId(), dto.getEquipId());

        if (existAlarm != null) {

            existAlarm.setAlarmMsg(dto.getAlarmMsg());
            existAlarm.setIsCleared(dto.getIsCleared());
            existAlarm.setClearedBy(dto.getClearedBy());
            existAlarm.setClearedAt(dto.getClearedAt());
            existAlarm.setDurationSeconds(dto.getDurationSeconds());

            this.queryManager.update(existAlarm, "alarmMsg", "isCleared", "clearedBy", "clearedAt", "durationSeconds");

        } else {

            LmsAlarmStatusDev newAlarm = new LmsAlarmStatusDev();

            newAlarm.setLcId(dto.getLcId());
            newAlarm.setEquipId(dto.getEquipId());
            newAlarm.setLineId(dto.getLineId());
            newAlarm.setOrderId(dto.getOrderId());

            newAlarm.setAlarmId(dto.getAlarmId());
            newAlarm.setAlarmType(dto.getAlarmType());
            newAlarm.setAlarmMsg(dto.getAlarmMsg());
            newAlarm.setIsCleared(dto.getIsCleared() != null ? dto.getIsCleared() : false);
            newAlarm.setOccurredAt(dto.getOccurredAt());
            newAlarm.setSourceSystem(dto.getSourceSystem());
            newAlarm.setClearedAt(dto.getClearedAt());

            this.queryManager.insert(newAlarm);
        }
    }

    // =========================================================
    // 2. 상태 처리 (Status)
    // =========================================================
    @Transactional
    public void saveStatusList(List<LmsStatusRequestDto> list) {
        Domain.setCurrentDomain(new Domain("7"));

        for (LmsStatusRequestDto dto : list) {
            this.processStatusItem(dto);
        }
    }

    private void processStatusItem(LmsStatusRequestDto dto) {
        LmsEquipStatusDev existStatus = this.findStatusByEquipId(dto.getLcId(), dto.getEquipId());

        if (existStatus != null) {

            existStatus.setCurrentStatus(dto.getCurrentStatus());
            existStatus.setPreStatus(dto.getPreStatus());
            existStatus.setErrCd(dto.getErrCd());
            existStatus.setErrMsg(dto.getErrMsg());
            existStatus.setOperatingCnt(dto.getOperatingCnt());
            existStatus.setErrCnt(dto.getErrCnt());
            existStatus.setDataUpdatedAt(dto.getDataUpdatedAt());
            existStatus.setStatusUpdatedAt(dto.getStatusUpdatedAt());
            existStatus.setSensorValue(dto.getSensorValue()); // 센서값 누락 주의

            this.queryManager.update(existStatus, "currentStatus", "preStatus", "errCd", "errMsg", "sensorValue", "operatingCnt", "errCnt", "dataUpdatedAt", "statusUpdatedAt");

        } else {
            log.info("설비 상태 신규 등록: {} / {}", dto.getLcId(), dto.getEquipId());

            LmsEquipStatusDev newStatus = new LmsEquipStatusDev();

            newStatus.setLcId(dto.getLcId());
            newStatus.setEquipId(dto.getEquipId());
            newStatus.setLineId(dto.getLineId());
            newStatus.setOrderId(dto.getOrderId());

            newStatus.setCurrentStatus(dto.getCurrentStatus());
            newStatus.setPreStatus(dto.getPreStatus());
            newStatus.setErrCd(dto.getErrCd());
            newStatus.setErrMsg(dto.getErrMsg());
            newStatus.setSensorUnit(dto.getSensorUnit());
            newStatus.setSensorValue(dto.getSensorValue());
            newStatus.setOperatingCnt(dto.getOperatingCnt());
            newStatus.setErrCnt(dto.getErrCnt());
            newStatus.setDataUpdatedAt(dto.getDataUpdatedAt());
            newStatus.setStatusUpdatedAt(dto.getStatusUpdatedAt());
            newStatus.setSourceSystem(dto.getSourceSystem());

            this.queryManager.insert(newStatus);
        }
    }

    // =========================================================
    // 3. 내부 조회용 Helper (Native SQL)
    // =========================================================

    /**
     * 알람 ID + 설비 ID로 기존 알람 조회
     */
    private LmsAlarmStatusDev findAlarmByAlarmId(String alarmId, String equipId) {
        String sql = "SELECT * FROM lms_alarm_status_dev WHERE alarm_id = :alarmId AND equip_id = :equipId";

        Map<String, Object> params = new HashMap<>();
        params.put("alarmId", alarmId);
        params.put("equipId", equipId);

        return this.queryManager.selectBySql(sql, params, LmsAlarmStatusDev.class);
    }

    /**
     * 센터ID + 설비ID로 기존 상태 조회
     */
    private LmsEquipStatusDev findStatusByEquipId(String lcId, String equipId) {
        String sql = "SELECT * FROM lms_equipment_status_dev WHERE lc_id = :lcId AND equip_id = :equipId";

        Map<String, Object> params = new HashMap<>();
        params.put("lcId", lcId);
        params.put("equipId", equipId);

        return this.queryManager.selectBySql(sql, params, LmsEquipStatusDev.class);
    }
}