package operato.logis.wcs.service.impl.allocation.location;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.LocStatus;
import operato.logis.wcs.consts.LocType;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.dto.AllocationResult;
import operato.logis.wcs.dto.LocWithPosition;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.service.impl.allocation.port.PortService;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static operato.logis.wcs.common.util.check.Validator.hasLocationKey;

/**
 * 로케이션 락/언락/격리(block)/Soft Reservation/입고 가용 조회 서비스.
 *
 * 락은 native UPDATE 한 줄 단위로 동작.
 * 이중 락은 데드락 방지를 위해 locId 사전순으로 시도.
 * 격리는 입출고 플래그를 false 로 닫고 taskId 에 추적 마커를 남긴다.
 */
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);

    // 포트 타입 — 언락 시 PortService 라우팅 대상
    private static final Set<String> PORT_LOC_TYPES = Set.of(
            LocType.INBOUND_PORT.code(),
            LocType.OUTBOUND_PORT.code(),
            LocType.IN_OUTBOUND_PORT.code());

    private final InventoryLocationRepository locationRepository;
    private final PortService portService;

    /**
     * 입고 가능한 RACK 목록 조회 (EMPTY 상태).
     */
    public List<LocWithPosition> findAvailableForInbound(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) return Collections.emptyList();
        List<ExtTbInventoryLocation> locs =
                locationRepository.findAvailableRacks(eqGroupId, LocType.RACK, LocStatus.EMPTY);
        return attachPositions(eqGroupId, locs);
    }

    /**
     * 오더 유형별 락 처리.
     * INBOUND 는 toLoc, OUTBOUND 는 fromLoc, MOVE 는 from+to 양쪽을 락한다.
     */
    public void lockForOrder(AllocationResult allocation, String orderType,
                             String orderKey, String hostOrderKey) {
        String from = allocation.getFromLocId();
        String to = allocation.getToLocId();
        String eqGroupId = allocation.getEqGroupId();

        // 오더 유형 파싱
        OrderType type = OrderType.from(orderType);
        if (ValueUtil.isEmpty(type)) {
            throw new ElidomRuntimeException("알 수 없는 주문 유형입니다: " + orderType);
        }

        // 오더 유형별 분기
        switch (type) {
            case INBOUND -> lock(eqGroupId, to, orderKey, hostOrderKey);
            case OUTBOUND -> lock(eqGroupId, from, orderKey, hostOrderKey);
            case MOVE -> lockBoth(eqGroupId, from, to, orderKey, hostOrderKey);
        }
    }

    /**
     * 오더 유형별 언락 처리.
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlockForOrder(AllocationResult allocation, String orderType) {
        String from = allocation.getFromLocId();
        String to = allocation.getToLocId();
        String eqGroupId = allocation.getEqGroupId();

        // 오더 유형 파싱 - 알 수 없는 값은 무시
        OrderType type = OrderType.from(orderType);
        if (ValueUtil.isEmpty(type)) return;

        // 오더 유형별 분기
        switch (type) {
            case MOVE -> unlockBoth(eqGroupId, from, to);
            case INBOUND -> unlock(eqGroupId, to);
            case OUTBOUND -> unlock(eqGroupId, from);
        }
    }

    /**
     * 단일 로케이션 락 - lockable 타입만 처리.
     * 빈 자리(task_id IS NULL)거나 자기 hostOrderKey 가 soft reserve 한 자리만 인계 가능.
     */
    @Transactional(rollbackFor = Exception.class)
    public void lock(String eqGroupId, String locId, String orderKey, String expectedHostKey) {

        // 입력값 검증
        if (!hasLocationKey(eqGroupId, locId)) {
            throw new ElidomRuntimeException("로케이션 락 실패: 파라미터가 누락되었습니다.");
        }

        // lockable 아닌 타입은 스킵
        ExtTbInventoryLocation loc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (ValueUtil.isNotEmpty(loc) && !LocType.isLockable(loc.getLocType())) return;

        // 락 시도 - 자기 host key 일 때만 인계
        boolean ok = locationRepository.lockOverride(eqGroupId, locId, orderKey, expectedHostKey);
        if (!ok) throw new ElidomRuntimeException("로케이션 락 실패");
    }

    /**
     * 단일 로케이션 언락.
     * 포트 타입이면 PortService 로 라우팅 (이벤트 발행 통합).
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlock(String eqGroupId, String locId) {

        // 입력값 검증
        if (!hasLocationKey(eqGroupId, locId)) return;

        ExtTbInventoryLocation loc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);

        // 포트 라우팅 — PortService 가 통합 로그 + event publish 책임
        if (ValueUtil.isNotEmpty(loc) && PORT_LOC_TYPES.contains(loc.getLocType())) {
            portService.unlock(eqGroupId, locId, null);
            return;
        }

        // lockable 아닌 타입(CHARGE_PORT, VIRTUAL 등)은 스킵
        if (ValueUtil.isNotEmpty(loc) && !LocType.isLockable(loc.getLocType())) return;

        // 언락 실행
        locationRepository.unlock(eqGroupId, locId);
        logger.info("[ Allocation ][ Loc ] unlock success - eqGroupId={}, locId={}", eqGroupId, locId);
    }

    /**
     * 두 로케이션 동시 락 - 데드락 방지를 위해 locId 사전순으로 순차 락한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void lockBoth(String eqGroupId, String loc1, String loc2,
                         String orderKey, String hostOrderKey) {

        // 같으면 단일 락
        if (Objects.equals(loc1, loc2)) {
            lock(eqGroupId, loc1, orderKey, hostOrderKey);
            return;
        }

        // 사전순 순차 락 (데드락 방지)
        List<String> sorted = new ArrayList<>(List.of(loc1, loc2));
        Collections.sort(sorted);
        for (String locId : sorted) {
            lock(eqGroupId, locId, orderKey, hostOrderKey);
        }
    }

    /**
     * 두 로케이션 동시 언락.
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlockBoth(String eqGroupId, String fLoc, String tLoc) {
        unlock(eqGroupId, fLoc);
        unlock(eqGroupId, tLoc);
    }

    /**
     * 운영 에러 격리 - 입출고 플래그를 닫고 원인 오더 키를 taskId 에 기록한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void blockLocationOnError(String eqGroupId, String locId, String orderKey) {

        // 입력값 검증
        if (!hasLocationKey(eqGroupId, locId)) {
            logger.warn("[ Allocation ][ Loc ] block fail - params missing. eqGroupId={}, locId={}", eqGroupId, locId);
            return;
        }

        // 대상 로케이션 조회
        ExtTbInventoryLocation loc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (ValueUtil.isEmpty(loc)) {
            logger.warn("[ Allocation ][ Loc ] block fail - target missing. eqGroupId={}, locId={}", eqGroupId, locId);
            return;
        }

        // 방어적 로깅 - 현재 락과 보고된 orderKey 가 다르면 경고
        if (StringUtils.hasText(orderKey) && !orderKey.equals(loc.getTaskId())) {
            logger.warn("[ Allocation ][ Loc ] block mismatch - reportedOrder={}, currentTaskId={} - force block",
                    orderKey, loc.getTaskId());
        }

        // 입출고 플래그 차단
        loc.setIsInboundEnabled(false);
        loc.setIsOutboundEnabled(false);

        // 업데이트 필드 동적 구성 (orderKey 있을 때만 taskId 포함)
        List<String> updateFields = new ArrayList<>(List.of("isInboundEnabled", "isOutboundEnabled"));
        if (StringUtils.hasText(orderKey)) {
            loc.setTaskId(orderKey);
            updateFields.add("taskId");
        }

        locationRepository.update(loc, updateFields.toArray(new String[0]));
        logger.error("[ Allocation ][ Loc ] blocked - eqGroupId={}, locId={}, causeOrder={}", eqGroupId, locId, orderKey);
    }

    /**
     * 논리 에러(DOUBLE_IN / EMPTY_OUT) 격리 - 발생 시점 stockId 스냅샷을 보존한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void blockLocationWithSpecificTask(String eqGroupId, String locId,
                                              String taskIdentifier, String reasonOrderKey) {

        // 입력값 검증
        if (!hasLocationKey(eqGroupId, locId)) {
            logger.warn("[ Allocation ][ Loc ] block logical fail - params missing. eqGroupId={}, locId={}", eqGroupId, locId);
            return;
        }

        // 대상 로케이션 조회
        ExtTbInventoryLocation loc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (ValueUtil.isEmpty(loc)) {
            logger.warn("[ Allocation ][ Loc ] block logical fail - target missing. eqGroupId={}, locId={}", eqGroupId, locId);
            return;
        }

        // 격리 처리 + 발생 시점 stockId 스냅샷 보존
        loc.setBlockSnapshotStockId(loc.getStockId());
        loc.setIsInboundEnabled(false);
        loc.setIsOutboundEnabled(false);
        loc.setTaskId(taskIdentifier);
        loc.setBlockReasonOrderKey(reasonOrderKey);

        locationRepository.update(loc,
                "isInboundEnabled", "isOutboundEnabled", "taskId",
                "blockReasonOrderKey", "blockSnapshotStockId");

        logger.error("[ Allocation ][ Loc ] blocked logical - eqGroupId={}, locId={}, cause={}, orderKey={}, snapshotStockId={}",
                eqGroupId, locId, taskIdentifier, reasonOrderKey, loc.getBlockSnapshotStockId());
    }

    /**
     * 격리 해제 - 플래그와 스냅샷을 모두 초기화한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearBlockedLocation(String eqGroupId, String locId) {

        // 입력값 검증
        if (!hasLocationKey(eqGroupId, locId)) {
            logger.warn("[ Allocation ][ Loc ] unblock fail - params missing. eqGroupId={}, locId={}", eqGroupId, locId);
            return;
        }

        // 대상 로케이션 조회
        ExtTbInventoryLocation loc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (ValueUtil.isEmpty(loc)) {
            logger.warn("[ Allocation ][ Loc ] unblock fail - target missing. eqGroupId={}, locId={}", eqGroupId, locId);
            return;
        }

        // 격리 해제 - 플래그 복원 + 스냅샷 정보 비움
        loc.setIsInboundEnabled(true);
        loc.setIsOutboundEnabled(true);
        loc.setTaskId(null);
        loc.setBlockReasonOrderKey(null);
        loc.setBlockSnapshotStockId(null);

        locationRepository.update(loc,
                "isInboundEnabled", "isOutboundEnabled", "taskId",
                "blockReasonOrderKey", "blockSnapshotStockId");
        logger.info("[ Allocation ][ Loc ] unblocked - eqGroupId={}, locId={}", eqGroupId, locId);
    }

    /**
     * Host 단계 logical reservation — location.task_id 를 hostOrderKey 로 점유한다.
     * task_id 가 비어있을 때만 성공. 실패 시 STOCK_BUSY 예외.
     *
     * shuttle 단계 lock() 과의 차이:
     *   - lock()        : taskId = wcsOrderKey (물리 단계 락)
     *   - softReserve() : taskId = hostOrderKey (논리 단계 예약)
     */
    @Transactional(rollbackFor = Exception.class)
    public void softReserve(String eqGroupId, String locId, String hostOrderKey) {

        // 입력값 검증
        if (!hasLocationKey(eqGroupId, locId)) {
            throw new ElidomRuntimeException(
                    "INVALID_PARAMETER",
                    "로케이션 soft reserve 실패: 파라미터가 누락되었습니다.");
        }
        if (ValueUtil.isEmpty(hostOrderKey)) {
            throw new ElidomRuntimeException(
                    "INVALID_PARAMETER",
                    "soft reserve 실패: hostOrderKey 가 비어있습니다.");
        }

        // lockable 아닌 타입(포트 등)은 스킵
        ExtTbInventoryLocation loc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (ValueUtil.isNotEmpty(loc) && !LocType.isLockable(loc.getLocType())) {
            logger.info("[ Allocation ][ Loc ] soft reserve skip - not lockable. locId={}, type={}",
                    locId, loc.getLocType());
            return;
        }

        // soft reserve 시도
        boolean ok = locationRepository.softReserve(eqGroupId, locId, hostOrderKey);
        if (!ok) {
            String holder = ValueUtil.isNotEmpty(loc) ? loc.getTaskId() : "unknown";
            logger.warn("[ Allocation ][ Loc ] soft reserve fail - eqGroupId={}, locId={}, holder={}, requestedBy={}",
                    eqGroupId, locId, holder, hostOrderKey);
            throw new ElidomRuntimeException(
                    "STOCK_BUSY",
                    "이미 다른 작업이 점유 중입니다 (작업키=" + holder + ")");
        }
        logger.info("[ Allocation ][ Loc ] soft reserve - eqGroupId={}, locId={}, hostOrderKey={}",
                eqGroupId, locId, hostOrderKey);
    }

    /**
     * Soft Release — 자기 hostOrderKey 가 잡고 있던 task_id 만 해제한다.
     * 이미 shuttle 단계로 넘어가서 wcsOrderKey 로 덮어쓰여있으면 no-op.
     */
    @Transactional(rollbackFor = Exception.class)
    public void softRelease(String eqGroupId, String locId, String hostOrderKey) {
        if (!hasLocationKey(eqGroupId, locId)) return;
        if (ValueUtil.isEmpty(hostOrderKey)) return;

        boolean released = locationRepository.softRelease(eqGroupId, locId, hostOrderKey);
        if (released) {
            logger.info("[ Allocation ][ Loc ] soft release - eqGroupId={}, locId={}, hostOrderKey={}",
                    eqGroupId, locId, hostOrderKey);
        } else {
            logger.debug("[ Allocation ][ Loc ] soft release noop - not my key. locId={}, hostKey={}",
                    locId, hostOrderKey);
        }
    }

    // 로케이션 목록에 위치 정보(LocWithPosition) 결합
    private List<LocWithPosition> attachPositions(String eqGroupId, List<ExtTbInventoryLocation> locs) {
        if (ValueUtil.isEmpty(locs)) return Collections.emptyList();

        // locId 목록 추출
        List<String> locIds = locs.stream()
                .map(ExtTbInventoryLocation::getLocId)
                .filter(StringUtils::hasText)
                .toList();

        // 위치 정보 일괄 조회 후 locId 기준 Map 생성
        List<LocWithPosition> positions = locationRepository.findMultipleWithPosition(eqGroupId, locIds, null);
        Map<String, LocWithPosition> posMap = new HashMap<>();
        for (LocWithPosition p : positions) {
            if (ValueUtil.isNotEmpty(p.getLoc()) && StringUtils.hasText(p.getLoc().getLocId())) {
                posMap.put(p.getLoc().getLocId(), p);
            }
        }

        // 원본 순서 유지하면서 위치 정보 결합 (없으면 기본 위치 부여)
        List<LocWithPosition> result = new ArrayList<>(locs.size());
        for (ExtTbInventoryLocation loc : locs) {
            if (ValueUtil.isEmpty(loc)) continue;
            LocWithPosition pos = posMap.get(loc.getLocId());
            result.add(ValueUtil.isNotEmpty(pos) ? pos : new LocWithPosition(loc, 0, 0, 0, false));
        }
        return result;
    }
}
