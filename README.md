# voice0

Voice-controlled DeFi on Solana — native Android app built with Jetpack Compose and the Solana Mobile Stack. Speak a command, review the transaction, sign it.

> "swap 50 USDC for SOL" → press-hold mic → review screen → wallet pops up → done.

---

## Stack

| Layer | Choice |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Language | Kotlin 2.0 |
| Architecture | MVVM (`AndroidViewModel` + `StateFlow`) |
| Wallet | Mobile Wallet Adapter (`com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.3`) |
| Solana primitives | `com.solanamobile:web3-solana`, `rpc-core`, `multimult` |
| Intent parsing | Anthropic Claude (direct from device) |
| STT | ElevenLabs (direct from device) |
| Swap routing | Jupiter v6 |
| RPC | Helius (mainnet & devnet) |
| HTTP | Ktor + OkHttp engine |
| Serialization | kotlinx.serialization |
| Build | Gradle Kotlin DSL + version catalog |

---

## Prerequisites

- Android Studio Ladybug (2024.2+) or newer
- JDK 17 (bundled with recent Android Studio)
- An Android device/emulator running API 26+ (Android 8.0)
- An MWA-compatible wallet installed on the device (Phantom, Solflare, Backpack)
- API keys for: Anthropic, ElevenLabs, Helius

---

## Getting started

```powershell
# 1. Open the project in Android Studio
#    (File → Open → select this directory)
#    Android Studio will run `gradle wrapper` and download the Gradle distribution.

# 2. Copy the local-properties template and fill in your keys
Copy-Item local.properties.example local.properties
# then edit local.properties

# 3. Sync Gradle (Android Studio prompts on import; or `./gradlew assembleDebug`)
# 4. Run on a connected device with an MWA-compatible wallet installed.
```

If Android Studio doesn't auto-generate `gradlew`/`gradlew.bat`, run once:

```powershell
gradle wrapper --gradle-version 8.10.2
```

---

## Project layout

```
voice0/
├── settings.gradle.kts
├── build.gradle.kts                 # root, plugin declarations only
├── gradle/libs.versions.toml        # version catalog
├── gradle.properties
├── local.properties.example         # → copy to local.properties
└── app/
    ├── build.gradle.kts             # AGP, deps, BuildConfig from local.properties
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml      # mic + internet perms, MWA <queries>
        ├── res/                     # theme, colors, launcher icon, backup rules
        └── java/com/voice0/app/
            ├── MainActivity.kt              # ActivityResultSender + Compose host
            ├── Voice0Application.kt
            ├── data/                        # Cluster, Tokens, Amount, Models, Phase
            ├── network/                     # Ktor clients (Anthropic, ElevenLabs, Jupiter, Helius RPC, prices)
            ├── parser/IntentParser.kt       # Claude → SolanaTxBundle, with validation
            ├── solana/
            │   ├── TxBuilder.kt             # Native + SPL transfers; Jupiter swap delegation
            │   ├── TxAsserter.kt            # decodes tx and asserts destination/amount/payer
            │   ├── Simulator.kt             # build → simulate → annotate fee/price-impact
            │   └── Balances.kt              # SOL + SPL balance fetch + format
            ├── audio/AudioRecorder.kt       # MediaRecorder wrapper, m4a output
            ├── wallet/WalletManager.kt      # MWA + EncryptedSharedPreferences for auth_token
            ├── viewmodel/HomeViewModel.kt   # state machine, coroutines, pipeline orchestration
            └── ui/
                ├── HomeScreen.kt            # phase router
                ├── ReviewScreen.kt          # step list + sim result + footer
                ├── ExecutionScreen.kt
                ├── SuccessScreen.kt
                ├── components/
                │   ├── VoiceButton.kt
                │   ├── StepCard.kt
                │   ├── BalanceStrip.kt
                │   └── NetworkToggle.kt
                └── theme/{Color,Type,Theme}.kt
```

---

## How it works

```
[Hold mic]      → MediaRecorder writes m4a to cache dir
[Release]       → ElevenLabsClient.transcribe() returns text
                  → IntentParser calls Claude with <user_intent>…</user_intent> wrapping
                  → Zod-style validation: pubkey check, slippage clamp, drop unsupported steps
[Connect wallet] → MWA authorize() / reauthorize() (cached in EncryptedSharedPreferences)
[Simulate]      → for each step: TxBuilder.buildTransfer/buildSwap → HeliusRpc.simulateTransaction
                  → annotate step with feeLamports, priceImpactPct, requiresExtraConfirm
[Review]        → user sees per-step cards, fee, warnings, high-impact ack checkbox
[Confirm]       → REBUILD txs from validated params (don't trust simulator bytes)
                  → TxAsserter.assertTransfer / assertSwap (payer + destination + amount checks)
                  → MWA.signAndSendTransactions
[Done]          → signatures shown, Solscan links, balances refreshed
```

App phases: `IDLE → RECORDING → TRANSCRIBING → PARSING → SIMULATING → REVIEWING → EXECUTING → DONE` (or `ERROR` at any step).

---

## Supported tokens

| Symbol | Mint | Decimals |
|---|---|---|
| SOL | native | 9 |
| USDC | EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v | 6 |
| USDT | Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB | 6 |
| BONK | DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263 | 5 |
| JUP | JUPyiwrYJFskUPiHa7hkeR8VUtAeFoSYbKedZNsDvCN | 6 |

Adding a new token: edit `data/Tokens.kt`. The rest of the pipeline (balance fetch, price fetch, swap routing) picks it up automatically. Decimals for unmapped mints are fetched on-chain via `HeliusRpc.getMintDecimals`.

---

## Testing

```powershell
./gradlew test                    # unit tests (Amount, Tokens, Balances format)
./gradlew connectedAndroidTest    # instrumented tests
```

Unit tests are in `app/src/test/java/com/voice0/app/`.

---

## Known caveats

- **API surface drift**: the MWA Kotlin client (`mobile-wallet-adapter-clientlib-ktx`) and `web3-solana` libraries are still pre-1.0. If 2.0.3 ships method renames vs. what's in `WalletManager.kt` / `TxBuilder.kt`, the fix is a one-line rename per call site — the structure stays the same. See https://docs.solanamobile.com/get-started/kotlin/installation .
- **Devnet airdrop**: switch the in-app toggle to Devnet, then from a terminal: `solana airdrop 2 <wallet-address> --url devnet`.
- **MediaRecorder min press**: a press shorter than ~200ms produces an empty file and ElevenLabs returns 400. The app surfaces the error; the user retries.
- **Blockhash expiry**: txs are rebuilt at confirm time, so a stale blockhash is rare. If the wallet rejects with `BlockhashNotFound`, tap Cancel and re-confirm — that re-fetches.
- **iOS**: not supported. MWA is Android-only by design.

---

## Roadmap

- DataStore-backed recents (last N voice intents)
- Editable per-step params on the review screen (amount/slippage/destination without re-parsing)
- Pre-flight balance check ("insufficient X")
- Compose UI tests for the review screen
