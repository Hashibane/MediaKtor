# MediaKtor
TODO - code coverage

Implementation of Mediator pattern using KSP and code generation. The project provides concise annotation-based API for
functions with minimal overhead. It comes with out-of-the box pipeline support, notifications and Koin integration.

The key differences from other mediator projects include:
- Function-based handlers
- Any non-nullable type as a message
- Code generation (no reflection)
- Pipeline order and targeting flexibility

> In honor of our humble beginnings, the name reflects framework, which early versions were written for - Ktor.

## Contents

TODO

## Install

TODO

## Quickstart

TODO

## Usage

### Message types

Different from other implementations there is no dedicated Message or Notification type. You may use any non-nullable
type as your request type. 

### Handler types

Handlers are implemented through annotations. This means any function annotated with either of:

 - ```@RequestHandler```
 - ```@NotificationHandler```
 - ```@PipelineBehavior```

| Handler               | Calls on request | Returns     | Dispatched with                           |
|-----------------------|------------------|-------------|-------------------------------------------|
| `RequestHandler`      | exactly one      | `T`         | `mediator.send()`                         |
| `NotificationHandler` | one or more      | Unit        | `mediator.publish()`                      |
| `PipelineBehavior`    | zero or more     | Any? or `T` | `mediator.send()` or `mediator.publish()` | (see options)

> [!IMPORTANT]
> By convention, the first argument of annotated function must be the actual request and all others are dependencies injected
> on a function call. Using a DI framework to resolve dependencies is a preferred method.

If no corresponding handlers request type is found for the request, IllegalArgumentException is thrown.

Every handler has also config options. For handler options see <Link to handler options>

### Handler options

#### General options

- Lifespan 
    Handlers are singletons (`SINGLE`) by default. You can also specify `FACTORY` to create new handler every call.

#### Notification handlers

- Order
    You may specify which notification handler goes first with the order parameter. The lower the value, the earlier
    the handler is called. For example handler with order value 0 gets called before handler with order 1.
- Parallelism
    Notifications are called inside ```launch {}``` and run in parallel. If you specify `SEQUENTIAL` value for parallel, the
    handler gets called like ordinary function.

#### Pipeline Handlers

- Order
    Works the same like notification handlers order. `PASS` pipelines are always applied first regardless of pipeline order.
- Target
    Specifies target for the pipeline. `PASS` targets return Any? type, because they are meant to pass return type of the
    next parameter. `STRICT` targets return same type as underlying handlers.

    There are two types of pipelines based on matching of the request type.
    For strict matching the return type of any given handler exactly as is - ```STRICT```, or
    for just passing the return value - ```PASS```. You may apply pipelines selectively to notifications or requests and
    specify their strictness by ```target```. In total there are six targets:

    | Target                 | Applied to                                                                            | Request type                       | Return type                  |
    |------------------------|---------------------------------------------------------------------------------------|------------------------------------|------------------------------|
    | `STRICT_REQUESTS`      | Request handlers that match request and return type                                   | Request handlers request type      | Request handlers return type |
    | `STRICT_NOTIFICATIONS` | Notification handlers that match request type                                         | Notification handlers request type | Unit                         |
    | `STRICT_BOTH`          | Notification or request handlers with matching <br/>request type and Unit return type | Handlers request type              | Unit                         |
    | `PASS_REQUEST`         | Request handlers that match request type                                              | Request handlers request type      | Any?                         |
    | `PASS_NOTIFICATIONS`   | Notification handlers that match request type                                         | Notification handlers request type | Any?                         |
    | `PASS_BOTH`            | Notification or request handlers that match request type                              | Handlers request type              | Any?                         |


### Request Handlers

Request handlers are functions annotated with ```@RequestHandler```. 

Request handler will respond only if a request type (or subtype) matches the message type sent from the mediator
and may return any given type. There may only be one request handler for any given type. Requests are sent
via mediators ```send()``` method.

#### Example
```
data class Request(val content: String)

@RequestHandler(HandlerLifespan.FACTORY)
fun requestOne(arg: Request, logger: Logger): String {
    logger.info("Received message on requestOne with content: ${arg.content}")
    return arg.content
}

fun sendMessage(message: Request) {
    val mediator by inject<Mediator>() // in Koin
    val response = mediator.send(message)
}
```

### Notification Handlers

Notification handlers are functions annotated with ```@NotificationHandler```. Similarly to request handlers, they
respond to exact (sub)type of the message. For any given type, there may be any number of notification handlers. The return
type must always be Unit. Notifications are published via mediators ```publish()``` method. `

Notifications are run in order and can be run parallelly (the default). For details check out options ---LINK---.

#### Example
```
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
    val mediator by inject<Mediator>() // in Koin
    val response = mediator.publish(message)
}
```

### Pipeline Behaviors

Pipeline behaviors are functions annotated with ```@PipelineBehavior```. All pipelines must contain the `next` parameter
with type `suspend () -> <HandlerReturnType or Any?>` depending on the pipeline target. All pipelines
must be suspend functions.

Pipelines are ordered according to their order value and target (---see options---)

If a strict pipeline does not match the return type but matches the request type it is not applied and a warning is emitted.

#### Example

```
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
    val mediator by inject<Mediator>() // in Koin
    val response = mediator.publish(message)
}
```

### Request type inheritance

If you register a type, then mediator calls the most specific handler (request or notification) 
for given type. A handler will handle its requests subtypes if there is no handler that would handle them.
A pipeline will also intercept any calls that involve its request subtype.

#### Example

```
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

### Koin integration

To automatically inject all required dependencies, declare a Koin module and call 
`provideMediator()`:

```
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