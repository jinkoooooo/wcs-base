package operato.logis.ecs.base.ecs.command.crane;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import operato.logis.ecs.base.ecs.domain.cell.CraneCell;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class StackerCraneCommandResult {

    private final boolean success;
    private final String description;
    private List<CraneCell> reservedPath;
}
