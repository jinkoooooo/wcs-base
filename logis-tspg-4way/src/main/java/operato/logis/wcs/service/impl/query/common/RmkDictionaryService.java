package operato.logis.wcs.service.impl.query.common;

import operato.logis.wcs.entity.RmkDictionary;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 공통 코드 Dictionary 조회 서비스.
 *
 * common_codes + common_code_details 에서 코드→명칭 매핑을 조회한다.
 * 프론트 그리드에서 코드값을 사람이 읽을 수 있는 명칭으로 표시하는 데 사용.
 */
@Service
public class RmkDictionaryService extends AbstractQueryService {

    /**
     * 공통 코드 Dictionary 전체 조회. parentName 기준 그룹핑한 { name, dictionary } 배열 반환.
     */
    public List<RmkDictionary> getRmkDictionary() {
        // 도메인 단위 조회
        Map<String, Object> param = ValueUtil.newMap("domainId", Domain.currentDomainId());

        String sql = """
            SELECT p.name AS parent_name,
                   d.name AS key,
                   d.description AS value
              FROM common_code_details d
              JOIN common_codes p ON p.id = d.common_code_id
             WHERE p.domain_id = :domainId
             ORDER BY p.name, d.name
            """;

        List<RmkDictionary> details = this.queryManager.selectListBySql(sql, param, RmkDictionary.class, 0, 0);

        // parentName 집합 추출 (입력 순서 유지)
        Set<String> parentNames = new LinkedHashSet<>();
        for (RmkDictionary detail : details) {
            if (ValueUtil.isNotEmpty(detail.getParentName())) {
                parentNames.add(detail.getParentName());
            }
        }

        // parentName 별 그룹핑
        List<RmkDictionary> result = new ArrayList<>();
        for (String parentName : parentNames) {
            RmkDictionary data = new RmkDictionary();
            Map<String, String> dic = new HashMap<>();

            data.setName(parentName);
            for (RmkDictionary detail : details) {
                if (parentName.equals(detail.getParentName())) {
                    String key = detail.getKey();
                    String value = detail.getValue();
                    if (ValueUtil.isNotEmpty(key)) {
                        dic.put(key, value);
                    }
                }
            }
            data.setDictionary(dic);
            result.add(data);
        }

        return result;
    }
}
