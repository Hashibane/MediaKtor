import handlers.Request
import interfaces.Mediator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mediaktorKoin.provideMediator
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    EngineMain.main(args)
}

val appModule = module {
    provideMediator()
}

fun Application.module() {
    install(Koin) {
        modules(appModule)
    }

    val mediator by inject<Mediator>()

    routing {
        get("/test/{text}") {
            val echoText = call.parameters["text"].toString()
            val response = mediator(Request(echoText))
            call.respond(HttpStatusCode.OK, response.toString())
        }
    }
}