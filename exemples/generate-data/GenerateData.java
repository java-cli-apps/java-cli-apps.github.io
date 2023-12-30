///usr/bin/java --source 21 --enable-preview -classpath $APP_DIR/lib/picocli-4.7.5.jar:$APP_DIR/lib/commons-lang3-3.14.0.jar "$0" "$@"; exit $?

import org.apache.commons.lang3.RandomStringUtils;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static java.util.UUID.randomUUID;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

@Command(name = "GenerateData", mixinStandardHelpOptions = true, version = "0.1")
class GenerateData implements Callable<Integer> {

    @Option(names = {"-c", "--column"}, description = "Map a column with a fixed value, mapping function or value from a file")
    String[] columnMappings;

    @Option(names = {"-n", "--count"}, description = "Number of lines to generate")
    int lineCount;

    @Parameters(arity = "1", description = "The file containing the SQL create table request")
    File sqlRequestFile;

    /**
     * On utilise ici <code>void main(String... args)</code>
     * au lien de l'historique <code>public static void main(String... args)</code>
     * grâce à la JEP 445 : Unnamed Classes and Instance Main Methods (Preview)
     * et l'utilisation de <code>--enable-preview</code>.
     */
    void main(String... args) {
        System.exit(new CommandLine(new GenerateData()).execute(args));
    }

    @Override
    public Integer call() throws IOException {
        System.out.println(TableData.generate(sqlRequestFile, columnMappings, lineCount).toSQLInserts());
        return 0;
    }

    record TableData(TableDefinition tableDefinition, TableRows tableRows) implements Exportable {

        static TableData generate(File sqlRequestFile, String[] columnMappingsArray, int lineCount) throws IOException {
            TableDefinition tableDefinition = TableDefinition.from(sqlRequestFile);
            ColumnMappings columnMappings = ColumnMappings.from(columnMappingsArray);
            List<TableRows.TableRow> tableRows = IntStream.range(0, lineCount)
                    .mapToObj(rowIndex -> generateColumnValues(tableDefinition, columnMappings))
                    .map(TableRows.TableRow::new)
                    .toList();
            return new TableData(tableDefinition, new TableRows(tableRows));
        }

        private static List<Object> generateColumnValues(TableDefinition tableDefinition, ColumnMappings columnMappings) {
            List<Object> columnValues = new ArrayList<>();
            IntStream.range(0, tableDefinition.columnDefinitions.size()).forEach(columnIndex -> {
                ColumnDefinition columnDefinition = tableDefinition.columnDefinitions.get(columnIndex);
                Object columnValue = generateColumnValue(columnDefinition, columnMappings);
                columnValues.add(columnIndex, columnValue);
            });
            return columnValues;
        }

        private static Object generateColumnValue(ColumnDefinition columnDefinition, ColumnMappings columnMappings) {
            Optional<ColumnMappings.ColumnMapping> optColumnMapping = columnMappings.find(columnDefinition.name);
            if (optColumnMapping.isEmpty()) {
                return null;
            }
            ColumnMappings.ColumnMapping columnMapping = optColumnMapping.get();
            return switch (columnMapping.type) {
                case CONSTANT -> columnMapping.value;
                case RANDOM -> columnDefinition.type.randomValue();
                case FILE -> DataReference.nextValue(columnMapping.value);
            };
        }

        @Override
        public String toSQLInserts() {
            return tableDefinition.toSQLInserts() + tableRows.toSQLInserts();
        }

        private record TableRows(List<TableRow> tableRows) implements Exportable {

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
                        case LocalDateTime _ ->
                                throw new UnsupportedOperationException("Not implemented yet !");
                        default -> null;
                    };
                }

                private static String quote(String columnValue) {
                    return columnValue.replaceAll("^|$", "'");
                }
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

    record TableDefinition(String name, List<ColumnDefinition> columnDefinitions) implements Exportable {

        static TableDefinition from(File sqlRequestFile) throws IOException {
            String rawSqlRequest = Files.readString(sqlRequestFile.toPath());

            String sqlRequest = String.join("", rawSqlRequest.lines().toList());
            int startIndex = sqlRequest.indexOf('(') + 1;
            int endIndex = sqlRequest.indexOf(')');

            String[] createTableWords = sqlRequest.substring(0, startIndex - 1).split(" ");
            String name = createTableWords[createTableWords.length - 1];

            String columnsSubRequest = sqlRequest.substring(startIndex, endIndex).trim().replaceAll(" +", " ");
            List<String> columnNameAndTypes = Arrays.stream(columnsSubRequest.split(",")).map(String::stripLeading).toList();

            List<ColumnDefinition> columnDefinitions = columnNameAndTypes.stream()
                    .map(ColumnDefinition::from)
                    .toList();

            return new TableDefinition(name, columnDefinitions);
        }

        @Override
        public String toSQLInserts() {
            StringBuilder builder = new StringBuilder();
            builder.append("INSERT INTO ")
                    .append(name)
                    .append(" (");
            int columnCount = columnDefinitions.size();
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                builder.append(columnDefinitions.get(columnIndex).name)
                        .append(columnIndex < columnCount - 1 ? ", " : "");
            }
            builder.append(')')
                    .append('\n')
                    .append("VALUES")
                    .append('\n');
            return builder.toString();
        }
    }

    record ColumnDefinition(String name, ColumnType type) {

        static ColumnDefinition from(String columnNameAndType) {
            String[] columnNameAndTypeArray = columnNameAndType.split(" ");
            String name = columnNameAndTypeArray[0];
            ColumnType type = ColumnType.from(columnNameAndTypeArray[1]);
            return new ColumnDefinition(name, type);
        }

        private enum ColumnType {
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

            private enum ColumnMappingType {
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
    interface Exportable {
        String toSQLInserts();
    }
}
