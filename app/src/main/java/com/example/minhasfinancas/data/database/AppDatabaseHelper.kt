package com.example.minhasfinancas.data.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.minhasfinancas.data.model.AuditHistoryItem
import com.example.minhasfinancas.data.model.InstallmentPlan
import com.example.minhasfinancas.data.model.RecurrenceFrequency
import com.example.minhasfinancas.data.model.RecurringPlan
import com.example.minhasfinancas.data.model.TagMarker
import com.example.minhasfinancas.data.model.Transaction
import com.example.minhasfinancas.data.model.TransactionType
import com.example.minhasfinancas.ui.util.Formatters
import java.util.Calendar

class AppDatabaseHelper(
    context: Context,
    databaseName: String = DATABASE_NAME
) : SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "minhas_financas.db"
        const val DATABASE_VERSION = 5

        // Tabela Transactions
        const val TABLE_TRANSACTIONS = "transactions"
        const val COL_TRANS_ID = "id"
        const val COL_TRANS_DESC = "description"
        const val COL_TRANS_AMOUNT = "amount"
        const val COL_TRANS_TYPE = "type"
        const val COL_TRANS_DATE = "date"
        const val COL_TRANS_TAG = "tag"
        const val COL_TRANS_IS_RECURRING = "is_recurring"
        const val COL_TRANS_RECURRING_PLAN_ID = "recurring_plan_id"
        const val COL_TRANS_INSTALLMENT_PLAN_ID = "installment_plan_id"
        const val COL_TRANS_INSTALLMENT_NUM = "installment_number"
        const val COL_TRANS_INSTALLMENT_TOTAL = "installment_total"
        const val COL_TRANS_PAYMENT_METHOD = "payment_method"
        const val COL_TRANS_MACHINE_GROSS = "machine_gross_amount"
        const val COL_TRANS_MACHINE_FEE = "machine_fee_amount"
        const val COL_TRANS_MACHINE_RATE = "machine_fee_percent"
        const val COL_TRANS_CREATED_AT = "created_at"

        // Tabela Recurring Plans
        const val TABLE_RECURRING = "recurring_plans"
        const val COL_REC_ID = "id"
        const val COL_REC_DESC = "description"
        const val COL_REC_AMOUNT = "amount"
        const val COL_REC_TYPE = "type"
        const val COL_REC_TAG = "tag"
        const val COL_REC_FREQ = "frequency"
        const val COL_REC_START = "start_timestamp"
        const val COL_REC_ACTIVE = "active"
        const val COL_REC_CREATED_AT = "created_at"

        // Tabela Installment Plans
        const val TABLE_INSTALLMENTS = "installment_plans"
        const val COL_INST_ID = "id"
        const val COL_INST_DESC = "description"
        const val COL_INST_TOTAL_AMOUNT = "total_amount"
        const val COL_INST_PARCEL_AMOUNT = "installment_amount"
        const val COL_INST_NUM_PARCELS = "number_of_installments"
        const val COL_INST_TAG = "tag"
        const val COL_INST_START = "start_timestamp"
        const val COL_INST_CREATED_AT = "created_at"

        // Tabela Tag Markers
        const val TABLE_TAGS = "tag_markers"
        const val COL_TAG_ID = "id"
        const val COL_TAG_NAME = "name"
        const val COL_TAG_COLOR = "color_hex"
        const val COL_TAG_IS_DEFAULT = "is_default"
        const val COL_TAG_DISPLAY_ORDER = "display_order"
        const val COL_TAG_VISIBLE_ON_HOME = "is_visible_on_home"

        // Tabela Histórico Imutável (Audit Log)
        const val TABLE_AUDIT_HISTORY = "audit_history"
        const val COL_AUDIT_ID = "id"
        const val COL_AUDIT_ACTION = "action_type"
        const val COL_AUDIT_DESC = "description"
        const val COL_AUDIT_AMOUNT = "amount"
        const val COL_AUDIT_TYPE = "type"
        const val COL_AUDIT_TAG = "tag"
        const val COL_AUDIT_EVENT_TIMESTAMP = "event_timestamp"
        const val COL_AUDIT_DETAILS = "details"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTransactionsTable = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COL_TRANS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TRANS_DESC TEXT NOT NULL,
                $COL_TRANS_AMOUNT REAL NOT NULL,
                $COL_TRANS_TYPE TEXT NOT NULL,
                $COL_TRANS_DATE INTEGER NOT NULL,
                $COL_TRANS_TAG TEXT NOT NULL,
                $COL_TRANS_IS_RECURRING INTEGER NOT NULL DEFAULT 0,
                $COL_TRANS_RECURRING_PLAN_ID INTEGER,
                $COL_TRANS_INSTALLMENT_PLAN_ID INTEGER,
                $COL_TRANS_INSTALLMENT_NUM INTEGER,
                $COL_TRANS_INSTALLMENT_TOTAL INTEGER,
                $COL_TRANS_PAYMENT_METHOD TEXT,
                $COL_TRANS_MACHINE_GROSS REAL,
                $COL_TRANS_MACHINE_FEE REAL,
                $COL_TRANS_MACHINE_RATE REAL,
                $COL_TRANS_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        val createRecurringTable = """
            CREATE TABLE $TABLE_RECURRING (
                $COL_REC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_REC_DESC TEXT NOT NULL,
                $COL_REC_AMOUNT REAL NOT NULL,
                $COL_REC_TYPE TEXT NOT NULL,
                $COL_REC_TAG TEXT NOT NULL,
                $COL_REC_FREQ TEXT NOT NULL,
                $COL_REC_START INTEGER NOT NULL,
                $COL_REC_ACTIVE INTEGER NOT NULL DEFAULT 1,
                $COL_REC_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        val createInstallmentsTable = """
            CREATE TABLE $TABLE_INSTALLMENTS (
                $COL_INST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_INST_DESC TEXT NOT NULL,
                $COL_INST_TOTAL_AMOUNT REAL NOT NULL,
                $COL_INST_PARCEL_AMOUNT REAL NOT NULL,
                $COL_INST_NUM_PARCELS INTEGER NOT NULL,
                $COL_INST_TAG TEXT NOT NULL,
                $COL_INST_START INTEGER NOT NULL,
                $COL_INST_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        val createTagsTable = """
            CREATE TABLE $TABLE_TAGS (
                $COL_TAG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TAG_NAME TEXT NOT NULL UNIQUE,
                $COL_TAG_COLOR TEXT NOT NULL,
                $COL_TAG_IS_DEFAULT INTEGER NOT NULL DEFAULT 0,
                $COL_TAG_DISPLAY_ORDER INTEGER NOT NULL DEFAULT 0,
                $COL_TAG_VISIBLE_ON_HOME INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent()

        val createAuditHistoryTable = """
            CREATE TABLE $TABLE_AUDIT_HISTORY (
                $COL_AUDIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_AUDIT_ACTION TEXT NOT NULL,
                $COL_AUDIT_DESC TEXT NOT NULL,
                $COL_AUDIT_AMOUNT REAL NOT NULL,
                $COL_AUDIT_TYPE TEXT NOT NULL,
                $COL_AUDIT_TAG TEXT NOT NULL,
                $COL_AUDIT_EVENT_TIMESTAMP INTEGER NOT NULL,
                $COL_AUDIT_DETAILS TEXT NOT NULL
            )
        """.trimIndent()

        db.execSQL(createTransactionsTable)
        db.execSQL(createRecurringTable)
        db.execSQL(createInstallmentsTable)
        db.execSQL(createTagsTable)
        db.execSQL(createAuditHistoryTable)

        // Inserir marcadores padrão
        insertDefaultTags(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS $TABLE_AUDIT_HISTORY (
                        $COL_AUDIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_AUDIT_ACTION TEXT NOT NULL,
                        $COL_AUDIT_DESC TEXT NOT NULL,
                        $COL_AUDIT_AMOUNT REAL NOT NULL,
                        $COL_AUDIT_TYPE TEXT NOT NULL,
                        $COL_AUDIT_TAG TEXT NOT NULL,
                        $COL_AUDIT_EVENT_TIMESTAMP INTEGER NOT NULL,
                        $COL_AUDIT_DETAILS TEXT NOT NULL
                    )
                """.trimIndent())
            } catch (_: Exception) {}
        }
        if (oldVersion < 3) {
            try {
                if (!hasColumn(db, TABLE_TAGS, COL_TAG_DISPLAY_ORDER)) {
                    db.execSQL("ALTER TABLE $TABLE_TAGS ADD COLUMN $COL_TAG_DISPLAY_ORDER INTEGER NOT NULL DEFAULT 0")
                }
            } catch (_: Exception) {}
        }
        if (oldVersion < 4) {
            try {
                if (!hasColumn(db, TABLE_TAGS, COL_TAG_VISIBLE_ON_HOME)) {
                    db.execSQL("ALTER TABLE $TABLE_TAGS ADD COLUMN $COL_TAG_VISIBLE_ON_HOME INTEGER NOT NULL DEFAULT 1")
                }
            } catch (_: Exception) {}
        }
        if (oldVersion < 5) {
            try {
                if (!hasColumn(db, TABLE_TRANSACTIONS, COL_TRANS_PAYMENT_METHOD)) {
                    db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COL_TRANS_PAYMENT_METHOD TEXT")
                }
            } catch (_: Exception) {}
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Garante que tabelas e colunas necessárias existam mesmo em bancos criados anteriormente
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_AUDIT_HISTORY (
                    $COL_AUDIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_AUDIT_ACTION TEXT NOT NULL,
                    $COL_AUDIT_DESC TEXT NOT NULL,
                    $COL_AUDIT_AMOUNT REAL NOT NULL,
                    $COL_AUDIT_TYPE TEXT NOT NULL,
                    $COL_AUDIT_TAG TEXT NOT NULL,
                    $COL_AUDIT_EVENT_TIMESTAMP INTEGER NOT NULL,
                    $COL_AUDIT_DETAILS TEXT NOT NULL
                )
            """.trimIndent())
        } catch (_: Exception) {}

        try {
            if (!hasColumn(db, TABLE_TAGS, COL_TAG_DISPLAY_ORDER)) {
                db.execSQL("ALTER TABLE $TABLE_TAGS ADD COLUMN $COL_TAG_DISPLAY_ORDER INTEGER NOT NULL DEFAULT 0")
            }
        } catch (_: Exception) {}

        try {
            if (!hasColumn(db, TABLE_TAGS, COL_TAG_VISIBLE_ON_HOME)) {
                db.execSQL("ALTER TABLE $TABLE_TAGS ADD COLUMN $COL_TAG_VISIBLE_ON_HOME INTEGER NOT NULL DEFAULT 1")
            }
        } catch (_: Exception) {}

        try {
            if (!hasColumn(db, TABLE_TRANSACTIONS, COL_TRANS_PAYMENT_METHOD)) {
                db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COL_TRANS_PAYMENT_METHOD TEXT")
            }
        } catch (_: Exception) {}

        try {
            if (!hasColumn(db, TABLE_TRANSACTIONS, COL_TRANS_MACHINE_GROSS)) {
                db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COL_TRANS_MACHINE_GROSS REAL")
            }
        } catch (_: Exception) {}

        try {
            if (!hasColumn(db, TABLE_TRANSACTIONS, COL_TRANS_MACHINE_FEE)) {
                db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COL_TRANS_MACHINE_FEE REAL")
            }
        } catch (_: Exception) {}

        try {
            if (!hasColumn(db, TABLE_TRANSACTIONS, COL_TRANS_MACHINE_RATE)) {
                db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COL_TRANS_MACHINE_RATE REAL")
            }
        } catch (_: Exception) {}
    }

    private fun insertDefaultTags(db: SQLiteDatabase) {
        val defaultTags = listOf(
            TagMarker(name = "Alimentação", colorHex = "#F59E0B", isDefault = true),
            TagMarker(name = "Transporte", colorHex = "#3B82F6", isDefault = true),
            TagMarker(name = "Moradia", colorHex = "#8B5CF6", isDefault = true),
            TagMarker(name = "Lazer", colorHex = "#EC4899", isDefault = true),
            TagMarker(name = "Saúde", colorHex = "#EF4444", isDefault = true),
            TagMarker(name = "Educação", colorHex = "#14B8A6", isDefault = true),
            TagMarker(name = "Salário", colorHex = "#10B981", isDefault = true),
            TagMarker(name = "Investimentos", colorHex = "#06B6D4", isDefault = true),
            TagMarker(name = "Contas Fixas", colorHex = "#6366F1", isDefault = true),
            TagMarker(name = "Cartão de Crédito", colorHex = "#F97316", isDefault = true),
            TagMarker(name = "Outros", colorHex = "#64748B", isDefault = true)
        )

        for ((index, tag) in defaultTags.withIndex()) {
            val cv = ContentValues().apply {
                put(COL_TAG_NAME, tag.name)
                put(COL_TAG_COLOR, tag.colorHex)
                put(COL_TAG_IS_DEFAULT, 1)
                put(COL_TAG_DISPLAY_ORDER, index)
                put(COL_TAG_VISIBLE_ON_HOME, 1)
            }
            db.insertWithOnConflict(TABLE_TAGS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    // --- OPERAÇÕES DE AUDITORIA / HISTÓRICO IMUTÁVEL ---

    fun logAudit(
        action: String,
        description: String,
        amount: Double,
        type: TransactionType,
        tag: String,
        details: String = ""
    ) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_AUDIT_ACTION, action)
            put(COL_AUDIT_DESC, description)
            put(COL_AUDIT_AMOUNT, amount)
            put(COL_AUDIT_TYPE, type.name)
            put(COL_AUDIT_TAG, tag)
            put(COL_AUDIT_EVENT_TIMESTAMP, System.currentTimeMillis())
            put(COL_AUDIT_DETAILS, details)
        }
        db.insert(TABLE_AUDIT_HISTORY, null, cv)
    }

    fun getAllAuditHistory(): List<AuditHistoryItem> {
        val list = mutableListOf<AuditHistoryItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_AUDIT_HISTORY,
            null,
            null,
            null,
            null,
            null,
            "$COL_AUDIT_EVENT_TIMESTAMP DESC, $COL_AUDIT_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val typeStr = it.getString(it.getColumnIndexOrThrow(COL_AUDIT_TYPE))
                val transType = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.SAIDA }
                list.add(
                    AuditHistoryItem(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_AUDIT_ID)),
                        actionType = it.getString(it.getColumnIndexOrThrow(COL_AUDIT_ACTION)),
                        description = it.getString(it.getColumnIndexOrThrow(COL_AUDIT_DESC)),
                        amount = it.getDouble(it.getColumnIndexOrThrow(COL_AUDIT_AMOUNT)),
                        type = transType,
                        tag = it.getString(it.getColumnIndexOrThrow(COL_AUDIT_TAG)),
                        eventTimestamp = it.getLong(it.getColumnIndexOrThrow(COL_AUDIT_EVENT_TIMESTAMP)),
                        details = it.getString(it.getColumnIndexOrThrow(COL_AUDIT_DETAILS))
                    )
                )
            }
        }
        return list
    }

    // --- OPERAÇÕES DE TAGS ---

    fun getAllTags(): List<TagMarker> {
        val list = mutableListOf<TagMarker>()
        val db = readableDatabase
        val hasDisplayOrderCol = hasColumn(db, TABLE_TAGS, COL_TAG_DISPLAY_ORDER)
        val orderBy = if (hasDisplayOrderCol) "$COL_TAG_DISPLAY_ORDER ASC, $COL_TAG_ID ASC" else "$COL_TAG_NAME ASC"

        val cursor = db.query(TABLE_TAGS, null, null, null, null, null, orderBy)
        cursor.use {
            while (it.moveToNext()) {
                val orderIdx = it.getColumnIndex(COL_TAG_DISPLAY_ORDER)
                val displayOrder = if (orderIdx != -1 && !it.isNull(orderIdx)) it.getInt(orderIdx) else 0
                val visIdx = it.getColumnIndex(COL_TAG_VISIBLE_ON_HOME)
                val isVisible = if (visIdx != -1 && !it.isNull(visIdx)) it.getInt(visIdx) == 1 else true
                list.add(
                    TagMarker(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_TAG_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COL_TAG_NAME)),
                        colorHex = it.getString(it.getColumnIndexOrThrow(COL_TAG_COLOR)),
                        isDefault = it.getInt(it.getColumnIndexOrThrow(COL_TAG_IS_DEFAULT)) == 1,
                        displayOrder = displayOrder,
                        isVisibleOnHome = isVisible
                    )
                )
            }
        }
        return list
    }

    private fun hasColumn(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
            val nameColIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameColIdx != -1 && cursor.getString(nameColIdx).equals(columnName, ignoreCase = true)) {
                    return true
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }
        return false
    }

    fun insertTag(name: String, colorHex: String, isVisibleOnHome: Boolean = true): Long {
        val db = writableDatabase
        val maxOrder = getMaxTagDisplayOrder(db)
        val cv = ContentValues().apply {
            put(COL_TAG_NAME, name)
            put(COL_TAG_COLOR, colorHex)
            put(COL_TAG_IS_DEFAULT, 0)
            put(COL_TAG_DISPLAY_ORDER, maxOrder + 1)
            put(COL_TAG_VISIBLE_ON_HOME, if (isVisibleOnHome) 1 else 0)
        }
        return db.insertWithOnConflict(TABLE_TAGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateTagVisibility(id: Long, isVisible: Boolean): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_TAG_VISIBLE_ON_HOME, if (isVisible) 1 else 0)
        }
        val rows = db.update(TABLE_TAGS, cv, "$COL_TAG_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    private fun getMaxTagDisplayOrder(db: SQLiteDatabase): Int {
        if (!hasColumn(db, TABLE_TAGS, COL_TAG_DISPLAY_ORDER)) return 0
        try {
            val cursor = db.rawQuery("SELECT MAX($COL_TAG_DISPLAY_ORDER) FROM $TABLE_TAGS", null)
            cursor.use {
                if (it.moveToFirst() && !it.isNull(0)) {
                    return it.getInt(0)
                }
            }
        } catch (_: Exception) {}
        return 0
    }

    fun updateTag(id: Long, oldName: String, newName: String, newColorHex: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put(COL_TAG_NAME, newName)
                put(COL_TAG_COLOR, newColorHex)
            }
            val rows = db.update(TABLE_TAGS, cv, "$COL_TAG_ID = ?", arrayOf(id.toString()))
            if (rows > 0 && oldName != newName) {
                // Atualiza também nas transações existentes vinculadas a esse marcador
                val cvTrans = ContentValues().apply {
                    put(COL_TRANS_TAG, newName)
                }
                db.update(TABLE_TRANSACTIONS, cvTrans, "$COL_TRANS_TAG = ?", arrayOf(oldName))

                val cvRec = ContentValues().apply {
                    put(COL_REC_TAG, newName)
                }
                db.update(TABLE_RECURRING, cvRec, "$COL_REC_TAG = ?", arrayOf(oldName))

                val cvInst = ContentValues().apply {
                    put(COL_INST_TAG, newName)
                }
                db.update(TABLE_INSTALLMENTS, cvInst, "$COL_INST_TAG = ?", arrayOf(oldName))
            }
            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTag(id: Long, tagName: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val rows = db.delete(TABLE_TAGS, "$COL_TAG_ID = ?", arrayOf(id.toString()))
            // Se existirem transações com essa tag, move para 'Outros' para não corromper histórico
            val cvTrans = ContentValues().apply {
                put(COL_TRANS_TAG, "Outros")
            }
            db.update(TABLE_TRANSACTIONS, cvTrans, "$COL_TRANS_TAG = ?", arrayOf(tagName))
            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    fun updateTagsOrder(orderedTags: List<TagMarker>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((index, tag) in orderedTags.withIndex()) {
                val cv = ContentValues().apply {
                    put(COL_TAG_DISPLAY_ORDER, index)
                }
                db.update(TABLE_TAGS, cv, "$COL_TAG_ID = ?", arrayOf(tag.id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // --- OPERAÇÕES DE TRANSAÇÃO ---

    fun getTransactionById(id: Long): Transaction? {
        val db = readableDatabase
        val cursor = db.query(TABLE_TRANSACTIONS, null, "$COL_TRANS_ID = ?", arrayOf(id.toString()), null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToTransaction(it)
            }
        }
        return null
    }

    fun insertTransaction(transaction: Transaction): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_TRANS_DESC, transaction.description)
            put(COL_TRANS_AMOUNT, transaction.amount)
            put(COL_TRANS_TYPE, transaction.type.name)
            put(COL_TRANS_DATE, transaction.date)
            put(COL_TRANS_TAG, transaction.tag)
            put(COL_TRANS_IS_RECURRING, if (transaction.isRecurring) 1 else 0)
            put(COL_TRANS_RECURRING_PLAN_ID, transaction.recurringPlanId)
            put(COL_TRANS_INSTALLMENT_PLAN_ID, transaction.installmentPlanId)
            put(COL_TRANS_INSTALLMENT_NUM, transaction.installmentNumber)
            put(COL_TRANS_INSTALLMENT_TOTAL, transaction.installmentTotal)
            put(COL_TRANS_PAYMENT_METHOD, transaction.paymentMethod)
            put(COL_TRANS_MACHINE_GROSS, transaction.machineGrossAmount)
            put(COL_TRANS_MACHINE_FEE, transaction.machineFeeAmount)
            put(COL_TRANS_MACHINE_RATE, transaction.machineFeePercent)
            put(COL_TRANS_CREATED_AT, if (transaction.createdAt > 0) transaction.createdAt else System.currentTimeMillis())
        }
        val id = db.insert(TABLE_TRANSACTIONS, null, cv)
        if (id != -1L) {
            val payMethodInfo = if (!transaction.paymentMethod.isNullOrBlank()) " | Meio de pagamento: ${transaction.paymentMethod}" else ""
            logAudit(
                action = "CRIADA",
                description = transaction.description,
                amount = transaction.amount,
                type = transaction.type,
                tag = transaction.tag,
                details = "Data da movimentação: ${Formatters.formatDate(transaction.date)}$payMethodInfo"
            )
        }
        return id
    }

    fun deleteTransaction(id: Long) {
        val db = writableDatabase
        val trans = getTransactionById(id)
        db.delete(TABLE_TRANSACTIONS, "$COL_TRANS_ID = ?", arrayOf(id.toString()))
        if (trans != null) {
            logAudit(
                action = "EXCLUÍDA",
                description = trans.description,
                amount = trans.amount,
                type = trans.type,
                tag = trans.tag,
                details = "Movimentação excluída da Home Page (permanece gravada no Histórico)"
            )
        }
    }

    fun updateTransaction(transaction: Transaction): Boolean {
        val db = writableDatabase
        val old = getTransactionById(transaction.id)
        val cv = ContentValues().apply {
            put(COL_TRANS_DESC, transaction.description)
            put(COL_TRANS_AMOUNT, transaction.amount)
            put(COL_TRANS_TYPE, transaction.type.name)
            put(COL_TRANS_DATE, transaction.date)
            put(COL_TRANS_TAG, transaction.tag)
            put(COL_TRANS_PAYMENT_METHOD, transaction.paymentMethod)
            put(COL_TRANS_MACHINE_GROSS, transaction.machineGrossAmount)
            put(COL_TRANS_MACHINE_FEE, transaction.machineFeeAmount)
            put(COL_TRANS_MACHINE_RATE, transaction.machineFeePercent)
        }
        val rows = db.update(TABLE_TRANSACTIONS, cv, "$COL_TRANS_ID = ?", arrayOf(transaction.id.toString()))
        if (rows > 0 && old != null) {
            val oldPay = if (!old.paymentMethod.isNullOrBlank()) " | Meio: ${old.paymentMethod}" else ""
            val newPay = if (!transaction.paymentMethod.isNullOrBlank()) " | Meio: ${transaction.paymentMethod}" else ""
            logAudit(
                action = "EDITADA",
                description = transaction.description,
                amount = transaction.amount,
                type = transaction.type,
                tag = transaction.tag,
                details = "Anterior: '${old.description}', ${Formatters.formatCurrency(old.amount)}, ${old.type.name}, ${Formatters.formatDate(old.date)}, Marcador: ${old.tag}$oldPay -> Atual: '${transaction.description}', ${Formatters.formatCurrency(transaction.amount)}, ${transaction.type.name}, ${Formatters.formatDate(transaction.date)}, Marcador: ${transaction.tag}$newPay"
            )
        }
        return rows > 0
    }

    fun getInstallmentPlanById(id: Long): InstallmentPlan? {
        val db = readableDatabase
        val cursor = db.query(TABLE_INSTALLMENTS, null, "$COL_INST_ID = ?", arrayOf(id.toString()), null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                return InstallmentPlan(
                    id = it.getLong(it.getColumnIndexOrThrow(COL_INST_ID)),
                    description = it.getString(it.getColumnIndexOrThrow(COL_INST_DESC)),
                    totalAmount = it.getDouble(it.getColumnIndexOrThrow(COL_INST_TOTAL_AMOUNT)),
                    installmentAmount = it.getDouble(it.getColumnIndexOrThrow(COL_INST_PARCEL_AMOUNT)),
                    numberOfInstallments = it.getInt(it.getColumnIndexOrThrow(COL_INST_NUM_PARCELS)),
                    tag = it.getString(it.getColumnIndexOrThrow(COL_INST_TAG)),
                    startTimestamp = it.getLong(it.getColumnIndexOrThrow(COL_INST_START)),
                    createdAt = it.getLong(it.getColumnIndexOrThrow(COL_INST_CREATED_AT))
                )
            }
        }
        return null
    }

    fun deleteInstallmentPlan(planId: Long) {
        val db = writableDatabase
        val plan = getInstallmentPlanById(planId)
        db.beginTransaction()
        try {
            db.delete(TABLE_TRANSACTIONS, "$COL_TRANS_INSTALLMENT_PLAN_ID = ?", arrayOf(planId.toString()))
            db.delete(TABLE_INSTALLMENTS, "$COL_INST_ID = ?", arrayOf(planId.toString()))
            if (plan != null) {
                logAudit(
                    action = "PARCELAMENTO EXCLUÍDO",
                    description = plan.description,
                    amount = plan.totalAmount,
                    type = TransactionType.SAIDA,
                    tag = plan.tag,
                    details = "Plano de ${plan.numberOfInstallments} parcelas cancelado/excluído"
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getRecurringPlanById(id: Long): RecurringPlan? {
        val db = readableDatabase
        val cursor = db.query(TABLE_RECURRING, null, "$COL_REC_ID = ?", arrayOf(id.toString()), null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                return RecurringPlan(
                    id = it.getLong(it.getColumnIndexOrThrow(COL_REC_ID)),
                    description = it.getString(it.getColumnIndexOrThrow(COL_REC_DESC)),
                    amount = it.getDouble(it.getColumnIndexOrThrow(COL_REC_AMOUNT)),
                    type = TransactionType.valueOf(it.getString(it.getColumnIndexOrThrow(COL_REC_TYPE))),
                    tag = it.getString(it.getColumnIndexOrThrow(COL_REC_TAG)),
                    frequency = RecurrenceFrequency.valueOf(it.getString(it.getColumnIndexOrThrow(COL_REC_FREQ))),
                    startTimestamp = it.getLong(it.getColumnIndexOrThrow(COL_REC_START)),
                    active = it.getInt(it.getColumnIndexOrThrow(COL_REC_ACTIVE)) == 1,
                    createdAt = it.getLong(it.getColumnIndexOrThrow(COL_REC_CREATED_AT))
                )
            }
        }
        return null
    }

    fun deleteRecurringPlan(planId: Long) {
        val db = writableDatabase
        val plan = getRecurringPlanById(planId)
        db.beginTransaction()
        try {
            db.delete(TABLE_TRANSACTIONS, "$COL_TRANS_RECURRING_PLAN_ID = ?", arrayOf(planId.toString()))
            db.delete(TABLE_RECURRING, "$COL_REC_ID = ?", arrayOf(planId.toString()))
            if (plan != null) {
                logAudit(
                    action = "RECORRENTE EXCLUÍDA",
                    description = plan.description,
                    amount = plan.amount,
                    type = plan.type,
                    tag = plan.tag,
                    details = "Plano recorrente cancelado/excluído"
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllTransactions(): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val db = readableDatabase
        val cursor = db.query(TABLE_TRANSACTIONS, null, null, null, null, null, "$COL_TRANS_DATE DESC, $COL_TRANS_ID DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTransaction(it))
            }
        }
        return list
    }

    fun getTransactionsByMonth(year: Int, month: Int): List<Transaction> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis

        val list = mutableListOf<Transaction>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_TRANS_DATE >= ? AND $COL_TRANS_DATE <= ?",
            arrayOf(startMs.toString(), endMs.toString()),
            null,
            null,
            "$COL_TRANS_DATE DESC, $COL_TRANS_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTransaction(it))
            }
        }
        return list
    }

    fun getTransactionsByDateRange(startMs: Long, endMs: Long): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_TRANS_DATE >= ? AND $COL_TRANS_DATE <= ?",
            arrayOf(startMs.toString(), endMs.toString()),
            null,
            null,
            "$COL_TRANS_DATE DESC, $COL_TRANS_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTransaction(it))
            }
        }
        return list
    }

    private fun cursorToTransaction(c: Cursor): Transaction {
        val recPlanId = if (c.isNull(c.getColumnIndexOrThrow(COL_TRANS_RECURRING_PLAN_ID))) null else c.getLong(c.getColumnIndexOrThrow(COL_TRANS_RECURRING_PLAN_ID))
        val instPlanId = if (c.isNull(c.getColumnIndexOrThrow(COL_TRANS_INSTALLMENT_PLAN_ID))) null else c.getLong(c.getColumnIndexOrThrow(COL_TRANS_INSTALLMENT_PLAN_ID))
        val instNum = if (c.isNull(c.getColumnIndexOrThrow(COL_TRANS_INSTALLMENT_NUM))) null else c.getInt(c.getColumnIndexOrThrow(COL_TRANS_INSTALLMENT_NUM))
        val instTotal = if (c.isNull(c.getColumnIndexOrThrow(COL_TRANS_INSTALLMENT_TOTAL))) null else c.getInt(c.getColumnIndexOrThrow(COL_TRANS_INSTALLMENT_TOTAL))
        val payMethodIdx = c.getColumnIndex(COL_TRANS_PAYMENT_METHOD)
        val payMethod = if (payMethodIdx != -1 && !c.isNull(payMethodIdx)) c.getString(payMethodIdx) else null
        val grossIdx = c.getColumnIndex(COL_TRANS_MACHINE_GROSS)
        val gross = if (grossIdx != -1 && !c.isNull(grossIdx)) c.getDouble(grossIdx) else null
        val feeIdx = c.getColumnIndex(COL_TRANS_MACHINE_FEE)
        val fee = if (feeIdx != -1 && !c.isNull(feeIdx)) c.getDouble(feeIdx) else null
        val rateIdx = c.getColumnIndex(COL_TRANS_MACHINE_RATE)
        val rate = if (rateIdx != -1 && !c.isNull(rateIdx)) c.getDouble(rateIdx) else null

        return Transaction(
            id = c.getLong(c.getColumnIndexOrThrow(COL_TRANS_ID)),
            description = c.getString(c.getColumnIndexOrThrow(COL_TRANS_DESC)),
            amount = c.getDouble(c.getColumnIndexOrThrow(COL_TRANS_AMOUNT)),
            type = TransactionType.valueOf(c.getString(c.getColumnIndexOrThrow(COL_TRANS_TYPE))),
            date = c.getLong(c.getColumnIndexOrThrow(COL_TRANS_DATE)),
            tag = c.getString(c.getColumnIndexOrThrow(COL_TRANS_TAG)),
            isRecurring = c.getInt(c.getColumnIndexOrThrow(COL_TRANS_IS_RECURRING)) == 1,
            recurringPlanId = recPlanId,
            installmentPlanId = instPlanId,
            installmentNumber = instNum,
            installmentTotal = instTotal,
            paymentMethod = payMethod,
            machineGrossAmount = gross,
            machineFeeAmount = fee,
            machineFeePercent = rate,
            createdAt = c.getLong(c.getColumnIndexOrThrow(COL_TRANS_CREATED_AT))
        )
    }

    // --- OPERAÇÕES DE PARCELAMENTO ---

    fun insertInstallmentPlan(
        description: String,
        totalAmount: Double,
        installmentAmount: Double,
        numberOfInstallments: Int,
        tag: String,
        startTimestamp: Long,
        type: TransactionType = TransactionType.SAIDA,
        paymentMethod: String? = "Cartão de Crédito"
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        var planId: Long = -1
        try {
            val cvPlan = ContentValues().apply {
                put(COL_INST_DESC, description)
                put(COL_INST_TOTAL_AMOUNT, totalAmount)
                put(COL_INST_PARCEL_AMOUNT, installmentAmount)
                put(COL_INST_NUM_PARCELS, numberOfInstallments)
                put(COL_INST_TAG, tag)
                put(COL_INST_START, startTimestamp)
                put(COL_INST_CREATED_AT, System.currentTimeMillis())
            }
            planId = db.insert(TABLE_INSTALLMENTS, null, cvPlan)

            // Criar as transações mensais para cada parcela
            val calendar = Calendar.getInstance().apply {
                timeInMillis = startTimestamp
            }

            for (i in 1..numberOfInstallments) {
                val cvTrans = ContentValues().apply {
                    put(COL_TRANS_DESC, "$description ($i/$numberOfInstallments)")
                    put(COL_TRANS_AMOUNT, installmentAmount)
                    put(COL_TRANS_TYPE, type.name)
                    put(COL_TRANS_DATE, calendar.timeInMillis)
                    put(COL_TRANS_TAG, tag)
                    put(COL_TRANS_IS_RECURRING, 0)
                    put(COL_TRANS_INSTALLMENT_PLAN_ID, planId)
                    put(COL_TRANS_INSTALLMENT_NUM, i)
                    put(COL_TRANS_INSTALLMENT_TOTAL, numberOfInstallments)
                    put(COL_TRANS_PAYMENT_METHOD, paymentMethod ?: "Cartão de Crédito")
                    put(COL_TRANS_CREATED_AT, System.currentTimeMillis())
                }
                db.insert(TABLE_TRANSACTIONS, null, cvTrans)

                // Avança 1 mês para a próxima parcela
                calendar.add(Calendar.MONTH, 1)
            }

            val payMethodInfo = if (!paymentMethod.isNullOrBlank()) " | Meio de pagamento: $paymentMethod" else ""
            logAudit(
                action = "PARCELAMENTO CRIADO",
                description = description,
                amount = totalAmount,
                type = type,
                tag = tag,
                details = "$numberOfInstallments parcelas de ${Formatters.formatCurrency(installmentAmount)}$payMethodInfo"
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return planId
    }

    fun getAllInstallmentPlans(): List<InstallmentPlan> {
        val list = mutableListOf<InstallmentPlan>()
        val db = readableDatabase
        val cursor = db.query(TABLE_INSTALLMENTS, null, null, null, null, null, "$COL_INST_CREATED_AT DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    InstallmentPlan(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_INST_ID)),
                        description = it.getString(it.getColumnIndexOrThrow(COL_INST_DESC)),
                        totalAmount = it.getDouble(it.getColumnIndexOrThrow(COL_INST_TOTAL_AMOUNT)),
                        installmentAmount = it.getDouble(it.getColumnIndexOrThrow(COL_INST_PARCEL_AMOUNT)),
                        numberOfInstallments = it.getInt(it.getColumnIndexOrThrow(COL_INST_NUM_PARCELS)),
                        tag = it.getString(it.getColumnIndexOrThrow(COL_INST_TAG)),
                        startTimestamp = it.getLong(it.getColumnIndexOrThrow(COL_INST_START)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_INST_CREATED_AT))
                    )
                )
            }
        }
        return list
    }

    // --- OPERAÇÕES DE GASTOS/RECEITAS RECORRENTES ---

    fun insertRecurringPlan(
        description: String,
        amount: Double,
        type: TransactionType,
        tag: String,
        frequency: RecurrenceFrequency,
        startTimestamp: Long,
        monthsToPreGenerate: Int = 24
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        var planId: Long = -1
        try {
            val cvPlan = ContentValues().apply {
                put(COL_REC_DESC, description)
                put(COL_REC_AMOUNT, amount)
                put(COL_REC_TYPE, type.name)
                put(COL_REC_TAG, tag)
                put(COL_REC_FREQ, frequency.name)
                put(COL_REC_START, startTimestamp)
                put(COL_REC_ACTIVE, 1)
                put(COL_REC_CREATED_AT, System.currentTimeMillis())
            }
            planId = db.insert(TABLE_RECURRING, null, cvPlan)

            // Pré-gerar transações para os próximos ciclos (ex: 24 meses se mensal, 5 anos se anual)
            val calendar = Calendar.getInstance().apply {
                timeInMillis = startTimestamp
            }

            val iterations = if (frequency == RecurrenceFrequency.MENSAL) monthsToPreGenerate else 5
            for (i in 0 until iterations) {
                val cvTrans = ContentValues().apply {
                    put(COL_TRANS_DESC, description)
                    put(COL_TRANS_AMOUNT, amount)
                    put(COL_TRANS_TYPE, type.name)
                    put(COL_TRANS_DATE, calendar.timeInMillis)
                    put(COL_TRANS_TAG, tag)
                    put(COL_TRANS_IS_RECURRING, 1)
                    put(COL_TRANS_RECURRING_PLAN_ID, planId)
                    put(COL_TRANS_CREATED_AT, System.currentTimeMillis())
                }
                db.insert(TABLE_TRANSACTIONS, null, cvTrans)

                if (frequency == RecurrenceFrequency.MENSAL) {
                    calendar.add(Calendar.MONTH, 1)
                } else {
                    calendar.add(Calendar.YEAR, 1)
                }
            }

            logAudit(
                action = "RECORRENTE CRIADA",
                description = description,
                amount = amount,
                type = type,
                tag = tag,
                details = "Frequência: ${if (frequency == RecurrenceFrequency.MENSAL) "Mensal" else "Anual"}"
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return planId
    }

    fun getAllRecurringPlans(): List<RecurringPlan> {
        val list = mutableListOf<RecurringPlan>()
        val db = readableDatabase
        val cursor = db.query(TABLE_RECURRING, null, null, null, null, null, "$COL_REC_CREATED_AT DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    RecurringPlan(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_REC_ID)),
                        description = it.getString(it.getColumnIndexOrThrow(COL_REC_DESC)),
                        amount = it.getDouble(it.getColumnIndexOrThrow(COL_REC_AMOUNT)),
                        type = TransactionType.valueOf(it.getString(it.getColumnIndexOrThrow(COL_REC_TYPE))),
                        tag = it.getString(it.getColumnIndexOrThrow(COL_REC_TAG)),
                        frequency = RecurrenceFrequency.valueOf(it.getString(it.getColumnIndexOrThrow(COL_REC_FREQ))),
                        startTimestamp = it.getLong(it.getColumnIndexOrThrow(COL_REC_START)),
                        active = it.getInt(it.getColumnIndexOrThrow(COL_REC_ACTIVE)) == 1,
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_REC_CREATED_AT))
                    )
                )
            }
        }
        return list
    }

    fun restoreProfileData(
        tags: List<TagMarker>,
        transactions: List<Transaction>,
        recurringPlans: List<RecurringPlan>,
        installmentPlans: List<InstallmentPlan>,
        auditHistory: List<AuditHistoryItem>
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Limpa dados atuais deste banco
            db.delete(TABLE_TRANSACTIONS, null, null)
            db.delete(TABLE_RECURRING, null, null)
            db.delete(TABLE_INSTALLMENTS, null, null)
            db.delete(TABLE_TAGS, null, null)
            db.delete(TABLE_AUDIT_HISTORY, null, null)

            // Restaura Marcadores
            for (tag in tags) {
                val cvTag = ContentValues().apply {
                    if (tag.id > 0) put(COL_TAG_ID, tag.id)
                    put(COL_TAG_NAME, tag.name)
                    put(COL_TAG_COLOR, tag.colorHex)
                    put(COL_TAG_DISPLAY_ORDER, tag.displayOrder)
                    put(COL_TAG_VISIBLE_ON_HOME, if (tag.isVisibleOnHome) 1 else 0)
                    put(COL_TAG_IS_DEFAULT, if (tag.isDefault) 1 else 0)
                }
                db.insertWithOnConflict(TABLE_TAGS, null, cvTag, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // Restaura Planos Recorrentes
            for (rec in recurringPlans) {
                val cvRec = ContentValues().apply {
                    if (rec.id > 0) put(COL_REC_ID, rec.id)
                    put(COL_REC_DESC, rec.description)
                    put(COL_REC_AMOUNT, rec.amount)
                    put(COL_REC_TYPE, rec.type.name)
                    put(COL_REC_TAG, rec.tag)
                    put(COL_REC_FREQ, rec.frequency.name)
                    put(COL_REC_START, rec.startTimestamp)
                    put(COL_REC_ACTIVE, if (rec.active) 1 else 0)
                    put(COL_REC_CREATED_AT, rec.createdAt)
                }
                db.insertWithOnConflict(TABLE_RECURRING, null, cvRec, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // Restaura Planos de Parcelamento
            for (inst in installmentPlans) {
                val cvInst = ContentValues().apply {
                    if (inst.id > 0) put(COL_INST_ID, inst.id)
                    put(COL_INST_DESC, inst.description)
                    put(COL_INST_TOTAL_AMOUNT, inst.totalAmount)
                    put(COL_INST_PARCEL_AMOUNT, inst.installmentAmount)
                    put(COL_INST_NUM_PARCELS, inst.numberOfInstallments)
                    put(COL_INST_TAG, inst.tag)
                    put(COL_INST_START, inst.startTimestamp)
                    put(COL_INST_CREATED_AT, inst.createdAt)
                }
                db.insertWithOnConflict(TABLE_INSTALLMENTS, null, cvInst, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // Restaura Transações
            for (trans in transactions) {
                val cvTrans = ContentValues().apply {
                    if (trans.id > 0) put(COL_TRANS_ID, trans.id)
                    put(COL_TRANS_DESC, trans.description)
                    put(COL_TRANS_AMOUNT, trans.amount)
                    put(COL_TRANS_TYPE, trans.type.name)
                    put(COL_TRANS_DATE, trans.date)
                    put(COL_TRANS_TAG, trans.tag)
                    put(COL_TRANS_IS_RECURRING, if (trans.isRecurring) 1 else 0)
                    put(COL_TRANS_RECURRING_PLAN_ID, trans.recurringPlanId)
                    put(COL_TRANS_INSTALLMENT_PLAN_ID, trans.installmentPlanId)
                    put(COL_TRANS_INSTALLMENT_NUM, trans.installmentNumber)
                    put(COL_TRANS_INSTALLMENT_TOTAL, trans.installmentTotal)
                    put(COL_TRANS_PAYMENT_METHOD, trans.paymentMethod)
                    put(COL_TRANS_CREATED_AT, trans.createdAt)
                }
                db.insertWithOnConflict(TABLE_TRANSACTIONS, null, cvTrans, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // Restaura Histórico de Auditoria
            for (audit in auditHistory) {
                val cvAudit = ContentValues().apply {
                    if (audit.id > 0) put(COL_AUDIT_ID, audit.id)
                    put(COL_AUDIT_ACTION, audit.actionType)
                    put(COL_AUDIT_DESC, audit.description)
                    put(COL_AUDIT_AMOUNT, audit.amount)
                    put(COL_AUDIT_TYPE, audit.type.name)
                    put(COL_AUDIT_TAG, audit.tag)
                    put(COL_AUDIT_EVENT_TIMESTAMP, audit.eventTimestamp)
                    put(COL_AUDIT_DETAILS, audit.details)
                }
                db.insertWithOnConflict(TABLE_AUDIT_HISTORY, null, cvAudit, SQLiteDatabase.CONFLICT_REPLACE)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
