package app.ladno.domain

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionKind { EXPENSE, INCOME, REFUND, TRANSFER }
@Serializable
enum class ReviewStatus { PENDING, CONFIRMED, IGNORED }
@Serializable
enum class TransactionSource { MANUAL, NOTIFICATION, CSV }

@Serializable
data class TransactionSplit(val categoryId: EntityId, val amount: Money) {
    init { require(amount.minorUnits > 0) }
}

@Serializable
data class RefundLink(val originalExpenseId: EntityId?, val allocated: Money) {
    init { require(allocated.minorUnits > 0) }
}

/** One transfer aggregate; both notification legs become evidence, never two expenses. */
@Serializable
data class TransferLink(val counterpartyAccountId: EntityId, val evidenceLocalIds: Set<String> = emptySet())

@Serializable
data class Transaction(
    override val metadata: EntityMetadata,
    val kind: TransactionKind,
    val amount: Money,
    val accountId: EntityId,
    val occurredAt: Instant,
    val budgetDate: LocalDate,
    val zoneAtAssignment: String,
    val source: TransactionSource,
    val reviewStatus: ReviewStatus,
    val cardId: EntityId? = null,
    val capturedAt: Instant? = null,
    val merchant: String? = null,
    val note: String? = null,
    val splits: List<TransactionSplit> = emptyList(),
    val tags: Set<String> = emptySet(),
    val excludedFromBudget: Boolean = false,
    val refund: RefundLink? = null,
    val transfer: TransferLink? = null,
) : Entity {
    init {
        require(amount.minorUnits > 0)
        validateZone(zoneAtAssignment)
        require((kind == TransactionKind.TRANSFER) == (transfer != null))
        require((kind == TransactionKind.REFUND) == (refund != null))
        require(transfer?.counterpartyAccountId != accountId)
        require(refund?.originalExpenseId != metadata.id)
        if (refund != null) require(refund.allocated == amount)
        if (kind == TransactionKind.EXPENSE || kind == TransactionKind.REFUND) {
            require(splits.isNotEmpty())
            require(splits.fold(Money(0, amount.currency)) { sum, it -> sum + it.amount } == amount) { "Split sum mismatch" }
        } else require(splits.isEmpty())
    }

    /** Signed net spending; refunds keep their own dates, including previous-month purchases. */
    val budgetImpact: Money get() = Money(
        when {
            reviewStatus != ReviewStatus.CONFIRMED || excludedFromBudget || metadata.deletedAt != null -> 0
            kind == TransactionKind.EXPENSE -> amount.minorUnits
            kind == TransactionKind.REFUND -> -amount.minorUnits
            else -> 0
        }, amount.currency,
    )

    /** Validate current bindings only for a new operation or an explicit source change. */
    fun validateNewSources(account: Account, card: Card? = null, counterparty: Account? = null) {
        require(account.metadata.id == accountId && account.currency == amount.currency)
        require(account.archivedAt == null && account.metadata.deletedAt == null)
        require(cardId == card?.metadata?.id)
        card?.validateAccount(account)
        if (transfer != null) {
            require(counterparty != null && counterparty.metadata.id == transfer.counterpartyAccountId)
            require(counterparty.currency == amount.currency)
            require(counterparty.archivedAt == null && counterparty.metadata.deletedAt == null)
        } else require(counterparty == null)
    }

    fun validateCategories(categories: List<Category>, forNewAssignment: Boolean) {
        for (split in splits) {
            val category = requireNotNull(categories.singleOrNull { it.metadata.id == split.categoryId })
            require(category.metadata.deletedAt == null)
            if (forNewAssignment) require(category.archivedAt == null)
        }
    }

    /** priorRefunds must include all live refunds linked to this expense, excluding this id. */
    fun validateRefund(original: Transaction, priorRefunds: List<Transaction>) {
        require(kind == TransactionKind.REFUND && refund?.originalExpenseId == original.metadata.id)
        require(original.kind == TransactionKind.EXPENSE && original.metadata.deletedAt == null)
        require(original.amount.currency == amount.currency)
        val live = priorRefunds.filter { it.metadata.deletedAt == null && it.reviewStatus != ReviewStatus.IGNORED }
        require(live.map { it.metadata.id }.distinct().size == live.size)
        var total = amount
        for (previous in live) {
            require(previous.metadata.id != metadata.id && previous.kind == TransactionKind.REFUND)
            require(previous.refund?.originalExpenseId == original.metadata.id)
            total += previous.amount
        }
        require(total.minorUnits <= original.amount.minorUnits) { "Refund exceeds expense" }
        // A linked refund cannot silently move spending to another category.
        for (categoryId in splits.map { it.categoryId }.distinct()) {
            val originalAmount = original.splits.filter { it.categoryId == categoryId }
                .fold(Money(0, amount.currency)) { sum, it -> sum + it.amount }
            val returned = (live + this).flatMap { it.splits }.filter { it.categoryId == categoryId }
                .fold(Money(0, amount.currency)) { sum, it -> sum + it.amount }
            require(returned.minorUnits <= originalAmount.minorUnits)
        }
    }
}
