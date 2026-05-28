package operato.logis.ecs.base.ecs.plc.crane;

import lombok.Getter;

import java.util.List;

@Getter
public class StackerCraneReadMap {

    private int craneFloor;
    private int firstDeviceCode;
    private List<Integer> wordValues;
    private int craneMode;
    private int craneWorkingStatus;
    private int craneRunStatus;
    private int craneWorkId;
    private int craneInterLock;
    private int craneChargeStatus;
    private int craneLocation;
    private int craneLocation2;
    private int craneCompleteFlag;
    private int craneCompleteWorkId;
    private int craneErrorCode;
    private int craneCargoStatus;
    private int craneCenserStatus;
    private int craneCellLocation;

    public StackerCraneReadMap() { }

    public void setReadValues(int firstDeviceCode, List<Integer> wordValues) {
        this.firstDeviceCode = firstDeviceCode;
        this.wordValues = wordValues;
        convertReadValues();
    }

    private void convertReadValues() {
        // todo: read 메모리 설정
    }
}