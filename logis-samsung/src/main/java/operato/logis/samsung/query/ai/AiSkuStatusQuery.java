package operato.logis.samsung.query.ai;

import operato.logis.samsung.dto.ai.response.SkuIoStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class AiSkuStatusQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SkuIoStatusDto getSkuStatus(String itemCode, LocalDate targetDate, Long domainId) {
        String sql = """
                select
                    :itemCode as item_code,
                    (
                        select im.item_name
                        from samsung_mw.tb_mw_item_master im
                        where im.item_code = :itemCode
                          and im.domain_id = :domainId
                        order by coalesce(im.updated_at, im.created_at) desc
                        limit 1
                    ) as item_name,
                    coalesce((
                        select sum(d.item_qty)
                        from samsung_mw.tb_mw_inbound_delivery d
                        where d.item_code = :itemCode
                          and d.inbound_date = :targetDate
                          and d.domain_id = :domainId
                    ), 0) as today_inbound_qty,
                    coalesce((
                        select sum(o.target_num)
                        from samsung_mw.tb_mw_xyz_order o
                        where o.item_code = :itemCode
                          and o.accept_datetime = :targetDate
                          and o.domain_id = :domainId
                    ), 0) as today_outbound_target_qty,
                    coalesce((
                        select sum(o.pass_qty)
                        from samsung_mw.tb_mw_xyz_order o
                        where o.item_code = :itemCode
                          and o.accept_datetime = :targetDate
                          and o.domain_id = :domainId
                    ), 0) as today_outbound_pass_qty,
                    coalesce((
                        select sum(o.ng_qty)
                        from samsung_mw.tb_mw_xyz_order o
                        where o.item_code = :itemCode
                          and o.accept_datetime = :targetDate
                          and o.domain_id = :domainId
                    ), 0) as today_outbound_ng_qty,
                    coalesce((
                        select count(*)
                        from samsung_mw.tb_mw_reject_box r
                        where r.item_code = :itemCode
                          and r.inbound_date = :targetDate
                          and r.domain_id = :domainId
                    ), 0) as reject_qty,
                    (
                        select coalesce(b.updated_at, b.created_at)
                        from samsung_mw.tb_mw_box b
                        where b.item_code = :itemCode
                          and b.domain_id = :domainId
                        order by coalesce(b.updated_at, b.created_at) desc
                        limit 1
                    ) as last_processed_at,
                    (
                        select coalesce(b.tracking_desc, b.final_remark)
                        from samsung_mw.tb_mw_box b
                        where b.item_code = :itemCode
                          and b.domain_id = :domainId
                        order by coalesce(b.updated_at, b.created_at) desc
                        limit 1
                    ) as last_status
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("itemCode", itemCode)
                .addValue("targetDate", targetDate)
                .addValue("domainId", domainId);

        return jdbcTemplate.queryForObject(
                sql,
                params,
                BeanPropertyRowMapper.newInstance(SkuIoStatusDto.class)
        );
    }
}