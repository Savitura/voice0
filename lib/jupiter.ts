import type { SwapParams } from '@/lib/types';

const QUOTE_API = 'https://quote-api.jup.ag/v6/quote';
const SWAP_API = 'https://quote-api.jup.ag/v6/swap';

export interface JupiterQuote {
  inputMint: string;
  outputMint: string;
  inAmount: string;
  outAmount: string;
  otherAmountThreshold: string;
  swapMode: string;
  slippageBps: number;
  priceImpactPct: string;
  routePlan: unknown[];
  [key: string]: unknown;
}

export async function getQuote(params: SwapParams): Promise<JupiterQuote> {
  const url = new URL(QUOTE_API);
  url.searchParams.set('inputMint', params.inputMint);
  url.searchParams.set('outputMint', params.outputMint);
  url.searchParams.set('amount', String(params.amount));
  url.searchParams.set('slippageBps', String(params.slippageBps));
  url.searchParams.set('onlyDirectRoutes', 'false');

  const res = await fetch(url.toString());
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Jupiter quote failed (${res.status}): ${text}`);
  }
  return res.json() as Promise<JupiterQuote>;
}

export async function buildSwapTransaction(
  quote: JupiterQuote,
  userPublicKey: string,
): Promise<string> {
  const res = await fetch(SWAP_API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      quoteResponse: quote,
      userPublicKey,
      wrapAndUnwrapSol: true,
      dynamicComputeUnitLimit: true,
      prioritizationFeeLamports: 'auto',
    }),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Jupiter swap build failed (${res.status}): ${text}`);
  }

  const data = (await res.json()) as { swapTransaction?: string };
  if (!data.swapTransaction) {
    throw new Error('Jupiter returned no swapTransaction field');
  }
  return data.swapTransaction;
}

export async function quoteAndBuild(
  params: SwapParams,
  userPublicKey: string,
): Promise<{ quote: JupiterQuote; txBase64: string }> {
  const quote = await getQuote(params);
  const txBase64 = await buildSwapTransaction(quote, userPublicKey);
  return { quote, txBase64 };
}
