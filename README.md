# REPL for Moleculer

Java REPL (Interactive Developer Console) for [Moleculer](https://moleculer-java.github.io/moleculer-java/).

## Download

**Maven**

```xml
<dependencies>
	<dependency>
		<groupId>com.github.berkesa</groupId>
		<artifactId>moleculer-java-repl</artifactId>
		<version>1.0.2</version>
		<scope>runtime</scope>
	</dependency>
</dependencies>
```

**Gradle**

```gradle
dependencies {
	compile group: 'com.github.berkesa', name: 'moleculer-java-repl', version: '1.0.2' 
}
```

## Usage from code

```java
// Create and Service Broker
ServiceBroker broker = new ServiceBroker();
broker.start();

// Start console
broker.repl();
```

## Screenshot

![Java-based REPL Console](docs/console-java.png)

# License
moleculer-java-repl is available under the [MIT license](https://tldrlegal.com/license/mit-license).
