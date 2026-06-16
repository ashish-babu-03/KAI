package examples.kaios.billing

import java.math.BigDecimal

enum class CustomerTier {
    STARTUP,
    GROWTH,
    ENTERPRISE,
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
}

data class Invoice(
    val id: String,
    val customerTier: CustomerTier,
    val amount: BigDecimal,
    val daysPastDue: Int,
    val hasRecentDispute: Boolean,
)
