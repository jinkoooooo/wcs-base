package operato.logis.ecs.base.ecs;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.entity.TbEqGroupMst;
import operato.logis.ecs.base.ecs.equipment.ConveyorPlc;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;
import operato.logis.ecs.base.ecs.plc.conveyor.ConveyorPlcManager;
import operato.logis.ecs.base.ecs.plc.crane.StackerCranePlcManager;
import operato.logis.ecs.base.ecs.repository.EcsRepository;
import operato.logis.ecs.base.ecs.service.StackerCranePlcReadService;
import operato.logis.ecs.base.ecs.service.conveyor.ConveyorPlcReadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// todo: 로직검토
@RestController
@Slf4j
@RequestMapping("/api/ecs-base/query/test")
@RequiredArgsConstructor
public class TestQuery {

    private final EcsRepository repository;

    @GetMapping("/boot")
    public ResponseEntity<?> boot() {
        try {
            log.info("=================boot=================");
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/select")
    public ResponseEntity<?> select() {
        try {
            var groupMstList = repository.findAll(TbEqGroupMst.class, "tb_eq_group_mst");
            if (!groupMstList.isEmpty()) {
                groupMstList.stream().forEach(mst -> {
                    log.info(mst.getId());
                });
            } else {
                log.info("groupMstList is empty");
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/select2")
    public ResponseEntity<?> select2() {
        try {
            var driveLineList = repository.findDriveOnlyLineByEqIdAndLevel("RACK_1", 1);

            if (!driveLineList.isEmpty()) {
                Integer[] array = driveLineList.toArray(new Integer[0]);
                log.info(array[0].toString());
            } else {
                log.info("driveLineList is empty");
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private final StackerCranePlcManager stackerCranePlcManager;

    @GetMapping("/select3")
    public ResponseEntity<?> select3() {
        try {
            StackerCranePlc cranePlc = stackerCranePlcManager.getEquipment("STACKER_CRANE_1");
            StackerCranePlcReadService service = new StackerCranePlcReadService();
            service.readCraneMemoryTest(cranePlc);
            service.logInfo(cranePlc);

            var a = service.findById("STACKER_CRANE_1");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValueAsString(a);
            log.info(a.toString());


            service.updatePlcStatus(cranePlc);

            var b = service.findById("STACKER_CRANE_1");
            mapper.writeValueAsString(b);
            log.info(b.toString());

        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private final ConveyorPlcManager conveyorPlcManager;

    @GetMapping("/select4")
    public ResponseEntity<?> select4() {
        try {
            ConveyorPlc cvPlc = conveyorPlcManager.getEquipment("CV_1");
            ConveyorPlcReadService service = new ConveyorPlcReadService();
            service.readConveyorMemoryTest(cvPlc);
            service.logInfo(cvPlc);

            var a = service.findListById("CV_1");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValueAsString(a);
            log.info(a.toString());

            service.updatePlcStatus(cvPlc);

            var b = service.findListById("CV_1");
            mapper.writeValueAsString(b);
            log.info(b.toString());

        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
