# MediaKtor
TODO - code coverage

Implementation of Mediator pattern using KSP and code generation. The project provides concise annotation-based API for
functions with minimal overhead. It comes with out-of-the box pipeline support, notifications and optional Koin integration.

The key differences from other mediator projects include:
- Function-based handlers
- Any non-nullable type as a message
- Code generation (no reflection)
- Pipeline order and targeting flexibility

> In honor of our humble beginnings, the name reflects framework, which early versions were written for - Ktor.

## Contents

TODO

## Install

Include plugin KSP in your `build.gradle.kts`:
```kotlin
plugins {
    // ...
    id("com.google.devtools.ksp") version "2.3.11" // or other
}
```

and `mediaktor-core` and one of the DI framework extensions in dependencies:

```kotlin
dependencies {
    // ...
    
    val version = "<Current Version>"
    implementation("com.hashibane:mediaktor-core:$version")

    // for no DI framework
    ksp("com.hashibane:mediaktor-bare:$version")

    // for koin
    ksp("com.hashibane:mediaktor-koin:$version")
    implementation("com.hashibane:mediaktor-koin:$version")
    
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core) // or other corresponding koin platform
}
```

## Quickstart with Koin

```kotlin
class DBConnection(val connectionString: String)

@RequestHandler
fun intToString(arg: Int, connection: DBConnection): String {
    println("Connecting to database: ${connection.connectionString}")
    return arg.toString()
}

fun main() {
    val appModule = module {
        // Your handler dependencies - loggers, database connections etc. that are required for handlers/pipelines
        single { DBConnection("<SomeUrl>") }

        provideMediator()
    }

    startKoin {
        modules(appModule)
    }

    val mediator: Mediator = get()
    
    println(mediator.send(1234))
}
```

> [!NOTE]
> For more in depth sample look at `samples/ktorSample`

## Usage

### Message types

Different from other implementations, there is no dedicated Message or Notification type. You may use any non-nullable
type as your request type.

### Handler types

Handlers are implemented through annotations. This means any function annotated with either of:

 - ```@RequestHandler```
 - ```@NotificationHandler```
 - ```@PipelineBehavior```

| Handler               | Calls on request | Returns       | Dispatched with                           |
|-----------------------|------------------|---------------|-------------------------------------------|
| `RequestHandler`      | exactly one      | `T`           | `mediator.send()`                         |
| `NotificationHandler` | one or more      | `Unit`        | `mediator.publish()`                      |
| `PipelineBehavior`    | zero or more     | `Any?` or `T` | `mediator.send()` or `mediator.publish()` |

Every handler has also config options. PipelineBehavior types depend on their `target`.

> [!IMPORTANT]
> By convention, the first argument of annotated function must be the actual request and all others are dependencies injected
> on a function call. Using a DI framework to resolve dependencies is a preferred method.

If no corresponding handlers request type is found for the request, `IllegalArgumentException` is thrown.

### Request Handlers

Request handlers are functions annotated with ```@RequestHandler```.

Request handler will respond only if a request type (or subtype) matches the message type sent from the mediator
and may return any given type. There may only be one request handler for any given request type. Requests are sent
via mediators ```send()``` method.

#### Example
```kotlin
data class Request(val content: String)

@RequestHandler(HandlerLifespan.FACTORY)
fun requestOne(arg: Request, logger: Logger): String {
    logger.info("Received message on requestOne with content: ${arg.content}")
    return arg.content
}

fun sendMessage(message: Request) {
    val mediator: Mediator = get() // in Koin
    val response = mediator.send(message)
}
```

### Notification Handlers

Notification handlers are functions annotated with ```@NotificationHandler```. Similarly to request handlers, they
respond to an exact (sub)type of the message. For any given type, there may be any number of notification handlers. The return
type must always be `Unit`. Notifications are published via mediators ```publish()``` method.

#### Example
```kotlin
data class Request(val content: String)

@NotificationHandler(order = 5)
fun notifierOne(arg: Request, logger: Logger) {
    logger.info("Received notification on notifierOne with content: ${arg.content}")
}


@NotificationHandler(parallel = NotificationParallel.SEQUENTIAL)
fun notifierTwo(arg: Request, logger: Logger) {
    logger.info("Received notification on notifierTwo with content: ${arg.content}")
}

fun publishNotification(notification: Request) {
    val mediator: Mediator = get() // in Koin
    val response = mediator.publish(message)
}
```

### Pipeline Behaviors

Pipeline behaviors are functions annotated with ```@PipelineBehavior```. All pipelines must contain the `next` parameter
with type `suspend () -> <HandlerReturnType or Any?>` depending on the pipeline target. All pipelines
must be `suspend` functions.

#### Example

```kotlin
data class Request(val content: String)

@PipelineBehavior(target = PipelineTarget.STRICT_NOTIFICATIONS)
suspend fun verify(arg: Request, next: suspend () -> Unit) {
    if (arg.content.contains("OK")) {
        next()
    } else {
        throw RuntimeException("Message must contain OK!")
    }
}

@NotificationHandler
fun notifierOne(arg: Request, logger: Logger) {
    logger.info("Received notification on notifierOne with content: ${arg.content}")
}

fun publishNotification(notification: Request) {
    val mediator: Mediator = get() // in Koin
    val response = mediator.publish(message)
}
```

### Request type inheritance

If you register a type, then mediator calls the most specific handler (request or notification) 
for a given type. A handler will handle its requests subtypes if there is no handler that would handle them.
A pipeline will also intercept any calls that involve its request subtype.

#### Example

```kotlin
interface Request
data class RequestSubtype(val content: String) : Request

@PipelineBehavior(target = PipelineTarget.STRICT_NOTIFICATIONS)
suspend fun verify(arg: Request, next: suspend () -> Unit) {
    if (arg.content.contains("OK")) {
        next()
    } else {
        throw RuntimeException("Message must contain OK!")
    }
}

@NotificationHandler
fun notifierOne(arg: RequestSubtype, logger: Logger) {
    logger.info("Received notification on notifierOne with content: ${arg.content}")
}

fun publishNotification(notification: Request = RequestSubtype("subtype!")) {
    val mediator by inject<Mediator>() // in Koin
    val response = mediator.publish(message)
}
```

### Handler options

#### General options

- Lifespan
  Handlers are singletons (`SINGLE`) by default. You can also specify `FACTORY` to create new handler every call.
  Lifespan does not matter when using `mediaktor-bare`.

#### Notification handlers

- Order
  You may specify which notification handler goes first with the order parameter. The lower the value, the earlier
  the handler is called. For example, handler with order value 0 gets called before handler with order 1. The default
  order is `Int.MIN_VALUE` (outermost possible).
- Parallelism
  Notifications calls are wrapped inside `launch {}` and by default run in parallel.
  If you specify `SEQUENTIAL` value for `parallel` parameter, the handler gets called without the `launch {}`.

#### Pipeline Handlers

- Order
  Works the same as notification handlers order. `REQUEST_MATCH` pipelines are applied strictly after any other pipelines, but
  they respect order value between eachother. Default value is `INT.MIN_VALUE`.
- Target
  Specifies target for the pipeline. Every pipeline must have return type and "next" parameter type chosen according
  to their target.

  | Target           | Applied to                                               | Return type                        | `next` type              |
  |------------------|----------------------------------------------------------|------------------------------------|--------------------------|
  | `REQUESTS_MATCH` | Request handlers that match request and return type      | Request handlers return type [`T`] | ```suspend () -> T```    |
  | `REQUEST`        | Request handlers that match request type                 | `Any?`                             | ```suspend () -> Any?``` |
  | `NOTIFICATIONS`  | Notification handlers                                    | `Unit`                             | ```suspend () -> Unit``` | 
  | `BOTH`           | Notification or request handlers that match request type | `Any?`                             | ```suspend () -> Any?``` |

If a `REQUEST_MATCH` pipeline does not match the return type but matches the request type, it is not applied and a warning is emitted.

### Koin integration

To automatically inject all required dependencies, declare a Koin module and call 
`provideMediator()`:

#### Example
```kotlin
val appModule = module {
        // Your handler dependencies - loggers, database connections etc.
        single { logger }

        provideMediator()
}

// for Ktor
install(Koin) { 
    modules(appModule)
}
```

then you will be able to inject mediator via `Mediator` interface.

### Usage tips

Pipelines are generally not meant to care about what the next parameter will return. If not needed, you should pass
the `next()` result to the return as is or wrapped. If you need to check for the returned type or modify it, you
should use `REQEST_MATCH` target.


Using MediaKtor with DI framework is the recommended way. Otherwise, you will need to track handler classes manually and
handle implementation details that are otherwise hidden.