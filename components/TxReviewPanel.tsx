import {
  View,
  Text,
  ScrollView,
  Pressable,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import type { SolanaTxBundle } from '@/lib/types';
import { StepCard } from '@/components/StepCard';

interface TxReviewPanelProps {
  bundle: SolanaTxBundle;
  prices?: Record<string, number>;
  onConfirm: () => void;
  onCancel: () => void;
  isExecuting: boolean;
}

function formatLamports(lamports: number): string {
  const sol = lamports / 1e9;
  if (sol < 0.001) return `${lamports} lamports`;
  return `~${sol.toFixed(5)} SOL`;
}

export function TxReviewPanel({
  bundle,
  prices,
  onConfirm,
  onCancel,
  isExecuting,
}: TxReviewPanelProps) {
  const hasWarnings = bundle.warnings.length > 0;
  const canExecute = bundle.steps.some((s) => s.transaction != null);

  return (
    <View style={styles.panel}>
      <View style={styles.handle} />

      <Text style={styles.intentLabel}>Parsed intent</Text>
      <Text style={styles.intentText}>"{bundle.intent}"</Text>

      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false}>
        <Text style={styles.sectionLabel}>
          {bundle.steps.length} step{bundle.steps.length !== 1 ? 's' : ''}
        </Text>

        {bundle.steps.map((step, i) => (
          <StepCard key={step.id} step={step} index={i} prices={prices} />
        ))}

        {hasWarnings && (
          <View style={styles.warningsBox}>
            <Text style={styles.warningsTitle}>⚠ Warnings</Text>
            {bundle.warnings.map((w, i) => (
              <Text key={i} style={styles.warningItem}>
                • {w}
              </Text>
            ))}
          </View>
        )}

        <View style={styles.feeRow}>
          <Text style={styles.feeLabel}>Estimated fee</Text>
          <Text style={styles.feeValue}>
            {formatLamports(bundle.estimatedFeeLamports)}
          </Text>
        </View>

        {!bundle.simulationPassed && (
          <Text style={styles.simFailed}>
            ⚠ Simulation did not pass — review warnings before confirming
          </Text>
        )}
      </ScrollView>

      <View style={styles.actions}>
        <Pressable
          style={[styles.btn, styles.cancelBtn]}
          onPress={onCancel}
          disabled={isExecuting}
        >
          <Text style={styles.cancelText}>Cancel</Text>
        </Pressable>

        <Pressable
          style={[
            styles.btn,
            styles.confirmBtn,
            (!canExecute || isExecuting) && styles.btnDisabled,
          ]}
          onPress={onConfirm}
          disabled={!canExecute || isExecuting}
        >
          {isExecuting ? (
            <ActivityIndicator color="#fff" size="small" />
          ) : (
            <Text style={styles.confirmText}>
              {bundle.simulationPassed ? 'Confirm & Execute' : 'Execute Anyway'}
            </Text>
          )}
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  panel: {
    backgroundColor: '#0f0f23',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingTop: 12,
    paddingHorizontal: 20,
    paddingBottom: 32,
    maxHeight: '80%',
    borderTopWidth: 1,
    borderColor: '#1e1e3a',
  },
  handle: {
    width: 40,
    height: 4,
    backgroundColor: '#334155',
    borderRadius: 2,
    alignSelf: 'center',
    marginBottom: 16,
  },
  intentLabel: {
    color: '#64748b',
    fontSize: 11,
    textTransform: 'uppercase',
    letterSpacing: 1,
    marginBottom: 4,
  },
  intentText: {
    color: '#e2e8f0',
    fontSize: 16,
    fontStyle: 'italic',
    marginBottom: 20,
  },
  scroll: {
    flex: 1,
  },
  sectionLabel: {
    color: '#64748b',
    fontSize: 12,
    marginBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  warningsBox: {
    backgroundColor: '#1c1508',
    borderRadius: 8,
    padding: 12,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#78350f',
  },
  warningsTitle: {
    color: '#f59e0b',
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 6,
  },
  warningItem: {
    color: '#d97706',
    fontSize: 12,
    lineHeight: 18,
  },
  feeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderTopWidth: 1,
    borderColor: '#1e293b',
    marginBottom: 8,
  },
  feeLabel: {
    color: '#64748b',
    fontSize: 14,
  },
  feeValue: {
    color: '#94a3b8',
    fontSize: 14,
    fontWeight: '500',
  },
  simFailed: {
    color: '#f59e0b',
    fontSize: 12,
    textAlign: 'center',
    marginBottom: 8,
  },
  actions: {
    flexDirection: 'row',
    gap: 12,
    paddingTop: 16,
  },
  btn: {
    flex: 1,
    height: 52,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cancelBtn: {
    backgroundColor: '#1e293b',
    flex: 0.4,
  },
  confirmBtn: {
    backgroundColor: '#0a7ea4',
  },
  btnDisabled: {
    opacity: 0.4,
  },
  cancelText: {
    color: '#94a3b8',
    fontSize: 16,
    fontWeight: '500',
  },
  confirmText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
