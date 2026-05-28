package operato.logis.ecs.base.ecs.domain.crane;

import lombok.AllArgsConstructor;
import lombok.Getter;
import operato.logis.ecs.base.ecs.entity.TbEcsRackOrder;
import operato.logis.ecs.base.ecs.plc.crane.StackerCraneWriteMap;

import java.util.List;

@Getter
@AllArgsConstructor
public class StackerCraneContext {

    // todo: rackEqId, cvEqId 다중 설비 관리
    private final String craneId;
    private final String craneStatus;// todo: 사용여부 확인
    private final List<StackerCraneWriteMap> path; // todo: 사용여부 확인
    private final TbEcsRackOrder order; // todo: 사용여부 확인

    private final String rackEqId;
    private final String cvEqId;
    private final int asiel1;
    private final int asiel2;
}