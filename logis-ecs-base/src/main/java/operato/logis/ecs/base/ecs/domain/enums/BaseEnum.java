package operato.logis.ecs.base.ecs.domain.enums;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 모든 Enum에서 공통적으로 사용할 인터페이스
 */
public interface BaseEnum<V> {

    /**
     * 각 enum은 헬퍼 객체를 반환해야 한다.
     */
    EnumHelper<V> getHelper();

    /**
     * 헬퍼 객체에서 value를 가져온다.
     */
    default V getValue() {
        return getHelper().getValue();
    }

    /**
     * 헬퍼 객체에서 description을 가져온다.
     */
    default String getDescription() {
        return getHelper().getDescription();
    }

    /**
     * Enum 값을 기반으로 빠르게 조회하는 맵을 생성 (O(1) 조회 가능)
     */
    static <V, T extends Enum<T> & BaseEnum<V>> Map<V, T> createLookupMap(Class<T> enumClass) {
        return Stream.of(enumClass.getEnumConstants())
                .collect(Collectors.toMap(BaseEnum::getValue, e -> e));
    }

    /**
     * Enum 값과 비교하는 메서드
     */
    default boolean is(V value) {
        return getValue().equals(value);
    }
}