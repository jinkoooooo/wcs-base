package operato.logis.asrs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.asrs.entity.LocationGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryLocationService extends AbstractQueryService {

    @Transactional
    public void generateLocations(List<LocationGenerator> locations) {
        if (locations == null || locations.isEmpty()) {
            throw new RuntimeException("생성할 로케이션 데이터가 없습니다.");
        }

        // 1. 중복 생성 방지 로직 (페이로드의 첫 번째 데이터에서 존 그룹을 추출)
        String locGroup = locations.get(0).getLocGroup();

        // Elidom Query 객체를 사용하여 해당 존(locGroup)에 데이터가 있는지 카운트
        Query query = new Query();
        query.addFilter("locGroup", locGroup);

        int existCount = this.queryManager.selectSize(LocationGenerator.class, query);

        if (existCount > 0) {
            log.warn("중복 생성 차단: 이미 해당 구역({})에 로케이션이 존재합니다.", locGroup);
            throw new RuntimeException("해당 구역(" + locGroup + ")에는 이미 " + existCount + "개의 로케이션이 존재합니다. 생성을 취소합니다.");
        }

        // 2. 안전하게 신규 Insert 수행
        log.info("로케이션 자동 생성 시작: 총 {}건, 구역: {}", locations.size(), locGroup);

        for (LocationGenerator loc : locations) {
            // ID가 없으면 프레임워크(UUID 규칙)가 자동으로 부여하며 순수 Insert 처리됨
            this.queryManager.insert(loc);
        }

        log.info("로케이션 자동 생성 완료!");
    }
}