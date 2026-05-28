package operato.logis.connector.plc;

public interface PlcBitEnum {
    int getBitIndex();
    String getDescription();

    default boolean isSet(int wordValue) {
        return ((wordValue >> getBitIndex()) & 1) == 1;
    }
}
