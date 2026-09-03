package com.teya.agent.safety

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teya.agent.household.ContactExtra
import com.teya.agent.household.ContactExtraDao
import com.teya.agent.household.MemoryDao
import com.teya.agent.household.MemoryEntry
import com.teya.agent.household.Persona
import com.teya.agent.household.PersonaDao
import com.teya.agent.household.VoiceSample
import com.teya.agent.household.VoiceSampleDao

@Database(
    entities = [Contact::class, Persona::class, MemoryEntry::class, ContactExtra::class, VoiceSample::class],
    version = 4,
)
abstract class TeyaDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun personaDao(): PersonaDao
    abstract fun memoryDao(): MemoryDao
    abstract fun contactExtraDao(): ContactExtraDao
    abstract fun voiceSampleDao(): VoiceSampleDao

    companion object {
        @Volatile
        private var instance: TeyaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `persona` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `aliases` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `memory_entry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectType` TEXT NOT NULL, `subjectKey` TEXT, `text` TEXT NOT NULL, `addedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `contact_extra` (`lookupKey` TEXT NOT NULL, `aliases` TEXT NOT NULL, PRIMARY KEY(`lookupKey`))")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `memory_entry`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `memory_entry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectType` TEXT NOT NULL, `subjectKey` TEXT, `text` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, `category` TEXT NOT NULL, `strength` REAL NOT NULL, `lastAccessedAt` INTEGER NOT NULL, `embedding` BLOB, `tier` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `voice_sample` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `lookupKey` TEXT NOT NULL, `embedding` BLOB NOT NULL, `recordedAt` INTEGER NOT NULL)")
            }
        }

        fun get(context: Context): TeyaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, TeyaDatabase::class.java, "teya-db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }
    }
}
