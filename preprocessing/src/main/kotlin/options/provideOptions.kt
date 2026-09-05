package options

data class MediatorOptions(
    val parallelNotifications: Boolean = true
)

fun provideOptions() {
    val lines = object {}.javaClass.getResourceAsStream("mediaktor.yaml").bufferedReader().lines()
}