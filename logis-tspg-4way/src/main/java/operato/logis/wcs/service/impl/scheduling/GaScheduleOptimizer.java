package operato.logis.wcs.service.impl.scheduling;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.LiftDto;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.Request;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.Response;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.ShuttleDto;
import operato.logis.wcs.service.impl.scheduling.GaSchedule.TaskDto;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * GA 기반 release 우선순위 산출기 v3.
 *
 * 입력: 그룹 내 release 후보 (OUTBOUND 부모 + 독립 MOVE) + REAL 셔틀 위치.
 * 출력: orderKey → 정규화 점수 + breakdown.
 * MultiShuttleSimulator 로 적합도 계산, ScoringEngine 으로 점수 breakdown 생성.
 */
@Service
@RequiredArgsConstructor
public class GaScheduleOptimizer {

    private static final int POPULATION_SIZE       = 30;
    private static final int MAX_GENERATIONS       = 50;
    private static final int ELITISM_COUNT         = 2;
    private static final int EARLY_STOP_STABLE_GEN = 10;
    private static final double FIXED_PM           = 0.05;
    private static final double K1 = 0.9;
    private static final double K2 = 0.6;
    private static final int AGING_CAP_CYCLES = 36;

    private final MultiShuttleSimulator simulator;

    /**
     * GA 진입점 — 초기 개체군 생성 후 세대 진화, 최선해 → orderKey 점수 매핑 반환.
     */
    public Response optimize(Request req) {
        if (ValueUtil.isEmpty(req) || ValueUtil.isEmpty(req.getCandidates())) {
            return emptyResponse();
        }

        List<TaskDto> tasks = req.getCandidates();
        List<ShuttleDto> shuttles = ValueUtil.isEmpty(req.getShuttles())
                ? Collections.emptyList() : req.getShuttles();
        Random rng = new Random(req.getSeed());
        int liftLevel = pickLiftLevel(req.getLifts());

        // 초기 개체군 — 셔플된 순열
        List<int[]> population = new ArrayList<>(POPULATION_SIZE);
        for (int i = 0; i < POPULATION_SIZE; i++) {
            int[] perm = identityPerm(tasks.size());
            shuffle(perm, rng);
            population.add(perm);
        }

        double[] fitness = new double[POPULATION_SIZE];
        double bestFitness = Double.NEGATIVE_INFINITY;
        int[] bestPerm = null;
        double bestMakespan = 0;
        int stableGen = 0;
        int convergedAt = 0;

        // 세대 진화
        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            // 적합도 평가 — makespan 의 역수
            double sum = 0;
            double fMax = Double.NEGATIVE_INFINITY;
            int bestIdx = 0;
            for (int i = 0; i < POPULATION_SIZE; i++) {
                double makespan = simulator.simulate(population.get(i), tasks, shuttles, liftLevel);
                fitness[i] = 1.0 / Math.max(makespan, 0.01);
                sum += fitness[i];
                if (fitness[i] > fMax) { fMax = fitness[i]; bestIdx = i; }
            }
            double fAvg = sum / POPULATION_SIZE;

            // 최선해 갱신 + early-stop 카운터
            if (fMax > bestFitness) {
                bestFitness = fMax;
                bestPerm = population.get(bestIdx).clone();
                bestMakespan = simulator.simulate(bestPerm, tasks, shuttles, liftLevel);
                stableGen = 0;
                convergedAt = gen;
            } else {
                stableGen++;
                if (stableGen >= EARLY_STOP_STABLE_GEN) break;
            }

            // 다음 세대 — 엘리트 보존 + 룰렛 + adaptive pc + 5% reverse mutation
            List<int[]> next = new ArrayList<>(POPULATION_SIZE);
            Integer[] order = new Integer[POPULATION_SIZE];
            for (int i = 0; i < POPULATION_SIZE; i++) order[i] = i;
            final double[] fitRef = fitness;
            Arrays.sort(order, (a, b) -> Double.compare(fitRef[b], fitRef[a]));
            for (int i = 0; i < ELITISM_COUNT && i < POPULATION_SIZE; i++) {
                next.add(population.get(order[i]).clone());
            }

            while (next.size() < POPULATION_SIZE) {
                int p1Idx = rouletteSelectIndex(fitness, sum, rng);
                int p2Idx = rouletteSelectIndex(fitness, sum, rng);
                int[] p1 = population.get(p1Idx);
                int[] p2 = population.get(p2Idx);

                // adaptive crossover rate
                double parentBestFit = Math.max(fitness[p1Idx], fitness[p2Idx]);
                double pc = parentBestFit > fAvg
                        ? K1 * (fMax - parentBestFit) / Math.max(fMax - fAvg, 0.001)
                        : K2;
                pc = Math.max(0.1, Math.min(pc, 0.95));

                int[] c1, c2;
                if (rng.nextDouble() < pc && p1.length >= 2) {
                    c1 = orderCrossover(p1, p2, rng);
                    c2 = orderCrossover(p2, p1, rng);
                } else {
                    c1 = p1.clone();
                    c2 = p2.clone();
                }

                if (rng.nextDouble() < FIXED_PM) reverseMutate(c1, rng);
                if (rng.nextDouble() < FIXED_PM) reverseMutate(c2, rng);

                next.add(c1);
                if (next.size() < POPULATION_SIZE) next.add(c2);
            }
            population = next;
        }

        // 최선해 순열 → orderKey 별 raw GA 점수 (0~100)
        Map<String, Double> rawGaScoreByKey = new HashMap<>();
        if (ValueUtil.isNotEmpty(bestPerm) && bestPerm.length > 0) {
            int n = bestPerm.length;
            for (int i = 0; i < n; i++) {
                String key = tasks.get(bestPerm[i]).getOrderKey();
                rawGaScoreByKey.put(key, ((double) (n - i) / n) * 100.0);
            }
        } else {
            // GA 실패 fallback — 입력 순서 그대로 점수 부여
            int n = tasks.size();
            for (int i = 0; i < n; i++) {
                rawGaScoreByKey.put(tasks.get(i).getOrderKey(),
                        ((double) (n - i) / n) * 100.0);
            }
        }

        // task 별 breakdown 생성 — OUTBOUND 부모는 bundle, 그 외는 move
        Map<String, Double> finalScoreByKey = new HashMap<>();
        Map<String, ScoreBreakdown> breakdownByKey = new HashMap<>();
        for (TaskDto t : tasks) {
            Double rawGa = rawGaScoreByKey.get(t.getOrderKey());
            ScoreBreakdown bd;
            if ("OUTBOUND".equals(t.getOrderType())) {
                bd = ScoringEngine.computeBundleScore(
                        t.getPriority(), t.getAgingCount(), AGING_CAP_CYCLES,
                        t.getChildMoveCount(), rawGa);
            } else {
                bd = ScoringEngine.computeMoveScore(t.getPriority(), rawGa);
            }
            breakdownByKey.put(t.getOrderKey(), bd);
            finalScoreByKey.put(t.getOrderKey(), ValueUtil.isNotEmpty(rawGa) ? rawGa : 0.0);
        }

        return Response.builder()
                .scoreByOrderKey(finalScoreByKey)
                .breakdownByOrderKey(breakdownByKey)
                .totalEstimatedTime(bestMakespan)
                .convergedGeneration(convergedAt)
                .usedShuttleCount(shuttles.size())
                .build();
    }

    /**
     * Order Crossover (OX). 부모 1의 [a,b] 구간 보존, 나머지는 부모 2 순서대로 채움.
     */
    private int[] orderCrossover(int[] p1, int[] p2, Random rng) {
        int n = p1.length;
        if (n < 2) return p1.clone();
        int[] child = new int[n];
        Arrays.fill(child, -1);
        int a = rng.nextInt(n), b = rng.nextInt(n);
        if (a > b) { int t = a; a = b; b = t; }
        Set<Integer> copied = new HashSet<>();
        for (int i = a; i <= b; i++) {
            child[i] = p1[i];
            copied.add(p1[i]);
        }
        int idx = (b + 1) % n;
        for (int i = 0; i < n; i++) {
            int v = p2[(b + 1 + i) % n];
            if (!copied.contains(v)) {
                child[idx] = v;
                idx = (idx + 1) % n;
            }
        }
        return child;
    }

    /**
     * Reverse(2-opt) mutation — 임의 구간 뒤집기.
     */
    private void reverseMutate(int[] perm, Random rng) {
        if (perm.length < 2) return;
        int a = rng.nextInt(perm.length), b = rng.nextInt(perm.length);
        if (a > b) { int t = a; a = b; b = t; }
        while (a < b) {
            int tmp = perm[a]; perm[a] = perm[b]; perm[b] = tmp;
            a++; b--;
        }
    }

    /**
     * 룰렛 선택. 합이 0 이하면 균등 무작위.
     */
    private int rouletteSelectIndex(double[] fitness, double sum, Random rng) {
        if (sum <= 0) return rng.nextInt(fitness.length);
        double r = rng.nextDouble() * sum;
        double acc = 0;
        for (int i = 0; i < fitness.length; i++) {
            acc += fitness[i];
            if (acc >= r) return i;
        }
        return fitness.length - 1;
    }

    /**
     * 0..n-1 identity 순열.
     */
    private int[] identityPerm(int n) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        return p;
    }

    /**
     * Fisher-Yates 셔플.
     */
    private void shuffle(int[] arr, Random rng) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = arr[i]; arr[i] = arr[j]; arr[j] = t;
        }
    }

    /**
     * 첫 번째 리프트의 현재 층 — 비면 1.
     */
    private int pickLiftLevel(List<LiftDto> lifts) {
        if (ValueUtil.isEmpty(lifts)) return 1;
        return Math.max(lifts.get(0).getCurrentLevel(), 1);
    }

    /**
     * 빈 응답 — candidates 가 없을 때.
     */
    private Response emptyResponse() {
        return Response.builder()
                .scoreByOrderKey(Collections.emptyMap())
                .breakdownByOrderKey(Collections.emptyMap())
                .totalEstimatedTime(0)
                .convergedGeneration(0)
                .usedShuttleCount(0)
                .build();
    }
}
