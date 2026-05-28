package operato.logis.samsung.query.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AiChatHistoryQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void insertHistory(
            String id,
            String question,
            String intentType,
            String userId,
            Long domainId,
            String requestJson,
            String responseJson,
            String successYn,
            String errorMsg
    ) {
        jdbcTemplate.update("""
                insert into samsung_mw.tb_mw_ai_chat_history (
                    id, question, intent_type, user_id, domain_id,
                    request_json, response_json, success_yn, error_msg,
                    created_at, updated_at
                )
                values (
                    :id, :question, :intentType, :userId, :domainId,
                    :requestJson, :responseJson, :successYn, :errorMsg,
                    now(), now()
                )
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("question", question)
                        .addValue("intentType", intentType)
                        .addValue("userId", userId)
                        .addValue("domainId", domainId)
                        .addValue("requestJson", requestJson)
                        .addValue("responseJson", responseJson)
                        .addValue("successYn", successYn)
                        .addValue("errorMsg", errorMsg)
        );
    }
}