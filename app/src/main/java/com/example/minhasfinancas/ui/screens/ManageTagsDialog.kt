package com.example.minhasfinancas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minhasfinancas.data.model.TagMarker
import com.example.minhasfinancas.ui.theme.ExpenseRed
import com.example.minhasfinancas.ui.util.Formatters

private val tagPaletteColors = listOf(
    "#EF4444", "#F97316", "#F59E0B", "#10B981", "#14B8A6",
    "#06B6D4", "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899",
    "#64748B", "#475569"
)

@Composable
fun ManageTagsDialog(
    tags: List<TagMarker>,
    onDismiss: () -> Unit,
    onAddNewTag: () -> Unit,
    onUpdateTag: (id: Long, oldName: String, newName: String, newColorHex: String) -> Unit,
    onDeleteTag: (id: Long, name: String) -> Unit,
    onReorderTags: (List<TagMarker>) -> Unit,
    onToggleVisibility: (id: Long, isVisible: Boolean) -> Unit
) {
    var editingTag by remember { mutableStateOf<TagMarker?>(null) }
    var tagToDelete by remember { mutableStateOf<TagMarker?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sell,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Gerenciar Marcadores",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Altere a ordem ou oculte/exiba marcadores na Home. Edite também nome, cor ou exclua.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(tags, key = { _, tag -> tag.id }) { index, tag ->
                        val color = Formatters.parseHexColor(tag.colorHex)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tag.isVisibleOnHome) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Ponto de cor e Nome do marcador
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(if (tag.isVisibleOnHome) color else color.copy(alpha = 0.35f))
                                    )
                                    Column {
                                        Text(
                                            text = tag.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (tag.isVisibleOnHome) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        )
                                        if (!tag.isVisibleOnHome) {
                                            Text(
                                                text = "Oculto na Home",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }

                                // Controles: Ocultar/Exibir, Subir / Descer, Editar, Excluir
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // Alternar Visibilidade na Home
                                    IconButton(
                                        onClick = { onToggleVisibility(tag.id, !tag.isVisibleOnHome) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (tag.isVisibleOnHome) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (tag.isVisibleOnHome) "Ocultar da Home" else "Exibir na Home",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (tag.isVisibleOnHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    }

                                    // Mover para Cima
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = tags.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index - 1]
                                                mutable[index - 1] = temp
                                                onReorderTags(mutable)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Mover para cima",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }

                                    // Mover para Baixo
                                    IconButton(
                                        onClick = {
                                            if (index < tags.size - 1) {
                                                val mutable = tags.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index + 1]
                                                mutable[index + 1] = temp
                                                onReorderTags(mutable)
                                            }
                                        },
                                        enabled = index < tags.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Mover para baixo",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (index < tags.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }

                                    // Editar Nome e Cor
                                    IconButton(
                                        onClick = { editingTag = tag },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar marcador",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Excluir Marcador
                                    IconButton(
                                        onClick = { tagToDelete = tag },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir marcador",
                                            modifier = Modifier.size(16.dp),
                                            tint = ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        onAddNewTag()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Criar Novo Marcador")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )

    // Modal de Edição de Marcador (Nome e Cor)
    if (editingTag != null) {
        val target = editingTag!!
        var editedName by remember(target) { mutableStateOf(target.name) }
        var editedColor by remember(target) { mutableStateOf(target.colorHex) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingTag = null },
            title = {
                Text(
                    text = "Editar Marcador",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = {
                            editedName = it
                            errorMessage = null
                        },
                        label = { Text("Nome do marcador") },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Escolha uma cor:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(90.dp)
                    ) {
                        items(tagPaletteColors) { colorHex ->
                            val color = Formatters.parseHexColor(colorHex)
                            val isSelected = editedColor.equals(colorHex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editedColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editedName.trim()
                        if (clean.isBlank()) {
                            errorMessage = "Informe o nome do marcador"
                        } else {
                            onUpdateTag(target.id, target.name, clean, editedColor)
                            editingTag = null
                        }
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTag = null }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modal de Confirmação para Excluir Marcador
    if (tagToDelete != null) {
        val target = tagToDelete!!
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ExpenseRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Excluir Marcador?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Tem certeza de que deseja excluir o marcador \"${target.name}\"? Se houver movimentações vinculadas a este marcador, elas serão migradas para \"Outros\".",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTag(target.id, target.name)
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Janela dedicada exclusivamente para o visor "Filtrar por Marcador" da Home Page.
 * Possui APENAS 2 funcionalidades de visualização:
 * 1) Escolher quais marcadores irão aparecer (mostrar/ocultar da Home Page)
 * 2) Alterar a ordem de exibição dos marcadores na Home Page
 * Esta tela NÃO faz nenhuma alteração nem exclusão nos dados criados pelo usuário.
 */
@Composable
fun ManageVisorMarkersDialog(
    tags: List<TagMarker>,
    onDismiss: () -> Unit,
    onToggleVisibility: (id: Long, isVisible: Boolean) -> Unit,
    onReorderTags: (List<TagMarker>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Exibição dos Marcadores",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Escolha quais marcadores aparecem no visor da Home e a ordem de exibição. Esta tela não altera ou exclui nenhum dado das suas finanças.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(tags, key = { _, tag -> tag.id }) { index, tag ->
                        val color = Formatters.parseHexColor(tag.colorHex)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tag.isVisibleOnHome) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Ponto de cor e Nome do marcador
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(if (tag.isVisibleOnHome) color else color.copy(alpha = 0.3f))
                                    )
                                    Column {
                                        Text(
                                            text = tag.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (tag.isVisibleOnHome) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        )
                                        if (!tag.isVisibleOnHome) {
                                            Text(
                                                text = "Oculto na Home",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }

                                // Controles: Apenas 2 funções (Visibilidade e Ordem)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // 1ª Função: Quais marcadores irão aparecer (Visibilidade)
                                    IconButton(
                                        onClick = { onToggleVisibility(tag.id, !tag.isVisibleOnHome) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (tag.isVisibleOnHome) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (tag.isVisibleOnHome) "Ocultar da Home" else "Exibir na Home",
                                            modifier = Modifier.size(20.dp),
                                            tint = if (tag.isVisibleOnHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    }

                                    // 2ª Função: Ordem de exibição na Home (Mover para cima)
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = tags.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index - 1]
                                                mutable[index - 1] = temp
                                                onReorderTags(mutable)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Mover para cima",
                                            modifier = Modifier.size(20.dp),
                                            tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }

                                    // 2ª Função: Ordem de exibição na Home (Mover para baixo)
                                    IconButton(
                                        onClick = {
                                            if (index < tags.size - 1) {
                                                val mutable = tags.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index + 1]
                                                mutable[index + 1] = temp
                                                onReorderTags(mutable)
                                            }
                                        },
                                        enabled = index < tags.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Mover para baixo",
                                            modifier = Modifier.size(20.dp),
                                            tint = if (index < tags.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Concluir")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
