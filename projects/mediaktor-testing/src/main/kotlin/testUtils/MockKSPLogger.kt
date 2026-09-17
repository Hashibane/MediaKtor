package testUtils

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode


object MockKSPLogger : KSPLogger {
    override fun error(message: String, symbol: KSNode?) {}

    override fun exception(e: Throwable) {}

    override fun info(message: String, symbol: KSNode?) {}

    override fun logging(message: String, symbol: KSNode?) {}

    override fun warn(message: String, symbol: KSNode?) {}
}