package com.teya.agent.household

import android.content.Context
import com.teya.agent.safety.TeyaDatabase
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.sqrt

class MemoryManager(context: Context) {
    private val dao = TeyaDatabase.get(context).memoryDao()

    suspend fun remember(
        text: String,
        subjectType: String = SUBJECT_GENERAL,
        subjectKey: String? = null,
        category: String? = null,
        embedding: FloatArray? = null,
    ): Long {
        val clean = text.trim()
        if (clean.isEmpty()) return -1L
        val now = System.currentTimeMillis()
        return dao.insert(
            MemoryEntry(
                subjectType = subjectType,
                subjectKey = subjectKey,
                text = clean,
                addedAt = now,
                category = normalizeCategory(category),
                strength = 1.0f,
                lastAccessedAt = now,
                embedding = embedding?.toBytes(),
                tier = TIER_HOT,
            )
        )
    }

    suspend fun forget(query: String, subjectKey: String? = null): Int {
        val q = query.trim().lowercase()
        if (q.length < 3) return 0
        val candidates = if (subjectKey != null) dao.bySubject(subjectKey) else dao.getAll()
        val hits = candidates.filter { it.text.lowercase().contains(q) }
        hits.forEach { dao.delete(it.id) }
        return hits.size
    }

    suspend fun search(query: String, queryEmbedding: FloatArray?, topK: Int = 5): List<MemoryEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val pool = dao.searchable()
        if (pool.isEmpty()) return emptyList()
        val semantic = if (queryEmbedding != null) {
            pool.mapNotNull { e ->
                val emb = e.embedding?.toFloatArray() ?: return@mapNotNull null
                val sim = cosine(queryEmbedding, emb)
                if (sim >= MIN_SIM) e to sim else null
            }.sortedByDescending { it.second }.map { it.first }
        } else emptyList()
        val ranked = (semantic + keywordMatch(pool, q, topK)).distinctBy { it.id }.take(topK)
        val now = System.currentTimeMillis()
        ranked.forEach { dao.reinforce(it.id, 1.0f, now) }
        return ranked
    }

    private fun keywordMatch(pool: List<MemoryEntry>, q: String, topK: Int): List<MemoryEntry> =
        pool.filter { it.text.lowercase().contains(q) }.take(topK)

    suspend fun recentEpisodic(since: Long): List<MemoryEntry> = dao.episodicSince(since)

    suspend fun pruneOrphans(validContactKeys: Set<String>): Int {
        if (validContactKeys.isEmpty()) return 0
        val orphans = dao.getAll().filter { it.subjectType == SUBJECT_CONTACT && it.subjectKey !in validContactKeys }
        orphans.forEach { dao.delete(it.id) }
        return orphans.size
    }

    suspend fun hasSimilar(text: String, subjectKey: String?): Boolean {
        val norm = normalizeText(text)
        if (norm.isEmpty()) return false
        val pool = dao.getAll().filter { it.category.uppercase() != CAT_EPISODIC }
        val scope = if (subjectKey != null) pool.filter { it.subjectKey == subjectKey }
                    else pool.filter { it.subjectType == SUBJECT_GENERAL }
        return scope.any {
            val e = normalizeText(it.text)
            e.isNotEmpty() && (e == norm || e.contains(norm) || norm.contains(e))
        }
    }

    private fun normalizeText(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    suspend fun all(): List<MemoryEntry> = dao.getAll()

    suspend fun delete(id: Int) = dao.delete(id)

    suspend fun runDecay(now: Long = System.currentTimeMillis()): DreamSummary {
        val all = dao.getAll()
        var cooled = 0
        var pruned = 0
        all.forEach { e ->
            val s = strengthNow(e, now)
            if (e.category.uppercase() == CAT_EPISODIC && s < DEAD_THRESHOLD) {
                dao.delete(e.id)
                pruned++
            } else {
                val tier = if (s >= HOT_THRESHOLD) TIER_HOT else TIER_COLD
                dao.retier(e.id, s, tier)
                if (tier == TIER_COLD && e.tier == TIER_HOT) cooled++
            }
        }
        return DreamSummary(scanned = all.size, cooled = cooled, pruned = pruned, at = now)
    }

    private fun strengthNow(e: MemoryEntry, now: Long): Float {
        val elapsedDays = (now - e.lastAccessedAt).coerceAtLeast(0L) / 86_400_000.0
        return 0.5.pow(elapsedDays / halfLifeDays(e.category)).toFloat().coerceIn(0f, 1f)
    }

    private fun halfLifeDays(category: String): Double = when (category.uppercase()) {
        CAT_EPISODIC -> 3.0
        CAT_PREFERENCE -> 45.0
        CAT_ROUTINE -> 120.0
        else -> 3650.0
    }

    suspend fun memoryContextBlock(members: List<Member>): String {
        val persona = dao.hot().filter { it.subjectType == SUBJECT_CONTACT }
        if (persona.isEmpty()) return ""
        val nameByKey = members.mapNotNull { m -> m.lookupKey?.let { it to m.displayName } }.toMap()
        val byMember = LinkedHashMap<String, MutableList<String>>()
        persona.forEach { e ->
            val name = nameByKey[e.subjectKey] ?: return@forEach
            byMember.getOrPut(name) { mutableListOf() }.add(e.text)
        }
        if (byMember.isEmpty()) return ""
        val sb = StringBuilder("What you remember (authoritative — durable memory about this family):\n")
        byMember.forEach { (name, facts) -> sb.append("- $name: ").append(facts.joinToString("; ")).append("\n") }
        return sb.toString().trimEnd()
    }

    private fun normalizeCategory(raw: String?): String {
        val c = raw?.trim()?.uppercase()
        return if (c != null && c in CATEGORIES) c else CAT_FACT
    }

    companion object {
        const val SUBJECT_CONTACT = "CONTACT"
        const val SUBJECT_PERSONA = "PERSONA"
        const val SUBJECT_GENERAL = "GENERAL"
        const val CAT_FACT = "FACT"
        const val CAT_PREFERENCE = "PREFERENCE"
        const val CAT_ROUTINE = "ROUTINE"
        const val CAT_EPISODIC = "EPISODIC"
        private val CATEGORIES = setOf(CAT_FACT, CAT_PREFERENCE, CAT_ROUTINE, CAT_EPISODIC)
        const val TIER_HOT = "HOT"
        const val TIER_COLD = "COLD"
        private const val MIN_SIM = 0.35f
        private const val HOT_THRESHOLD = 0.5f
        private const val DEAD_THRESHOLD = 0.05f
    }
}

data class DreamSummary(val scanned: Int, val cooled: Int, val pruned: Int, val at: Long) {
    fun note(): String = "scanned $scanned · cooled $cooled · pruned $pruned"
}

internal fun FloatArray.toBytes(): ByteArray {
    val buf = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
    forEach { buf.putFloat(it) }
    return buf.array()
}

internal fun ByteArray.toFloatArray(): FloatArray {
    val buf = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    return FloatArray(size / 4) { buf.float }
}

internal fun cosine(a: FloatArray, b: FloatArray): Float {
    if (a.size != b.size) return 0f
    var dot = 0f; var na = 0f; var nb = 0f
    for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
    val denom = sqrt(na) * sqrt(nb)
    return if (denom == 0f) 0f else dot / denom
}
