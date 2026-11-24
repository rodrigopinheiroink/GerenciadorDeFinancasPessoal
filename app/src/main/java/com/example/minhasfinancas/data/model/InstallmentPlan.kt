package com.example.minhasfinancas.data.model

data class InstallmentPlan(
    val id: Long = 0,
    val description: String,
    val totalAmount: Double,
    val installmentAmount: Double,
    val numberOfInstallments: Int,
    val tag: String,
    val startTimestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)
