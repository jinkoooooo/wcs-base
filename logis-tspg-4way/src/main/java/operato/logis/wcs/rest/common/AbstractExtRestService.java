package operato.logis.wcs.rest.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.util.ValueUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Ext* 엔티티(상속 기반 확장 엔티티)를 위한 공통 RestService
 */
public abstract class AbstractExtRestService extends AbstractRestService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractExtRestService.class);

    /**
     * updateOne 시 input 값으로 덮어쓰지 않을 보존 필드.
     */
    private static final Set<String> PROTECTED_FIELDS = new HashSet<>(Arrays.asList(
            SysConstants.ENTITY_FIELD_ID,
            SysConstants.ENTITY_FIELD_DOMAIN_ID,
            SysConstants.ENTITY_FIELD_CREATED_AT,
            SysConstants.ENTITY_FIELD_UPDATED_AT,
            SysConstants.ENTITY_FIELD_CREATOR_ID,
            SysConstants.ENTITY_FIELD_UPDATER_ID,
            SysConstants.ENTITY_FIELD_CREATOR,
            SysConstants.ENTITY_FIELD_UPDATER
    ));

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T updateOne(T input) {
        AssertUtil.assertNotEmpty("terms.label.parameter", input);

        beforeSaveEntity(input);

        Object[] keys = this.getPkValues(input);
        T oneToUpdate = (T) this.queryManager.select(input.getClass(), keys);

        if (ValueUtil.isEmpty(oneToUpdate)) {
            throw new RuntimeException(
                    "업데이트 대상이 존재하지 않습니다. entity=%s, keys=%s"
                            .formatted(input.getClass().getSimpleName(), Arrays.toString(keys)));
        }

        deepCloneFields(input, oneToUpdate);

        this.queryManager.upsert(oneToUpdate);

        deepCloneFields(oneToUpdate, input);
        afterSaveEntity(input);

        return input;
    }

    /**
     * 클래스 계층(hierarchy) 을 traverse 하면서 source 의 non-null 필드 값을 target 에 복사.
     *
     * 핵심 로직: while 루프로 getSuperclass() 를 따라 Object 직전까지 거슬러 올라가며
     * 단계마다 getDeclaredFields() 호출. ValueUtil.cloneObject() 의 한계를 우회하는 핵심 차이.
     *
     * 제외 대상:
     *   - static / final / synthetic 필드
     *   - {@link #PROTECTED_FIELDS} 등록 필드 (id, created_at, creator 등)
     *   - source 의 null 필드 (DB 원본 값 유지)
     *
     * null 정책: source 가 null 인 필드는 복사하지 않는다.
     * 클라이언트가 변경분만 보낼 때 NOT NULL 컬럼이 null 로 덮어써져 제약 위반되는 것을 막는다.
     * "명시적 null 클리어" 는 미지원 - 비우려면 빈 문자열/0 등 명시값을 보내야 한다.
     */
    private static void deepCloneFields(Object source, Object target) {
        if (ValueUtil.isEmpty(source) || ValueUtil.isEmpty(target)) return;

        Class<?> clazz = source.getClass();
        while (ValueUtil.isNotEmpty(clazz) && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                int mod = field.getModifiers();

                // static / final / synthetic 제외
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || field.isSynthetic()) {
                    continue;
                }
                // 보존 필드 제외 (id, created_at, creator 등)
                if (PROTECTED_FIELDS.contains(field.getName())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(source);
                    if (ValueUtil.isEmpty(value)) continue;  // null 은 DB 원본 유지 (NOT NULL 컬럼 보호)
                    field.set(target, value);
                } catch (Exception e) {
                    // 필드 1개 실패는 건너뛰고 계속 (전체 복사 중단 방지)
                    logger.error("[ Ext ][ Clone ] field copy failed - class={}, field={}",
                            clazz.getSimpleName(), field.getName(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}