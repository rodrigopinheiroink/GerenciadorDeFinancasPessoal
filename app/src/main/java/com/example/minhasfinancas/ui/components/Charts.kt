package com.example.minhasfinancas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minhasfinancas.data.model.MonthlySummary
import com.example.minhasfinancas.data.model.TagMarker
import com.example.minhasfinancas.ui.theme.ExpenseRed
import com.example.minhasfinancas.ui.theme.IncomeGreen
import com.example.minhasfinancas.ui.util.Formatters

@Composable
fun MonthlyComparisonBarChart(
    summaries: List<MonthlySummary>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entradas vs Saídas por Mês",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Legenda
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendItem(color = IncomeGreen, label = "Entradas")
                    LegendItem(color = ExpenseRed, label = "Saídas")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (summaries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem dados suficientes para exibir o gráfico",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxVal = summaries.maxOfOrNull { maxOf(it.totalIncome, it.totalExpense) } ?: 0.0
                val safeMax = if (maxVal > 0) maxVal else 1.0

                // Canvas de Barras
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height - 30f // Espaço inferior para o texto do mês
                    val count = summaries.size
                    val groupWidth = width / count
                    val barWidth = (groupWidth * 0.32f).coerceAtMost(24.dp.toPx())
                    val barSpacing = 4.dp.toPx()

                    // Linhas de grade horizontais sutis
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = height * (i / gridSteps.toFloat())
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    summaries.forEachIndexed { index, summary ->
                        val centerX = groupWidth * index + groupWidth / 2f

                        // Altura das barras proporcional ao safeMax
                        val incomeHeight = ((summary.totalIncome / safeMax) * (height - 10f)).toFloat()
                        val expenseHeight = ((summary.totalExpense / safeMax) * (height - 10f)).toFloat()

                        val incomeX = centerX - barWidth - barSpacing / 2f
                        val expenseX = centerX + barSpacing / 2f

                        // Barra de Entrada (Verde)
                        if (incomeHeight > 0f) {
                            drawRoundRect(
                                color = IncomeGreen,
                                topLeft = Offset(incomeX, height - incomeHeight),
                                size = Size(barWidth, incomeHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                            )
                        } else {
                            // Marca sutil de barra zero
                            drawCircle(
                                color = IncomeGreen.copy(alpha = 0.4f),
                                radius = 2.dp.toPx(),
                                center = Offset(incomeX + barWidth / 2f, height - 2f)
                            )
                        }

                        // Barra de Saída (Vermelha)
                        if (expenseHeight > 0f) {
                            drawRoundRect(
                                color = ExpenseRed,
                                topLeft = Offset(expenseX, height - expenseHeight),
                                size = Size(barWidth, expenseHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                            )
                        } else {
                            drawCircle(
                                color = ExpenseRed.copy(alpha = 0.4f),
                                radius = 2.dp.toPx(),
                                center = Offset(expenseX + barWidth / 2f, height - 2f)
                            )
                        }
                    }
                }

                // Rótulos dos meses sob as barras
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    summaries.forEach { summary ->
                        Text(
                            text = Formatters.getShortMonthName(summary.month),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseDonutChart(
    expensesByTag: Map<String, Double>,
    tags: List<TagMarker>,
    modifier: Modifier = Modifier
) {
    val totalExpense = expensesByTag.values.sum()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Gastos por Marcador",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (expensesByTag.isEmpty() || totalExpense <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum gasto registrado neste mês",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val tagColors = tags.associate { it.name to Formatters.parseHexColor(it.colorHex) }
                val defaultPalette = listOf(
                    Color(0xFFF59E0B), Color(0xFF3B82F6), Color(0xFF8B5CF6),
                    Color(0xFFEC4899), Color(0xFFEF4444), Color(0xFF14B8A6),
                    Color(0xFF06B6D4), Color(0xFFF97316), Color(0xFF6366F1)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gráfico de Donut
                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(140.dp)) {
                            var startAngle = -90f
                            val strokeWidth = 26.dp.toPx()

                            expensesByTag.entries.forEachIndexed { index, entry ->
                                val sweepAngle = ((entry.value / totalExpense) * 360f).toFloat()
                                val color = tagColors[entry.key]
                                    ?: defaultPalette[index % defaultPalette.size]

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 2f, // Pequeno espaçamento entre fatias
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        // Texto Central
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatCurrency(totalExpense),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Lista detalhada com porcentagens
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        expensesByTag.entries.sortedByDescending { it.value }.take(5).forEachIndexed { idx, entry ->
                            val color = tagColors[entry.key] ?: defaultPalette[idx % defaultPalette.size]
                            val percent = (entry.value / totalExpense) * 100

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = entry.key,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }

                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", percent)}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
