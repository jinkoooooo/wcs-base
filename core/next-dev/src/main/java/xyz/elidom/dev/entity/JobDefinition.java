package xyz.elidom.dev.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "jobs", idStrategy = GenerationRule.UUID)
public class JobDefinition extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "service",nullable = false)
    private String service;

    @Column(name = "method",nullable = false)
    private String method;

    @Column(name = "method_param")
    private String methodParam;

    @Column(name = "description")
    private String description;

    @Column(name = "ok_count", nullable = false)
    private Integer okCount;

    @Column(name = "ng_count", nullable = false)
    private Integer ngCount;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "schedule_type", nullable = false)
    private String scheduleType;

    @Column(name = "sched_value", nullable = false)
    private String schedValue;

    @Column(name = "last_result")
    private String lastResult;
}
