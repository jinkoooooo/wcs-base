package operato.logis.samsung.service.buffer;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.consts.BufferOperationType;
import operato.logis.samsung.consts.BufferTaskStatus;
import operato.logis.samsung.consts.BufferTaskType;
import operato.logis.samsung.entity.buffer.TbMwBufferStorageArea;
import operato.logis.samsung.entity.buffer.TbMwBufferTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * - 시퀀스버퍼 작업 생성
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TbMwBufferTaskService extends AbstractQueryService {

    // TODO: Spiral 진입 박스 중 시퀀스버퍼 진입 박스에 대해서만 입고 예정 작업 생성
    // TODO: Spiral 진입 박스 중 시퀀스버퍼 미진입 박스에 대해 tbMwBoxBuffer 데이터 삭제

    public void createAllocationTask(Map<String, List<TbMwBufferStorageArea>> allocation) {
        List<TbMwBufferTask> tasks = new ArrayList<>();

        for(Map.Entry<String, List<TbMwBufferStorageArea>> entry : allocation.entrySet()){
            String itemCode = entry.getKey();

            TbMwBufferTask task = new TbMwBufferTask();
            task.setTaskType(BufferTaskType.INBOUND.getValue());
            task.setTaskMode(BufferOperationType.AUTO.getValue());
            task.setTaskStatus(BufferTaskStatus.READY.getValue());
            task.setItemCode(itemCode);
            task.setPriority(null); // todo: 별도 구현
            task.setTaskId(generateTaskId(task));

            tasks.add(task);
        }
        queryManager.insertBatch(tasks);
    }

    /**
     * 작업 번호 생성
     * - 형식 : {TASK TYPE}-{yyyyMMddHHmmss}-{BARCODE}-{RANDOM} 형식
     * - 예시 : INB-20260507153022-534-482
     *
     * @param task 작업 데이터
     * @return 작업번호 (unique)
     */
    private String generateTaskId(TbMwBufferTask task) {
        if (task.getTaskId() != null) return task.getTaskId();
        if (task.getItemCode() == null) {
            logger.error("[BUFFER-TASK] Create TaskId Error. TaskId[{}], ItemCode[{}]", task, task.getItemCode());
            return null;
        }

        String taskType = BufferTaskType.fromValue(task.getTaskType()).name().substring(0, 3);
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rawItemCode = task.getItemCode();
        String itemCode = rawItemCode.length() <= 3 ? rawItemCode : rawItemCode.substring(rawItemCode.length() - 3);
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        if (itemCode == null) {
            return taskType + "-" + dateTime + "-" + random;
        }
        return taskType + "-" + dateTime + "-" + itemCode + "-" + random;
    }
}