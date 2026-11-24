package com.example.minhasfinancas.data.preferences

import android.content.Context
import org.json.JSONArray

object BankPreferences {

    private const val PREFS_NAME = "banks_config"

    private fun getKey(context: Context, paymentMethod: String): String {
        val profileId = ProfilePreferences.getActiveProfile(context)?.id ?: "default"
        val normalizedMethod = paymentMethod.trim().lowercase()
        return "banks_${profileId}_$normalizedMethod"
    }

    fun getBanks(context: Context, paymentMethod: String): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(getKey(context, paymentMethod), null) ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i)?.trim()
                if (!item.isNullOrBlank() && !list.contains(item)) {
                    list.add(item)
                }
            }
        } catch (_: Exception) {
            // Retorna vazio em caso de erro de parsing
        }
        return list
    }

    fun getAllUsedBanks(context: Context): List<String> {
        val allMethods = listOf("Pix", "Cartão de Crédito", "Cartão de Débito")
        val allBanks = mutableListOf<String>()
        for (m in allMethods) {
            val list = getBanks(context, m)
            for (b in list) {
                if (!allBanks.any { it.equals(b, ignoreCase = true) }) {
                    allBanks.add(b)
                }
            }
        }
        return allBanks
    }

    val bankPaletteColors = listOf(
        "#0284C7", "#0D9488", "#2563EB", "#7C3AED", "#4F46E5",
        "#059669", "#D97706", "#EA580C", "#DC2626", "#DB2777"
    )

    fun getBankColorHex(bankName: String): String {
        val hash = Math.abs(bankName.trim().lowercase().hashCode())
        return bankPaletteColors[hash % bankPaletteColors.size]
    }

    fun extractBanks(paymentMethod: String?): List<String> {
        if (paymentMethod.isNullOrBlank()) return emptyList()
        val regex = Regex("""\(([^)]+)\)""")
        return regex.findAll(paymentMethod).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
    }

    fun transactionMatchesBank(paymentMethod: String?, bankName: String): Boolean {
        if (paymentMethod.isNullOrBlank() || bankName.isBlank()) return false
        val extracted = extractBanks(paymentMethod)
        if (extracted.any { it.equals(bankName.trim(), ignoreCase = true) }) return true
        return paymentMethod.contains(bankName.trim(), ignoreCase = true)
    }

    fun addBank(context: Context, paymentMethod: String, bankName: String): List<String> {
        val trimmed = bankName.trim()
        if (trimmed.isBlank()) return getBanks(context, paymentMethod)

        val current = getBanks(context, paymentMethod).toMutableList()
        if (!current.any { it.equals(trimmed, ignoreCase = true) }) {
            current.add(trimmed)
            saveBanks(context, paymentMethod, current)
        }

        return current
    }

    fun updateBankName(context: Context, paymentMethod: String, oldName: String, newName: String): List<String> {
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank() || trimmedOld.equals(trimmedNew, ignoreCase = true)) {
            return getBanks(context, paymentMethod)
        }

        val current = getBanks(context, paymentMethod).toMutableList()
        val index = current.indexOfFirst { it.equals(trimmedOld, ignoreCase = true) }
        if (index != -1) {
            current[index] = trimmedNew
            saveBanks(context, paymentMethod, current)
        }

        return current
    }

    fun reorderBanks(context: Context, paymentMethod: String, orderedBanks: List<String>): List<String> {
        saveBanks(context, paymentMethod, orderedBanks)
        return orderedBanks
    }

    fun removeBank(context: Context, paymentMethod: String, bankName: String): List<String> {
        val trimmed = bankName.trim()
        val current = getBanks(context, paymentMethod).toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        saveBanks(context, paymentMethod, current)
        return current
    }

    fun getAllBanksForProfile(context: Context, profileId: String): Map<String, List<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allMethods = listOf("Pix", "Cartão de Crédito", "Cartão de Débito")
        val map = mutableMapOf<String, List<String>>()
        for (m in allMethods) {
            val key = "banks_${profileId}_${m.trim().lowercase()}"
            val jsonStr = prefs.getString(key, null)
            if (!jsonStr.isNullOrBlank()) {
                val list = mutableListOf<String>()
                try {
                    val arr = JSONArray(jsonStr)
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i)?.trim()
                        if (!item.isNullOrBlank() && !list.contains(item)) {
                            list.add(item)
                        }
                    }
                } catch (_: Exception) {}
                if (list.isNotEmpty()) {
                    map[m] = list
                }
            }
        }
        return map
    }

    fun restoreBanksForProfile(context: Context, profileId: String, banksMap: Map<String, List<String>>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for ((m, list) in banksMap) {
            val key = "banks_${profileId}_${m.trim().lowercase()}"
            val arr = JSONArray()
            for (b in list) arr.put(b)
            editor.putString(key, arr.toString())
        }
        editor.apply()
    }

    private fun saveBanks(context: Context, paymentMethod: String, banks: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (b in banks) {
            jsonArray.put(b)
        }
        prefs.edit().putString(getKey(context, paymentMethod), jsonArray.toString()).apply()
    }
}
