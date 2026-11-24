package com.example.minhasfinancas.data.model

data class RecurringPlan(
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val tag: String,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MENSAL,
    val startTimestamp: Long,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
