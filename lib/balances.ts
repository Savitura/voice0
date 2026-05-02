import { Connection, PublicKey, LAMPORTS_PER_SOL } from '@solana/web3.js';
import { MINT_TO_TOKEN } from '@/lib/tokens';

export interface TokenBalance {
  symbol: string;
  mint: string;
  balance: number;
  decimals: number;
}

export async function fetchBalances(
  publicKey: PublicKey,
  connection: Connection,
): Promise<TokenBalance[]> {
  // Lazy import: @solana/spl-token → @solana/spl-token-metadata uses `Buffer`
  // as a global at module init time. If imported at the top of this file it
  // would be evaluated before Expo Router finishes loading _layout.tsx (which
  // sets up the Buffer polyfill), crashing on `Property 'Buffer' doesn't exist`.
  // Deferring the import to the first function call guarantees the polyfill is
  // already in place.
  const { TOKEN_PROGRAM_ID } = await import('@solana/spl-token');

  const results: TokenBalance[] = [];

  const [lamports, tokenAccounts] = await Promise.all([
    connection.getBalance(publicKey),
    connection.getParsedTokenAccountsByOwner(publicKey, {
      programId: TOKEN_PROGRAM_ID,
    }),
  ]);

  results.push({
    symbol: 'SOL',
    mint: 'native',
    balance: lamports / LAMPORTS_PER_SOL,
    decimals: 9,
  });

  for (const { account } of tokenAccounts.value) {
    const info = (account.data as { parsed?: { info?: unknown } }).parsed?.info as
      | { mint?: string; tokenAmount?: { uiAmount?: number } }
      | undefined;

    if (!info?.mint) continue;

    const tokenDef = MINT_TO_TOKEN[info.mint];
    if (!tokenDef) continue;

    const amount = info.tokenAmount?.uiAmount ?? 0;
    if (amount <= 0) continue;

    results.push({
      symbol: tokenDef.symbol,
      mint: info.mint,
      balance: amount,
      decimals: tokenDef.decimals,
    });
  }

  return results;
}

export function formatBalance(balance: number, decimals: number): string {
  if (balance === 0) return '0';
  if (balance >= 1_000_000) return (balance / 1_000_000).toFixed(1) + 'M';
  if (balance >= 1000) return balance.toLocaleString('en-US', { maximumFractionDigits: 0 });
  if (balance >= 1) return balance.toFixed(2);
  if (balance >= 0.001) return balance.toFixed(4);
  return balance.toPrecision(3);
}
