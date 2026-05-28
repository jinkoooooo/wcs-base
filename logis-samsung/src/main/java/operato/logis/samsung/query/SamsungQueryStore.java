package operato.logis.samsung.query;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * WCS 쿼리
 * 
 * @author shortstop
 */
@Component
public class SamsungQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/samsung/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/samsung/query/ansi/";
	}

	/**
	 * 일별 리포트 상단 KPI 요약
	 */
	public String SelectDailyReportSummary() {
		return this.getQueryByPath("report/daily_report_summary");
	}

	/**
	 * 일별 리포트 타임라인
	 */
	public String SelectDailyReportTimeline() {
		return this.getQueryByPath("report/daily_report_timeline");
	}

	/**
	 * 일별 리포트 BCR 원본
	 */
	public String SelectDailyReportBcrRaw() {
		return this.getQueryByPath("report/daily_report_bcr_raw");
	}

	/**
	 * 일별 리포트 SORTER 원본
	 */
	public String SelectDailyReportSorterRaw() {
		return this.getQueryByPath("report/daily_report_sorter_raw");
	}

	/**
	 * 일별 리포트 PALLETIZED 원본
	 */
	public String SelectDailyReportPalletizedRaw() {
		return this.getQueryByPath("report/daily_report_palletized_raw");
	}

	/**
	 * 가용 Chute 조회
	 */
	public String getAvailableChute() {
		return """
				WITH AVAILABLE_CHUTES AS (
				    -- 조건 1, 2를 만족하는 슈트를 먼저 필터링합니다.
				    SELECT
				        ID,
				        START_POINT_CD,
				        UPDATED_AT
				    FROM
				        TB_MW_CHUTES
				    WHERE
				        (ITEM_CODE IS NULL OR ITEM_CODE = '') -- 1. ITEM_CODE가 NULL이거나 빈 문자열
				      AND IS_USE = TRUE -- 2. IS_USE가 TRUE
				),
				     GROUPED_CHUTES AS (
				         -- 필터링된 슈트를 START_POINT_CD 별로 그룹화하고 각 그룹의 개수를 계산합니다.
				         SELECT
				             START_POINT_CD,
				             COUNT(*) AS CHUTE_COUNT
				         FROM
				             AVAILABLE_CHUTES
				         GROUP BY
				             START_POINT_CD
				     )
				-- 최종적으로 슈트를 선택합니다.
				SELECT
				    C.ID,
				    C.START_POINT_CD,
				    C.END_POINT_CD,
				    C.PALLET_SEQUENCE,
				    C.ITEM_CODE,
				    C.IS_USE,
				    C.BOX_QTY,
				    C.DOMAIN_ID,
				    C.CREATOR_ID,
				    C.UPDATER_ID,
				    C.CREATED_AT,
				    C.UPDATED_AT
				FROM
				    TB_MW_CHUTES C
				        JOIN
				    GROUPED_CHUTES GC ON C.START_POINT_CD = GC.START_POINT_CD
				WHERE
				    (C.ITEM_CODE IS NULL OR C.ITEM_CODE = '') AND C.IS_USE = TRUE
				ORDER BY
				    GC.CHUTE_COUNT DESC, -- 우선순위 1: 미사용 팔렛타이저 우선 사용
				    C.UPDATED_AT ASC     -- 우선순위 2: 사용한지 가장 오래된 Pallet Conveyor 우선 사용
				LIMIT 1
				FOR UPDATE
				""";
	}

	/**
	 * 가용 Chute List 조회
	 */
	public String getAvailableChuteList() {
		return """
				WITH AVAILABLE_CHUTES AS (
				    -- 조건 1, 2를 만족하는 슈트를 먼저 필터링합니다.
				    SELECT
				        ID,
				        START_POINT_CD,
				        UPDATED_AT
				    FROM
				        TB_MW_CHUTES
				    WHERE
				        (ITEM_CODE IS NULL OR ITEM_CODE = '') -- 1. ITEM_CODE가 NULL이거나 빈 문자열
				      AND IS_USE = TRUE -- 2. IS_USE가 TRUE
				),
				     GROUPED_CHUTES AS (
				         -- 필터링된 슈트를 START_POINT_CD 별로 그룹화하고 각 그룹의 개수를 계산합니다.
				         SELECT
				             START_POINT_CD,
				             COUNT(*) AS CHUTE_COUNT
				         FROM
				             AVAILABLE_CHUTES
				         GROUP BY
				             START_POINT_CD
				     )
				-- 최종적으로 슈트를 선택합니다.
				SELECT
				    C.ID,
				    C.START_POINT_CD,
				    C.END_POINT_CD,
				    C.PALLET_SEQUENCE,
				    C.ITEM_CODE,
				    C.IS_USE,
				    C.BOX_QTY,
				    C.DOMAIN_ID,
				    C.CREATOR_ID,
				    C.UPDATER_ID,
				    C.CREATED_AT,
				    C.UPDATED_AT
				FROM
				    TB_MW_CHUTES C
				        JOIN
				    GROUPED_CHUTES GC ON C.START_POINT_CD = GC.START_POINT_CD
				WHERE
				    (C.ITEM_CODE IS NULL OR C.ITEM_CODE = '') AND C.IS_USE = TRUE
				ORDER BY
				    GC.CHUTE_COUNT DESC, -- 우선순위 1: 미사용 팔렛타이저 우선 사용
				    C.UPDATED_AT ASC     -- 우선순위 2: 사용한지 가장 오래된 Pallet Conveyor 우선 사용
				FOR UPDATE
				""";
	}
}