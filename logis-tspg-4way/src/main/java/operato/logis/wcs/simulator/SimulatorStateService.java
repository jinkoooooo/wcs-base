package operato.logis.wcs.simulator;

import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import operato.logis.wcs.entity.TbWcsSimulatorState;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 시뮬레이터 상태 관리 (그룹별 한 행).
 *
 * DB-only — 메모리 상태 없음. 페이즈는 HostSimulator.Phase 로 통일.
 * PK 는 UUID(id), 비즈니스 키는 eq_group_id (UNIQUE).
 */
@Service
@RequiredArgsConstructor
public class SimulatorStateService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorStateService.class);

    private final HostSimulator host;
    private final EcsPlcSimulator plc;

    /**
     * 부팅 복원 — DB 의 running 플래그/페이즈를 그대로 사용해 host/plc 재가동.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void applyOnBoot() {
        if (!SimulatorConfig.ENABLED) {
            logger.info("[ Sim ][ Boot ] disabled - skip DB restore");
            return;
        }
        try {
            List<TbWcsSimulatorState> all = loadAll();
            if (ValueUtil.isEmpty(all)) {
                logger.info("[ Sim ][ Boot ] no group state");
                return;
            }

            // 그룹별 host/plc 재가동
            int hostOn = 0, plcOn = 0, portOn = 0;
            for (TbWcsSimulatorState s : all) {
                String groupId = s.getEqGroupId();
                if (ValueUtil.isEmpty(groupId)) continue;

                if (Boolean.TRUE.equals(s.getHostRunning())) {
                    host.startKeepingPhase(groupId);
                    hostOn++;
                }
                if (Boolean.TRUE.equals(s.getPlcRunning())) {
                    plc.start(groupId);
                    plcOn++;
                }

                logger.info("[ Sim ][ Boot ] restored - eqGroup={}, host={}, plc={}, port={}, phase={}, startedAt={}",
                        groupId,
                        Boolean.TRUE.equals(s.getHostRunning()),
                        Boolean.TRUE.equals(s.getPlcRunning()),
                        Boolean.TRUE.equals(s.getPortModeRunning()),
                        s.getCurrentPhase(),
                        s.getPhaseStartedAt());
            }
            logger.info("[ Sim ][ Boot ] restore done - groups={}, host={}, plc={}, port={}",
                    all.size(), hostOn, plcOn, portOn);
        } catch (Exception e) {
            logger.error("[ Sim ][ Boot ] applyOnBoot failed", e);
        }
    }

    /**
     * 모든 시뮬 상태 행.
     */
    public List<TbWcsSimulatorState> loadAll() {
        return this.queryManager.selectList(TbWcsSimulatorState.class, ValueUtil.newMap(""));
    }

    /**
     * host_running=true 인 그룹 id 목록.
     */
    @SuppressWarnings("rawtypes")
    public List<String> findActiveHostGroups() {
        String sql = "SELECT eq_group_id FROM tb_wcs_simulator_state WHERE host_running = TRUE";
        return selectIds(sql);
    }

    /**
     * plc_running=true 인 그룹 id 목록.
     */
    @SuppressWarnings("rawtypes")
    public List<String> findActivePlcGroups() {
        String sql = "SELECT eq_group_id FROM tb_wcs_simulator_state WHERE plc_running = TRUE";
        return selectIds(sql);
    }

    /**
     * port_mode_running=true 인 그룹 id 목록.
     */
    @SuppressWarnings("rawtypes")
    public List<String> findActivePortModeGroups() {
        String sql = "SELECT eq_group_id FROM tb_wcs_simulator_state WHERE port_mode_running = TRUE";
        return selectIds(sql);
    }

    /**
     * id 컬럼만 추출하는 헬퍼.
     */
    @SuppressWarnings("rawtypes")
    private List<String> selectIds(String sql) {
        List<Map> rows = queryManager.selectListBySql(sql, ValueUtil.newMap(""), Map.class, 0, 1000);
        return rows.stream().map(r -> (String) r.get("eq_group_id")).toList();
    }

    /**
     * eq_group_id 로 row 조회 (없으면 null).
     */
    public TbWcsSimulatorState findByEqGroupId(String eqGroupId) {
        return this.queryManager.selectByCondition(TbWcsSimulatorState.class, ValueUtil.newMap("eqGroupId", eqGroupId));
    }

    /**
     * 그룹의 row 조회 후 없으면 생성.
     * 동시성 보호: insert 가 UNIQUE 위반으로 실패하면 한 번 더 조회.
     */
    public TbWcsSimulatorState loadOrCreate(String eqGroupId) {
        TbWcsSimulatorState s = findByEqGroupId(eqGroupId);
        if (ValueUtil.isNotEmpty(s)) return s;

        try {
            s = defaultState(eqGroupId);
            this.queryManager.insert(s);
            return s;
        } catch (Exception e) {
            // 동시 insert 충돌 가능 — 다시 조회
            TbWcsSimulatorState retry = findByEqGroupId(eqGroupId);
            if (ValueUtil.isNotEmpty(retry)) return retry;
            logger.error("[ Sim ][ State ] loadOrCreate failed - eqGroupId={}", eqGroupId, e);
            throw e;
        }
    }

    public void saveHostRunning(String eqGroupId, boolean running) {
        TbWcsSimulatorState s = loadOrCreate(eqGroupId);
        s.setHostRunning(running);
        this.queryManager.update(s);
    }

    public void savePlcRunning(String eqGroupId, boolean running) {
        TbWcsSimulatorState s = loadOrCreate(eqGroupId);
        s.setPlcRunning(running);
        this.queryManager.update(s);
    }

    public void savePortModeRunning(String eqGroupId, boolean running) {
        TbWcsSimulatorState s = loadOrCreate(eqGroupId);
        s.setPortModeRunning(running);
        this.queryManager.update(s);
    }

    public Boolean isSimulatorRunByEqGroupId(String eqGroupId) {
        TbWcsSimulatorState s = findByEqGroupId(eqGroupId);
        return s.getRun();
    }

    /**
     * 페이즈 저장 — HostSimulator.Phase 받음.
     */
    public void savePhase(String eqGroupId, HostSimulator.Phase phase, Date startedAt) {
        TbWcsSimulatorState s = loadOrCreate(eqGroupId);
        s.setCurrentPhase(phase.name());
        s.setPhaseStartedAt(startedAt);
        this.queryManager.update(s);
    }

    /**
     * 초기 row 빌더 — INBOUND 페이즈 + 글로벌 ratio 디폴트.
     */
    private TbWcsSimulatorState defaultState(String eqGroupId) {
        TbWcsSimulatorState s = new TbWcsSimulatorState();
        s.setEqGroupId(eqGroupId);

        // running 플래그 (NOT NULL)
        s.setHostRunning(false);
        s.setPlcRunning(false);
        s.setPortModeRunning(false);

        // 페이즈 정보
        s.setCurrentPhase(HostSimulator.Phase.INBOUND.name());
        s.setPhaseStartedAt(new Date());

        // 페이즈 타깃 — Integer(nullable) 이지만 운영상 디폴트
        s.setTargetInbound(SimulatorConfig.RATIO_INBOUND);
        s.setTargetOutbound(SimulatorConfig.RATIO_OUTBOUND);
        s.setTargetMove(0);

        return s;
    }
}
