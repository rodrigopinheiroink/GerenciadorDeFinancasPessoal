package com.example.minhasfinancas.ui.util

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    private val ptBrLocale = Locale.Builder().setLanguage("pt").setRegion("BR").build()
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBrLocale)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)

    fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    private val monthNames = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    private val shortMonthNames = listOf(
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    )

    fun getMonthName(month: Int): String {
        return if (month in 0..11) monthNames[month] else ""
    }

    fun getShortMonthName(month: Int): String {
        return if (month in 0..11) shortMonthNames[month] else ""
    }

    fun parseHexColor(hex: String, fallback: Color = Color(0xFF64748B)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorLong = if (cleanHex.length == 6) {
                ("FF$cleanHex").toLong(16)
            } else {
                cleanHex.toLong(16)
            }
            Color(colorLong)
        } catch (_: Exception) {
            fallback
        }
    }
}
