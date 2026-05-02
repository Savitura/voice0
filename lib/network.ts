export type Cluster = 'mainnet-beta' | 'devnet';

let _cluster: Cluster = 'mainnet-beta';

export const getCluster = (): Cluster => _cluster;
export const isDevnet = (): boolean => _cluster === 'devnet';

export function setCluster(cluster: Cluster): void {
  _cluster = cluster;
}

export function getRpcUrl(cluster?: Cluster): string {
  const c = cluster ?? _cluster;
  if (c === 'devnet') {
    // EXPO_PUBLIC_ available client-side; plain var available server-side
    return (
      process.env.EXPO_PUBLIC_HELIUS_DEVNET_RPC_URL ??
      process.env.HELIUS_DEVNET_RPC_URL ??
      'https://api.devnet.solana.com'
    );
  }
  return (
    process.env.EXPO_PUBLIC_HELIUS_RPC_URL ??
    process.env.HELIUS_RPC_URL ??
    'https://api.mainnet-beta.solana.com'
  );
}
