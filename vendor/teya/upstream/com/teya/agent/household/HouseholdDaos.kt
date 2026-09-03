package com.teya.agent.household

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PersonaDao {
    @Query("SELECT * FROM persona ORDER BY name")
    suspend fun getAll(): List<Persona>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(persona: Persona): Long

    @Query("DELETE FROM persona WHERE id = :id")
    suspend fun delete(id: Int)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_entry ORDER BY addedAt DESC")
    suspend fun getAll(): List<MemoryEntry>

    @Query("SELECT * FROM memory_entry WHERE subjectKey = :subjectKey ORDER BY strength DESC, addedAt DESC")
    suspend fun bySubject(subjectKey: String): List<MemoryEntry>

    @Query("SELECT * FROM memory_entry WHERE tier = 'HOT' ORDER BY strength DESC, addedAt DESC")
    suspend fun hot(): List<MemoryEntry>

    @Query("SELECT * FROM memory_entry WHERE category = 'EPISODIC' AND addedAt > :since ORDER BY addedAt")
    suspend fun episodicSince(since: Long): List<MemoryEntry>

    @Query("SELECT * FROM memory_entry WHERE NOT (subjectType = 'CONTACT' AND tier = 'HOT')")
    suspend fun searchable(): List<MemoryEntry>

    @Insert
    suspend fun insert(entry: MemoryEntry): Long

    @Update
    suspend fun update(entry: MemoryEntry)

    @Query("UPDATE memory_entry SET strength = :strength, lastAccessedAt = :at, tier = 'HOT' WHERE id = :id")
    suspend fun reinforce(id: Int, strength: Float, at: Long)

    @Query("UPDATE memory_entry SET strength = :strength, tier = :tier WHERE id = :id")
    suspend fun retier(id: Int, strength: Float, tier: String)

    @Query("DELETE FROM memory_entry WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("UPDATE memory_entry SET subjectKey = :new WHERE subjectKey = :old")
    suspend fun remapSubject(old: String, new: String)

    @Query("DELETE FROM memory_entry WHERE subjectKey = :subjectKey")
    suspend fun deleteBySubject(subjectKey: String)
}

@Dao
interface ContactExtraDao {
    @Query("SELECT * FROM contact_extra")
    suspend fun getAll(): List<ContactExtra>

    @Query("SELECT * FROM contact_extra WHERE lookupKey = :lookupKey LIMIT 1")
    suspend fun find(lookupKey: String): ContactExtra?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(extra: ContactExtra)

    @Query("DELETE FROM contact_extra WHERE lookupKey = :lookupKey")
    suspend fun delete(lookupKey: String)

    @Query("DELETE FROM contact_extra")
    suspend fun clear()
}

@Dao
interface VoiceSampleDao {
    @Query("SELECT * FROM voice_sample ORDER BY recordedAt")
    suspend fun getAll(): List<VoiceSample>

    @Query("SELECT * FROM voice_sample WHERE lookupKey = :lookupKey ORDER BY recordedAt")
    suspend fun byMember(lookupKey: String): List<VoiceSample>

    @Insert
    suspend fun insert(sample: VoiceSample): Long

    @Query("DELETE FROM voice_sample WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM voice_sample WHERE lookupKey = :lookupKey")
    suspend fun deleteByMember(lookupKey: String)

    @Query("UPDATE voice_sample SET lookupKey = :new WHERE lookupKey = :old")
    suspend fun remapMember(old: String, new: String)
}
