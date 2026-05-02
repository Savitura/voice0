import Anthropic from '@anthropic-ai/sdk';
import { z } from 'zod';

const client = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });

const SYSTEM_PROMPT = `You are a DeFi intent parser for the Solana blockchain.
Parse the user's natural language intent into a structured JSON object.
You MUST respond with ONLY valid JSON, no markdown, no explanation.

Token mint registry:
- SOL: native (decimals: 9)
- USDC: EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v (decimals: 6)
- USDT: Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB (decimals: 6)
- BONK: DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263 (decimals: 5)
- JUP: JUPyiwrYJFskUPiHa7hkeR8VUtAeFoSYbKedZNsDvCN (decimals: 6)

Rules:
- Amount must be in HUMAN units (e.g. "50 USDC" -> amount: 50, NOT 50000000)
- "swap", "exchange", "convert" -> type: "swap" with inputMint, outputMint, amount, slippageBps (default 50)
- "send", "transfer" -> type: "transfer" with mint ("native" for SOL), amount, destination
- Only support swap and transfer. If the intent mentions "bridge", add a warning and skip the step.
- If destination is missing for a transfer, add to warnings[]
- Set warnings[] if there are concerns (very large amount, unknown token, etc.)
- Generate a unique id for each step (use a short random hex string)

Response schema (return ONLY this JSON, nothing else):
{
  "intent": "<original user text>",
  "steps": [
    {
      "id": "<short-hex-id>",
      "type": "swap" | "transfer",
      "humanSummary": "<plain English e.g. Swap 50 USDC to SOL via Jupiter>",
      "params": {
        // SwapParams: inputMint, outputMint, amount, slippageBps
        // TransferParams: mint, amount, destination
      }
    }
  ],
  "simulationPassed": false,
  "estimatedFeeLamports": 0,
  "warnings": []
}`;

const SwapParamsSchema = z.object({
  inputMint: z.string(),
  outputMint: z.string(),
  amount: z.number().positive(),
  slippageBps: z.number().int().min(0).max(10000),
});

const TransferParamsSchema = z.object({
  mint: z.string(),
  amount: z.number().positive(),
  destination: z.string(),
});

const StepSchema = z.object({
  id: z.string(),
  type: z.enum(['swap', 'transfer', 'bridge', 'stake']),
  humanSummary: z.string(),
  transaction: z.string().optional(),
  params: z.union([SwapParamsSchema, TransferParamsSchema]),
});

const BundleSchema = z.object({
  intent: z.string(),
  steps: z.array(StepSchema),
  simulationPassed: z.boolean(),
  estimatedFeeLamports: z.number(),
  warnings: z.array(z.string()),
});

export async function POST(request: Request): Promise<Response> {
  try {
    const body = (await request.json()) as { text?: unknown };
    const { text } = body;

    if (!text || typeof text !== 'string') {
      return Response.json({ error: 'Missing text field' }, { status: 400 });
    }

    const message = await client.messages.create({
      model: 'claude-sonnet-4-6',
      max_tokens: 1024,
      system: SYSTEM_PROMPT,
      messages: [{ role: 'user', content: text }],
    });

    const rawContent = message.content[0];
    if (rawContent.type !== 'text') {
      return Response.json(
        { error: 'Unexpected response type from Claude' },
        { status: 500 },
      );
    }

    let parsed: unknown;
    try {
      parsed = JSON.parse(rawContent.text);
    } catch {
      return Response.json(
        { error: 'Claude returned non-JSON output', raw: rawContent.text },
        { status: 422 },
      );
    }

    const validated = BundleSchema.safeParse(parsed);
    if (!validated.success) {
      return Response.json(
        {
          error: 'Claude output failed schema validation',
          issues: validated.error.issues,
        },
        { status: 422 },
      );
    }

    return Response.json(validated.data);
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Unknown error';
    return Response.json({ error: message }, { status: 500 });
  }
}
