package exceptions

class DatabaseException(override val message: String) : Exception(message)