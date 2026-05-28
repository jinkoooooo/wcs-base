package operato.logis.connector.sap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "operato.logis.connector.sap.rfc")
public class RfcPropertiesConfig {

    private String aeskey;
    private String abapas;
    private String ashost;
    private String sysnr;
    private String lang;
    private String client;
    private String clientOther;
    private String group;
    private String r3name;
    private String user;
    private String passwd;
    // 커넥션 풀 옵션 추가
    private String poolCapacity;      // 최소 커넥션
    private String peakLimit;         // 최대 커넥션
    private String expirationTime;    // 미사용 만료(초)
    private String maxGetTime;        // 커넥션 획득 대기(ms)


    public String getAeskey() {
        return aeskey;
    }

    public void setAeskey(String aeskey) {
        this.aeskey = aeskey;
    }

    public String getAbapas() {
        return abapas;
    }

    public void setAbapas(String abapas) {
        this.abapas = abapas;
    }

    public String getAshost() {
        return ashost;
    }

    public void setAshost(String ashost) {
        this.ashost = ashost;
    }

    public String getSysnr() {
        return sysnr;
    }

    public void setSysnr(String sysnr) {
        this.sysnr = sysnr;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getClientOther() {
        return clientOther;
    }

    public void setClientOther(String clientOther) {
        this.clientOther = clientOther;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getR3name() {
        return r3name;
    }

    public void setR3name(String r3name) {
        this.r3name = r3name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }
    
    public String getPoolCapacity() {
        return poolCapacity;
    }
    public void setPoolCapacity(String poolCapacity) {
        this.poolCapacity = poolCapacity;
    }
    public String getPeakLimit() {
        return peakLimit;
    }
    public void setPeakLimit(String peakLimit) {
        this.peakLimit = peakLimit;
    }
    public String getExpirationTime() {
        return expirationTime;
    }
    public void setExpirationTime(String expirationTime) {
        this.expirationTime = expirationTime;
    }
    public String getMaxGetTime() {
        return maxGetTime;
    }
    public void setMaxGetTime(String maxGetTime) {
        this.maxGetTime = maxGetTime;
    }
}
