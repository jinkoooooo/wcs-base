package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.entity.TbEcsTaskProcess;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.Date;

@Service
public class TbEcsTaskProcessService extends AbstractQueryService {

    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(TbEcsTaskProcessService.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TbEcsTaskProcess saveReqCommandHistory(TbWcsOrder task, String interfaceName) {
        TbEcsTaskProcess history = new TbEcsTaskProcess();
        history.setReqTime(new Date());
        history.setReqType("REQ");
        history.setReqCod(task.getOrderId());
        history.setTaskId(task.getOrderId());
        history.setEquipType(task.getEquipType());
        history.setCurrentPositionCod(task.getCurrentPositionCod());
        history.setToPositionCod(task.getToPositionCod());
        history.setEquipId(task.getEquipId());
        history.setDomainId(7L);
        history.setEventType(interfaceName);

        this.queryManager.insert(history);
        return history;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveResTaskCallback(TbWcsOrder order, String eventType) {
        TbEcsTaskProcess history = new TbEcsTaskProcess();
        history.setReqType("RES");
        history.setTaskId(order.getOrderId());
        history.setEventType(eventType);
        history.setReqCod(order.getTaskId() + "-" + eventType);
        history.setReqTime(new Date());
        history.setCurrentPositionCod(order.getCurrentPositionCod());
        history.setToPositionCod(order.getToPositionCod());
        history.setEquipType(order.getEquipType());

        this.queryManager.insert(history);
    }
}
