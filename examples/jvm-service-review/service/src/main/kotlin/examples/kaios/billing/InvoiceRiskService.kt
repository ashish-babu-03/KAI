package examples.kaios.billing

import java.math.BigDecimal

class InvoiceRiskService(
    private val enterpriseGraceDays: Int = 45,
    private val standardGraceDays: Int = 30,
) {
    fun assess(invoice: Invoice): RiskAssessment {
        val ageLimit = if (invoice.customerTier == CustomerTier.ENTERPRISE) enterpriseGraceDays else standardGraceDays
        val signals = mutableListOf<String>()
        if (invoice.daysPastDue >= ageLimit) signals += "past_due"
        if (invoice.hasRecentDispute) signals += "recent_dispute"
        if (invoice.amount >= BigDecimal("10000.00") && invoice.customerTier != CustomerTier.ENTERPRISE) {
            signals += "large_balance"
        }

        val level = when {
            signals.size >= 2 -> RiskLevel.HIGH
            signals.isNotEmpty() -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        return RiskAssessment(
            invoiceId = invoice.id,
            level = level,
            signals = signals.toList(),
        )
    }
}

data class RiskAssessment(
    val invoiceId: String,
    val level: RiskLevel,
    val signals: List<String>,
)
