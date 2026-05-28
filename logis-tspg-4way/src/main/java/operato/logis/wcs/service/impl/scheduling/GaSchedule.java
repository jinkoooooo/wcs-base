package operato.logis.wcs.service.impl.scheduling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * GA 스케줄링 모듈 DTO 컨테이너.
 */
public class GaSchedule {

    /**
     * GA 입력 요청 — candidates 순열에서 최적해 탐색.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String eqGroupId;
        private List<TaskDto> candidates;
        private List<ShuttleDto> shuttles;
        private List<LiftDto> lifts;
        /** 결정성 시드 — 같은 30s 윈도우 내 동일 결과 보장. */
        private long seed;
    }

    /**
     * GA 출력 — 정렬 점수 + breakdown + 메타.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        /** orderKey → 정규화 점수 (0~100, 정렬 직접 사용). */
        private Map<String, Double> scoreByOrderKey;
        /** orderKey → 점수 구성 요소 breakdown (REST 조회·디버깅용). */
        private Map<String, ScoreBreakdown> breakdownByOrderKey;
        /** 추정 makespan (초) — 모니터링용. */
        private double totalEstimatedTime;
        /** 최선해 발견 세대. */
        private int convergedGeneration;
        /** GA 가 사용한 셔틀 수 (관찰용). */
        private int usedShuttleCount;
    }

    /**
     * GA 입력용 태스크.
     * OUTBOUND 부모: orderKey = 부모 키, childMoveCount = 자식 MOVE 수.
     * 독립 MOVE: orderKey = 본인 키, childMoveCount = 0.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDto {
        private String orderKey;
        private String orderType;
        private int priority;
        private int level;
        private int agingCount;
        private int childMoveCount;
        private int fromRow;
        private int fromBay;
        private int fromLevel;
        private int toRow;
        private int toBay;
        private int toLevel;
    }

    /**
     * 셔틀 실시간 상태 — TbEqCarMst 의 PLC 동기화 필드 사용.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShuttleDto {
        private String id;
        private int currentRow;
        private int currentBay;
        private int currentLevel;
        /** 화물 적재 여부 — 시뮬레이터가 가중에 활용 가능. */
        private boolean cargoYn;
    }

    /**
     * 리프트 실시간 상태.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiftDto {
        private String id;
        private int currentLevel;
        private int aisleRow;
    }
}
