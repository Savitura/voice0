import { Connection, VersionedTransaction } from '@solana/web3.js';
import { getCluster, getRpcUrl, type Cluster } from '@/lib/network';

let _connection: Connection | null = null;
let _connectionCluster: Cluster | null = null;

export function getConnection(cluster?: Cluster): Connection {
  const c = cluster ?? getCluster();
  if (!_connection || _connectionCluster !== c) {
    _connection = new Connection(getRpcUrl(c), 'confirmed');
    _connectionCluster = c;
  }
  return _connection;
}

export interface SimulationResult {
  success: boolean;
  logs: string[];
  unitsConsumed: number;
  error: string | null;
}

export async function simulateTx(
  txBase64: string,
  connection?: Connection,
): Promise<SimulationResult> {
  const conn = connection ?? getConnection();
  const txBytes = Buffer.from(txBase64, 'base64');
  const tx = VersionedTransaction.deserialize(txBytes);

  const response = await conn.simulateTransaction(tx, {
    sigVerify: false,
    replaceRecentBlockhash: true,
  });

  const { err, logs, unitsConsumed } = response.value;

  return {
    success: err === null,
    logs: logs ?? [],
    unitsConsumed: unitsConsumed ?? 0,
    error: err ? JSON.stringify(err) : null,
  };
}

export function estimateFee(unitsConsumed: number): number {
  return 5000 + unitsConsumed;
}
