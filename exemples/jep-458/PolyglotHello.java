///usr/bin/env java --source 22 -classpath $APP_DIR/lib/'*' --enable-preview "$0" "$@"; exit $?

import fr.Hello;

class Main {
    void main() {
        switch (Language.guess()) {
            case French -> Hello.main();
            case English -> en.Hello.main();
        }
        System.exit(0);
    }

    private enum Language {
        French, English;

        public static Language guess() {
            var langEnvVar = System.getenv("LANG");
            var lang = langEnvVar.substring(0, langEnvVar.indexOf('_'));
            return switch (lang) {
                case "fr" -> French;
                case "en" -> English;
                default -> throw new IllegalStateException(STR."Unexpected Language: \{lang}");
            };
        }
    }
}
