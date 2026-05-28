package operato.logis.ecs.base.ecs.domain.crane;

import lombok.Data;
import operato.logis.ecs.base.ecs.domain.cell.CraneCell;
import operato.logis.ecs.base.ecs.plc.crane.StackerCraneWriteMap;

import java.util.ArrayList;
import java.util.List;

@Data
public class StackerCrane {

    private String stackerCraneId;
    private int station; // todo: station 타입 확인
    private int asiel;
    private int bay;
    private int level;
    private int originAsiel;
    private int originBay;
    private int originLevel;

    private List<StackerCraneWriteMap> cranePathCmdList = new ArrayList<>(); // 경로
    private int reserveCmdCurrentIndex = 0;
    public int reserveCmdSize = 0;

    public StackerCrane(String stackerCraneId) { this.stackerCraneId = stackerCraneId; }

    public void updateReserveCmdIndex(int index) {
        reserveCmdCurrentIndex = index;
    }

    public boolean hasNextCmd() {
        if (reserveCmdCurrentIndex + 1 < reserveCmdSize) {
            return true;
        }
        cranePathCmdList = new ArrayList<>();
        reserveCmdSize = 0;
        reserveCmdCurrentIndex = 0;
        return false;
    }

    public void initPathCmdList() {
        cranePathCmdList = new ArrayList<>();
        reserveCmdSize = 0;
        reserveCmdCurrentIndex = 0;
    }

    public void addPrePixCmd(StackerCraneWriteMap prefixCmd) {
        // 현재 인덱스의 경로
        var originIndexCmd = cranePathCmdList.get(reserveCmdCurrentIndex);
        // 생성된 경로가 원래 경로와 중복되지 않게 원래 경로를 수정.
        List<CraneCell> remainPath = new ArrayList<>(originIndexCmd.getPath().subList(prefixCmd.getPath().size(), originIndexCmd.getPath().size()));
        originIndexCmd.setPath(remainPath);
        originIndexCmd.setFromCraneCell(prefixCmd.getToCraneCell());

        // 생성된 경로를 현재 인덱스에 추가
        cranePathCmdList.add(reserveCmdCurrentIndex, prefixCmd);

        reserveCmdSize = cranePathCmdList.size();
    }

    public void addPrePixCmd(List<StackerCraneWriteMap> prefixCmdList) {
        for (var cmd : prefixCmdList) {
            addPrePixCmd(cmd);
        }
    }
}
