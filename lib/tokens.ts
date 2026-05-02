export interface TokenInfo {
  symbol: string;
  mint: string;
  decimals: number;
  name: string;
}

export const TOKENS: Record<string, TokenInfo> = {
  SOL: { symbol: 'SOL', mint: 'native', decimals: 9, name: 'Solana' },
  USDC: {
    symbol: 'USDC',
    mint: 'EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v',
    decimals: 6,
    name: 'USD Coin',
  },
  USDT: {
    symbol: 'USDT',
    mint: 'Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB',
    decimals: 6,
    name: 'Tether USD',
  },
  BONK: {
    symbol: 'BONK',
    mint: 'DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263',
    decimals: 5,
    name: 'Bonk',
  },
  JUP: {
    symbol: 'JUP',
    mint: 'JUPyiwrYJFskUPiHa7hkeR8VUtAeFoSYbKedZNsDvCN',
    decimals: 6,
    name: 'Jupiter',
  },
};

export const MINT_TO_TOKEN: Record<string, TokenInfo> = Object.fromEntries(
  Object.values(TOKENS).map((t) => [t.mint, t]),
);

export function toBaseUnits(amount: number, symbol: string): number {
  const token = TOKENS[symbol.toUpperCase()];
  if (!token) throw new Error(`Unknown token: ${symbol}`);
  return Math.floor(amount * Math.pow(10, token.decimals));
}

export function fromBaseUnits(amount: number, symbol: string): number {
  const token = TOKENS[symbol.toUpperCase()];
  if (!token) throw new Error(`Unknown token: ${symbol}`);
  return amount / Math.pow(10, token.decimals);
}

export function getSymbolFromMint(mint: string): string {
  if (mint === 'native') return 'SOL';
  return MINT_TO_TOKEN[mint]?.symbol ?? 'UNKNOWN';
}
