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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minhasfinancas.data.model.TagMarker
import com.example.minhasfinancas.ui.components.DatePickerField
import com.example.minhasfinancas.ui.components.InstallmentCounter
import com.example.minhasfinancas.ui.util.Formatters
import java.util.Calendar
import kotlin.math.roundToInt

enum class InstallmentInputMode {
    VALOR_TOTAL,      // Usuário informa o valor total da compra
    VALOR_DA_PARCELA  // Usuário informa o valor de cada parcela
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddInstallmentDialog(
    tags: List<TagMarker>,
    onDismiss: () -> Unit,
    onConfirm: (
        description: String,
        totalAmount: Double,
        installmentAmount: Double,
        numberOfInstallments: Int,
        tag: String,
        startTimestamp: Long
    ) -> Unit,
    onAddNewTag: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var inputMode by remember { mutableStateOf(InstallmentInputMode.VALOR_TOTAL) }
    var amountText by remember { mutableStateOf("") }
    var numInstallments by remember { androidx.compose.runtime.mutableIntStateOf(10) }
    var selectedTag by remember { mutableStateOf(tags.firstOrNull { it.name.contains("Cartão", ignoreCase = true) }?.name ?: tags.firstOrNull()?.name ?: "Cartão de Crédito") }
    var selectedDateMillis by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cálculos em tempo real
    val inputAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0

    val calculatedTotal: Double
    val calculatedParcel: Double

    if (inputMode == InstallmentInputMode.VALOR_TOTAL) {
        calculatedTotal = inputAmount
        calculatedParcel = if (numInstallments > 0) inputAmount / numInstallments else 0.0
    } else {
        calculatedParcel = inputAmount
        calculatedTotal = inputAmount * numInstallments
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = Color(0xFFF97316)
                )
                Text(
                    text = "Compra Parcelada",
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
                // Alternador de Modo: Valor Total vs Valor da Parcela
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeSelectCard(
                        title = "Valor Total",
                        subtitle = "Dividir total em parcelas",
                        isSelected = inputMode == InstallmentInputMode.VALOR_TOTAL,
                        modifier = Modifier.weight(1f),
                        onClick = { inputMode = InstallmentInputMode.VALOR_TOTAL }
                    )

                    ModeSelectCard(
                        title = "Valor da Parcela",
                        subtitle = "Multiplicar por parcelas",
                        isSelected = inputMode == InstallmentInputMode.VALOR_DA_PARCELA,
                        modifier = Modifier.weight(1f),
                        onClick = { inputMode = InstallmentInputMode.VALOR_DA_PARCELA }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Campo de Valor dependendo do modo
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.replace(",", ".")
                        errorMessage = null
                    },
                    label = {
                        Text(if (inputMode == InstallmentInputMode.VALOR_TOTAL) "Valor Total da Compra (R$)" else "Valor de Cada Parcela (R$)")
                    },
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
                    label = { Text("Descrição da compra") },
                    placeholder = { Text("Ex: Notebook, Smartphone, Seguro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Data da 1ª Parcela
                DatePickerField(
                    selectedDateMillis = selectedDateMillis,
                    onDateSelected = { selectedDateMillis = it },
                    label = "Data da 1ª Parcela"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Contador de Parcelas (com + e -, hold to repeat e toque para digitar)
                InstallmentCounter(
                    value = numInstallments,
                    onValueChange = { numInstallments = it },
                    minValue = 1,
                    maxValue = 60
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Resumo do Parcelamento em Tempo Real
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Resumo do Parcelamento",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Serão geradas $numInstallments parcelas de ${Formatters.formatCurrency(calculatedParcel)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Valor Total da Compra: ${Formatters.formatCurrency(calculatedTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Marcadores / Categorias
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
                    if (desc.isBlank()) {
                        errorMessage = "Informe a descrição da compra"
                    } else if (calculatedTotal <= 0.0 || calculatedParcel <= 0.0) {
                        errorMessage = "Informe um valor válido maior que zero"
                    } else {
                        onConfirm(
                            desc,
                            calculatedTotal,
                            calculatedParcel,
                            numInstallments,
                            selectedTag,
                            selectedDateMillis
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Adicionar Parcelamento")
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
private fun ModeSelectCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
