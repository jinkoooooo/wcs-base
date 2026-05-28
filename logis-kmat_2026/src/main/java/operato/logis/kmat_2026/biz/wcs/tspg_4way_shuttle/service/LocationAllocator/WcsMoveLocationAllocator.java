package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.LocationAllocator;


import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ErrorEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.OrderTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.*;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.util.WcsLocationUtil;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;
import operato.logis.kmat_2026.service.impl.TbWcsLocMstService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 이동 출발지/목적지 로케이션 할당 서비스 (고도화 버전)
 */
@Service
public class WcsMoveLocationAllocator {

    private static final Logger logger = LoggerFactory.getLogger(WcsMoveLocationAllocator.class);

    @Autowired
    protected TbWcsLocMstService locMstService;

    public AllocationResult allocate(WcsOrderCommand command) {
        if (command == null) return AllocationResult.fail(ErrorEnumCode.INVALID_PARAMETER.codeAsString(), "Command is null");

        // 1. orderType 검증
        if (!OrderTypeEnumCode.MOVE.codeAsString().equalsIgnoreCase(command.getOrderType())) {
            return AllocationResult.fail(ErrorEnumCode.INVALID_PARAMETER.codeAsString(), "Order type is not MOVE");
        }

        String fromLocCode = trimToNull(command.getFromLocCode());
        String toLocCode = trimToNull(command.getToLocCode());

        // 출발지는 무조건 있어야 함 (재고 이동이니까)
        if (fromLocCode == null) {
            return AllocationResult.fail(ErrorEnumCode.INVALID_PARAMETER.codeAsString(), "fromLocCode is required for MOVE");
        }

        // 정책 1: from + to 둘 다 있는 경우 (검증만 수행)
        if (toLocCode != null) {
            return validateAndReturnDirectMove(command, fromLocCode, toLocCode);
        }

        // 정책 2: to 가 없는 경우 (최적의 목적지 계산)
        return allocateTargetOnly(command, fromLocCode);
    }

    /**
     * 목적지(toLocCode) 자동 할당 - 샌드위치 회피 및 Select Lock 적용
     */
    private AllocationResult allocateTargetOnly(WcsOrderCommand command, String fromLocCode) {
        String eqGroupId = command.getEqGroupId();

        // 1. 전체 구역 RACK 데이터 조회 (파라미터 정확히 적용)
        List<LocWithPosition> allRacks = locMstService.findMultipleWithPosition(eqGroupId, null, LocTypeEnumCode.RACK);

        // 2. 출발지 존재 확인
        boolean fromExists = allRacks.stream().anyMatch(l -> l.getLoc().getLocCode().equals(fromLocCode));
        if (!fromExists) {
            return AllocationResult.fail(ErrorEnumCode.INVALID_PARAMETER.codeAsString(), "Invalid fromLocCode: " + fromLocCode);
        }

        // 3. 지형 분석 (Sandwich Map 생성)
        Map<String, Boolean> sandwichMap = WcsLocationUtil.buildSandwichMap(allRacks);

        // 4. 이동 가능한 후보지(EMPTY & UNLOCKED) 추출 - 자기 자신 제외
        List<LocWithPosition> candidates = allRacks.stream()
                .filter(lwp -> (lwp.getLoc().getStatus() != null && lwp.getLoc().getStatus() == 0)
                        && lwp.getLoc().getLockYn() == 0
                        && !lwp.getLoc().getLocCode().equals(fromLocCode))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return AllocationResult.fail(ErrorEnumCode.NO_AVAILABLE_LOCATION.codeAsString(), "No available target RACK");
        }

        // 5. [전략] 최적 목적지 리스트 생성 (Tier 1: Edge 우선)
        List<String> orderedCodes = calculateOrderedCodes(candidates, sandwichMap);

        // 6. [핵심] 물리적 선점 (Select Lock)
        TbWcsLocMst lockedLoc = selectAndLockBestCandidate(eqGroupId, orderedCodes);

        if (lockedLoc == null) {
            return AllocationResult.fail(ErrorEnumCode.NO_AVAILABLE_LOCATION.codeAsString(), "Target selection failed due to concurrent lock");
        }

        logger.info("Move target allocated & locked. sourceOrderKey={}, from={}, to={}, Tier={}",
                command.getSourceOrderKey(), fromLocCode, lockedLoc.getLocCode(),
                Boolean.TRUE.equals(sandwichMap.get(lockedLoc.getLocCode())) ? "Sandwiched" : "Edge");

        return AllocationResult.success(fromLocCode, lockedLoc.getLocCode(), eqGroupId);
    }

    /**
     * 직접 지정된 from/to 검증 및 Select Lock 적용
     */
    private AllocationResult validateAndReturnDirectMove(WcsOrderCommand command, String fromLocCode, String toLocCode) {
        if (fromLocCode.equals(toLocCode)) {
            return AllocationResult.fail(ErrorEnumCode.INVALID_PARAMETER.codeAsString(), "From and To are same");
        }

        String eqGroupId = command.getEqGroupId();

        // 1. 전체 지형 정보 조회 (분석용)
        List<LocWithPosition> allRacks = locMstService.findMultipleWithPosition(eqGroupId, null, LocTypeEnumCode.RACK);

        // 2. 존재 여부 확인
        LocWithPosition fromPos = allRacks.stream().filter(l -> l.getLoc().getLocCode().equals(fromLocCode)).findFirst().orElse(null);
        LocWithPosition toPos = allRacks.stream().filter(l -> l.getLoc().getLocCode().equals(toLocCode)).findFirst().orElse(null);

        if (fromPos == null || toPos == null) {
            return AllocationResult.fail(ErrorEnumCode.INVALID_PARAMETER.codeAsString(), "Location not found in RACK master");
        }

        // 3. 목적지(toLocCode) 샌드위치 여부 검증
        Map<String, Boolean> sandwichMap = WcsLocationUtil.buildSandwichMap(allRacks);
        if (Boolean.TRUE.equals(sandwichMap.get(toLocCode))) {
            logger.warn("Manual Move failed: Target {} is in a sandwiched area.", toLocCode);
            return AllocationResult.fail(ErrorEnumCode.INVALID_PARAMETER.codeAsString(), "Target location is currently blocked by neighbors (Sandwiched).");
        }

        // 4. [핵심] 지정된 목적지 물리적 선점 시도
        TbWcsLocMst lockedLoc = selectAndLockBestCandidate(eqGroupId, List.of(toLocCode));
        if (lockedLoc == null) {
            return AllocationResult.fail(ErrorEnumCode.NO_AVAILABLE_LOCATION.codeAsString(), "Target location is already locked by another process.");
        }

        return AllocationResult.success(fromLocCode, lockedLoc.getLocCode(), eqGroupId);
    }

    /**
     * 지형 분석 결과를 반영하여 우선순위 코드 리스트 생성
     */
    private List<String> calculateOrderedCodes(List<LocWithPosition> candidates, Map<String, Boolean> sandwichMap) {
        List<LocWithPosition> tier1_edges = new ArrayList<>();
        List<LocWithPosition> tier2_sandwiches = new ArrayList<>();

        for (LocWithPosition cp : candidates) {
            if (Boolean.TRUE.equals(sandwichMap.get(cp.getLoc().getLocCode()))) tier2_sandwiches.add(cp);
            else tier1_edges.add(cp);
        }

        return Stream.concat(
                tier1_edges.stream().sorted(Comparator.comparingInt(a -> a.getLoc().getLocSeq() != null ? a.getLoc().getLocSeq() : 999)),
                tier2_sandwiches.stream().sorted(Comparator.comparingInt(a -> a.getLoc().getLocSeq() != null ? a.getLoc().getLocSeq() : 999))
        ).map(lwp -> lwp.getLoc().getLocCode()).collect(Collectors.toList());
    }

    /**
     * [교체 지점] DB 물리적 락 실행 브릿지 메서드
     */
    protected TbWcsLocMst selectAndLockBestCandidate(String eqGroupId, List<String> orderedCodes) {
        return locMstService.findAndLockBestOne(eqGroupId, orderedCodes);
    }

    private String trimToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
}