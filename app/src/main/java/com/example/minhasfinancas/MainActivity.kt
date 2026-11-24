package com.example.minhasfinancas

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.minhasfinancas.data.backup.ProfileBackupManager
import com.example.minhasfinancas.data.repository.FinanceRepository
import com.example.minhasfinancas.ui.screens.HistoryScreen
import com.example.minhasfinancas.ui.screens.HomeScreen
import com.example.minhasfinancas.ui.screens.ProfileManagerDialog
import com.example.minhasfinancas.ui.screens.ProfileOnboardingDialog
import com.example.minhasfinancas.ui.screens.RecurringScreen
import com.example.minhasfinancas.ui.theme.MinhasFinancasTheme
import com.example.minhasfinancas.ui.viewmodel.FinanceViewModel
import com.example.minhasfinancas.ui.viewmodel.FinanceViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: FinanceViewModel

    override fun attachBaseContext(newBase: Context) {
        val locale = MinhasFinancasApp.LOCALE_PT_BR
        java.util.Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MinhasFinancasApp.setAppLocale(this)
        enableEdgeToEdge()

        val repository = FinanceRepository(applicationContext, lifecycleScope)
        val factory = FinanceViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[FinanceViewModel::class.java]

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            MinhasFinancasTheme(darkTheme = isDarkTheme) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun MainAppScreen(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val allProfiles by viewModel.allProfiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    var showProfileManagerDialog by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var selectedScreenIndex by rememberSaveable { mutableIntStateOf(0) }

    val navItems = listOf(
        NavItem(title = "Início", icon = Icons.Default.Home),
        NavItem(title = "Recorrências", icon = Icons.Default.Repeat)
    )

    // Decodifica a foto de perfil salva, se houver
    val profileBitmap = remember(activeProfile?.photoUri) {
        activeProfile?.photoUri?.let { path ->
            val file = File(path)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            } else null
        }
    }

    // Seletor de foto da galeria para o perfil ativo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && activeProfile != null) {
            val currentProfileId = activeProfile!!.id
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val photoFile = File(context.filesDir, "profile_${currentProfileId}.jpg")
                        photoFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        viewModel.updateProfilePhoto(currentProfileId, photoFile.absolutePath)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Seletor de arquivo para importar backup de perfil
    val importProfileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().use { it.readText() }
                    }
                    if (!jsonString.isNullOrBlank()) {
                        val imported = viewModel.importProfile(jsonString)
                        withContext(Dispatchers.Main) {
                            if (imported != null) {
                                Toast.makeText(
                                    context,
                                    "Perfil \"${imported.name}\" importado com sucesso!",
                                    Toast.LENGTH_LONG
                                ).show()
                                showProfileManagerDialog = false
                            } else {
                                Toast.makeText(
                                    context,
                                    "Arquivo de backup inválido ou incompatível.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Erro ao importar perfil: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // ModalNavigationDrawer para o menu de opções lateral
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !showHistoryScreen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                // Cabeçalho do Drawer com o perfil ativo e foto clicável
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap,
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Indicador de câmera sobre o avatar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Selecionar foto",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = activeProfile?.name ?: "Meu Perfil",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Toque no avatar para alterar a foto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opção Dark Mode (alterna com 1 toque)
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            text = if (isDarkTheme) "Modo Claro" else "Modo Escuro",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    badge = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleDarkTheme() }
                        )
                    },
                    selected = false,
                    onClick = {
                        viewModel.toggleDarkTheme()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Opção 1: Perfil (gerenciar, alterar nome, alternar ou criar perfis)
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Perfil", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showProfileManagerDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Opção 2: Histórico (registro imutável de todas as transações)
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Histórico", fontWeight = FontWeight.SemiBold) },
                    selected = showHistoryScreen,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showHistoryScreen = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Opção 3: Sobre
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Sobre", fontWeight = FontWeight.SemiBold) },
                    selected = showAboutDialog,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAboutDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        if (showHistoryScreen) {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { showHistoryScreen = false }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val isSelected = selectedScreenIndex == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedScreenIndex = index },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                when (selectedScreenIndex) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onManageProfiles = { showProfileManagerDialog = true },
                        modifier = Modifier.padding(innerPadding)
                    )
                    1 -> RecurringScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // Modal de Boas-vindas para primeiro uso (Criar Perfil inicial)
    if (allProfiles.isEmpty()) {
        ProfileOnboardingDialog(
            onConfirm = { name ->
                viewModel.createProfile(name)
            }
        )
    }

    // Gerenciador de Perfis (Acessado via Menu -> Perfil)
    if (showProfileManagerDialog) {
        ProfileManagerDialog(
            activeProfile = activeProfile,
            allProfiles = allProfiles,
            onDismiss = { showProfileManagerDialog = false },
            onSwitchProfile = { profile ->
                viewModel.switchProfile(profile)
                showProfileManagerDialog = false
            },
            onCreateProfile = { name ->
                viewModel.createProfile(name)
                showProfileManagerDialog = false
            },
            onUpdateName = { id, newName ->
                viewModel.updateProfileName(id, newName)
            },
            onDeleteProfile = { profileId ->
                viewModel.deleteProfile(profileId)
                if (allProfiles.size <= 1) {
                    showProfileManagerDialog = false
                }
            },
            onExportProfile = {
                if (activeProfile != null) {
                    try {
                        val shareIntent = ProfileBackupManager.createShareIntent(context, activeProfile!!)
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erro ao exportar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onImportProfile = {
                importProfileLauncher.launch("*/*")
            }
        )
    }

    // Modal de Sobre a Aplicação
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Minhas Finanças",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Versão 1.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aplicação criada por",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rodrigo Pinheiro Ink",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Gerenciador Financeiro Pessoal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
