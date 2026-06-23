# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`moleculer-java-repl` is a library (published to Maven Central as `com.github.berkesa:moleculer-java-repl`)
that adds an interactive REPL developer console to the [Moleculer for Java](https://moleculer-java.github.io/site/)
microservices framework. The console runs text commands that test Moleculer actions/event listeners and measure
service response times, over standard input/output or telnet. It is itself a Moleculer `Service`, installed into a
running `ServiceBroker`.

## Build & run

Maven project, bytecode target **Java 17** (`<maven.compiler.release>17</maven.compiler.release>`), build JDK 17+ (JDK 25 in use). Minimum consumer runtime: **JDK 17** (Spring 6 transitive via moleculer-java). `groupId = com.github.berkesa`, `version = 2.0.0`. Built with plain `javac` via `maven-compiler-plugin`. The
published artifact is `com.github.berkesa:moleculer-java-repl`; the jar file is named after the artifactId
(`moleculer-java-repl-<version>.jar`).

```bash
mvn clean install                 # compile + test + install to local ~/.m2
mvn clean verify                  # full build (no unit tests to run — see Testing)
mvn -Prelease clean deploy        # sources+javadoc+GPG sign + Central Portal publish
```

When bumping the version, change it in **two** places in `pom.xml`: the top-level `<version>` and (when not using
the workspace lockstep `2.0.0`) the `com.github.berkesa:moleculer-java` dependency `<version>`. Both are `2.0.0`.

## Testing

There are **no JUnit tests** — the `junit-jupiter` dependency is only present so the manual harness in
`src/test/java` compiles. `mvn verify` therefore has nothing to run and passes trivially. `src/test/java` is a
manual harness:

- `services.moleculer.repl.Sample` — a `main()` that boots a `ServiceBroker` with a `TcpTransporter` + `CircuitBreaker`
  and installs a `LocalRepl`. Run this class to exercise the console interactively.
- `testcase/GreeterService`, `testcase/MathService` — sample Moleculer services to call from the console.
- `testcase/HelloCommand` — a sample **custom command**; `Sample` registers it via `repl.setPackagesToScan("testcase")`.

To try a change end-to-end, run `Sample` and type commands (`help`, `call greeter.hello "name"`, `bench ...`, etc.).

## Architecture

**Service hierarchy.** `Repl` (abstract, extends Moleculer `Service`) → `LocalRepl` → `RemoteRepl`. A REPL is
installed like any service: `broker.createService("$repl", new LocalRepl())`, or via the broker's `repl()` helper.
`Repl.started()` calls `startReading()`; `stopped()` calls `stopReading()`. `setEnabled(false)` stops the reader loop.

- **`LocalRepl`** — reads `System.in`/`System.out` on a single-thread executor (`run()` loop using `LocalReader`).
  Holds the command registry (`ConcurrentHashMap<String, Command>`), parses input lines (handling quotes),
  dispatches to commands, prints help, and suggests a command on typos. `r`/`repeat` re-runs the last command;
  `q` maps to `exit`.
- **`RemoteRepl`** — overrides the reader loop with a non-blocking NIO telnet server (default port 23, max 8
  sessions, username/password `admin`/`admin` — **change via `setPassword()`**; connection is not encrypted).
  Uses `RemoteReader` per connection.

**Commands.** Every command extends the abstract `Command` (in `services.moleculer.repl`) and lives in
`services.moleculer.repl.commands`. A command implements:

- `getDescription()`, `getUsage()`, `getNumberOfRequiredParameters()`
- `onCommand(ServiceBroker broker, PrintWriter out, String[] parameters)` — the actual work.

The command **name comes from the `@Name` annotation** (lowercased). `Command`'s helpers:
`option(name, desc)` registers a flag for `--help` output; `parseFlags(...)` parses `--flag value` pairs into a
`Tree`; `getPayload(...)` turns positional args **or** an inline JSON string (`{...}`, `[...]`, `'...'`) into a
`Tree` payload (and stamps `$repl` meta header `META_REPL_HEADER`).

**Registering a new built-in command:** create the class in `commands/` with an `@Name`, then add its *simple class
name* to the `load(...)` string list in `LocalRepl.startReading()` — that list, not package scanning, is what loads
built-ins. (Custom/user commands instead come from `setPackagesToScan(...)` package scanning or `addCommand(...)`.)

**Data & calls.** Payloads and responses are `io.datatree.Tree` objects (from the `datatree`/moleculer stack).
Actions are invoked with `broker.call(action, params, opts)` returning a Promise — use `.then()`/`.catchError()`,
or `.waitFor()` to block. `CallOptions` carries timeout/nodeID (e.g. `dcall` targets a specific node).

**Colored output.** Do not emit raw ANSI. Prefix strings with `ColorWriter` color constants (e.g.
`ColorWriter.YELLOW` = `"§!"`, `GREEN`, `CYAN`, `GRAY`, ...). `ColorWriter` translates the `§`-codes to colors for
the local console via **JColor** (`com.diogonunes.jcolor`, `Ansi.colorize(text, Attribute…)`) and to raw ANSI escapes for telnet. For JSON, call `tree.toString("colorized-json", ...)` —
`JsonColorizer` is registered as a datatree `TreeWriter` named `"colorized-json"` in `Repl`'s static initializer.
`TextTable` renders aligned tables (used for the `help` listing).
