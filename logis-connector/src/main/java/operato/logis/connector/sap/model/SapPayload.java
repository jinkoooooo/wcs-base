package operato.logis.connector.sap.model;

import java.util.List;
import java.util.Map;

public class SapPayload {
	private boolean isSend;
	private Map<String, String> importParams;
	private Map<String, List<Map<String, Object>>> tableDataMap;

	public SapPayload() {}
	public SapPayload(boolean isSend, Map<String, String> importParams, Map<String, List<Map<String, Object>>> tableDataMap) {
		this.isSend = isSend;
		this.importParams = importParams;
		this.tableDataMap = tableDataMap;
	}

	public boolean isSend() { return isSend; }
	public void setSend(boolean send) { this.isSend = send; }

	public Map<String, String> getImportParams() { return importParams; }
	public void setImportParams(Map<String, String> importParams) { this.importParams = importParams; }

	public Map<String, List<Map<String, Object>>> getTableDataMap() { return tableDataMap; }
	public void setTableDataMap(Map<String, List<Map<String, Object>>> tableDataMap) { this.tableDataMap = tableDataMap; }
}