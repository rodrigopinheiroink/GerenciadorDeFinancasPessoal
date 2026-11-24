package com.example.minhasfinancas.data.preferences

import android.content.Context
import android.content.SharedPreferences

object MachineFeePreferences {

    private const val PREFS_NAME = "machine_fees_prefs"
    private const val KEY_DEBIT_FEE = "debit_fee"
    private const val KEY_CREDIT_CASH_FEE = "credit_cash_fee"
    private const val KEY_CREDIT_INSTALLMENT_FEE = "credit_installment_fee"
    private const val KEY_ADVANCE_FEE = "advance_fee"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveDebitFee(context: Context, fee: String) {
        getPrefs(context).edit().putString(KEY_DEBIT_FEE, fee).apply()
    }

    fun getDebitFee(context: Context): String {
        return getPrefs(context).getString(KEY_DEBIT_FEE, "") ?: ""
    }

    fun saveCreditCashFee(context: Context, fee: String) {
        getPrefs(context).edit().putString(KEY_CREDIT_CASH_FEE, fee).apply()
    }

    fun getCreditCashFee(context: Context): String {
        return getPrefs(context).getString(KEY_CREDIT_CASH_FEE, "") ?: ""
    }

    fun saveCreditInstallmentFee(context: Context, fee: String) {
        getPrefs(context).edit().putString(KEY_CREDIT_INSTALLMENT_FEE, fee).apply()
    }

    fun getCreditInstallmentFee(context: Context): String {
        return getPrefs(context).getString(KEY_CREDIT_INSTALLMENT_FEE, "") ?: ""
    }

    fun saveAdvanceFee(context: Context, fee: String) {
        getPrefs(context).edit().putString(KEY_ADVANCE_FEE, fee).apply()
    }

    fun getAdvanceFee(context: Context): String {
        return getPrefs(context).getString(KEY_ADVANCE_FEE, "") ?: ""
    }
}
