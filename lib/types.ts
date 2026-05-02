export interface SwapParams {
  inputMint: string;
  outputMint: string;
  amount: number;
  slippageBps: number;
}

export interface TransferParams {
  mint: string; // 'native' for SOL
  amount: number;
  destination: string;
}

export type TxParams = SwapParams | TransferParams;

export interface SolanaTxStep {
  id: string;
  type: 'swap' | 'transfer' | 'bridge' | 'stake';
  humanSummary: string;
  transaction?: string; // base64 serialized VersionedTransaction
  params: TxParams;
}

export interface SolanaTxBundle {
  intent: string;
  steps: SolanaTxStep[];
  simulationPassed: boolean;
  estimatedFeeLamports: number;
  warnings: string[];
}

export type AppPhase =
  | 'idle'
  | 'recording'
  | 'transcribing'
  | 'parsing'
  | 'simulating'
  | 'reviewing'
  | 'executing'
  | 'done'
  | 'error';

export interface AppError {
  phase: AppPhase;
  message: string;
}
