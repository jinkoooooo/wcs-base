package operato.logis.ecs.base.ecs.plc.crane;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.ecs.base.ecs.command.crane.StackerCraneCommandResult;
import operato.logis.ecs.base.ecs.command.crane.StackerCraneCommonCommand;
import operato.logis.ecs.base.ecs.command.crane.StackerCraneLoadAndUnloadCommand;
import operato.logis.ecs.base.ecs.command.crane.StackerCraneMoveCommand;
import operato.logis.ecs.base.ecs.domain.cell.CraneCell;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneStatus;
import operato.logis.ecs.base.ecs.domain.crane.StackerCrane;
import operato.logis.ecs.base.ecs.domain.enums.StackerCraneWriteConsts;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;
import operato.logis.ecs.base.ecs.service.crane.StackerCranePlcWriteService;
import operato.logis.ecs.base.ecs.service.path.StackerCranePathService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 스태커크레인 PLC adapter
 * - Write Map 생성
 * - PLC Write
 * - PLC Read
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StackerCranePortAdapter implements IStackerCranePort {

    private final ObjectMapper mapper;
    private final StackerCranePathService scPathService;
    private final StackerCranePlcManager scPlcManager;
    private final StackerCranePlcWriteService scPlcWriteService;

    @Override
    public StackerCraneCommandResult move(StackerCraneMoveCommand command) {

        try {
            StackerCranePlc scPlc = scPlcManager.getEquipment(command.getStackerCraneId());
            if (scPlc == null) {
                return new StackerCraneCommandResult(false, "StackerCrane Not Found", null);
            }
            StackerCrane sc = scPlc.getCrane();
            if (sc == null) {
                return new StackerCraneCommandResult(false, "StackerCrane Not Found", null);
            }

            List<CraneCell> pathCells = scPathService.findPath(command.getFromCell(), command.getToCell());
            if (pathCells == null || pathCells.isEmpty()) {
                return new StackerCraneCommandResult(false, "Path Not Found", null);
            }

            boolean canUsePath = scPathService.canUseCellPathByCellStatus(pathCells, sc);
            if (!canUsePath) {
                return new StackerCraneCommandResult(false, "Path Blocked", null);
            }

            if (sc.getCranePathCmdList() == null || sc.getCranePathCmdList().isEmpty()) {
                List<StackerCraneWriteMap> pathCommands = scPathService.buildMovePath(pathCells);
                if (pathCommands == null || pathCommands.isEmpty()) {
                    return new StackerCraneCommandResult(false, "Command generate fail", null);
                }
                sc.addPrePixCmd(pathCommands);
            }

            StackerCraneWriteMap currentCommand = sc.getCranePathCmdList().get(sc.getReserveCmdCurrentIndex());

            int[] plcCommand = currentCommand.getMoveAndClearCommand(command.getWorkId());
            write(scPlc.getId(), plcCommand);
            sc.setReserveCmdCurrentIndex(sc.getReserveCmdCurrentIndex() + 1);
            return new StackerCraneCommandResult(true, null, pathCells);

        } catch (Exception e) {
            log.error("[move] StackerCrane command send fail");
            return new StackerCraneCommandResult(false, "PLC Error", null);
        }
    }

    @Override
    public void home(StackerCraneCommonCommand command) { }

    @Override
    public void inbound(StackerCraneLoadAndUnloadCommand command) { }

    @Override
    public void outbound(StackerCraneLoadAndUnloadCommand command) { }

    /** PLC 상태 조회 */
    @Override
    public StackerCraneStatus getStatus(String stackerCraneId) { return null; }

    /** 작업 clear */
    @Override
    public StackerCraneCommandResult clear(StackerCraneCommonCommand command) {
        StackerCranePlc scPlc = scPlcManager.getEquipment(command.getStackerCraneId());
        if (scPlc == null) {
            return new StackerCraneCommandResult(false, "StackerCrane Not Found", null);
        }
        StackerCrane sc = scPlc.getCrane();
        if (sc == null) {
            return new StackerCraneCommandResult(false, "StackerCrane Not Found", null);
        }

        int[] plcCommand = scPlc.getWriteMap().getCompleteClearCommand();
        write(scPlc.getId(), plcCommand);
        sc.initPathCmdList();
        return new StackerCraneCommandResult(true, null, null);
    }

    /** todo: return type 확정 / boolean or StackerCrane*/
    @Override
    public StackerCrane isAvailable(String stackerCraneId) {
        StackerCranePlc scPlc = scPlcManager.getEquipment(stackerCraneId);
        if (scPlc == null) {
            return null;
        }
        StackerCrane sc = scPlc.getCrane();
        if (sc == null) {
            return null;
        }

        return sc;
    }

    /** todo: 임시 추가. 검토 후 삭제 */
    @Override
    public void sendPlcCommand(String craneId, int[] command) {
        write(craneId, command);
    }

    /** todo: 임시 추가. 검토 후 삭제 */
    @Override
    public StackerCranePlc getStackerCranePlc(String craneId) {
        StackerCranePlc scPlc = scPlcManager.getEquipment(craneId);
        if (scPlc == null) {
            return null;
        }
        return scPlc;
    }

    /** 검토완료) 설비 관리 - 스태커 크레인 PLC 명령 송신 */
    private void write(String craneId, int[] command) {
        log.info("[PLC write] stackerCrane PlcCommand : " + craneId);
        try {
            log.info(this.mapper.writeValueAsString(command));
        } catch (Exception e) {
            log.error(e.toString());
        }

        scPlcWriteService.sendCommandCrane(craneId, MelsecConsts.DeviceCode.R, StackerCraneWriteConsts.StackerCraneWriteAddress.WORK_ID.getAddress(), command);
    }
}
