import { useState, useCallback, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  Alert,
  Platform,
  SafeAreaView,
  StyleSheet,
} from 'react-native';
import { VoiceButton } from '@/components/VoiceButton';
import { TxReviewPanel } from '@/components/TxReviewPanel';
import { NetworkToggle } from '@/components/NetworkToggle';
import { BalanceStrip } from '@/components/BalanceStrip';
import { startRecording, stopRecording, transcribeAudio } from '@/lib/elevenlabs';
import { signAndSendTransactions, connectWallet, getCachedSession, clearSession } from '@/lib/wallet';
import { getCluster, setCluster, type Cluster } from '@/lib/network';
import { getConnection } from '@/lib/solana';
import { fetchBalances, type TokenBalance } from '@/lib/balances';
import { fetchPrices, mintsFromBundle } from '@/lib/prices';
import type { AppPhase, AppError, SolanaTxBundle } from '@/lib/types';

const BASE_URL = process.env.EXPO_PUBLIC_BASE_URL ?? 'http://localhost:8081';

async function parseIntent(text: string): Promise<SolanaTxBundle> {
  const res = await fetch(`${BASE_URL}/api/parse-intent`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error ?? 'Parse failed');
  return data as SolanaTxBundle;
}

async function simulateBundle(
  bundle: SolanaTxBundle,
  userPublicKey: string,
  cluster: Cluster,
): Promise<SolanaTxBundle> {
  const res = await fetch(`${BASE_URL}/api/simulate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ bundle, userPublicKey, cluster }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error ?? 'Simulate failed');
  return data as SolanaTxBundle;
}

export default function HomeScreen() {
  const [cluster, setClusterState] = useState<Cluster>(getCluster());
  const [phase, setPhase] = useState<AppPhase>('idle');
  const [error, setError] = useState<AppError | null>(null);
  const [transcript, setTranscript] = useState('');
  const [bundle, setBundle] = useState<SolanaTxBundle | null>(null);
  const [prices, setPrices] = useState<Record<string, number>>({});
  const [balances, setBalances] = useState<TokenBalance[]>([]);
  const [signatures, setSignatures] = useState<string[]>([]);
  const [isExecuting, setIsExecuting] = useState(false);

  const refreshBalances = useCallback(async () => {
    const session = getCachedSession();
    if (!session) return;
    try {
      const conn = getConnection();
      const bals = await fetchBalances(session.publicKey, conn);
      setBalances(bals);
      // Fetch prices for all balance mints
      const mints = bals.map((b) => b.mint);
      const p = await fetchPrices(mints);
      setPrices((prev) => ({ ...prev, ...p }));
    } catch {
      // Silently fail — balances are non-critical
    }
  }, []);

  const resetToIdle = useCallback(() => {
    setPhase('idle');
    setError(null);
    setTranscript('');
    setBundle(null);
    setSignatures([]);
    setIsExecuting(false);
  }, []);

  const handleNetworkChange = useCallback(
    (newCluster: Cluster) => {
      setCluster(newCluster);
      setClusterState(newCluster);
      clearSession();
      setBalances([]);
      resetToIdle();
    },
    [resetToIdle],
  );

  const handlePressIn = useCallback(() => {
    if (phase !== 'idle') return;
    setPhase('recording');
    startRecording().catch((err: unknown) => {
      setError({
        phase: 'recording',
        message: err instanceof Error ? err.message : 'Recording failed',
      });
      setPhase('error');
    });
  }, [phase]);

  const handlePressOut = useCallback(async () => {
    if (phase !== 'recording') return;

    try {
      setPhase('transcribing');
      const uri = await stopRecording();
      const text = await transcribeAudio(uri);
      setTranscript(text);

      let session = getCachedSession();
      if (!session && Platform.OS === 'android') {
        session = await connectWallet();
        // Fetch balances after first wallet connect
        if (session) {
          const conn = getConnection();
          const bals = await fetchBalances(session.publicKey, conn);
          setBalances(bals);
          const mints = bals.map((b) => b.mint);
          fetchPrices(mints).then((p) => setPrices((prev) => ({ ...prev, ...p })));
        }
      }
      const pubkey = session?.publicKey.toBase58() ?? '';

      setPhase('parsing');
      const parsed = await parseIntent(text);

      setPhase('simulating');
      const simulated = pubkey
        ? await simulateBundle(parsed, pubkey, cluster)
        : {
            ...parsed,
            warnings: [
              ...parsed.warnings,
              'No wallet connected — simulation skipped',
            ],
          };

      // Fetch prices for the bundle's tokens in parallel
      const bundleMints = mintsFromBundle(simulated.steps);
      fetchPrices(bundleMints).then((p) => setPrices((prev) => ({ ...prev, ...p })));

      setBundle(simulated);
      setPhase('reviewing');
    } catch (err) {
      setError({
        phase: phase as AppPhase,
        message: err instanceof Error ? err.message : 'Unknown error',
      });
      setPhase('error');
    }
  }, [phase, cluster]);

  const handleConfirm = useCallback(async () => {
    if (!bundle) return;

    const txBase64s = bundle.steps
      .map((s) => s.transaction)
      .filter((t): t is string => t != null);

    if (txBase64s.length === 0) {
      Alert.alert('Nothing to execute', 'No transactions were built.');
      return;
    }

    if (Platform.OS !== 'android') {
      Alert.alert('Android only', 'Mobile Wallet Adapter is only supported on Android.');
      return;
    }

    try {
      setIsExecuting(true);
      setPhase('executing');
      const sigs = await signAndSendTransactions(txBase64s);
      setSignatures(sigs);
      setPhase('done');
      // Refresh balances after execution
      refreshBalances();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Execution failed';
      const isUserRejection =
        msg.toLowerCase().includes('cancelled') ||
        msg.toLowerCase().includes('rejected');

      if (isUserRejection) {
        setPhase('reviewing');
      } else {
        setError({ phase: 'executing', message: msg });
        setPhase('error');
      }
    } finally {
      setIsExecuting(false);
    }
  }, [bundle, refreshBalances]);

  const handleCancel = useCallback(() => resetToIdle(), [resetToIdle]);

  return (
    <SafeAreaView style={styles.root}>
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.headerRow}>
          <Text style={styles.title}>voice0</Text>
          <NetworkToggle cluster={cluster} onChange={handleNetworkChange} />
        </View>
        <Text style={styles.subtitle}>Speak your DeFi intent</Text>
      </View>

      {/* Balance strip — visible when wallet is connected */}
      {balances.length > 0 && (
        <BalanceStrip balances={balances} prices={prices} />
      )}

      {/* Main content */}
      {phase !== 'reviewing' && (
        <View style={styles.center}>
          <VoiceButton
            phase={phase}
            onPressIn={handlePressIn}
            onPressOut={handlePressOut}
          />

          {transcript.length > 0 && phase !== 'idle' && phase !== 'error' && (
            <Text style={styles.transcript}>"{transcript}"</Text>
          )}

          {phase === 'error' && error && (
            <View style={styles.errorBox}>
              <Text style={styles.errorText}>{error.message}</Text>
              <Text style={styles.retryHint} onPress={resetToIdle}>
                Tap to try again
              </Text>
            </View>
          )}

          {phase === 'done' && signatures.length > 0 && (
            <View style={styles.doneBox}>
              <Text style={styles.doneTitle}>Transactions sent!</Text>
              {signatures.map((sig, i) => (
                <Text key={i} style={styles.sig}>
                  {sig.slice(0, 20)}…
                </Text>
              ))}
              <Text style={styles.retryHint} onPress={resetToIdle}>
                Tap to start over
              </Text>
            </View>
          )}

          {phase === 'done' && signatures.length === 0 && (
            <Text style={styles.retryHint} onPress={resetToIdle}>
              Tap to start over
            </Text>
          )}
        </View>
      )}

      {/* Transaction review panel */}
      {phase === 'reviewing' && bundle && (
        <View style={styles.reviewContainer}>
          <ScrollView contentContainerStyle={styles.reviewScroll}>
            <TxReviewPanel
              bundle={bundle}
              prices={prices}
              onConfirm={handleConfirm}
              onCancel={handleCancel}
              isExecuting={isExecuting}
            />
          </ScrollView>
        </View>
      )}

      {Platform.OS !== 'android' && (
        <Text style={styles.androidWarning}>
          ⚠ Wallet execution requires Android + a Solana wallet app
        </Text>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#080818',
  },
  header: {
    paddingTop: 24,
    paddingHorizontal: 24,
    paddingBottom: 12,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  title: {
    color: '#e2e8f0',
    fontSize: 28,
    fontWeight: '700',
    letterSpacing: -0.5,
  },
  subtitle: {
    color: '#475569',
    fontSize: 14,
    marginTop: 4,
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
    gap: 24,
  },
  transcript: {
    color: '#94a3b8',
    fontSize: 14,
    fontStyle: 'italic',
    textAlign: 'center',
    maxWidth: 280,
  },
  errorBox: {
    alignItems: 'center',
    gap: 8,
  },
  errorText: {
    color: '#f97316',
    fontSize: 14,
    textAlign: 'center',
    maxWidth: 300,
  },
  retryHint: {
    color: '#0a7ea4',
    fontSize: 14,
    textDecorationLine: 'underline',
  },
  doneBox: {
    alignItems: 'center',
    gap: 6,
  },
  doneTitle: {
    color: '#22c55e',
    fontSize: 16,
    fontWeight: '600',
  },
  sig: {
    color: '#475569',
    fontSize: 11,
    fontFamily: 'monospace',
  },
  reviewContainer: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  reviewScroll: {
    flexGrow: 1,
    justifyContent: 'flex-end',
  },
  androidWarning: {
    color: '#78350f',
    fontSize: 11,
    textAlign: 'center',
    paddingBottom: 8,
    paddingHorizontal: 16,
  },
});
