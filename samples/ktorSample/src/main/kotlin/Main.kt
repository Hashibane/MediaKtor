import database.FakeDatabase
import database.ItemModel
import handlers.BackupEvent
import handlers.Permission
import handlers.ReadInventoryQuery
import handlers.WriteInventoryCommand
import mediaktor.core.interfaces.Mediator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mediaktor.koin.provideMediator
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import repositories.ReadRepository
import repositories.WriteRepository

fun main(args: Array<String>) {
    EngineMain.main(args)
}


fun Application.module() {
    val appModule = module {
        // Your handler dependencies - loggers, database connections etc.
        single { log }
        single { FakeDatabase("<Connection String>") }
        singleOf(::ReadRepository)
        singleOf(::WriteRepository)

        provideMediator()
    }

    install(Koin) {
        modules(appModule)
    }

    val mediator by inject<Mediator>()

    // for simplicity everything in GETs
    routing {
        get("/authorized/items/{itemId}") {
            val itemId = call.parameters["itemId"].toString()
            val response = mediator.send(ReadInventoryQuery(itemId, Permission.AUTHORIZED))
            call.respond(HttpStatusCode.OK, response.toString())
        }

        get("/unauthorized/items/{itemId}") {
            val itemId = call.parameters["itemId"].toString()
            val response = mediator.send(ReadInventoryQuery(itemId, Permission.NOT_AUTHORIZED))
            call.respond(HttpStatusCode.OK, response.toString())
        }

        get("/authorized/items/create/{itemId}") {
            val itemId = call.parameters["itemId"].toString()
            val response = mediator.send(
                WriteInventoryCommand(
                    ItemModel(itemId, "SomeName", 12, "SomeConfidentialData"),
                    Permission.AUTHORIZED
                )
            )
            call.respond(HttpStatusCode.OK, response.toString())
        }

        get("/backup") {
            val responseText = mediator.publish(BackupEvent)
            call.respond(HttpStatusCode.OK, responseText.toString())
        }
    }
}