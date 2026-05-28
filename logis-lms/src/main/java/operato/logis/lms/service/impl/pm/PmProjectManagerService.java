package operato.logis.lms.service.impl.pm;

import operato.logis.lms.entity.pm.TbPmProjectDetailStep;
import operato.logis.lms.entity.pm.TbPmProjectMain;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PmProjectManagerService extends AbstractQueryService {

    /**
     * 프로젝트 메인 조회
     * - from_date, to_date : end_year 범위 조건
     * - project_name : like (ILIKE)
     */
    public List<TbPmProjectMain> selectProjectMainInfo(Map<String, Object> params) {

        Integer fromYear = toIntOrNull(params.get("from_date"));
        Integer toYear   = toIntOrNull(params.get("to_date"));
        String projectName = toStrOrNull(params.get("project_name"));
        String teamName = toStrOrNull(params.get("owner_team_name"));
        String partName = toStrOrNull(params.get("owner_part_name"));

        Map<String, Object> param = new HashMap<>();
        param.put("from_year", fromYear);
        param.put("to_year", toYear);
        param.put("project_name", projectName);
        param.put("team_name", teamName);
        param.put("part_name", partName);

        String sql = """
            SELECT
                *
            FROM
                tb_pm_project_main
            WHERE 1=1
              AND (
                   end_year IS NULL
                   OR (
                        end_year >= COALESCE(:from_year, end_year)
                    AND end_year <= COALESCE(:to_year, end_year)
                   )
              )
              AND (
                  COALESCE(:project_name, '') = ''
                  OR project_name ILIKE ('%' || :project_name || '%')
              )
              AND (
                  COALESCE(:team_name, '') = ''
                  OR owner_team_name IS NULL
                  OR btrim(owner_team_name) = ''
                  OR owner_team_name = :team_name
              )
              AND (
                  COALESCE(:part_name, '') = ''
                  OR owner_part_name IS NULL
                  OR btrim(owner_part_name) = ''
                  OR owner_part_name = :part_name
              )
            ORDER BY
                end_year DESC NULLS LAST,
                start_year DESC NULLS LAST,
                project_name ASC
        """;


        return queryManager.selectListBySql(sql, param, TbPmProjectMain.class, 0, 0);
    }

    /**
     * 프로젝트 디테일 스탭 조회
     * - main_id : 부모 id
     */
    public List<TbPmProjectDetailStep> selectProjectDetailStepInfo(Map<String, Object> params) {

        String mainId = toStrOrNull(params.get("main_id"));

        Map<String, Object> param = new HashMap<>();
        param.put("main_id", mainId);

        String sql = """
            SELECT
                      *
                  FROM
                      tb_pm_project_detail_step
                  WHERE 1=1
                      AND project_main_id = :main_id::text
                  ORDER BY
                      step_cd ASC
        """;

        return queryManager.selectListBySql(sql, param, TbPmProjectDetailStep.class, 0, 0);
    }

    private static String toStrOrNull(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer toIntOrNull(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }
}
