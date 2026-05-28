package operato.logis.connector.equipment.tspg.shuttle4way.service;

import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.ConveyorWriteConsts;

public class ConveyorWriteMap {

    public ConveyorWriteMap() {}


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

    public int[] getCarLiftCommand(int workId, int fromRackCvId, int toRackCvId) {
        int workTypeWord = 1 << ConveyorWriteConsts.ConveyorWorkType.INBOUND.getBitIndex();
        var command = new int[]{workId, workTypeWord, fromRackCvId, toRackCvId};
        return command;
    }

    public int[] getSizeCheckerCommand(int workId, int size) {
        var command = new int[]{workId, 0, 0, 0, size};
        return command;
    }
}
