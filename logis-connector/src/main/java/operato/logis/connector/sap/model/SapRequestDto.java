package operato.logis.connector.sap.model;

import java.util.List;
import java.util.Map;

public class SapRequestDto {
    private String functionName;
    private boolean isSend;
    private Map<String, String> importParams;
    private Map<String, List<Map<String, Object>>> tableDataMap;

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public boolean getIsSend() {
        return isSend;
    }

    public void setIsSend(boolean isSend) {
        this.isSend = isSend;
    }

    public Map<String, String> getImportParams() {
        return importParams;
    }

    public void setImportParams(Map<String, String> importParams) {
        this.importParams = importParams;
    }

    public Map<String, List<Map<String, Object>>> getTableDataMap() {
        return tableDataMap;
    }

    public void setTableDataMap(Map<String, List<Map<String, Object>>> tableDataMap) {
        this.tableDataMap = tableDataMap;
    }
}
