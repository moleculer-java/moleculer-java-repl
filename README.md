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

// Switch to REPL mode
broker.repl();
```

## Usage with Spring Framework

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="...">

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

![image](docs/console-java.png)


## REPL Commands

```
Commands:

  actions [options]                           List of actions
  bench <action> [jsonParams]                 Benchmark a service
  broadcast <eventName>                       Broadcast an event
  broadcastLocal <eventName>                  Broadcast an event locally
  call <actionName> [jsonParams]              Call an action
  clear <pattern>                             Delete cached entries by pattern
  dcall <nodeID> <actionName> [jsonParams]    Direct call an action
  emit <eventName>                            Emit an event
  env                                         Lists of environment properties
  events [options]                            List of event listeners
  exit, q                                     Exit application
  find <fullClassName>                        Find a class or resource
  gc                                          Invoke garbage collector
  info                                        Information about the broker
  memory                                      Show memory usage
  nodes [options]                             List of nodes
  props                                       List of Java properties
  services [options]                          List of services
  threads                                     List of threads
```

### List nodes

```bash
mol $ nodes
```

**Options**

```
    --help                    output usage information
    --details, -d             detailed list
    --all, -a                 list all (offline) nodes
    --raw                     print service registry as JSON
    --save [filename], -a     save service registry to JSON file
```

**Output**

![image](docs/nodes.png)

**Detailed output**

![image](docs/nodes-detailed.png)

### List services

```bash
mol $ services
```

**Options**

```
    --local, -l           only local services
    --skipinternal, -i    skip internal services
    --details, -d         print endpoints
    --all, -a             list all (offline) services
```

**Output**

![image](docs/services.png)

**Detailed output**

![image](docs/services-detailed.png)


### List actions

```bash
mol $ actions
```

**Options**
```
    --local, -l           only local actions
    --skipinternal, -i    skip internal actions
    --details, -d         print endpoints
    --all, -a             list all (offline) actions
```

**Output**

![image](docs/actions.png)

**Detailed output**

![image](docs/actions-detailed.png)

### List events

```bash
mol $ events
```

**Options**

```
    --local, -l           only local event listeners
    --skipinternal, -i    skip internal event listeners
    --details, -d         print endpoints
    --all, -a             list all (offline) event listeners
```

**Output**

![image](docs/events.png)

**Detailed output**

![image](docs/events-detailed.png)

### Show common information

```bash
mol $ info
```

**Output**

![image](docs/info.png)

### List environment variables

```bash
mol $ env
```

### List system properties of Java

```bash
mol $ props
```

### Call an action

```bash
mol $ call math.add {"a":3,"b":4}
```

**Output**

![image](docs/call.png)

#### Call an action with parameters

```bash
mol $ call math.add --a 5 --b Bob --c --no-d --e.f "hello"
```

#### Call with JSON string parameter

```bash
mol $ call math.add {"a": 5, "b": "Bob", "c": true, "d": false, "e": { "f": "hello" } }
```

Params will be `{"a":5, "b":"Bob", "c":"--no-d", "e":{ "f":"hello" }}`

**Output**

![image](docs/call2.png)

### Direct call

Get health info from `node-12` node

```bash
mol $ dcall node-12 $node.health
```

>Parameter passing is similar to `call` command.

### Emit an event

```bash
mol $ emit user.created
```

#### Emit an event with parameters

```bash
mol $ emit user.created --a 5 --b Bob --c --no-d --e.f "hello"
```

Params will be `{"a":5, "b":"Bob", "c":"--no-d", "e":{ "f":"hello" }}`

### Benchmark services

Moleculer REPL module has a new bench command to measure your services.

```bash
# Call service until 5 seconds (default)
mol $ bench $node.list
```

**Output**

![image](docs/bench.png)

```bash
# Call service until 30 seconds
mol $ bench $node.list --time 30
```

**Output**

![image](docs/bench2.png)

```bash
# Call service 5000 times
mol $ bench $node.list --num 5000
```

**Output**

![image](docs/bench3.png)

#### Parameters

```bash
mol $ bench math.add --time 10 --a 3 --b 6
# or
mol $ bench math.add --time 10 {"a":3,"b":6}
```

**Output**

![image](docs/bench4.png)

### Dump hierarchy of threads

```bash
mol $ threads
```

**Output**

![image](docs/threads.png)

### Show JVM's heap usage

```bash
mol $ memory
```

**Output**

![image](docs/memory.png)

### Invoke Garbage Collector

```bash
mol $ gc
```

**Output**

![image](docs/gc.png)

# License

Moleculer-java-repl is available under the [MIT license](https://tldrlegal.com/license/mit-license).
