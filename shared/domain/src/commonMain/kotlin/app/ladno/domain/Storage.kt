package app.ladno.domain

import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

/** Local decrypted projections only. Implementations exclude tombstones from observations. */
interface LocalEntityStore<T : Entity> {
    fun observe(includeArchived: Boolean = false): Flow<List<T>>
    suspend fun get(id: EntityId): T?
    suspend fun save(entity: T)
}

/** Archive retains ids/history; adapters must reject tombstoning referenced catalog entries. */
interface CatalogStore<T : Entity> : LocalEntityStore<T> {
    suspend fun archive(id: EntityId, at: Instant)
    suspend fun restore(id: EntityId)
    suspend fun tombstoneUnreferenced(id: EntityId, at: Instant)
}

interface BankProfileStore : CatalogStore<BankProfile>
interface AccountStore : CatalogStore<Account>
interface CardStore : CatalogStore<Card>
interface CategoryStore : CatalogStore<Category>
interface TransactionStore : LocalEntityStore<Transaction> {
    suspend fun tombstone(id: EntityId, at: Instant)
}
interface BudgetMonthStore : LocalEntityStore<BudgetMonth>
interface PlannedPaymentStore : LocalEntityStore<PlannedPayment>
interface ReviewItemStore : LocalEntityStore<ReviewItem>

/**
 * Every mutation runs in one local transaction with a durable encrypted outbox command.
 * Failure rolls both back; retries preserve command id and ciphertext. Implementations validate
 * references, currencies, refund totals and new source/category assignments before commit.
 * Refund lookup, validation and write must be serialized against competing local mutations;
 * callers cannot supply a partial refund snapshot. Incoming sync batches revalidate the full
 * aggregate and quarantine conflicts (including concurrent over-refunds across devices).
 * Existing transaction accountId survives card rebinding and catalog archival.
 * Payment matching changes allocations and the confirmed expense in this same boundary.
 * Plaintext projections are never persisted by these ports (see docs/ENCRYPTION.md).
 */
interface LocalLedger {
    val banks: BankProfileStore
    val accounts: AccountStore
    val cards: CardStore
    val categories: CategoryStore
    val transactions: TransactionStore
    val budgets: BudgetMonthStore
    val plannedPayments: PlannedPaymentStore
    val reviewItems: ReviewItemStore
    suspend fun <T> mutate(commandId: EntityId, change: suspend LocalLedger.() -> T): T
}

/** Offline blob resolution; adapters validate/decrypt images and enqueue encrypted uploads. */
interface IconAssetStore {
    suspend fun read(ref: IconAssetRef): ByteArray?
    suspend fun save(ref: IconAssetRef, sanitizedImage: ByteArray)
}
