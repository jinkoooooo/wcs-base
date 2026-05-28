package operato.logis.ecs.base.ecs.plc.conveyor;

import operato.logis.ecs.base.ecs.domain.enums.ConveyorWriteConsts;

public class ConveyorWriteMap {

    private int workId;
    private ConveyorWriteConsts.ConveyorWorkType workType;
    private int fromId;
    private int toId;
    private int sizeCheckerValue;

    public ConveyorWriteMap() { }

    public ConveyorWriteMap(int workId, ConveyorWriteConsts.ConveyorWorkType workType, int fromId, int toId, int sizeCheckerValue) {
        this.workId = workId;
        this.workType = workType;
        this.fromId = fromId;
        this.toId = toId;
    }

    public int[] getCommand() {
        int workTypeWord = 1 << workType.getBitIndex();
        var command = new int[]{ workId, workTypeWord, fromId, toId, sizeCheckerValue };
        return command;
    }

    public int[] getInboundCommand(int workId, int fromCvId, int toCvId) {
        int workTypeWord = 1 << ConveyorWriteConsts.ConveyorWorkType.INBOUND.getBitIndex();
        var command = new int[]{workId, workTypeWord, fromCvId, toCvId};
        return command;
    }

    public int[] getOutboundCommand(int workId, int fromCvId, int toCvId) {
        int workTypeWord = 1 << ConveyorWriteConsts.ConveyorWorkType.OUTBOUND.getBitIndex();

        var command = new int[]{workId, workTypeWord, fromCvId, toCvId};
        return command;
    }

    public int[] getMoveCommand(int workId, int fromCvId, int toCvId) {
        int workTypeWord = 1 << ConveyorWriteConsts.ConveyorWorkType.MOVE.getBitIndex();
        var command = new int[]{workId, workTypeWord, fromCvId, toCvId};
        return command;
    }

    public int[] getCraneLiftCommand(int workId, int fromRackCvId, int toRackCvId) {
        int workTypeWord = 1 << ConveyorWriteConsts.ConveyorWorkType.INBOUND.getBitIndex();
        var command = new int[]{workId, workTypeWord, fromRackCvId, toRackCvId};
        return command;
    }

    public int[] getSizeCheckerCommand(int workId, int size) {
        var command = new int[]{workId, 0, 0, 0, size};
        return command;
    }
}
