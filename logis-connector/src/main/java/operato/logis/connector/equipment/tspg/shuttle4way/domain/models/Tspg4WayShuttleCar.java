package operato.logis.connector.equipment.tspg.shuttle4way.domain.models;

import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayWriteMap;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class Tspg4WayShuttleCar {
    private String shuttleCarId;
    private int x;
    private int y;
    private int level;
    private int originLevel;

    private List<Shuttle4WayWriteMap> ShuttlePathCmdList = new ArrayList<>(); // 경로
    private int reserveCmdCurrentIndex = 0;
    public int reserveCmdSize = 0;

    public Tspg4WayShuttleCar(String id) {this.shuttleCarId = id;}

    public void updateReserveCmdIndex(int index){
        reserveCmdCurrentIndex = index;
    }
    public void rollBackReserveCmdCurrentIndex(){
        if(reserveCmdCurrentIndex > 0)
            reserveCmdCurrentIndex = reserveCmdCurrentIndex - 1;
    }
    public void updateReserveCmdCurrentIndex(){
        reserveCmdCurrentIndex = reserveCmdCurrentIndex + 1;
    }
    public boolean hasNextCmd(){
        if(reserveCmdCurrentIndex + 1 < reserveCmdSize) {
            return true;
        }
        ShuttlePathCmdList = new ArrayList<>();
        reserveCmdSize = 0;
        reserveCmdCurrentIndex = 0;
        return false;
    }
    public void initPathCmdList(){
        ShuttlePathCmdList = new ArrayList<>();
        reserveCmdSize = 0;
        reserveCmdCurrentIndex = 0;
    }
    public void addPrePixCmd(Shuttle4WayWriteMap prefixCmd){
        // 현재 인덱스의 경로
        var originIndexCmd = ShuttlePathCmdList.get(reserveCmdCurrentIndex);
        // 생성된 경로가 원래 경로와 중복되지 않게 원래 경로를 수정.
        List<Cell> remainPath = new ArrayList<>(originIndexCmd.getPath().subList(prefixCmd.getPath().size(), originIndexCmd.getPath().size()));
        originIndexCmd.setPath(remainPath);
        originIndexCmd.setFromCell(prefixCmd.getToCell());

        // 생성된 경로를 현재 인덱스에 추가
        ShuttlePathCmdList.add(reserveCmdCurrentIndex, prefixCmd);

        reserveCmdSize = ShuttlePathCmdList.size();
    }
    public void addPrePixCmd(List<Shuttle4WayWriteMap> prefixCmdList){
        for(var cmd : prefixCmdList){
            addPrePixCmd(cmd);
        }
    }
}
