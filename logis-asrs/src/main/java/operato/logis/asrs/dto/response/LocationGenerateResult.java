package operato.logis.asrs.dto.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 로케이션 자동생성 실행 결과 DTO.
 *
 * <p>
 * 실제 생성 후 총 요청 건수 / 생성 건수 / 중복 스킵 건수와
 * 샘플 생성/스킵 로케이션 코드를 응답한다.
 * </p>
 */
public class LocationGenerateResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 로케이션 프로파일 ID */
    private String locationProfileId;

    /** 영역 ID */
    private String areaId;

    /** 전체 대상 건수 */
    private int requestedCount;

    /** 실제 생성 건수 */
    private int createdCount;

    /** 중복으로 스킵한 건수 */
    private int skippedCount;

    /** 샘플 생성 로케이션 코드 목록 */
    private List<String> createdLocationCodes = new ArrayList<>();

    /** 샘플 스킵 로케이션 코드 목록 */
    private List<String> skippedLocationCodes = new ArrayList<>();

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

    public int getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<String> getCreatedLocationCodes() {
        return createdLocationCodes;
    }

    public void setCreatedLocationCodes(List<String> createdLocationCodes) {
        this.createdLocationCodes = createdLocationCodes;
    }

    public List<String> getSkippedLocationCodes() {
        return skippedLocationCodes;
    }

    public void setSkippedLocationCodes(List<String> skippedLocationCodes) {
        this.skippedLocationCodes = skippedLocationCodes;
    }

    public void addCreatedLocationCode(String locationCode) {
        this.createdLocationCodes.add(locationCode);
    }

    public void addSkippedLocationCode(String locationCode) {
        this.skippedLocationCodes.add(locationCode);
    }
}