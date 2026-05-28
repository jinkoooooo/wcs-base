package operato.logis.ecs.base.ecs.domain.crane;

import lombok.AllArgsConstructor;
import lombok.Getter;
import operato.logis.ecs.base.ecs.domain.cell.CraneCell;

@Getter
@AllArgsConstructor
public class StackerCraneStatus {

    private final boolean isIdel;
    private final boolean isAlarm;
    private final boolean isAuto;
    private final CraneCell currentCraneCell;
    private final String currentCommand; // todo: 타입 재 확인
}
