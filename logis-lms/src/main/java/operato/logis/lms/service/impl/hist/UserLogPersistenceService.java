package operato.logis.lms.service.impl.hist;

import operato.logis.lms.consts.SessionStatus;
import operato.logis.lms.consts.SignOutType;
import operato.logis.lms.entity.hist.AccessSessionLog;
import operato.logis.lms.entity.hist.AccessSignInLog;
import operato.logis.lms.entity.hist.AccessSysLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 로그 DB 저장 전담 서비스
 */
@Service
public class UserLogPersistenceService extends AbstractQueryService {

    @Transactional
    public void insertSysLogBatch(List<AccessSysLog> logs) {
        this.queryManager.insertBatch(logs);
    }

    /**
     * 로그아웃 이력 저장 (sign_out_type 설정)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSignOutHistory(String signOutType) {
        logger.info("Access Sigin-out history");
        try {
            if (signOutType == null) {
                signOutType = SignOutType.UNKNOWN.getValue();
            }

            User user = User.currentUser();
            if (user == null) {
                logger.info("UPDATE SIGNOUT HISTORY - EMPTY USER");
                return;
            }

            String sessionId = SessionUtil.getSessionId();
            String sql = """
                        SELECT * FROM lms_access_sign_in_log_p
                        WHERE log_date BETWEEN :startDate AND :endDate
                        AND user_id = :userId
                        AND session_id = :sessionId;
                    """;
            Map<String, Object> params = ValueUtil.newMap("startDate,endDate,userId,sessionId", LocalDate.now().minusDays(1), LocalDate.now(), user.getId(), sessionId);
            List<AccessSignInLog> newHistory = queryManager.selectListBySql(sql, params, AccessSignInLog.class, 0, 0);

            if (!newHistory.isEmpty()) {
                sql = "UPDATE lms_access_sign_in_log_p "
                        + "SET sign_out_type = :signOutType "
                        + "WHERE user_id = :userId "
                        + "AND session_id = :sessionId "
                        + "AND sign_in_result = 1 "
                        + "AND http_status_cd = 200 "
                        + "AND sign_out_type IS NULL";
                params = ValueUtil.newMap("signOutType,userId,sessionId", signOutType, user.getId(), sessionId);
                queryManager.executeByQl(sql, params);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 만료 세션 이력 수정 (sign_out_type, session_status 설정)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSessionHistory(String signOutType) {
        try {
            User user = User.currentUser();
            if (user == null) {
                return;
            }

            Integer sessionStatus = SessionStatus.EXPIRED.getValue();
            if (signOutType == null) {
                signOutType = SignOutType.UNKNOWN.getValue();
                sessionStatus = SessionStatus.UNKNOWN.getValue();
            } else if (signOutType.equals(SignOutType.MENU.getValue())) {
                sessionStatus = SessionStatus.LOGGED_OUT.getValue();
            }

            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            Map<String, Object> params = ValueUtil.newMap("oneDayAgo,userId,sessionId,domainId",
                    oneDayAgo, user.getId(), SessionUtil.getSessionId(), Domain.currentDomainId());

            String query = """
                    SELECT *
                    FROM lms_access_session_log_p
                    WHERE created_at >= :oneDayAgo
                        AND sign_out_type IS NULL
                        AND user_id = :userId
                        AND session_id = :sessionId
                        AND domain_id = :domainId
                    ORDER BY created_at DESC
                    """;
            List<AccessSessionLog> newHistory = queryManager.selectListBySql(query, params, AccessSessionLog.class, 0, 0);

            logger.info("ACCESS SIGNOUT HISTORY : length = {}", newHistory.size());
            if (!newHistory.isEmpty()) {
                String sql = "UPDATE lms_access_session_log_p "
                        + "SET session_status = :sessionStatus, "
                        + "    sign_out_type = :signOutType "
                        + "WHERE created_at >= :oneDayAgo "
                        + "AND sign_out_type IS NULL "
                        + "AND domain_id = :domainId "
                        + "AND user_id = :userId "
                        + "AND session_id = :sessionId";
                params = ValueUtil.newMap("sessionStatus,signOutType,oneDayAgo,userId,sessionId,domainId", sessionStatus, signOutType, oneDayAgo, user.getId(), SessionUtil.getSessionId(), Domain.currentDomainId());
                queryManager.executeByQl(sql, params);
            }
            logger.info("updateSessionHistory [SUCCESS] : sessionStatus = {}", sessionStatus);
        } catch (Exception e) {
            logger.error("updateSessionHistory [FAIL]", e);
        }
    }
}