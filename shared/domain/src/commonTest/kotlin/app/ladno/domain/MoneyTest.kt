package app.ladno.domain

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {
    @Test fun decimalPrecisionAndBounds() {
        assertEquals(Money(123456, Currency.RUB), Money.parseMajor("1234,56", Currency.RUB))
        assertEquals(1, Money.parseMajor("0.001", Currency.KWD).minorUnits)
        assertEquals(123, Money.parseMajor("123", Currency.JPY).minorUnits)
        assertEquals(Long.MAX_VALUE, Money.parseMajor("92233720368547758.07", Currency.RUB).minorUnits)
        assertEquals(Long.MIN_VALUE, Money.parseMajor("-92233720368547758.08", Currency.RUB).minorUnits)
        for (text in listOf("1.001", "92233720368547758.08", "-92233720368547758.09", "1e2", "NaN", "", "1,2.3")) {
            assertFailsWith<IllegalArgumentException>(text) { Money.parseMajor(text, Currency.RUB) }
        }
        assertFailsWith<IllegalArgumentException> { Money.parseMajor("1.0", Currency.JPY) }
    }

    @Test fun arithmeticChecksEveryBoundaryAndCurrency() {
        val max = Money(Long.MAX_VALUE, Currency.RUB)
        val min = Money(Long.MIN_VALUE, Currency.RUB)
        assertFailsWith<IllegalArgumentException> { max + Money(1, Currency.RUB) }
        assertFailsWith<IllegalArgumentException> { min + Money(-1, Currency.RUB) }
        assertFailsWith<IllegalArgumentException> { min - Money(1, Currency.RUB) }
        assertFailsWith<IllegalArgumentException> { max - Money(-1, Currency.RUB) }
        assertEquals(0, (min - min).minorUnits)
        assertFailsWith<IllegalArgumentException> { max + Money(1, Currency.USD) }
        assertFailsWith<IllegalArgumentException> { max - Money(1, Currency.USD) }
    }

    @Test fun explicitRoundingIsExactAndSymmetric() {
        for (sign in listOf(-1L, 1L)) {
            assertEquals(sign * 2, Money(sign * 5, Currency.RUB).dividedBy(2, MoneyRounding.HALF_EVEN).minorUnits)
            assertEquals(sign * 4, Money(sign * 7, Currency.RUB).dividedBy(2, MoneyRounding.HALF_EVEN).minorUnits)
            assertEquals(sign * 3, Money(sign * 5, Currency.RUB).dividedBy(2, MoneyRounding.HALF_UP).minorUnits)
            assertEquals(sign * 2, Money(sign * 5, Currency.RUB).dividedBy(2, MoneyRounding.TOWARD_ZERO).minorUnits)
        }
        assertEquals(Long.MIN_VALUE, Money(Long.MIN_VALUE, Currency.RUB).dividedBy(1, MoneyRounding.HALF_EVEN).minorUnits)
        assertEquals(1, Money(Long.MAX_VALUE, Currency.RUB).dividedBy(Long.MAX_VALUE, MoneyRounding.HALF_UP).minorUnits)
        assertFailsWith<IllegalArgumentException> { Money(1, Currency.RUB).dividedBy(0, MoneyRounding.HALF_UP) }
        for (minor in -1000L..1000L) {
            val money = Money(minor, Currency.RUB)
            assertEquals(money, (money + Money(37, Currency.RUB)) - Money(37, Currency.RUB))
        }
    }

    @Test fun jsonUsesStrictDecimalStringsBeyondJavascriptSafeInteger() {
        for (minor in listOf(0L, -1L, 9007199254740993L, Long.MIN_VALUE, Long.MAX_VALUE)) {
            val money = Money(minor, Currency.RUB)
            val json = Json.encodeToString(money)
            assertEquals("{\"minorUnits\":\"$minor\",\"currency\":\"RUB\"}", json)
            assertEquals(money, Json.decodeFromString<Money>(json))
        }
        for (value in listOf("1", "1.0", "null", "\"01\"", "\"-0\"", "\"1e2\"", "\"9223372036854775808\"", "\"1.00\"")) {
            assertFailsWith<IllegalArgumentException> { Json.decodeFromString<Money>("{\"minorUnits\":$value,\"currency\":\"RUB\"}") }
        }
    }
}
