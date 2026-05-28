package operato.logis.samsung.service.mw;

import operato.logis.samsung.entity.mw.TbMwBoxConveyorInfo;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
public class TbMwBoxConveyorInfoService extends AbstractQueryService {

    /**
     * BoxConveyor Event 호출 시 이력 생성
     * @param boxList 대상 Box 정보 List
     * @param taskId 대상 박스에 대한 XYZ Order ID
     */
    public void createBoxConveyorInfo(List<TbMwBoxConveyorInfo> boxList, String taskId) {
        for (TbMwBoxConveyorInfo box : boxList) {
            box.setTaskId(taskId);
            box.setPicked(false);
        }
        this.queryManager.insertBatch(boxList);
    }

    /**
     * BoxConveyor Event 호출 시 이력 생성
     * @param taskId 완료된 XYZ Order ID
     * @param palletSequence 완료 작업에 대한 Pallet Sequence
     * @param pickedNum XYZ 완료 Box 수량
     *
     *                  251209 JJG : BoxTracking 완료를 위한 PID 추출을 위해 리턴형식 변경
     */
    public List<TbMwBoxConveyorInfo> pickBoxConveyor(String taskId, String palletSequence, int pickedNum) {
        // 아직 처리되지 않은 박스 중 pickedNum 만큼 조회
        String sql = """
                SELECT *
                FROM tb_mw_box_conveyor_info
                WHERE task_id = :taskId
                  AND is_picked = false
                ORDER BY index ASC
                LIMIT :pickedNum
                """;
        Map<String, Object> param = ValueUtil.newMap("taskId,pickedNum", taskId, pickedNum);
        List<TbMwBoxConveyorInfo> boxList = this.queryManager.selectListBySql(sql, param, TbMwBoxConveyorInfo.class, 0, 0);
        if (ValueUtil.isEmpty(boxList)) {
            logger.error("Task ID : {}에 해당하는 처리 가능한 BoxConveyor 정보가 존재하지 않습니다.", taskId);
            return null;
        } else if (boxList.size() < pickedNum) {
            logger.error("BoxConveyor 정보 개수({})가 PickedNum({})보다 작습니다. Task ID : {}", boxList.size(), pickedNum, taskId);
        }

        // 박스 상태 업데이트
        for (TbMwBoxConveyorInfo box : boxList) {
            box.setPalletSequence(palletSequence);
            box.setPicked(true);
            this.queryManager.update(box, "palletSequence", "isPicked");
        }

        return boxList;
    }
}