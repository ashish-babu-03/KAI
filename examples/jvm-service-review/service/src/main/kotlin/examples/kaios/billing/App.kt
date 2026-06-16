package examples.kaios.billing

import java.math.BigDecimal

fun main() {
    val service = InvoiceRiskService()
    val assessment = service.assess(
        Invoice(
            id = "inv-demo",
            customerTier = CustomerTier.GROWTH,
            amount = BigDecimal("12500.00"),
            daysPastDue = 12,
            hasRecentDispute = false,
        ),
    )

    println("${assessment.invoiceId}: ${assessment.level} ${assessment.signals}")
}
