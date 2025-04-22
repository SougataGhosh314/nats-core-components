📦 nats-core-components

nats-core-components is a modular, extensible, and production-ready core library designed for building secure, 
observable, and schema-validated microservices that communicate over NATS. It provides reusable building blocks 
that simplify writing producers, consumers, and RPC-style functions using NATS with protobuf payloads.

🚀 What It Does

This library allows you to:
* Securely connect to a NATS server using TLS + JWT + NKEY authentication. 
* Register event-driven components (suppliers, consumers, functions) via JSON configuration. 
* Bind a single handler to multiple topics with explicit message types. 
* Implement schema validation at runtime using a central registry. 
* Add fan-out capabilities for multi-topic publish flows. 
* Observe system health with Spring Boot Actuator endpoints. 
* Monitor usage with Micrometer-based metrics. 
* Perform graceful shutdowns on dispatcher threads and connections. 
* Customize threading with executor config for high-throughput suppliers.

🧱 Key Modules

🔐 Security

Connect securely using TLS with CA certs and JWT + NKEY authentication.
Easily toggle secure/unsafe modes via properties.

⚙️ Dispatchers
Modular dispatcher types:
* ConsumerDispatcher: handles raw messages from subscribed topics. 
* FunctionDispatcher: listens for requests, processes, and responds to reply topics. 
* SupplierDispatcher: continuously pushes messages (in a thread pool). 
* Fanout variants: FunctionFanoutDispatcher, SupplierFanoutDispatcher send multiple responses per request/tick.

All dispatchers support:
MDC logging
Metrics tracking
Topic validation

📜 Contract Enforcement

Uses nats-schema-registry to validate Protobuf contracts.
Enforces strict topic-message bindings at startup.
Prevents incompatible producers/consumers from silently failing at runtime.

🛠️ Config-First Design

Driven entirely by a declarative event-config.json file.
Support for:
* multiple topics per handler 
* explicit message types 
* queue groups for load balancing 
* fanout support

📊 Observability 

* /actuator/health verifies NATS connection. 
* /actuator/metrics exposes:
  * nats.message.sent 
  * nats.message.received 
  * nats.message.error

MDC logs include:

correlationId for tracking end-to-end request flow

🧵 Thread Pool Support

Suppliers run in configurable thread pools:

nats.supplier.thread-pool-size: 4

🚦 Graceful Shutdown

* Dispatchers and NATS connections are drained and closed via @PreDestroy. 
* Prevents message loss and dangling consumers.


🧪 How to Use This Library

1️⃣ Add Dependency
Assuming you’ve published the core to your local or remote Maven repo:

<dependency>
    <groupId>com.sougata</groupId>
    <artifactId>nats-core-components</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>

2️⃣ Create an event-config.json

Define what each component (supplier, consumer, etc.) listens to or emits:

{
"components": [
{
"handlerType": "FUNCTION",
"handlerClass": "com.yourapp.logic.MyFunction",
"readTopics": [
{
"topicName": "user.create.v1",
"messageType": "com.example.protos.UserRequest",
"queueGroup": "creator"
}
],
"writeTopics": [
{
"topicName": "user.response.v1",
"messageType": "com.example.protos.UserResponse"
}
]
}
]
}


3️⃣ Define Your Logic

Implement handler interfaces:
* PayloadFunction 
* PayloadFunctionFanout 
* PayloadSupplier 
* PayloadConsumer 
* PayloadSupplierFanout

Example:

public class MyFunction implements PayloadFunction {
public PayloadWrapper<byte[]> process(PayloadWrapper<byte[]> request) {
// parse, process, respond
}
}


4️⃣ Configure application.yml

nats.url: nats://localhost:4222
nats.credsFile: C:/nats-config/users/my-service.creds

spring.config.import: optional:classpath:event-config.json


☁️ Run a NATS Server

🧪 Dev (no auth)

nats-server -DV

🔐 Production (secure: JWT + NKEY + TLS)
* Install nsc.
Create operator, account, and users. 
* Enable resolver mode:

port: 4222
operator: "/etc/nats/MyOperator.jwt"
resolver: {
type: full
dir: "/etc/nats/accounts"
}
tls {
cert_file: "/etc/nats/certs/server-cert.pem"
key_file: "/etc/nats/certs/server-key.pem"
ca_file: "/etc/nats/certs/ca.pem"
}

Start it via Docker:

docker run -p 4222:4222 -v ./nats-config:/etc/nats nats:2.11.1 -c /etc/nats/nats-server.conf

🔍 Schema Registry

We recommend using a central registry like nats-schema-registry.

✔️ Automatically validates each topic’s expected Protobuf message
✔️ SHA-256 validation of .proto descriptors
✔️ Prevents accidental mismatches between producer and consumer
✔️ Supports versioned topics (user.create.v1, user.create.v2, etc.)

Check out the example client built using this library: https://github.com/SougataGhosh314/user-service-nats-client