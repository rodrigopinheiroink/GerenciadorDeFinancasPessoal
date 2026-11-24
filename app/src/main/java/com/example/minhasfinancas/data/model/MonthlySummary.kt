package com.example.minhasfinancas.data.model

data class MonthlySummary(
    val year: Int,
    val month: Int, // 0 = Janeiro, 11 = Dezembro
    val totalIncome: Double,
    val totalExpense: Double
) {
    val balance: Double get() = totalIncome - totalExpense
}
