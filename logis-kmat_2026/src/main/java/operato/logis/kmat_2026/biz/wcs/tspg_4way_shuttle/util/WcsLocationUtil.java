package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.util;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.LocWithPosition;
import java.util.*;

/**
 * [WCS 로케이션 최적화 유틸리티 - 최종형]
 * 샌드위치(ㅁ) 판별 시, 주행 라인(Drive-only) 인접 여부를 고려하여 실질적인 접근성을 산출합니다.
 */
public class WcsLocationUtil {

    /**
     * 구역 내 전체 랙을 분석하여 샌드위치(ㅁ) 지도를 생성합니다.
     */
    public static Map<String, Boolean> buildSandwichMap(List<LocWithPosition> allRacks) {
        if (allRacks == null || allRacks.isEmpty()) return Collections.emptyMap();

        // 1. 층별 지형 데이터 캐싱 (속도 최적화를 위해 Map 및 Set 사용)
        Map<Integer, Integer> minRow = new HashMap<>(), maxRow = new HashMap<>();
        Map<Integer, Integer> minBay = new HashMap<>(), maxBay = new HashMap<>();
        Set<String> driveLines = new HashSet<>(); // 주행 라인 좌표 셋 (R:B:L)

        for (LocWithPosition lwp : allRacks) {
            int r = lwp.getRow(), b = lwp.getBay(), lev = lwp.getLevel();
            String posKey = posKey(r, b, lev);

            // 주행 전용 라인(고속도로) 등록
            if (lwp.isDriveOnly()) {
                driveLines.add(posKey);
            }

            // 장애물(물건 있음 or 예약됨) 좌표 파악
            if (isObstacle(lwp)) {
                minRow.put(lev, Math.min(minRow.getOrDefault(lev, Integer.MAX_VALUE), r));
                maxRow.put(lev, Math.max(maxRow.getOrDefault(lev, Integer.MIN_VALUE), r));
                minBay.put(lev, Math.min(minBay.getOrDefault(lev, Integer.MAX_VALUE), b));
                maxBay.put(lev, Math.max(maxBay.getOrDefault(lev, Integer.MIN_VALUE), b));
            }
        }

        // 2. 각 로케이션별 접근성(샌드위치 여부) 판별
        Map<String, Boolean> sandwichMap = new HashMap<>();
        for (LocWithPosition lwp : allRacks) {
            int r = lwp.getRow(), b = lwp.getBay(), lev = lwp.getLevel();
            String locCode = lwp.getLoc().getLocCode();

            // 해당 층에 장애물이 없으면 샌드위치일 수 없음
            if (!minRow.containsKey(lev)) {
                sandwichMap.put(locCode, false);
                continue;
            }

            // [Step 1] 수치상 샌드위치 구역(ㅁ)인지 판별
            boolean rowSandwiched = r > minRow.get(lev) && r < maxRow.get(lev);
            boolean baySandwiched = b > minBay.get(lev) && b < maxBay.get(lev);
            boolean isSandwiched = rowSandwiched || baySandwiched;

            // [Step 2] 고도화 조건: 샌드위치 상태라도 동서남북 중 하나가 주행 라인이면 해제
            if (isSandwiched) {
                boolean hasHighwayAccess =
                        driveLines.contains(posKey(r - 1, b, lev)) || // 왼쪽이 주행로
                                driveLines.contains(posKey(r + 1, b, lev)) || // 오른쪽이 주행로
                                driveLines.contains(posKey(r, b - 1, lev)) || // 앞쪽이 주행로
                                driveLines.contains(posKey(r, b + 1, lev));   // 뒤쪽이 주행로

                if (hasHighwayAccess) {
                    isSandwiched = false; // 주행로와 접해 있으므로 탈출 가능!
                }
            }

            sandwichMap.put(locCode, isSandwiched);
        }

        return sandwichMap;
    }

    /**
     * 후보지 중 가장 우수한 티어의 로케이션을 반환합니다.
     */
    public static LocWithPosition selectOptimalLocation(List<LocWithPosition> candidates, List<LocWithPosition> allRacks) {
        if (candidates == null || candidates.isEmpty()) return null;

        Map<String, Boolean> sandwichMap = buildSandwichMap(allRacks);

        List<LocWithPosition> tier1 = new ArrayList<>(); // 명당 (가장자리 or 주행로 인접)
        List<LocWithPosition> tier2 = new ArrayList<>(); // 샌드위치 (ㅁ)

        for (LocWithPosition c : candidates) {
            if (Boolean.TRUE.equals(sandwichMap.get(c.getLoc().getLocCode()))) {
                tier2.add(c);
            } else {
                tier1.add(c);
            }
        }

        return !tier1.isEmpty() ? sortBySeq(tier1) : sortBySeq(tier2);
    }

    private static LocWithPosition sortBySeq(List<LocWithPosition> list) {
        return list.stream()
                .min(Comparator.comparingInt(a -> a.getLoc().getLocSeq() != null ? a.getLoc().getLocSeq() : 9999))
                .orElse(null);
    }

    private static boolean isObstacle(LocWithPosition lwp) {
        if (lwp == null || lwp.getLoc() == null || lwp.isDriveOnly()) return false;
        Integer status = lwp.getLoc().getStatus();
        // 주행 라인이 아닌 곳 중 물건이 있거나 선점(Lock)된 곳은 벽이다.
        return LocStatusEnumCode.OCCUPIED.code().equals(status) || lwp.getLoc().getLockYn() == 1;
    }

    private static String posKey(int r, int b, int l) {
        return r + ":" + b + ":" + l;
    }
}