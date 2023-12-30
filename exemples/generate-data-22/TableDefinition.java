import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

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
            builder.append(columnDefinitions.get(columnIndex).name())
                    .append(columnIndex < columnCount - 1 ? ", " : "");
        }
        builder.append(')')
                .append('\n')
                .append("VALUES")
                .append('\n');
        return builder.toString();
    }
}
