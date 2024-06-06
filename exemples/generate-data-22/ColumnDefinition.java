import java.util.Random;
import static java.util.UUID.randomUUID;

import org.apache.commons.lang3.RandomStringUtils;

record ColumnDefinition(String name, ColumnType type) {

    static ColumnDefinition from(String columnNameAndType) {
        String[] columnNameAndTypeArray = columnNameAndType.split(" ");
        String name = columnNameAndTypeArray[0];
        ColumnType type = ColumnType.from(columnNameAndTypeArray[1]);
        return new ColumnDefinition(name, type);
    }

    enum ColumnType {
        UUID,
        INTEGER,
        VARCHAR,
        TIMESTAMP;

        static ColumnType from(String columnType) {
            try {
                return ColumnType.valueOf(columnType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(STR."Invalid Column Type: \{columnType}", e);
            }
        }

        Object randomValue() {
            return switch (this) {
                case UUID -> randomUUID();
                case INTEGER -> Math.abs((new Random().nextInt(10) + 1) * 50);
                case VARCHAR -> RandomStringUtils.randomAlphabetic(32);
                case TIMESTAMP -> throw new UnsupportedOperationException("Not implemented yet !");
            };
        }
    }
}
