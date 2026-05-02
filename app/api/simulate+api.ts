import {
  Connection,
  PublicKey,
  SystemProgram,
  TransactionMessage,
  VersionedTransaction,
} from '@solana/web3.js';
import {
  getAssociatedTokenAddress,
  createTransferInstruction,
  getMint,
} from '@solana/spl-token';
import { z } from 'zod';
import { simulateTx, estimateFee } from '@/lib/solana';
import { quoteAndBuild } from '@/lib/jupiter';
import { TOKENS, getSymbolFromMint } from '@/lib/tokens';
import type { SolanaTxBundle, SolanaTxStep, SwapParams, TransferParams } from '@/lib/types';

function getServerConnection(): Connection {
  const rpcUrl = process.env.HELIUS_RPC_URL;
  if (!rpcUrl) throw new Error('HELIUS_RPC_URL is not set');
  return new Connection(rpcUrl, 'confirmed');
}

async function buildTransferTx(
  p: TransferParams,
  from: PublicKey,
  connection: Connection,
): Promise<string> {
  const { blockhash } = await connection.getLatestBlockhash();
  const to = new PublicKey(p.destination);

  let instructions;
  if (p.mint === 'native') {
    const lamports = Math.floor(p.amount * 1e9);
    instructions = [
      SystemProgram.transfer({ fromPubkey: from, toPubkey: to, lamports }),
    ];
  } else {
    const mint = new PublicKey(p.mint);
    const mintInfo = await getMint(connection, mint);
    const amountBase = BigInt(
      Math.floor(p.amount * Math.pow(10, mintInfo.decimals)),
    );
    const fromATA = await getAssociatedTokenAddress(mint, from);
    const toATA = await getAssociatedTokenAddress(mint, to);
    instructions = [createTransferInstruction(fromATA, toATA, from, amountBase)];
  }

  const message = new TransactionMessage({
    payerKey: from,
    recentBlockhash: blockhash,
    instructions,
  }).compileToV0Message();

  const tx = new VersionedTransaction(message);
  return Buffer.from(tx.serialize()).toString('base64');
}

const RequestSchema = z.object({
  bundle: z.object({
    intent: z.string(),
    steps: z.array(z.unknown()),
    simulationPassed: z.boolean(),
    estimatedFeeLamports: z.number(),
    warnings: z.array(z.string()),
  }),
  userPublicKey: z.string(),
});

export async function POST(request: Request): Promise<Response> {
  try {
    const body = await request.json();
    const parsed = RequestSchema.safeParse(body);
    if (!parsed.success) {
      return Response.json({ error: 'Invalid request body' }, { status: 400 });
    }

    const { bundle, userPublicKey } = parsed.data;
    const pubkey = new PublicKey(userPublicKey);
    const connection = getServerConnection();

    let totalFeeLamports = 0;
    const enrichedSteps: SolanaTxStep[] = [];
    const warnings = [...bundle.warnings];

    for (const rawStep of bundle.steps) {
      const step = rawStep as SolanaTxStep;

      if (step.type === 'swap') {
        const p = step.params as SwapParams;
        const symbol = getSymbolFromMint(p.inputMint);
        const tokenInfo = TOKENS[symbol];
        const amountBase = Math.floor(
          p.amount * Math.pow(10, tokenInfo?.decimals ?? 6),
        );

        const { txBase64 } = await quoteAndBuild(
          { ...p, amount: amountBase },
          userPublicKey,
        );

        const simResult = await simulateTx(txBase64);
        if (!simResult.success) {
          warnings.push(
            `Simulation failed for "${step.humanSummary}": ${simResult.error}`,
          );
        }

        totalFeeLamports += estimateFee(simResult.unitsConsumed);
        enrichedSteps.push({ ...step, transaction: txBase64 });
      } else if (step.type === 'transfer') {
        const p = step.params as TransferParams;
        const txBase64 = await buildTransferTx(p, pubkey, connection);

        const simResult = await simulateTx(txBase64);
        if (!simResult.success) {
          warnings.push(
            `Simulation failed for "${step.humanSummary}": ${simResult.error}`,
          );
        }

        totalFeeLamports += estimateFee(simResult.unitsConsumed);
        enrichedSteps.push({ ...step, transaction: txBase64 });
      } else {
        warnings.push(`Unsupported step type: ${step.type} — skipped`);
        enrichedSteps.push(step);
      }
    }

    const hasSimFailure = warnings.some((w) => w.includes('Simulation failed'));
    const resultBundle: SolanaTxBundle = {
      intent: bundle.intent,
      steps: enrichedSteps,
      simulationPassed: !hasSimFailure,
      estimatedFeeLamports: totalFeeLamports,
      warnings,
    };

    return Response.json(resultBundle);
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Unknown error';
    return Response.json({ error: message }, { status: 500 });
  }
}
