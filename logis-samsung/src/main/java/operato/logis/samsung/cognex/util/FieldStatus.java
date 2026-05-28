package operato.logis.samsung.cognex.util;

public class FieldStatus {

    private String fieldName; // 예: "TF1"
    private String status;    // 예: "ACTIVE" 또는 "INACTIVE"

    public FieldStatus(String fieldName, String status) {
        this.fieldName = fieldName;
        this.status = status;
    }

        // DB 저장 등을 위해 Getter 필요 시 추가
    public String getFieldName() { return fieldName; }
    public String getStatus() { return status; }
}
