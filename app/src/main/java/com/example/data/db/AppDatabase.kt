package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Player
import com.example.data.model.TournamentAccount
import com.example.data.model.TournamentConfig
import com.example.data.model.TournamentMatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Player::class, TournamentMatch::class, TournamentAccount::class, TournamentConfig::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun accountDao(): AccountDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rajdevi_mayor_cup.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database)
                }
            }
        }
    }
}

suspend fun populateInitialData(database: AppDatabase) {
    val accountDao = database.accountDao()
    val playerDao = database.playerDao()
    val matchDao = database.matchDao()
    val configDao = database.configDao()

    // 1. Default accounts matching Code_1.gs (admin@2083, ward1..ward9)
    val accounts = mutableListOf(
        TournamentAccount("admin", "admin@2083", "Administrator", "admin", 0)
    )
    for (w in 1..9) {
        accounts.add(
            TournamentAccount("ward$w", "ward$w@2083", "Ward $w Captain", "captain", w)
        )
    }
    accountDao.insertAll(accounts)

    // 2. Initial players across the 9 wards (shown until server sync completes)
    val initialPlayers = listOf(
        Player("p101", 1, "7", "Amit Shah", "Birendra Shah", "1999-05-12", "9801234567", "O+", "Rajdevi-1, Pipra", "All-rounder", "Right Hand", "Right Arm Fast", "Captain", "", "approved"),
        Player("p102", 1, "18", "Suraj Sah", "Ram Chandra Sah", "2001-08-20", "9812345678", "B+", "Rajdevi-1, Chowk", "Batsman", "Right Hand", "None", "Vice Captain", "", "approved"),
        Player("p103", 1, "99", "Dhiraj Kushwaha", "Lalan Kushwaha", "2003-02-14", "9823456789", "A+", "Rajdevi-1, Tole", "Bowler", "Right Hand", "Right Arm Spin", "", "", "approved"),
        Player("p201", 2, "10", "Rohit Yadav", "Kameshwar Yadav", "1998-11-04", "9802345671", "B+", "Rajdevi-2, Belbichhiya", "Batsman", "Right Hand", "None", "Captain", "", "approved"),
        Player("p202", 2, "45", "Sunil Paswan", "Rajesh Paswan", "2000-04-18", "9813456782", "AB+", "Rajdevi-2, Ward Chowk", "All-rounder", "Right Hand", "Right Arm Medium", "Vice Captain", "", "approved"),
        Player("p301", 3, "17", "Bikash Chaudhary", "Ganga Chaudhary", "1997-07-30", "9803456712", "A+", "Rajdevi-3, Bhalohiya", "All-rounder", "Left Hand", "Left Arm Fast", "Captain", "", "approved"),
        Player("p401", 4, "1", "Sandeep Giri", "Prem Giri", "1999-03-15", "9804567821", "B+", "Rajdevi-4, Basantpur", "Batsman", "Right Hand", "None", "Captain", "", "approved"),
        Player("p501", 5, "11", "Rameshwar Mandal", "Devendra Mandal", "1998-10-10", "9805678912", "O+", "Rajdevi-5, Pathra", "All-rounder", "Right Hand", "Right Arm Medium", "Captain", "", "approved"),
        Player("p601", 6, "9", "Karan Mahato", "Bishnu Mahato", "2001-09-09", "9806789012", "AB+", "Rajdevi-6, Brahamapuri", "Wicket Keeper", "Right Hand", "None", "Captain", "", "approved"),
        Player("p701", 7, "21", "Santosh Tiwari", "Umesh Tiwari", "1997-04-14", "9807890123", "O+", "Rajdevi-7, Gaur Road", "All-rounder", "Right Hand", "Right Arm Fast", "Captain", "", "approved"),
        Player("p801", 8, "12", "Naresh Ray", "Hira Ray", "1999-12-01", "9808901234", "B-", "Rajdevi-8, Laxmipur", "Batsman", "Right Hand", "None", "Captain", "", "approved"),
        Player("p901", 9, "15", "Pramod Mishra", "Dwarika Mishra", "1998-06-25", "9809012345", "O+", "Rajdevi-9, Madhuban", "All-rounder", "Left Hand", "Left Arm Spin", "Captain", "", "approved")
    )
    playerDao.insertAll(initialPlayers)

    // 3. Initial matches
    val initialMatches = listOf(
        TournamentMatch(1, "League", 1, 2, 88, 4, 10.0, 72, 7, 10.0, "A", "2083-01-10", "Inaugural Match · Ground A"),
        TournamentMatch(2, "League", 3, 4, 104, 3, 10.0, 89, 8, 10.0, "A", "2083-01-11", "High scoring thriller"),
        TournamentMatch(3, "League", 5, 6, 75, 10, 8.4, 76, 3, 7.2, "B", "2083-01-12", "Ward 6 chased comfortably")
    )
    matchDao.insertAll(initialMatches)

    // 4. Default configuration
    val configs = listOf(
        TournamentConfig("maxovers", "10"),
        TournamentConfig("champion", ""),
        TournamentConfig("runnerup", ""),
        TournamentConfig("mom", ""),
        TournamentConfig("bestbat", ""),
        TournamentConfig("bestbowl", ""),
        TournamentConfig("url", "https://script.google.com/macros/s/AKfycbwK-MFE9YzHqI0wJBNia_g-Se7Rp2hAu4leMUH5dW4iKZU_2XLzK8wq2QQrkNM3Hk0/exec")
    )
    configDao.setAll(configs)
}
