package operato.logis.samsung.service.mw;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;

@Component
public class DemoMwDataService extends AbstractQueryService {

    @Transactional
    public boolean resetDemoDataSet() {

        try {
            // TbMwChute 리셋
            queryManager.executeBySql("update tb_mw_chutes set pallet_sequence = null, order_id = null, item_code = null, box_qty = 0 where is_use = true", null);
            // tb_mw_xyz_order 리셋
            queryManager.executeBySql("update tb_mw_xyz_order set process_status = 39 where process_status < 33", null);
            // tb_mw_box plc_seq 리셋
            queryManager.executeBySql("UPDATE tb_mw_box SET plc_seq_no = 'END-' || plc_seq_no WHERE plc_seq_no NOT LIKE 'END%'", null);
            // tb_mw_inbound_delivery 리셋
            queryManager.executeBySql("UPDATE tb_mw_inbound_delivery SET inbound_status = 1", null);

            return true;
        } catch (Exception e) {
            logger.error("DemoMwDataService.resetDemoDataSet => {}", e.toString(), e);
            return false;
        }
    }

    public boolean generateDemoBatchSet() {
        try {
            // TODO : TbMwInboundDelivery 임의 생성 로직 추가

            return true;
        }catch (Exception e){
            logger.error("TestBatchDataGenerator.generateDemoBatchSet => {}", e.toString());
            e.printStackTrace();
            return false;
        }
    }
}