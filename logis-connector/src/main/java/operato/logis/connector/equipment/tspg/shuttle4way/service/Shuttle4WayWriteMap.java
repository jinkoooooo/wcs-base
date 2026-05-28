package operato.logis.connector.equipment.tspg.shuttle4way.service;


import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayWriteConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Cell;
import lombok.Data;

import java.util.List;

@Data
public class Shuttle4WayWriteMap {
    private List<Cell> path;
    private Cell fromCell;
    private Cell toCell;

    public Shuttle4WayWriteMap() {}

    public Shuttle4WayWriteMap(Cell fromCell, Cell toCell, List<Cell> curPath) {
        this.fromCell = fromCell;
        this.toCell = toCell;
        this.path = curPath;
    }


    @Override
    public String toString() {
        return "("+fromCell.getLocX()+","+fromCell.getLocY()+") >> ("
                + toCell.getLocX()+","+toCell.getLocY()+")";
    }

    public int[] getEmergencyCommand() {
        int emergencyOptionWord = 1 << Shuttle4WayWriteConsts.ShuttleEmergencyOption.EMO_STOP.getBitIndex();
        var command = new int[]{0, 0, emergencyOptionWord};
        return command;
    }

    public int[] getCompleteClearCommand() {
        int clearOptionWord = 1 << Shuttle4WayWriteConsts.ShuttleClearOption.CLEAR_COMP.getBitIndex();
        var command = new int[]{0, 0, 0, clearOptionWord};
        return command;
    }
    public int[] getErrorClearCommand() {
        int clearOptionWord = 1 << Shuttle4WayWriteConsts.ShuttleClearOption.CLEAR_ERROR.getBitIndex();
        var command = new int[]{0, 0, 0, clearOptionWord};
        return command;
    }

    public int[] getMoveAndClearCommand(int workId, int floor) {
        // TODO : 셀 형식 확인 필요.
        int cellId = 0;
        if(String.valueOf(toCell.getLocX()).length() == 1)
            cellId = Integer.parseInt(toCell.getLocY() + "0" + toCell.getLocX());
        else
            cellId = Integer.parseInt(toCell.getLocY() + "" + toCell.getLocX());
        int workTypeWord = 1 <<Shuttle4WayWriteConsts.ShuttleWorkType.MOVE.getBitIndex();
        int clearOptionWord = 1 << Shuttle4WayWriteConsts.ShuttleClearOption.CLEAR_COMP.getBitIndex();
        var command = new int[]{workId, workTypeWord, 0, clearOptionWord, cellId};
        return command;
    }
    public int[] getMoveCommand(int workId, int x, int y) {
        int cellId = 0;
        if(String.valueOf(x).length() == 1)
            cellId = Integer.parseInt(y + "0" + x);
        else
            cellId = Integer.parseInt(y + "" + x);
        int workTypeWord = 1 <<Shuttle4WayWriteConsts.ShuttleWorkType.MOVE.getBitIndex();
        int clearOptionWord = 1 << Shuttle4WayWriteConsts.ShuttleClearOption.CLEAR_COMP.getBitIndex();
        var command = new int[]{workId, workTypeWord, 0, clearOptionWord, cellId};
        return command;
    }

    public int[] getMoveAndFloorCommand(int workId, int x, int y, int floor) {
        int cellId = 0;
        if(String.valueOf(x).length() == 1)
            cellId = Integer.parseInt(y + "0" + x);
        else
            cellId = Integer.parseInt(y + "" + x);
        int workTypeWord = 1 <<Shuttle4WayWriteConsts.ShuttleWorkType.MOVE.getBitIndex();
        int clearOptionWord = 1 << Shuttle4WayWriteConsts.ShuttleClearOption.CLEAR_COMP.getBitIndex();
        var command = new int[]{workId, workTypeWord, 0, clearOptionWord, cellId, floor};
        return command;
    }

    public int[] getLoadAndClearCommand(int workId) {
        int workTypeWord = 1 << Shuttle4WayWriteConsts.ShuttleWorkType.LOAD.getBitIndex();
        int clearOptionWord = 1 << Shuttle4WayWriteConsts.ShuttleClearOption.CLEAR_COMP.getBitIndex();
        var command = new int[]{workId, workTypeWord, 0, clearOptionWord, 0};
        return command;
    }
    public int[] getUnLoadAndClearCommand(int workId) {
        int workTypeWord = 1 << Shuttle4WayWriteConsts.ShuttleWorkType.UNLOAD.getBitIndex();
        int clearOptionWord = 1 << Shuttle4WayWriteConsts.ShuttleClearOption.CLEAR_COMP.getBitIndex();
        var command = new int[]{workId, workTypeWord, 0, clearOptionWord, 0};
        return command;
    }



}
