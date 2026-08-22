package errors

//TODO : Change to more sane exception subclass
class PreprocessingException(override val message: String) : Exception()