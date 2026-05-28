package operato.logis.ecs.base.ecs.plc.crane;

import lombok.Data;
import operato.logis.ecs.base.ecs.domain.cell.CraneCell;
import operato.logis.ecs.base.ecs.domain.enums.StackerCraneWriteConsts;

import java.util.List;

@Data
public class StackerCraneWriteMap {

    private List<CraneCell> path;
    private CraneCell fromCraneCell;
    private CraneCell toCraneCell;

    private static final int COMMAND_BASE_ADDRESS = StackerCraneWriteConsts.StackerCraneWriteAddress.WORK_ID.getAddress();
    private static final int COMMAND_END_ADDRESS = StackerCraneWriteConsts.StackerCraneWriteAddress.ST3_STATION_WORK_ID.getAddress();

    public StackerCraneWriteMap() { }

    public StackerCraneWriteMap(CraneCell fromCraneCell, CraneCell toCraneCell, List<CraneCell> curPath) {
        this.fromCraneCell = fromCraneCell;
        this.toCraneCell = toCraneCell;
        this.path = curPath;
    }

    @Override
    public String toString() {
        return "(" + fromCraneCell.getStationNo() + "," + fromCraneCell.getAsiel() + "," + fromCraneCell.getBay() + "," + fromCraneCell.getLevel() + ") >> ("
                + toCraneCell.getStationNo() + "," + toCraneCell.getAsiel() + "," + toCraneCell.getBay() + "," + toCraneCell.getLevel() + ")";
    }

    // todo: moveCommand 생성 전, clear 비트 on

    /** 작업 지시 Command 생성 - 이동, 입고, 출고, Rack to Rack, 목적지 변경, Station to Station 명령 */
    public int[] getMoveCommand(
            Integer workId,
            StackerCraneWriteConsts.StackerCraneWorkType workType,
            CraneCell from,
            CraneCell to
    ) {
        int workTypeWord = 1 << workType.getBitIndex();

        int[] command = new int[COMMAND_END_ADDRESS - COMMAND_BASE_ADDRESS + 1];
        putWord(command, StackerCraneWriteConsts.StackerCraneWriteAddress.WORK_ID, workId);
        putWord(command, StackerCraneWriteConsts.StackerCraneWriteAddress.WORK_TYPE, workTypeWord);
        applyLocation(command, StackerCraneWriteConsts.StackerCraneWriteAddress.FROM_STATION.getAddress(), from);
        applyLocation(command, StackerCraneWriteConsts.StackerCraneWriteAddress.TO_STATION.getAddress(), to);
        return command;
    }

    public int[] getMoveAndClearCommand(int workId) {
        int workTypeWord = 1 << StackerCraneWriteConsts.StackerCraneWorkType.MOVE.getBitIndex();
        int clearOptionWord = 1 << StackerCraneWriteConsts.StackerCraneClearOption.CLEAR_FORK1_WORK.getBitIndex();

        int[] command = new int[COMMAND_END_ADDRESS - COMMAND_BASE_ADDRESS + 1];
        putWord(command, StackerCraneWriteConsts.StackerCraneWriteAddress.WORK_ID, workId);
        putWord(command, StackerCraneWriteConsts.StackerCraneWriteAddress.WORK_TYPE, workTypeWord);
        applyLocation(command, StackerCraneWriteConsts.StackerCraneWriteAddress.FROM_STATION.getAddress(), this.fromCraneCell);
        applyLocation(command, StackerCraneWriteConsts.StackerCraneWriteAddress.TO_STATION.getAddress(), this.toCraneCell);
        putWord(command, StackerCraneWriteConsts.StackerCraneWriteAddress.CLEAR_OPTION, clearOptionWord);
        return command;
    }

    public int[] getPutawayAndClearCommand(int workId) {
        int workTypeWord = 1 << StackerCraneWriteConsts.StackerCraneWorkType.PUTAWAY.getBitIndex();
        int clearOptionWord = 1 << StackerCraneWriteConsts.StackerCraneClearOption.CLEAR_FORK1_WORK.getBitIndex();

        int[] command = new int[COMMAND_END_ADDRESS - COMMAND_BASE_ADDRESS + 1];
        putWord(command, StackerCraneWriteConsts.StackerCraneWriteAddress.WORK_ID, workId);
        putWord(command, StackerCraneWriteConsts.StackerCraneWriteAddress.WORK_TYPE, workTypeWord);
        applyLocation(command, StackerCraneWriteConsts.StackerCraneWriteAddress.FROM_STATION.getAddress(), this.fromCraneCell);
        applyLocation(command, StackerCraneWriteConsts.StackerCraneWriteAddress.TO_STATION.getAddress(), this.toCraneCell);
        putWord(command, StackerCraneWriteConsts.StackerCraneWriteAddress.CLEAR_OPTION, clearOptionWord);
        return command;
    }

    // todo: step by step 방식. 로직 구현
    public int[] getUnloadAndClearCommand(int workId) {
        int[] command = new int[COMMAND_END_ADDRESS - COMMAND_BASE_ADDRESS + 1];
        return command;
    }

    /** 초기화 명령 Command 생성 - heartbeat, 홈복귀, 이상리셋, 전체 작업 삭제, Fork 별 작업 삭제, S/C Online 시작 */
    public int[] getCompleteClearCommand() {
        int[] command = new int[COMMAND_END_ADDRESS - COMMAND_BASE_ADDRESS + 1];

        int clearOptionWord = 1 << StackerCraneWriteConsts.StackerCraneClearOption.CLEAR_FORK1_WORK.getBitIndex();
        putWord(command, StackerCraneWriteConsts.StackerCraneWriteAddress.CLEAR_OPTION, clearOptionWord);

        return command;
    }

    /** 작업 위치 추가 */
    private void applyLocation(int[] command, int startAddress, CraneCell craneCell) {
        if (craneCell == null) {
            craneCell = CraneCell.empty();
        }

        int[] words = craneCell.toWordArray();
        int baseIndex = startAddress - COMMAND_BASE_ADDRESS;

        for (int i = 0; i < words.length; i++) {
            command[baseIndex + i] = words[i];
        }
    }

    private void putWord(
            int[] command,
            StackerCraneWriteConsts.StackerCraneWriteAddress address,
            Integer value
    ) {
        int baseIndex = address.getAddress() - COMMAND_BASE_ADDRESS;

        command[baseIndex] = defaultZero(value);
    }

    /** 유틸 - PLC 적용 가능하도록 null -> 0 변환 */
    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}