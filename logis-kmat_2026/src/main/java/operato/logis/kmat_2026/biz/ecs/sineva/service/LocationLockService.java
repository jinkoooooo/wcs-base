package operato.logis.kmat_2026.biz.ecs.sineva.service;

import operato.logis.kmat_2026.util.InventoryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * ============================================================================
 * Location Lock Service
 * ============================================================================
 *
 * [역할]
 * - tb_ecs_loc_mst 의 locked_yn / lock_order_id 를 이용해
 *   로케이션 점유와 해제를 담당한다.
 *
 * [핵심 정책]
 * - 조회 시점의 lockedYn 은 믿지 않는다.
 * - 반드시 조건부 UPDATE 결과(updated row count)로 lock 성공/실패를 판단한다.
 * - 별도 lock owner 테이블 없이 lock_order_id 로 소유 오더를 추적한다.
 */
@Service("sinevaLocationLockService")
public class LocationLockService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(LocationLockService.class);

    /**
     * 로케이션 선점
     *
     * [정책]
     * - locked_yn = 0 인 경우에만 점유 성공
     * - 성공 시 locK_order_id 도 함께 기록
     *
     * @param locationCd 선점 대상 위치
     * @param orderId    선점 주체 orderId
     * @param taskId     taskId (현재는 로그용)
     * @param lockType   FROM / TO / BUFFER / PRE_STAGE 등 로그용
     * @return true: 선점 성공, false: 선점 실패
     */
    @Transactional
    public boolean tryLockLocation(String locationCd, String orderId, String taskId, String lockType) {
        if (ValueUtil.isEmpty(locationCd) || ValueUtil.isEmpty(orderId)) {
            logger.warn("[LocationLockService] invalid lock request - locationCd={}, orderId={}, taskId={}, lockType={}",
                    locationCd, orderId, taskId, lockType);
            return false;
        }

        String sql =
                "UPDATE tb_ecs_loc_mst " +
                        "   SET locked_yn = 1, " +
                        "       lock_order_id = :orderId " +
                        " WHERE domain_id = :domainId " +
                        "   AND location_cd = :locationCd " +
                        "   AND locked_yn = 0";

        int updated = queryManager.executeBySql(
                sql,
                ValueUtil.newMap(
                        "domainId,locationCd,orderId",
                        InventoryConstants.DOMAIN,
                        locationCd,
                        orderId
                )
        );

        boolean success = updated == 1;

        if (success) {
            logger.info("[LocationLockService] lock success - locationCd={}, orderId={}, taskId={}, lockType={}",
                    locationCd, orderId, taskId, lockType);
        } else {
            logger.info("[LocationLockService] lock fail - locationCd={}, orderId={}, taskId={}, lockType={}",
                    locationCd, orderId, taskId, lockType);
        }

        return success;
    }

    /**
     * 내 오더가 잡은 락만 해제
     *
     * @param locationCd 위치 코드
     * @param orderId    owner orderId
     * @return true: 해제 성공, false: owner mismatch 또는 이미 해제됨
     */
    @Transactional
    public boolean unlockLocationByOrder(String locationCd, String orderId) {
        if (ValueUtil.isEmpty(locationCd) || ValueUtil.isEmpty(orderId)) {
            return false;
        }

        String sql =
                "UPDATE tb_ecs_loc_mst " +
                        "   SET locked_yn = 0, " +
                        "       lock_order_id = NULL " +
                        " WHERE domain_id = :domainId " +
                        "   AND location_cd = :locationCd " +
                        "   AND lock_order_id = :orderId";

        int updated = queryManager.executeBySql(
                sql,
                ValueUtil.newMap(
                        "domainId,locationCd,orderId",
                        InventoryConstants.DOMAIN,
                        locationCd,
                        orderId
                )
        );

        boolean success = updated == 1;

        if (success) {
            logger.info("[LocationLockService] unlock success - locationCd={}, orderId={}", locationCd, orderId);
        } else {
            logger.info("[LocationLockService] unlock skip - locationCd={}, orderId={}, reason=owner mismatch or already unlocked",
                    locationCd, orderId);
        }

        return success;
    }

    /**
     * 특정 오더가 보유한 from/to 락 해제
     */
    @Transactional
    public int releaseOrderOwnedLocks(String orderId, String fromPositionCd, String toPositionCd) {
        int released = 0;

        if (ValueUtil.isNotEmpty(fromPositionCd) && unlockLocationByOrder(fromPositionCd, orderId)) {
            released++;
        }

        if (ValueUtil.isNotEmpty(toPositionCd)
                && !toPositionCd.equals(fromPositionCd)
                && unlockLocationByOrder(toPositionCd, orderId)) {
            released++;
        }

        logger.info("[LocationLockService] releaseOrderOwnedLocks - orderId={}, released={}", orderId, released);
        return released;
    }
}