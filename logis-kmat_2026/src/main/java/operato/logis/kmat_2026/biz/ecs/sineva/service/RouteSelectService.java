package operato.logis.kmat_2026.biz.ecs.sineva.service;

import operato.logis.kmat_2026.entity.TbEcsLocMst;
import operato.logis.kmat_2026.service.impl.TbEcsLocMstService;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.LocationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * Route Select Service
 * ============================================================================
 *
 * [역할]
 * - 기존 BufferLocationSelector 역할을 정리한 서비스
 * - 로케이션 "조회" + "선점 확정" 정책을 함께 제공
 *
 * [핵심 정책]
 * 1. SELECT ... FOR UPDATE SKIP LOCKED 로 후보를 선점성 있게 조회
 * 2. 조회된 row 에 대해 locked_yn=0 조건부 UPDATE 로 최종 선점 확정
 * 3. 성공한 건만 반환
 *
 * [중요]
 * - 조회 시점의 TbEcsLocMst.lockedYn 값은 신뢰하지 않는다.
 * - 반드시 LocationLockService.tryLockLocation(...) 결과로 확정해야 한다.
 */
@Service
public class RouteSelectService {

    private static final Logger logger = LoggerFactory.getLogger(RouteSelectService.class);

    @Autowired
    protected TbEcsLocMstService tbEcsLocMstService;

    @Autowired
    protected LocationLockService locationLockService;

    /**
     * Shuttle Inbound 기본 목적지 선택
     *
     * 현재 기본 정책:
     * - AMRBUF
     * - EMPTY
     * - location_seq ASC
     *
     * 필요 시 여기 정책만 바꾸면 된다.
     */
    @Transactional
    public TbEcsLocMst selectInboundTargetWithLock(String orderId, String taskId) {
        return selectOneWithLockSkip(
                "TSPG_CV_IO",
                LocationStatus.EMPTY.getCode(),
                true,
                orderId,
                taskId,
                "TO"
        );
    }

    /**
     * Shuttle Inbound 기본 목적지 선택
     *
     * 현재 기본 정책:
     * - AMRBUF
     * - EMPTY
     * - location_seq ASC
     *
     * 필요 시 여기 정책만 바꾸면 된다.
     */
    @Transactional
    public TbEcsLocMst selectTspgConveyorOutboundTargetWithLock(String orderId, String taskId) {
        return selectOneWithLockSkip(
                "AMRBUF",
                LocationStatus.EMPTY.getCode(),
                true,
                orderId,
                taskId,
                "TO"
        );
    }

    /**
     * 공통 단건 선택 + lock 확정
     */
    @Transactional
    public TbEcsLocMst selectOneWithLockSkip(
            String groupCd,
            String locationStatus,
            boolean asc,
            String orderId,
            String taskId,
            String lockType
    ) {
        return selectOneWithLockSkip(groupCd, locationStatus, asc, orderId, taskId, lockType, null);
    }

    /**
     * 공통 단건 선택 + lock 확정 (podCd 필터 포함)
     */
    @Transactional
    public TbEcsLocMst selectOneWithLockSkip(
            String groupCd,
            String locationStatus,
            boolean asc,
            String orderId,
            String taskId,
            String lockType,
            List<String> podCdList
    ) {
        TbEcsLocMst candidate = tbEcsLocMstService.selectOneCandidateWithLockSkip(
                groupCd,
                locationStatus,
                asc,
                podCdList
        );

        if (ValueUtil.isEmpty(candidate) || ValueUtil.isEmpty(candidate.getLocationCd())) {
            return null;
        }

        boolean locked = locationLockService.tryLockLocation(
                candidate.getLocationCd(),
                orderId,
                taskId,
                lockType
        );

        if (!locked) {
            logger.info("[RouteSelectService] single lock fail - locationCd={}, orderId={}",
                    candidate.getLocationCd(), orderId);
            return null;
        }

        return candidate;
    }

    @Transactional
    public List<TbEcsLocMst> selectListWithLockSkip(
            String groupCd,
            String locationStatus,
            int size,
            boolean asc,
            String orderId,
            String taskId,
            String lockType
    ) {
        List<TbEcsLocMst> candidates = tbEcsLocMstService.selectListCandidateWithLockSkip(
                groupCd,
                locationStatus,
                size,
                asc
        );

        if (ValueUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }

        return candidates.stream()
                .filter(loc -> {
                    if (ValueUtil.isEmpty(loc) || ValueUtil.isEmpty(loc.getLocationCd())) {
                        return false;
                    }

                    boolean ok = locationLockService.tryLockLocation(
                            loc.getLocationCd(),
                            orderId,
                            taskId,
                            lockType
                    );

                    if (!ok) {
                        logger.info("[RouteSelectService] list lock fail - locationCd={}, orderId={}",
                                loc.getLocationCd(), orderId);
                    }

                    return ok;
                })
                .collect(Collectors.toList());
    }
}