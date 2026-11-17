package app.ladno.domain

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

@Serializable
data class EntityId(val value: String) {
    init { require(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matches(value)) }
}

@Serializable
enum class SyncState { CLEAN, PENDING, CONFLICT }

@Serializable
data class EntityMetadata(
    val id: EntityId,
    @Serializable(with = DecimalLongSerializer::class) val serverRevision: Long = 0,
    val deletedAt: Instant? = null,
    val syncState: SyncState = SyncState.PENDING,
    @Serializable(with = DecimalLongSerializer::class) val lastAcknowledgedRevision: Long = 0,
) {
    init { require(serverRevision >= 0 && lastAcknowledgedRevision in 0..serverRevision) }
}

interface Entity { val metadata: EntityMetadata }

@Serializable
data class IconAssetRef(val assetId: EntityId)

@Serializable
data class BankProfile(
    override val metadata: EntityMetadata,
    val displayName: String,
    val builtInIconKey: String? = null,
    val customIcon: IconAssetRef? = null,
    val archivedAt: Instant? = null,
) : Entity { init { require(displayName.isNotBlank()) } }

@Serializable
enum class AccountKind { DEBIT, CREDIT, CASH }

@Serializable
data class Account(
    override val metadata: EntityMetadata,
    val name: String,
    val currency: Currency,
    val kind: AccountKind,
    val bankProfileId: EntityId? = null,
    val iconOverride: IconAssetRef? = null,
    val archivedAt: Instant? = null,
) : Entity { init { require(name.isNotBlank()) } }

@Serializable
data class Card(
    override val metadata: EntityMetadata,
    val accountId: EntityId,
    val name: String,
    val last4: String? = null,
    val paymentSystem: String? = null,
    val iconOverride: IconAssetRef? = null,
    val archivedAt: Instant? = null,
) : Entity {
    init { require(name.isNotBlank()); require(last4 == null || Regex("[0-9]{4}").matches(last4)) }
    fun validateAccount(account: Account) {
        require(account.metadata.id == accountId && account.metadata.deletedAt == null)
        require(account.archivedAt == null && archivedAt == null) { "Archived source" }
    }
}

@Serializable
enum class SpendingType { FIXED, VARIABLE }

@Serializable
data class Category(
    override val metadata: EntityMetadata,
    val name: String,
    val iconKey: String,
    val colorToken: String,
    val fixedOrVariableDefault: SpendingType = SpendingType.VARIABLE,
    val parentId: EntityId? = null,
    val archivedAt: Instant? = null,
) : Entity {
    init {
        require(name.isNotBlank() && iconKey.isNotBlank() && colorToken.isNotBlank())
        require(parentId != metadata.id)
    }
}

@Serializable
enum class ReviewReason { UNKNOWN_FORMAT, AMBIGUOUS_AMOUNT, POSSIBLE_DUPLICATE, INCOMPLETE_TRANSFER, HIDDEN_TEXT, UNKNOWN_SOURCE }
@Serializable
enum class ReviewResolution { OPEN, CONFIRMED, IGNORED, LINKED }

@Serializable
data class ReviewItem(
    override val metadata: EntityMetadata,
    val reason: ReviewReason,
    val candidateFields: Map<String, String> = emptyMap(),
    val evidenceLocalId: String? = null,
    val resolution: ReviewResolution = ReviewResolution.OPEN,
    val linkedTransactionId: EntityId? = null,
) : Entity {
    init { require(resolution !in setOf(ReviewResolution.CONFIRMED, ReviewResolution.LINKED) || linkedTransactionId != null) }
}

internal fun validateZone(zoneId: String) {
    initializeTimeZones()
    TimeZone.of(zoneId)
}

internal expect fun initializeTimeZones()

@Serializable
enum class RolloverPolicy { NONE, EXPLICIT }

@Serializable
data class BudgetCategory(val categoryId: EntityId, val allocated: Money, val protected: Boolean = false) {
    init { require(allocated.minorUnits >= 0) }
}

@Serializable
data class BudgetMonth(
    override val metadata: EntityMetadata,
    val year: Int,
    val month: Int,
    val zoneId: String,
    val expenseLimit: Money,
    val reserve: Money,
    val categories: List<BudgetCategory> = emptyList(),
    val rolloverPolicy: RolloverPolicy = RolloverPolicy.NONE,
    @Serializable(with = DecimalLongSerializer::class) val version: Long = 1,
) : Entity {
    init {
        LocalDate(year, month, 1)
        validateZone(zoneId)
        require(version > 0 && expenseLimit.minorUnits >= 0 && reserve.minorUnits >= 0)
        require(categories.map { it.categoryId }.distinct().size == categories.size)
        val allocated = categories.fold(reserve) { sum, category -> sum + category.allocated }
        require(allocated.currency == expenseLimit.currency && allocated.minorUnits <= expenseLimit.minorUnits)
    }
    val unallocated: Money get() = expenseLimit - categories.fold(reserve) { sum, category -> sum + category.allocated }
}

@Serializable
enum class PlannedPaymentState { UPCOMING, PAID, SKIPPED }

@Serializable
data class PaymentAllocation(val transactionId: EntityId, val amount: Money) {
    init { require(amount.minorUnits > 0) }
}

@Serializable
data class PlannedPayment(
    override val metadata: EntityMetadata,
    val dueDate: LocalDate,
    val expected: Money,
    val categoryId: EntityId,
    val accountId: EntityId? = null,
    val allocations: List<PaymentAllocation> = emptyList(),
    val state: PlannedPaymentState = PlannedPaymentState.UPCOMING,
) : Entity {
    init {
        require(expected.minorUnits > 0)
        require(allocations.map { it.transactionId }.distinct().size == allocations.size)
        val covered = allocations.fold(Money(0, expected.currency)) { sum, it -> sum + it.amount }
        require((state == PlannedPaymentState.PAID) == (covered.minorUnits >= expected.minorUnits))
        require(state != PlannedPaymentState.SKIPPED || allocations.isEmpty())
    }
    val remaining: Money get() {
        if (state == PlannedPaymentState.SKIPPED) return Money(0, expected.currency)
        val covered = allocations.fold(Money(0, expected.currency)) { sum, it -> sum + it.amount }
        return Money((expected.minorUnits - covered.minorUnits).coerceAtLeast(0), expected.currency)
    }
}
