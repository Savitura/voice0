import { Pressable, View, Text, StyleSheet } from 'react-native';
import type { Cluster } from '@/lib/network';

interface NetworkToggleProps {
  cluster: Cluster;
  onChange: (cluster: Cluster) => void;
}

export function NetworkToggle({ cluster, onChange }: NetworkToggleProps) {
  const isDevnet = cluster === 'devnet';

  return (
    <Pressable
      style={[styles.pill, isDevnet && styles.pillDevnet]}
      onPress={() => onChange(isDevnet ? 'mainnet-beta' : 'devnet')}
      hitSlop={8}
    >
      <View style={[styles.dot, isDevnet ? styles.dotDevnet : styles.dotMainnet]} />
      <Text style={[styles.label, isDevnet && styles.labelDevnet]}>
        {isDevnet ? 'devnet' : 'mainnet'}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  pill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 99,
    backgroundColor: '#0d2a1e',
    borderWidth: 1,
    borderColor: '#166534',
  },
  pillDevnet: {
    backgroundColor: '#2d1b00',
    borderColor: '#92400e',
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  dotMainnet: {
    backgroundColor: '#22c55e',
  },
  dotDevnet: {
    backgroundColor: '#f59e0b',
  },
  label: {
    fontSize: 11,
    fontWeight: '600',
    color: '#22c55e',
    letterSpacing: 0.3,
  },
  labelDevnet: {
    color: '#f59e0b',
  },
});
