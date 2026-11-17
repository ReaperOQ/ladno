package app.ladno.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

@Serializable
enum class Currency(val precision: Int) { RUB(2), USD(2), EUR(2), JPY(0), KWD(3) }

enum class MoneyRounding { TOWARD_ZERO, HALF_UP, HALF_EVEN }

/** Signed minor units; operation amounts impose their own positivity constraint. */
@Serializable
data class Money(
    @Serializable(with = DecimalLongSerializer::class) val minorUnits: Long,
    val currency: Currency,
) {
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        return copy(minorUnits = checkedAdd(minorUnits, other.minorUnits))
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        val result = minorUnits - other.minorUnits
        require(((minorUnits xor other.minorUnits) and (minorUnits xor result)) >= 0) { "Money overflow" }
        return copy(minorUnits = result)
    }

    fun dividedBy(divisor: Long, rounding: MoneyRounding): Money {
        require(divisor > 0)
        val quotient = minorUnits / divisor
        val remainder = minorUnits % divisor
        val magnitude = if (remainder < 0) -remainder else remainder
        val aboveHalf = magnitude > divisor / 2
        val atHalf = divisor % 2 == 0L && magnitude == divisor / 2
        val increment = when (rounding) {
            MoneyRounding.TOWARD_ZERO -> false
            MoneyRounding.HALF_UP -> aboveHalf || atHalf
            MoneyRounding.HALF_EVEN -> aboveHalf || (atHalf && quotient % 2 != 0L)
        }
        return copy(minorUnits = if (increment) checkedAdd(quotient, if (minorUnits < 0) -1 else 1) else quotient)
    }

    companion object {
        /** Ungrouped major units, dot or comma separator; never silently rounds. */
        fun parseMajor(text: String, currency: Currency): Money {
            require(text.length in 1..128 && Regex("-?[0-9]+([.,][0-9]+)?").matches(text)) { "Invalid decimal" }
            val parts = text.removePrefix("-").split('.', ',')
            val fraction = parts.getOrElse(1) { "" }
            require(fraction.length <= currency.precision) { "Excess currency precision" }
            val digits = (parts[0] + fraction.padEnd(currency.precision, '0')).trimStart('0').ifEmpty { "0" }
            val signed = if (text.startsWith('-')) "-$digits" else digits
            return Money(requireNotNull(signed.toLongOrNull()) { "Money overflow" }, currency)
        }
    }
}

internal fun checkedAdd(left: Long, right: Long): Long {
    val result = left + right
    require(((left xor result) and (right xor result)) >= 0) { "Integer overflow" }
    return result
}

/** Reject JSON numbers even when a decoder could coerce them to strings. */
object DecimalLongSerializer : KSerializer<Long> {
    override val descriptor = PrimitiveSerialDescriptor("DecimalLong", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Long {
        val text = if (decoder is JsonDecoder) {
            val value = decoder.decodeJsonElement()
            require(value is JsonPrimitive && value.isString) { "Expected decimal string" }
            value.content
        } else decoder.decodeString()
        require(Regex("0|-?[1-9][0-9]*").matches(text)) { "Expected canonical integer" }
        return requireNotNull(text.toLongOrNull()) { "Integer overflow" }
    }
}
