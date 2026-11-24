package com.example.minhasfinancas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.minhasfinancas.data.model.Transaction
import com.example.minhasfinancas.data.preferences.BankPreferences
import com.example.minhasfinancas.ui.theme.ExpenseRed
import com.example.minhasfinancas.ui.theme.IncomeGreen
import com.example.minhasfinancas.ui.util.DatePickerHelper
import com.example.minhasfinancas.ui.util.Formatters
import java.util.Calendar
import java.util.Locale

private enum class QuickPeriod(val label: String) {
    ESTE_MES("Este Mês"),
    MES_ANTERIOR("Mês Anterior"),
    ULTIMOS_30_DIAS("Últimos 30 dias"),
    ULTIMOS_90_DIAS("Últimos 90 dias"),
    ESTE_ANO("Este Ano"),
    TODAS("Todas as Vendas")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MachineFeesDialog(
    allTransactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Todas as transações com dados de maquininha
    val machineTransactions = remember(allTransactions) {
        allTransactions.filter { it.isMachineTransaction }
    }

    // Inicializa período com o mês atual
    val initialDates = remember {
        val calStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        Pair(calStart.timeInMillis, calEnd.timeInMillis)
    }

    var startDateMillis by remember { mutableStateOf(initialDates.first) }
    var endDateMillis by remember { mutableStateOf(initialDates.second) }
    var selectedQuickPeriod by remember { mutableStateOf<QuickPeriod?>(QuickPeriod.ESTE_MES) }
    var selectedBank by remember { mutableStateOf<String?>(null) } // null = Todos

    // Lista de bancos presentes nas transações de maquininha
    val availableBanks = remember(machineTransactions) {
        val set = mutableSetOf<String>()
        for (t in machineTransactions) {
            val banks = BankPreferences.extractBanks(t.paymentMethod)
            set.addAll(banks)
        }
        set.sorted()
    }

    // Filtragem das transações no período e banco selecionados
    val filteredTransactions = remember(machineTransactions, startDateMillis, endDateMillis, selectedBank) {
        machineTransactions.filter { t ->
            val inDate = t.date in startDateMillis..endDateMillis
            val inBank = if (selectedBank == null) true
            else BankPreferences.transactionMatchesBank(t.paymentMethod, selectedBank!!)
            inDate && inBank
        }.sortedByDescending { it.date }
    }

    // Cálculos de totais
    val totalGross = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.getEffectiveMachineGross() }
    }
    val totalNet = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.amount }
    }
    val totalFees = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.getEffectiveMachineFee() }
    }
    val effectiveRatePercent = remember(totalGross, totalFees) {
        if (totalGross > 0.0) (totalFees / totalGross) * 100.0 else 0.0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Barra Superior do Modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Taxas da Maquininha",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Juros e taxas cobradas no cartão",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Bloco 1: Filtros de Período Personalizáveis (com Calendário PT-BR)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Filtrar por Período Personalizado",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Botões Seletores: Data Inicial e Data Final
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Data Inicial
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
                                                DatePickerHelper.showDatePicker(
                                                    context = context,
                                                    initialYear = cal.get(Calendar.YEAR),
                                                    initialMonth = cal.get(Calendar.MONTH),
                                                    initialDay = cal.get(Calendar.DAY_OF_MONTH)
                                                ) { year, month, day ->
                                                    val newCal = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, year)
                                                        set(Calendar.MONTH, month)
                                                        set(Calendar.DAY_OF_MONTH, day)
                                                        set(Calendar.HOUR_OF_DAY, 0)
                                                        set(Calendar.MINUTE, 0)
                                                        set(Calendar.SECOND, 0)
                                                        set(Calendar.MILLISECOND, 0)
                                                    }
                                                    startDateMillis = newCal.timeInMillis
                                                    if (startDateMillis > endDateMillis) {
                                                        endDateMillis = newCal.apply {
                                                            set(Calendar.HOUR_OF_DAY, 23)
                                                            set(Calendar.MINUTE, 59)
                                                            set(Calendar.SECOND, 59)
                                                            set(Calendar.MILLISECOND, 999)
                                                        }.timeInMillis
                                                    }
                                                    selectedQuickPeriod = null
                                                }
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "Data Inicial",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = Formatters.formatDate(startDateMillis),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Data Final
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val cal = Calendar.getInstance().apply { timeInMillis = endDateMillis }
                                                DatePickerHelper.showDatePicker(
                                                    context = context,
                                                    initialYear = cal.get(Calendar.YEAR),
                                                    initialMonth = cal.get(Calendar.MONTH),
                                                    initialDay = cal.get(Calendar.DAY_OF_MONTH)
                                                ) { year, month, day ->
                                                    val newCal = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, year)
                                                        set(Calendar.MONTH, month)
                                                        set(Calendar.DAY_OF_MONTH, day)
                                                        set(Calendar.HOUR_OF_DAY, 23)
                                                        set(Calendar.MINUTE, 59)
                                                        set(Calendar.SECOND, 59)
                                                        set(Calendar.MILLISECOND, 999)
                                                    }
                                                    endDateMillis = newCal.timeInMillis
                                                    if (endDateMillis < startDateMillis) {
                                                        startDateMillis = newCal.apply {
                                                            set(Calendar.HOUR_OF_DAY, 0)
                                                            set(Calendar.MINUTE, 0)
                                                            set(Calendar.SECOND, 0)
                                                            set(Calendar.MILLISECOND, 0)
                                                        }.timeInMillis
                                                    }
                                                    selectedQuickPeriod = null
                                                }
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "Data Final",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = Formatters.formatDate(endDateMillis),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                // Chips de Períodos Rápidos
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(QuickPeriod.entries) { period ->
                                        val isSelected = selectedQuickPeriod == period
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedQuickPeriod = period
                                                when (period) {
                                                    QuickPeriod.ESTE_MES -> {
                                                        val calStart = Calendar.getInstance().apply {
                                                            set(Calendar.DAY_OF_MONTH, 1)
                                                            set(Calendar.HOUR_OF_DAY, 0)
                                                            set(Calendar.MINUTE, 0)
                                                            set(Calendar.SECOND, 0)
                                                            set(Calendar.MILLISECOND, 0)
                                                        }
                                                        val calEnd = Calendar.getInstance().apply {
                                                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                                            set(Calendar.HOUR_OF_DAY, 23)
                                                            set(Calendar.MINUTE, 59)
                                                            set(Calendar.SECOND, 59)
                                                            set(Calendar.MILLISECOND, 999)
                                                        }
                                                        startDateMillis = calStart.timeInMillis
                                                        endDateMillis = calEnd.timeInMillis
                                                    }
                                                    QuickPeriod.MES_ANTERIOR -> {
                                                        val calStart = Calendar.getInstance().apply {
                                                            add(Calendar.MONTH, -1)
                                                            set(Calendar.DAY_OF_MONTH, 1)
                                                            set(Calendar.HOUR_OF_DAY, 0)
                                                            set(Calendar.MINUTE, 0)
                                                            set(Calendar.SECOND, 0)
                                                            set(Calendar.MILLISECOND, 0)
                                                        }
                                                        val calEnd = Calendar.getInstance().apply {
                                                            add(Calendar.MONTH, -1)
                                                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                                            set(Calendar.HOUR_OF_DAY, 23)
                                                            set(Calendar.MINUTE, 59)
                                                            set(Calendar.SECOND, 59)
                                                            set(Calendar.MILLISECOND, 999)
                                                        }
                                                        startDateMillis = calStart.timeInMillis
                                                        endDateMillis = calEnd.timeInMillis
                                                    }
                                                    QuickPeriod.ULTIMOS_30_DIAS -> {
                                                        val calEnd = Calendar.getInstance()
                                                        val calStart = Calendar.getInstance().apply {
                                                            add(Calendar.DAY_OF_YEAR, -30)
                                                            set(Calendar.HOUR_OF_DAY, 0)
                                                            set(Calendar.MINUTE, 0)
                                                            set(Calendar.SECOND, 0)
                                                            set(Calendar.MILLISECOND, 0)
                                                        }
                                                        startDateMillis = calStart.timeInMillis
                                                        endDateMillis = calEnd.timeInMillis
                                                    }
                                                    QuickPeriod.ULTIMOS_90_DIAS -> {
                                                        val calEnd = Calendar.getInstance()
                                                        val calStart = Calendar.getInstance().apply {
                                                            add(Calendar.DAY_OF_YEAR, -90)
                                                            set(Calendar.HOUR_OF_DAY, 0)
                                                            set(Calendar.MINUTE, 0)
                                                            set(Calendar.SECOND, 0)
                                                            set(Calendar.MILLISECOND, 0)
                                                        }
                                                        startDateMillis = calStart.timeInMillis
                                                        endDateMillis = calEnd.timeInMillis
                                                    }
                                                    QuickPeriod.ESTE_ANO -> {
                                                        val calStart = Calendar.getInstance().apply {
                                                            set(Calendar.MONTH, Calendar.JANUARY)
                                                            set(Calendar.DAY_OF_MONTH, 1)
                                                            set(Calendar.HOUR_OF_DAY, 0)
                                                            set(Calendar.MINUTE, 0)
                                                            set(Calendar.SECOND, 0)
                                                            set(Calendar.MILLISECOND, 0)
                                                        }
                                                        val calEnd = Calendar.getInstance().apply {
                                                            set(Calendar.MONTH, Calendar.DECEMBER)
                                                            set(Calendar.DAY_OF_MONTH, 31)
                                                            set(Calendar.HOUR_OF_DAY, 23)
                                                            set(Calendar.MINUTE, 59)
                                                            set(Calendar.SECOND, 59)
                                                            set(Calendar.MILLISECOND, 999)
                                                        }
                                                        startDateMillis = calStart.timeInMillis
                                                        endDateMillis = calEnd.timeInMillis
                                                    }
                                                    QuickPeriod.TODAS -> {
                                                        startDateMillis = 0L
                                                        endDateMillis = Long.MAX_VALUE
                                                    }
                                                }
                                            },
                                            label = { Text(period.label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }

                                // Filtro opcional por Banco
                                if (availableBanks.isNotEmpty()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Filtrar por Banco:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = selectedBank == null,
                                                onClick = { selectedBank = null },
                                                label = { Text("Todos", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                            )
                                        }
                                        items(availableBanks) { bank ->
                                            val isBankSelected = selectedBank == bank
                                            val bankColor = Color(android.graphics.Color.parseColor(BankPreferences.getBankColorHex(bank)))
                                            FilterChip(
                                                selected = isBankSelected,
                                                onClick = { selectedBank = if (isBankSelected) null else bank },
                                                label = { Text(bank, fontSize = 11.sp) },
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(bankColor)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bloco 2: Painel de Indicadores de Taxas (Visualização em % e em R$)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Resumo de Taxas no Período",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Destaque: Total em Taxas (R$) e Taxa Média (%)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Total Pago em Taxas (R$)
                                    Card(
                                        modifier = Modifier.weight(1.1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = ExpenseRed.copy(alpha = 0.10f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Total em Taxas (R$)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = ExpenseRed
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = Formatters.formatCurrency(totalFees),
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = ExpenseRed
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Pago à maquininha",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Taxa Média Efetiva (%)
                                    Card(
                                        modifier = Modifier.weight(0.9f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF97316).copy(alpha = 0.12f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Taxa Média (%)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color(0xFFEA580C)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", effectiveRatePercent)}%",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color(0xFFEA580C)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Sobre o bruto",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Indicadores Complementares: Valor Cobrado (Bruto) e Valor Recebido (Líquido)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Valor Cobrado (Bruto)
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Total Cobrado (Bruto)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = Formatters.formatCurrency(totalGross),
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Valor Recebido (Líquido)
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = IncomeGreen.copy(alpha = 0.10f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Recebido em Conta (Líq)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = IncomeGreen
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = Formatters.formatCurrency(totalNet),
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = IncomeGreen
                                            )
                                        }
                                    }
                                }

                                // Barra Visual Comparativa: Proporção Líquido vs Taxa
                                if (totalGross > 0.0) {
                                    val netPercent = ((totalNet / totalGross) * 100.0).coerceIn(0.0, 100.0)
                                    val feePercent = ((totalFees / totalGross) * 100.0).coerceIn(0.0, 100.0)

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Recebido: ${String.format(Locale.US, "%.1f", netPercent)}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = IncomeGreen
                                            )
                                            Text(
                                                text = "Taxas: ${String.format(Locale.US, "%.1f", feePercent)}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = ExpenseRed
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(ExpenseRed.copy(alpha = 0.4f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .weight((netPercent / 100.0).toFloat().coerceAtLeast(0.01f))
                                                    .background(IncomeGreen)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .weight((feePercent / 100.0).toFloat().coerceAtLeast(0.01f))
                                                    .background(ExpenseRed)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bloco 3: Lista de Transações da Maquininha no Período
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vendas na Maquininha (${filteredTransactions.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (filteredTransactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(42.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Nenhuma venda com maquininha encontrada",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ajuste os filtros de data ou registre uma nova receita com 'Recebimento na maquininha'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredTransactions, key = { it.id }) { trans ->
                            val transGross = trans.getEffectiveMachineGross()
                            val transNet = trans.amount
                            val transFee = trans.getEffectiveMachineFee()
                            val transRate = trans.getEffectiveMachineFeePercent()

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Topo do card: Descrição e Data
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = trans.description,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = Formatters.formatDate(trans.date),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Tags e Meio de pagamento / Banco
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Tag Marcador
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = trans.tag,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Meio de pagamento / Banco
                                        if (!trans.paymentMethod.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = trans.paymentMethod,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )

                                    // Linha de Valores: Cobrado, Recebido, Taxa (R$) e Taxa (%)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Cobrado: ${Formatters.formatCurrency(transGross)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Recebido: ${Formatters.formatCurrency(transNet)}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = IncomeGreen
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Taxa:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ExpenseRed
                                                )
                                                Text(
                                                    text = "- ${Formatters.formatCurrency(transFee)}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = ExpenseRed
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(ExpenseRed.copy(alpha = 0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.2f", transRate)}%",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = ExpenseRed,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
