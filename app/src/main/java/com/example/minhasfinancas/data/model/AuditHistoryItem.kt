package com.example.minhasfinancas.data.model

data class AuditHistoryItem(
    val id: Long = 0,
    val actionType: String, // "CRIADA", "EXCLUIDA", "PARCELAMENTO_CRIADO", "PARCELAMENTO_EXCLUIDO", "RECORRENTE_CRIADA", "RECORRENTE_EXCLUIDA"
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val tag: String,
    val eventTimestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)
