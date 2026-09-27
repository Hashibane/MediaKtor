package preprocessing.processors

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference

internal object TypeCache {
    private val cache: MutableMap<KSTypeReference, KSType> = mutableMapOf()

    operator fun get(reference: KSTypeReference): KSType = when (val hit = cache[reference]) {
        null -> {
            val resolved = reference.resolve()
            cache[reference] = resolved
            resolved
        }
        else -> hit
    }
}