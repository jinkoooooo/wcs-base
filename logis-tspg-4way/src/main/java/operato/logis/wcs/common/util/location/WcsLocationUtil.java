package operato.logis.wcs.common.util.location;

import operato.logis.inventory.dto.RelocationTaskDto;
import operato.logis.wcs.dto.LocWithPosition;
import operato.logis.wcs.entity.ExtTbInventoryLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xyz.elidom.util.ValueUtil;

/**
 * WCS 로케이션 최적화 유틸리티 — 샌드위치(ㅁ) 판별 시 주행 라인(Drive-only) 인접 여부를 고려해
 * 실질적인 접근성을 산출한다.
 */
public final class WcsLocationUtil {

    private WcsLocationUtil() {}

    /** 구역 내 전체 랙을 분석해 샌드위치 지도를 생성. */
    public static Map<String, Boolean> buildSandwichMap(List<LocWithPosition> allRacks) {
        if (ValueUtil.isEmpty(allRacks)) return Collections.emptyMap();

        Map<Integer, Integer> minRow = new HashMap<>(), maxRow = new HashMap<>();
        Map<Integer, Integer> minBay = new HashMap<>(), maxBay = new HashMap<>();
        Set<String> driveLines = new HashSet<>();

        for (LocWithPosition lwp : allRacks) {
            int r = lwp.getRow(), b = lwp.getBay(), lev = lwp.getLevel();
            String posKey = posKey(r, b, lev);

            if (lwp.isDriveOnly()) {
                driveLines.add(posKey);
            }

            if (isObstacle(lwp)) {
                minRow.put(lev, Math.min(minRow.getOrDefault(lev, Integer.MAX_VALUE), r));
                maxRow.put(lev, Math.max(maxRow.getOrDefault(lev, Integer.MIN_VALUE), r));
                minBay.put(lev, Math.min(minBay.getOrDefault(lev, Integer.MAX_VALUE), b));
                maxBay.put(lev, Math.max(maxBay.getOrDefault(lev, Integer.MIN_VALUE), b));
            }
        }

        Map<String, Boolean> sandwichMap = new HashMap<>();
        for (LocWithPosition lwp : allRacks) {
            int r = lwp.getRow(), b = lwp.getBay(), lev = lwp.getLevel();
            String locId = lwp.getLoc().getLocId();

            if (!minRow.containsKey(lev)) {
                sandwichMap.put(locId, false);
                continue;
            }

            boolean rowSandwiched = r > minRow.get(lev) && r < maxRow.get(lev);
            boolean baySandwiched = b > minBay.get(lev) && b < maxBay.get(lev);
            boolean isSandwiched = rowSandwiched || baySandwiched;

            if (isSandwiched) {
                boolean hasHighwayAccess =
                        driveLines.contains(posKey(r - 1, b, lev))
                                || driveLines.contains(posKey(r + 1, b, lev))
                                || driveLines.contains(posKey(r, b - 1, lev))
                                || driveLines.contains(posKey(r, b + 1, lev));
                if (hasHighwayAccess) {
                    isSandwiched = false;
                }
            }

            sandwichMap.put(locId, isSandwiched);
        }

        return sandwichMap;
    }

    /** 후보지 중 가장 우수한 티어의 로케이션을 반환. */
    public static LocWithPosition selectOptimalLocation(List<LocWithPosition> candidates, List<LocWithPosition> allRacks) {
        if (ValueUtil.isEmpty(candidates)) return null;

        Map<String, Boolean> sandwichMap = buildSandwichMap(allRacks);

        List<LocWithPosition> tier1 = new ArrayList<>();
        List<LocWithPosition> tier2 = new ArrayList<>();

        for (LocWithPosition c : candidates) {
            if (Boolean.TRUE.equals(sandwichMap.get(c.getLoc().getLocId()))) {
                tier2.add(c);
            } else {
                tier1.add(c);
            }
        }

        return ValueUtil.isNotEmpty(tier1) ? sortBySeq(tier1) : sortBySeq(tier2);
    }

    private static LocWithPosition sortBySeq(List<LocWithPosition> list) {
        return list.stream()
                .min(Comparator.comparing(a -> ValueUtil.isNotEmpty(a.getLoc().getLocId()) ? a.getLoc().getLocId() : "ZZZZ"))
                .orElse(null);
    }

    private static boolean isObstacle(LocWithPosition lwp) {
        if (ValueUtil.isEmpty(lwp) || ValueUtil.isEmpty(lwp.getLoc()) || lwp.isDriveOnly()) return false;
        return ValueUtil.isNotEmpty(lwp.getLoc().getStockId()) || ValueUtil.isNotEmpty(lwp.getLoc().getTaskId());
    }

    private static String posKey(int r, int b, int l) {
        return r + ":" + b + ":" + l;
    }

    public static List<String> getObstacles(ExtTbInventoryLocation lockedLoc) {
        return null;
    }

    public static List<RelocationTaskDto> getStrategicObstacles(ExtTbInventoryLocation lockedLoc) {
        return null;
    }
}
