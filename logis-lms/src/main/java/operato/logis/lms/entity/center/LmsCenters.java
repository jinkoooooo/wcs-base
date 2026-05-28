package operato.logis.lms.entity.center;

import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;

import java.io.Serial;

@Table(name = "lms_centers", idStrategy = GenerationRule.UUID, notnullFields = "lcId,lcNm,status", uniqueFields = "lcId",
        indexes = { @Index(name = "lms_centers_unique", columnList = "lc_id", unique = true) })
public class LmsCenters extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @Serial
    private static final long serialVersionUID = -2261701872936271525L;

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 36)
    private String lcId;

    @Column(name = "lc_nm", nullable = false, length = 100)
    private String lcNm;

    @Column(name = "db_type", length = 50)
    private String dbType;

    @Column(name = "db_connect_info", type = ColumnType.TEXT)
    private String dbConnectInfo;

    @Column(name = "edge_module_version", length = OrmConstants.FIELD_SIZE_VALUE_255)
    private String edgeModuleVersion;

    /**
     * 운영상태
     * D: Dev       개발 중
     * P: Pilot     시범운영
     * A: Active    운영 중
     * M: Maint     점검 중
     * I: Inactive  운영중단
     */
    @Column(name = "status", nullable = false, length = OrmConstants.FIELD_SIZE_STATUS)
    private String status;

    @Column(name = "address_plain", type = ColumnType.TEXT)
    private String addressPlain;

    @Column(name = "address_ct")
    private byte[] addressCt;

    @Column(name = "address_iv")
    private byte[] addressIv;

    @Column(name = "address_key_id")
    private Short addressKeyId;

    @Column(name = "edge_ct")
    private byte[] edgeCt;

    @Column(name = "edge_iv")
    private byte[] edgeIv;

    @Column(name = "edge_key_id")
    private Short edgeKeyId;

    public String getId() { return id; }

    public String getLcId() { return lcId; }

    public String getLcNm() { return lcNm; }

    public String getDbType() { return dbType; }

    public String getDbConnectInfo() { return dbConnectInfo; }

    public String getEdgeModuleVersion() { return edgeModuleVersion; }

    public String getStatus() { return status; }

    public String getAddressPlain() { return addressPlain; }

    public byte[] getAddressCt() { return addressCt; }

    public byte[] getAddressIv() { return addressIv; }

    public Short getAddressKeyId() { return addressKeyId; }

    public byte[] getEdgeCt() { return edgeCt; }

    public byte[] getEdgeIv() { return edgeIv; }

    public Short getEdgeKeyId() { return edgeKeyId; }

    public void setId(String id) { this.id = id; }

    public void setLcId(String lcId) { this.lcId = lcId; }

    public void setLcNm(String lcNm) { this.lcNm = lcNm; }

    public void setDbType(String dbType) { this.dbType = dbType; }

    public void setDbConnectInfo(String dbConnectInfo) { this.dbConnectInfo = dbConnectInfo; }

    public void setEdgeModuleVersion(String edgeModuleVersion) { this.edgeModuleVersion = edgeModuleVersion; }

    public void setStatus(String status) { this.status = status; }

    public void setAddressPlain(String addressCipher) { this.addressPlain = addressCipher; }

    public void setAddressCt(byte[] addressCt) { this.addressCt = addressCt; }

    public void setAddressIv(byte[] addressIv) { this.addressIv = addressIv; }

    public void setAddressKeyId(Short addressKeyId) { this.addressKeyId = addressKeyId; }

    public void setEdgeCt(byte[] edgeCt) { this.edgeCt = edgeCt; }

    public void setEdgeIv(byte[] edgeIv) { this.edgeIv = edgeIv; }

    public void setEdgeKeyId(Short edgeKeyId) { this.edgeKeyId = edgeKeyId; }
}