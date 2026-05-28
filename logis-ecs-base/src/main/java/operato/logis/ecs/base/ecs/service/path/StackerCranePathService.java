package operato.logis.ecs.base.ecs.service.path;

import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.domain.cell.CraneCell;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneContext;
import operato.logis.ecs.base.ecs.domain.crane.StackerCrane;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.TbEqCvMst;
import operato.logis.ecs.base.ecs.entity.TbEqRackMst;
import operato.logis.ecs.base.ecs.plc.crane.StackerCraneWriteMap;
import operato.logis.ecs.base.ecs.service.common.TbEcsRackOrderService;
import operato.logis.ecs.base.ecs.service.common.TbEqCvMstService;
import operato.logis.ecs.base.ecs.service.common.TbEqRackMstService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * todo: map 구조 변경 (row, bay, level -> asiel, bay, level, side)
 */
@Service
@Slf4j
public class StackerCranePathService {

    private boolean arrivedTarget = false;
    public CraneCell[][][] map;

    private final TbEcsRackOrderService tbEcsRackOrderService;
    private final TbEqCvMstService tbEqCvMstService;
    private final TbEqRackMstService tbEqRackMstService;

    public StackerCranePathService(TbEcsRackOrderService tbEcsRackOrderService, TbEqCvMstService tbEqCvMstService, TbEqRackMstService tbEqRackMstService) {
        this.tbEcsRackOrderService = tbEcsRackOrderService;
        this.tbEqCvMstService = tbEqCvMstService;
        this.tbEqRackMstService = tbEqRackMstService;
    }

    public List<StackerCraneWriteMap> createPath(CraneCell from, CraneCell to) {
        List<StackerCraneWriteMap> commands = new ArrayList<>();
        StackerCraneWriteMap cmd = new StackerCraneWriteMap();

        cmd.setFromCraneCell(from);
        cmd.setToCraneCell(to);
        cmd.setPath(List.of(from, to));

        commands.add(cmd);

        return commands;
    }

    public void updateEnabledCell(int asiel, int bay, int level, boolean enabled) {
        map[asiel][bay][level].setEnabled(enabled);
    }

    public void updateCargoCell(int asiel, int bay, int level, boolean hasCargo) {
        map[asiel][bay][level].setHasCargo(hasCargo);
    }

    public boolean isTarget(int sBay, int sLevel, int tBay, int tLevel) {
        arrivedTarget = (sBay == tBay && sLevel == tLevel);
        return arrivedTarget;
    }

    public boolean getArrivedTarget() {
        return arrivedTarget;
    }

    public CraneCell[][][] getMap() {
        return map;
    }

    /*
     * 경로탐색
     * - todo: from, to 지정이 아닌 경로 생성 필요 시 로직 추가 구현
     */
    public List<CraneCell> findPath(CraneCell fromCell, CraneCell toCell) {
        return List.of(fromCell, toCell);
    }

    /*
     * 경로에 따른 PLC 지시 커맨드 생성
     * - todo: from, to 지정이 아닌 경로 생성 필요 시 로직 추가 구현
     */
    public List<StackerCraneWriteMap> buildMovePath(List<CraneCell> paths) {
        List<StackerCraneWriteMap> commands = new ArrayList<>();

        if (paths == null || paths.size() < 2) {
            return commands;
        }

        List<CraneCell> curPath = new ArrayList<>();
        for (int i = 1; i < paths.size(); i++) {
            CraneCell prev = paths.get(i - 1);
            CraneCell cur = paths.get(i);

            commands.add(new StackerCraneWriteMap(prev, cur, List.of(prev, cur)));
        }
        return commands;
    }

    /*
     * 검토중) 랙 관리 - 경로 주행 가능한지 확인
     * 1. 랙 상태 조회
     * 2. 존재하지 않은 랙은 주행 불가
     * 3. 로드지시는 랙이 대기상태(READY) 또는 화물적재상태인(CARGO) 경우
     * 셀의 화물 유무와 예약 여부를 확인
     */
    public boolean canUseCellPathByCellStatus(StackerCraneContext context, List<CraneCell> paths, boolean isLoadMove, StackerCrane crane) {
        log.info(crane.getStackerCraneId() + " canUseCellPathByCellStatus");

        boolean canUseCellPath = false;

        Set<String> pathSet = paths.stream().map(cell -> cell.getAsiel() + "|" + cell.getBay() + "|" + cell.getLevel()).collect(Collectors.toSet());
        List<TbEqRackMst> cells = tbEqRackMstService.selectCellStatus(context); // todo: 랙단 컨베이어 확인

        List<TbEqRackMst> matched = cells.stream().filter(rack -> pathSet.contains(rack.getAsiel() + "|" + rack.getBay() + "|" + rack.getLevel())).collect(Collectors.toList());
        if (matched == null)
            return canUseCellPath;

        if (isLoadMove) {
            // 로드인경우
            log.info("canUseCellPathByCellStatus isLoadMove");

            canUseCellPath = !matched.isEmpty()
                    && matched.stream().allMatch(cell ->
                    cell.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue()
                            || cell.getStatus() == EcsDBConsts.EqRackStatus.CARGO.getValue());
        } else {
            // 언로드인경우
            log.info("canUseCellPathByCellStatus isNotLoadMove");

            // 출고포트인지 조회 / todo: 출고대 랙인지 확인하는 로직으로 간소화 가능 여부 확인
            List<TbEqCvMst> cvMstList = tbEqCvMstService.selectRackInCvStatus(context); // todo: 랙단 컨베이어 확인
            TbEqCvMst rackInCvMst = new TbEqCvMst();
            boolean isOutboundPort = false;
            int i = 0;
            for (CraneCell p : paths) {
                if (i == 0) {
                    i += 1;
                    continue;
                }

                rackInCvMst = cvMstList.stream().filter(cv ->
                        (cv.getAsiel() == context.getAsiel1() || cv.getAsiel() == context.getAsiel2()) // todo: 로직 재 확인
                                && cv.getRackLevel() == p.getLevel()
                                && cv.getRackBay() == p.getBay()
                ).findAny().orElse(null);
                if (rackInCvMst != null) {
                    isOutboundPort = true;
                    break;
                }
            }

            // todo: if-else 구분 필요여부를 모르겠음
            if (isOutboundPort) {
                // 출고대인 경우, 출고대에 화물이 없어야 함
                log.info("canUseCellPathByCellStatus isOutboundPort");
                canUseCellPath = !matched.isEmpty() && matched.stream().allMatch(rack ->
                        rack.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue()
                                && !rack.isCargoYn())
                        && !rackInCvMst.isCargoYn();
                if (!canUseCellPath) {
                    // TODO 출고포트에 화물이 있는경우. 상위 알림 또는 경로 초기화 등.
                }
            } else {
                // 출고대가 아닌 경우,
                log.info("canUseCellPathByCellStatus isOutboundPort else");
                canUseCellPath = !matched.isEmpty() && matched.stream().allMatch(rack ->
                        rack.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue()
                                && !rack.isCargoYn());
            }
        }
        log.info(crane.getStackerCraneId() + " canUseCellPathByCellStatus result: " + canUseCellPath);
        return canUseCellPath;
    }

    public boolean canUseCellPathByCellStatus(List<CraneCell> pathCells, StackerCrane sc) {
        //todo: 구현
        return true;
    }

    /*
     * if (needWaitZone(from, to)) {
     *     CraneLocation wait = createWaitLocation();
     *     result.add(createCommand(from, wait));
     *     result.add(createCommand(wait, to));
     * }
     */
}
