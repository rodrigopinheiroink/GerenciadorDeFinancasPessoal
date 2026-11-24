package com.example.minhasfinancas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.minhasfinancas.data.preferences.BankPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minhasfinancas.data.model.RecurrenceFrequency
import com.example.minhasfinancas.data.model.TagMarker
import com.example.minhasfinancas.data.model.TransactionType
import com.example.minhasfinancas.ui.components.DatePickerField
import androidx.compose.ui.platform.LocalContext
import com.example.minhasfinancas.data.preferences.MachineFeePreferences
import com.example.minhasfinancas.ui.components.InstallmentCounter
import com.example.minhasfinancas.ui.theme.ExpenseRed
import com.example.minhasfinancas.ui.theme.IncomeGreen
import com.example.minhasfinancas.ui.util.Formatters
import java.util.Calendar

enum class CardPaymentType {
    DEBITO,
    CREDITO
}

enum class AmountInputType {
    VALOR_BRUTO,
    VALOR_DESCONTADO
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    tags: List<TagMarker>,
    onDismiss: () -> Unit,
    onConfirmNormal: (
        description: String,
        amount: Double,
        type: TransactionType,
        date: Long,
        tag: String,
        paymentMethod: String?,
        machineGrossAmount: Double?,
        machineFeeAmount: Double?,
        machineFeePercent: Double?
    ) -> Unit,
    onConfirmRecurring: (description: String, amount: Double, type: TransactionType, tag: String, frequency: RecurrenceFrequency, startTimestamp: Long) -> Unit,
    onConfirmInstallment: (description: String, totalAmount: Double, installmentAmount: Double, numberOfInstallments: Int, tag: String, startTimestamp: Long, type: TransactionType, paymentMethod: String?) -> Unit,
    onAddNewTag: () -> Unit,
    onManageTags: () -> Unit = {}
) {
    val context = LocalContext.current

    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.SAIDA) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var isTagsExpanded by remember { mutableStateOf(false) }
    var showMissingTagAlert by remember { mutableStateOf(false) }

    var selectedDateMillis by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Opções de Mais Detalhes / Meio de Pagamento e Bancos
    var selectedPaymentMethods by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedBanks by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var bankListVersion by remember { mutableIntStateOf(0) }
    var bankDialogMethod by remember { mutableStateOf<String?>(null) }
    var newBankInput by remember { mutableStateOf("") }
    var bankToEdit by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editBankInput by remember { mutableStateOf("") }
    var bankToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var splitAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Opções de Recorrente
    var isRecurring by remember { mutableStateOf(false) }
    var recurringFrequency by remember { mutableStateOf(RecurrenceFrequency.MENSAL) }

    // Opções de Parcelamento / Maquininha
    var isInstallment by remember { mutableStateOf(false) }
    var installmentMode by remember { mutableStateOf(InstallmentInputMode.VALOR_TOTAL) }
    var numInstallments by remember { mutableIntStateOf(10) }

    // Campos da Maquininha (Entrada de Receita)
    var machineGrossText by remember { mutableStateOf("") }
    var machineNetText by remember { mutableStateOf("") }

    // Chave e Campos legados da Calculadora de Maquininha
    var cardPaymentType by remember { mutableStateOf(CardPaymentType.CREDITO) }
    var amountInputType by remember { mutableStateOf(AmountInputType.VALOR_BRUTO) }
    var debitFeeText by remember { mutableStateOf(MachineFeePreferences.getDebitFee(context)) }              // % taxa débito
    var creditCashFeeText by remember { mutableStateOf(MachineFeePreferences.getCreditCashFee(context)) }         // % taxa crédito à vista (1x)
    var creditInstallmentFeeText by remember { mutableStateOf(MachineFeePreferences.getCreditInstallmentFee(context)) }  // % taxa crédito parcelado
    var advanceFeeText by remember { mutableStateOf(MachineFeePreferences.getAdvanceFee(context)) }            // % taxa antecipação / adiantamento

    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0

    // Cálculos de valor bruto (venda ou compra)
    val effectiveInstallments = if (selectedType == TransactionType.ENTRADA && cardPaymentType == CardPaymentType.DEBITO) 1 else numInstallments

    // Porcentagens de taxas
    val debitFeePercent = debitFeeText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val creditCashFeePercent = creditCashFeeText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val creditInstallmentFeePercent = creditInstallmentFeeText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val advanceFeePercent = advanceFeeText.replace(",", ".").toDoubleOrNull() ?: 0.0

    // Cálculo da taxa percentual total aplicável
    val appliedFeePercent: Double
    if (selectedType == TransactionType.ENTRADA) {
        appliedFeePercent = if (cardPaymentType == CardPaymentType.DEBITO) {
            debitFeePercent
        } else {
            if (effectiveInstallments <= 1) {
                creditCashFeePercent
            } else {
                creditInstallmentFeePercent + advanceFeePercent
            }
        }
    } else {
        appliedFeePercent = 0.0
    }

    val feeRate = appliedFeePercent / 100.0

    // Cálculos bidirecionais (Valor Bruto vs Valor Descontado)
    val grossTotal: Double
    val grossParcel: Double
    val netTotal: Double
    val netParcel: Double
    val totalFeesAmount: Double
    val debitFeeAmount: Double
    val creditCashFeeAmount: Double
    val installmentFeeAmount: Double
    val advanceFeeAmount: Double

    if (selectedType == TransactionType.ENTRADA && isInstallment) {
        if (amountInputType == AmountInputType.VALOR_BRUTO) {
            // Usuário informou o valor bruto a ser cobrado na máquina
            if (installmentMode == InstallmentInputMode.VALOR_TOTAL) {
                grossTotal = parsedAmount
                grossParcel = if (effectiveInstallments > 0) parsedAmount / effectiveInstallments else 0.0
            } else {
                grossParcel = parsedAmount
                grossTotal = parsedAmount * effectiveInstallments
            }

            totalFeesAmount = (grossTotal * feeRate).coerceAtLeast(0.0)
            netTotal = (grossTotal - totalFeesAmount).coerceAtLeast(0.0)
            netParcel = if (effectiveInstallments > 0) netTotal / effectiveInstallments else 0.0
        } else {
            // Usuário informou o valor descontado (líquido) que caiu ou deve cair na conta
            if (installmentMode == InstallmentInputMode.VALOR_TOTAL) {
                netTotal = parsedAmount
                netParcel = if (effectiveInstallments > 0) parsedAmount / effectiveInstallments else 0.0
            } else {
                netParcel = parsedAmount
                netTotal = parsedAmount * effectiveInstallments
            }

            // Cálculo reverso do valor bruto a ser passado na máquina: Bruto = Líquido / (1 - taxa)
            val divisor = (1.0 - feeRate).coerceAtLeast(0.0001)
            grossTotal = if (feeRate < 1.0) netTotal / divisor else netTotal
            grossParcel = if (effectiveInstallments > 0) grossTotal / effectiveInstallments else 0.0
            totalFeesAmount = (grossTotal - netTotal).coerceAtLeast(0.0)
        }

        // Decomposição individual das taxas sobre o valor bruto
        if (cardPaymentType == CardPaymentType.DEBITO) {
            debitFeeAmount = (grossTotal * debitFeePercent) / 100.0
            creditCashFeeAmount = 0.0
            installmentFeeAmount = 0.0
            advanceFeeAmount = 0.0
        } else {
            debitFeeAmount = 0.0
            if (effectiveInstallments <= 1) {
                creditCashFeeAmount = (grossTotal * creditCashFeePercent) / 100.0
                installmentFeeAmount = 0.0
                advanceFeeAmount = 0.0
            } else {
                creditCashFeeAmount = 0.0
                installmentFeeAmount = (grossTotal * creditInstallmentFeePercent) / 100.0
                advanceFeeAmount = (grossTotal * advanceFeePercent) / 100.0
            }
        }
    } else {
        // Saída ou movimentação convencional
        if (installmentMode == InstallmentInputMode.VALOR_TOTAL) {
            grossTotal = parsedAmount
            grossParcel = if (effectiveInstallments > 0) parsedAmount / effectiveInstallments else 0.0
        } else {
            grossParcel = parsedAmount
            grossTotal = parsedAmount * effectiveInstallments
        }
        netTotal = grossTotal
        netParcel = grossParcel
        totalFeesAmount = 0.0
        debitFeeAmount = 0.0
        creditCashFeeAmount = 0.0
        installmentFeeAmount = 0.0
        advanceFeeAmount = 0.0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    Surface(
                        tonalElevation = 2.dp,
                        shadowElevation = 3.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar"
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nova Movimentação",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = {
                                    val rawDesc = description.trim()
                                    if (rawDesc.isBlank()) {
                                        errorMessage = "Informe a descrição"
                                        return@Button
                                    }

                                    // Validação de Marcador Obrigatório
                                    if (selectedTag.isNullOrBlank()) {
                                        showMissingTagAlert = true
                                        return@Button
                                    }

                                    val chosenTag = selectedTag!!

                                    // Validação do meio de pagamento combinado (quando mais de 1 forma selecionada)
                                    if (selectedPaymentMethods.size > 1) {
                                        val targetTotal = if (isInstallment && selectedType == TransactionType.SAIDA) grossTotal else parsedAmount
                                        val splitSum = selectedPaymentMethods.sumOf { method ->
                                            splitAmounts[method]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                        }
                                        if (kotlin.math.abs(targetTotal - splitSum) > 0.01) {
                                            errorMessage = "A soma entre as formas de pagamento está incorreta, verifique o valor total informado"
                                            return@Button
                                        }
                                    }

                                    // Meios de pagamento formatados com seus respectivos bancos se selecionados
                                    val formattedMethodsList = selectedPaymentMethods.map { method ->
                                        val bank = selectedBanks[method]
                                        if (!bank.isNullOrBlank()) "$method ($bank)" else method
                                    }

                                    // Se mais de uma forma selecionada, formata na descrição como "desc (pix+cartao de credito)"
                                    val formattedMethods = if (selectedPaymentMethods.size > 1) {
                                        selectedPaymentMethods.joinToString("+")
                                    } else null

                                    val desc = if (formattedMethods != null) "$rawDesc ($formattedMethods)" else rawDesc
                                    val chosenPaymentMethod = if (formattedMethodsList.isNotEmpty()) formattedMethodsList.joinToString(" + ") else null

                                    if (isInstallment) {
                                        if (selectedType == TransactionType.ENTRADA) {
                                            val parsedGross = machineGrossText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                            val parsedNet = machineNetText.replace(",", ".").toDoubleOrNull() ?: 0.0

                                            if (parsedGross <= 0.0) {
                                                errorMessage = "Informe o valor cobrado na maquininha"
                                                return@Button
                                            }
                                            if (parsedNet <= 0.0) {
                                                errorMessage = "Informe o valor recebido em conta"
                                                return@Button
                                            }
                                            if (parsedNet > parsedGross) {
                                                errorMessage = "O valor recebido não pode ser maior que o valor cobrado"
                                                return@Button
                                            }

                                            val feeAmount = (parsedGross - parsedNet).coerceAtLeast(0.0)
                                            val feePercent = if (parsedGross > 0.0) (feeAmount / parsedGross) * 100.0 else 0.0

                                            val detailNote = "Taxa Maquininha: ${String.format(java.util.Locale.US, "%.2f", feePercent)}% (${Formatters.formatCurrency(feeAmount)})"
                                            val finalDesc = if (rawDesc.isNotBlank()) "$desc ($detailNote)" else "Venda na Maquininha ($detailNote)"

                                            val entradaPaymentMethod = if (chosenPaymentMethod.isNullOrBlank()) "Maquininha de Cartão" else chosenPaymentMethod

                                            onConfirmNormal(
                                                finalDesc,
                                                parsedNet,
                                                TransactionType.ENTRADA,
                                                selectedDateMillis,
                                                chosenTag,
                                                entradaPaymentMethod,
                                                parsedGross,
                                                feeAmount,
                                                feePercent
                                            )
                                        } else {
                                            if (grossTotal <= 0.0 || grossParcel <= 0.0) {
                                                errorMessage = "Informe um valor válido"
                                                return@Button
                                            }
                                            // Compra parcelada (Saída)
                                            onConfirmInstallment(
                                                desc,
                                                grossTotal,
                                                grossParcel,
                                                numInstallments,
                                                chosenTag,
                                                selectedDateMillis,
                                                TransactionType.SAIDA,
                                                chosenPaymentMethod ?: "Cartão de Crédito"
                                            )
                                        }
                                    } else if (isRecurring) {
                                        if (parsedAmount <= 0.0) {
                                            errorMessage = "Informe um valor válido"
                                            return@Button
                                        }
                                        onConfirmRecurring(
                                            desc,
                                            parsedAmount,
                                            selectedType,
                                            chosenTag,
                                            recurringFrequency,
                                            selectedDateMillis
                                        )
                                    } else {
                                        if (parsedAmount <= 0.0) {
                                            errorMessage = "Informe um valor válido"
                                            return@Button
                                        }
                                        onConfirmNormal(
                                            desc,
                                            parsedAmount,
                                            selectedType,
                                            selectedDateMillis,
                                            chosenTag,
                                            chosenPaymentMethod,
                                            null,
                                            null,
                                            null
                                        )
                                    }
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Adicionar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                // Seletor Entrada vs Saída
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectTypeCard(
                        title = "Saída (Gasto)",
                        icon = Icons.Default.ArrowDownward,
                        isSelected = selectedType == TransactionType.SAIDA,
                        activeColor = ExpenseRed,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedType = TransactionType.SAIDA
                        }
                    )

                    SelectTypeCard(
                        title = "Entrada (Receita)",
                        icon = Icons.Default.ArrowUpward,
                        isSelected = selectedType == TransactionType.ENTRADA,
                        activeColor = IncomeGreen,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedType = TransactionType.ENTRADA
                            selectedPaymentMethods = selectedPaymentMethods - setOf("Cartão de Crédito", "Cartão de Débito")
                            selectedBanks = selectedBanks - setOf("Cartão de Crédito", "Cartão de Débito")
                            splitAmounts = splitAmounts - setOf("Cartão de Crédito", "Cartão de Débito")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Valor
                val amountLabel = when {
                    isInstallment && selectedType == TransactionType.ENTRADA -> {
                        if (amountInputType == AmountInputType.VALOR_BRUTO) {
                            if (installmentMode == InstallmentInputMode.VALOR_TOTAL) "Valor Bruto Total da Venda (R$)"
                            else "Valor Bruto da Parcela (R$)"
                        } else {
                            if (installmentMode == InstallmentInputMode.VALOR_TOTAL) "Valor Descontado Líquido (R$)"
                            else "Valor Descontado da Parcela (R$)"
                        }
                    }
                    isInstallment && selectedType == TransactionType.SAIDA && installmentMode == InstallmentInputMode.VALOR_TOTAL -> "Valor Total da Compra (R$)"
                    isInstallment && selectedType == TransactionType.SAIDA && installmentMode == InstallmentInputMode.VALOR_DA_PARCELA -> "Valor de Cada Parcela (R$)"
                    else -> "Valor (R$)"
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.replace(",", ".")
                        errorMessage = null
                    },
                    label = { Text(amountLabel) },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Descrição
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        errorMessage = null
                    },
                    label = { Text("Descrição") },
                    placeholder = { Text("Ex: Supermercado, Aluguel, Salário, Venda") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Data da Movimentação
                DatePickerField(
                    selectedDateMillis = selectedDateMillis,
                    onDateSelected = { selectedDateMillis = it }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Marcador / Categoria com opção de Maximizar ou Compactar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (selectedTag != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sell,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Marcador / Categoria",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Botão Maximizar / Compactar (sem elementos extras ao lado)
                        TextButton(
                            onClick = { isTagsExpanded = !isTagsExpanded },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isTagsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (isTagsExpanded) "Compactar" else "Maximizar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isTagsExpanded) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.forEach { tag ->
                                val isSelected = selectedTag.equals(tag.name, ignoreCase = true)
                                val color = Formatters.parseHexColor(tag.colorHex)

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTag = tag.name },
                                    label = { Text(tag.name, fontSize = 12.sp) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                    }
                                )
                            }

                            FilterChip(
                                selected = false,
                                onClick = onAddNewTag,
                                label = { Text("Criar Novo", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Criar novo marcador",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )

                            FilterChip(
                                selected = false,
                                onClick = onManageTags,
                                label = { Text("Gerenciar", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Gerenciar marcadores",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    } else {
                        if (selectedTag != null) {
                            val selectedTagObj = tags.firstOrNull { it.name.equals(selectedTag, ignoreCase = true) }
                            val color = selectedTagObj?.let { Formatters.parseHexColor(it.colorHex) } ?: MaterialTheme.colorScheme.primary

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(color.copy(alpha = 0.12f))
                                    .clickable { isTagsExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = selectedTag!!,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "Alterar",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { isTagsExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum marcador selecionado (Toque para escolher)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(14.dp))

                // Caixa: Mais Detalhes sobre a transação (Meio de Pagamento para Saída e Entrada)
                val hasPaymentSelection = selectedPaymentMethods.isNotEmpty()
                val formattedSelectedMethods = selectedPaymentMethods.map { method ->
                    val b = selectedBanks[method]
                    if (!b.isNullOrBlank()) "$method ($b)" else method
                }
                val paymentHeaderTitle = if (!hasPaymentSelection) {
                    "Mais detalhes sobre a transação"
                } else {
                    "Meio de pagamento selecionado: ${formattedSelectedMethods.joinToString(" + ")}"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.dp,
                            color = if (hasPaymentSelection) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = if (hasPaymentSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = paymentHeaderTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (hasPaymentSelection) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = if (hasPaymentSelection) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (hasPaymentSelection) {
                                TextButton(
                                    onClick = {
                                        if (isInstallment && selectedType == TransactionType.SAIDA) {
                                            selectedPaymentMethods = setOf("Cartão de Crédito")
                                            selectedBanks = selectedBanks.filterKeys { it == "Cartão de Crédito" }
                                        } else {
                                            selectedPaymentMethods = emptySet()
                                            selectedBanks = emptyMap()
                                        }
                                        splitAmounts = emptyMap()
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = "Limpar",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Meio de pagamento utilizado (opcional):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val paymentOptions = if (selectedType == TransactionType.ENTRADA) {
                                listOf(
                                    "Pix",
                                    "Dinheiro em espécie"
                                )
                            } else {
                                listOf(
                                    "Pix",
                                    "Cartão de Crédito",
                                    "Cartão de Débito",
                                    "Dinheiro em espécie"
                                )
                            }

                            paymentOptions.forEach { option ->
                                val isSelected = selectedPaymentMethods.contains(option)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            if (!(isInstallment && selectedType == TransactionType.SAIDA && option == "Cartão de Crédito")) {
                                                selectedPaymentMethods = selectedPaymentMethods - option
                                                selectedBanks = selectedBanks - option
                                                splitAmounts = splitAmounts - option
                                            }
                                        } else {
                                            selectedPaymentMethods = selectedPaymentMethods + option
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = option,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        val icon = when (option) {
                                            "Pix" -> Icons.Default.QrCode
                                            "Cartão de Crédito", "Cartão de Débito" -> Icons.Default.CreditCard
                                            else -> Icons.Default.Payments
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }

                            // Opção para escolher ou criar banco para cada meio exceto "Dinheiro em espécie"
                            val eligibleBankMethods = selectedPaymentMethods.filter { it != "Dinheiro em espécie" }
                            if (eligibleBankMethods.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    eligibleBankMethods.forEach { method ->
                                        val banks = remember(method, bankListVersion) {
                                            BankPreferences.getBanks(context, method)
                                        }
                                        val currentSelectedBank = selectedBanks[method]

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AccountBalance,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = if (selectedType == TransactionType.ENTRADA) "Qual banco está entrando o dinheiro ($method)?" else "Qual banco está saindo o dinheiro ($method)?",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }

                                                    TextButton(
                                                        onClick = {
                                                            newBankInput = ""
                                                            bankDialogMethod = method
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Tune,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "Criar / Gerenciar",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                if (banks.isEmpty()) {
                                                    Text(
                                                        text = "Nenhum banco cadastrado para $method. Toque em 'Criar / Gerenciar' para adicionar seu banco.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    FlowRow(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        banks.forEach { bankName ->
                                                            val isBankSelected = currentSelectedBank == bankName
                                                            FilterChip(
                                                                selected = isBankSelected,
                                                                onClick = {
                                                                    selectedBanks = if (isBankSelected) {
                                                                        selectedBanks - method
                                                                    } else {
                                                                        selectedBanks + (method to bankName)
                                                                    }
                                                                },
                                                                label = {
                                                                    Text(
                                                                        text = bankName,
                                                                        fontSize = 12.sp,
                                                                        fontWeight = if (isBankSelected) FontWeight.Bold else FontWeight.Normal
                                                                    )
                                                                },
                                                                trailingIcon = {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Close,
                                                                        contentDescription = "Excluir $bankName",
                                                                        modifier = Modifier
                                                                            .size(14.dp)
                                                                            .clickable {
                                                                                bankToDelete = method to bankName
                                                                            }
                                                                    )
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Caixa para informar valor em cada forma de pagamento caso mais de 1 seja selecionada
                            if (selectedPaymentMethods.size > 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (selectedType == TransactionType.ENTRADA) "Informe o valor recebido em cada forma:" else "Informe o valor pago em cada forma:",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val targetTotal = if (isInstallment && selectedType == TransactionType.SAIDA) grossTotal else parsedAmount
                                        val currentSum = selectedPaymentMethods.sumOf { method ->
                                            splitAmounts[method]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                        }
                                        val difference = targetTotal - currentSum
                                        val isSumMatching = kotlin.math.abs(difference) < 0.01

                                        selectedPaymentMethods.forEach { method ->
                                            OutlinedTextField(
                                                value = splitAmounts[method] ?: "",
                                                onValueChange = { newVal ->
                                                    if (newVal.all { it.isDigit() || it == ',' || it == '.' }) {
                                                        splitAmounts = splitAmounts + (method to newVal)
                                                    }
                                                },
                                                label = { Text("Valor em $method") },
                                                placeholder = { Text("0,00") },
                                                prefix = { Text("R$ ", fontSize = 13.sp) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        }

                                        // Comparativo visual
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSumMatching && targetTotal > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Valor total informado:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = Formatters.formatCurrency(targetTotal),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Soma das formas:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = Formatters.formatCurrency(currentSum),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSumMatching && targetTotal > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                                )
                                            }
                                            if (!isSumMatching && currentSum > 0.0) {
                                                Text(
                                                    text = "Diferença: ${Formatters.formatCurrency(kotlin.math.abs(difference))}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                // 1. Repetir periodicamente (Sempre visível; cinza opaco se desmarcado)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.dp,
                            color = if (isRecurring) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isRecurring = !isRecurring
                                    if (isRecurring) isInstallment = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isRecurring,
                                onCheckedChange = {
                                    isRecurring = it
                                    if (it) isInstallment = false
                                }
                            )
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = if (isRecurring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Repetir periodicamente (Recorrente)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isRecurring) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isRecurring) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp)
                                .graphicsLayer(alpha = if (isRecurring) 1f else 0.38f)
                        ) {
                            Text(
                                text = "Frequência de repetição:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = isRecurring && recurringFrequency == RecurrenceFrequency.MENSAL,
                                    onClick = { if (isRecurring) recurringFrequency = RecurrenceFrequency.MENSAL },
                                    enabled = isRecurring,
                                    label = { Text("Todo Mês") }
                                )
                                FilterChip(
                                    selected = isRecurring && recurringFrequency == RecurrenceFrequency.ANUAL,
                                    onClick = { if (isRecurring) recurringFrequency = RecurrenceFrequency.ANUAL },
                                    enabled = isRecurring,
                                    label = { Text("Todo Ano") }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Pagamento / Recebimento Parcelado & Calculadora da Maquininha
                val isIncome = selectedType == TransactionType.ENTRADA
                val installmentThemeColor = if (isIncome) IncomeGreen else Color(0xFFF97316)
                val installmentTitle = if (isIncome) "Recebimento na maquininha (Cartão)" else "Compra parcelada"

                val isCreditActive = isInstallment && !isIncome

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.dp,
                            color = if (isInstallment) installmentThemeColor.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isInstallment = !isInstallment
                                    if (isInstallment) {
                                        isRecurring = false
                                        selectedPaymentMethods = selectedPaymentMethods + "Cartão de Crédito"
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isInstallment,
                                onCheckedChange = {
                                    isInstallment = it
                                    if (it) {
                                        isRecurring = false
                                        selectedPaymentMethods = selectedPaymentMethods + "Cartão de Crédito"
                                    }
                                }
                            )
                            Icon(
                                imageVector = if (isIncome) Icons.Default.PointOfSale else Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = if (isInstallment) installmentThemeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = installmentTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isInstallment) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isInstallment) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Conteúdo da Maquininha / Parcelamento
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 4.dp)
                                .graphicsLayer(alpha = if (isInstallment) 1f else 0.38f)
                        ) {
                            if (isIncome) {
                                // Apenas 2 campos para o usuário:
                                // 1º campo: Valor cobrado
                                // 2º campo: Valor recebido em conta
                                // E um campo abaixo informando para o usuário quanto de taxa ele pagou para a maquininha
                                // informando apenas a porcentagem e o valor em R$
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = machineGrossText,
                                        onValueChange = {
                                            machineGrossText = it.replace(",", ".")
                                            errorMessage = null
                                        },
                                        label = { Text("Valor cobrado na maquininha (R$)") },
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        enabled = isInstallment,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = machineNetText,
                                        onValueChange = {
                                            machineNetText = it.replace(",", ".")
                                            amountText = it.replace(",", ".")
                                            errorMessage = null
                                        },
                                        label = { Text("Valor recebido em conta (R$)") },
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        enabled = isInstallment,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    val parsedGross = machineGrossText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    val parsedNet = machineNetText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    val machineFee = (parsedGross - parsedNet).coerceAtLeast(0.0)
                                    val machineRate = if (parsedGross > 0.0) (machineFee / parsedGross) * 100.0 else 0.0

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isInstallment) installmentThemeColor.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "Taxa paga para a maquininha:",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Porcentagem: ${String.format(java.util.Locale.US, "%.2f", machineRate)}%",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (machineFee > 0.0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Valor: ${Formatters.formatCurrency(machineFee)}",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (machineFee > 0.0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Modo de entrada da parcela (Saída)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = isCreditActive && installmentMode == InstallmentInputMode.VALOR_TOTAL,
                                        onClick = { if (isCreditActive) installmentMode = InstallmentInputMode.VALOR_TOTAL },
                                        enabled = isCreditActive,
                                        label = { Text("Valor total", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = isCreditActive && installmentMode == InstallmentInputMode.VALOR_DA_PARCELA,
                                        onClick = { if (isCreditActive) installmentMode = InstallmentInputMode.VALOR_DA_PARCELA },
                                        enabled = isCreditActive,
                                        label = { Text("Valor da parcela", fontSize = 11.sp) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Contador de Parcelas
                                InstallmentCounter(
                                    value = numInstallments,
                                    onValueChange = { if (isCreditActive) numInstallments = it },
                                    minValue = 1,
                                    maxValue = 60,
                                    enabled = isCreditActive
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isInstallment) installmentThemeColor.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "$numInstallments parcelas de ${Formatters.formatCurrency(grossParcel)} (Total: ${Formatters.formatCurrency(grossTotal)})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

    // Alerta caso o usuário tente adicionar sem selecionar um marcador
    if (showMissingTagAlert) {
        AlertDialog(
            onDismissRequest = { showMissingTagAlert = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Sell,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Marcador Obrigatório",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Para manter o controle e a organização das suas finanças sempre em dia, você deve selecionar um marcador para esta movimentação.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMissingTagAlert = false
                        isTagsExpanded = true // Maximiza os marcadores para o usuário escolher imediatamente
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selecionar Marcador")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Diálogo para criar, ordenar, editar e excluir bancos para um meio de pagamento
    if (bankDialogMethod != null) {
        val method = bankDialogMethod!!
        val currentBanks = remember(method, bankListVersion) {
            BankPreferences.getBanks(context, method)
        }
        val allUsedBanks = remember(bankListVersion) {
            BankPreferences.getAllUsedBanks(context)
        }
        val suggestions = allUsedBanks.filter { used -> !currentBanks.any { it.equals(used, ignoreCase = true) } }

        AlertDialog(
            onDismissRequest = {
                bankDialogMethod = null
                newBankInput = ""
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Criar e Gerenciar Bancos ($method)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Cadastre um novo banco para $method ou gerencie a ordem, nomes e exclusão:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Campo para adicionar novo banco
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = newBankInput,
                            onValueChange = { newBankInput = it },
                            label = { Text("Nome do banco") },
                            placeholder = { Text("Ex: Nubank, Inter...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                val trimmed = newBankInput.trim()
                                if (trimmed.isNotBlank()) {
                                    BankPreferences.addBank(context, method, trimmed)
                                    selectedBanks = selectedBanks + (method to trimmed)
                                    newBankInput = ""
                                    bankListVersion++
                                }
                            },
                            enabled = newBankInput.trim().isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Salvar")
                        }
                    }

                    // Sugestões de bancos já criados em outros meios
                    if (suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Bancos utilizados anteriormente (toque para adicionar):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            suggestions.forEach { prevBank ->
                                AssistChip(
                                    onClick = {
                                        BankPreferences.addBank(context, method, prevBank)
                                        selectedBanks = selectedBanks + (method to prevBank)
                                        bankListVersion++
                                    },
                                    label = {
                                        Text("+ $prevBank", fontSize = 11.sp)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ordem dos bancos cadastrados:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (currentBanks.isEmpty()) {
                        Text(
                            text = "Nenhum banco cadastrado ainda para $method. Digite o nome acima para adicionar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(currentBanks, key = { _, bank -> "$method-$bank" }) { index, bankName ->
                                val isSelected = selectedBanks[method] == bankName
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    selectedBanks = if (isSelected) selectedBanks - method
                                                    else selectedBanks + (method to bankName)
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = bankName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Text(
                                                    text = "(ativo)",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Controles: Subir, Descer, Editar, Excluir
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Mover para cima
                                            IconButton(
                                                onClick = {
                                                    if (index > 0) {
                                                        val mutable = currentBanks.toMutableList()
                                                        val temp = mutable[index]
                                                        mutable[index] = mutable[index - 1]
                                                        mutable[index - 1] = temp
                                                        BankPreferences.reorderBanks(context, method, mutable)
                                                        bankListVersion++
                                                    }
                                                },
                                                enabled = index > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Mover para cima",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                                )
                                            }

                                            // Mover para baixo
                                            IconButton(
                                                onClick = {
                                                    if (index < currentBanks.size - 1) {
                                                        val mutable = currentBanks.toMutableList()
                                                        val temp = mutable[index]
                                                        mutable[index] = mutable[index + 1]
                                                        mutable[index + 1] = temp
                                                        BankPreferences.reorderBanks(context, method, mutable)
                                                        bankListVersion++
                                                    }
                                                },
                                                enabled = index < currentBanks.size - 1,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Mover para baixo",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (index < currentBanks.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                                )
                                            }

                                            // Editar
                                            IconButton(
                                                onClick = {
                                                    bankToEdit = method to bankName
                                                    editBankInput = bankName
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Editar nome",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            // Excluir
                                            IconButton(
                                                onClick = {
                                                    bankToDelete = method to bankName
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Excluir banco",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        bankDialogMethod = null
                        newBankInput = ""
                    }
                ) {
                    Text("Concluir")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Diálogo para editar nome do banco
    if (bankToEdit != null) {
        val (method, oldName) = bankToEdit!!
        AlertDialog(
            onDismissRequest = { bankToEdit = null },
            title = {
                Text(
                    text = "Editar Nome do Banco",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Informe o novo nome para o banco:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editBankInput,
                        onValueChange = { editBankInput = it },
                        label = { Text("Nome do banco") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editBankInput.trim()
                        if (trimmed.isNotBlank()) {
                            BankPreferences.updateBankName(context, method, oldName, trimmed)
                            if (selectedBanks[method] == oldName) {
                                selectedBanks = selectedBanks + (method to trimmed)
                            }
                            bankToEdit = null
                            bankListVersion++
                        }
                    },
                    enabled = editBankInput.trim().isNotBlank()
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { bankToEdit = null }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Diálogo de confirmação para excluir banco da lista
    if (bankToDelete != null) {
        val (method, bankName) = bankToDelete!!
        AlertDialog(
            onDismissRequest = { bankToDelete = null },
            title = {
                Text(
                    text = "Excluir Banco",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text("Deseja remover o banco \"$bankName\" da sua lista de $method?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        BankPreferences.removeBank(context, method, bankName)
                        if (selectedBanks[method] == bankName) {
                            selectedBanks = selectedBanks - method
                        }
                        bankToDelete = null
                        bankListVersion++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { bankToDelete = null }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SelectTypeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, activeColor, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                ),
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
