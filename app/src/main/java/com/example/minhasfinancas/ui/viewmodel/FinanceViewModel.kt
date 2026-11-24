package com.example.minhasfinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minhasfinancas.data.model.AuditHistoryItem
import com.example.minhasfinancas.data.model.InstallmentPlan
import com.example.minhasfinancas.data.model.MonthlySummary
import com.example.minhasfinancas.data.model.RecurrenceFrequency
import com.example.minhasfinancas.data.model.RecurringPlan
import com.example.minhasfinancas.data.model.TagMarker
import com.example.minhasfinancas.data.model.Transaction
import com.example.minhasfinancas.data.model.TransactionType
import com.example.minhasfinancas.data.model.UserProfile
import com.example.minhasfinancas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import com.example.minhasfinancas.data.preferences.BankPreferences
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VisorTagSummary(
    val tagName: String,
    val colorHex: String,
    val income: Double,
    val expense: Double
)

data class VisorBankSummary(
    val bankName: String,
    val colorHex: String,
    val income: Double,
    val expense: Double
)

class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val allProfiles: StateFlow<List<UserProfile>> = repository.allProfiles
    val activeProfile: StateFlow<UserProfile?> = repository.activeProfile
    val auditHistory: StateFlow<List<AuditHistoryItem>> = repository.auditHistory
    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme

    val selectedYear: StateFlow<Int> = repository.selectedYear
    val selectedMonth: StateFlow<Int> = repository.selectedMonth
    val tags: StateFlow<List<TagMarker>> = repository.tags
    val recurringPlans: StateFlow<List<RecurringPlan>> = repository.recurringPlans
    val installmentPlans: StateFlow<List<InstallmentPlan>> = repository.installmentPlans
    val historicalSummaries: StateFlow<List<MonthlySummary>> = repository.historicalSummaries

    val visibleHomeTags: StateFlow<List<TagMarker>> = repository.tags.map { list ->
        list.filter { it.isVisibleOnHome }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    val currentMonthTransactions: StateFlow<List<Transaction>> = repository.currentMonthTransactions
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        currentMonthTransactions,
        _selectedTagFilter
    ) { transactions, filter ->
        if (filter.isNullOrBlank()) {
            transactions
        } else {
            transactions.filter { it.tag.equals(filter, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = currentMonthTransactions.map { list ->
        list.filter { it.type == TransactionType.ENTRADA }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = currentMonthTransactions.map { list ->
        list.filter { it.type == TransactionType.SAIDA }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val expensesByTag: StateFlow<Map<String, Double>> = currentMonthTransactions.map { list ->
        list.filter { it.type == TransactionType.SAIDA }
            .groupBy { it.tag }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val incomesByTag: StateFlow<Map<String, Double>> = currentMonthTransactions.map { list ->
        list.filter { it.type == TransactionType.ENTRADA }
            .groupBy { it.tag }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- ESTADO DO VISOR INDEPENDENTE DE MARCADORES ---
    private val _selectedVisorTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedVisorTags: StateFlow<Set<String>> = _selectedVisorTags.asStateFlow()

    private val _visorDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val visorDateRange: StateFlow<Pair<Long, Long>?> = _visorDateRange.asStateFlow()

    private val _customRangeTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    private val visorBaseTransactions: StateFlow<List<Transaction>> = combine(
        currentMonthTransactions,
        _customRangeTransactions,
        _visorDateRange
    ) { currentMonth, customRange, dateRange ->
        if (dateRange != null) customRange else currentMonth
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visorFilteredTransactions: StateFlow<List<Transaction>> = combine(
        visorBaseTransactions,
        _selectedVisorTags
    ) { transactions, selectedTags ->
        if (selectedTags.isEmpty()) {
            emptyList()
        } else {
            transactions.filter { selectedTags.contains(it.tag) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visorTotalIncome: StateFlow<Double> = visorFilteredTransactions.map { list ->
        list.filter { it.type == TransactionType.ENTRADA }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val visorTotalExpense: StateFlow<Double> = visorFilteredTransactions.map { list ->
        list.filter { it.type == TransactionType.SAIDA }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val visorBalance: StateFlow<Double> = combine(visorTotalIncome, visorTotalExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val visorBreakdown: StateFlow<List<VisorTagSummary>> = combine(
        visorFilteredTransactions,
        repository.tags
    ) { transactions, allTags ->
        val tagMap = allTags.associateBy { it.name }
        transactions.groupBy { it.tag }.map { (tagName, list) ->
            val colorHex = tagMap[tagName]?.colorHex ?: "#64748B"
            val inc = list.filter { it.type == TransactionType.ENTRADA }.sumOf { it.amount }
            val exp = list.filter { it.type == TransactionType.SAIDA }.sumOf { it.amount }
            VisorTagSummary(tagName, colorHex, inc, exp)
        }.sortedByDescending { it.income + it.expense }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ESTADO DO VISOR DE BANCOS ---
    val availableBanks: StateFlow<List<String>> = repository.availableBanks

    private val _selectedVisorBanks = MutableStateFlow<Set<String>>(emptySet())
    val selectedVisorBanks: StateFlow<Set<String>> = _selectedVisorBanks.asStateFlow()

    val visorBankFilteredTransactions: StateFlow<List<Transaction>> = combine(
        visorBaseTransactions,
        _selectedVisorBanks
    ) { transactions, selectedBanks ->
        if (selectedBanks.isEmpty()) {
            emptyList()
        } else {
            transactions.filter { transaction ->
                val banks = BankPreferences.extractBanks(transaction.paymentMethod)
                banks.any { selectedBanks.contains(it) } ||
                    selectedBanks.any { b -> transaction.paymentMethod?.contains(b, ignoreCase = true) == true }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visorBankTotalIncome: StateFlow<Double> = visorBankFilteredTransactions.map { list ->
        list.filter { it.type == TransactionType.ENTRADA }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val visorBankTotalExpense: StateFlow<Double> = visorBankFilteredTransactions.map { list ->
        list.filter { it.type == TransactionType.SAIDA }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val visorBankBalance: StateFlow<Double> = combine(visorBankTotalIncome, visorBankTotalExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val visorBankBreakdown: StateFlow<List<VisorBankSummary>> = combine(
        visorBankFilteredTransactions,
        availableBanks
    ) { transactions, banks ->
        val result = mutableListOf<VisorBankSummary>()
        for (b in banks) {
            val matching = transactions.filter { t ->
                BankPreferences.transactionMatchesBank(t.paymentMethod, b)
            }
            if (matching.isNotEmpty()) {
                val inc = matching.filter { it.type == TransactionType.ENTRADA }.sumOf { it.amount }
                val exp = matching.filter { it.type == TransactionType.SAIDA }.sumOf { it.amount }
                val colorHex = BankPreferences.getBankColorHex(b)
                result.add(VisorBankSummary(b, colorHex, inc, exp))
            }
        }
        result.sortedByDescending { it.income + it.expense }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.tags.collect { tagsList ->
                if (_selectedVisorTags.value.isEmpty()) {
                    val visibleNames = tagsList.filter { it.isVisibleOnHome }.map { it.name }.toSet()
                    if (visibleNames.isNotEmpty()) {
                        _selectedVisorTags.value = visibleNames
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.availableBanks.collect { banksList ->
                if (_selectedVisorBanks.value.isEmpty() && banksList.isNotEmpty()) {
                    _selectedVisorBanks.value = banksList.toSet()
                }
            }
        }
    }

    fun toggleVisorTag(tagName: String) {
        val current = _selectedVisorTags.value
        _selectedVisorTags.value = if (current.contains(tagName)) {
            current - tagName
        } else {
            current + tagName
        }
    }

    fun selectAllVisibleVisorTags() {
        val visibleNames = visibleHomeTags.value.map { it.name }.toSet()
        _selectedVisorTags.value = visibleNames
    }

    fun clearVisorTags() {
        _selectedVisorTags.value = emptySet()
    }

    fun toggleVisorBank(bankName: String) {
        val current = _selectedVisorBanks.value
        _selectedVisorBanks.value = if (current.contains(bankName)) {
            current - bankName
        } else {
            current + bankName
        }
    }

    fun selectAllVisorBanks() {
        _selectedVisorBanks.value = availableBanks.value.toSet()
    }

    fun clearVisorBanks() {
        _selectedVisorBanks.value = emptySet()
    }

    fun refreshBanks() {
        repository.loadAvailableBanks()
    }

    fun setVisorDateRange(startMs: Long, endMs: Long) {
        _visorDateRange.value = Pair(startMs, endMs)
        reloadCustomRangeTransactions()
    }

    fun clearVisorDateRange() {
        _visorDateRange.value = null
        _customRangeTransactions.value = emptyList()
    }

    private fun reloadCustomRangeTransactions() {
        val range = _visorDateRange.value
        if (range != null) {
            viewModelScope.launch {
                _customRangeTransactions.value = repository.getTransactionsByDateRange(range.first, range.second)
            }
        }
    }

    fun updateTagVisibility(id: Long, isVisible: Boolean) {
        viewModelScope.launch {
            repository.updateTagVisibility(id, isVisible)
        }
    }

    fun nextMonth() = repository.nextMonth()
    fun previousMonth() = repository.previousMonth()
    fun resetToCurrentMonth() = repository.resetToCurrentMonth()

    fun setTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    fun addTransaction(
        description: String,
        amount: Double,
        type: TransactionType,
        date: Long,
        tag: String,
        paymentMethod: String? = null,
        machineGrossAmount: Double? = null,
        machineFeeAmount: Double? = null,
        machineFeePercent: Double? = null
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                description = description,
                amount = amount,
                type = type,
                date = date,
                tag = tag,
                paymentMethod = paymentMethod,
                machineGrossAmount = machineGrossAmount,
                machineFeeAmount = machineFeeAmount,
                machineFeePercent = machineFeePercent
            )
            reloadCustomRangeTransactions()
        }
    }

    suspend fun getAllTransactions(): List<Transaction> = repository.getAllTransactions()

    suspend fun getTransactionsByDateRange(startMs: Long, endMs: Long): List<Transaction> =
        repository.getTransactionsByDateRange(startMs, endMs)

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            reloadCustomRangeTransactions()
        }
    }

    fun setMonth(year: Int, month: Int) = repository.changeMonth(year, month)

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            reloadCustomRangeTransactions()
        }
    }

    fun addCustomTag(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.addTag(name, colorHex)
        }
    }

    fun updateTag(id: Long, oldName: String, newName: String, newColorHex: String) {
        viewModelScope.launch {
            repository.updateTag(id, oldName, newName, newColorHex)
        }
    }

    fun deleteTag(id: Long, tagName: String) {
        viewModelScope.launch {
            if (_selectedTagFilter.value.equals(tagName, ignoreCase = true)) {
                _selectedTagFilter.value = null
            }
            repository.deleteTag(id, tagName)
        }
    }

    fun updateTagsOrder(orderedTags: List<TagMarker>) {
        viewModelScope.launch {
            repository.updateTagsOrder(orderedTags)
        }
    }

    fun addRecurringExpense(
        description: String,
        amount: Double,
        type: TransactionType,
        tag: String,
        frequency: RecurrenceFrequency,
        startTimestamp: Long
    ) {
        viewModelScope.launch {
            repository.addRecurringExpense(description, amount, type, tag, frequency, startTimestamp)
        }
    }

    fun deleteRecurringPlan(planId: Long) {
        viewModelScope.launch {
            repository.deleteRecurringPlan(planId)
        }
    }

    fun addInstallmentPlan(
        description: String,
        totalAmount: Double,
        installmentAmount: Double,
        numberOfInstallments: Int,
        tag: String,
        startTimestamp: Long,
        type: TransactionType = TransactionType.SAIDA,
        paymentMethod: String? = "Cartão de Crédito"
    ) {
        viewModelScope.launch {
            repository.addInstallmentPlan(
                description = description,
                totalAmount = totalAmount,
                installmentAmount = installmentAmount,
                numberOfInstallments = numberOfInstallments,
                tag = tag,
                startTimestamp = startTimestamp,
                type = type,
                paymentMethod = paymentMethod
            )
        }
    }

    fun deleteInstallmentPlan(planId: Long) {
        viewModelScope.launch {
            repository.deleteInstallmentPlan(planId)
        }
    }

    fun switchProfile(profile: UserProfile) {
        _selectedTagFilter.value = null
        repository.switchProfile(profile)
    }

    fun createProfile(name: String): UserProfile {
        _selectedTagFilter.value = null
        return repository.createProfile(name)
    }

    fun updateProfileName(id: String, newName: String) {
        repository.updateProfileName(id, newName)
    }

    fun deleteProfile(profileId: String) {
        _selectedTagFilter.value = null
        repository.deleteProfile(profileId)
    }

    fun toggleDarkTheme() {
        repository.toggleDarkTheme()
    }

    fun updateProfilePhoto(profileId: String, photoUri: String?) {
        repository.updateProfilePhoto(profileId, photoUri)
    }

    fun importProfile(jsonStr: String): UserProfile? {
        val imported = repository.importProfile(jsonStr)
        if (imported != null) {
            _selectedTagFilter.value = null
        }
        return imported
    }
}
