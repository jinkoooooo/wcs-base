package operato.logis.ecs.base.ecs.domain.conveyor;

import lombok.Data;

@Data
public class ConveyorStatus {

    private int conveyorStatus;
    private int sizeCheckErrorStatus;
    private int currentLiftConveyorLevel;
    private int conveyorError;

    public ConveyorStatus(int conveyorStatus, int sizeCheckErrorStatus, int currentLiftConveyorLevel, int conveyorError) {
        this.conveyorStatus = conveyorStatus;
        this.sizeCheckErrorStatus = sizeCheckErrorStatus;
        this.currentLiftConveyorLevel = currentLiftConveyorLevel;
        this.conveyorError = conveyorError;
    }
}
