# LifeOS v44 — OCR Quality Architecture

Status: **experimental validation pipeline**. Images/OCR are deliberately **not** a normal LifeOS capability yet.

## Product rule

OCR output is derived evidence, not the source of truth. The original image/URI is preserved independently of OCR success, OCR engine version, preprocessing strategy, or later correction.

A normal user-facing `Images` capability may be promoted only after measured Arabic + mixed Arabic/English accuracy passes the agreed benchmark gate on real device samples.

## Current local pipeline

```text
Explicit image selection
        ↓
Durable ImageObject + stable image ID
        ↓
ImagePreprocessor
  - EXIF orientation
  - bounded decode
  - small-text upscale
  - contrast grayscale
  - Otsu threshold
        ↓
Parallel candidate families
  - ML Kit Latin OCR, multi-pass
  - Tesseract tessdata_best Arabic OCR, multi-pass
        ↓
Candidate scoring + variant agreement
        ↓
Consensus / distinct-line merge
        ↓
Raw OCR text (immutable per run)
        +
Search-normalized text (derived)
        +
Critical tokens (derived)
        +
Engine candidate diagnostics
        ↓
Durable OcrResult
```

## Immutable evidence contract

`ImageOcrStore` keeps:

- image ID
- persisted content URI when Android grants it
- display name / MIME type / dimensions / source / added timestamp
- OCR status and latest run ID
- every OCR run independently
- raw OCR text
- search-normalized OCR text
- engine summary / detected scripts / confidence / processing duration
- line data and bounding boxes when available
- critical token candidates
- candidate-engine diagnostics
- user corrections
- benchmark ground truth

A correction never rewrites the raw OCR run. It creates benchmark truth/correction data so later engines can be compared objectively.

## Arabic strategy

Cortex used a single Tesseract Arabic pass with a fast model. LifeOS v44 instead uses:

- `tessdata_best` Arabic model;
- several image preprocessing variants;
- Arabic-density + text-quality scoring;
- cross-variant agreement;
- no normalization of the stored raw OCR text.

Search normalization may normalize Alef/Yeh forms, tatweel/diacritics, Arabic digits and spacing. This is strictly a search representation and is never presented as the original recognized text.

## Latin strategy

ML Kit Text Recognition is run over multiple sensible preprocessing variants. The best candidate is selected using text-quality score plus agreement with other variants. Bounding boxes are retained from the selected candidate.

## Critical-token policy

Amounts, dates, times, phone numbers, URLs and email addresses receive a separate derived extraction pass. These tokens are surfaced for inspection but are not silently 'corrected' into a different numeric value.

Numeric hallucination is unacceptable. Ambiguous tokens remain ambiguous until supported by stronger OCR or explicit correction.

## OCR Lab

The experimental `OCR quality lab` is reachable from LifeOS status → Advanced. It allows:

1. explicit image import through Android document/image picker;
2. automatic OCR run;
3. preview of raw result, confidence, engine details and critical tokens;
4. re-scan;
5. inspection of candidate-engine output;
6. exact ground-truth correction without overwriting raw OCR;
7. per-image CER/WER;
8. aggregate benchmark CER/WER.

## Promotion gate

Initial product-promotion threshold:

- at least **30** ground-truth images;
- corpus includes Arabic-only and mixed Arabic/English real screenshots/documents;
- aggregate **CER ≤ 8%**;
- aggregate **WER ≤ 18%**;
- critical numeric/amount failures reviewed separately;
- no unresolved data-loss or source-image persistence defects.

Passing the threshold does not prove perfect OCR. It is the minimum gate before exposing Images/OCR as a normal searchable LifeOS feature.

## Cloud OCR A/B — intentionally not operational yet

The architecture reserves a high-accuracy recovery/benchmark path for cloud OCR. No provider key is embedded in the Android client and no cloud provider is claimed operational yet.

Target design:

```text
low confidence / benchmark sample
        ↓
LifeOS authorized secure backend
        ↓
Provider A: Google document OCR
Provider B: Azure Read OCR
        ↓
provider-specific raw candidates
        ↓
measured CER/WER + critical-token accuracy
        ↓
policy selected from evidence, not vendor preference
```

The Android app must never ship long-lived cloud OCR credentials. Cloud images must not be uploaded until a deliberately configured secure backend and privacy contract exist.

## Future promotion sequence

- [x] Durable image store independent from OCR.
- [x] Multi-pass preprocessing.
- [x] ML Kit Latin candidate engine.
- [x] Tesseract Best Arabic candidate engine.
- [x] Candidate scoring and agreement.
- [x] Raw vs normalized text separation.
- [x] Critical-token extraction.
- [x] Ground-truth correction store.
- [x] CER/WER benchmark implementation.
- [x] On-device OCR Lab.
- [ ] Collect ≥30 real ground-truth samples.
- [ ] Validate Arabic-only set.
- [ ] Validate mixed Arabic/English set.
- [ ] Review amount/date/phone critical-token errors separately.
- [ ] Implement authorized secure cloud OCR gateway.
- [ ] Run Google vs Azure A/B benchmark if cloud recovery is needed.
- [ ] Select routing/recovery policy from measured results.
- [ ] Only then register `Images` in `FunctionalCapabilityRegistry`.
- [ ] Only then federate OCR text into normal Search and Ask grounding.
- [ ] Only then expose Image in normal Quick Capture.

## Non-claims

Current v44 does **not** claim:

- production-grade Images capability;
- cloud OCR;
- full visual understanding/VLM classification;
- reliable receipt/document/chat classification;
- semantic truth derived solely from OCR;
- automatic screenshot ingestion;
- automatic gallery scanning.

These remain unavailable until their own end-to-end truth gates are satisfied.
