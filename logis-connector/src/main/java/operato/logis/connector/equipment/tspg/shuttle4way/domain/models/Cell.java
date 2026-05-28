package operato.logis.connector.equipment.tspg.shuttle4way.domain.models;

import lombok.Data;

@Data
public class Cell {
    private String id;
    private int locX;               // X축
    private int locY;               // Y축
    private int locLevel;           // 층
    private boolean isUsed;         // 사용유무
    private boolean isDriveLine;    // 주행전용라인 유무
    private boolean hasCargo;       // 화물 적재 유무

    private String reservedBy;      // 예약 셔틀 ID

    public Cell(int locX, int locY, boolean isDriveLine) {
        this.locX = locX;
        this.locY = locY;
        this.isDriveLine = isDriveLine;
    }

    public Cell(int locX, int locY) {
        this.locX = locX;
        this.locY = locY;
    }

}
