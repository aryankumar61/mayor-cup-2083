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
    version = 1,
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
                ).addCallback(DatabaseCallback()).build()
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

    // 1. Initial accounts
    val accounts = mutableListOf(
        TournamentAccount("admin", "admin123", "Organizing Committee", "admin", 0)
    )
    for (w in 1..9) {
        accounts.add(
            TournamentAccount("captain$w", "ward123", "Captain Ward $w", "captain", w)
        )
    }
    accountDao.insertAll(accounts)

    // 2. Initial realistic players across the 9 wards
    val initialPlayers = listOf(
        Player("p101", 1, "7", "Amit Shah", "Birendra Shah", "1999-05-12", "9801234567", "O+", "Rajdevi-1, Pipra", "All-rounder", "Right Hand", "Right Arm Fast", "Captain", "", "approved"),
        Player("p102", 1, "18", "Suraj Sah", "Ram Chandra Sah", "2001-08-20", "9812345678", "B+", "Rajdevi-1, Chowk", "Batsman", "Right Hand", "None", "Vice Captain", "", "approved"),
        Player("p103", 1, "99", "Dhiraj Kushwaha", "Lalan Kushwaha", "2003-02-14", "9823456789", "A+", "Rajdevi-1, Tole", "Bowler", "Right Hand", "Right Arm Spin", "", "", "approved"),
        
        Player("p201", 2, "10", "Rohit Yadav", "Kameshwar Yadav", "1998-11-04", "9802345671", "B+", "Rajdevi-2, Belbichhiya", "Batsman", "Right Hand", "None", "Captain", "", "approved"),
        Player("p202", 2, "45", "Sunil Paswan", "Rajesh Paswan", "2000-04-18", "9813456782", "AB+", "Rajdevi-2, Ward Chowk", "All-rounder", "Right Hand", "Right Arm Medium", "Vice Captain", "", "approved"),
        Player("p203", 2, "23", "Deepak Raut", "Bhagwan Raut", "2002-09-25", "9824567893", "O-", "Rajdevi-2, Paschim Tole", "Wicket Keeper", "Left Hand", "None", "", "", "approved"),

        Player("p301", 3, "17", "Bikash Chaudhary", "Ganga Chaudhary", "1997-07-30", "9803456712", "A+", "Rajdevi-3, Bhalohiya", "All-rounder", "Left Hand", "Left Arm Fast", "Captain", "", "approved"),
        Player("p302", 3, "8", "Arjun Patel", "Mahesh Patel", "2002-12-05", "9814567823", "O+", "Rajdevi-3, Purwa", "Bowler", "Right Hand", "Right Arm Fast", "", "", "approved"),

        Player("p401", 4, "1", "Sandeep Giri", "Prem Giri", "1999-03-15", "9804567821", "B+", "Rajdevi-4, Basantpur", "Batsman", "Right Hand", "None", "Captain", "", "approved"),
        Player("p402", 4, "33", "Manoj Jha", "Shyam Jha", "2001-06-22", "9815678934", "A+", "Rajdevi-4, Madhyam Tole", "Bowler", "Right Hand", "Right Arm Spin", "Vice Captain", "", "approved"),

        Player("p501", 5, "11", "Rameshwar Mandal", "Devendra Mandal", "1998-10-10", "9805678912", "O+", "Rajdevi-5, Pathra", "All-rounder", "Right Hand", "Right Arm Medium", "Captain", "", "approved"),
        Player("p502", 5, "77", "Mukesh Singh", "Shambhu Singh", "2000-01-28", "9816789045", "B+", "Rajdevi-5, Uttar Tole", "Batsman", "Left Hand", "None", "", "", "approved"),

        Player("p601", 6, "9", "Karan Mahato", "Bishnu Mahato", "2001-09-09", "9806789012", "AB+", "Rajdevi-6, Brahamapuri", "Wicket Keeper", "Right Hand", "None", "Captain", "", "approved"),
        Player("p602", 6, "14", "Prakash Das", "Anand Das", "2003-05-17", "9817890123", "A+", "Rajdevi-6, Purab Tole", "Bowler", "Left Hand", "Left Arm Spin", "", "", "approved"),

        Player("p701", 7, "21", "Santosh Tiwari", "Umesh Tiwari", "1997-04-14", "9807890123", "O+", "Rajdevi-7, Gaur Road", "All-rounder", "Right Hand", "Right Arm Fast", "Captain", "", "approved"),
        Player("p702", 7, "5", "Gopal Thakur", "Sitaram Thakur", "2002-08-11", "9818901234", "B+", "Rajdevi-7, Mandir Chowk", "Batsman", "Right Hand", "None", "", "", "approved"),

        Player("p801", 8, "12", "Naresh Ray", "Hira Ray", "1999-12-01", "9808901234", "B-", "Rajdevi-8, Laxmipur", "Batsman", "Right Hand", "None", "Captain", "", "approved"),
        Player("p802", 8, "27", "Bijay Sahani", "Pradeep Sahani", "2001-07-19", "9819012345", "A+", "Rajdevi-8, Nadi kinara", "Bowler", "Right Hand", "Right Arm Medium", "Vice Captain", "", "approved"),

        Player("p901", 9, "15", "Pramod Mishra", "Dwarika Mishra", "1998-06-25", "9809012345", "O+", "Rajdevi-9, Madhuban", "All-rounder", "Left Hand", "Left Arm Spin", "Captain", "", "approved"),
        Player("p902", 9, "44", "Ashok Baitha", "Raju Baitha", "2002-10-30", "9820123456", "B+", "Rajdevi-9, Dakshin Tole", "Bowler", "Right Hand", "Right Arm Fast", "", "", "approved")
    )
    playerDao.insertAll(initialPlayers)

    // 3. Initial matches
    val initialMatches = listOf(
        TournamentMatch(1, "League", 1, 2, 88, 4, 10.0, 72, 7, 10.0, "A", "2083-01-10", "Inaugural Match · Ground A"),
        TournamentMatch(2, "League", 3, 4, 104, 3, 10.0, 89, 8, 10.0, "A", "2083-01-11", "High scoring thriller"),
        TournamentMatch(3, "League", 5, 6, 75, 10, 8.4, 76, 3, 7.2, "B", "2083-01-12", "Ward 6 chased comfortably"),
        TournamentMatch(4, "League", 7, 8, 95, 6, 10.0, 91, 5, 10.0, "A", "2083-01-13", "Last over finish"),
        TournamentMatch(5, "League", 1, 9, 112, 2, 10.0, 68, 9, 10.0, "A", "2083-01-14", "Dominant batting by Ward 1"),
        TournamentMatch(6, "League", 2, 3, 85, 8, 10.0, 86, 5, 9.1, "B", "2083-01-15", "Ward 3 won in 10th over"),
        TournamentMatch(7, "League", 4, 5, 0, 0, 0.0, 0, 0, 0.0, "", "2083-01-17", "Upcoming League fixture")
    )
    matchDao.insertAll(initialMatches)

    // 4. Default configuration
    val configs = listOf(
        TournamentConfig("maxovers", "10"),
        TournamentConfig("champion", ""),
        TournamentConfig("runnerup", ""),
        TournamentConfig("mom", "Amit Shah (Ward 1)"),
        TournamentConfig("bestbat", "Rohit Yadav (Ward 2)"),
        TournamentConfig("bestbowl", "Dhiraj Kushwaha (Ward 1)"),
        TournamentConfig("url", "https://script.google.com/macros/s/AKfycbwK-MFE9YzHqI0wJBNia_g-Se7Rp2hAu4leMUH5dW4iKZU_2XLzK8wq2QQrkNM3Hk0/exec")
    )
    configDao.setAll(configs)
}
