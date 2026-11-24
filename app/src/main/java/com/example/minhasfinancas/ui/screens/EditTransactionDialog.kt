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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minhasfinancas.data.model.TagMarker
import com.example.minhasfinancas.data.model.Transaction
import com.example.minhasfinancas.data.model.TransactionType
import com.example.minhasfinancas.data.preferences.BankPreferences
import com.example.minhasfinancas.ui.components.DatePickerField
import com.example.minhasfinancas.ui.theme.ExpenseRed
import com.example.minhasfinancas.ui.theme.IncomeGreen
import com.example.minhasfinancas.ui.util.Formatters

private fun parsePaymentMethod(raw: String?): Pair<Set<String>, Map<String, String>> {
    if (raw.isNullOrBlank()) return emptySet<String>() to emptyMap<String, String>()
    val parts = raw.split(" + ").map { it.trim() }
    val methods = mutableSetOf<String>()
    val banks = mutableMapOf<String, String>()
    val known = listOf("Pix", "Cartão de Crédito", "Cartão de Débito", "Dinheiro em espécie")

    for (part in parts) {
        for (k in known) {
            if (part.startsWith(k)) {
                methods.add(k)
                val openParen = part.indexOf('(')
                val closeParen = part.indexOf(')')
                if (openParen != -1 && closeParen > openParen) {
                    val b = part.substring(openParen + 1, closeParen).trim()
                    if (b.isNotEmpty()) banks[k] = b
                }
                break
            }
        }
    }
    return methods to banks
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    tags: List<TagMarker>,
    onDismiss: () -> Unit,
    onConfirmEdit: (updatedTransaction: Transaction) -> Unit,
    onDeleteTransaction: ((Long) -> Unit)? = null,
    onAddNewTag: () -> Unit = {}
) {
    val context = LocalContext.current

    var description by remember { mutableStateOf(transaction.description) }
    var amountText by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%.2f", transaction.amount).replace(".", ","))
    }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedTag by remember { mutableStateOf<String?>(transaction.tag) }
    var selectedDateMillis by remember { mutableStateOf(transaction.date) }
    var isTagsExpanded by remember { mutableStateOf(false) }
    var showMissingTagAlert by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Parse prévio das formas de pagamento e bancos da transação existente
    val (initialMethods, initialBanks) = remember(transaction) { parsePaymentMethod(transaction.paymentMethod) }
    var selectedPaymentMethods by remember { mutableStateOf<Set<String>>(initialMethods) }
    var selectedBanks by remember { mutableStateOf<Map<String, String>>(initialBanks) }
    var bankListVersion by remember { mutableIntStateOf(0) }
    var bankDialogMethod by remember { mutableStateOf<String?>(null) }
    var newBankInput by remember { mutableStateOf("") }
    var bankToEdit by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editBankInput by remember { mutableStateOf("") }
    var bankToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var splitAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0

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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar"
                                    )
                                }
                                Text(
                                    text = "Editar Movimentação",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (onDeleteTransaction != null) {
                                IconButton(
                                    onClick = { showDeleteConfirmDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Excluir movimentação",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
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

                                    if (parsedAmount <= 0.0) {
                                        errorMessage = "Informe um valor válido"
                                        return@Button
                                    }

                                    if (selectedTag.isNullOrBlank()) {
                                        showMissingTagAlert = true
                                        return@Button
                                    }

                                    val chosenTag = selectedTag!!

                                    // Validação do meio de pagamento combinado caso mais de 1 seja selecionado
                                    if (selectedPaymentMethods.size > 1) {
                                        val splitSum = selectedPaymentMethods.sumOf { method ->
                                            splitAmounts[method]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                        }
                                        if (kotlin.math.abs(parsedAmount - splitSum) > 0.01) {
                                            errorMessage = "A soma entre as formas de pagamento está incorreta, verifique o valor total informado"
                                            return@Button
                                        }
                                    }

                                    val formattedMethodsList = selectedPaymentMethods.map { method ->
                                        val bank = selectedBanks[method]
                                        if (!bank.isNullOrBlank()) "$method ($bank)" else method
                                    }

                                    val chosenPaymentMethod = if (formattedMethodsList.isNotEmpty()) {
                                        formattedMethodsList.joinToString(" + ")
                                    } else null

                                    val updated = transaction.copy(
                                        description = rawDesc,
                                        amount = parsedAmount,
                                        type = selectedType,
                                        date = selectedDateMillis,
                                        tag = chosenTag,
                                        paymentMethod = chosenPaymentMethod
                                    )

                                    onConfirmEdit(updated)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Salvar Alterações", fontWeight = FontWeight.Bold)
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
                    Text(
                        text = "Altere as informações da movimentação abaixo:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                // Seletor Tipo: Entrada vs Saída
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedType = TransactionType.ENTRADA
                                selectedPaymentMethods = selectedPaymentMethods - setOf("Cartão de Crédito", "Cartão de Débito")
                                selectedBanks = selectedBanks - setOf("Cartão de Crédito", "Cartão de Débito")
                                splitAmounts = splitAmounts - setOf("Cartão de Crédito", "Cartão de Débito")
                            }
                            .then(
                                if (selectedType == TransactionType.ENTRADA) Modifier.border(2.dp, IncomeGreen, RoundedCornerShape(12.dp))
                                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedType == TransactionType.ENTRADA) IncomeGreen.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface
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
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (selectedType == TransactionType.ENTRADA) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Entrada",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedType == TransactionType.ENTRADA) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                               ),
                                color = if (selectedType == TransactionType.ENTRADA) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedType = TransactionType.SAIDA }
                            .then(
                                if (selectedType == TransactionType.SAIDA) Modifier.border(2.dp, ExpenseRed, RoundedCornerShape(12.dp))
                                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedType == TransactionType.SAIDA) ExpenseRed.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface
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
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (selectedType == TransactionType.SAIDA) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saída",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedType == TransactionType.SAIDA) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                ),
                                color = if (selectedType == TransactionType.SAIDA) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Campo Descrição
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        errorMessage = null
                    },
                    label = { Text("Descrição") },
                    placeholder = { Text("Ex: Mercado, Salário, Restaurante...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Campo Valor
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == ',' || it == '.' }) {
                            amountText = newValue
                            errorMessage = null
                        }
                    },
                    label = { Text("Valor") },
                    placeholder = { Text("0,00") },
                    prefix = { Text("R$ ", fontSize = 14.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Seletor de Data
                DatePickerField(
                    selectedDateMillis = selectedDateMillis,
                    onDateSelected = { selectedDateMillis = it },
                    label = "Data da movimentação"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Marcador / Categoria
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.dp,
                            color = if (selectedTag != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTagsExpanded = !isTagsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val selectedTagObj = tags.firstOrNull { it.name.equals(selectedTag, ignoreCase = true) }
                                val tagColor = selectedTagObj?.let { Formatters.parseHexColor(it.colorHex) } ?: MaterialTheme.colorScheme.primary
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(tagColor)
                                )
                                Text(
                                    text = if (selectedTag != null) "Marcador: $selectedTag" else "Selecionar Marcador *",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedTag != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (isTagsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isTagsExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tags.forEach { tag ->
                                    val isTagSelected = selectedTag.equals(tag.name, ignoreCase = true)
                                    val color = Formatters.parseHexColor(tag.colorHex)
                                    FilterChip(
                                        selected = isTagSelected,
                                        onClick = {
                                            selectedTag = tag.name
                                            isTagsExpanded = false
                                        },
                                        label = {
                                            Text(
                                                text = tag.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isTagSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                        }
                                    )
                                }
                                AssistChip(
                                    onClick = { onAddNewTag() },
                                    label = { Text("+ Novo Marcador", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Meios de Pagamento e Bancos (Para Saída e Entrada)
                Spacer(modifier = Modifier.height(14.dp))

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
                                        selectedPaymentMethods = emptySet()
                                        selectedBanks = emptyMap()
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
                                                selectedPaymentMethods = selectedPaymentMethods - option
                                                selectedBanks = selectedBanks - option
                                                splitAmounts = splitAmounts - option
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

                            // Opção de bancos para meios exceto dinheiro em espécie
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

                            // Caixa para informar valor em cada forma caso mais de 1 seja selecionada
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

                                        val currentSum = selectedPaymentMethods.sumOf { method ->
                                            splitAmounts[method]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                        }
                                        val difference = parsedAmount - currentSum
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
                                                    if (isSumMatching && parsedAmount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Valor total da movimentação:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = Formatters.formatCurrency(parsedAmount),
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
                                                    color = if (isSumMatching && parsedAmount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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

    // Alerta de marcador obrigatório
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
                        isTagsExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selecionar Marcador")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Diálogo de confirmação para excluir transação
    if (showDeleteConfirmDialog && onDeleteTransaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Excluir Movimentação",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text("Deseja realmente excluir esta movimentação? Ela será removida da lista atual, mas permanecerá registrada no Histórico do sistema.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteTransaction(transaction.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surface
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

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
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
