package operato.logis.ecs.base.ecs.domain.enums;

public class EnumHelper<V> {

    private final V value;
    private final String description;

    public EnumHelper(V value, String description) {
        this.value = value;
        this.description = description;
    }

    public V getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}