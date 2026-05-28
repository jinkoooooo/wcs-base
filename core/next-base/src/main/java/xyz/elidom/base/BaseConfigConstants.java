/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.base;

import xyz.elidom.core.CoreConfigConstants;

/**
 * Base 모듈 설정에 필요한 상수 정의
 * 
 * @author Minu.Kim
 */
public class BaseConfigConstants extends CoreConfigConstants {

	/**
	 * 메뉴 컬럼 정보 중에 그리드의 코드 콤보 편집기의 코드 데이터를 서버에서 조회해서 채워줄 지 클라이언트가 서비스 호출해서 받아 올 지 결정하는 설정 : 'grid.codecombo.data.search.at.server'
	 */
	public static final String CODE_COMBO_DATA_FILL_AT_SERVER = "grid.codecombo.data.search.at.server";

	/**
	 * Page 전환 이력을 생성 할 타입 목록. ex) "menu,player..."
	 */
	public static final String PAGE_ROUTE_HISTORY_TYPES = "page.route.history.types";
	
	/**
	 * Page 전환 이력 활성화 여부.
	 */
	public static final String PAGE_ROUTE_HISTORY_ENABLE = "page.route.history.enable";
	
	/**
	 * Page 전환 이력을 생성 할 데이터 소스 이름.
	 */
	public static final String PAGE_ROUTE_HISTORY_DATASOURCE = "page.route.history.datasource";
	
	/**
	 * Page 전환 이력을 저장 할 기간(월).
	 */
	public static final String PAGE_ROUTE_HISTORY_SAVE_PERIOD_MONTH = "page.route.history.save.period.month";
}