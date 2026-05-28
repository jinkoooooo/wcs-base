package operato.logis.changwon;

import xyz.elidom.sys.entity.Domain;

public class WcsConstants {
    public static final String LC_ID = "0000001660";
    public static final long DOMAIN_ID = 7L;
    public static final String DOMAIN_NAME = "WCS";
    public static final String TASK_ID_DELIMITER = "-";
    public static final String WMS_EMPTY_LOCATION_VALUE = "0";

    public static final int SAFETY_MARGIN = 5;
    public static final int CONVERT_RUNNER_TO_SHUTTLE = 200;

    public static final String INBOUND_CONVEYOR = "C201";
    public static final String OUTBOUND_CONVEYOR = "C206";
    public static final String TRANSFER_CONVEYOR = "C205";
    public static final String FORCE_INBOUND_START_POINT = "P00-00-00";

    public static final int DEFAULT_INBOUND_TASK_PRIORITY = 3000;
    public static final int DEFAULT_OUTBOUND_TASK_PRIORITY = 4000;
    public static final int DEFAULT_TRANSFER_TASK_PRIORITY = 3000;
    public static final int DEFAULT_SORT_TASK_PRIORITY = 2000;
    public static final int HIGHEST_PRIORITY = 1;

    public static final String START_METHOD = "start";
    public static final String LOADING_METHOD = "loading";
    public static final String END_METHOD = "end";
    public static final String CANCEL_METHOD = "cancel";
    public static final String ERROR_METHOD = "error";

    public static final String FORCE_INBOUND = "forceInbound";
    public static final String TRANSFER_BETWEEN_EQUIPMENT_1 = "transferBetweenEquipment1";
    public static final String TRANSFER_BETWEEN_EQUIPMENT_2 = "transferBetweenEquipment2";

    public static void setupDomainContext() {
        Domain domain = new Domain();
        domain.setId(DOMAIN_ID);
        domain.setName(DOMAIN_NAME);
        Domain.setCurrentDomain(domain);
    }
}