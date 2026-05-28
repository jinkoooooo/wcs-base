package xyz.elidom.dev.entity;

import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainStampHook;

@Table(name = "service_out_params", idStrategy = GenerationRule.UUID, uniqueFields = "domainId,resourceType,resourceId,name", indexes = {
        @Index(name = "ix_svc_out_param_0", columnList = "domain_id,resource_type,resource_id,name", unique=true),
        @Index(name = "ix_svc_out_param_1", columnList = "resource_type,resource_id")
})
public class ServiceOutParam extends DomainStampHook {

    /**
     * SerialVersion UID
     */
    private static final long serialVersionUID = -2125791906825417283L;

    @PrimaryKey
    @Column(name = "id", length = OrmConstants.FIELD_SIZE_UUID, nullable = false)
    private String id;

    @Column(name = "name", length = OrmConstants.FIELD_SIZE_NAME, nullable = false)
    private String name;

    @Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
    private String description;

    @Column(name = "resource_id", length = OrmConstants.FIELD_SIZE_MEANINGFUL_ID, nullable = false)
    private String resourceId;

    @Column(name = "resource_type", length = OrmConstants.FIELD_SIZE_NAME, nullable = false)
    private String resourceType;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "data_type", length = 20)
    private String dataType;

    @Column(name = "nullable")
    private Boolean nullable;

    public ServiceOutParam() {
    }

    public ServiceOutParam(String resourceType, String resourceId) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

}