package operato.logis.wcs.rest.system;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.scheduling.GaPlanCache;
import operato.logis.wcs.service.impl.scheduling.ScoreBreakdown;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GA 산출 plan 조회 REST.
 * 운영자가 release 정렬 결과를 즉시 확인하여 GA 영향력·정렬 디버깅에 활용.
 * 모든 데이터는 캐시 snapshot 으로 read-only.
 */
@RestController
@RequiredArgsConstructor
public class GaPlanInspectController {

    private final GaPlanCache cache;

    /**
     * 그룹의 현재 GA plan 의 정렬 점수 맵을 반환한다.
     * 점수가 높을수록 release 우선.
     */
    @GetMapping("/rest/wcs/ga-plan/{eqGroupId}")
    public ResponseEntity<Map<String, Object>> getPlan(@PathVariable("eqGroupId") String eqGroupId) {
        Map<String, Object> meta = cache.snapshotMeta(eqGroupId);
        Map<String, Double> scores = cache.snapshotScores(eqGroupId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eqGroupId", eqGroupId);
        body.put("meta", meta);
        body.put("scores", round2(scores));
        return ResponseEntity.ok(body);
    }

    /**
     * 그룹의 현재 GA plan 의 점수 구성 요소(설명 가능한 breakdown)를 반환한다.
     * 각 orderKey 별로 priority/aging/efficiency/ga 의 정규화 값과 formula 명을 노출.
     */
    @GetMapping("/rest/wcs/ga-plan/{eqGroupId}/explain")
    public ResponseEntity<Map<String, Object>> getPlanExplain(@PathVariable("eqGroupId") String eqGroupId) {
        Map<String, Object> meta = cache.snapshotMeta(eqGroupId);
        Map<String, ScoreBreakdown> breakdowns = cache.snapshotBreakdowns(eqGroupId);

        Map<String, Object> explained = new LinkedHashMap<>();
        breakdowns.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().getTotal(), a.getValue().getTotal()))
                .forEach(e -> explained.put(e.getKey(), e.getValue().toMap()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eqGroupId", eqGroupId);
        body.put("meta", meta);
        body.put("breakdowns", explained);
        return ResponseEntity.ok(body);
    }

    private static Map<String, Double> round2(Map<String, Double> in) {
        return in.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Math.round(e.getValue() * 100.0) / 100.0,
                        (x, y) -> x,
                        LinkedHashMap::new));
    }
}
