package operato.logis.ecs.base.ecs.command.crane;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import operato.logis.ecs.base.ecs.domain.cell.CraneCell;

import java.util.List;

/** 스태커 크레인 본체 이동 시 명령어 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class StackerCraneMoveCommand {

    private final String stackerCraneId;
    private final int workId;
    private final CraneCell fromCell;
    private final CraneCell toCell;
    private final List<CraneCell> pathCells;
}
