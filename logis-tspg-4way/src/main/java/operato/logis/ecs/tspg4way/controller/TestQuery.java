package operato.logis.ecs.tspg4way.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttlePlc;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.TspgConveyorPlc;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import operato.logis.ecs.tspg4way.entity.TbEqGroupMst;
import operato.logis.ecs.tspg4way.domain.registry.TspgConveyorPlcRegistry;
import operato.logis.ecs.tspg4way.domain.registry.TspgShuttlePlcRegistry;
import operato.logis.ecs.tspg4way.repository.EcsRepository;
import operato.logis.ecs.tspg4way.service.TspgConveyorPlcReadService;
import operato.logis.ecs.tspg4way.service.TspgShuttlePlcReadService;

@RestController
@Slf4j
@RequestMapping("/api/tspg/query/test")
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
            var groupMstList =  repository.findAll(TbEqGroupMst.class, "tb_eq_group_mst");
            if(!groupMstList.isEmpty()){
                groupMstList.stream().forEach(mst -> {
                    log.info(mst.getId());
                });
            }else{
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

            if(!driveLineList.isEmpty()){
                Integer[] array = driveLineList.toArray(new Integer[0]);
                log.info(array[0].toString());
            }else{
                log.info("driveLineList is empty");
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }


    private final TspgShuttlePlcRegistry tspgShuttlePlcRegistry;

    @GetMapping("/select3")
    public ResponseEntity<?> select3() {
        try {
            Tspg4WayShuttlePlc carPlc = tspgShuttlePlcRegistry.getEquipment("SHUTTLE_CAR_1");
            TspgShuttlePlcReadService service = new TspgShuttlePlcReadService();
            service.readShuttleMemoryTest(carPlc);
            service.logInfo(carPlc);

             var a= service.findById("SHUTTLE_CAR_1");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValueAsString(a);
            log.info(a.toString());


            service.updatePlcStatus(carPlc);

            var b= service.findById("SHUTTLE_CAR_1");
            mapper.writeValueAsString(b);
            log.info(b.toString());

        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }


    private final TspgConveyorPlcRegistry tspgConveyorPlcRegistry;

    @GetMapping("/select4")
    public ResponseEntity<?> select4() {
        try {
            TspgConveyorPlc cvPlc = tspgConveyorPlcRegistry.getEquipment("CV_1");
            TspgConveyorPlcReadService service = new TspgConveyorPlcReadService();
            service.readConveyorMemoryTest(cvPlc);
            service.logInfo(cvPlc);

            var a= service.findListById("CV_1");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValueAsString(a);
            log.info(a.toString());

            service.updatePlcStatus(cvPlc);

            var b= service.findListById("CV_1");
            mapper.writeValueAsString(b);
            log.info(b.toString());

        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
