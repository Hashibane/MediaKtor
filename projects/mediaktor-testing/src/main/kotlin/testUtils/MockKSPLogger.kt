package testUtils

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import io.github.oshai.kotlinlogging.KotlinLogging


object MockKSPLogger : KSPLogger {
    val logger = KotlinLogging.logger {  }
    override fun error(message: String, symbol: KSNode?) {
        logger.error { message }
    }

    override fun exception(e: Throwable) {
        logger.error { "Exception thrown: $e" }
    }

    override fun info(message: String, symbol: KSNode?) {
        logger.info { message }
    }

    override fun logging(message: String, symbol: KSNode?) {
        logger.info { message }
    }

    override fun warn(message: String, symbol: KSNode?) {
        TODO("Not yet implemented")
    }
}