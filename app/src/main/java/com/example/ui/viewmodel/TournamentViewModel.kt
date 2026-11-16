package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Player
import com.example.data.model.StandingRow
import com.example.data.model.TournamentAccount
import com.example.data.model.TournamentMatch
import com.example.data.repository.ApiOutcome
import com.example.data.repository.CricketRepository
import com.example.data.repository.UserSession
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class TournamentTab {
    PLAYERS, MATCHES, STANDINGS, ACCOUNTS
}

data class UiState(
    val currentTab: TournamentTab = TournamentTab.PLAYERS,
    val selectedWardFilter: Int = 0, // 0 = All, 1..9
    val statusFilter: String = "", // "", "pending", "approved", "rejected"
    val searchQuery: String = "",
    val currentUser: TournamentAccount? = null,
    val authToken: String = "",
    val selectedPlayer: Player? = null,
    val isAddEditPlayerOpen: Boolean = false,
    val playerBeingEdited: Player? = null,
    val isSavingPlayer: Boolean = false,
    val isLoginOpen: Boolean = false,
    val isLoggingIn: Boolean = false,
    val isAddEditMatchOpen: Boolean = false,
    val matchBeingEdited: TournamentMatch? = null,
    val isEditingExistingMatch: Boolean = false,
    val isAccountModalOpen: Boolean = false,
    val accountBeingEdited: TournamentAccount? = null,
    val isChangePasswordOpen: Boolean = false,
    val showIncompleteLeagueConfirm: Boolean = false,
    val snackbarMessage: String? = null,
    val isSyncing: Boolean = false
)

class TournamentViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = CricketRepository(database, application)

    val players: StateFlow<List<Player>> = repository.playersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matches: StateFlow<List<TournamentMatch>> = repository.matchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<TournamentAccount>> = repository.accountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val configs: StateFlow<Map<String, String>> = repository.configsFlow
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val savedSession = repository.loadSavedSession()
        if (savedSession != null) {
            _uiState.update {
                it.copy(
                    currentUser = savedSession.toAccount(),
                    authToken = savedSession.token
                )
            }
        }

        viewModelScope.launch {
            if (database.accountDao().getCount() == 0) {
                com.example.data.db.populateInitialData(database)
            }
            repository.saveConfig("url", CricketRepository.APP_SCRIPT_WEB_APP_URL)
            syncWithServer(silentOnOffline = true)
        }
    }

    val standings: StateFlow<List<StandingRow>> = combine(matches, configs) { matchList, cfg ->
        val maxOvers = cfg["maxovers"]?.toDoubleOrNull() ?: 10.0
        repository.calculateStandings(matchList, maxOvers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Players visible to the current role:
     * - Public: only approved players
     * - Captain: approved players + own ward players
     * - Admin: all players
     */
    val visiblePlayers: StateFlow<List<Player>> = combine(
        players,
        _uiState.map { it.currentUser }.distinctUntilChanged()
    ) { allPlayers, user ->
        when {
            user?.role == "admin" -> allPlayers
            user?.role == "captain" -> allPlayers.filter {
                it.status.equals("approved", ignoreCase = true) || it.ward == user.ward
            }
            else -> allPlayers.filter { it.status.equals("approved", ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPlayers: StateFlow<List<Player>> = combine(
        visiblePlayers,
        _uiState.map { it.selectedWardFilter }.distinctUntilChanged(),
        _uiState.map { it.statusFilter }.distinctUntilChanged(),
        _uiState.map { it.searchQuery }.distinctUntilChanged()
    ) { visible, ward, status, query ->
        val q = query.trim().lowercase()
        visible.filter { p ->
            (ward == 0 || p.ward == ward) &&
                (status.isEmpty() || p.status.equals(status, ignoreCase = true)) &&
                (q.isEmpty() ||
                    p.name.lowercase().contains(q) ||
                    p.father.lowercase().contains(q) ||
                    p.mobile.contains(q) ||
                    p.jersey.contains(q) ||
                    p.address.lowercase().contains(q))
        }.sortedWith(compareBy({ it.ward }, { it.jersey.toIntOrNull() ?: 999 }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun handleAuthExpired() {
        repository.saveSession(null)
        _uiState.update {
            it.copy(
                currentUser = null,
                authToken = "",
                statusFilter = "",
                currentTab = if (it.currentTab == TournamentTab.ACCOUNTS) TournamentTab.PLAYERS else it.currentTab
            )
        }
        showSnackbar("Session expired - login again")
        viewModelScope.launch {
            syncWithServer(silentOnOffline = true)
        }
    }

    private fun <T> handleOutcome(
        outcome: ApiOutcome<T>,
        silentOnOffline: Boolean = false,
        onSuccess: (T) -> Unit
    ) {
        when (outcome) {
            is ApiOutcome.Success -> onSuccess(outcome.data)
            is ApiOutcome.ServerError -> {
                if (outcome.isAuthExpired && _uiState.value.currentUser != null) {
                    handleAuthExpired()
                } else {
                    showSnackbar(outcome.message)
                }
            }
            is ApiOutcome.NetworkUnavailable -> {
                if (!silentOnOffline) {
                    showSnackbar("Offline mode: ${outcome.reason}")
                }
            }
        }
    }

    fun selectTab(tab: TournamentTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == TournamentTab.ACCOUNTS && _uiState.value.currentUser?.role == "admin") {
            loadUsers()
        }
    }

    fun setWardFilter(ward: Int) {
        _uiState.update { it.copy(selectedWardFilter = ward) }
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun showSnackbar(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // ---------- Auth ----------

    fun openLogin() = _uiState.update { it.copy(isLoginOpen = true) }
    fun closeLogin() = _uiState.update { it.copy(isLoginOpen = false) }

    fun login(user: String, pass: String) {
        if (user.isBlank() || pass.isBlank()) {
            showSnackbar("Enter username and password")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true) }
            val outcome = repository.login(user, pass)
            _uiState.update { it.copy(isLoggingIn = false) }
            handleOutcome(outcome) { session ->
                _uiState.update {
                    it.copy(
                        currentUser = session.toAccount(),
                        authToken = session.token,
                        isLoginOpen = false
                    )
                }
                showSnackbar("Welcome ${session.displayName}")
                viewModelScope.launch { syncWithServer(silentOnOffline = true) }
            }
        }
    }

    fun logout() {
        val token = _uiState.value.authToken
        viewModelScope.launch {
            repository.logout(token)
            _uiState.update {
                it.copy(
                    currentUser = null,
                    authToken = "",
                    statusFilter = "",
                    currentTab = if (it.currentTab == TournamentTab.ACCOUNTS) TournamentTab.PLAYERS else it.currentTab
                )
            }
            showSnackbar("Logged out")
            syncWithServer(silentOnOffline = true)
        }
    }

    fun openChangePassword() = _uiState.update { it.copy(isChangePasswordOpen = true) }
    fun closeChangePassword() = _uiState.update { it.copy(isChangePasswordOpen = false) }

    fun changePassword(newPass: String) {
        val current = _uiState.value.currentUser ?: return
        if (newPass.length < 6) {
            showSnackbar("New password must be at least 6 characters")
            return
        }
        val session = UserSession(
            username = current.username,
            displayName = current.displayName,
            role = current.role,
            ward = current.ward,
            token = _uiState.value.authToken
        )
        viewModelScope.launch {
            val outcome = repository.changePassword(session, newPass)
            handleOutcome(outcome) {
                _uiState.update { it.copy(isChangePasswordOpen = false) }
                showSnackbar("Password changed")
            }
        }
    }

    // ---------- Players ----------

    fun openPlayerProfile(player: Player) {
        _uiState.update { it.copy(selectedPlayer = player) }
    }

    fun closePlayerProfile() {
        _uiState.update { it.copy(selectedPlayer = null) }
    }

    fun openAddPlayer() {
        val user = _uiState.value.currentUser
        val initialWard = if (user?.role == "captain") {
            user.ward
        } else if (_uiState.value.selectedWardFilter > 0) {
            _uiState.value.selectedWardFilter
        } else {
            1
        }
        val newPlayer = Player(
            id = UUID.randomUUID().toString(),
            ward = initialWard,
            name = "",
            status = if (user?.role == "captain") "pending" else "approved"
        )
        _uiState.update { it.copy(isAddEditPlayerOpen = true, playerBeingEdited = newPlayer) }
    }

    fun openEditPlayer(player: Player) {
        _uiState.update { it.copy(isAddEditPlayerOpen = true, playerBeingEdited = player) }
    }

    fun closeAddEditPlayer() {
        _uiState.update { it.copy(isAddEditPlayerOpen = false, playerBeingEdited = null) }
    }

    fun savePlayer(player: Player) {
        if (player.name.isBlank()) {
            showSnackbar("Player name required")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPlayer = true) }
            val user = _uiState.value.currentUser
            val defaultStatus = if (user?.role == "captain") "pending" else player.status
            val prepared = player.copy(status = defaultStatus)
            val outcome = repository.savePlayerRemoteAndLocal(_uiState.value.authToken, prepared)
            _uiState.update { it.copy(isSavingPlayer = false) }

            handleOutcome(outcome) { savedStatus ->
                val finalPlayer = prepared.copy(status = savedStatus)
                closeAddEditPlayer()
                if (_uiState.value.selectedPlayer?.id == finalPlayer.id) {
                    _uiState.update { it.copy(selectedPlayer = finalPlayer) }
                }
                showSnackbar(
                    if (savedStatus == "pending") "Saved - waiting for admin approval"
                    else "Saved"
                )
                viewModelScope.launch { syncWithServer(silentOnOffline = true) }
            }
        }
    }

    fun setPlayerStatus(id: String, status: String) {
        viewModelScope.launch {
            val outcome = repository.setPlayerStatusRemoteAndLocal(_uiState.value.authToken, id, status)
            handleOutcome(outcome) {
                closePlayerProfile()
                showSnackbar(if (status == "approved") "Approved ✓" else "Rejected")
                viewModelScope.launch { syncWithServer(silentOnOffline = true) }
            }
        }
    }

    fun deletePlayer(id: String) {
        viewModelScope.launch {
            val outcome = repository.deletePlayerRemoteAndLocal(_uiState.value.authToken, id)
            handleOutcome(outcome) {
                closePlayerProfile()
                showSnackbar("Player deleted")
                viewModelScope.launch { syncWithServer(silentOnOffline = true) }
            }
        }
    }

    // ---------- Matches ----------

    fun openAddMatch() {
        val nextNo = (matches.value.maxOfOrNull { it.no } ?: 0) + 1
        val newMatch = TournamentMatch(
            no = nextNo,
            stage = "League",
            teamA = 1,
            teamB = 2
        )
        _uiState.update {
            it.copy(
                isAddEditMatchOpen = true,
                matchBeingEdited = newMatch,
                isEditingExistingMatch = false
            )
        }
    }

    fun openEditMatch(match: TournamentMatch) {
        _uiState.update {
            it.copy(
                isAddEditMatchOpen = true,
                matchBeingEdited = match,
                isEditingExistingMatch = true
            )
        }
    }

    fun closeAddEditMatch() {
        _uiState.update {
            it.copy(
                isAddEditMatchOpen = false,
                matchBeingEdited = null,
                isEditingExistingMatch = false
            )
        }
    }

    fun saveMatch(match: TournamentMatch) {
        viewModelScope.launch {
            val outcome = repository.saveMatchRemoteAndLocal(_uiState.value.authToken, match)
            handleOutcome(outcome) {
                closeAddEditMatch()
                showSnackbar("Match saved")
                viewModelScope.launch { syncWithServer(silentOnOffline = true) }
            }
        }
    }

    fun deleteMatch(no: Int) {
        viewModelScope.launch {
            val outcome = repository.deleteMatchRemoteAndLocal(_uiState.value.authToken, no)
            handleOutcome(outcome) {
                showSnackbar("Match $no deleted")
                viewModelScope.launch { syncWithServer(silentOnOffline = true) }
            }
        }
    }

    fun dismissIncompleteLeagueConfirm() {
        _uiState.update { it.copy(showIncompleteLeagueConfirm = false) }
    }

    fun autoGenerateKnockouts(confirmIncomplete: Boolean = false) {
        viewModelScope.launch {
            val currentMatches = matches.value
            val currentStandings = standings.value
            val leagueMatches = currentMatches.filter { it.stage.equals("League", ignoreCase = true) }
            val semiMatches = currentMatches.filter { it.stage.equals("Semi Final", ignoreCase = true) }
            val finalMatches = currentMatches.filter { it.stage.equals("Final", ignoreCase = true) }

            if (leagueMatches.isEmpty()) {
                showSnackbar("Add league matches first")
                return@launch
            }

            var nextNo = (currentMatches.maxOfOrNull { it.no } ?: 0) + 1

            if (semiMatches.isEmpty()) {
                if (!confirmIncomplete && leagueMatches.any { it.calculatedResult().isEmpty() }) {
                    _uiState.update { it.copy(showIncompleteLeagueConfirm = true) }
                    return@launch
                }
                _uiState.update { it.copy(showIncompleteLeagueConfirm = false) }

                if (currentStandings.size < 4) {
                    showSnackbar("Need at least 4 teams in standings")
                    return@launch
                }
                val sf1 = TournamentMatch(
                    no = nextNo++,
                    stage = "Semi Final",
                    teamA = currentStandings[0].ward,
                    teamB = currentStandings[3].ward,
                    note = "SF1: 1st vs 4th"
                )
                val sf2 = TournamentMatch(
                    no = nextNo,
                    stage = "Semi Final",
                    teamA = currentStandings[1].ward,
                    teamB = currentStandings[2].ward,
                    note = "SF2: 2nd vs 3rd"
                )
                val token = _uiState.value.authToken
                val o1 = repository.saveMatchRemoteAndLocal(token, sf1)
                if (o1 is ApiOutcome.ServerError) {
                    handleOutcome(o1) {}
                    return@launch
                }
                val o2 = repository.saveMatchRemoteAndLocal(token, sf2)
                handleOutcome(o2) {
                    showSnackbar("Semi Finals created")
                    viewModelScope.launch { syncWithServer(silentOnOffline = true) }
                }
            } else if (finalMatches.isEmpty()) {
                if (semiMatches.size < 2 || semiMatches.any { it.calculatedWinner() == 0 }) {
                    showSnackbar("Complete both semi-final results first")
                    return@launch
                }
                val win1 = semiMatches[0].calculatedWinner()
                val win2 = semiMatches[1].calculatedWinner()
                val finalMatch = TournamentMatch(
                    no = nextNo,
                    stage = "Final",
                    teamA = win1,
                    teamB = win2,
                    note = "Final"
                )
                val outcome = repository.saveMatchRemoteAndLocal(_uiState.value.authToken, finalMatch)
                handleOutcome(outcome) {
                    showSnackbar("Final created")
                    viewModelScope.launch { syncWithServer(silentOnOffline = true) }
                }
            } else {
                showSnackbar("Final already created")
            }
        }
    }

    // ---------- Config & Awards ----------

    fun saveAwardsAndConfig(
        champion: String,
        runnerup: String,
        mom: String,
        bestbat: String,
        bestbowl: String,
        maxovers: String
    ) {
        viewModelScope.launch {
            val map = mapOf(
                "champion" to champion.trim(),
                "runnerup" to runnerup.trim(),
                "mom" to mom.trim(),
                "bestbat" to bestbat.trim(),
                "bestbowl" to bestbowl.trim(),
                "maxovers" to maxovers.trim(),
                "url" to CricketRepository.APP_SCRIPT_WEB_APP_URL
            )
            val outcome = repository.saveConfigsRemoteAndLocal(_uiState.value.authToken, map)
            handleOutcome(outcome) {
                showSnackbar("Saved ✓")
                viewModelScope.launch { syncWithServer(silentOnOffline = true) }
            }
        }
    }

    // ---------- Accounts (Admin) ----------

    fun loadUsers() {
        viewModelScope.launch {
            val outcome = repository.fetchUsers(_uiState.value.authToken)
            handleOutcome(outcome, silentOnOffline = true) {}
        }
    }

    fun openAddAccount() {
        val newAccount = TournamentAccount(
            username = "",
            password = "",
            displayName = "",
            role = "captain",
            ward = 1
        )
        _uiState.update { it.copy(isAccountModalOpen = true, accountBeingEdited = newAccount) }
    }

    fun openEditAccount(account: TournamentAccount) {
        _uiState.update { it.copy(isAccountModalOpen = true, accountBeingEdited = account) }
    }

    fun closeAccountModal() {
        _uiState.update { it.copy(isAccountModalOpen = false, accountBeingEdited = null) }
    }

    fun saveAccount(account: TournamentAccount, isNew: Boolean) {
        if (account.username.isBlank() || account.displayName.isBlank()) {
            showSnackbar("Username and display name are required")
            return
        }
        if (isNew && account.password.isBlank()) {
            showSnackbar("Password is required for new account")
            return
        }
        viewModelScope.launch {
            val outcome = repository.saveUserRemoteAndLocal(_uiState.value.authToken, account)
            handleOutcome(outcome) {
                closeAccountModal()
                showSnackbar("Saved")
                loadUsers()
            }
        }
    }

    fun deleteAccount(username: String) {
        if (username.equals("admin", ignoreCase = true)) {
            showSnackbar("Cannot delete primary admin")
            return
        }
        viewModelScope.launch {
            val outcome = repository.deleteUserRemoteAndLocal(_uiState.value.authToken, username)
            handleOutcome(outcome) {
                showSnackbar("Account deleted")
                loadUsers()
            }
        }
    }

    // ---------- Cloud Sync ----------

    fun syncGoogleSheet() {
        viewModelScope.launch {
            syncWithServer(silentOnOffline = false)
        }
    }

    private suspend fun syncWithServer(silentOnOffline: Boolean) {
        _uiState.update { it.copy(isSyncing = true) }
        val outcome = repository.syncWithGoogleSheets(_uiState.value.authToken)
        _uiState.update { it.copy(isSyncing = false) }
        handleOutcome(outcome, silentOnOffline = silentOnOffline) { msg ->
            if (!silentOnOffline) {
                showSnackbar(msg)
            }
        }
    }

    fun getCsvData(): String {
        return repository.generatePlayersCsv(players.value)
    }
}
