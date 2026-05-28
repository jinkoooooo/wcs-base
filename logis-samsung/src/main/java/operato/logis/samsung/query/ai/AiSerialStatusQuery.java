package operato.logis.samsung.query.ai;

import operato.logis.samsung.dto.ai.response.SerialProgressDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AiSerialStatusQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SerialProgressDto getSerialProgress(String serialNo, Long domainId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("serialNo", serialNo)
                .addValue("domainId", domainId);

        SerialProgressDto dto = new SerialProgressDto();
        dto.setSerialNo(serialNo);

        List<Map<String, Object>> boxRows = jdbcTemplate.queryForList("""
                select
                    item_code,
                    item_name,
                    final_status,
                    reject_type
                from samsung_mw.tb_mw_box
                where domain_id = :domainId
                  and (
                        parcel_id = :serialNo
                     or box_id = :serialNo
                     or plc_seq_no = :serialNo
                  )
                order by coalesce(updated_at, created_at) desc
                limit 1
                """, params);

        if (!boxRows.isEmpty()) {
            Map<String, Object> row = boxRows.get(0);
            dto.setItemCode(str(row.get("item_code")));
            dto.setItemName(str(row.get("item_name")));
            dto.setFinalStatus(intVal(row.get("final_status")));
            dto.setRejectType(str(row.get("reject_type")));
        }

        List<Map<String, Object>> trackRows = jdbcTemplate.queryForList("""
                select
                    tracking_type,
                    tracking_status,
                    tracking_desc,
                    tracking_at,
                    line_id,
                    equip_id
                from samsung_mw.tb_mw_box_track
                where domain_id = :domainId
                  and (
                        parcel_id = :serialNo
                     or box_id = :serialNo
                     or plc_seq_no = :serialNo
                  )
                order by tracking_at desc nulls last, coalesce(updated_at, created_at) desc
                limit 1
                """, params);

        if (!trackRows.isEmpty()) {
            Map<String, Object> row = trackRows.get(0);
            dto.setCurrentTrackingType(str(row.get("tracking_type")));
            dto.setCurrentTrackingStatus(intVal(row.get("tracking_status")));
            dto.setTrackingDesc(str(row.get("tracking_desc")));
            dto.setLastTrackingAt(toLdt(row.get("tracking_at")));
            dto.setCurrentLineId(str(row.get("line_id")));
            dto.setCurrentEquipId(str(row.get("equip_id")));
        }

        List<Map<String, Object>> conveyorRows = jdbcTemplate.queryForList("""
                select
                    pid,
                    is_picked,
                    item_code
                from samsung_mw.tb_mw_box_conveyor_info
                where domain_id = :domainId
                  and serial_no = :serialNo
                order by coalesce(updated_at, created_at) desc
                limit 1
                """, params);

        if (!conveyorRows.isEmpty()) {
            Map<String, Object> row = conveyorRows.get(0);
            dto.setCurrentPid(str(row.get("pid")));
            dto.setPicked((Boolean) row.get("is_picked"));
            if (dto.getItemCode() == null) {
                dto.setItemCode(str(row.get("item_code")));
            }
        }

        return dto;
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer intVal(Object value) {
        if (value == null) return null;
        return ((Number) value).intValue();
    }

    private LocalDateTime toLdt(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().atStartOfDay();
        }
        return null;
    }
}