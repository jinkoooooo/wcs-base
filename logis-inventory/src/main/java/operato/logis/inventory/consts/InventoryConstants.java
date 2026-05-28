package operato.logis.inventory.consts;

public class InventoryConstants {


    /**
     * TbInventoryItemGroup RestrictionType 목록
     */
    public static String RESTRICTION_DEDICATED = "DEDICATED";
    public static String RESTRICTION_FORBIDDEN = "FORBIDDEN";

    /**
     * TbInventorySetting OptionName 목록
     */
    public static String DEDICATED_OVERFLOW = "DEDICATED_OVERFLOW";
    public static String SKU_CONCENTRATED = "SKU_CONCENTRATED";
    public static String SHUTTLE_WAY_INFO = "4_WAY_SHUTTLE";
    public static String PATH_STANDARD = "PATH_STANDARD";

    /**
     * TbInventorySetting OptionValue 목록
     */
    public static String OPTION_VALUE_TRUE = "true";
    public static String OPTION_VALUE_FALSE = "false";
    public static String COLUMN_DIRECTION = "COLUMN";
    public static String ROW_DIRECTION = "ROW";

    /**
     * TbInventoryLocation 공용 Cell에 대한 그룹 코드 / 기둥에 대한 LocationType
     */
    public static String PUBLIC_ITEM_GROUP = "PUBLIC";
    public static String LOCATION_TYPE_PILLAR = "Pillar";

    /**
     * Inventory 모듈 Controller 기본 반환값
     */
    public static String SUCCESS_MESSAGE = "success";
}