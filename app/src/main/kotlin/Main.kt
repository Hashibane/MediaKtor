import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    routing {
        get("/test/{text}") {
            val echoText = call.parameters["text"].toString()
            call.respond(HttpStatusCode.OK, echoText)
        }
    }
}