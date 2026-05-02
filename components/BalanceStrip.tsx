import { ScrollView, View, Text, StyleSheet } from 'react-native';
import type { TokenBalance } from '@/lib/balances';
import { formatBalance } from '@/lib/balances';
import { formatUsd } from '@/lib/prices';

interface BalanceStripProps {
  balances: TokenBalance[];
  prices?: Record<string, number>;
}

function BalancePill({
  bal,
  prices,
}: {
  bal: TokenBalance;
  prices?: Record<string, number>;
}) {
  const price = prices?.[bal.mint];
  const usdValue = price != null ? price * bal.balance : null;
  const formatted = formatBalance(bal.balance, bal.decimals);

  return (
    <View style={styles.pill}>
      <Text style={styles.symbol}>{bal.symbol}</Text>
      <Text style={styles.amount}>{formatted}</Text>
      {usdValue != null && (
        <Text style={styles.usd}>{formatUsd(usdValue)}</Text>
      )}
    </View>
  );
}

export function BalanceStrip({ balances, prices }: BalanceStripProps) {
  if (balances.length === 0) return null;

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={styles.strip}
      style={styles.container}
    >
      {balances.map((b) => (
        <BalancePill key={b.mint} bal={b} prices={prices} />
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    maxHeight: 52,
  },
  strip: {
    paddingHorizontal: 20,
    paddingVertical: 8,
    gap: 8,
    flexDirection: 'row',
  },
  pill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: '#111827',
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderWidth: 1,
    borderColor: '#1e293b',
  },
  symbol: {
    color: '#94a3b8',
    fontSize: 11,
    fontWeight: '600',
    letterSpacing: 0.5,
  },
  amount: {
    color: '#e2e8f0',
    fontSize: 13,
    fontWeight: '500',
  },
  usd: {
    color: '#475569',
    fontSize: 11,
  },
});
