package com.example.minhasfinancas.ui.util

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

object DatePickerHelper {

    val PT_BR: Locale = Locale.Builder().setLanguage("pt").setRegion("BR").build()

    fun findActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun showDatePicker(
        context: Context,
        initialYear: Int,
        initialMonth: Int,
        initialDay: Int,
        minDateMillis: Long? = null,
        maxDateMillis: Long? = null,
        onDateSelected: (year: Int, month: Int, dayOfMonth: Int) -> Unit
    ): DatePickerDialog? {
        val activity = findActivity(context)
        if (activity != null && (activity.isFinishing || activity.isDestroyed)) {
            return null
        }
        val targetContext = activity ?: context

        Locale.setDefault(PT_BR)
        try {
            val res = targetContext.resources
            val config = Configuration(res.configuration)
            config.setLocale(PT_BR)
            config.setLayoutDirection(PT_BR)
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)

            val appRes = targetContext.applicationContext.resources
            val appConfig = Configuration(appRes.configuration)
            appConfig.setLocale(PT_BR)
            appConfig.setLayoutDirection(PT_BR)
            @Suppress("DEPRECATION")
            appRes.updateConfiguration(appConfig, appRes.displayMetrics)
        } catch (_: Exception) {
        }

        val dialog = DatePickerDialog(
            targetContext,
            { _, year, month, dayOfMonth ->
                onDateSelected(year, month, dayOfMonth)
            },
            initialYear,
            initialMonth,
            initialDay
        )

        minDateMillis?.let { dialog.datePicker.minDate = it }
        maxDateMillis?.let { dialog.datePicker.maxDate = it }

        dialog.setOnShowListener {
            dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.text = "OK"
            dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.text = "Cancelar"
        }

        try {
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return dialog
    }
}
