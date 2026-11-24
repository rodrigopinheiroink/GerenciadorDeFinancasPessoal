package com.example.minhasfinancas.data.model

data class Transaction(
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val date: Long, // timestamp em milissegundos
    val tag: String, // Marcador / Categoria
    val isRecurring: Boolean = false,
    val recurringPlanId: Long? = null,
    val installmentPlanId: Long? = null,
    val installmentNumber: Int? = null,
    val installmentTotal: Int? = null,
    val paymentMethod: String? = null,
    val machineGrossAmount: Double? = null,
    val machineFeeAmount: Double? = null,
    val machineFeePercent: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isMachineTransaction: Boolean
        get() = (machineFeeAmount != null && machineFeeAmount > 0.0) ||
                (machineGrossAmount != null && machineGrossAmount > 0.0) ||
                (paymentMethod?.contains("Maquininha", ignoreCase = true) == true) ||
                description.contains("Maquininha", ignoreCase = true) ||
                description.contains("Venda Débito", ignoreCase = true) ||
                description.contains("Venda Crédito", ignoreCase = true)

    fun getEffectiveMachineGross(): Double {
        if (machineGrossAmount != null && machineGrossAmount > 0.0) return machineGrossAmount
        val match = Regex("""(?:Bruto|Cobrado):\s*R\$\s*([\d\.,]+)""").find(description)
        if (match != null) {
            val str = match.groupValues[1].replace(".", "").replace(",", ".")
            val parsed = str.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) return parsed
        }
        return amount + getEffectiveMachineFee()
    }

    fun getEffectiveMachineFee(): Double {
        if (machineFeeAmount != null) return machineFeeAmount
        val matchFee = Regex("""(?:Taxa|Pago à Maquininha):\s*-?\s*R\$\s*([\d\.,]+)""").find(description)
        if (matchFee != null) {
            val str = matchFee.groupValues[1].replace(".", "").replace(",", ".")
            val parsed = str.toDoubleOrNull()
            if (parsed != null) return parsed
        }
        val matchParen = Regex("""Taxa Maquininha:\s*[\d\.,]+%\s*\(R\$\s*([\d\.,]+)\)""").find(description)
        if (matchParen != null) {
            val str = matchParen.groupValues[1].replace(".", "").replace(",", ".")
            val parsed = str.toDoubleOrNull()
            if (parsed != null) return parsed
        }
        return 0.0
    }

    fun getEffectiveMachineFeePercent(): Double {
        if (machineFeePercent != null && machineFeePercent > 0.0) return machineFeePercent
        val matchPct = Regex("""(?:Taxa Maquininha|Taxa):\s*([\d\.,]+)%""").find(description)
        if (matchPct != null) {
            val str = matchPct.groupValues[1].replace(",", ".")
            val parsed = str.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) return parsed
        }
        val gross = getEffectiveMachineGross()
        val fee = getEffectiveMachineFee()
        return if (gross > 0.0) (fee / gross) * 100.0 else 0.0
    }
}
