package operato.logis.lms.service.impl.hist;

import operato.logis.lms.config.PartitionConfig;
import operato.logis.lms.consts.PartitionTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PartitionManageService extends AbstractQueryService {

    @Autowired
    private PartitionConfig partitionConfig;

    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(PartitionManageService.class);

    /**
     * 매일 새벽 3시에 파티션 관리 실행 (job: Manage partitioned log tables)
     */
    public void managePartitions() {
        logger.info("Starting partition management");

        try {
            createFuturePartitions();
            dropExpiredPartitions();
            logger.info("Partition management completed successfully");
        } catch (Exception e) {
            logger.error("Partition management failed", e);
        }
    }

    /**
     * 미래 파티션 생성
     */
    @Transactional
    public void createFuturePartitions() {
        logger.info("Creating future partition");

        // 파티션 대상 테이블 목록
        List<String> partitionTables = getPartitionTables();

        for (String tableName : partitionTables) {
            createPartitionsForTable(tableName);
        }
    }

    /**
     * 특정 테이블의 파티션 생성
     */
    private void createPartitionsForTable(String tableName) {
        try {
            // 현재 존재하는 파티션 확인
            List<String> existingPartitions = getExistingPartitions(tableName);

            // 테이블이 파티셔닝되지 않은 경우 초기 파티셔닝 적용
            if (existingPartitions.isEmpty()) {
                createInitialPartitioning(tableName);
                return;
            }

            // 필요한 파티션 생성
            LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
            String prefix = tableName.replace("lms_access_", "").replaceAll("_p$", "");

            for (int i = 0; i <= this.partitionConfig.getBufferMonths(); i++) {
                LocalDate targetMonth = currentMonth.plusMonths(i);
                String partitionName = "p_" + prefix + "_" + targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));

                if (!existingPartitions.contains(partitionName)) {
                    createPartition(tableName, partitionName, targetMonth);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to create partitions for table [{}", tableName, e);
        }
    }

    /**
     * 초기 파티셔닝 생성 (테이블이 파티셔닝되지 않은 경우)
     */
    private void createInitialPartitioning(String tableName) {
        logger.info("Creating initial partitioning for table [{}]", tableName);

        try {
            // 테이블의 가장 오래된 데이터 날짜 조회
            LocalDate oldestDate = getOldestDataDate(tableName);
            LocalDate startMonth = (oldestDate != null) ?
                    oldestDate.withDayOfMonth(1) :
                    LocalDate.now().withDayOfMonth(1);

            LocalDate endMonth = LocalDate.now().withDayOfMonth(1).plusMonths(partitionConfig.getBufferMonths());

            String prefix = tableName.replace("lms_access_", "").replaceAll("_p$", "");
            LocalDate current = startMonth;

            while (!current.isAfter(endMonth)) {
                String partitionName = "p_" + prefix + "_" + current.format(DateTimeFormatter.ofPattern("yyyyMM"));
                LocalDate nextMonth = current.plusMonths(1);
                String sql = String.format(
                        "CREATE TABLE IF NOT EXISTS %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s');",
                        partitionName,
                        tableName,
                        current,
                        nextMonth
                );
                this.queryManager.executeBySql(sql, new HashMap<>());
                logger.info("Initial partitioning completed for table [{}]. SQL: {}", tableName, sql);
                current = nextMonth;
            }


        } catch (Exception e) {
            logger.error("Failed to perform initial partitioning for table [{}]. Error: {}", tableName, e);
        }
    }

    /**
     * 새 파티션 생성
     */
    private void createPartition(String tableName, String partitionName, LocalDate targetMonth) {
        try {
            LocalDate from = targetMonth.withDayOfMonth(1);
            LocalDate to = from.plusMonths(1);

            String sql = String.format(
                    "CREATE TABLE IF NOT EXISTS %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s');",
                    partitionName,
                    tableName,
                    from,
                    to
            );
            logger.info("Partition range: from {} to {}", from, to);

            this.queryManager.executeBySql(sql, new HashMap<>());
            logger.info("Partition created successfully: {}.{}. SQL: {}", tableName, partitionName, sql);

        } catch (Exception e) {
            logger.error("Failed to create partition: {}.{}", tableName, partitionName, e);
        }
    }

    /**
     * 만료된 파티션 삭제
     */
    @Transactional
    public void dropExpiredPartitions() {
        logger.info("Deleting expired partitions");

        List<String> partitionTables = getPartitionTables();
        LocalDate cutoffDate = LocalDate.now().withDayOfMonth(1).minusMonths(this.partitionConfig.getRetentionMonths());

        for (String tableName : partitionTables) {
            dropExpiredPartitionsForTable(tableName, cutoffDate);
        }
    }

    /**
     * 특정 날짜 파티션 삭제
     * - yearMonth 형식: 6자리 숫자 (e.g. 202601)
     */
    @Transactional
    public void dropPartitionByDate(PartitionTable tableName, String yearMonth) {
        if (!yearMonth.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid yearMonth: " + yearMonth + ", yearMonth format must be yyyyMM");
        }

        String partitionName = "p_" + tableName.getValue() + "_log_" + yearMonth;
        logger.info("Drop Partition By Date. partitionName = {}", partitionName);

        dropPartition(partitionName);
    }

    /**
     * 특정 테이블의 만료된 파티션 삭제
     */
    private void dropExpiredPartitionsForTable(String tableName, LocalDate cutoffDate) {
        try {
            String sql = """
                    SELECT c.relname AS partition_name
                    FROM pg_inherits i
                    JOIN pg_class c ON i.inhrelid = c.oid
                    JOIN pg_class p ON i.inhparent = p.oid
                    WHERE p.relname = :tableName
                      AND c.relname LIKE 'p_%'
                      AND c.relname != 'p_max'
                      AND c.relname < :cutoffPartition
                    """;

            String prefix = tableName.replace("lms_access_", "").replaceAll("_p$", "");
            Map<String, Object> params = new HashMap<>();
            params.put("tableName", tableName);
            params.put("cutoffPartition", "p_" + prefix + "_" + cutoffDate.format(DateTimeFormatter.ofPattern("yyyyMM")));

            List<Map> expiredPartitions = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

            for (Map partition : expiredPartitions) {
                String partitionName = (String) partition.get("partition_name");
                dropPartition(partitionName);
            }

        } catch (Exception e) {
            logger.error("Failed to delete expired partitions for table [{}]", tableName, e);
        }
    }

    /**
     * 파티션 삭제
     */
    public void dropPartition(String partitionName) {
        try {
            String sql = "DROP TABLE IF EXISTS " + partitionName;
            this.queryManager.executeBySql(sql, new HashMap<>());
            logger.info("Partition [{}] deleted successfull", partitionName);

        } catch (Exception e) {
            logger.error("Failed to delete partition [{}]", partitionName, e);
        }
    }

    /**
     * 파티션 대상 테이블 목록 조회
     */
    private List<String> getPartitionTables() {
        // 테이블에서 retention 정책 테이블 조회
        String sql = "SELECT DISTINCT log_tb_nm FROM lms_log_retention";

        try {
            List<String> tableList = this.queryManager.selectListBySql(sql, new HashMap<>(), String.class, 0, 0);
            if (tableList != null && !tableList.isEmpty()) {
                logger.info("Loaded partition tables from DB: {}", tableList);
                return tableList;
            }
        } catch (Exception e) {
            logger.warn("Failed to load partition tables from DB. Falling back to configuration.");
        }

        // 없으면 config 사용
        List<String> fallback = partitionConfig.getTables();
        logger.info("Using configured partition tables: {}", fallback);
        return fallback != null ? fallback : new ArrayList<>();
    }

    /**
     * 기존 파티션 목록 조회
     */
    private List<String> getExistingPartitions(String tableName) {
        String sql = """
                SELECT inhrelid::regclass AS child_partition
                FROM pg_inherits
                WHERE inhparent = :tableName::regclass
                """;
        List<String> partitions = this.queryManager.selectListBySql(sql, ValueUtil.newMap("tableName", tableName), String.class, 0, 0);
        logger.info("getExistingPartitions for [{}] = {}", tableName, partitions);
        return partitions;
    }

    /**
     * 테이블의 가장 오래된 데이터 날짜 조회
     */
    private LocalDate getOldestDataDate(String tableName) {
        try {
            String sql = String.format(
                    "SELECT DATE(MIN(log_date)) as oldest_date FROM %s", tableName
            );

            return this.queryManager.selectBySql(sql, new HashMap<>(), LocalDate.class);
        } catch (Exception e) {
            logger.warn("Failed to find the oldest log_date in table [{}]; using current date instead", tableName);
            return null;
        }
    }

    /**
     * 파티션 상태 조회 (모니터링용)
     */
    public List<Map<String, Object>> getPartitionStatus() {
        String sql = """
                SELECT
                    parent.relname AS table_name,
                    child.relname AS partition_name,
                    n.nspname AS schema_name,
                    pg_get_expr(child.relpartbound, child.oid) AS partition_description,
                    pg_size_pretty(pg_total_relation_size(child.oid)) AS size,
                    pg_stat_get_live_tuples(child.oid) AS live_tuples
                FROM pg_inherits
                JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
                JOIN pg_class child ON pg_inherits.inhrelid = child.oid
                JOIN pg_namespace n ON child.relnamespace = n.oid
                WHERE parent.relname IN (:tableNames)
                ORDER BY parent.relname, child.relname
                """;
        Map<String, Object> params = ValueUtil.newMap("tableNames", getPartitionTables());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawList = queryManager.selectListBySql(
                sql, params, (Class<Map<String, Object>>) (Class<?>) Map.class, 0, 0
        );

        return rawList;
    }

    /**
     * 수동 파티션 관리 (관리자용)
     */
    public void manualPartitionManagement() {
        logger.info("Manual partition management triggered");
        managePartitions();
    }
}