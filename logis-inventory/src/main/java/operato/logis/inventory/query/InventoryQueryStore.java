package operato.logis.inventory.query;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * Inventory 쿼리
 * 
 * @author shortstop
 */
@Component
public class InventoryQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/inventory/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/inventory/query/ansi/";
	}

	/**
	 * 입고 위치 추천 SQL
	 */
	public String getCalculateInboundLocationSql() {
		return this.getQueryByPath("location/CalculateInboundLocation");
	}

	/**
	 * 출고 재고 추천 SQL
	 */
	public String getCalculateOutboundStockSql() {
		return this.getQueryByPath("stock/CalculateOutboundStock");
	}

	/**
	 * 정렬 위치 추천 SQL
	 */
	public String getCalculateSortLocation2WaySql() {
		return this.getQueryByPath("location/CalculateSortLocation2Way");
	}
	public String getCalculateSortLocation4WaySql() {
		return this.getQueryByPath("location/CalculateSortLocation4Way");
	}
}