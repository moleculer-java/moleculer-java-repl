# TODO — Modernize `moleculer-java-repl` to 2.0.0

> **You are the per-project Claude Code instance for `moleculer-java-repl`.** Self-contained file.
> Goal: Gradle/Java 8 → **Maven + JDK 21**, deps upgraded, legacy files removed, version **2.0.0**.
> This module adds an interactive REPL developer console (local stdin/out or telnet) as a Moleculer
> `Service`. Packages: `services.moleculer.repl(.commands)`.

## Coordinates & facts
- Maven: `com.github.berkesa:moleculer-java-repl`, `jar`, license **MIT**.
- `name`: *Moleculer REPL Module* · `inceptionYear`: 2018
- `url`: https://moleculer-java.github.io/moleculer-java-repl/ · `scm`: https://github.com/moleculer-java/moleculer-java-repl.git
- developer: `berkesa` / Andras Berkes / andras.berkes@programmer.net
- **Version → `2.0.0`** (old build hard-codes it in 3 places: `version`, `jar`, and the
  `moleculer-java` dependency — collapses to Maven `<version>` + the dependency `<version>`).

## Inter-project dependency (PIN to 2.0.0)
- `com.github.berkesa:moleculer-java:2.0.0` (was **1.2.24** — behind core's 1.2.28; now unified).
  Build `moleculer-java` first.

## Target versions
| Dependency | Current | Target | Scope |
|---|---|---|---|
| `com.github.berkesa:moleculer-java` | 1.2.24 | **2.0.0** | compile |
| `com.diogonunes:JCDP` 3.0.4 | → **`com.diogonunes:JColor:5.5.1`** (🔒 lockstep — locked by moleculer-java) | ⚠ package `com.diogonunes.jcdp` → `com.diogonunes.jcolor` | compile |
| `org.slf4j:slf4j-api`, `slf4j-jdk14`, `log4j-over-slf4j`, `jcl-over-slf4j` | 1.7.30 | **2.0.18** | runtime |
| Eclipse `ecj` 4.4.2 | — | **remove** | — |
| Java | 1.8 | **21** | — |

## ⚠ JCDP → JColor migration
`ColorWriter` translates `§`-color codes to JCDP colors for the local console (and to raw ANSI for
telnet). JCDP 3.x was renamed to **JColor 5.x** with a new package/API
(`com.diogonunes.jcdp.color` → `com.diogonunes.jcolor`, `Ansi.colorize(...)` style). Rewrite
`ColorWriter`'s JCDP calls against JColor; keep the public `ColorWriter` constants (`YELLOW="§!"`,
`GREEN`, `CYAN`, `GRAY`, …) and the telnet ANSI path unchanged.

## Steps
1. **`pom.xml`** (metadata + MIT + `release=21`). Deps: `moleculer-java:2.0.0-SNAPSHOT`,
   `com.diogonunes:JColor:5.5.1`, slf4j 2.0.18 (four jars, `runtime`). Build plugins: compiler **3.15.0**,
   surefire **3.5.4** (lockstep with the rest of the workspace), (release profile) sources/javadoc/gpg + central-publishing 0.9.0.
2. **Remove ECJ** → javac (the old build set `options.encoding = 'UTF-8'` — set
   `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`).
3. **Migrate JCDP → JColor** in `ColorWriter` (and anywhere else JCDP is imported).
4. **Tests:** there are **no JUnit tests** — `src/test/java` is a manual harness
   (`services.moleculer.repl.Sample` `main()` + `testcase/GreeterService`, `MathService`,
   `HelloCommand`). Keep the harness compiling under JDK 21. The `junit` dep can be dropped or set to
   `junit-jupiter` test scope (no tests to run). `mvn verify` should pass (nothing to fail).
5. **Preserve behavior:** the `Repl`→`LocalRepl`→`RemoteRepl` hierarchy, built-in command loading
   via the `load(...)` name list in `LocalRepl.startReading()`, `@Name`-derived command names,
   `getPayload`/`parseFlags`, and the `JsonColorizer` registered as the `"colorized-json"` datatree
   writer in `Repl`'s static initializer.
6. **Cleanup — delete:** `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/`,
   `.gradle/`, `.codacy.yaml`, `.classpath`, `.project`, `.settings/`. (No `.travis.yml` here.)
7. **VSCode + .gitignore.** Add `.vscode/launch.json` for `services.moleculer.repl.Sample` (run it to
   exercise the console interactively).
8. **Build & install:** `mvn clean install`, then `mvn clean verify`.
9. **Update `CLAUDE.md`:** Maven commands; version now in 2 Maven spots (pom + dependency), both
   `2.0.0`; note the JCDP→JColor change.

## Definition of done
- `mvn clean verify` green on JDK 21; JColor migration done; manual `Sample` harness compiles.
- Legacy files gone; VSCode (+ launch.json) + .gitignore; version `2.0.0`; depends on
  `moleculer-java:2.0.0`; installed to local `~/.m2`; publishing configured.
