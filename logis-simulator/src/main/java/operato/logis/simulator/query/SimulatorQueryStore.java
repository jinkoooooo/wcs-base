package operato.logis.simulator.query;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * Simulator 쿼리
 * 
 * @author shortstop
 */
@Component
public class SimulatorQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/simulator/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/simulator/query/ansi/";
	}
}