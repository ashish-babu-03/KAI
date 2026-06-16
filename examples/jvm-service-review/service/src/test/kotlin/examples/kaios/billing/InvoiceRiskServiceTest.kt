package examples.kaios.billing

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvoiceRiskServiceTest {
    @Test
    fun flagsPastDueGrowthInvoice() {
        val service = InvoiceRiskService()

        val assessment = service.assess(
            invoice(daysPastDue = 31),
        )

        assertEquals(RiskLevel.MEDIUM, assessment.level)
        assertTrue("past_due" in assessment.signals)
    }

    @Test
    fun enterpriseCustomersGetLongerGraceWindow() {
        val service = InvoiceRiskService()

        val assessment = service.assess(
            invoice(customerTier = CustomerTier.ENTERPRISE, daysPastDue = 31),
        )

        assertEquals(RiskLevel.LOW, assessment.level)
    }

    @Test
    fun disputeAndLargeBalanceBecomeHighRisk() {
        val service = InvoiceRiskService()

        val assessment = service.assess(
            invoice(amount = BigDecimal("12000.00"), hasRecentDispute = true),
        )

        assertEquals(RiskLevel.HIGH, assessment.level)
    }

    private fun invoice(
        customerTier: CustomerTier = CustomerTier.GROWTH,
        amount: BigDecimal = BigDecimal("250.00"),
        daysPastDue: Int = 0,
        hasRecentDispute: Boolean = false,
    ): Invoice =
        Invoice(
            id = "inv-1001",
            customerTier = customerTier,
            amount = amount,
            daysPastDue = daysPastDue,
            hasRecentDispute = hasRecentDispute,
        )
}
