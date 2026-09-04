package com.kareem.lifeos

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File

/**
 * Process-local LiteRT-LM runtime for LifeOS background semantics.
 *
 * The engine is heavyweight, so it is initialized lazily on the dedicated BackgroundBrain
 * executor and reused across notification items. The no-think Qwen build is intentionally used
 * because LifeOS wants short structured JSON, not chain-of-thought text.
 */
object BackgroundSemanticRuntime {
    private var engine: Engine? = null
    private var loadedPath: String = ""

    @JvmStatic
    @Synchronized
    fun generate(context: Context, modelPath: String, prompt: String): String {
        val app = context.applicationContext
        val current = if (engine == null || loadedPath != modelPath) {
            try { engine?.close() } catch (_: Throwable) {}
            val cache = File(app.cacheDir, "litertlm-background").apply { mkdirs() }
            Engine(
                EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    cacheDir = cache.absolutePath,
                )
            ).also {
                it.initialize()
                engine = it
                loadedPath = modelPath
            }
        } else engine!!

        val config = ConversationConfig(
            systemInstruction = Contents.of(
                "You are LifeOS background semantic classification. Return only the requested JSON. " +
                    "Never invent facts, people, dates, urgency, actions, outcomes, or obligations."
            ),
            samplerConfig = SamplerConfig(temperature = 0.0, topK = 1, topP = 1.0),
        )
        current.createConversation(config).use { conversation ->
            return conversation.sendMessage(prompt).contents.toString().trim()
        }
    }

    @JvmStatic
    @Synchronized
    fun reset() {
        try { engine?.close() } catch (_: Throwable) {}
        engine = null
        loadedPath = ""
    }
}
