package operato.logis.lms.rest.hist;

import lombok.AllArgsConstructor;
import operato.logis.lms.consts.PartitionTable;
import operato.logis.lms.service.impl.hist.PartitionManageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.sys.entity.User;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/partition")
@AllArgsConstructor
public class PartitionController {

    private PartitionManageService partitionManageService;

    /**
     * 파티션 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<List<Map<String, Object>>> getPartitionStatus() {
        if (User.currentUser() != null && User.isCurrentUserAdmin()) {
            return ResponseEntity.ok(this.partitionManageService.getPartitionStatus());
        }
        throw new ElidomClientException(HttpStatus.FORBIDDEN.value(), "Access Denied", null, null);
    }

    /**
     * 수동 파티션 관리
     * - 미래 파티션 생성
     * - 만료 파티션 삭제
     */
    @PostMapping("/manage")
    public ResponseEntity<String> manualPartitionManagement() {
        if (User.isCurrentUserAdmin()) {
            this.partitionManageService.manualPartitionManagement();
            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * 미래 파티션 생성
     */
    @PostMapping("/create-future")
    public ResponseEntity<String> createFuturePartitions() {
        if (User.isCurrentUserAdmin()) {
            this.partitionManageService.createFuturePartitions();
            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * 만료된 파티션 삭제
     */
    @DeleteMapping("/drop-expired")
    public ResponseEntity<String> dropExpiredPartitions() {
        if (User.isCurrentUserAdmin()) {
            this.partitionManageService.dropExpiredPartitions();
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * 특정 날짜 파티션 삭제
     * @param tableName sesison | sign_in | sys
     * @param yearMonth yyyyMM 형식 (e.g 202601)
     */
    @DeleteMapping("/drop/{tableName}/{yearMonth}")
    public ResponseEntity<String> dropSysLogPartition(
            @PathVariable("tableName") String tableName,
            @PathVariable("yearMonth") String yearMonth
    ) {
        if (User.isCurrentUserAdmin()) {
            this.partitionManageService.dropPartitionByDate(PartitionTable.fromValue(tableName), yearMonth);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}