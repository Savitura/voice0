import { useState, useCallback } from 'react';
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
import { startRecording, stopRecording, transcribeAudio } from '@/lib/elevenlabs';
import { signAndSendTransactions, connectWallet, getCachedSession } from '@/lib/wallet';
import type { AppPhase, AppError, SolanaTxBundle } from '@/lib/types';

const BASE_URL =
  process.env.EXPO_PUBLIC_BASE_URL ?? 'http://localhost:8081';

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
): Promise<SolanaTxBundle> {
  const res = await fetch(`${BASE_URL}/api/simulate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ bundle, userPublicKey }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error ?? 'Simulate failed');
  return data as SolanaTxBundle;
}

export default function HomeScreen() {
  const [phase, setPhase] = useState<AppPhase>('idle');
  const [error, setError] = useState<AppError | null>(null);
  const [transcript, setTranscript] = useState<string>('');
  const [bundle, setBundle] = useState<SolanaTxBundle | null>(null);
  const [signatures, setSignatures] = useState<string[]>([]);

  const resetToIdle = useCallback(() => {
    setPhase('idle');
    setError(null);
    setTranscript('');
    setBundle(null);
    setSignatures([]);
  }, []);

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

      // Ensure we have a wallet session before simulating
      let session = getCachedSession();
      if (!session && Platform.OS === 'android') {
        session = await connectWallet();
      }
      const pubkey = session?.publicKey.toBase58() ?? '';

      setPhase('parsing');
      const parsed = await parseIntent(text);

      setPhase('simulating');
      const simulated = pubkey
        ? await simulateBundle(parsed, pubkey)
        : { ...parsed, warnings: [...parsed.warnings, 'No wallet connected — simulation skipped'] };

      setBundle(simulated);
      setPhase('reviewing');
    } catch (err) {
      setError({
        phase: phase as AppPhase,
        message: err instanceof Error ? err.message : 'Unknown error',
      });
      setPhase('error');
    }
  }, [phase]);

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
      Alert.alert(
        'Android only',
        'Mobile Wallet Adapter is only supported on Android.',
      );
      return;
    }

    try {
      setPhase('executing');
      const sigs = await signAndSendTransactions(txBase64s);
      setSignatures(sigs);
      setPhase('done');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Execution failed';
      const isUserRejection = msg.toLowerCase().includes('cancelled') ||
        msg.toLowerCase().includes('rejected');

      if (isUserRejection) {
        setPhase('reviewing');
      } else {
        setError({ phase: 'executing', message: msg });
        setPhase('error');
      }
    }
  }, [bundle]);

  const handleCancel = useCallback(() => {
    resetToIdle();
  }, [resetToIdle]);

  return (
    <SafeAreaView style={styles.root}>
      <View style={styles.header}>
        <Text style={styles.title}>voice0</Text>
        <Text style={styles.subtitle}>Speak your DeFi intent</Text>
      </View>

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

      {phase === 'reviewing' && bundle && (
        <View style={styles.reviewContainer}>
          <ScrollView contentContainerStyle={styles.reviewScroll}>
            <TxReviewPanel
              bundle={bundle}
              onConfirm={handleConfirm}
              onCancel={handleCancel}
              isExecuting={false}
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
    paddingBottom: 16,
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
