import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.IntStream;

record TableData(TableDefinition tableDefinition, TableRows tableRows) implements Exportable {

    static TableData generate(File sqlRequestFile, String[] columnMappingsArray, int lineCount) throws IOException {
        TableDefinition tableDefinition = TableDefinition.from(sqlRequestFile);
        ColumnMappings columnMappings = ColumnMappings.from(columnMappingsArray);
        List<TableRow> tableRows = IntStream.range(0, lineCount)
                .mapToObj(rowIndex -> generateColumnValues(tableDefinition, columnMappings))
                .map(TableRow::new)
                .toList();
        return new TableData(tableDefinition, new TableRows(tableRows));
    }

    private static List<Object> generateColumnValues(TableDefinition tableDefinition, ColumnMappings columnMappings) {
        List<Object> columnValues = new ArrayList<>();
        IntStream.range(0, tableDefinition.columnDefinitions().size()).forEach(columnIndex -> {
            ColumnDefinition columnDefinition = tableDefinition.columnDefinitions().get(columnIndex);
            Object columnValue = generateColumnValue(columnDefinition, columnMappings);
            columnValues.add(columnIndex, columnValue);
        });
        return columnValues;
    }

    private static Object generateColumnValue(ColumnDefinition columnDefinition, ColumnMappings columnMappings) {
        Optional<ColumnMappings.ColumnMapping> optColumnMapping = columnMappings.find(columnDefinition.name());
        if (optColumnMapping.isEmpty()) {
            return null;
        }
        ColumnMappings.ColumnMapping columnMapping = optColumnMapping.get();
        return switch (columnMapping.type()) {
            case CONSTANT -> columnMapping.value();
            case RANDOM -> columnDefinition.type().randomValue();
            case FILE -> DataReference.nextValue(columnMapping.value());
        };
    }

    @Override
    public String toSQLInserts() {
        return tableDefinition.toSQLInserts() + tableRows.toSQLInserts();
    }

    record TableRows(List<TableRow> tableRows) implements Exportable {

        @Override
        public String toSQLInserts() {
            StringBuilder builder = new StringBuilder();
            int rowCount = tableRows.size();
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                builder.append('(')
                        .append(tableRows.get(rowIndex).toSQLInserts())
                        .append(')')
                        .append(rowIndex < rowCount - 1 ? ',' : ';')
                        .append('\n');
            }
            return builder.toString();
        }
    }

    record TableRow(List<Object> columnValues) implements Exportable {

        @Override
        public String toSQLInserts() {
            StringJoiner joiner = new StringJoiner(", ");
            columnValues.forEach(columnValue -> joiner.add(convertToSQL(columnValue)));
            return joiner.toString();
        }

        private static String convertToSQL(Object columnValue) {
            return switch (columnValue) {
                case null -> "null";
                case UUID anUUID -> quote(anUUID.toString());
                case Integer anInteger -> String.valueOf(anInteger);
                case String aString -> quote(aString);
                case LocalDateTime _ -> throw new UnsupportedOperationException("Not implemented yet !");
                default -> null;
            };
        }

        private static String quote(String columnValue) {
            return columnValue.replaceAll("^|$", "'");
        }
    }

    private static class DataReference {

        private static final Map<String, List<String>> fileLinesByFileNames = new HashMap<>();

        static String nextValue(String fileName) {
            List<String> fileLines = fileLinesByFileNames.get(fileName);
            if (fileLines == null) {
                try {
                    fileLines = Files.readAllLines(Paths.get(fileName));
                } catch (IOException e) {
                    throw new RuntimeException(STR."Failed to read: \{fileName}", e);
                }
                fileLinesByFileNames.put(fileName, fileLines);
            }
            return fileLines.removeFirst();
        }
    }
}
