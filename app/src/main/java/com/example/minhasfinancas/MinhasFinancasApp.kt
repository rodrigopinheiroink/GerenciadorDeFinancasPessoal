package com.example.minhasfinancas

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class MinhasFinancasApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setAppLocale(this)
    }

    override fun attachBaseContext(base: Context) {
        val locale = LOCALE_PT_BR
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    companion object {
        val LOCALE_PT_BR: Locale = Locale.Builder().setLanguage("pt").setRegion("BR").build()

        fun setAppLocale(context: Context) {
            Locale.setDefault(LOCALE_PT_BR)
            val config = Configuration(context.resources.configuration)
            config.setLocale(LOCALE_PT_BR)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
