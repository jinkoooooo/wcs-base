package operato.logis.ecs.tspg4way.dashboard.generator.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RackCellSyncRequest {
    private String eqGroupId;
    private String rackEqId;
    private List<CellOp> ops;
}
