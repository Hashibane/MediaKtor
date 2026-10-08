package mediaktor.core.exceptions

/** Thrown whenever there is a failure to provide DI framework */
class DIException(override val message: String) : IllegalStateException(message)