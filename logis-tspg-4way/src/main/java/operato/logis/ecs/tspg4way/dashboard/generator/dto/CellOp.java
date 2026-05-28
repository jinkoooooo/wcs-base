package operato.logis.ecs.tspg4way.dashboard.generator.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CellOp {
    private String cellId;
    private int level;
    private int row;
    private int bay;
    private CellOpKind kind;
    private Integer rackType;
}
