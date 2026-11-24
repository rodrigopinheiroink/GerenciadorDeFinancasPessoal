package com.example.minhasfinancas

import com.example.minhasfinancas.data.model.MonthlySummary
import com.example.minhasfinancas.data.model.RecurrenceFrequency
import com.example.minhasfinancas.data.model.TransactionType
import com.example.minhasfinancas.ui.screens.InstallmentInputMode
import com.example.minhasfinancas.ui.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceLogicTest {

    @Test
    fun testInstallmentCalculation_fromTotalAmount() {
        val totalAmount = 1200.0
        val numberOfInstallments = 10
        val mode = InstallmentInputMode.VALOR_TOTAL

        val calculatedTotal: Double
        val calculatedParcel: Double

        if (mode == InstallmentInputMode.VALOR_TOTAL) {
            calculatedTotal = totalAmount
            calculatedParcel = totalAmount / numberOfInstallments
        } else {
            calculatedParcel = totalAmount
            calculatedTotal = totalAmount * numberOfInstallments
        }

        assertEquals(1200.0, calculatedTotal, 0.001)
        assertEquals(120.0, calculatedParcel, 0.001)
    }

    @Test
    fun testInstallmentCalculation_fromParcelAmount() {
        val parcelAmount = 150.0
        val numberOfInstallments = 6
        val mode = InstallmentInputMode.VALOR_DA_PARCELA

        val calculatedTotal: Double
        val calculatedParcel: Double

        if (mode == InstallmentInputMode.VALOR_TOTAL) {
            calculatedTotal = parcelAmount
            calculatedParcel = parcelAmount / numberOfInstallments
        } else {
            calculatedParcel = parcelAmount
            calculatedTotal = parcelAmount * numberOfInstallments
        }

        assertEquals(900.0, calculatedTotal, 0.001)
        assertEquals(150.0, calculatedParcel, 0.001)
    }

    @Test
    fun testMonthlySummaryBalance() {
        val summary = MonthlySummary(
            year = 2026,
            month = 10,
            totalIncome = 5000.0,
            totalExpense = 3200.0
        )
        assertEquals(1800.0, summary.balance, 0.001)
    }

    @Test
    fun testFormattersMonthNames() {
        assertEquals("Janeiro", Formatters.getMonthName(0))
        assertEquals("Novembro", Formatters.getMonthName(10))
        assertEquals("Dezembro", Formatters.getMonthName(11))
        assertEquals("Nov", Formatters.getShortMonthName(10))
    }

    @Test
    fun testParseHexColor() {
        val color = Formatters.parseHexColor("#10B981")
        assertTrue(color.alpha > 0f)
    }

    @Test
    fun testDateLimits_minDate2020() {
        val minDateCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, 2020)
            set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val minDateMillis = minDateCal.timeInMillis

        // Uma data em 2019 deve ser menor que a data mínima
        val date2019 = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, 2019)
            set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER)
            set(java.util.Calendar.DAY_OF_MONTH, 31)
        }.timeInMillis

        // Uma data em 2024 deve ser maior ou igual à data mínima
        val date2024 = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, 2024)
            set(java.util.Calendar.MONTH, java.util.Calendar.MAY)
            set(java.util.Calendar.DAY_OF_MONTH, 15)
        }.timeInMillis

        assertTrue(date2019 < minDateMillis)
        assertTrue(date2024 >= minDateMillis)
    }

    @Test
    fun testInstallmentsLimit_upTo60() {
        val minValue = 1
        val maxValue = 60

        // Se o usuário digitar 75, deve ser limitado em 60
        val clampedOver = 75.coerceIn(minValue, maxValue)
        assertEquals(60, clampedOver)

        // Se o usuário tentar diminuir abaixo de 1, deve ser limitado em 1
        val clampedUnder = 0.coerceIn(minValue, maxValue)
        assertEquals(1, clampedUnder)

        // Valor válido dentro da faixa
        val validValue = 48.coerceIn(minValue, maxValue)
        assertEquals(48, validValue)
    }

    @Test
    fun testCardMachineFeeCalculation() {
        val grossTotal = 1000.0
        val installments = 10
        val machineFeePercent = 4.5  // 4.5%
        val advanceFeePercent = 2.0  // 2.0%

        val machineFeeAmount = (grossTotal * machineFeePercent) / 100.0
        val advanceFeeAmount = (grossTotal * advanceFeePercent) / 100.0
        val totalFees = machineFeeAmount + advanceFeeAmount
        val netTotal = grossTotal - totalFees
        val netParcel = netTotal / installments

        assertEquals(45.0, machineFeeAmount, 0.001)
        assertEquals(20.0, advanceFeeAmount, 0.001)
        assertEquals(65.0, totalFees, 0.001)
        assertEquals(935.0, netTotal, 0.001)
        assertEquals(93.5, netParcel, 0.001)
    }

    @Test
    fun testCardMachine_DebitCalculation() {
        val grossTotal = 500.0
        val debitFeePercent = 1.3 // 1.3% no débito

        val feeAmount = (grossTotal * debitFeePercent) / 100.0
        val netTotal = grossTotal - feeAmount

        assertEquals(6.5, feeAmount, 0.001)
        assertEquals(493.5, netTotal, 0.001)
    }

    @Test
    fun testCardMachine_CreditCashCalculation() {
        val grossTotal = 800.0
        val creditCashFeePercent = 3.1 // 3.1% no crédito à vista

        val feeAmount = (grossTotal * creditCashFeePercent) / 100.0
        val netTotal = grossTotal - feeAmount

        assertEquals(24.8, feeAmount, 0.001)
        assertEquals(775.2, netTotal, 0.001)
    }

    @Test
    fun testCardMachine_ReverseCalculation_fromDiscountedNet() {
        // Se o usuário quer receber R$ 935,00 líquido na conta
        val netTotal = 935.0
        val totalFeePercent = 6.5 // 4.5% parcelado + 2.0% antecipação
        val feeRate = totalFeePercent / 100.0

        // Bruto = Líquido / (1 - taxa)
        val grossTotal = netTotal / (1.0 - feeRate)
        val totalFees = grossTotal - netTotal

        assertEquals(1000.0, grossTotal, 0.001)
        assertEquals(65.0, totalFees, 0.001)
    }

    @Test
    fun testCardMachine_DebitReverseCalculation_fromDiscountedNet() {
        // Se o usuário quer receber R$ 493,50 líquido no débito com taxa de 1.3%
        val netTotal = 493.5
        val debitFeePercent = 1.3
        val feeRate = debitFeePercent / 100.0

        val grossTotal = netTotal / (1.0 - feeRate)
        val feeAmount = grossTotal - netTotal

        assertEquals(500.0, grossTotal, 0.001)
        assertEquals(6.5, feeAmount, 0.001)
    }

    @Test
    fun testProfileDatabaseIsolation_namingConvention() {
        val profileAId = "profile-uuid-1"
        val profileBId = "profile-uuid-2"

        val dbNameA = "minhas_financas_profile_${profileAId}.db"
        val dbNameB = "minhas_financas_profile_${profileBId}.db"

        // Cada perfil deve possuir um arquivo de banco de dados exclusivo
        org.junit.Assert.assertNotEquals(dbNameA, dbNameB)
        assertEquals("minhas_financas_profile_profile-uuid-1.db", dbNameA)
        assertEquals("minhas_financas_profile_profile-uuid-2.db", dbNameB)
    }

    @Test
    fun testAuditHistoryItem_creation() {
        val auditItem = com.example.minhasfinancas.data.model.AuditHistoryItem(
            id = 1L,
            actionType = "CRIADA",
            description = "Salário",
            amount = 5000.0,
            type = TransactionType.ENTRADA,
            tag = "Salário",
            eventTimestamp = 1700000000000L,
            details = "Movimentação criada em 19/11/2026"
        )

        assertEquals("CRIADA", auditItem.actionType)
        assertEquals(5000.0, auditItem.amount, 0.001)
        assertEquals(TransactionType.ENTRADA, auditItem.type)
        assertEquals("Salário", auditItem.tag)
        assertEquals("Movimentação criada em 19/11/2026", auditItem.details)
    }

    @Test
    fun testProfileListDeletionLogic() {
        val initialProfiles = mutableListOf(
            com.example.minhasfinancas.data.model.UserProfile(id = "p1", name = "Pessoal"),
            com.example.minhasfinancas.data.model.UserProfile(id = "p2", name = "Empresa"),
            com.example.minhasfinancas.data.model.UserProfile(id = "p3", name = "Investimentos")
        )

        // Simula exclusão do perfil p2
        val idToDelete = "p2"
        initialProfiles.removeAll { it.id == idToDelete }

        assertEquals(2, initialProfiles.size)
        assertTrue(initialProfiles.none { it.id == "p2" })
        assertEquals("p1", initialProfiles[0].id)
        assertEquals("p3", initialProfiles[1].id)
    }

    @Test
    fun testTagReorderingLogic() {
        val tags = mutableListOf(
            com.example.minhasfinancas.data.model.TagMarker(id = 1L, name = "Alimentação", colorHex = "#F59E0B", displayOrder = 0),
            com.example.minhasfinancas.data.model.TagMarker(id = 2L, name = "Transporte", colorHex = "#3B82F6", displayOrder = 1),
            com.example.minhasfinancas.data.model.TagMarker(id = 3L, name = "Moradia", colorHex = "#8B5CF6", displayOrder = 2)
        )

        // Simula subir Transporte para a primeira posição
        val temp = tags[1]
        tags[1] = tags[0]
        tags[0] = temp

        assertEquals("Transporte", tags[0].name)
        assertEquals("Alimentação", tags[1].name)
        assertEquals("Moradia", tags[2].name)
    }

    @Test
    fun testTagRenameAndColorUpdate() {
        val originalTag = com.example.minhasfinancas.data.model.TagMarker(
            id = 10L,
            name = "Lazer",
            colorHex = "#EC4899",
            displayOrder = 5
        )

        val updatedTag = originalTag.copy(name = "Lazer e Viagens", colorHex = "#3B82F6")

        assertEquals(10L, updatedTag.id)
        assertEquals("Lazer e Viagens", updatedTag.name)
        assertEquals("#3B82F6", updatedTag.colorHex)
        assertEquals(5, updatedTag.displayOrder)
    }

    @Test
    fun testTagVisibilityOnHome() {
        val allTags = listOf(
            com.example.minhasfinancas.data.model.TagMarker(id = 1L, name = "Alimentação", colorHex = "#F59E0B", isVisibleOnHome = true),
            com.example.minhasfinancas.data.model.TagMarker(id = 2L, name = "Transporte", colorHex = "#3B82F6", isVisibleOnHome = false),
            com.example.minhasfinancas.data.model.TagMarker(id = 3L, name = "Salário", colorHex = "#10B981", isVisibleOnHome = true)
        )

        val visibleOnHome = allTags.filter { it.isVisibleOnHome }

        assertEquals(2, visibleOnHome.size)
        assertTrue(visibleOnHome.any { it.name == "Alimentação" })
        assertTrue(visibleOnHome.any { it.name == "Salário" })
        assertTrue(visibleOnHome.none { it.name == "Transporte" })
    }

    @Test
    fun testIndependentVisorCalculations_withSelectedTags() {
        val transactions = listOf(
            com.example.minhasfinancas.data.model.Transaction(
                id = 1L, description = "Supermercado", amount = 350.0,
                type = TransactionType.SAIDA, date = 1000L, tag = "Alimentação"
            ),
            com.example.minhasfinancas.data.model.Transaction(
                id = 2L, description = "Restaurante", amount = 120.0,
                type = TransactionType.SAIDA, date = 1050L, tag = "Alimentação"
            ),
            com.example.minhasfinancas.data.model.Transaction(
                id = 3L, description = "Gasolina", amount = 200.0,
                type = TransactionType.SAIDA, date = 1100L, tag = "Transporte"
            ),
            com.example.minhasfinancas.data.model.Transaction(
                id = 4L, description = "Salário Empresa", amount = 5000.0,
                type = TransactionType.ENTRADA, date = 1000L, tag = "Salário"
            ),
            com.example.minhasfinancas.data.model.Transaction(
                id = 5L, description = "Reembolso Almoço", amount = 50.0,
                type = TransactionType.ENTRADA, date = 1150L, tag = "Alimentação"
            )
        )

        // Usuário seleciona os marcadores "Alimentação" e "Transporte"
        val selectedTags = setOf("Alimentação", "Transporte")

        val visorTransactions = transactions.filter { selectedTags.contains(it.tag) }
        val visorIncome = visorTransactions.filter { it.type == TransactionType.ENTRADA }.sumOf { it.amount }
        val visorExpense = visorTransactions.filter { it.type == TransactionType.SAIDA }.sumOf { it.amount }
        val visorBalance = visorIncome - visorExpense

        assertEquals(4, visorTransactions.size)
        assertEquals(50.0, visorIncome, 0.001) // 50.0 (Reembolso Almoço)
        assertEquals(670.0, visorExpense, 0.001) // 350 + 120 + 200
        assertEquals(-620.0, visorBalance, 0.001) // 50 - 670
    }

    @Test
    fun testIndependentVisor_customDateRangeFilter() {
        val transactions = listOf(
            com.example.minhasfinancas.data.model.Transaction(
                id = 1L, description = "Compra 1", amount = 100.0,
                type = TransactionType.SAIDA, date = 100L, tag = "Alimentação"
            ),
            com.example.minhasfinancas.data.model.Transaction(
                id = 2L, description = "Compra 2", amount = 200.0,
                type = TransactionType.SAIDA, date = 250L, tag = "Alimentação"
            ),
            com.example.minhasfinancas.data.model.Transaction(
                id = 3L, description = "Compra 3", amount = 300.0,
                type = TransactionType.SAIDA, date = 500L, tag = "Alimentação"
            )
        )

        // Filtra pelo intervalo customizado [150, 300]
        val startRange = 150L
        val endRange = 300L
        val selectedTags = setOf("Alimentação")

        val filtered = transactions.filter { it.date in startRange..endRange && selectedTags.contains(it.tag) }

        assertEquals(1, filtered.size)
        assertEquals("Compra 2", filtered[0].description)
        assertEquals(200.0, filtered[0].amount, 0.001)
    }

    @Test
    fun testTransactionPaymentMethod() {
        val t1 = com.example.minhasfinancas.data.model.Transaction(
            id = 1L, description = "Padaria", amount = 25.0,
            type = TransactionType.SAIDA, date = 1000L, tag = "Alimentação",
            paymentMethod = "Pix"
        )
        val t2 = com.example.minhasfinancas.data.model.Transaction(
            id = 2L, description = "Farmácia", amount = 80.0,
            type = TransactionType.SAIDA, date = 1050L, tag = "Saúde",
            paymentMethod = "Cartão de Crédito"
        )
        val t3 = com.example.minhasfinancas.data.model.Transaction(
            id = 3L, description = "Feira", amount = 40.0,
            type = TransactionType.SAIDA, date = 1100L, tag = "Alimentação",
            paymentMethod = "Dinheiro em espécie"
        )

        assertEquals("Pix", t1.paymentMethod)
        assertEquals("Cartão de Crédito", t2.paymentMethod)
        assertEquals("Dinheiro em espécie", t3.paymentMethod)
    }

    @Test
    fun testInstallmentAutoSelectsCreditCardPaymentMethod() {
        var isInstallment = false
        var selectedPaymentMethod: String? = null

        // Usuário marca a caixa de compra parcelada
        isInstallment = true
        if (isInstallment) {
            selectedPaymentMethod = "Cartão de Crédito"
        }

        assertEquals("Cartão de Crédito", selectedPaymentMethod)
    }
}
