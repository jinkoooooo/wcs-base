package equipment.tspg;


import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Cell;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttleCar;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayPathService;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayWriteMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class Shuttle4wayPathSearchTest {

    private int maxX = 16;
    private int maxY = 11;
    private int[] driveLine = new int[]{6,10};
    private Cell[][] map = new Cell[maxX][maxY];
    private Shuttle4WayPathService pathService;

    public List<Cell> getReservablePrefix(List<Cell> path, int safetyBuffer) {
        List<Cell> result = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            Cell cell = path.get(i);
            // 이미 예약되어 있으면 STOP
            if (cell.getReservedBy() != null) {
                break;
            }
            result.add(cell);
        }
        // 🔥 핵심: 안전거리 확보
        int safeSize = Math.max(0, result.size() - safetyBuffer);
        return result.subList(0, safeSize);
    }

    private void setTestConfig(){
        pathService = new Shuttle4WayPathService(maxX, maxY,driveLine , 1,2,1);
        // 기둥
        pathService.updateUseCell(4,7,false);
        pathService.updateUseCell(10,7,false);
        // 셀 없는 공간
        pathService.updateUseCell(1,1,false);
        pathService.updateUseCell(1,2,false);
        pathService.updateUseCell(1,3,false);
        pathService.updateUseCell(1,4,false);
        pathService.updateUseCell(2,1,false);
        pathService.updateUseCell(2,2,false);
        pathService.updateUseCell(2,3,false);
        pathService.updateUseCell(2,4,false);
        pathService.updateUseCell(1,3,false);
        pathService.updateUseCell(2,3,false);
        pathService.updateUseCell(1,4,false);
        pathService.updateUseCell(2,4,false);
        // 화물 있음
        // pathService.updateCargoCell(5,6,true);
        // pathService.updateCargoCell(7,5,true);
    }

    @Test
    void 입고_경로_탐색() {
        setTestConfig();
        var path = getPath();
        System.out.println("=== PATH ===");
        path.forEach(c ->
                System.out.println("(" + c.getLocX() + "," + c.getLocY() + ")")
        );
        assert !path.isEmpty();
    }

    @Test
    void 셔틀_지시_생성(){
        setTestConfig();
        List<Cell> path = getPath();
        List<Shuttle4WayWriteMap> cmds = getShuttleCmd(path);
        System.out.println("=== MOVE COMMANDS ===");
        for (Shuttle4WayWriteMap cmd : cmds) {
            System.out.println(cmd.toString());
            cmd.getPath().forEach(c -> {
                System.out.println("(" + c.getLocX() + "," + c.getLocY() + ")");
            });
        }
        assert !path.isEmpty() && !cmds.isEmpty();
    }

    @Test
    void 셀_에약_시나리오_테스트() throws InterruptedException {
        setTestConfig();
        List<Cell> path = getPath();
        List<Shuttle4WayWriteMap> cmdList = getShuttleCmd(path);

        Tspg4WayShuttleCar car1 = new Tspg4WayShuttleCar("car1");
        Tspg4WayShuttleCar car2 = new Tspg4WayShuttleCar("car2");

        // 셔틀 카 1에 지시 가능한 상황이 왔을 때
        // 셔틀카 지시
        cmdCar(car1, cmdList);

        // 셔틀 car1 작업 중
        System.out.println("=== CAR1 ORDER WORKING ===");

        // 셔틀 car1 작업 완료 보고
        System.out.println("=== CAR1 ORDER COMPLETE ===");
        // - 셀 예약 해제
        setReserveCancelCell(cmdList.get(car1.getReserveCmdCurrentIndex()).getPath(), car1.getShuttleCarId());
        // - 셔틀 카 지시 완료 업데이트
        updateCarStatus(car1);

        // 셔틀 car1 예약 지시 남았는지 확인
        if(cmdList.size() == car1.getReserveCmdCurrentIndex()){
            System.out.println("CAR1 예약 지시 수행 완료");
            assert true;
        }else{
            // 셔틀 카 1에 지시 가능한 상황이 왔을 때
            cmdCar(car1, cmdList);
            // ...
            //  셔틀 카 지시 완료시 이런식으로 반복
        }
    }

    @Test
    void 셔틀카_2대_시나리오_테스트(){
        setTestConfig();
        List<Cell> path = getPath();
        List<Shuttle4WayWriteMap> cmdList = getShuttleCmd(path);
        Tspg4WayShuttleCar car1 = new Tspg4WayShuttleCar("car1");
        Tspg4WayShuttleCar car2 = new Tspg4WayShuttleCar("car2");

        System.out.println("=== STEP 1: CAR1 지시 ===");
        boolean car1Result = cmdCar(car1, cmdList);
        Assertions.assertTrue(car1Result);

        System.out.println("=== STEP 2: CAR2 지시 시도 ===");
        boolean car2Result = cmdCar(car2, cmdList);
        Assertions.assertFalse(car2Result);

        System.out.println("=== STEP 3: CAR1 작업 완료 ===");
        setReserveCancelCell(
                cmdList.get(car1.getReserveCmdCurrentIndex()).getPath(),
                car1.getShuttleCarId()
        );
        updateCarStatus(car1);

        System.out.println("=== STEP 4: CAR2 재시도 ===");
        boolean car2RetryResult = cmdCar(car2, cmdList);
        Assertions.assertTrue(car2RetryResult);
    }

    // 셔틀 카 상태 업데이트
    private void updateCarStatus(Tspg4WayShuttleCar car1) {
        if(car1.getReserveCmdCurrentIndex() + 1 <= car1.getReserveCmdSize()){
            car1.setReserveCmdCurrentIndex(car1.getReserveCmdCurrentIndex() + 1);
        }
    }


    // 카 지시 등록
    private boolean cmdCar(Tspg4WayShuttleCar car, List<Shuttle4WayWriteMap> cmdList) {
        if(car.getReserveCmdSize() == 0){
            car.setReserveCmdSize(cmdList.size());
        }
        Shuttle4WayWriteMap curCmd = cmdList.get(car.getReserveCmdCurrentIndex());
        List<Cell> curPath = curCmd.getPath();
        // 셀 예약 여부 확인
        if(!canUseCellListOfPath(curPath, car.getShuttleCarId())){
            System.out.println("지시의 현재 셀들이 예약상태입니다. 완료될 때까지 대기합니다.");
            return false;
        }
        // 셀 예약
        setReserveCell(curPath, car.getShuttleCarId());
        // 지시 전송
        sendOrder(curCmd);
        return true;
    }

    // 지시 전송
    private void sendOrder(Shuttle4WayWriteMap cmd) {
        System.out.println("=== SEND ORDER ===");
        System.out.println(cmd.toString());
    }
    // 셀 예약
    private void setReserveCell(List<Cell> path, String carId) {
        System.out.println("=== RESERVE CELL BY " + carId + " ===");
        path.forEach(c -> {
            c.setReservedBy(carId);
            System.out.println("(" + c.getLocX() + "," + c.getLocY() + ")");
        });
    }
    // 셀 예약 해제
    private void setReserveCancelCell(List<Cell> path, String carId) {
        System.out.println("=== RESERVE CANCEL CELL BY " + carId + " ===");
        path.forEach(c -> {
            if (carId.equals(c.getReservedBy())) {
                c.setReservedBy(null);
            }
            System.out.println("(" + c.getLocX() + "," + c.getLocY() + ")");
        });
    }
    // 셀 이용 가능한 상황인지
    private boolean canUseCellListOfPath(List<Cell> cellList, String carId) {
        for (Cell c : cellList) {
            if (c.getReservedBy() != null &&
                    !carId.equals(c.getReservedBy())) {
                return false;
            }
        }
        return true;
    }


    // 경로 탐색
    private List<Cell> getPath(){
        return pathService.findPath(true,2, 6, 2, 7);
    }


    // 셔틀 명령 생성
    private List<Shuttle4WayWriteMap> getShuttleCmd(List<Cell> path){
        return pathService.buildMoveCommands(path);
    }
}
