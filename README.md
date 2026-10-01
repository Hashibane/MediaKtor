# MediaKtor
[![codecov](https://codecov.io/github/Hashibane/MediaKtor/graph/badge.svg?token=5271LZAKP3)](https://codecov.io/github/Hashibane/MediaKtor)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)

Implementation of Mediator pattern using KSP and code generation. The project provides concise annotation-based API for
functions with minimal overhead. It comes with out-of-the box pipeline support, notifications and optional Koin integration.

The key differences from other mediator projects include:
- Function-based handlers
- Any non-nullable type as a message
- Code generation (no reflection)
- Pipeline order and targeting flexibility

You can check the [API documentation](hashibane.github.io/MediaKtor/).

> In honor of our humble beginnings, the name reflects framework, which early versions were written for - Ktor.

## Contents

- [MediaKtor](#mediaktor)
    - [Contents](#contents)
    - [1. Install](#1-install)
    - [2. Quickstart](#2-quickstart-with-koin--ktor)
    - [3. Usage](#3-usage)
        - [3.1 Message types](#31-message-types)
        - [3.2 Handler types](#32-handler-types)
            - [3.2.1 Request Handlers](#321-request-handlers)
            - [3.2.2 Notification Handlers](#322-notification-handlers)
            - [3.2.3 Pipeline Behaviors](#323-pipeline-behaviors)
        - [3.3 Request type matching](#33-request-type-matching)
        - [3.4 Handler options](#34-handler-options)
    - [4. DI integration](#4-di-integration)
        - [4.1 Bare (no DI)](#41-bare-no-di)
        - [4.2 Koin](#42-koin-integration)
    - [5. Usage tips](#5-usage-tips)
    - [6. Contributions](#6-contributions)
---

## 1. Install

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
    implementation("io.github.hashibane:mediaktor-core:$version")

    // for no DI framework
    ksp("io.github.hashibane:mediaktor-bare:$version")

    // for koin
    ksp("io.github.hashibane:mediaktor-koin:$version")
    implementation("io.github.hashibane:mediaktor-koin:$version")

    // for koin libs.versions.toml configuration see koin documentation
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core) // or other corresponding koin platform
}
```
## 2. Quickstart with Koin & Ktor

```kotlin
class DBConnection(val connectionString: String)

@RequestHandler
fun intToString(arg: Int, connection: DBConnection): String {
    println("Connecting to database: ${connection.connectionString}")
    return arg.toString()
}

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val appModule = module {
        // Your handler dependencies - loggers, database connections etc. that are required for handlers/pipelines
        single { DBConnection("<SomeUrl>") }

        provideMediator()
    }
    // or startKoin { modules(appModule) } in koin core
    install(Koin) {
        modules(appModule)
    }

    // or val mediator: Mediator = get() in koin core
    val mediator by inject<Mediator>()

    routing {
        get("/{number}") {
            val number = call.parameters["number"].toInt()
            val response = mediator.send(number)
            call.respond(HttpStatusCode.OK, response.toString())
        }
    }
}
```

> [!NOTE]
> For more in depth sample look at `samples/ktorSample`

## 3. Usage

### 3.1 Message types

Different from other implementations, there is no dedicated Message or Notification type. You may use any non-nullable
type as your request type.

### 3.2 Handler types

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
> By convention, the first argument of annotated function must be the actual request type and all other dependencies are injected
> on function call. Using a DI framework to resolve dependencies is the preferred method.

If no corresponding handlers request type is found for the request, `IllegalArgumentException` is thrown.

### 3.2.1 Request Handlers

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

### 3.2.2 Notification Handlers

Notification handlers are functions annotated with ```@NotificationHandler```. Similarly to request handlers, they
respond to an exact [sub]type of the message. For any given type, there may be any number of notification handlers. The return
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

### 3.2.3 Pipeline Behaviors

Pipeline behaviors are functions annotated with ```@PipelineBehavior```. All pipelines must contain the `next` parameter
with type `suspend () -> <HandlerReturnType>` or `suspend () -> Any?` depending on the pipeline target. All pipelines
must be `suspend` functions.

#### Example

```kotlin
data class Request(val content: String)

@PipelineBehavior(target = PipelineTarget.NOTIFICATIONS)
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

### 3.3 Request type matching

If you register a type, then mediator calls the most specific handler (request or notification)
for a given type. A handler will handle its request subtypes if there is no handler that would handle them.
A pipeline will also intercept any calls that involve its request subtype.

#### Example

```kotlin
interface Request
data class RequestSubtype(val content: String) : Request

@PipelineBehavior(target = PipelineTarget.NOTIFICATIONS)
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
    val mediator: Mediator = get() // in Koin
    val response = mediator.publish(message)
}
```

### 3.4 Handler options

#### General options

- **Lifespan**

  Handlers are singletons (`SINGLE`) by default. You can also specify `FACTORY` to create new handler every call.
  Lifespan does not matter when using `mediaktor-bare`.

#### Notification handlers

- **Order**

  You may specify which notification handler goes first with the order parameter. The lower the value, the earlier
  the handler is called. For example, handler with order value `0` gets called before handler with order `1`. The default
  order is `Int.MIN_VALUE` (outermost possible).

- **Parallel**

  Notifications calls are wrapped inside `launch {}` and by default run in parallel.
  If you specify `SEQUENTIAL` value for `parallel` parameter, the handler gets called without the `launch {}`.

#### Pipeline Handlers

- **Order**

  Works the same as notification handlers order. `REQUEST_MATCH` pipelines are applied strictly after any other pipelines, but they respect order value between eachother. Default value is `INT.MIN_VALUE`.

- **Target**

  Specifies to which handlers the pipeline should be applied. Every pipeline must have return type and `next` parameter type chosen according
  to their target.

  | Target          | Applied to                                                 | Return type                               | `next` type              |
  |-----------------|------------------------------------------------------------|-------------------------------------------|--------------------------|
  | `REQUEST_MATCH` | Request handlers matching<br/>request and return type      | Request handlers<br/>that return type `T` | ```suspend () -> T```    |
  | `REQUESTS`      | Request handlers matching<br/>request type                 | `Any?`                                    | ```suspend () -> Any?``` |
  | `NOTIFICATIONS` | Notification handlers                                      | `Unit`                                    | ```suspend () -> Unit``` | 
  | `BOTH`          | Notification or request<br/>handlers matching request type | `Any?`                                    | ```suspend () -> Any?``` |


    If a `REQUEST_MATCH` pipeline does not match the return type but matches the request type, it is not applied and a warning is emitted.

## 4. DI integration

### 4.1 Bare (no DI)

In bare version, you use generated names without any abstractions. The generated mediator class is `Mediator__Impl` declared in package of the same name. If you want to use handler generated from function `functionName()` from package `packageName`, the generated class will have the same package as function and name `Handler__<packageNameWithDotsReplaced>__functionName`, where `packageNameWithDotsReplaced` is `packageName`, where each dot was replaced by double `_`.

#### Example

```kotlin
import Mediator__Impl.Mediator__Impl
import annotations.RequestHandler

@RequestHandler
fun testHandler(arg: Int): String = arg.toString()

suspend fun runBareMediator() {
    val mediator = Mediator__Impl({ Handler____testHandler() })

    println(mediator.send(4))
}
```
In this example, the package name is empty so the generated name is:

`Handler + __ + (empty string) + __ + functionName = Handler____functionName`

### 4.2 Koin integration

To automatically inject all required dependencies, declare a Koin module and call
`provideMediator()`:

#### Example
```kotlin
val appModule = module {
    // Your handler dependencies - loggers, database connections etc.
    // ...

    provideMediator()
}

fun main() {
    // for core
    startKoin {
        modules(appModule)
    }

    val mediator: Mediator = get()
}
```

then you will be able to inject mediator via `Mediator` interface.

## 5. Usage tips

Pipelines are generally not meant to care about what the `next` parameter will return. If not needed, you should pass
`next()` result to the return as is or wrapped. If you need to check for the returned type or modify it, you
should use `REQEST_MATCH` target.


Using MediaKtor with DI framework is the recommended way. Otherwise, you will need to track handler classes names and
implementation details that are usually hidden.

## 6. Contributions

Thank you for your work <3

<a href="https://github.com/Hashibane/MediaKtor/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Hashibane/MediaKtor&max=24" />
</a>

---
Special thanks to [codringher](https://github.com/codringher) for initial feature suggestions and review.
