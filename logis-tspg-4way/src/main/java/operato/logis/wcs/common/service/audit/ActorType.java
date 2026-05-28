package operato.logis.wcs.common.service.audit;

/** 감사 행위자 유형. 사람(USER)·상위 호스트(HOST)·설비제어(ECS)·스케줄러(SCHEDULER)·시스템(SYSTEM). */
public enum ActorType {
    USER, HOST, ECS, SCHEDULER, SYSTEM
}
