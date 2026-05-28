package operato.logis.ecs.base.ecs.plc.crane;

import operato.logis.ecs.base.ecs.command.crane.StackerCraneCommandResult;
import operato.logis.ecs.base.ecs.command.crane.StackerCraneCommonCommand;
import operato.logis.ecs.base.ecs.command.crane.StackerCraneLoadAndUnloadCommand;
import operato.logis.ecs.base.ecs.command.crane.StackerCraneMoveCommand;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneStatus;
import operato.logis.ecs.base.ecs.domain.crane.StackerCrane;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;

public interface IStackerCranePort {

    StackerCraneCommandResult move(StackerCraneMoveCommand command);

    void home(StackerCraneCommonCommand command);

    void inbound(StackerCraneLoadAndUnloadCommand command);

    void outbound(StackerCraneLoadAndUnloadCommand command);

    StackerCraneStatus getStatus(String stackerCraneId);

    StackerCraneCommandResult clear(StackerCraneCommonCommand command);

    StackerCrane isAvailable(String stackerCraneId);

    /* todo: 임시추가. 검토 후 삭제 */
    void sendPlcCommand(String craneId, int[] command);

    /* todo: 임시추가. 검토 후 삭제 */
    StackerCranePlc getStackerCranePlc(String craneId);
}