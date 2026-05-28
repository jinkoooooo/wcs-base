package operato.logis.ecs.base.ecs.command.crane;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import operato.logis.ecs.base.ecs.domain.cell.CraneCell;

import java.util.List;

/** 스태커 크레인 입고, 출고, 재고이동 명령어 */
@Getter
@Setter
@AllArgsConstructor
public class StackerCraneLoadAndUnloadCommand {

    private final String stackerCraneId;
    private final int workId;
    private final CraneCell fromCraneCell;
    private final CraneCell toCraneCell;
    private final List<CraneCell> path;
}
