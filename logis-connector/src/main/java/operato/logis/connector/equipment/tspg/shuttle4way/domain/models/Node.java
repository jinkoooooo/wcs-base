package operato.logis.connector.equipment.tspg.shuttle4way.domain.models;

import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayWriteConsts;
import lombok.Data;

@Data
public class Node {
    private int x;
    private int y;
    private double costFromStart;
    private double costToGoal;
    private Shuttle4WayWriteConsts.ShuttleDirection direction;
    private Node parentNode;

    public double totalCost() {
        return costFromStart + costToGoal;
    }

}
