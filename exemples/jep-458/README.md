# Exemple de code utilisant la JEP 458

Un exemple de code utilisant la JEP 458 (Launch Multi-File Source-Code Programs).

On lance la classe `PolyglotHello` et les classes qu'elle utilise `fr.Hello` et `en.Hello` sont trouvées automatiquement par l'interpréteur `java`.

Les dépendances sont trouvées grâce à `--class-path $APP_DIR/lib/'*'` sur la ligne de commande.

```bash
$ tree
.
├── en
│   └── Hello.java
├── fr
│   └── Hello.java
├── lib
│   ├── jackson-annotations-2.16.1.jar
│   ├── jackson-core-2.16.1.jar
│   ├── jackson-databind-2.16.1.jar
│   └── jemoji-1.3.3.jar
├── PolyglotHello.java
└── README.md

3 directories, 8 files
```

```bash
$ echo $LANG
```

```console
fr_FR.UTF-8
```

```bash
$ ./PolyglotHello.java
```

```console
Bonjour 🇫🇷
```

```bash
$ LANG=en_EN.UTF-8 ./PolyglotHello.java
```

```console
Hello 🇬🇧
```
