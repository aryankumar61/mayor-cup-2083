package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.components.LoginDialog
import com.example.ui.components.PlayerEditDialog
import com.example.ui.components.PlayerProfileDialog
import com.example.ui.components.PrivateDocumentDialog
import com.example.ui.components.SifarisDialog
import com.example.ui.screens.*
import com.example.ui.theme.CricketPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.TournamentTab
import com.example.ui.viewmodel.TournamentViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TournamentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                TournamentApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentApp(viewModel: TournamentViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allPlayers by viewModel.players.collectAsStateWithLifecycle()
    val visiblePlayers by viewModel.visiblePlayers.collectAsStateWithLifecycle()
    val filteredPlayers by viewModel.filteredPlayers.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val standings by viewModel.standings.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val configs by viewModel.configs.collectAsStateWithLifecycle()
    val uploadedPaperWards by viewModel.uploadedPaperWards.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.currentTab != TournamentTab.PLAYERS) {
        BackHandler {
            viewModel.selectTab(TournamentTab.PLAYERS)
        }
    }

    val pendingCount = remember(allPlayers) {
        allPlayers.count { it.status == "pending" }
    }

    val isAdmin = uiState.currentUser?.role == "admin"

    Scaffold(
        topBar = {
            TopHeaderBar(
                currentUser = uiState.currentUser,
                isSyncing = uiState.isSyncing,
                onLoginClick = { viewModel.openLogin() },
                onLogoutClick = { viewModel.logout() },
                onChangePassClick = { viewModel.openChangePassword() },
                onRefreshClick = { viewModel.syncGoogleSheet() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == TournamentTab.PLAYERS,
                    onClick = { viewModel.selectTab(TournamentTab.PLAYERS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (isAdmin && pendingCount > 0) {
                                    Badge { Text("$pendingCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Group, contentDescription = "Players")
                        }
                    },
                    label = {
                        Text(
                            "Players",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CricketPrimary,
                        selectedTextColor = CricketPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_players")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == TournamentTab.MATCHES,
                    onClick = { viewModel.selectTab(TournamentTab.MATCHES) },
                    icon = { Icon(Icons.Default.SportsCricket, contentDescription = "Matches") },
                    label = {
                        Text(
                            "Matches",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CricketPrimary,
                        selectedTextColor = CricketPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_matches")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == TournamentTab.STANDINGS,
                    onClick = { viewModel.selectTab(TournamentTab.STANDINGS) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Standings") },
                    label = {
                        Text(
                            "Standings",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CricketPrimary,
                        selectedTextColor = CricketPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_standings")
                )

                if (isAdmin) {
                    NavigationBarItem(
                        selected = uiState.currentTab == TournamentTab.ACCOUNTS,
                        onClick = { viewModel.selectTab(TournamentTab.ACCOUNTS) },
                        icon = { Icon(Icons.Default.VpnKey, contentDescription = "Accounts") },
                        label = {
                            Text(
                                "Accounts",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CricketPrimary,
                            selectedTextColor = CricketPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_accounts")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                TournamentTab.PLAYERS -> {
                    PlayersScreen(
                        players = filteredPlayers,
                        allPlayers = visiblePlayers,
                        selectedWard = uiState.selectedWardFilter,
                        statusFilter = uiState.statusFilter,
                        searchQuery = uiState.searchQuery,
                        currentUser = uiState.currentUser,
                        uploadedPaperWards = uploadedPaperWards,
                        onWardSelected = { viewModel.setWardFilter(it) },
                        onStatusFilterSelected = { viewModel.setStatusFilter(it) },
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onPlayerClick = { viewModel.openPlayerProfile(it) },
                        onAddPlayerClick = { viewModel.openAddPlayer() },
                        onOpenSifarisClick = { viewModel.openSifarisDialog() },
                        onExportCsvClick = {
                            val csvContent = viewModel.getCsvData()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Rajdevi Mayor Cup 2083 - Players")
                                putExtra(Intent.EXTRA_TEXT, csvContent)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Players CSV"))
                        }
                    )
                }

                TournamentTab.MATCHES -> {
                    MatchesScreen(
                        matches = matches,
                        currentUser = uiState.currentUser,
                        onAddMatchClick = { viewModel.openAddMatch() },
                        onEditMatchClick = { viewModel.openEditMatch(it) },
                        onDeleteMatchClick = { viewModel.deleteMatch(it) },
                        onAutoSemiFinalClick = { viewModel.autoGenerateKnockouts() }
                    )
                }

                TournamentTab.STANDINGS -> {
                    StandingsScreen(
                        standings = standings,
                        matches = matches,
                        configs = configs,
                        currentUser = uiState.currentUser,
                        onSaveAwardsAndConfig = { champion, runnerup, mom, bestbat, bestbowl, maxovers ->
                            viewModel.saveAwardsAndConfig(champion, runnerup, mom, bestbat, bestbowl, maxovers)
                        }
                    )
                }

                TournamentTab.ACCOUNTS -> {
                    AccountsScreen(
                        accounts = accounts,
                        onAddAccountClick = { viewModel.openAddAccount() },
                        onEditAccountClick = { viewModel.openEditAccount(it) },
                        onDeleteAccountClick = { viewModel.deleteAccount(it) }
                    )
                }
            }
        }
    }

    // Modal Dialogs
    PlayerProfileDialog(
        player = uiState.selectedPlayer,
        currentUser = uiState.currentUser,
        onDismiss = { viewModel.closePlayerProfile() },
        onEdit = { player ->
            viewModel.closePlayerProfile()
            viewModel.openEditPlayer(player)
        },
        onApprove = { id -> viewModel.setPlayerStatus(id, "approved") },
        onReject = { id -> viewModel.setPlayerStatus(id, "rejected") },
        onDelete = { id -> viewModel.deletePlayer(id) },
        onViewCitizenshipDoc = { player ->
            viewModel.viewPrivateDoc(
                kind = "citizen",
                id = player.id,
                title = "${player.name} — Citizenship"
            )
        }
    )

    val captainWard = uiState.currentUser?.ward ?: 0
    val hasWardSifaris = uiState.currentUser?.role != "captain" || (captainWard in uploadedPaperWards)

    PlayerEditDialog(
        isOpen = uiState.isAddEditPlayerOpen,
        player = uiState.playerBeingEdited,
        currentUser = uiState.currentUser,
        hasWardSifaris = hasWardSifaris,
        isSaving = uiState.isSavingPlayer,
        onDismiss = { viewModel.closeAddEditPlayer() },
        onOpenSifaris = {
            viewModel.closeAddEditPlayer()
            viewModel.openSifarisDialog()
        },
        onSave = { updated, citizenDataUrl ->
            viewModel.savePlayer(updated, citizenDataUrl)
        }
    )

    SifarisDialog(
        isOpen = uiState.isSifarisDialogOpen,
        currentUser = uiState.currentUser,
        uploadedWards = uploadedPaperWards,
        isUploading = uiState.isUploadingSifaris,
        onDismiss = { viewModel.closeSifarisDialog() },
        onUploadSifaris = { ward, paperDataUrl ->
            viewModel.uploadSifaris(ward, paperDataUrl)
        },
        onViewSifaris = { ward ->
            viewModel.viewPrivateDoc(
                kind = "sifaris",
                id = ward.toString(),
                title = "Ward $ward Sifaris"
            )
        }
    )

    PrivateDocumentDialog(
        document = uiState.viewingDocument,
        isLoading = uiState.isLoadingDoc,
        onDismiss = { viewModel.closePrivateDoc() }
    )

    MatchEditDialog(
        isOpen = uiState.isAddEditMatchOpen,
        match = uiState.matchBeingEdited,
        isEditingExisting = uiState.isEditingExistingMatch,
        onDismiss = { viewModel.closeAddEditMatch() },
        onSave = { updated -> viewModel.saveMatch(updated) }
    )

    AccountEditDialog(
        isOpen = uiState.isAccountModalOpen,
        account = uiState.accountBeingEdited,
        onDismiss = { viewModel.closeAccountModal() },
        onSave = { updated, isNew -> viewModel.saveAccount(updated, isNew) }
    )

    LoginDialog(
        isOpen = uiState.isLoginOpen,
        isLoggingIn = uiState.isLoggingIn,
        onDismiss = { viewModel.closeLogin() },
        onLogin = { u, p -> viewModel.login(u, p) }
    )

    ChangePasswordDialog(
        isOpen = uiState.isChangePasswordOpen,
        onDismiss = { viewModel.closeChangePassword() },
        onSubmit = { newPass -> viewModel.changePassword(newPass) }
    )

    if (uiState.showIncompleteLeagueConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissIncompleteLeagueConfirm() },
            title = { Text("Create Semi Finals?", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("Some league matches have no result yet. Create semi finals from the current table?") },
            confirmButton = {
                Button(onClick = { viewModel.autoGenerateKnockouts(confirmIncomplete = true) }) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissIncompleteLeagueConfirm() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
