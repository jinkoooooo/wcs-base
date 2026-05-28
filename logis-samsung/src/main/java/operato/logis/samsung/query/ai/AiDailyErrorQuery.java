package operato.logis.samsung.query.ai;

import operato.logis.samsung.dto.ai.response.DailyErrorStatusDto;
import operato.logis.samsung.dto.ai.response.DailyErrorTopDto;
import operato.logis.samsung.dto.ai.response.UnresolvedErrorDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiDailyErrorQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DailyErrorStatusDto getDailyErrorStatus(LocalDate targetDate, Long domainId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("targetDate", targetDate)
                .addValue("domainId", domainId);

        Integer totalErrorCount = jdbcTemplate.queryForObject("""
                select coalesce(count(*), 0)
                from samsung_mw.tb_mw_unit_error_log
                where domain_id = :domainId
                  and created_at::date = :targetDate
                """, params, Integer.class);

        Integer abnormalUnitCount = jdbcTemplate.queryForObject("""
                select coalesce(count(*), 0)
                from samsung_mw.tb_mw_unit_heartbeat
                where domain_id = :domainId
                  and coalesce(status, "99") <> "0"
                """, params, Integer.class);

        List<DailyErrorTopDto> topErrors = jdbcTemplate.query("""
                select
                    error_code,
                    count(*)::int as occurrence_count
                from samsung_mw.tb_mw_unit_error_log
                where domain_id = :domainId
                  and created_at::date = :targetDate
                group by error_code
                order by occurrence_count desc
                limit 5
                """,
                params,
                BeanPropertyRowMapper.newInstance(DailyErrorTopDto.class)
        );

        List<UnresolvedErrorDto> unresolvedErrors = jdbcTemplate.query("""
                select
                    e.unit_type,
                    e.unit_code,
                    e.error_code,
                    e.error_msg,
                    e.created_at as occurred_at
                from (
                    select distinct on (unit_type, unit_code)
                        unit_type,
                        unit_code,
                        error_code,
                        error_msg,
                        created_at
                    from samsung_mw.tb_mw_unit_error_log
                    where domain_id = :domainId
                      and created_at::date = :targetDate
                    order by unit_type, unit_code, created_at desc
                ) e
                join samsung_mw.tb_mw_unit_heartbeat s
                  on s.domain_id = :domainId
                 and s.unit_type = e.unit_type
                 and s.unit_code = e.unit_code
                where coalesce(s.status, "99") <> "0"
                order by e.created_at desc
                limit 20
                """,
                params,
                BeanPropertyRowMapper.newInstance(UnresolvedErrorDto.class)
        );

        DailyErrorStatusDto dto = new DailyErrorStatusDto();
        dto.setTargetDate(targetDate);
        dto.setTotalErrorCount(totalErrorCount == null ? 0 : totalErrorCount);
        dto.setAbnormalUnitCount(abnormalUnitCount == null ? 0 : abnormalUnitCount);
        dto.setTopErrors(topErrors);
        dto.setUnresolvedErrors(unresolvedErrors);
        return dto;
    }
}