# ExpenseMind AI — MVP Source

An offline, on-device Android app that imports bank statement PDFs and answers
questions about your spending. Nothing leaves your phone.

## What's actually in this codebase right now

This is a **working MVP skeleton**, not a finished app. Specifically:

✅ **Fully implemented and should work once built:**
- SQLite schema (Room) for transactions + merchant aliases
- PDF text extraction (PDFBox-Android)
- Bank detection (HDFC / SBI signatures)
- Regex-based statement parsers for HDFC and SBI (**see calibration note below**)
- Merchant name normalizer (built-in dictionary + user aliases)
- Rule-based category classifier
- Finance tool functions (monthly spend, category breakdown, month comparison,
  recurring payment detection, merchant search, savings calculation)
- Rule-based chat agent that routes questions to the right tool
- Chat UI (Jetpack Compose) with PDF import via file picker

❌ **Not implemented yet — by design, so you have something buildable first:**
- Local LLM (the chat agent currently uses keyword matching, not real AI reasoning)
- ICICI parser (only HDFC + SBI regex templates exist)
- OCR, voice input, budget planning, investment tracking, family mode
- "Remember that X is my contractor" isn't wired to the database yet (see AgentController.kt)

## ⚠️ Most important next step: calibrate the parsers

The HDFC and SBI regex patterns in `HdfcParser.kt` and `SbiParser.kt` are
**templates based on typical statement formats** — they were not built
against your actual statements (I don't have access to them). Real bank
PDFs vary by account type, statement period format, and change over time.

**Before this app will work correctly:**
1. Export one real statement PDF from your bank (redact/blank out the
   account number if sharing with any tool)
2. Open it and copy 10-15 raw transaction lines as text
3. Give those sample lines to Claude Code along with the parser file —
   it can adjust the regex to match exactly

This is the single task most likely to need iteration. Don't expect the
current regex to work on the first try.

## How to build the actual APK

You need Android Studio (or Claude Code, which can drive Gradle directly)
plus the Android SDK. Steps:

1. Install **Android Studio** (free, from developer.android.com) — or use
   **Claude Code**, which can install the SDK and run builds for you
2. Open this project folder in Android Studio (`File > Open`, select the
   `ExpenseMindAI` folder)
3. Let Gradle sync (downloads dependencies — needs internet just for this step)
4. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**
5. The APK lands in `app/build/outputs/apk/debug/app-debug.apk`
6. Transfer that file to your phone and install it (you'll need to allow
   "install from unknown sources")

If using Claude Code: point it at this folder and ask it to run
`./gradlew assembleDebug` — it can install missing SDK components and fix
build errors as they come up.

## Adding the local LLM (upgrade from rule-based to real AI)

The current `AgentController.kt` uses keyword matching, which works but
isn't real reasoning. To upgrade to an actual on-device LLM:

1. Add the MediaPipe dependency (already commented out in `app/build.gradle.kts`):
   `implementation("com.google.mediapipe:tasks-genai:0.10.14")`
2. Download a quantized Gemma model in `.task` format (Google publishes these
   for on-device use — search "MediaPipe LLM Inference Gemma Android")
3. Bundle the model file with the app or download it on first launch (it's
   several hundred MB, so on-first-launch download is usually better than
   bundling in the APK)
4. Replace the `when` block in `AgentController.handleMessage()` with a call
   that sends the tool list + user message to the LLM, gets back which tool
   to call, executes it via `FinanceTools`, then asks the LLM to phrase the
   final answer using the real numbers

Test inference speed on your actual phone early — this is the biggest
technical risk in the whole project. A 1-3B parameter model in 4-bit
quantization is roughly the ceiling for smooth on-device performance today.

## Project structure

```
app/src/main/java/com/expensemind/ai/
├── data/            Room database: Transaction, MerchantAlias entities + DAOs
├── parser/          PDF extraction, bank detection, HDFC/SBI parsers, import pipeline
├── normalize/        Merchant name cleaning, category classification
├── tools/           FinanceTools - the "functions" the agent can call
├── agent/           AgentController - routes chat messages to tools
└── ui/              Compose chat screen + ViewModel
```

## Suggested order of work from here

1. Calibrate HDFC parser against a real statement (highest priority)
2. Build and install the debug APK, test the import flow end-to-end
3. Add your second bank's parser
4. Wire up "remember that X is my contractor" to `MerchantAliasDao`
5. Expand the category classifier keyword lists based on your real spending
6. Only then: tackle the local LLM upgrade
