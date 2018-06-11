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
// Create Service Broker
ServiceBroker broker = new ServiceBroker();
broker.start();

// Start console
broker.repl();
```

## Usage with Spring Framework

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>

	<!-- PACKAGE OF YOUR MOLECULER SERVICES -->

	<context:component-scan base-package="package.of.my.services" />

	<!-- INSTALL DEVELOPER CONSOLE -->

	<bean id="$repl" class="services.moleculer.repl.LocalRepl" />

	<!-- CONFIGURE TRANSPORTER -->

	<bean id="transporter" class="services.moleculer.transporter.TcpTransporter" />

	<!-- OTHER SERVICE BROKER SETTINGS -->

	<bean id="brokerConfig" class="services.moleculer.config.ServiceBrokerConfig">
		<property name="nodeID" value="node-1" />
		<property name="transporter" ref="transporter" />
	</bean>

	<!-- CREATE SERVICE BROKER INSTANCE -->

	<bean id="broker" class="services.moleculer.ServiceBroker"
		init-method="start" destroy-method="stop">
		<constructor-arg ref="brokerConfig" />
	</bean>

	<!-- MOLECULER / SPRING INTEGRATOR -->

	<bean id="registrator" class="services.moleculer.config.SpringRegistrator" />

</beans>
```

## Screenshot

![Java-based REPL Console](https://github.com/moleculer-java/moleculer-java-repl/raw/master/docs/console-java.png)

# License
moleculer-java-repl is available under the [MIT license](https://tldrlegal.com/license/mit-license).
