package com.teya.agent.household

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persona")
data class Persona(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val aliases: String = "",
)

@Entity(tableName = "memory_entry")
data class MemoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectType: String = "GENERAL",
    val subjectKey: String? = null,
    val text: String,
    val addedAt: Long,
    val category: String = "FACT",
    val strength: Float = 1.0f,
    val lastAccessedAt: Long = 0L,
    val embedding: ByteArray? = null,
    val tier: String = "HOT",
)

@Entity(tableName = "contact_extra")
data class ContactExtra(
    @PrimaryKey val lookupKey: String,
    val aliases: String = "",
)

@Entity(tableName = "voice_sample")
data class VoiceSample(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lookupKey: String,
    val embedding: ByteArray,
    val recordedAt: Long,
)
