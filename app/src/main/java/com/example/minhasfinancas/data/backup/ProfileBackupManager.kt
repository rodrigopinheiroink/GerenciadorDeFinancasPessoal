package com.example.minhasfinancas.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.minhasfinancas.data.database.AppDatabaseHelper
import com.example.minhasfinancas.data.model.AuditHistoryItem
import com.example.minhasfinancas.data.model.InstallmentPlan
import com.example.minhasfinancas.data.model.RecurrenceFrequency
import com.example.minhasfinancas.data.model.RecurringPlan
import com.example.minhasfinancas.data.model.TagMarker
import com.example.minhasfinancas.data.model.Transaction
import com.example.minhasfinancas.data.model.TransactionType
import com.example.minhasfinancas.data.model.UserProfile
import com.example.minhasfinancas.data.preferences.BankPreferences
import com.example.minhasfinancas.data.preferences.ProfilePreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object ProfileBackupManager {

    private const val APP_IDENTIFIER = "MinhasFinancas"
    private const val SCHEMA_VERSION = 1

    fun exportProfileToJson(context: Context, profile: UserProfile): String {
        val root = JSONObject()
        root.put("app", APP_IDENTIFIER)
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Perfil
        val profileObj = JSONObject().apply {
            put("id", profile.id)
            put("name", profile.name)
            put("createdAt", profile.createdAt)

            // Se tiver foto, codifica em Base64 para transferir junto
            if (!profile.photoUri.isNullOrBlank()) {
                try {
                    val file = File(profile.photoUri)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        put("photoBase64", base64)
                    }
                } catch (_: Exception) {}
            }
        }
        root.put("profile", profileObj)

        val dbName = "minhas_financas_profile_${profile.id}.db"
        val dbHelper = AppDatabaseHelper(context, dbName)

        // 2. Marcadores / Tags
        val tagsArray = JSONArray()
        for (tag in dbHelper.getAllTags()) {
            val tObj = JSONObject().apply {
                put("id", tag.id)
                put("name", tag.name)
                put("colorHex", tag.colorHex)
                put("displayOrder", tag.displayOrder)
                put("isVisibleOnHome", tag.isVisibleOnHome)
                put("isDefault", tag.isDefault)
            }
            tagsArray.put(tObj)
        }
        root.put("tags", tagsArray)

        // 3. Transações
        val transArray = JSONArray()
        for (t in dbHelper.getAllTransactions()) {
            val tObj = JSONObject().apply {
                put("id", t.id)
                put("description", t.description)
                put("amount", t.amount)
                put("type", t.type.name)
                put("date", t.date)
                put("tag", t.tag)
                put("isRecurring", t.isRecurring)
                t.recurringPlanId?.let { put("recurringPlanId", it) }
                t.installmentPlanId?.let { put("installmentPlanId", it) }
                t.installmentNumber?.let { put("installmentNumber", it) }
                t.installmentTotal?.let { put("installmentTotal", it) }
                t.paymentMethod?.let { put("paymentMethod", it) }
                t.machineGrossAmount?.let { put("machineGrossAmount", it) }
                t.machineFeeAmount?.let { put("machineFeeAmount", it) }
                t.machineFeePercent?.let { put("machineFeePercent", it) }
                put("createdAt", t.createdAt)
            }
            transArray.put(tObj)
        }
        root.put("transactions", transArray)

        // 4. Planos Recorrentes
        val recArray = JSONArray()
        for (rec in dbHelper.getAllRecurringPlans()) {
            val rObj = JSONObject().apply {
                put("id", rec.id)
                put("description", rec.description)
                put("amount", rec.amount)
                put("type", rec.type.name)
                put("tag", rec.tag)
                put("frequency", rec.frequency.name)
                put("startTimestamp", rec.startTimestamp)
                put("active", rec.active)
                put("createdAt", rec.createdAt)
            }
            recArray.put(rObj)
        }
        root.put("recurringPlans", recArray)

        // 5. Planos de Parcelamento
        val instArray = JSONArray()
        for (inst in dbHelper.getAllInstallmentPlans()) {
            val iObj = JSONObject().apply {
                put("id", inst.id)
                put("description", inst.description)
                put("totalAmount", inst.totalAmount)
                put("installmentAmount", inst.installmentAmount)
                put("numberOfInstallments", inst.numberOfInstallments)
                put("tag", inst.tag)
                put("startTimestamp", inst.startTimestamp)
                put("createdAt", inst.createdAt)
            }
            instArray.put(iObj)
        }
        root.put("installmentPlans", instArray)

        // 6. Bancos Personalizados
        val banksMap = BankPreferences.getAllBanksForProfile(context, profile.id)
        val banksObj = JSONObject()
        for ((m, list) in banksMap) {
            val arr = JSONArray()
            for (b in list) arr.put(b)
            banksObj.put(m, arr)
        }
        root.put("banks", banksObj)

        // 7. Histórico de Auditoria
        val auditArray = JSONArray()
        for (a in dbHelper.getAllAuditHistory()) {
            val aObj = JSONObject().apply {
                put("id", a.id)
                put("actionType", a.actionType)
                put("description", a.description)
                put("amount", a.amount)
                put("type", a.type.name)
                put("tag", a.tag)
                put("eventTimestamp", a.eventTimestamp)
                put("details", a.details)
            }
            auditArray.put(aObj)
        }
        root.put("auditHistory", auditArray)

        return root.toString(2)
    }

    fun createShareIntent(context: Context, profile: UserProfile): Intent {
        val jsonStr = exportProfileToJson(context, profile)
        val safeName = profile.name.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val fileName = "perfil_${safeName}_backup.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(jsonStr)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup Perfil Financeiro - ${profile.name}")
            putExtra(Intent.EXTRA_TEXT, "Arquivo de backup do perfil '${profile.name}' gerado pelo aplicativo Minhas Finanças.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Exportar Perfil Financeiro")
    }

    fun importProfileFromJson(context: Context, jsonStr: String): UserProfile {
        val root = JSONObject(jsonStr)
        val profileObj = root.getJSONObject("profile")

        var importedId = profileObj.optString("id", UUID.randomUUID().toString())
        val existingProfiles = ProfilePreferences.getAllProfiles(context)
        // Se já existir um perfil com o mesmo ID, gera um novo ID para não sobrepor
        if (existingProfiles.any { it.id == importedId }) {
            importedId = UUID.randomUUID().toString()
        }

        var importedName = profileObj.getString("name").trim()
        if (existingProfiles.any { it.name.equals(importedName, ignoreCase = true) }) {
            importedName = "$importedName (Importado)"
        }

        // Restaura foto se existir
        var photoPath: String? = null
        if (profileObj.has("photoBase64")) {
            try {
                val b64 = profileObj.getString("photoBase64")
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val photoFile = File(context.filesDir, "profile_${importedId}.jpg")
                photoFile.writeBytes(bytes)
                photoPath = photoFile.absolutePath
            } catch (_: Exception) {}
        }

        val newProfile = UserProfile(
            id = importedId,
            name = importedName,
            photoUri = photoPath,
            createdAt = profileObj.optLong("createdAt", System.currentTimeMillis())
        )

        // Salva perfil nas preferências
        ProfilePreferences.saveImportedProfile(context, newProfile)

        // 2. Restaura Marcadores
        val tagsList = mutableListOf<TagMarker>()
        val tagsArray = root.optJSONArray("tags")
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                val obj = tagsArray.getJSONObject(i)
                tagsList.add(
                    TagMarker(
                        id = obj.optLong("id", 0),
                        name = obj.getString("name"),
                        colorHex = obj.getString("colorHex"),
                        displayOrder = obj.optInt("displayOrder", i),
                        isVisibleOnHome = obj.optBoolean("isVisibleOnHome", true),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }
        }

        // 3. Restaura Transações
        val transList = mutableListOf<Transaction>()
        val transArray = root.optJSONArray("transactions")
        if (transArray != null) {
            for (i in 0 until transArray.length()) {
                val obj = transArray.getJSONObject(i)
                transList.add(
                    Transaction(
                        id = obj.optLong("id", 0),
                        description = obj.getString("description"),
                        amount = obj.getDouble("amount"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        date = obj.getLong("date"),
                        tag = obj.getString("tag"),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        recurringPlanId = if (obj.has("recurringPlanId")) obj.getLong("recurringPlanId") else null,
                        installmentPlanId = if (obj.has("installmentPlanId")) obj.getLong("installmentPlanId") else null,
                        installmentNumber = if (obj.has("installmentNumber")) obj.getInt("installmentNumber") else null,
                        installmentTotal = if (obj.has("installmentTotal")) obj.getInt("installmentTotal") else null,
                        paymentMethod = if (obj.has("paymentMethod")) obj.getString("paymentMethod") else null,
                        machineGrossAmount = if (obj.has("machineGrossAmount")) obj.getDouble("machineGrossAmount") else null,
                        machineFeeAmount = if (obj.has("machineFeeAmount")) obj.getDouble("machineFeeAmount") else null,
                        machineFeePercent = if (obj.has("machineFeePercent")) obj.getDouble("machineFeePercent") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // 4. Planos Recorrentes
        val recList = mutableListOf<RecurringPlan>()
        val recArray = root.optJSONArray("recurringPlans")
        if (recArray != null) {
            for (i in 0 until recArray.length()) {
                val obj = recArray.getJSONObject(i)
                recList.add(
                    RecurringPlan(
                        id = obj.optLong("id", 0),
                        description = obj.getString("description"),
                        amount = obj.getDouble("amount"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        tag = obj.getString("tag"),
                        frequency = RecurrenceFrequency.valueOf(obj.optString("frequency", "MENSAL")),
                        startTimestamp = obj.getLong("startTimestamp"),
                        active = obj.optBoolean("active", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // 5. Planos de Parcelamento
        val instList = mutableListOf<InstallmentPlan>()
        val instArray = root.optJSONArray("installmentPlans")
        if (instArray != null) {
            for (i in 0 until instArray.length()) {
                val obj = instArray.getJSONObject(i)
                instList.add(
                    InstallmentPlan(
                        id = obj.optLong("id", 0),
                        description = obj.getString("description"),
                        totalAmount = obj.getDouble("totalAmount"),
                        installmentAmount = obj.getDouble("installmentAmount"),
                        numberOfInstallments = obj.getInt("numberOfInstallments"),
                        tag = obj.getString("tag"),
                        startTimestamp = obj.getLong("startTimestamp"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // 6. Restaura Histórico de Auditoria
        val auditList = mutableListOf<AuditHistoryItem>()
        val auditArray = root.optJSONArray("auditHistory")
        if (auditArray != null) {
            for (i in 0 until auditArray.length()) {
                val obj = auditArray.getJSONObject(i)
                auditList.add(
                    AuditHistoryItem(
                        id = obj.optLong("id", 0),
                        actionType = obj.getString("actionType"),
                        description = obj.getString("description"),
                        amount = obj.getDouble("amount"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        tag = obj.getString("tag"),
                        eventTimestamp = obj.optLong("eventTimestamp", System.currentTimeMillis()),
                        details = obj.optString("details", "")
                    )
                )
            }
        }

        // Abre o banco deste perfil e salva todas as tabelas
        val dbHelper = AppDatabaseHelper(context, "minhas_financas_profile_${importedId}.db")
        dbHelper.restoreProfileData(
            tags = tagsList,
            transactions = transList,
            recurringPlans = recList,
            installmentPlans = instList,
            auditHistory = auditList
        )

        // 7. Restaura Bancos nas preferências
        val banksObj = root.optJSONObject("banks")
        if (banksObj != null) {
            val banksMap = mutableMapOf<String, List<String>>()
            val keys = banksObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = banksObj.getJSONArray(k)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                banksMap[k] = list
            }
            BankPreferences.restoreBanksForProfile(context, importedId, banksMap)
        }

        return newProfile
    }
}
