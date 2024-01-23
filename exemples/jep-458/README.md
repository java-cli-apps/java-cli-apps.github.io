# Exemple de code utilisant la JEP 458

Un exemple de code utilisant la JEP 458 (Launch Multi-File Source-Code Programs).

On lance la classe `PolyglotHello` et les classes qu'elle utilise `fr.Hello` et `en.Hello` sont trouvées automatiquement par l'interpréteur `java`.

```bash
$ tree
.
├── en
│   └── Hello.java
├── fr
│   └── Hello.java
├── PolyglotHello.Java
└── README.md

3 directories, 5 files
```

```bash
$ echo $LANG
fr_FR.UTF-8
```

```bash
$ ./PolyglotHello.java
Bonjour
```
