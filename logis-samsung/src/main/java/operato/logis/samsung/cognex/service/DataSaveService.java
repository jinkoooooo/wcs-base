package operato.logis.samsung.cognex.service;


import operato.logis.connector.gtr.entity.GtrToken;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

@Service
public class DataSaveService extends AbstractQueryService {

    public GtrToken getToken() {
        String sql = "select * from samsung.ecs_bcr_data";
        return this.queryManager.selectBySql(sql, null, GtrToken.class);
    }
}
