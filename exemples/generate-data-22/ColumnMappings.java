import java.util.Arrays;
import java.util.List;
import java.util.Optional;

record ColumnMappings(List<ColumnMapping> columnMappings) {

    Optional<ColumnMapping> find(String columnName) {
        return columnMappings.stream()
                .filter(columnMapping -> columnMapping.name.equals(columnName))
                .findAny();
    }

    static ColumnMappings from(String[] columnMappings) {
        return new ColumnMappings(Arrays.stream(columnMappings)
                .map(ColumnMapping::from)
                .toList());
    }

    record ColumnMapping(String name, ColumnMappingType type, String value) {

        static ColumnMapping from(String columnMapping) {
            String[] columnMappingWords = columnMapping.split("::");
            String name = columnMappingWords[0];
            ColumnMappingType type = ColumnMappingType.from(columnMappingWords[1], columnMapping);
            String value = columnMappingWords.length > 2 ? columnMappingWords[2] : null;
            return new ColumnMapping(name, type, value);
        }

        enum ColumnMappingType {
            CONSTANT,
            RANDOM,
            FILE;

            static ColumnMappingType from(String columnType, String columnMapping) {
                try {
                    return ColumnMappingType.valueOf(columnType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(STR."Invalid Column Mapping: \{columnMapping}", e);
                }
            }
        }
    }
}
