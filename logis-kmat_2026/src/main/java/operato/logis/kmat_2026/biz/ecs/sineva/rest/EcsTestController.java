package operato.logis.kmat_2026.biz.ecs.sineva.rest;

import operato.logis.kmat_2026.biz.wcs.kmat_2026.dto.ShuttleTestDto;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.handler.WcsMoveOrderHandler;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/ecs/shuttle/test")
public class EcsTestController {

    @Autowired
    protected WcsMoveOrderHandler wcsMoveOrderHandler;

    @PostMapping("/task/create")
    public TbWcsShuttleOrder createTask(@RequestBody ShuttleTestDto param) {
        return wcsMoveOrderHandler.createShuttleOrder(param.getCommand(), param.getAllocation());
    }
}