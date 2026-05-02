# voice0

Voice-controlled DeFi on Solana. Speak a command, review the transaction, sign it — all from your phone.

voice0 lets you interact with the Solana blockchain using natural language. Say "swap 50 USDC for SOL" or "send 2 SOL to [address]" and the app parses your intent, builds the transaction, simulates it, and sends it to your mobile wallet for signing.

---

## Features

- Press-and-hold voice recording with real-time state feedback
- Speech-to-text via ElevenLabs
- Intent parsing via Claude (Anthropic) — understands swap, transfer, and multi-step commands
- Jupiter DEX integration for token swaps with live quotes
- Transaction simulation on Solana RPC before signing (fees, warnings, pass/fail)
- Full transaction review panel — inspect each step, estimated fees, and warnings before confirming
- Mobile Wallet Adapter for secure signing (Android)
- Mainnet / Devnet toggle
- Live token balances and USD prices

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Expo 54 + React Native 0.81.5 |
| Routing | Expo Router 6 (file-based) |
| Styling | NativeWind 4 (Tailwind CSS) |
| Language | TypeScript 5.9 |
| Intent parsing | Anthropic Claude (`@anthropic-ai/sdk`) |
| Speech-to-text | ElevenLabs |
| Blockchain | Solana (`@solana/web3.js`) |
| RPC | Helius |
| DEX | Jupiter Aggregator |
| Token ops | `@solana/spl-token` |
| Wallet | Mobile Wallet Adapter (Android) |
| Validation | Zod |

---

## How It Works

```
[Hold button] → record audio
      ↓
[Release]     → ElevenLabs transcribes speech to text
      ↓
POST /api/parse-intent  → Claude parses text into a SolanaTxBundle
      ↓
POST /api/simulate      → server builds txs (Jupiter quotes for swaps,
                          transfer instructions), simulates on RPC,
                          estimates fees
      ↓
[Review panel]          → user inspects steps, warnings, fees
      ↓
[Confirm]               → Mobile Wallet Adapter opens wallet for signing
      ↓
[Done]                  → transaction signatures displayed
```

App phases: `idle` → `recording` → `transcribing` → `parsing` → `simulating` → `reviewing` → `executing` → `done` (or `error` at any step).

---

## Prerequisites

- Node.js 18+
- Expo CLI (`npm install -g expo-cli`) or `npx expo`
- Android device or emulator (required for wallet signing via Mobile Wallet Adapter)
- API keys: Anthropic, ElevenLabs, Helius

---

## Getting Started

```bash
# 1. Clone the repo
git clone <repo-url>
cd voice0

# 2. Install dependencies
npm install

# 3. Set up environment variables
cp .env.example .env
# Fill in your API keys — see Environment Variables section below

# 4. Start the dev server
npm start

# Or target a specific platform
npm run android   # Android emulator / device
npm run ios       # iOS simulator
npm run web       # Browser (wallet signing not available on web)
```

---

## Environment Variables

| Variable | Side | Required | Description |
|---|---|---|---|
| `ANTHROPIC_API_KEY` | Server | Yes | Claude API key for intent parsing |
| `HELIUS_RPC_URL` | Server | Yes | Helius mainnet RPC endpoint |
| `HELIUS_DEVNET_RPC_URL` | Server | Yes | Helius devnet RPC endpoint |
| `EXPO_PUBLIC_ELEVENLABS_API_KEY` | Client | Yes | ElevenLabs speech-to-text API key |
| `EXPO_PUBLIC_APP_NAME` | Client | No | App display name (default: `voice0`) |
| `EXPO_PUBLIC_BASE_URL` | Client | No | API base URL override for production deployments |

> Variables without the `EXPO_PUBLIC_` prefix are server-side only and are never exposed to the client bundle.

---

## Supported Tokens

| Symbol | Network | Decimals |
|---|---|---|
| SOL | Native | 9 |
| USDC | EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v | 6 |
| USDT | Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB | 6 |
| BONK | DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263 | 5 |
| JUP | JUPyiwrYJFskUPiHa7hkeR8VUtAeFoSYbKedZNsDvCN | 6 |

---

## Project Structure

```
voice0/
├── app/
│   ├── (tabs)/
│   │   ├── index.tsx          # Main screen — voice input, state machine, tx flow
│   │   └── explore.tsx
│   ├── api/
│   │   ├── parse-intent+api.ts  # POST: Claude intent parsing
│   │   └── simulate+api.ts      # POST: tx building & simulation
│   └── _layout.tsx
├── lib/
│   ├── types.ts               # SolanaTxBundle, SolanaTxStep, params types
│   ├── wallet.ts              # Mobile Wallet Adapter
│   ├── elevenlabs.ts          # Audio recording + STT
│   ├── solana.ts              # RPC connection + tx simulation
│   ├── jupiter.ts             # DEX quotes + swap tx building
│   ├── tokens.ts              # Token metadata registry
│   ├── balances.ts            # Token balance fetching
│   ├── prices.ts              # Token price fetching
│   └── network.ts             # Cluster management (mainnet/devnet)
├── components/
│   ├── VoiceButton.tsx        # Press-and-hold recording button
│   ├── TxReviewPanel.tsx      # Transaction review UI
│   ├── StepCard.tsx           # Individual step display
│   ├── BalanceStrip.tsx       # Token balance bar
│   └── NetworkToggle.tsx      # Mainnet / Devnet switcher
├── .env.example
└── package.json
```

---

## Contributing

### Development workflow

```bash
# Fork the repo, then clone your fork
git clone https://github.com/<your-username>/voice0.git
cd voice0
npm install
cp .env.example .env   # fill in your keys
```

Create a branch for your change:

```bash
git checkout -b feat/my-feature
```

Run the linter before pushing:

```bash
npm run lint
```

Open a pull request against `main`.

### Using devnet

Switch the in-app network toggle to **Devnet** to avoid spending real SOL while developing. Make sure `HELIUS_DEVNET_RPC_URL` is set in your `.env`. You can airdrop devnet SOL via the Solana CLI:

```bash
solana airdrop 2 <your-wallet-address> --url devnet
```

### Skipping ElevenLabs during development

If you don't have an ElevenLabs key, you can bypass STT by temporarily hardcoding a transcript string in `lib/elevenlabs.ts` and returning it directly instead of making the API call. This lets you work on intent parsing and the transaction flow without needing audio.

### Adding a new token

Token metadata lives in [`lib/tokens.ts`](lib/tokens.ts). Add an entry to the `TOKENS` map with the mint address, symbol, decimals, and display name. The rest of the pipeline (balance fetching, price fetching, Jupiter routing) picks it up automatically.

### Adding a new intent type

1. Add the new step type to `SolanaTxStep["type"]` in [`lib/types.ts`](lib/types.ts) and define its `params` shape.
2. Update the Claude system prompt in [`app/api/parse-intent+api.ts`](app/api/parse-intent+api.ts) to recognize and emit the new type.
3. Handle building and simulating the transaction in [`app/api/simulate+api.ts`](app/api/simulate+api.ts).
4. Update [`components/StepCard.tsx`](components/StepCard.tsx) to render the new step type in the review panel.

### Key areas to know

| Area | Files |
|---|---|
| State machine & main UI | `app/(tabs)/index.tsx` |
| Intent parsing prompt | `app/api/parse-intent+api.ts` |
| Tx building & simulation | `app/api/simulate+api.ts` |
| Shared types | `lib/types.ts` |
| Jupiter swap logic | `lib/jupiter.ts` |
| Wallet adapter | `lib/wallet.ts` |

---

## Known Limitations

- **Wallet signing is Android-only.** Mobile Wallet Adapter does not support iOS or web. You can browse and simulate on any platform, but signing requires an Android device or emulator with a compatible wallet installed (e.g., Phantom).
- **Five tokens supported.** The token registry is hardcoded. Arbitrary token addresses are not yet supported.
- **No bridge support.** Multi-chain bridge steps are parsed but skipped with a warning.
- **ElevenLabs STT runs client-side.** The API key is exposed in the client bundle. This is acceptable for development; proxy the request server-side before shipping to production.
