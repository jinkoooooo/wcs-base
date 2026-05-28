package operato.logis.samsung.query.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import operato.logis.samsung.dto.ai.response.AiGuideStepDto;
import operato.logis.samsung.dto.ai.response.ErrorGuideDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AiErrorGuideQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ErrorGuideDto getErrorGuide(String errorCode, Long domainId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("errorCode", errorCode)
                .addValue("domainId", domainId);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select
                    error_code,
                    unit_type,
                    error_name,
                    error_desc,
                    main_cause,
                    severity,
                    manual_required_yn,
                    action_steps_json
                from samsung_mw.tb_mw_error_guide
                where error_code = :errorCode
                  and domain_id = :domainId
                  and active_yn = 'Y'
                limit 1
                """, params);

        ErrorGuideDto dto = new ErrorGuideDto();
        dto.setErrorCode(errorCode);

        if (!rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            dto.setUnitType(str(row.get("unit_type")));
            dto.setErrorName(str(row.get("error_name")));
            dto.setErrorDesc(str(row.get("error_desc")));
            dto.setMainCause(str(row.get("main_cause")));
            dto.setSeverity(str(row.get("severity")));
            dto.setManualRequiredYn(str(row.get("manual_required_yn")));
            dto.setGuideSteps(parseSteps(str(row.get("action_steps_json"))));
        } else {
            dto.setErrorName("미등록 에러");
            dto.setErrorDesc("가이드 마스터에 등록되지 않은 에러코드입니다.");
            dto.setGuideSteps(Collections.singletonList(
                    AiGuideStepDto.builder()
                            .stepNo(1)
                            .title("설비 담당 확인")
                            .description("에러코드 마스터 미등록 상태입니다. 설비사 정의서 또는 운영 매뉴얼 기준으로 확인이 필요합니다.")
                            .build()
            ));
        }

        Integer recentCount = jdbcTemplate.queryForObject("""
                select coalesce(count(*), 0)
                from samsung_mw.tb_mw_unit_error_log
                where error_code = :errorCode
                  and domain_id = :domainId
                  and created_at >= now() - interval '7 days'
                """, params, Integer.class);

        dto.setRecentCount(recentCount == null ? 0 : recentCount);
        return dto;
    }

    public List<AiGuideStepDto> getGuideStepsOnly(String errorCode, Long domainId) {
        return getErrorGuide(errorCode, domainId).getGuideSteps();
    }

    private List<AiGuideStepDto> parseSteps(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<AiGuideStepDto>>() {});
        } catch (Exception e) {
            return Collections.singletonList(
                    AiGuideStepDto.builder()
                            .stepNo(1)
                            .title("가이드 파싱 실패")
                            .description(json)
                            .build()
            );
        }
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}