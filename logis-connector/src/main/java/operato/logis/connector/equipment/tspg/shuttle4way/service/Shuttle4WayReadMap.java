package operato.logis.connector.equipment.tspg.shuttle4way.service;

import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayReadConsts;
import lombok.Getter;

import java.util.List;

@Getter
public class Shuttle4WayReadMap {
    private int shuttleFloor;
    private int firstDeviceCode;
    private List<Integer> wordValues;
    private int shuttleMode;
    private int shuttleWorkingStatus;
    private int shuttleRunStatus;
    private int shuttleWorkId;
    private int shuttleInterLock;
    private int shuttleChargeStatus;
    private int shuttleLocation;
    private int shuttleLocation2;
    private int shuttleCompleteFlag;
    private int shuttleCompleteWorkId;
    private int shuttlErrorCode;
    private int shuttleCargoStatus;
    private int shuttleCenserStatus;
    private int shuttleCellLocation;

    public Shuttle4WayReadMap() {

    }

    public void setReadValues(int firstDeviceCode, List<Integer> wordValues) {
        this.firstDeviceCode = firstDeviceCode;
        this.wordValues = wordValues;
        convertReadValues();
    }

    private void convertReadValues(){
        this.shuttleMode = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.MODE.getAddress() - firstDeviceCode);
        this.shuttleWorkingStatus = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.WORKING_STATUS.getAddress() - firstDeviceCode);
        this.shuttleRunStatus = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.RUN_STATUS.getAddress() - firstDeviceCode);
        // this.shuttleWorkStep = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.WORK_STEP.getAddress() - firstDeviceCode);
        this.shuttleWorkId = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.WORK_ID.getAddress() - firstDeviceCode);
        this.shuttleInterLock = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.INTER_LOCK.getAddress() - firstDeviceCode);
        this.shuttleChargeStatus = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.CHARGE_STATUS.getAddress() - firstDeviceCode);
        // this.shuttleFrontDirection = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.FRONT_DIRECTION.getAddress() - firstDeviceCode);
        // this.shuttleWorkDirection = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.WORK_DIRECTION.getAddress() - firstDeviceCode);
        // this.shuttleDogCount = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.DOG_COUNT.getAddress() - firstDeviceCode);
        // this.shuttleSpeedMode = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.SPEED_MODE.getAddress() - firstDeviceCode);
        this.shuttleLocation = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.LOCATION.getAddress() - firstDeviceCode);
        this.shuttleLocation2 = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.LOCATION2.getAddress() - firstDeviceCode);
        this.shuttleCompleteFlag = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.COMPLETE_FLAG.getAddress() - firstDeviceCode);
        this.shuttleCompleteWorkId = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.COMPLETE_WORK_ID.getAddress() - firstDeviceCode);
        this.shuttlErrorCode = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.BLOCK_CODE.getAddress() - firstDeviceCode);
        this.shuttleCargoStatus = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.CARGO_STATUS.getAddress() - firstDeviceCode);
        this.shuttleCenserStatus = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.CENSER_STATUS.getAddress() - firstDeviceCode);
        this.shuttleCellLocation = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.CELL_LOCATION.getAddress() - firstDeviceCode);
        this.shuttleFloor = wordValues.get(Shuttle4WayReadConsts.ShuttleReadAddress.FLOOR_INFO.getAddress() - firstDeviceCode);
    }
}
