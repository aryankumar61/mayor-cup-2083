package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.Player
import com.example.data.model.StandingRow
import com.example.data.model.TournamentAccount
import com.example.data.model.TournamentConfig
import com.example.data.model.TournamentMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlin.math.roundToInt

data class UserSession(
    val username: String,
    val displayName: String,
    val role: String,
    val ward: Int,
    val token: String
) {
    fun toAccount(): TournamentAccount = TournamentAccount(
        username = username,
        password = "",
        displayName = displayName,
        role = role,
        ward = ward
    )
}

data class PrivateDocument(
    val title: String,
    val mime: String,
    val base64Data: String
)

sealed class ApiOutcome<out T> {
    data class Success<T>(val data: T) : ApiOutcome<T>()
    data class ServerError(val message: String, val isAuthExpired: Boolean = false) : ApiOutcome<Nothing>()
    data class NetworkUnavailable(val reason: String) : ApiOutcome<Nothing>()
}

class CricketRepository(
    private val database: AppDatabase,
    context: Context? = null
) {
    companion object {
        const val APP_SCRIPT_WEB_APP_URL =
            "https://script.google.com/macros/s/AKfycbwK-MFE9YzHqI0wJBNia_g-Se7Rp2hAu4leMUH5dW4iKZU_2XLzK8wq2QQrkNM3Hk0/exec"

        private const val PREFS_NAME = "rmc_session_prefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_NAME = "displayName"
        private const val KEY_ROLE = "role"
        private const val KEY_WARD = "ward"
        private const val KEY_TOKEN = "token"
    }

    private val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val playersFlow: Flow<List<Player>> = database.playerDao().getAllPlayersFlow()
    val matchesFlow: Flow<List<TournamentMatch>> = database.matchDao().getAllMatchesFlow()
    val accountsFlow: Flow<List<TournamentAccount>> = database.accountDao().getAllAccountsFlow()
    val configsFlow: Flow<List<TournamentConfig>> = database.configDao().getAllConfigsFlow()

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    fun loadSavedSession(): UserSession? {
        val p = prefs ?: return null
        val username = p.getString(KEY_USERNAME, null) ?: return null
        val displayName = p.getString(KEY_NAME, "") ?: ""
        val role = p.getString(KEY_ROLE, "") ?: ""
        val ward = p.getInt(KEY_WARD, 0)
        val token = p.getString(KEY_TOKEN, "") ?: ""
        if (role.isBlank()) return null
        return UserSession(username, displayName, role, ward, token)
    }

    fun saveSession(session: UserSession?) {
        val p = prefs ?: return
        p.edit().apply {
            if (session == null) {
                clear()
            } else {
                putString(KEY_USERNAME, session.username)
                putString(KEY_NAME, session.displayName)
                putString(KEY_ROLE, session.role)
                putInt(KEY_WARD, session.ward)
                putString(KEY_TOKEN, session.token)
            }
            apply()
        }
    }

    private fun JSONObject.optIntSafe(key: String, default: Int = 0): Int {
        if (!has(key) || isNull(key)) return default
        val raw = opt(key)
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.trim().toDoubleOrNull()?.toInt() ?: default
            else -> default
        }
    }

    private fun JSONObject.optDoubleSafe(key: String, default: Double = 0.0): Double {
        if (!has(key) || isNull(key)) return default
        val raw = opt(key)
        return when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.trim().toDoubleOrNull() ?: default
            else -> default
        }
    }

    private fun JSONObject.optCleanString(key: String, default: String = ""): String {
        if (!has(key) || isNull(key)) return default
        val raw = opt(key)?.toString()?.trim() ?: return default
        return if (raw.equals("null", ignoreCase = true) || raw.isEmpty()) default else raw
    }

    private suspend fun postApi(
        action: String,
        token: String? = null,
        configure: JSONObject.() -> Unit = {}
    ): ApiOutcome<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("action", action)
                if (!token.isNullOrBlank()) {
                    put("token", token)
                }
                configure()
            }.toString()

            val request = Request.Builder()
                .url(APP_SCRIPT_WEB_APP_URL)
                .post(payload.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ApiOutcome.NetworkUnavailable("HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty().trim()
                if (!body.startsWith("{")) {
                    return@withContext ApiOutcome.NetworkUnavailable("Non-JSON response from script endpoint")
                }
                val json = JSONObject(body)
                if (!json.optBoolean("ok", false)) {
                    val err = json.optString("error", "Request failed")
                    return@withContext ApiOutcome.ServerError(
                        message = err,
                        isAuthExpired = err.equals("auth", ignoreCase = true)
                    )
                }
                ApiOutcome.Success(json)
            }
        } catch (e: Exception) {
            ApiOutcome.NetworkUnavailable(e.message ?: "Network unavailable")
        }
    }

    // ---------- Auth ----------

    suspend fun login(username: String, password: String): ApiOutcome<UserSession> {
        val cleanUser = username.trim().lowercase()
        val cleanPass = password.trim()
        return when (val remote = postApi("login") {
            put("user", cleanUser)
            put("pass", cleanPass)
        }) {
            is ApiOutcome.Success -> {
                val json = remote.data
                val token = json.optString("token", "")
                val sess = json.optJSONObject("session")
                val userFromSess = sess?.optString("user", cleanUser)?.ifBlank { cleanUser } ?: cleanUser
                val role = sess?.optString("role", "captain") ?: "captain"
                val name = sess?.optString("name", userFromSess) ?: userFromSess
                val ward = sess?.optIntSafe("ward", 0) ?: 0
                val userSession = UserSession(
                    username = userFromSess,
                    displayName = name,
                    role = role,
                    ward = ward,
                    token = token
                )
                saveSession(userSession)
                ApiOutcome.Success(userSession)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                val local = withContext(Dispatchers.IO) {
                    database.accountDao().getAccount(cleanUser)
                }
                if (local != null && local.password == cleanPass) {
                    val localSession = UserSession(
                        username = local.username,
                        displayName = local.displayName,
                        role = local.role,
                        ward = local.ward,
                        token = ""
                    )
                    saveSession(localSession)
                    ApiOutcome.Success(localSession)
                } else {
                    ApiOutcome.ServerError("Wrong username or password")
                }
            }
        }
    }

    suspend fun logout(token: String?) {
        if (!token.isNullOrBlank()) {
            postApi("logout", token)
        }
        saveSession(null)
    }

    suspend fun changePassword(session: UserSession, newPass: String): ApiOutcome<Unit> {
        val remote = postApi("changePass", session.token) {
            put("pass", newPass)
        }
        return when (remote) {
            is ApiOutcome.Success -> {
                withContext(Dispatchers.IO) {
                    database.accountDao().getAccount(session.username)?.let {
                        database.accountDao().insertOrUpdate(it.copy(password = newPass))
                    }
                }
                ApiOutcome.Success(Unit)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                withContext(Dispatchers.IO) {
                    val existing = database.accountDao().getAccount(session.username)
                        ?: session.toAccount()
                    database.accountDao().insertOrUpdate(existing.copy(password = newPass))
                }
                ApiOutcome.Success(Unit)
            }
        }
    }

    // ---------- Players ----------

    suspend fun savePlayerRemoteAndLocal(
        token: String?,
        player: Player,
        citizenDataUrl: String? = null
    ): ApiOutcome<Player> {
        val playerJson = JSONObject().apply {
            put("id", player.id)
            put("ward", player.ward)
            put("jersey", player.jersey)
            put("name", player.name)
            put("father", player.father)
            put("dob", player.dob)
            put("mobile", player.mobile)
            put("blood", player.blood)
            put("address", player.address)
            put("role", player.role)
            put("bat", player.bat)
            put("bowl", player.bowl)
            put("cap", player.cap)
            put("photo", player.photo)
            put("citizen", player.citizen)
            if (!citizenDataUrl.isNullOrBlank()) {
                put("citizenData", citizenDataUrl)
            }
        }

        return when (val remote = postApi("savePlayer", token) {
            put("player", playerJson)
        }) {
            is ApiOutcome.Success -> {
                val returnedId = remote.data.optString("id", player.id).ifBlank { player.id }
                val returnedPhoto = remote.data.optString("photo", player.photo)
                val returnedStatus = remote.data.optString("status", player.status).ifBlank { player.status }
                val updated = player.copy(
                    id = returnedId,
                    photo = returnedPhoto,
                    status = returnedStatus
                )
                withContext(Dispatchers.IO) {
                    database.playerDao().insertOrUpdate(updated)
                }
                ApiOutcome.Success(updated)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                val localPlayer = if (!citizenDataUrl.isNullOrBlank()) {
                    player.copy(citizenimg = "local_attached")
                } else {
                    player
                }
                withContext(Dispatchers.IO) {
                    database.playerDao().insertOrUpdate(localPlayer)
                }
                ApiOutcome.Success(localPlayer)
            }
        }
    }

    suspend fun setPlayerStatusRemoteAndLocal(token: String?, id: String, status: String): ApiOutcome<Unit> {
        return when (val remote = postApi("setStatus", token) {
            put("id", id)
            put("status", status)
        }) {
            is ApiOutcome.Success -> {
                withContext(Dispatchers.IO) {
                    database.playerDao().updateStatus(id, status)
                }
                ApiOutcome.Success(Unit)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                withContext(Dispatchers.IO) {
                    database.playerDao().updateStatus(id, status)
                }
                ApiOutcome.Success(Unit)
            }
        }
    }

    suspend fun deletePlayerRemoteAndLocal(token: String?, id: String): ApiOutcome<Unit> {
        return when (val remote = postApi("deletePlayer", token) {
            put("id", id)
        }) {
            is ApiOutcome.Success -> {
                withContext(Dispatchers.IO) {
                    database.playerDao().deletePlayer(id)
                }
                ApiOutcome.Success(Unit)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                withContext(Dispatchers.IO) {
                    database.playerDao().deletePlayer(id)
                }
                ApiOutcome.Success(Unit)
            }
        }
    }

    // ---------- Documents (Ward Sifaris & Citizenship Photo) ----------

    suspend fun saveSifaris(token: String?, ward: Int, paperDataUrl: String): ApiOutcome<Unit> {
        return when (val remote = postApi("saveSifaris", token) {
            put("ward", ward)
            put("paperData", paperDataUrl)
        }) {
            is ApiOutcome.Success -> {
                withContext(Dispatchers.IO) {
                    val existing = database.configDao().getConfigValue("papers").orEmpty()
                    val set = existing.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
                    set.add(ward)
                    database.configDao().setConfig(
                        TournamentConfig("papers", set.sorted().joinToString(","))
                    )
                }
                ApiOutcome.Success(Unit)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                withContext(Dispatchers.IO) {
                    val existing = database.configDao().getConfigValue("papers").orEmpty()
                    val set = existing.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
                    set.add(ward)
                    database.configDao().setConfig(
                        TournamentConfig("papers", set.sorted().joinToString(","))
                    )
                }
                ApiOutcome.Success(Unit)
            }
        }
    }

    suspend fun getPrivateDoc(
        token: String?,
        kind: String, // "sifaris" or "citizen"
        id: String,
        title: String
    ): ApiOutcome<PrivateDocument> {
        return when (val remote = postApi("getDoc", token) {
            put("kind", kind)
            put("id", id)
        }) {
            is ApiOutcome.Success -> {
                val mime = remote.data.optString("mime", "image/jpeg")
                val data = remote.data.optString("data", "")
                if (data.isBlank()) {
                    ApiOutcome.ServerError("Document is empty")
                } else {
                    ApiOutcome.Success(PrivateDocument(title = title, mime = mime, base64Data = data))
                }
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> ApiOutcome.ServerError("Connect to internet to view Drive document")
        }
    }

    // ---------- Matches ----------

    suspend fun saveMatchRemoteAndLocal(token: String?, match: TournamentMatch): ApiOutcome<Unit> {
        val matchJson = JSONObject().apply {
            put("no", match.no)
            put("stage", match.stage)
            put("a", match.teamA)
            put("b", match.teamB)
            put("ra", if (match.oversA > 0.0 || match.runsA > 0) match.runsA else "")
            put("wa", if (match.oversA > 0.0 || match.wicketsA > 0) match.wicketsA else "")
            put("oa", if (match.oversA > 0.0) match.oversA else "")
            put("rb", if (match.oversB > 0.0 || match.runsB > 0) match.runsB else "")
            put("wb", if (match.oversB > 0.0 || match.wicketsB > 0) match.wicketsB else "")
            put("ob", if (match.oversB > 0.0) match.oversB else "")
            put("result", match.result)
            put("date", match.date)
            put("note", match.note)
        }

        return when (val remote = postApi("saveMatch", token) {
            put("match", matchJson)
        }) {
            is ApiOutcome.Success -> {
                withContext(Dispatchers.IO) {
                    database.matchDao().insertOrUpdate(match)
                }
                ApiOutcome.Success(Unit)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                withContext(Dispatchers.IO) {
                    database.matchDao().insertOrUpdate(match)
                }
                ApiOutcome.Success(Unit)
            }
        }
    }

    suspend fun deleteMatchRemoteAndLocal(token: String?, no: Int): ApiOutcome<Unit> {
        return when (val remote = postApi("deleteMatch", token) {
            put("no", no)
        }) {
            is ApiOutcome.Success -> {
                withContext(Dispatchers.IO) {
                    database.matchDao().deleteMatch(no)
                }
                ApiOutcome.Success(Unit)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                withContext(Dispatchers.IO) {
                    database.matchDao().deleteMatch(no)
                }
                ApiOutcome.Success(Unit)
            }
        }
    }

    // ---------- Config & Awards ----------

    suspend fun saveConfigsRemoteAndLocal(token: String?, cfgMap: Map<String, String>): ApiOutcome<Unit> {
        val cfgJson = JSONObject()
        cfgMap.forEach { (k, v) -> cfgJson.put(k, v) }

        return when (val remote = postApi("saveCfg", token) {
            put("cfg", cfgJson)
        }) {
            is ApiOutcome.Success -> {
                withContext(Dispatchers.IO) {
                    database.configDao().setAll(cfgMap.map { (k, v) -> TournamentConfig(k, v) })
                }
                ApiOutcome.Success(Unit)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                withContext(Dispatchers.IO) {
                    database.configDao().setAll(cfgMap.map { (k, v) -> TournamentConfig(k, v) })
                }
                ApiOutcome.Success(Unit)
            }
        }
    }

    suspend fun saveConfig(key: String, value: String) = withContext(Dispatchers.IO) {
        database.configDao().setConfig(TournamentConfig(key, value))
    }

    // ---------- Accounts (Admin) ----------

    suspend fun fetchUsers(token: String?): ApiOutcome<Unit> {
        return when (val remote = postApi("listUsers", token)) {
            is ApiOutcome.Success -> {
                val arr = remote.data.optJSONArray("users")
                if (arr != null) {
                    val list = mutableListOf<TournamentAccount>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            TournamentAccount(
                                username = obj.optString("username", ""),
                                password = "",
                                displayName = obj.optString("name", ""),
                                role = obj.optString("role", "captain"),
                                ward = obj.optIntSafe("ward", 0)
                            )
                        )
                    }
                    withContext(Dispatchers.IO) {
                        database.accountDao().replaceAll(list)
                    }
                }
                ApiOutcome.Success(Unit)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> ApiOutcome.Success(Unit)
        }
    }

    suspend fun saveUserRemoteAndLocal(token: String?, account: TournamentAccount): ApiOutcome<Unit> {
        val userJson = JSONObject().apply {
            put("username", account.username)
            put("pass", account.password)
            put("name", account.displayName)
            put("role", account.role)
            put("ward", account.ward)
        }

        return when (val remote = postApi("saveUser", token) {
            put("user", userJson)
        }) {
            is ApiOutcome.Success -> {
                withContext(Dispatchers.IO) {
                    val existing = database.accountDao().getAccount(account.username)
                    val finalPass = account.password.ifBlank { existing?.password ?: "" }
                    database.accountDao().insertOrUpdate(account.copy(password = finalPass))
                }
                ApiOutcome.Success(Unit)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                withContext(Dispatchers.IO) {
                    val existing = database.accountDao().getAccount(account.username)
                    val finalPass = account.password.ifBlank { existing?.password ?: "" }
                    database.accountDao().insertOrUpdate(account.copy(password = finalPass))
                }
                ApiOutcome.Success(Unit)
            }
        }
    }

    suspend fun deleteUserRemoteAndLocal(token: String?, username: String): ApiOutcome<Unit> {
        return when (val remote = postApi("deleteUser", token) {
            put("username", username)
        }) {
            is ApiOutcome.Success -> {
                withContext(Dispatchers.IO) {
                    database.accountDao().deleteAccount(username)
                }
                ApiOutcome.Success(Unit)
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> {
                withContext(Dispatchers.IO) {
                    database.accountDao().deleteAccount(username)
                }
                ApiOutcome.Success(Unit)
            }
        }
    }

    // ---------- Sync List ----------

    suspend fun syncWithGoogleSheets(token: String? = null): ApiOutcome<String> {
        return when (val remote = postApi("list", token)) {
            is ApiOutcome.Success -> {
                val json = remote.data
                withContext(Dispatchers.IO) {
                    if (json.has("players")) {
                        val arr = json.getJSONArray("players")
                        val downloaded = mutableListOf<Player>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            downloaded.add(
                                Player(
                                    id = obj.optCleanString("id", "p_$i"),
                                    ward = obj.optIntSafe("ward", 1),
                                    jersey = obj.optCleanString("jersey", ""),
                                    name = obj.optCleanString("name", "Player"),
                                    father = obj.optCleanString("father", ""),
                                    dob = obj.optCleanString("dob", "").take(10),
                                    mobile = obj.optCleanString("mobile", ""),
                                    blood = obj.optCleanString("blood", ""),
                                    address = obj.optCleanString("address", ""),
                                    role = obj.optCleanString("role", "Batsman"),
                                    bat = obj.optCleanString("bat", "Right Hand"),
                                    bowl = obj.optCleanString("bowl", "None"),
                                    cap = obj.optCleanString("cap", ""),
                                    photo = obj.optCleanString("photo", ""),
                                    status = obj.optCleanString("status", "approved").lowercase(),
                                    citizen = obj.optCleanString("citizen", ""),
                                    citizenimg = obj.optCleanString("citizenimg", ""),
                                    citizenmime = obj.optCleanString("citizenmime", ""),
                                    by = obj.optCleanString("by", ""),
                                    updated = obj.optCleanString("updated", "")
                                )
                            )
                        }
                        database.playerDao().replaceAll(downloaded)
                    }

                    if (json.has("matches")) {
                        val mArr = json.getJSONArray("matches")
                        val downloadedMatches = mutableListOf<TournamentMatch>()
                        for (i in 0 until mArr.length()) {
                            val mObj = mArr.getJSONObject(i)
                            downloadedMatches.add(
                                TournamentMatch(
                                    no = mObj.optIntSafe("no", i + 1),
                                    stage = mObj.optCleanString("stage", "League"),
                                    teamA = mObj.optIntSafe("a", 1),
                                    teamB = mObj.optIntSafe("b", 2),
                                    runsA = mObj.optIntSafe("ra", 0),
                                    wicketsA = mObj.optIntSafe("wa", 0),
                                    oversA = mObj.optDoubleSafe("oa", 0.0),
                                    runsB = mObj.optIntSafe("rb", 0),
                                    wicketsB = mObj.optIntSafe("wb", 0),
                                    oversB = mObj.optDoubleSafe("ob", 0.0),
                                    result = mObj.optCleanString("result", ""),
                                    date = mObj.optCleanString("date", "").take(10),
                                    note = mObj.optCleanString("note", "")
                                )
                            )
                        }
                        database.matchDao().replaceAll(downloadedMatches)
                    }

                    val cfgList = mutableListOf<TournamentConfig>()
                    if (json.has("cfg")) {
                        val cfgObj = json.optJSONObject("cfg")
                        if (cfgObj != null) {
                            val keys = cfgObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                cfgList.add(TournamentConfig(k, cfgObj.optCleanString(k, "")))
                            }
                        }
                    }
                    if (json.has("papers")) {
                        val pArr = json.optJSONArray("papers")
                        val wards = mutableListOf<Int>()
                        if (pArr != null) {
                            for (i in 0 until pArr.length()) {
                                val w = pArr.opt(i).toString().toDoubleOrNull()?.toInt()
                                if (w != null && w in 1..9) wards.add(w)
                            }
                        }
                        cfgList.add(TournamentConfig("papers", wards.distinct().sorted().joinToString(",")))
                    }
                    if (cfgList.isNotEmpty()) {
                        database.configDao().setAll(cfgList)
                    }
                }
                ApiOutcome.Success("Synced with Google Sheets")
            }
            is ApiOutcome.ServerError -> remote
            is ApiOutcome.NetworkUnavailable -> remote
        }
    }

    // ---------- Standings & CSV ----------

    private fun oversToBallsFraction(overs: Double): Double {
        val completedOvers = floor(overs)
        val balls = ((overs % 1.0) * 10.0).roundToInt()
        return completedOvers + (balls / 6.0)
    }

    fun calculateStandings(
        matches: List<TournamentMatch>,
        maxOversSetting: Double = 10.0
    ): List<StandingRow> {
        val leagueMatches = matches.filter { it.stage.equals("League", ignoreCase = true) }

        class WardStats(val ward: Int) {
            var played = 0
            var won = 0
            var lost = 0
            var tie = 0
            var runsFor = 0
            var oversForFraction = 0.0
            var runsAgainst = 0
            var oversAgainstFraction = 0.0
        }

        val tableMap = (1..9).associateWith { WardStats(it) }

        for (match in leagueMatches) {
            val result = match.calculatedResult()
            if (result.isEmpty()) continue

            val teamA = tableMap[match.teamA]
            val teamB = tableMap[match.teamB]
            if (teamA == null || teamB == null) continue

            teamA.played++
            teamB.played++

            when (result) {
                "A" -> {
                    teamA.won++
                    teamB.lost++
                }
                "B" -> {
                    teamB.won++
                    teamA.lost++
                }
                "tie", "nr" -> {
                    teamA.tie++
                    teamB.tie++
                }
            }

            if (result != "nr") {
                val oversAFraction = if (match.wicketsA >= 10 && maxOversSetting > 0) {
                    maxOversSetting
                } else {
                    oversToBallsFraction(match.oversA)
                }

                val oversBFraction = if (match.wicketsB >= 10 && maxOversSetting > 0) {
                    maxOversSetting
                } else {
                    oversToBallsFraction(match.oversB)
                }

                teamA.runsFor += match.runsA
                teamA.oversForFraction += oversAFraction
                teamA.runsAgainst += match.runsB
                teamA.oversAgainstFraction += oversBFraction

                teamB.runsFor += match.runsB
                teamB.oversForFraction += oversBFraction
                teamB.runsAgainst += match.runsA
                teamB.oversAgainstFraction += oversAFraction
            }
        }

        return tableMap.values.map { s ->
            val points = (s.won * 2) + s.tie
            val nrr = if (s.oversForFraction > 0 && s.oversAgainstFraction > 0) {
                (s.runsFor / s.oversForFraction) - (s.runsAgainst / s.oversAgainstFraction)
            } else 0.0

            StandingRow(
                ward = s.ward,
                played = s.played,
                won = s.won,
                lost = s.lost,
                tie = s.tie,
                runsFor = s.runsFor,
                oversFor = s.oversForFraction,
                runsAgainst = s.runsAgainst,
                oversAgainst = s.oversAgainstFraction,
                points = points,
                nrr = nrr
            )
        }.sortedWith(
            compareByDescending<StandingRow> { it.points }
                .thenByDescending { it.nrr }
                .thenBy { it.ward }
        )
    }

    fun generatePlayersCsv(players: List<Player>): String {
        val headers = listOf(
            "ward", "jersey", "name", "father", "dob",
            "mobile", "blood", "address", "role", "bat", "bowl", "cap", "citizen", "status"
        )
        val sb = StringBuilder("\uFEFF")
        sb.append(headers.joinToString(",")).append("\n")
        players.sortedWith(compareBy({ it.ward }, { it.jersey.toIntOrNull() ?: 999 }))
            .forEach { p ->
                val row = listOf(
                    p.ward.toString(),
                    p.jersey,
                    p.name,
                    p.father,
                    p.dob,
                    p.mobile,
                    p.blood,
                    p.address,
                    p.role,
                    p.bat,
                    p.bowl,
                    p.cap,
                    p.citizen,
                    p.status
                )
                sb.append(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }).append("\n")
            }
        return sb.toString()
    }
}
