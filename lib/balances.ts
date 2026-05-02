import { Connection, PublicKey, LAMPORTS_PER_SOL } from '@solana/web3.js';
import { TOKEN_PROGRAM_ID } from '@solana/spl-token';
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
    if (!tokenDef) continue; // skip tokens not in our registry

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
  if (decimals <= 2) return balance.toLocaleString('en-US');
  // Show at most 4 sig figs
  if (balance >= 1000) return balance.toLocaleString('en-US', { maximumFractionDigits: 0 });
  if (balance >= 1) return balance.toFixed(2);
  if (balance >= 0.001) return balance.toFixed(4);
  // Very small amounts (like BONK in large quantity)
  if (balance >= 1_000_000) return (balance / 1_000_000).toFixed(1) + 'M';
  return balance.toPrecision(3);
}
