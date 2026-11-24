package com.example.minhasfinancas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minhasfinancas.data.model.RecurrenceFrequency
import com.example.minhasfinancas.data.model.TagMarker
import com.example.minhasfinancas.data.model.TransactionType
import com.example.minhasfinancas.ui.components.DatePickerField
import com.example.minhasfinancas.ui.theme.ExpenseRed
import com.example.minhasfinancas.ui.theme.IncomeGreen
import com.example.minhasfinancas.ui.util.Formatters
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRecurringDialog(
    tags: List<TagMarker>,
    onDismiss: () -> Unit,
    onConfirm: (
        description: String,
        amount: Double,
        type: TransactionType,
        tag: String,
        frequency: RecurrenceFrequency,
        startTimestamp: Long
    ) -> Unit,
    onAddNewTag: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.SAIDA) }
    var selectedTag by remember { mutableStateOf(tags.firstOrNull()?.name ?: "Contas Fixas") }
    var selectedFrequency by remember { mutableStateOf(RecurrenceFrequency.MENSAL) }
    var selectedDateMillis by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Gasto/Receita Recorrente",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Tipo: Saída ou Entrada
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeSelectCard(
                        title = "Gasto Fixo",
                        icon = Icons.Default.ArrowDownward,
                        isSelected = selectedType == TransactionType.SAIDA,
                        activeColor = ExpenseRed,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedType = TransactionType.SAIDA }
                    )

                    TypeSelectCard(
                        title = "Receita Fixa",
                        icon = Icons.Default.ArrowUpward,
                        isSelected = selectedType == TransactionType.ENTRADA,
                        activeColor = IncomeGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedType = TransactionType.ENTRADA }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Valor
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.replace(",", ".")
                        errorMessage = null
                    },
                    label = { Text("Valor (R$)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Descrição
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        errorMessage = null
                    },
                    label = { Text("Descrição") },
                    placeholder = { Text("Ex: Mensalidade Internet, Aluguel, Netflix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Data de Início
                DatePickerField(
                    selectedDateMillis = selectedDateMillis,
                    onDateSelected = { selectedDateMillis = it },
                    label = "Data de Início da Recorrência"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Frequência
                Text(
                    text = "Periodicidade de repetição:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedFrequency == RecurrenceFrequency.MENSAL,
                        onClick = { selectedFrequency = RecurrenceFrequency.MENSAL },
                        label = { Text("Todo Mês") }
                    )
                    FilterChip(
                        selected = selectedFrequency == RecurrenceFrequency.ANUAL,
                        onClick = { selectedFrequency = RecurrenceFrequency.ANUAL },
                        label = { Text("Todo Ano") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Marcadores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Marcador / Categoria:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    TextButton(onClick = onAddNewTag) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Criar", fontSize = 12.sp)
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        val isSelected = selectedTag.equals(tag.name, ignoreCase = true)
                        val color = Formatters.parseHexColor(tag.colorHex)

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTag = tag.name },
                            label = { Text(tag.name, fontSize = 12.sp) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val desc = description.trim()
                    val amount = amountText.toDoubleOrNull()
                    if (desc.isBlank()) {
                        errorMessage = "Informe a descrição do gasto recorrente"
                    } else if (amount == null || amount <= 0.0) {
                        errorMessage = "Informe um valor válido maior que zero"
                    } else {
                        onConfirm(desc, amount, selectedType, selectedTag, selectedFrequency, selectedDateMillis)
                        onDismiss()
                    }
                }
            ) {
                Text("Adicionar Recorrente")
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

@Composable
private fun TypeSelectCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, activeColor, RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
