package operato.logis.wcs.service.impl.scheduling;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.ShuttleDto;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.TaskDto;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

import java.util.List;

/**
 * 다중 셔틀 makespan 시뮬레이터 — GA 적합도 함수.
 *
 * 각 셔틀의 (row, bay, level, availableAt) 상태를 유지하며 태스크 순열을 따라
 * 가장 빠르게 끝낼 수 있는 셔틀에 배정. 리프트는 1대 공유 가정.
 */
@Component
@RequiredArgsConstructor
public class MultiShuttleSimulator {

    private final KinematicsCalculator kinematics;

    private static final double NO_SHUTTLE_PENALTY = 1e9;

    /**
     * 순열을 시뮬레이션해 전체 makespan(초) 반환. 셔틀이 없으면 패널티 값.
     */
    public double simulate(int[] perm, List<TaskDto> tasks,
                           List<ShuttleDto> shuttles, int liftLevel) {
        if (ValueUtil.isEmpty(perm) || perm.length == 0) return 0.0;
        if (ValueUtil.isEmpty(shuttles)) return NO_SHUTTLE_PENALTY;

        // 초기 상태 — (row, bay, level, availableAt)
        int n = shuttles.size();
        double[][] state = new double[n][4];
        for (int i = 0; i < n; i++) {
            ShuttleDto s = shuttles.get(i);
            state[i][0] = s.getCurrentRow();
            state[i][1] = s.getCurrentBay();
            state[i][2] = Math.max(s.getCurrentLevel(), 1);
            state[i][3] = 0.0;
        }

        // 순열 순회 — 가장 빨리 끝낼 셔틀에 배정
        double makespan = 0.0;
        for (int idx : perm) {
            TaskDto t = tasks.get(idx);

            int bestS = 0;
            double bestFinish = Double.MAX_VALUE;

            for (int s = 0; s < n; s++) {
                double travel = kinematics.estimateTaskTime(
                        (int) state[s][0], (int) state[s][1], (int) state[s][2],
                        t.getFromRow(), t.getFromBay(), t.getFromLevel(),
                        t.getToRow(), t.getToBay(), t.getToLevel(),
                        liftLevel);
                double finishAt = state[s][3] + travel;
                if (finishAt < bestFinish) {
                    bestFinish = finishAt;
                    bestS = s;
                }
            }

            // 선택된 셔틀 상태 갱신
            state[bestS][0] = t.getToRow();
            state[bestS][1] = t.getToBay();
            state[bestS][2] = Math.max(t.getToLevel(), 1);
            state[bestS][3] = bestFinish;

            if (bestFinish > makespan) makespan = bestFinish;
        }
        return makespan;
    }
}
