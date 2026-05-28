package operato.logis.connector.sap.service;

import java.io.File;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;

import operato.logis.connector.sap.config.RfcPropertiesConfig;
import operato.logis.connector.sap.util.CustomDestinationDataProvider;

@Component
public class SapConnectionService {
    private final RfcPropertiesConfig rfcPropertiesConfig;
    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(SapConnectionService.class);
    private static boolean isProviderRegistered = false; //  중복 등록 방지 플래그

    public SapConnectionService(RfcPropertiesConfig rfcPropertiesConfig) {
        this.rfcPropertiesConfig = rfcPropertiesConfig;

        /*
            TODO : 불필요 시 추후 삭제.
         */
        // SAP JCo 로그 파일 경로 설정
        System.setProperty("jco.trace_path", System.getProperty("user.dir") + File.separator + "logis-connector" + File.separator + "logs");
        // 로그 레벨 설정 (선택)
        System.setProperty("jco.trace_level", "1"); // 1: ERROR, 2: INFO, 3: DEBUG, 10: FULL TRACE


        try {
            String destinationName = rfcPropertiesConfig.getAbapas();
            if (destinationName == null || destinationName.isBlank()) {
                throw new IllegalArgumentException("SAP Destination Name (abapas) Is Null. properties Check!");
            }

            // SAP 접속정보 Props 생성 및 셋팅
            Properties props = new Properties();
            props.setProperty(DestinationDataProvider.JCO_CLIENT, rfcPropertiesConfig.getClient());
            props.setProperty(DestinationDataProvider.JCO_USER, rfcPropertiesConfig.getUser());
            props.setProperty(DestinationDataProvider.JCO_PASSWD, rfcPropertiesConfig.getPasswd());
            props.setProperty(DestinationDataProvider.JCO_LANG, rfcPropertiesConfig.getLang());
            
            // Connection pool설정
            props.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, rfcPropertiesConfig.getPoolCapacity());
            props.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, rfcPropertiesConfig.getPeakLimit());
            props.setProperty(DestinationDataProvider.JCO_EXPIRATION_TIME, rfcPropertiesConfig.getExpirationTime());
            props.setProperty(DestinationDataProvider.JCO_MAX_GET_TIME, rfcPropertiesConfig.getMaxGetTime());

            if (rfcPropertiesConfig.getGroup() != null && !rfcPropertiesConfig.getGroup().isEmpty()) {
                props.setProperty(DestinationDataProvider.JCO_MSHOST, rfcPropertiesConfig.getAshost());
                props.setProperty(DestinationDataProvider.JCO_GROUP, rfcPropertiesConfig.getGroup());
                props.setProperty(DestinationDataProvider.JCO_R3NAME, rfcPropertiesConfig.getR3name());
            } else {
                props.setProperty(DestinationDataProvider.JCO_ASHOST, rfcPropertiesConfig.getAshost());
                props.setProperty(DestinationDataProvider.JCO_SYSNR, rfcPropertiesConfig.getSysnr());
            }

            //  CustomDestinationDataProvider 등록 및 Destination 추가
            CustomDestinationDataProvider provider = new CustomDestinationDataProvider();
            provider.addDestination(destinationName, props);

            if (!isProviderRegistered) {
                Environment.registerDestinationDataProvider(provider);
                isProviderRegistered = true;

                this.logger.info("JCo Destination Provider 등록 완료 : {}", destinationName);
            }

        } catch (Exception e) {
            throw new RuntimeException("SAP JCo 연결 구성 실패", e);
        }
    }

    public JCoDestination getDestination() throws JCoException {
        return JCoDestinationManager.getDestination(rfcPropertiesConfig.getAbapas());
    }
}
