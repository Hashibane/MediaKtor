package annotations

sealed interface HandlerType {
    object SingleOf : HandlerType
    object Transient : HandlerType
    object Scoped : HandlerType
}