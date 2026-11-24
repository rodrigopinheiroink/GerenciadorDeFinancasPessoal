package com.example.minhasfinancas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.minhasfinancas.data.model.Transaction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Tune
import com.example.minhasfinancas.data.preferences.BankPreferences
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minhasfinancas.ui.components.ExpenseDonutChart
import com.example.minhasfinancas.ui.components.MonthlyComparisonBarChart
import com.example.minhasfinancas.ui.components.MonthSelector
import com.example.minhasfinancas.ui.components.SummaryCard
import com.example.minhasfinancas.ui.components.TransactionItem
import com.example.minhasfinancas.ui.theme.ExpenseRed
import com.example.minhasfinancas.ui.theme.IncomeGreen
import com.example.minhasfinancas.ui.util.DatePickerHelper
import com.example.minhasfinancas.ui.util.Formatters
import com.example.minhasfinancas.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: FinanceViewModel,
    onOpenMenu: () -> Unit = {},
    onManageProfiles: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val year by viewModel.selectedYear.collectAsState()
    val month by viewModel.selectedMonth.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val visibleTags by viewModel.visibleHomeTags.collectAsState()
    val summaries by viewModel.historicalSummaries.collectAsState()
    val expensesByTag by viewModel.expensesByTag.collectAsState()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val context = LocalContext.current

    // Estados do Visor de Marcadores
    val selectedVisorTags by viewModel.selectedVisorTags.collectAsState()
    val visorDateRange by viewModel.visorDateRange.collectAsState()
    val visorTotalIncome by viewModel.visorTotalIncome.collectAsState()
    val visorTotalExpense by viewModel.visorTotalExpense.collectAsState()
    val visorBalance by viewModel.visorBalance.collectAsState()
    val visorBreakdown by viewModel.visorBreakdown.collectAsState()

    // Estados do Visor de Bancos
    val availableBanks by viewModel.availableBanks.collectAsState()
    val selectedVisorBanks by viewModel.selectedVisorBanks.collectAsState()
    val visorBankTotalIncome by viewModel.visorBankTotalIncome.collectAsState()
    val visorBankTotalExpense by viewModel.visorBankTotalExpense.collectAsState()
    val visorBankBalance by viewModel.visorBankBalance.collectAsState()
    val visorBankBreakdown by viewModel.visorBankBreakdown.collectAsState()

    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0 = Filtrar por Marcador, 1 = Filtrar por Bancos
    var showMachineFeesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshBanks()
    }

    // Transações filtradas de acordo com a aba de filtro ativa (Marcador ou Banco)
    val displayedTransactions = remember(
        transactions,
        selectedFilterTab,
        selectedVisorTags,
        visibleTags,
        selectedVisorBanks,
        availableBanks
    ) {
        when (selectedFilterTab) {
            0 -> {
                val visibleNames = visibleTags.map { it.name }.toSet()
                val isAllSelected = visibleNames.isNotEmpty() && visibleNames.all { selectedVisorTags.contains(it) }
                if (isAllSelected || selectedVisorTags.isEmpty()) {
                    transactions
                } else {
                    transactions.filter { selectedVisorTags.contains(it.tag) }
                }
            }
            1 -> {
                if (selectedVisorBanks.isEmpty()) {
                    emptyList()
                } else {
                    transactions.filter { t ->
                        selectedVisorBanks.any { b -> BankPreferences.transactionMatchesBank(t.paymentMethod, b) }
                    }
                }
            }
            else -> transactions
        }
    }

    var showProfileDropdown by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showManageTagsDialog by remember { mutableStateOf(false) }
    var showVisorMarkersDialog by remember { mutableStateOf(false) }
    var showDateRangeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nova Movimentação", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Barra superior: Botão de menu à esquerda e Seletor/Alternador de perfis à direita
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onOpenMenu,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu de Opções",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Minhas Finanças",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Botão no canto superior direito: Alternador de Perfis
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                                .clickable {
                                    showProfileDropdown = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = activeProfile?.name ?: "Perfil",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Alternar perfil",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                        // Menu suspenso para alternar rapidamente entre os perfis criados
                        DropdownMenu(
                            expanded = showProfileDropdown,
                            onDismissRequest = { showProfileDropdown = false },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Alternar Perfil",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )

                            allProfiles.forEach { profile ->
                                val isCurrent = profile.id == activeProfile?.id
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            RadioButton(
                                                selected = isCurrent,
                                                onClick = null
                                            )
                                            Text(
                                                text = profile.name,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        showProfileDropdown = false
                                        if (!isCurrent) {
                                            viewModel.switchProfile(profile)
                                        }
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Gerenciar / Criar Perfis",
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showProfileDropdown = false
                                    onManageProfiles()
                                }
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Seletor de Mês
                MonthSelector(
                    year = year,
                    month = month,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onOpenDatePicker = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        DatePickerHelper.showDatePicker(
                            context = context,
                            initialYear = cal.get(Calendar.YEAR),
                            initialMonth = cal.get(Calendar.MONTH),
                            initialDay = cal.get(Calendar.DAY_OF_MONTH)
                        ) { selectedYear, selectedMonth, _ ->
                            viewModel.setMonth(selectedYear, selectedMonth)
                        }
                    }
                )
            }

            item {
                // Card de Resumo (Entradas e Saídas)
                SummaryCard(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense
                )
            }

            item {
                // Gráfico Principal: Entradas vs Saídas por Mês
                MonthlyComparisonBarChart(summaries = summaries)
            }

            item {
                // Gráfico Secundário: Gastos por Marcador
                ExpenseDonutChart(expensesByTag = expensesByTag, tags = tags)
            }

            item {
                // Card de Acesso à Janela de Taxas da Maquininha
                Card(
                    onClick = { showMachineFeesDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
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
                                    text = "Filtre por período personalizado (juros em % e R$)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Abrir janela de taxas",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                // Seletor de Abas de Filtro: Marcadores vs Bancos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isMarcador = selectedFilterTab == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isMarcador) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { selectedFilterTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sell,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isMarcador) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Filtrar por Marcador",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isMarcador) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isBanco = selectedFilterTab == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isBanco) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { selectedFilterTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isBanco) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Filtrar por Bancos",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isBanco) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (selectedFilterTab == 0) {
            item {
                // Visor Independente de Marcadores
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Barra Superior do Visor: Título e Botões de Ação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtrar por Marcador",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Botão de Filtro por Período Personalizado
                            TextButton(
                                onClick = { showDateRangeDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Filtrar período de datas",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (visorDateRange != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (visorDateRange != null) {
                                        "${Formatters.formatDate(visorDateRange!!.first)} - ${Formatters.formatDate(visorDateRange!!.second)}"
                                    } else {
                                        "Período"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (visorDateRange != null) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (visorDateRange != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Botão Gerenciar Marcadores do Visor (Apenas Visibilidade e Ordem)
                            TextButton(
                                onClick = { showVisorMarkersDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Gerenciar exibição de marcadores no visor",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Gerenciar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Lista de Marcadores visíveis na Home com botão "Todos"
                    if (visibleTags.isEmpty()) {
                        Text(
                            text = "Nenhum marcador configurado para exibição na Home. Toque em 'Gerenciar' para reexibir marcadores.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        val visibleTagNames = remember(visibleTags) { visibleTags.map { it.name }.toSet() }
                        val isAllSelected = visibleTagNames.isNotEmpty() && visibleTagNames.all { selectedVisorTags.contains(it) }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Botão "Todos" para selecionar todos os marcadores visíveis na Home
                            item(key = "visor-tag-todos") {
                                FilterChip(
                                    selected = isAllSelected,
                                    onClick = {
                                        if (isAllSelected) {
                                            viewModel.clearVisorTags()
                                        } else {
                                            viewModel.selectAllVisibleVisorTags()
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = "Todos",
                                            fontSize = 12.sp,
                                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }

                            items(visibleTags, key = { it.id }) { tag ->
                                val isSelected = selectedVisorTags.contains(tag.name)
                                val tagColor = Formatters.parseHexColor(tag.colorHex)

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.toggleVisorTag(tag.name)
                                    },
                                    label = {
                                        Text(
                                            text = tag.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(tagColor)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Visor de Dados Independente
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Status do Período no Visor
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
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (visorDateRange != null) {
                                            "Período: ${Formatters.formatDate(visorDateRange!!.first)} até ${Formatters.formatDate(visorDateRange!!.second)}"
                                        } else {
                                            "Período: ${Formatters.getMonthName(month)} (Mês Atual)"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (visorDateRange != null) {
                                    TextButton(
                                        onClick = { viewModel.clearVisorDateRange() },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Voltar ao mês atual",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "Mês atual",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            if (selectedVisorTags.isEmpty()) {
                                Text(
                                    text = "Nenhum marcador selecionado. Selecione um ou mais marcadores acima para visualizar as entradas e saídas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                // Cards de Entradas e Saídas do Visor
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Total Entradas dos Marcadores Selecionados
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                    contentDescription = null,
                                                    tint = IncomeGreen,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Entradas",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = Formatters.formatCurrency(visorTotalIncome),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = IncomeGreen
                                            )
                                        }
                                    }

                                    // Total Saídas dos Marcadores Selecionados
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                                    contentDescription = null,
                                                    tint = ExpenseRed,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Saídas",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = Formatters.formatCurrency(visorTotalExpense),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ExpenseRed
                                            )
                                        }
                                    }
                                }

                                // Linha de Saldo Líquido dos Marcadores
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Resultado (${selectedVisorTags.size} marcador(es)):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(visorBalance),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (visorBalance >= 0) IncomeGreen else ExpenseRed
                                    )
                                }

                                // Detalhamento por Marcador selecionado
                                if (visorBreakdown.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Valores por marcador:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        visorBreakdown.forEach { item ->
                                            val itemColor = Formatters.parseHexColor(item.colorHex)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 1.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(itemColor)
                                                    )
                                                    Text(
                                                        text = item.tagName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (item.income > 0) {
                                                        Text(
                                                            text = "+${Formatters.formatCurrency(item.income)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = IncomeGreen
                                                        )
                                                    }
                                                    if (item.expense > 0) {
                                                        Text(
                                                            text = "-${Formatters.formatCurrency(item.expense)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = ExpenseRed
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (visorTotalIncome == 0.0 && visorTotalExpense == 0.0) {
                                    Text(
                                        text = "Nenhuma movimentação registrada para o(s) marcador(es) selecionado(s) neste período.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            } else {
                item {
                    // Visor Independente de Bancos
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Barra Superior do Visor: Título e Botão de Filtro por Período
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtrar por Bancos",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Botão de Filtro por Período Personalizado
                            TextButton(
                                onClick = { showDateRangeDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Filtrar período de datas",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (visorDateRange != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (visorDateRange != null) {
                                        "${Formatters.formatDate(visorDateRange!!.first)} - ${Formatters.formatDate(visorDateRange!!.second)}"
                                    } else {
                                        "Período"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (visorDateRange != null) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (visorDateRange != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Lista de Bancos com botão "Todos"
                        if (availableBanks.isEmpty()) {
                            Text(
                                text = "Nenhum banco cadastrado ainda. Crie bancos ao adicionar movimentações em 'Mais detalhes sobre a transação'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            val isAllBanksSelected = availableBanks.isNotEmpty() && availableBanks.all { selectedVisorBanks.contains(it) }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Botão "Todos" para selecionar todos os bancos
                                item(key = "visor-bank-todos") {
                                    FilterChip(
                                        selected = isAllBanksSelected,
                                        onClick = {
                                            if (isAllBanksSelected) {
                                                viewModel.clearVisorBanks()
                                            } else {
                                                viewModel.selectAllVisorBanks()
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = "Todos",
                                                fontSize = 12.sp,
                                                fontWeight = if (isAllBanksSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }

                                items(availableBanks, key = { it }) { bankName ->
                                    val isSelected = selectedVisorBanks.contains(bankName)
                                    val bankColor = Formatters.parseHexColor(BankPreferences.getBankColorHex(bankName))

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.toggleVisorBank(bankName)
                                        },
                                        label = {
                                            Text(
                                                text = bankName,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(bankColor)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Visor de Dados do Banco
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Status do Período no Visor
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
                                            imageVector = Icons.Default.Equalizer,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (visorDateRange != null) {
                                                "Período: ${Formatters.formatDate(visorDateRange!!.first)} até ${Formatters.formatDate(visorDateRange!!.second)}"
                                            } else {
                                                "Período: ${Formatters.getMonthName(month)} (Mês Atual)"
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (visorDateRange != null) {
                                        TextButton(
                                            onClick = { viewModel.clearVisorDateRange() },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Voltar ao mês atual",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Mês atual",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                if (selectedVisorBanks.isEmpty()) {
                                    Text(
                                        text = "Nenhum banco selecionado. Selecione um ou mais bancos acima para visualizar as entradas e saídas.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    // Cards de Entradas e Saídas do Visor de Bancos
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Total Entradas
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                        contentDescription = null,
                                                        tint = IncomeGreen,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Entradas",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = Formatters.formatCurrency(visorBankTotalIncome),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = IncomeGreen
                                                )
                                            }
                                        }

                                        // Total Saídas
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                                        contentDescription = null,
                                                        tint = ExpenseRed,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Saídas",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = Formatters.formatCurrency(visorBankTotalExpense),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = ExpenseRed
                                                )
                                            }
                                        }
                                    }

                                    // Linha de Saldo Líquido dos Bancos
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Resultado (${selectedVisorBanks.size} banco(s)):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = Formatters.formatCurrency(visorBankBalance),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (visorBankBalance >= 0) IncomeGreen else ExpenseRed
                                        )
                                    }

                                    // Detalhamento por Banco selecionado
                                    if (visorBankBreakdown.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Valores por banco:",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            visorBankBreakdown.forEach { item ->
                                                val itemColor = Formatters.parseHexColor(item.colorHex)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 1.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(itemColor)
                                                        )
                                                        Text(
                                                            text = item.bankName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        if (item.income > 0) {
                                                            Text(
                                                                text = "+${Formatters.formatCurrency(item.income)}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = IncomeGreen
                                                            )
                                                        }
                                                        if (item.expense > 0) {
                                                            Text(
                                                                text = "-${Formatters.formatCurrency(item.expense)}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = ExpenseRed
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (visorBankTotalIncome == 0.0 && visorBankTotalExpense == 0.0) {
                                        Text(
                                            text = "Nenhuma movimentação vinculada ao(s) banco(s) selecionado(s) neste período.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Título da Lista
                val listTitle = when (selectedFilterTab) {
                    1 -> {
                        if (selectedVisorBanks.size == 1) {
                            "Transações de ${Formatters.getMonthName(month)} (${selectedVisorBanks.first()})"
                        } else {
                            "Transações de ${Formatters.getMonthName(month)} (Bancos)"
                        }
                    }
                    else -> "Transações de ${Formatters.getMonthName(month)}"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${displayedTransactions.size} item(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (displayedTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val emptyMessage = if (selectedFilterTab == 1 && selectedVisorBanks.isEmpty()) {
                                "Nenhum banco selecionado"
                            } else if (selectedFilterTab == 1) {
                                "Nenhuma movimentação registrada para o(s) banco(s) selecionado(s)"
                            } else {
                                "Nenhuma movimentação registrada"
                            }
                            Text(
                                text = emptyMessage,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toque em 'Nova Movimentação' para adicionar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(displayedTransactions, key = { it.id }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        tags = tags,
                        onDelete = { id -> viewModel.deleteTransaction(id) },
                        onClick = { transactionToEdit = transaction }
                    )
                }
            }
        }
    }

    // Modal de Adicionar Transação
    if (showAddDialog) {
        AddTransactionDialog(
            tags = tags,
            onDismiss = {
                showAddDialog = false
                viewModel.refreshBanks()
            },
            onConfirmNormal = { desc, amount, type, date, tag, payMethod, gross, fee, rate ->
                viewModel.setTagFilter(null)
                viewModel.addTransaction(
                    description = desc,
                    amount = amount,
                    type = type,
                    date = date,
                    tag = tag,
                    paymentMethod = payMethod,
                    machineGrossAmount = gross,
                    machineFeeAmount = fee,
                    machineFeePercent = rate
                )
            },
            onConfirmRecurring = { desc, amount, type, tag, freq, startMs ->
                viewModel.setTagFilter(null)
                viewModel.addRecurringExpense(
                    description = desc,
                    amount = amount,
                    type = type,
                    tag = tag,
                    frequency = freq,
                    startTimestamp = startMs
                )
            },
            onConfirmInstallment = { desc, total, parcel, num, tag, startMs, type, payMethod ->
                viewModel.setTagFilter(null)
                viewModel.addInstallmentPlan(
                    description = desc,
                    totalAmount = total,
                    installmentAmount = parcel,
                    numberOfInstallments = num,
                    tag = tag,
                    startTimestamp = startMs,
                    type = type,
                    paymentMethod = payMethod
                )
            },
            onAddNewTag = { showAddTagDialog = true },
            onManageTags = { showManageTagsDialog = true }
        )
    }

    // Modal de Editar Movimentação
    if (transactionToEdit != null) {
        EditTransactionDialog(
            transaction = transactionToEdit!!,
            tags = tags,
            onDismiss = {
                transactionToEdit = null
                viewModel.refreshBanks()
            },
            onConfirmEdit = { updated ->
                viewModel.updateTransaction(updated)
                transactionToEdit = null
            },
            onDeleteTransaction = { id ->
                viewModel.deleteTransaction(id)
                transactionToEdit = null
            },
            onAddNewTag = { showAddTagDialog = true }
        )
    }

    // Modal de Criar Novo Marcador
    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.addCustomTag(name, colorHex)
            }
        )
    }

    // Modal de Gerenciar Marcadores (Editar nome, alterar cor, excluir, reordenar)
    if (showManageTagsDialog) {
        ManageTagsDialog(
            tags = tags,
            onDismiss = { showManageTagsDialog = false },
            onAddNewTag = {
                showAddTagDialog = true
            },
            onUpdateTag = { id, oldName, newName, newColorHex ->
                viewModel.updateTag(id, oldName, newName, newColorHex)
            },
            onDeleteTag = { id, name ->
                viewModel.deleteTag(id, name)
            },
            onReorderTags = { reorderedList ->
                viewModel.updateTagsOrder(reorderedList)
            },
            onToggleVisibility = { id, isVisible ->
                viewModel.updateTagVisibility(id, isVisible)
            }
        )
    }

    // Modal exclusivo do Visor "Filtrar por Marcador" (apenas visibilidade e ordem de exibição)
    if (showVisorMarkersDialog) {
        ManageVisorMarkersDialog(
            tags = tags,
            onDismiss = { showVisorMarkersDialog = false },
            onToggleVisibility = { id, isVisible ->
                viewModel.updateTagVisibility(id, isVisible)
            },
            onReorderTags = { reorderedList ->
                viewModel.updateTagsOrder(reorderedList)
            }
        )
    }

    // Modal de Filtro por Período de Datas (Data Inicial e Data Final)
    if (showDateRangeDialog) {
        DateRangeFilterDialog(
            initialStartMs = visorDateRange?.first,
            initialEndMs = visorDateRange?.second,
            currentYear = year,
            currentMonth = month,
            onDismiss = { showDateRangeDialog = false },
            onApplyRange = { startMs, endMs ->
                viewModel.setVisorDateRange(startMs, endMs)
                showDateRangeDialog = false
            },
            onResetToMonth = {
                viewModel.clearVisorDateRange()
                showDateRangeDialog = false
            }
        )
    }

    // Modal / Janela de Taxas da Maquininha
    if (showMachineFeesDialog) {
        MachineFeesDialog(
            allTransactions = allTransactions,
            onDismiss = { showMachineFeesDialog = false }
        )
    }
}

@Composable
fun DateRangeFilterDialog(
    initialStartMs: Long?,
    initialEndMs: Long?,
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onApplyRange: (startMs: Long, endMs: Long) -> Unit,
    onResetToMonth: () -> Unit
) {
    val context = LocalContext.current

    val defaultStart = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }

    val defaultEnd = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        cal.timeInMillis
    }

    var startDateMs by remember { mutableStateOf(initialStartMs ?: defaultStart) }
    var endDateMs by remember { mutableStateOf(initialEndMs ?: defaultEnd) }

    fun pickStartDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = startDateMs }
        DatePickerHelper.showDatePicker(
            context = context,
            initialYear = cal.get(Calendar.YEAR),
            initialMonth = cal.get(Calendar.MONTH),
            initialDay = cal.get(Calendar.DAY_OF_MONTH)
        ) { y, m, d ->
            val picked = Calendar.getInstance().apply {
                set(Calendar.YEAR, y)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            startDateMs = picked.timeInMillis
            if (startDateMs > endDateMs) {
                endDateMs = picked.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
            }
        }
    }

    fun pickEndDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = endDateMs }
        DatePickerHelper.showDatePicker(
            context = context,
            initialYear = cal.get(Calendar.YEAR),
            initialMonth = cal.get(Calendar.MONTH),
            initialDay = cal.get(Calendar.DAY_OF_MONTH)
        ) { y, m, d ->
            val picked = Calendar.getInstance().apply {
                set(Calendar.YEAR, y)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            endDateMs = picked.timeInMillis
            if (endDateMs < startDateMs) {
                startDateMs = picked.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Filtrar por Período",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Selecione a data inicial e a data final dentro de qualquer mês para visualizar os dados dos marcadores selecionados no visor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Campo Data Inicial
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickStartDate() },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Data Inicial",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatDate(startDateMs),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Selecionar data inicial",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Campo Data Final
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pickEndDate() },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Data Final",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatDate(endDateMs),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Selecionar data final",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (initialStartMs != null || initialEndMs != null) {
                    TextButton(
                        onClick = onResetToMonth,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Redefinir para o Mês Atual",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyRange(startDateMs, endDateMs) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Aplicar Filtro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
