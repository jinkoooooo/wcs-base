package xyz.elidom.dev.entity;

import java.util.List;
import java.util.Map;

import xyz.elidom.dbist.annotation.ChildEntity;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
import xyz.elidom.dbist.annotation.DetailRemovalStrategy;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Ignore;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.MasterDetailType;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.dev.rest.DiyServiceController;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;
import xyz.elidom.util.BeanUtil;

@Table(name = "diy_services", idStrategy = GenerationRule.UUID, uniqueFields = "domainId,name", indexes = {
        @Index(name = "ix_diy_svc_0", columnList = "domain_id,name", unique = true),
        @Index(name = "ix_diy_svc_1", columnList = "domain_id,category")
}, childEntities = {
        @ChildEntity(entityClass = ServiceInParam.class, type = MasterDetailType.ONE_TO_MANY, refFields = "resourceType,resourceId", dataProperty = "in_params", deleteStrategy = DetailRemovalStrategy.DELETE),
        @ChildEntity(entityClass = ServiceOutParam.class, type = MasterDetailType.ONE_TO_MANY, refFields = "resourceType,resourceId", dataProperty = "out_params", deleteStrategy = DetailRemovalStrategy.DELETE),
})
public class DiyService extends ElidomStampHook {

    /**
     * SerialVersion UID
     */
    private static final long serialVersionUID = 7580416966312330182L;

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
    private String id;

    @Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
    private String name;

    @Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
    private String description;

    @Column(name = "category", length = OrmConstants.FIELD_SIZE_CATEGORY)
    private String category;

    @Column(name = "lang_type", length = 10)
    private String langType;

    @Column(name = "script_type", length = 10)
    private String scriptType;

    @Column(name = "active_flag")
    private Boolean activeFlag;

    @Column(name = "atomic_flag")
    private Boolean atomicFlag;

    @Column(name = "service_logic", type = ColumnType.TEXT)
    private String serviceLogic;

    @Ignore
    private List<ServiceInParam> serviceInParams;

    @Ignore
    private List<ServiceOutParam> serviceOutParams;

    public DiyService() {
    }

    public DiyService(Long domainId, String name) {
        this.domainId = domainId;
        this.name = name;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * @return the scriptType
     */
    public String getScriptType() {
        return scriptType;
    }

    /**
     * @param scriptType the scriptType to set
     */
    public void setScriptType(String scriptType) {
        this.scriptType = scriptType;
    }

    /**
     * @return the langType
     */
    public String getLangType() {
        return langType;
    }

    /**
     * @param langType the langType to set
     */
    public void setLangType(String langType) {
        this.langType = langType;
    }

    /**
     * @return the activeFlag
     */
    public Boolean getActiveFlag() {
        return activeFlag;
    }

    /**
     * @param activeFlag the activeFlag to set
     */
    public void setActiveFlag(Boolean activeFlag) {
        this.activeFlag = activeFlag;
    }

    /**
     * @return the atomicFlag
     */
    public Boolean getAtomicFlag() {
        return atomicFlag;
    }

    /**
     * @param atomicFlag the atomicFlag to set
     */
    public void setAtomicFlag(Boolean atomicFlag) {
        this.atomicFlag = atomicFlag;
    }

    /**
     * @return the serviceLogic
     */
    public String getServiceLogic() {
        return serviceLogic;
    }

    /**
     * @param serviceLogic the serviceLogic to set
     */
    public void setServiceLogic(String serviceLogic) {
        this.serviceLogic = serviceLogic;
    }

    public List<ServiceInParam> getServiceInParams() {
        return serviceInParams;
    }

    public void setServiceInParams(List<ServiceInParam> serviceInParams) {
        this.serviceInParams = serviceInParams;
    }

    public List<ServiceOutParam> getServiceOutParams() {
        return serviceOutParams;
    }

    public void setServiceOutParams(List<ServiceOutParam> serviceOutParams) {
        this.serviceOutParams = serviceOutParams;
    }

    public static Object doDiyService(Long domainId, String serviceName, Map<String, Object> map) {
        DiyServiceController diyServiceController = BeanUtil.get(DiyServiceController.class);
        return diyServiceController.shoot(serviceName, map);
    }
}