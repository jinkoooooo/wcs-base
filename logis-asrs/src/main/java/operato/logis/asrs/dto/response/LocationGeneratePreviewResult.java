package operato.logis.asrs.dto.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 로케이션 자동생성 Preview 결과 DTO.
 *
 * <p>
 * 실제 insert 는 하지 않고, 생성 대상 총 건수 / 기존 중복 건수 / 생성 가능 건수와
 * 샘플 로케이션 코드를 응답한다.
 * </p>
 */
public class LocationGeneratePreviewResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 로케이션 프로파일 ID */
    private String locationProfileId;

    /** 영역 ID */
    private String areaId;

    /** 전체 대상 건수 */
    private int totalTargetCount;

    /** 이미 존재하는 좌표 건수 */
    private int existingCount;

    /** 실제 생성 가능 건수 */
    private int creatableCount;

    /** 샘플 로케이션 코드 목록 */
    private List<String> previewLocationCodes = new ArrayList<>();

    public String getLocationProfileId() {
        return locationProfileId;
    }

    public void setLocationProfileId(String locationProfileId) {
        this.locationProfileId = locationProfileId;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public int getTotalTargetCount() {
        return totalTargetCount;
    }

    public void setTotalTargetCount(int totalTargetCount) {
        this.totalTargetCount = totalTargetCount;
    }

    public int getExistingCount() {
        return existingCount;
    }

    public void setExistingCount(int existingCount) {
        this.existingCount = existingCount;
    }

    public int getCreatableCount() {
        return creatableCount;
    }

    public void setCreatableCount(int creatableCount) {
        this.creatableCount = creatableCount;
    }

    public List<String> getPreviewLocationCodes() {
        return previewLocationCodes;
    }

    public void setPreviewLocationCodes(List<String> previewLocationCodes) {
        this.previewLocationCodes = previewLocationCodes;
    }

    public void addPreviewLocationCode(String locationCode) {
        this.previewLocationCodes.add(locationCode);
    }
}