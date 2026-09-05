package com.teya.agent.ui.face

/**
 * LifeOS compatibility boundary for the Teya harness.
 *
 * Upstream Teya declares this enum inside its Compose AgentFace renderer. LifeOS intentionally
 * does not transplant that renderer because its product UI follows the GitHub Mobile-inspired
 * LifeOS shell. The harness contract itself only needs these five state names.
 */
enum class AgentState { IDLE, LISTENING, THINKING, SPEAKING, BRAIN_OFF }
