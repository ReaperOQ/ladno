package app.ladno.domain

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class EntitiesTest {
    private fun id(n: Int) = EntityId("00000000-0000-0000-0000-${n.toString().padStart(12, '0')}")
    private fun meta(n: Int) = EntityMetadata(id(n))
    private fun rub(n: Long) = Money(n, Currency.RUB)
    private val now = Instant.parse("2026-11-18T10:00:00Z")
    private val account = Account(meta(1), "Счёт", Currency.RUB, AccountKind.DEBIT)
    private val category = Category(meta(2), "Еда", "food", "primary")
    private val card = Card(meta(3), id(1), "Карта", "1234")
    private fun expense(n: Int = 4) = Transaction(
        meta(n), TransactionKind.EXPENSE, rub(100), id(1), now, LocalDate(2026, 11, 18),
        "Europe/Moscow", TransactionSource.MANUAL, ReviewStatus.CONFIRMED,
        cardId = id(3), splits = listOf(TransactionSplit(id(2), rub(100))),
    )
    private fun refund(n: Int, amount: Long) = expense(n).copy(
        kind = TransactionKind.REFUND, amount = rub(amount),
        refund = RefundLink(id(4), rub(amount)), splits = listOf(TransactionSplit(id(2), rub(amount))),
    )

    @Test fun splitSumCurrencyPositivityAndOverflow() {
        val split = expense().copy(splits = listOf(TransactionSplit(id(2), rub(40)), TransactionSplit(id(5), rub(60))))
        assertEquals(rub(100), split.budgetImpact)
        assertFailsWith<IllegalArgumentException> { split.copy(splits = emptyList()) }
        assertFailsWith<IllegalArgumentException> { split.copy(amount = rub(101)) }
        assertFailsWith<IllegalArgumentException> { TransactionSplit(id(2), rub(0)) }
        assertFailsWith<IllegalArgumentException> { split.copy(splits = listOf(TransactionSplit(id(2), Money(100, Currency.USD)))) }
        assertFailsWith<IllegalArgumentException> { split.copy(splits = listOf(TransactionSplit(id(2), rub(Long.MAX_VALUE)), TransactionSplit(id(5), rub(1)))) }
    }

    @Test fun cardsHaveOneAccountAndLastFourAreNotIdentity() {
        expense().validateNewSources(account, card)
        val otherAccount = account.copy(metadata = meta(5))
        val otherCard = card.copy(metadata = meta(6), accountId = id(5))
        otherCard.validateAccount(otherAccount)
        card.copy(metadata = meta(7)).validateAccount(account)
        assertEquals(card.last4, otherCard.last4)
        assertFailsWith<IllegalArgumentException> { expense().validateNewSources(account, otherCard) }
        val rebound = card.copy(accountId = id(5))
        assertFailsWith<IllegalArgumentException> { expense().validateNewSources(account, rebound) }
        val next = expense(8).copy(accountId = id(5))
        next.validateNewSources(otherAccount, rebound)
        assertEquals(id(1), expense().accountId)
        assertFailsWith<IllegalArgumentException> { next.validateNewSources(otherAccount.copy(currency = Currency.USD), rebound) }
        assertFailsWith<IllegalArgumentException> { card.copy(last4 = "12345") }
    }

    @Test fun archivedCatalogRetainsHistoryButRejectsNewAssignments() {
        val archivedBank = BankProfile(meta(9), "Банк").copy(archivedAt = now)
        val archivedAccount = account.copy(archivedAt = now)
        val archivedCard = card.copy(archivedAt = now)
        val archivedCategory = category.copy(archivedAt = now)
        assertNotNull(archivedBank.archivedAt)
        assertEquals(account.metadata.id, archivedAccount.metadata.id)
        assertEquals(card.accountId, archivedCard.accountId)
        expense().validateCategories(listOf(archivedCategory), forNewAssignment = false)
        assertEquals(rub(100), expense().budgetImpact)
        assertFailsWith<IllegalArgumentException> { expense().validateCategories(listOf(archivedCategory), forNewAssignment = true) }
        assertFailsWith<IllegalArgumentException> { expense().validateNewSources(archivedAccount, card) }
        assertFailsWith<IllegalArgumentException> { expense().validateNewSources(account, archivedCard) }
        expense().validateNewSources(archivedAccount.copy(archivedAt = null), archivedCard.copy(archivedAt = null))
    }

    @Test fun typeAndReviewStatusAreIndependent() {
        assertEquals(rub(100), expense().budgetImpact)
        for (status in listOf(ReviewStatus.PENDING, ReviewStatus.IGNORED)) {
            assertEquals(rub(0), expense().copy(reviewStatus = status).budgetImpact)
        }
        assertEquals(rub(0), expense().copy(excludedFromBudget = true).budgetImpact)
        assertEquals(rub(0), expense().copy(metadata = meta(4).copy(deletedAt = now)).budgetImpact)
        assertEquals(rub(0), expense().copy(kind = TransactionKind.INCOME, splits = emptyList()).budgetImpact)
    }

    @Test fun transferIsOneSameCurrencyAggregateAndFeeIsExpense() {
        val transfer = expense().copy(kind = TransactionKind.TRANSFER, splits = emptyList(), transfer = TransferLink(id(5), setOf("out", "in")))
        transfer.validateNewSources(account, card, account.copy(metadata = meta(5)))
        assertEquals(rub(0), transfer.budgetImpact)
        assertEquals(rub(100), expense(10).budgetImpact)
        assertFailsWith<IllegalArgumentException> { transfer.copy(transfer = TransferLink(id(1))) }
        assertFailsWith<IllegalArgumentException> { transfer.validateNewSources(account, card, account.copy(metadata = meta(5), currency = Currency.USD)) }
        assertFailsWith<IllegalArgumentException> { transfer.copy(transfer = null) }
        assertFailsWith<IllegalArgumentException> { expense().copy(transfer = TransferLink(id(5))) }
    }

    @Test fun partialRefundsUseOriginalCategoriesAndCannotExceedExpense() {
        val first = refund(5, 40)
        val second = refund(6, 60).copy(budgetDate = LocalDate(2026, 12, 1))
        first.validateRefund(expense(), emptyList())
        second.validateRefund(expense(), listOf(first))
        assertEquals(rub(-60), second.budgetImpact)
        assertEquals(LocalDate(2026, 12, 1), second.budgetDate)
        assertFailsWith<IllegalArgumentException> { refund(7, 61).validateRefund(expense(), listOf(first)) }
        assertFailsWith<IllegalArgumentException> { first.copy(splits = listOf(TransactionSplit(id(8), rub(40)))).validateRefund(expense(), emptyList()) }
        assertFailsWith<IllegalArgumentException> { first.validateRefund(expense().copy(amount = Money(100, Currency.USD), splits = listOf(TransactionSplit(id(2), Money(100, Currency.USD)))), emptyList()) }
        val unlinked = first.copy(refund = RefundLink(null, rub(40)))
        assertEquals(rub(-40), unlinked.budgetImpact)
        assertFailsWith<IllegalArgumentException> { unlinked.copy(splits = emptyList()) }
    }

    @Test fun budgetAllocationAndPaymentPartialCoverage() {
        val budget = BudgetMonth(meta(20), 2026, 11, "Europe/Moscow", rub(1000), rub(100), listOf(BudgetCategory(id(2), rub(600))))
        assertEquals(rub(300), budget.unallocated)
        assertFailsWith<IllegalArgumentException> { budget.copy(reserve = rub(401)) }
        assertFailsWith<IllegalArgumentException> { budget.copy(reserve = Money(100, Currency.USD)) }
        assertFailsWith<IllegalArgumentException> { budget.copy(month = 13) }
        val payment = PlannedPayment(meta(21), LocalDate(2026, 11, 20), rub(100), id(2))
        assertEquals(rub(100), payment.remaining)
        assertEquals(rub(60), payment.copy(allocations = listOf(PaymentAllocation(id(4), rub(40)))).remaining)
        assertEquals(rub(0), payment.copy(allocations = listOf(PaymentAllocation(id(4), rub(120))), state = PlannedPaymentState.PAID).remaining)
        assertEquals(rub(0), payment.copy(state = PlannedPaymentState.SKIPPED).remaining)
        assertFailsWith<IllegalArgumentException> { payment.copy(state = PlannedPaymentState.PAID) }
    }

    @Test fun encryptedPayloadJsonRoundTripValidatesEntitiesAndRevisions() {
        val value = expense().copy(metadata = meta(4).copy(serverRevision = 9007199254740993, lastAcknowledgedRevision = 1))
        val json = Json.encodeToString(value)
        assertEquals(value, Json.decodeFromString<Transaction>(json))
        assertFailsWith<IllegalArgumentException> { Json.decodeFromString<Transaction>(json.replace("\"100\"", "\"0\"")) }
        assertFailsWith<IllegalArgumentException> { Json.decodeFromString<Transaction>(json.replace("\"9007199254740993\"", "9007199254740993")) }
        assertFailsWith<IllegalArgumentException> { meta(1).copy(serverRevision = -1) }
    }
}
