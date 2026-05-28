package operato.logis.asrs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.asrs.entity.TbAcLocationProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.asrs.entity.WcsAsrsLayout;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

@Slf4j
@Service
@RequiredArgsConstructor
public class WcsAsrsLayoutService extends AbstractQueryService {

    @Transactional
    public void saveLayout(WcsAsrsLayout layoutEntity) {
        try {
            if (layoutEntity.getId() != null && !layoutEntity.getId().isEmpty()) {

                WcsAsrsLayout existingLayout = this.queryManager.select(WcsAsrsLayout.class, layoutEntity.getId());

                if (existingLayout != null) {
                    existingLayout.setCenterId(layoutEntity.getCenterId());
                    existingLayout.setZoneId(layoutEntity.getZoneId());
                    existingLayout.setLayoutVersion(layoutEntity.getLayoutVersion());
                    existingLayout.setIsActive(layoutEntity.getIsActive());
                    existingLayout.setLayoutData(layoutEntity.getLayoutData());

                    this.queryManager.update(existingLayout);
                    log.info("AS/RS 레이아웃 DB 업데이트(수정) 완료: ID={}", existingLayout.getId());
                }
            } else {
                this.queryManager.insert(layoutEntity);
                log.info("AS/RS 레이아웃 DB 신규 저장 완료");
            }
        } catch (Exception e) {
            log.error("레이아웃 데이터를 DB에 저장/수정하는 중 오류 발생", e);
            throw new RuntimeException("레이아웃 저장 실패: " + e.getMessage());
        }
    }

    private void syncLocationProfile(WcsAsrsLayout layout) throws Exception {
        if (layout.getLayoutData() == null || layout.getLayoutData().isEmpty()) return;

        // 🔥 1. 클래스 상단 선언 없이, 메서드 내부에서 직접 ObjectMapper 생성 후 파싱 (가장 깔끔한 방법)
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, Object> layoutMap = mapper.readValue(
                layout.getLayoutData(),
                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {}
        );

        /* * 💡 만약 위 방식도 에러가 나거나 프로젝트 표준이 Gson 이라면 아래 코드를 대신 사용하세요!
         * com.google.gson.Gson gson = new com.google.gson.Gson();
         * java.util.Map<String, Object> layoutMap = gson.fromJson(layout.getLayoutData(), java.util.Map.class);
         */

        java.util.Map<String, Object> rackConfig = (java.util.Map<String, Object>) layoutMap.get("rackConfig");

        if (rackConfig == null) return;

        // 매핑 규칙 (Area Code, Profile Code)
        String areaCode = layout.getCenterId();
        String profileCode = layout.getZoneId() + "_" + layout.getLayoutVersion(); // 예: Z01_V1.0

        log.info("🔄 프로파일 자동 동기화 시작 - Area: {}, Profile: {}", areaCode, profileCode);

        // 기존 프로파일이 존재하는지 조회
        xyz.elidom.dbist.dml.Query query = new xyz.elidom.dbist.dml.Query();
        query.addFilter("areaCode", areaCode);
        query.addFilter("profileCode", profileCode);
        TbAcLocationProfile profile = this.queryManager.selectByCondition(TbAcLocationProfile.class, query);

        boolean isNew = (profile == null);
        if (isNew) {
            profile = new TbAcLocationProfile();
            profile.setAreaId(areaCode);
            profile.setProfileCode(profileCode);
        }

        // 🔥 JSON (rackConfig) -> Profile 기준값 자동 매핑
        // JSON의 숫자 값들이 Integer가 아닐 수 있으므로 Number로 캐스팅 후 intValue() 처리하면 가장 안전합니다.
        profile.setAisleStart(1);
        profile.setAisleEnd(((Number) rackConfig.get("scCount")).intValue());

        profile.setBayStart(1);
        profile.setBayEnd(((Number) rackConfig.get("columns")).intValue());

        profile.setLevelStart(1);
        profile.setLevelEnd(((Number) rackConfig.get("tiers")).intValue());

        profile.setDepthStart(1);
        profile.setDepthEnd(((Number) rackConfig.get("rowsPerSide")).intValue());

        // 기본 속성 세팅
        profile.setSideCodes("L,R");
        profile.setLocationType("STORAGE");
        profile.setInboundAllowedYn("Y");
        profile.setOutboundAllowedYn("Y");
        profile.setMixedLoadYn("N");

        // DB 저장 또는 업데이트
        if (isNew) {
            this.queryManager.insert(profile);
            log.info("✅ 신규 로케이션 프로파일 생성 완료: {}", profileCode);
        } else {
            this.queryManager.update(profile);
            log.info("✅ 기존 로케이션 프로파일 업데이트 완료: {}", profileCode);
        }
    }
}