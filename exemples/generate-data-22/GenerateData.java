///usr/bin/env java --source 22 --enable-preview --class-path $APP_DIR/lib/picocli-4.7.5.jar:$APP_DIR/lib/commons-lang3-3.14.0.jar "$0" "$@"; exit $?

import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

@Command(name = "GenerateData", mixinStandardHelpOptions = true, version = "0.1")
class GenerateData22 implements Callable<Integer> {

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
        System.exit(new CommandLine(new GenerateData22()).execute(args));
    }

    @Override
    public Integer call() throws IOException {
        System.out.println(TableData.generate(sqlRequestFile, columnMappings, lineCount).toSQLInserts());
        return 0;
    }
}
