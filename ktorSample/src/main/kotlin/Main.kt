import handlers.Request
import interfaces.Mediator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mediaktorKoin.provideMediator
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    EngineMain.main(args)
}



fun Application.module() {
    val appModule = module {
        // Your handler dependencies - loggers, database connections etc.
        single { log }

        provideMediator()
    }

    install(Koin) {
        modules(appModule)
    }

    val mediator by inject<Mediator>()

    routing {
        get("/requests/{text}") {
            val echoText = call.parameters["text"].toString()
            val response = mediator.send(Request(echoText))
            call.respond(HttpStatusCode.OK, response.toString())
        }

        get("/notifications/{text}") {
            val echoText = call.parameters["text"].toString()
            val response = mediator.publish(Request(echoText))
            call.respond(HttpStatusCode.OK, response.toString())
        }
    }
}