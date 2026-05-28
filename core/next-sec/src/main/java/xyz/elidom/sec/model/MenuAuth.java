/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.model;

/**
 * 메뉴 권한 모델
 * 
 * @author shortstop
 */
public class MenuAuth {

	/**
	 * menuId
	 */
	private String menuId;
	/**
	 * create
	 */
	private boolean create;
	/**
	 * update
	 */
	private boolean update;
	/**
	 * delete
	 */
	private boolean delete;
	/**
	 * show
	 */
	private boolean show;

	/**
	 * @return the menuId
	 */
	public String getMenuId() {
		return menuId;
	}

	/**
	 * @param menuId the menuId to set
	 */
	public void setMenuId(String menuId) {
		this.menuId = menuId;
	}

	/**
	 * @return the create
	 */
	public boolean isCreate() {
		return create;
	}

	/**
	 * @param create the create to set
	 */
	public void setCreate(boolean create) {
		this.create = create;
	}

	/**
	 * @return the update
	 */
	public boolean isUpdate() {
		return update;
	}

	/**
	 * @param update the update to set
	 */
	public void setUpdate(boolean update) {
		this.update = update;
	}

	/**
	 * @return the delete
	 */
	public boolean isDelete() {
		return delete;
	}

	/**
	 * @param delete the delete to set
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	/**
	 * @return the show
	 */
	public boolean isShow() {
		return show;
	}

	/**
	 * @param show the show to set
	 */
	public void setShow(boolean show) {
		this.show = show;
	}

}