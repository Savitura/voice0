import { View, Text, StyleSheet } from 'react-native';
import type { SolanaTxStep, SwapParams, TransferParams } from '@/lib/types';
import { MINT_TO_TOKEN } from '@/lib/tokens';
import { formatUsd } from '@/lib/prices';

interface StepCardProps {
  step: SolanaTxStep;
  index: number;
  prices?: Record<string, number>;
  executed?: boolean;
}

function getTokenSymbol(mint: string): string {
  if (mint === 'native') return 'SOL';
  return MINT_TO_TOKEN[mint]?.symbol ?? mint.slice(0, 6) + '…';
}

function truncateAddress(address: string): string {
  if (address.length <= 12) return address;
  return address.slice(0, 6) + '…' + address.slice(-4);
}

function UsdBadge({ amount, mint, prices }: { amount: number; mint: string; prices?: Record<string, number> }) {
  const price = prices?.[mint];
  if (price == null) return null;
  return (
    <Text style={styles.usdBadge}>{formatUsd(amount * price)}</Text>
  );
}

export function StepCard({ step, index, prices, executed = false }: StepCardProps) {
  const isSwap = step.type === 'swap';
  const isTransfer = step.type === 'transfer';

  return (
    <View style={[styles.card, executed && styles.cardExecuted]}>
      <View style={styles.header}>
        <View style={styles.badge}>
          <Text style={styles.badgeText}>{index + 1}</Text>
        </View>
        <View style={styles.typeTag}>
          <Text style={styles.typeText}>{step.type.toUpperCase()}</Text>
        </View>
        {executed && (
          <View style={styles.doneTag}>
            <Text style={styles.doneText}>DONE</Text>
          </View>
        )}
      </View>

      <Text style={styles.summary}>{step.humanSummary}</Text>

      {isSwap && (
        <View style={styles.params}>
          {(() => {
            const p = step.params as SwapParams;
            return (
              <>
                <View style={styles.paramRowWrap}>
                  <Text style={styles.paramRow}>
                    <Text style={styles.paramLabel}>From </Text>
                    <Text style={styles.paramValue}>
                      {p.amount} {getTokenSymbol(p.inputMint)}
                    </Text>
                  </Text>
                  <UsdBadge amount={p.amount} mint={p.inputMint} prices={prices} />
                </View>
                <Text style={styles.arrow}>↓</Text>
                <Text style={styles.paramRow}>
                  <Text style={styles.paramLabel}>To </Text>
                  <Text style={styles.paramValue}>
                    {getTokenSymbol(p.outputMint)}
                  </Text>
                </Text>
                <Text style={styles.slippage}>
                  Slippage: {p.slippageBps / 100}%
                </Text>
              </>
            );
          })()}
        </View>
      )}

      {isTransfer && (
        <View style={styles.params}>
          {(() => {
            const p = step.params as TransferParams;
            return (
              <>
                <View style={styles.paramRowWrap}>
                  <Text style={styles.paramRow}>
                    <Text style={styles.paramLabel}>Amount </Text>
                    <Text style={styles.paramValue}>
                      {p.amount} {getTokenSymbol(p.mint)}
                    </Text>
                  </Text>
                  <UsdBadge amount={p.amount} mint={p.mint} prices={prices} />
                </View>
                {p.destination ? (
                  <Text style={styles.paramRow}>
                    <Text style={styles.paramLabel}>To </Text>
                    <Text style={styles.paramValue}>
                      {truncateAddress(p.destination)}
                    </Text>
                  </Text>
                ) : (
                  <Text style={styles.missingDest}>⚠ No destination set</Text>
                )}
              </>
            );
          })()}
        </View>
      )}

      {step.type !== 'swap' && step.type !== 'transfer' && (
        <Text style={styles.unsupported}>
          ⚠ {step.type} not supported in MVP
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#1a1a2e',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#2a2a4a',
  },
  cardExecuted: {
    borderColor: '#22c55e',
    opacity: 0.7,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
    gap: 8,
  },
  badge: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#0a7ea4',
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '700',
  },
  typeTag: {
    backgroundColor: '#0a3a4a',
    borderRadius: 4,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  typeText: {
    color: '#0a7ea4',
    fontSize: 10,
    fontWeight: '600',
    letterSpacing: 1,
  },
  doneTag: {
    backgroundColor: '#052e16',
    borderRadius: 4,
    paddingHorizontal: 6,
    paddingVertical: 2,
    marginLeft: 'auto',
  },
  doneText: {
    color: '#22c55e',
    fontSize: 10,
    fontWeight: '600',
  },
  summary: {
    color: '#e2e8f0',
    fontSize: 15,
    fontWeight: '500',
    marginBottom: 10,
  },
  params: {
    gap: 4,
  },
  paramRowWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  paramRow: {
    fontSize: 13,
    color: '#94a3b8',
  },
  paramLabel: {
    color: '#64748b',
  },
  paramValue: {
    color: '#cbd5e1',
    fontWeight: '500',
  },
  usdBadge: {
    color: '#22c55e',
    fontSize: 12,
    fontWeight: '500',
  },
  arrow: {
    color: '#0a7ea4',
    fontSize: 16,
    marginLeft: 2,
  },
  slippage: {
    color: '#475569',
    fontSize: 11,
    marginTop: 4,
  },
  missingDest: {
    color: '#f59e0b',
    fontSize: 13,
  },
  unsupported: {
    color: '#f59e0b',
    fontSize: 13,
  },
});
