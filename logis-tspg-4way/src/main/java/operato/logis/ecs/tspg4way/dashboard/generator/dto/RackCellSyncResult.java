package operato.logis.ecs.tspg4way.dashboard.generator.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RackCellSyncResult {
    private int disabled;
    private int enabled;
    private int created;
    private List<String> rejectedCellIds = new ArrayList<>();
    private List<String> rejectReasons = new ArrayList<>();
}
