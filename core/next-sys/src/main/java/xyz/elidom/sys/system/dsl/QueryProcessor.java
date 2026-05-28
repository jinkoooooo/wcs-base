/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.dsl;

import org.jruby.RubyArray;

import xyz.elidom.sys.util.SysValueUtil;

/**
 * QueryProcessor
 * 
 * @author shortstop
 */
public abstract class QueryProcessor {

	/**
	 * select 문
	 */
	protected String _select;
	/**
	 * where 문
	 */
	protected String _where;
	/**
	 * order 문
	 */
	protected String _order;
	/**
	 * value parameters
	 */
	protected Object[] _params;
	/**
	 * 한 페이지에 보여질 레코드 수 
	 */
	protected int _limit = 0;
	/**
	 * 현재 페이지 offset = (currentPage - 1) * limit
	 */
	protected int _currentPage = 0;
	/**
	 * 몇 개의 레코드를 스킵할 것인지, DBIST API로는 지원 못 함 ...  
	 */
	//private int offset = 0;
	
	/**
	 * call select
	 * 
	 * @param select
	 * @return
	 */
	public QueryProcessor select(String select) {
		this._select = select;
		return this;
	}
	
	/**
	 * call where
	 * 
	 * @param where
	 * @param params
	 * @return
	 */
	public QueryProcessor where(String where, RubyArray params) {
		Object[] jParams = new Object[params.size()];
		for(int i = 0 ; i < params.size() ; i++) {
			jParams[i] = params.get(i);
		}
		
		this._where = where;
		this._params = jParams;
		return this;
	}
	
	/**
	 * call where
	 * 
	 * @param where
	 * @param params
	 * @return
	 */
	public QueryProcessor conditions(String where, Object... params) {
		this._where = where;
		this._params = params;
		return this;
	}	
	
	/**
	 * call order
	 * 
	 * @param order
	 * @return
	 */
	public QueryProcessor order(String order) {
		this._order = order;
		return this;
	}
	
	/**
	 * call limit
	 * 
	 * @param limit
	 * @return
	 */
	public QueryProcessor limit(int limit) {
		this._limit = limit;
		return this;
	}
	
	/**
	 * call offset
	 * 
	 * @param offset
	 * @return
	 */
	/*public QueryProcessor offset(int offset) {
		this.offset = offset;
		return this;
	}*/
	
	/**
	 * call currentPage
	 * 
	 * @param currentPage
	 * @return
	 */
	public QueryProcessor currentPage(int currentPage) {
		this._currentPage = currentPage;
		return this;
	}

	/**
	 * 쿼리 수행 
	 * 
	 * @return
	 */
	public abstract Object process();
	
	/**
	 * clear query variables
	 */
	public void clearQuery() {
		this._limit = 0;
		this._currentPage = 0;
		// this._offset = 0;
		this._select = null;
		this._where = null;
		this._order = null;
		
		if(!SysValueUtil.isEmpty(this._params)) {
			for(int i = 0 ; i < this._params.length ; i++) {
				this._params[i] = null;
			}
		}
		
		this._params = null;
	}

}
