import { Platform } from 'react-native';
import { PublicKey, VersionedTransaction } from '@solana/web3.js';
import { getCluster, type Cluster } from '@/lib/network';

const APP_IDENTITY = {
  name: 'voice0',
  uri: 'https://voice0.app',
  icon: 'favicon.ico',
} as const;

export interface WalletSession {
  publicKey: PublicKey;
  authToken: string;
  cluster: Cluster;
}

let _session: WalletSession | null = null;

async function getTransact() {
  if (Platform.OS !== 'android') {
    throw new Error('Mobile Wallet Adapter is only supported on Android');
  }
  const { transact } = await import(
    '@solana-mobile/mobile-wallet-adapter-protocol-web3js'
  );
  return transact;
}

export async function connectWallet(): Promise<WalletSession> {
  const transact = await getTransact();
  const cluster = getCluster();

  return transact(async (wallet) => {
    const authResult = await wallet.authorize({
      cluster,
      identity: APP_IDENTITY,
    });

    const publicKey = new PublicKey(authResult.accounts[0].address);
    _session = { publicKey, authToken: authResult.auth_token, cluster };
    return _session;
  });
}

export async function signAndSendTransactions(
  txBase64Array: string[],
): Promise<string[]> {
  const transact = await getTransact();
  const cluster = getCluster();

  return transact(async (wallet) => {
    if (_session?.authToken && _session.cluster === cluster) {
      try {
        await wallet.reauthorize({
          auth_token: _session.authToken,
          identity: APP_IDENTITY,
        });
      } catch {
        const authResult = await wallet.authorize({ cluster, identity: APP_IDENTITY });
        const publicKey = new PublicKey(authResult.accounts[0].address);
        _session = { publicKey, authToken: authResult.auth_token, cluster };
      }
    } else {
      const authResult = await wallet.authorize({ cluster, identity: APP_IDENTITY });
      const publicKey = new PublicKey(authResult.accounts[0].address);
      _session = { publicKey, authToken: authResult.auth_token, cluster };
    }

    const serializedTxs = txBase64Array.map((b64) =>
      VersionedTransaction.deserialize(Buffer.from(b64, 'base64')),
    );

    const signatures = await wallet.signAndSendTransactions({
      transactions: serializedTxs,
    });

    return signatures.map((sig) => Buffer.from(sig).toString('base64'));
  });
}

/** Returns the cached session if it matches the current cluster, otherwise null. */
export function getCachedSession(): WalletSession | null {
  if (_session && _session.cluster !== getCluster()) {
    return null;
  }
  return _session;
}

export function clearSession(): void {
  _session = null;
}
