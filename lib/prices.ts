// Wrapped SOL mint — Jupiter uses this for SOL pricing
const WSOL_MINT = 'So11111111111111111111111111111111111111112';
const PRICE_API = 'https://api.jup.ag/price/v2';

function formatUsd(value: number): string {
  if (value >= 1000) {
    return '$' + value.toLocaleString('en-US', { maximumFractionDigits: 0 });
  }
  if (value >= 1) return '$' + value.toFixed(2);
  if (value >= 0.01) return '$' + value.toFixed(4);
  return '$' + value.toPrecision(3);
}

/** Fetch USD prices for a list of mint addresses.
 *  Pass 'native' for SOL — it will be mapped to wrapped SOL for the query. */
export async function fetchPrices(
  mints: string[],
): Promise<Record<string, number>> {
  const hasNative = mints.includes('native');
  const queryMints = [
    ...mints.filter((m) => m !== 'native'),
    ...(hasNative ? [WSOL_MINT] : []),
  ];

  if (queryMints.length === 0) return {};

  try {
    const url = `${PRICE_API}?ids=${queryMints.join(',')}`;
    const res = await fetch(url);
    if (!res.ok) return {};

    const data = (await res.json()) as {
      data?: Record<string, { price?: number }>;
    };
    const prices: Record<string, number> = {};

    for (const [mint, info] of Object.entries(data.data ?? {})) {
      if (typeof info.price === 'number') {
        prices[mint] = info.price;
        if (mint === WSOL_MINT) {
          prices['native'] = info.price;
        }
      }
    }

    return prices;
  } catch {
    return {};
  }
}

/** Extract all unique mints referenced by a bundle's steps. */
export function mintsFromBundle(steps: Array<{ type: string; params: unknown }>): string[] {
  const mints = new Set<string>();
  for (const step of steps) {
    const p = step.params as Record<string, unknown>;
    if (step.type === 'swap') {
      if (typeof p.inputMint === 'string') mints.add(p.inputMint);
      if (typeof p.outputMint === 'string') mints.add(p.outputMint);
    } else if (step.type === 'transfer') {
      if (typeof p.mint === 'string') mints.add(p.mint);
    }
  }
  return Array.from(mints);
}

export { formatUsd };
