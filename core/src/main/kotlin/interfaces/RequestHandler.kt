package interfaces

fun interface RequestHandler<in I, out R> {
    suspend fun handleRequest(data: I): R
}