# REPL Developer Console for Moleculer

An interactive developer console for the [Moleculer for Java](https://moleculer-java.github.io/site/)
microservices framework. It is itself a Moleculer `Service` that runs text commands against a
live `ServiceBroker`: **call** actions, **emit** events, **benchmark** the response time of a
service, inspect the cluster, and more. The console works over standard input/output or telnet,
and you can register your own **custom commands**.

## Documentation

[![Documentation](https://raw.githubusercontent.com/moleculer-java/site/master/docs/docs-button.png)](https://moleculer-java.github.io/site/moleculer-repl.html)

## Download

```xml
<dependency>
    <groupId>com.github.berkesa</groupId>
    <artifactId>moleculer-java-repl</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Quick start

Install the console into a running broker, then type commands at the prompt:

```java
ServiceBroker broker = new ServiceBroker("node-1");
broker.start();

broker.createService(new LocalRepl());   // interactive console on stdin/stdout
```

```text
call math.add --a 5 --b 3     // call an action
bench math.add --a 5 --b 3    // measure its response time
help                          // list the available commands
```

Use `RemoteRepl` instead of `LocalRepl` to expose the console over telnet.

## Requirements

Java 21 or newer.

## License

This project is available under the [MIT license](https://tldrlegal.com/license/mit-license).
