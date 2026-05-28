package operato.logis.connector.sap.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SapMappingMetaData {
    private String functionName;
    private boolean isSend = false;
    private Map<String, String> importParams;
    private Map<String, String> exportParams;

    // 여러 테이블을 지원하기 위한 구조
    private Map<String, List<Map<String, Object>>> tableDataMap;
    private Map<String, List<String>> fieldsMap;
    private Map<String, Map<String, String>> mappingMap;
    private Map<String, Map<String, String>> typeMap;

    private List<String> enDecryptFields;

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public boolean getIsSend() {
        return isSend;
    }

    public void setIsSend(boolean send) {
        isSend = send;
    }

    public Map<String, String> getImportParams() {
        return importParams;
    }

    public void setImportParams(Map<String, String> newImportParams) {
        validateParams("Import", this.importParams, newImportParams);
        this.importParams = newImportParams;
    }

    public Map<String, String> getExportParams() {
        return exportParams;
    }

    /**
     * SAP 처리 결과 메시지 타입 반환 (S=성공, E=오류 등)
     */
    public String getExportParamResult() {
        return exportParams != null ? exportParams.getOrDefault("O_MSGTY", "") : "";
    }

    /**
     * SAP 처리 결과 메시지 내용 반환
     */
    public String getExportParamDescription() {
        return exportParams != null ? exportParams.getOrDefault("O_MSGLIN", "") : "";
    }


    public void setExportParams(Map<String, String> newExportParams) {
        validateParams("Export", this.exportParams, newExportParams);
        this.exportParams = newExportParams;
    }

    public Map<String, List<Map<String, Object>>> getTableDataMap() {
        return tableDataMap;
    }

    public void setTableDataMap(Map<String, List<Map<String, Object>>> tableDataMap) {
        this.tableDataMap = tableDataMap;
    }

    public Map<String, List<String>> getFieldsMap() {
        return fieldsMap;
    }

    public void setFieldsMap(Map<String, List<String>> fieldsMap) {
        this.fieldsMap = fieldsMap;
    }

    public Map<String, Map<String, String>> getMappingMap() {
        return mappingMap;
    }

    public void setMappingMap(Map<String, Map<String, String>> mappingMap) {
        this.mappingMap = mappingMap;
    }

    public Map<String, Map<String, String>> getTypeMap() {
        return typeMap;
    }

    public void setTypeMap(Map<String, Map<String, String>> typeMap) {
        this.typeMap = typeMap;
    }

    public List<String> getEnDecryptFields() {
        return enDecryptFields;
    }

    public void setEnDecryptFields(List<String> enDecryptFields) {
        this.enDecryptFields = enDecryptFields;
    }

    private void validateParams(String label, Map<String, String> definedParams, Map<String, String> incomingParams) {
        if (definedParams == null || incomingParams == null) return;

        Set<String> definedKeys = definedParams.keySet();
        Set<String> incomingKeys = incomingParams.keySet();

        for (String incomingKey : incomingKeys) {
            if (!definedKeys.contains(incomingKey)) {
                throw new IllegalArgumentException(
                        String.format("[%s] - %s 파라미터에 정의되지 않은 키가 포함됨 → '%s'", this.functionName, label, incomingKey)
                );
            }
        }
    }

    @Override
    public String toString() {
        return "SapRfcFieldConfig{" +
                "functionName='" + functionName + '\'' +
                ", isSend=" + isSend +
                ", importParams=" + importParams +
                ", exportParams=" + exportParams +
                ", tableDataMap=" + tableDataMap +
                ", fieldsMap=" + fieldsMap +
                ", mappingMap=" + mappingMap +
                '}';
    }
}
