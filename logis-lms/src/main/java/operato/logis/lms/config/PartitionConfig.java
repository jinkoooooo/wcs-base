package operato.logis.lms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class PartitionConfig {

    // 미리 생성할 파티션 개월 수 (기본값)
    private static int bufferMonths = 3;

    // 로그 보관 개월 수 (기본값)
    private static int retentionMonths = 12;

    // 파티션 대상 테이블 (기본값)
    private List<String> tables = List.of("lms_access_sys_log_p");

    // 테이블별 개별 설정
    private Map<String, TablePartitionConfig> tableConfigs;


    public int getBufferMonths() { return bufferMonths; }

    public int getRetentionMonths() { return retentionMonths; }

    public List<String> getTables() { return tables; }

    public Map<String, TablePartitionConfig> getTableConfigs() { return tableConfigs; }

    public void setBufferMonths(int bufferMonths) { this.bufferMonths = bufferMonths; }

    public void setRetentionMonths(int retentionMonths) { this.retentionMonths = retentionMonths; }

    public void setTables(List<String> tables) { this.tables = tables; }

    public void setTableConfigs(Map<String, TablePartitionConfig> tableConfigs) { this.tableConfigs = tableConfigs; }


    public static class TablePartitionConfig {
        private String dateColumn = "log_date";     // 파티션 키 컬럼 (기본값)
        private int retentionMonths = 12;           // 로그 보관 개월 수 (기본값)

        public String getDateColumn() { return dateColumn; }

        public int getRetentionMonths() { return retentionMonths; }

        public void setDateColumn(String dateColumn) { this.dateColumn = dateColumn; }

        public void setRetentionMonths(int retentionMonths) { this.retentionMonths = retentionMonths; }
    }
}
