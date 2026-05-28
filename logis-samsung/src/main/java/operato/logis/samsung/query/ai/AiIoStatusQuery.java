package operato.logis.samsung.query.ai;

import operato.logis.samsung.dto.ai.response.IoStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class AiIoStatusQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public IoStatusDto getCurrentIoStatus(LocalDate targetDate, Long domainId) {
        String sql = """
                select
                    :targetDate as target_date,
                    coalesce((
                        select count(*)
                        from samsung_mw.tb_mw_inbound_job
                        where inbound_date = :targetDate
                          and domain_id = :domainId
                    ), 0) as inbound_job_count,
                    coalesce((
                        select sum(completed_item_qty)
                        from samsung_mw.tb_mw_inbound_job
                        where inbound_date = :targetDate
                          and domain_id = :domainId
                    ), 0) as inbound_completed_item_qty,
                    coalesce((
                        select sum(ng_item_qty)
                        from samsung_mw.tb_mw_inbound_job
                        where inbound_date = :targetDate
                          and domain_id = :domainId
                    ), 0) as inbound_ng_item_qty,
                    coalesce((
                        select count(*)
                        from samsung_mw.tb_mw_xyz_order
                        where accept_datetime = :targetDate
                          and domain_id = :domainId
                    ), 0) as outbound_order_count,
                    coalesce((
                        select sum(pass_qty)
                        from samsung_mw.tb_mw_xyz_order
                        where accept_datetime = :targetDate
                          and domain_id = :domainId
                    ), 0) as outbound_completed_qty,
                    coalesce((
                        select sum(ng_qty)
                        from samsung_mw.tb_mw_xyz_order
                        where accept_datetime = :targetDate
                          and domain_id = :domainId
                    ), 0) as outbound_ng_qty,
                    coalesce((
                        select count(*)
                        from samsung_mw.tb_mw_unit_heartbeat
                        where domain_id = :domainId
                          and coalesce(status, "99") <> "0"
                    ), 0) as abnormal_unit_count,
                    coalesce((
                        select count(*)
                        from samsung_mw.tb_mw_unit_error_log
                        where domain_id = :domainId
                          and created_at::date = :targetDate
                    ), 0) as today_error_count
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("targetDate", targetDate)
                .addValue("domainId", domainId);

        return jdbcTemplate.queryForObject(
                sql,
                params,
                BeanPropertyRowMapper.newInstance(IoStatusDto.class)
        );
    }
}