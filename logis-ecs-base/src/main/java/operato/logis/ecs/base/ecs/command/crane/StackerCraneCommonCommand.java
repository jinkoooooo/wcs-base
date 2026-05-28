package operato.logis.ecs.base.ecs.command.crane;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** 스태커 크레인 공통 명령어 */
@Getter
@Setter
@AllArgsConstructor
public class StackerCraneCommonCommand {

    private final String stackerCraneId;
    private final Integer workId;
}
