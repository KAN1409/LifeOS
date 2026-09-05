# LifeOS v44 — Voice Memory Architecture

Status: implemented provider, pending real-device acceptance.

## Product distinction

LifeOS has two different microphone behaviors and must not blur them:

- **Ask microphone** — speech input for a question to LifeOS.
- **Voice Memory** — durable first-party audio captured into LifeOS memory.

Voice Memory is a real capability, not a decorative prompt or temporary speech-recognition input.

## End-to-end chain

```text
Quick Capture → Voice
        ↓
RECORD_AUDIO permission
        ↓
VoiceCapture
16 kHz mono PCM16 WAV
        ↓
Original audio persisted locally FIRST
        ↓
VoiceMemoryRepository
stable voice:<id>
        ↓
Cloud transcription attempt
Arabic + English code-switch mode
        ↓
verbatim transcript + timestamped segments
        ↓
Search / Voice Memories / focused Ask
```

## Source-first safety

The original WAV is the durable source. Transcription is derived state.

A network failure, ASR error, empty transcript, or later re-transcription must never erase the source recording. Failed transcription leaves the recording browsable and exposes an explicit Retry transcript action.

## Recorder provenance

`VoiceCapture` is migrated from the proven Cortex recorder architecture rather than rewritten as a fake SpeechRecognizer flow:

- Android `AudioRecord`;
- 16 kHz;
- mono;
- PCM 16-bit;
- WAV header finalized on Stop;
- file saved under LifeOS private storage.

## Transcription

`VoiceTranscriber` migrates the Cortex server-side transcription path:

- HTTPS multipart upload;
- Arabic + English code-switch request (`ar,en`);
- ASR provider credentials remain server-side;
- same-host HTTPS redirect validation;
- retryable network/server failures remain distinguishable;
- full transcript + timestamped segment results are stored.

The Android app contains no ASR provider credential.

## Durable model

`lifeos_voice_memory.db` stores:

- stable voice ID;
- WAV path;
- created timestamp;
- duration;
- file size;
- state (`recorded`, `transcribing`, `transcribed`, `transcription_failed`, etc.);
- verbatim transcript;
- language;
- transcription engine/version;
- error state;
- timestamped transcript segments and confidence.

## UI

Voice Memory is available through:

- Now → Quick capture → Voice;
- Search → Voice memories;
- Voice Memories capability → Record voice memory;
- Search transcript results;
- Voice Memory detail → Play / Re-transcribe / Ask about this.

Voice Memory detail reloads by stable `voice:<id>` and plays the actual source WAV.

## Ask grounding

`Ask about this voice memory` passes `focus_capability=voice` and the stable object ID. `GroundedQueryEngine` reloads that exact object and supplies the verbatim transcript only when the stored transcript really exists.

No transcript means Ask is not allowed to pretend it heard or understood the recording.

## Functional acceptance

- [x] Real WAV capture.
- [x] Audio saved before transcription.
- [x] Stable Voice Memory IDs.
- [x] Durable Voice Memory store.
- [x] Timestamped transcript segments.
- [x] Mixed Arabic + English server-side transcription path migrated from Cortex.
- [x] Transcription failure keeps audio safe.
- [x] Manual re-transcription.
- [x] Real playback.
- [x] Search provider.
- [x] Quick Capture entry.
- [x] Stable-ID focused Ask grounding.
- [ ] Device proof: record → stop → WAV saved.
- [ ] Device proof: playback works.
- [ ] Device proof: Arabic-only transcription quality.
- [ ] Device proof: mixed Arabic/English transcription quality.
- [ ] Device proof: failed/offline transcription keeps recording and Retry works.
- [ ] Device proof: Search finds a known spoken phrase.
- [ ] Device proof: Ask about this uses the exact transcript.

## Non-claims

v44 does not yet claim speaker diarization, always-on ambient recording, background recording, call recording, or automatic semantic extraction from every transcript. Those require separate privacy, Android capability, and functional truth gates.
