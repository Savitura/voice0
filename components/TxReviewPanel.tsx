import {
  View,
  Text,
  ScrollView,
  Pressable,
  StyleSheet,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import type { SolanaTxBundle } from '@/lib/types';
import { StepCard } from '@/components/StepCard';

interface TxReviewPanelProps {
  bundle: SolanaTxBundle;
  prices?: Record<string, number>;
  transcript: string;
  onConfirm: () => void;
  onDismiss: () => void;
  onEditIntent: () => void;
}

function formatLamports(lamports: number): string {
  const sol = lamports / 1e9;
  return sol < 0.0001 ? `${lamports} lamports` : `~${sol.toFixed(5)} SOL`;
}

export function TxReviewPanel({
  bundle,
  prices,
  transcript,
  onConfirm,
  onDismiss,
  onEditIntent,
}: TxReviewPanelProps) {
  const hasWarnings = bundle.warnings.length > 0;
  const canExecute = bundle.simulationPassed && bundle.steps.some((s) => s.transaction != null);

  return (
    <SafeAreaView style={styles.root}>
      {/* ── Header ── */}
      <View style={styles.header}>
        <Text style={styles.title}>Review Transaction</Text>
        <Pressable style={styles.closeBtn} onPress={onDismiss} hitSlop={12}>
          <Text style={styles.closeIcon}>✕</Text>
        </Pressable>
      </View>

      {/* ── Transcript quote ── */}
      <View style={styles.quoteBlock}>
        <Text style={styles.quoteText}>"{transcript}"</Text>
      </View>

      {/* ── Scrollable body ── */}
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* Step cards */}
        {bundle.steps.map((step, i) => (
          <View key={step.id}>
            <StepCard
              step={step}
              index={i}
              prices={prices}
              status={bundle.simulationPassed ? 'ok' : 'pending'}
            />
            {/* Connector between steps */}
            {i < bundle.steps.length - 1 && <View style={styles.connector} />}
          </View>
        ))}

        {/* Simulation result block */}
        <View style={styles.simBlock}>
          <View style={styles.simRow}>
            <Text style={styles.simLabel}>Simulation</Text>
            <Text style={[styles.simValue, bundle.simulationPassed ? styles.simOk : styles.simFail]}>
              {bundle.simulationPassed ? '✓ Passed' : '✗ Failed'}
            </Text>
          </View>
          <View style={styles.simRow}>
            <Text style={styles.simLabel}>Estimated fee</Text>
            <Text style={styles.simValue}>{formatLamports(bundle.estimatedFeeLamports)}</Text>
          </View>
          {bundle.computeUnits != null && (
            <View style={styles.simRow}>
              <Text style={styles.simLabel}>Compute units</Text>
              <Text style={styles.simValue}>{bundle.computeUnits.toLocaleString()}</Text>
            </View>
          )}
        </View>

        {/* Simulation failed warning */}
        {!bundle.simulationPassed && (
          <View style={styles.simFailCard}>
            <Text style={styles.simFailTitle}>⚠ Simulation failed</Text>
            <Text style={styles.simFailBody}>
              This transaction could not be simulated. Execution is disabled until the intent is
              corrected.
            </Text>
          </View>
        )}

        {/* Warnings */}
        {hasWarnings && (
          <View style={styles.warningsCard}>
            <Text style={styles.warningsTitle}>⚠ Warnings</Text>
            {bundle.warnings.map((w, i) => (
              <Text key={i} style={styles.warningItem}>• {w}</Text>
            ))}
          </View>
        )}

        <View style={styles.scrollPad} />
      </ScrollView>

      {/* ── Sticky footer ── */}
      <View style={styles.footer}>
        <Pressable style={styles.editBtn} onPress={onEditIntent}>
          <Text style={styles.editBtnText}>Edit Intent</Text>
        </Pressable>
        <Pressable
          style={[styles.execBtn, !canExecute && styles.execBtnDisabled]}
          onPress={canExecute ? onConfirm : undefined}
        >
          <Text style={styles.execBtnText}>Execute</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#080818',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#0f172a',
  },
  title: {
    color: '#e2e8f0',
    fontSize: 18,
    fontWeight: '600',
  },
  closeBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#0f172a',
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeIcon: {
    color: '#64748b',
    fontSize: 14,
    fontWeight: '600',
  },
  quoteBlock: {
    marginHorizontal: 20,
    marginTop: 16,
    marginBottom: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#0c1221',
    borderRadius: 10,
    borderLeftWidth: 3,
    borderLeftColor: '#0369a1',
  },
  quoteText: {
    color: '#94a3b8',
    fontSize: 14,
    fontStyle: 'italic',
    lineHeight: 20,
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingTop: 8,
  },
  connector: {
    width: 2,
    height: 16,
    backgroundColor: '#1e293b',
    alignSelf: 'center',
    marginVertical: -4,
    zIndex: 1,
  },
  simBlock: {
    marginTop: 16,
    backgroundColor: '#0c1221',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#0f1a2e',
    overflow: 'hidden',
  },
  simRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#0f172a',
  },
  simLabel: {
    color: '#64748b',
    fontSize: 13,
  },
  simValue: {
    color: '#94a3b8',
    fontSize: 13,
    fontWeight: '500',
  },
  simOk: {
    color: '#22c55e',
  },
  simFail: {
    color: '#f87171',
  },
  simFailCard: {
    marginTop: 12,
    backgroundColor: '#1a0a0a',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#7f1d1d',
    padding: 14,
    gap: 6,
  },
  simFailTitle: {
    color: '#f87171',
    fontSize: 13,
    fontWeight: '600',
  },
  simFailBody: {
    color: '#9a3333',
    fontSize: 12,
    lineHeight: 18,
  },
  warningsCard: {
    marginTop: 12,
    backgroundColor: '#141005',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#78350f',
    padding: 14,
    gap: 6,
  },
  warningsTitle: {
    color: '#f59e0b',
    fontSize: 13,
    fontWeight: '600',
  },
  warningItem: {
    color: '#92400e',
    fontSize: 12,
    lineHeight: 18,
  },
  scrollPad: {
    height: 24,
  },
  footer: {
    flexDirection: 'row',
    gap: 12,
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderTopWidth: 1,
    borderTopColor: '#0f172a',
    backgroundColor: '#080818',
  },
  editBtn: {
    flex: 0,
    paddingHorizontal: 18,
    paddingVertical: 14,
    borderRadius: 14,
    backgroundColor: '#0f172a',
    borderWidth: 1,
    borderColor: '#1e293b',
    alignItems: 'center',
    justifyContent: 'center',
  },
  editBtnText: {
    color: '#64748b',
    fontSize: 14,
    fontWeight: '500',
  },
  execBtn: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 14,
    backgroundColor: '#0369a1',
    alignItems: 'center',
    justifyContent: 'center',
  },
  execBtnDisabled: {
    backgroundColor: '#0c1a2e',
    opacity: 0.5,
  },
  execBtnText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '700',
    letterSpacing: 0.3,
  },
});
