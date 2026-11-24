package com.example.minhasfinancas.data.repository

import android.content.Context
import com.example.minhasfinancas.data.database.AppDatabaseHelper
import com.example.minhasfinancas.data.model.AuditHistoryItem
import com.example.minhasfinancas.data.model.InstallmentPlan
import com.example.minhasfinancas.data.model.MonthlySummary
import com.example.minhasfinancas.data.model.RecurrenceFrequency
import com.example.minhasfinancas.data.model.RecurringPlan
import com.example.minhasfinancas.data.model.TagMarker
import com.example.minhasfinancas.data.model.Transaction
import com.example.minhasfinancas.data.model.TransactionType
import com.example.minhasfinancas.data.model.UserProfile
import com.example.minhasfinancas.data.backup.ProfileBackupManager
import com.example.minhasfinancas.data.preferences.BankPreferences
import com.example.minhasfinancas.data.preferences.ProfilePreferences
import com.example.minhasfinancas.data.preferences.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinanceRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _allProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val allProfiles: StateFlow<List<UserProfile>> = _allProfiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<UserProfile?>(null)
    val activeProfile: StateFlow<UserProfile?> = _activeProfile.asStateFlow()

    private var dbHelper: AppDatabaseHelper

    private val currentCal = Calendar.getInstance()

    private val _selectedYear = MutableStateFlow(currentCal.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(currentCal.get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _currentMonthTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val currentMonthTransactions: StateFlow<List<Transaction>> = _currentMonthTransactions.asStateFlow()

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val allTransactions: StateFlow<List<Transaction>> = _allTransactions.asStateFlow()

    private val _tags = MutableStateFlow<List<TagMarker>>(emptyList())
    val tags: StateFlow<List<TagMarker>> = _tags.asStateFlow()

    private val _recurringPlans = MutableStateFlow<List<RecurringPlan>>(emptyList())
    val recurringPlans: StateFlow<List<RecurringPlan>> = _recurringPlans.asStateFlow()

    private val _installmentPlans = MutableStateFlow<List<InstallmentPlan>>(emptyList())
    val installmentPlans: StateFlow<List<InstallmentPlan>> = _installmentPlans.asStateFlow()

    private val _historicalSummaries = MutableStateFlow<List<MonthlySummary>>(emptyList())
    val historicalSummaries: StateFlow<List<MonthlySummary>> = _historicalSummaries.asStateFlow()

    private val _auditHistory = MutableStateFlow<List<AuditHistoryItem>>(emptyList())
    val auditHistory: StateFlow<List<AuditHistoryItem>> = _auditHistory.asStateFlow()

    private val _availableBanks = MutableStateFlow<List<String>>(emptyList())
    val availableBanks: StateFlow<List<String>> = _availableBanks.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(ThemePreferences.isDarkMode(context))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleDarkTheme(): Boolean {
        val newMode = !_isDarkTheme.value
        ThemePreferences.setDarkMode(context, newMode)
        _isDarkTheme.value = newMode
        return newMode
    }

    init {
        val profiles = ProfilePreferences.getAllProfiles(context)
        _allProfiles.value = profiles
        val current = ProfilePreferences.getActiveProfile(context)
        _activeProfile.value = current

        val dbName = if (current != null) "minhas_financas_profile_${current.id}.db" else AppDatabaseHelper.DATABASE_NAME
        dbHelper = AppDatabaseHelper(context, dbName)

        refreshAll()
    }

    fun switchProfile(profile: UserProfile) {
        ProfilePreferences.setActiveProfileId(context, profile.id)
        _activeProfile.value = profile
        _allProfiles.value = ProfilePreferences.getAllProfiles(context)
        try {
            dbHelper.close()
        } catch (_: Exception) {}
        dbHelper = AppDatabaseHelper(context, "minhas_financas_profile_${profile.id}.db")
        refreshAll()
    }

    fun createProfile(name: String): UserProfile {
        val newProfile = ProfilePreferences.createProfile(context, name)
        switchProfile(newProfile)
        return newProfile
    }

    fun updateProfileName(id: String, newName: String) {
        ProfilePreferences.updateProfileName(context, id, newName)
        _allProfiles.value = ProfilePreferences.getAllProfiles(context)
        if (_activeProfile.value?.id == id) {
            _activeProfile.value = _activeProfile.value?.copy(name = newName.trim().ifBlank { _activeProfile.value!!.name })
        }
    }

    fun deleteProfile(profileId: String) {
        val wasActive = _activeProfile.value?.id == profileId

        // Se o banco que está aberto é deste perfil, fecha antes de excluir
        if (wasActive) {
            try {
                dbHelper.close()
            } catch (_: Exception) {}
        }

        // Exclui o arquivo do banco de dados local do perfil
        val dbFileName = "minhas_financas_profile_${profileId}.db"
        try {
            context.deleteDatabase(dbFileName)
        } catch (_: Exception) {}

        // Atualiza a lista de perfis salvos
        val nextActive = ProfilePreferences.deleteProfile(context, profileId)
        _allProfiles.value = ProfilePreferences.getAllProfiles(context)
        _activeProfile.value = nextActive

        if (nextActive != null) {
            dbHelper = AppDatabaseHelper(context, "minhas_financas_profile_${nextActive.id}.db")
            refreshAll()
        } else {
            // Se nenhum perfil sobrou, fecha ou reseta
            dbHelper = AppDatabaseHelper(context, AppDatabaseHelper.DATABASE_NAME)
            _currentMonthTransactions.value = emptyList()
            _tags.value = emptyList()
            _recurringPlans.value = emptyList()
            _installmentPlans.value = emptyList()
            _historicalSummaries.value = emptyList()
            _auditHistory.value = emptyList()
        }
    }

    fun updateProfilePhoto(id: String, photoUri: String?) {
        ProfilePreferences.updateProfilePhoto(context, id, photoUri)
        _allProfiles.value = ProfilePreferences.getAllProfiles(context)
        if (_activeProfile.value?.id == id) {
            _activeProfile.value = _activeProfile.value?.copy(photoUri = photoUri)
        }
    }

    fun importProfile(jsonStr: String): UserProfile? {
        return try {
            val imported = ProfileBackupManager.importProfileFromJson(context, jsonStr)
            _allProfiles.value = ProfilePreferences.getAllProfiles(context)
            switchProfile(imported)
            imported
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun refreshAll() {
        scope.launch(Dispatchers.IO) {
            try {
                loadTags()
                loadCurrentMonthTransactions()
                loadRecurringPlans()
                loadInstallmentPlans()
                loadHistoricalSummaries()
                loadAuditHistory()
                loadAvailableBanks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAvailableBanks() {
        try {
            val fromPrefs = BankPreferences.getAllUsedBanks(context).toMutableList()
            val transactions = dbHelper.getAllTransactions()
            for (t in transactions) {
                val banks = BankPreferences.extractBanks(t.paymentMethod)
                for (b in banks) {
                    if (!fromPrefs.any { it.equals(b, ignoreCase = true) }) {
                        fromPrefs.add(b)
                    }
                }
            }
            _availableBanks.value = fromPrefs
        } catch (_: Exception) {}
    }

    private suspend fun loadTags() {
        try {
            // Limpa eventuais marcadores de bancos que foram sincronizados na alteração anterior
            val cleanupPrefs = context.getSharedPreferences("banks_tag_cleanup_v2", Context.MODE_PRIVATE)
            if (!cleanupPrefs.getBoolean("cleaned_synced_bank_tags_v2", false)) {
                try {
                    val allBanks = com.example.minhasfinancas.data.preferences.BankPreferences.getAllUsedBanks(context)
                    val currentTags = dbHelper.getAllTags()
                    for (b in allBanks) {
                        val matching = currentTags.firstOrNull { it.name.equals(b, ignoreCase = true) && !it.isDefault }
                        if (matching != null) {
                            dbHelper.deleteTag(matching.id, matching.name)
                        }
                    }
                    cleanupPrefs.edit().putBoolean("cleaned_synced_bank_tags_v2", true).apply()
                } catch (_: Exception) {}
            }

            val list = dbHelper.getAllTags()
            _tags.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadCurrentMonthTransactions() {
        try {
            val list = dbHelper.getTransactionsByMonth(_selectedYear.value, _selectedMonth.value)
            _currentMonthTransactions.value = list
            _allTransactions.value = dbHelper.getAllTransactions()
            loadAvailableBanks()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadRecurringPlans() {
        try {
            val list = dbHelper.getAllRecurringPlans()
            _recurringPlans.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadInstallmentPlans() {
        try {
            val list = dbHelper.getAllInstallmentPlans()
            _installmentPlans.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadHistoricalSummaries() {
        try {
            // Carrega os últimos 6 meses até o mês selecionado
            val summaries = mutableListOf<MonthlySummary>()
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, _selectedYear.value)
                set(Calendar.MONTH, _selectedMonth.value)
                // Retrocede 5 meses para ter 6 meses no total
                add(Calendar.MONTH, -5)
            }

            for (i in 0 until 6) {
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH)
                val transactions = dbHelper.getTransactionsByMonth(y, m)
                val income = transactions.filter { it.type == TransactionType.ENTRADA }.sumOf { it.amount }
                val expense = transactions.filter { it.type == TransactionType.SAIDA }.sumOf { it.amount }
                summaries.add(MonthlySummary(year = y, month = m, totalIncome = income, totalExpense = expense))
                cal.add(Calendar.MONTH, 1)
            }
            _historicalSummaries.value = summaries
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadAuditHistory() {
        try {
            val list = dbHelper.getAllAuditHistory()
            _auditHistory.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun changeMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        scope.launch(Dispatchers.IO) {
            loadCurrentMonthTransactions()
            loadHistoricalSummaries()
        }
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value)
            add(Calendar.MONTH, 1)
        }
        changeMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun previousMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value)
            add(Calendar.MONTH, -1)
        }
        changeMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun resetToCurrentMonth() {
        val cal = Calendar.getInstance()
        changeMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    suspend fun addTransaction(
        description: String,
        amount: Double,
        type: TransactionType,
        date: Long,
        tag: String,
        paymentMethod: String? = null,
        machineGrossAmount: Double? = null,
        machineFeeAmount: Double? = null,
        machineFeePercent: Double? = null
    ): Long = withContext(Dispatchers.IO) {
        val t = Transaction(
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
        val id = dbHelper.insertTransaction(t)

        // Sincroniza visualização para o mês/ano da data selecionada
        val transCal = Calendar.getInstance().apply { timeInMillis = date }
        _selectedYear.value = transCal.get(Calendar.YEAR)
        _selectedMonth.value = transCal.get(Calendar.MONTH)

        loadCurrentMonthTransactions()
        loadHistoricalSummaries()
        loadAuditHistory()
        id
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.deleteTransaction(id)
        loadCurrentMonthTransactions()
        loadHistoricalSummaries()
        loadAuditHistory()
    }

    suspend fun updateTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        dbHelper.updateTransaction(transaction)

        // Sincroniza visualização para o mês/ano da transação
        val transCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
        _selectedYear.value = transCal.get(Calendar.YEAR)
        _selectedMonth.value = transCal.get(Calendar.MONTH)

        loadCurrentMonthTransactions()
        loadHistoricalSummaries()
        loadAuditHistory()
    }

    suspend fun addTag(name: String, colorHex: String): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertTag(name, colorHex)
        loadTags()
        id
    }

    suspend fun updateTag(id: Long, oldName: String, newName: String, newColorHex: String): Boolean = withContext(Dispatchers.IO) {
        val result = dbHelper.updateTag(id, oldName, newName, newColorHex)
        loadTags()
        loadCurrentMonthTransactions()
        loadHistoricalSummaries()
        result
    }

    suspend fun deleteTag(id: Long, tagName: String): Boolean = withContext(Dispatchers.IO) {
        val result = dbHelper.deleteTag(id, tagName)
        loadTags()
        loadCurrentMonthTransactions()
        loadHistoricalSummaries()
        result
    }

    suspend fun updateTagsOrder(orderedTags: List<TagMarker>) = withContext(Dispatchers.IO) {
        dbHelper.updateTagsOrder(orderedTags)
        loadTags()
    }

    suspend fun updateTagVisibility(id: Long, isVisible: Boolean): Boolean = withContext(Dispatchers.IO) {
        val result = dbHelper.updateTagVisibility(id, isVisible)
        loadTags()
        result
    }

    suspend fun getAllTransactions(): List<Transaction> = withContext(Dispatchers.IO) {
        dbHelper.getAllTransactions()
    }

    suspend fun getTransactionsByDateRange(startMs: Long, endMs: Long): List<Transaction> = withContext(Dispatchers.IO) {
        dbHelper.getTransactionsByDateRange(startMs, endMs)
    }

    suspend fun addRecurringExpense(
        description: String,
        amount: Double,
        type: TransactionType,
        tag: String,
        frequency: RecurrenceFrequency,
        startTimestamp: Long
    ): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertRecurringPlan(
            description = description,
            amount = amount,
            type = type,
            tag = tag,
            frequency = frequency,
            startTimestamp = startTimestamp
        )
        loadRecurringPlans()
        loadCurrentMonthTransactions()
        loadHistoricalSummaries()
        loadAuditHistory()
        id
    }

    suspend fun deleteRecurringPlan(planId: Long) = withContext(Dispatchers.IO) {
        dbHelper.deleteRecurringPlan(planId)
        loadRecurringPlans()
        loadCurrentMonthTransactions()
        loadHistoricalSummaries()
        loadAuditHistory()
    }

    suspend fun addInstallmentPlan(
        description: String,
        totalAmount: Double,
        installmentAmount: Double,
        numberOfInstallments: Int,
        tag: String,
        startTimestamp: Long,
        type: TransactionType = TransactionType.SAIDA,
        paymentMethod: String? = "Cartão de Crédito"
    ): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertInstallmentPlan(
            description = description,
            totalAmount = totalAmount,
            installmentAmount = installmentAmount,
            numberOfInstallments = numberOfInstallments,
            tag = tag,
            startTimestamp = startTimestamp,
            type = type,
            paymentMethod = paymentMethod
        )
        // Sincroniza visualização para o mês/ano da data inicial
        val transCal = Calendar.getInstance().apply { timeInMillis = startTimestamp }
        _selectedYear.value = transCal.get(Calendar.YEAR)
        _selectedMonth.value = transCal.get(Calendar.MONTH)

        loadInstallmentPlans()
        loadCurrentMonthTransactions()
        loadHistoricalSummaries()
        loadAuditHistory()
        id
    }

    suspend fun deleteInstallmentPlan(planId: Long) = withContext(Dispatchers.IO) {
        dbHelper.deleteInstallmentPlan(planId)
        loadInstallmentPlans()
        loadCurrentMonthTransactions()
        loadHistoricalSummaries()
        loadAuditHistory()
    }
}
