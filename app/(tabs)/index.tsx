import { useState, useCallback, useEffect, useRef } from 'react';
import {
  View,
  Text,
  Modal,
  Platform,
  Pressable,
  TextInput,
  BackHandler,
  StyleSheet,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { VoiceButton } from '@/components/VoiceButton';
import { TxReviewPanel } from '@/components/TxReviewPanel';
import { ExecutionScreen } from '@/components/ExecutionScreen';
import { SuccessScreen } from '@/components/SuccessScreen';
import { NetworkToggle } from '@/components/NetworkToggle';
import { BalanceStrip } from '@/components/BalanceStrip';
import { useVoiceRecorder } from '@/hooks/use-voice-recorder';
import { transcribeAudio } from '@/lib/elevenlabs';
import { loadRecentIntents, saveRecentIntent } from '@/lib/recentIntents';
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

type ProcessingStep = 'transcribed' | 'parsed' | 'simulated';

export default function HomeScreen() {
  const [cluster, setClusterState] = useState<Cluster>(getCluster());
  const [phase, setPhase] = useState<AppPhase>('idle');
  const [error, setError] = useState<AppError | null>(null);
  const [transcript, setTranscript] = useState('');
  const [editText, setEditText] = useState<string | null>(null);
  const [bundle, setBundle] = useState<SolanaTxBundle | null>(null);
  const [prices, setPrices] = useState<Record<string, number>>({});
  const [balances, setBalances] = useState<TokenBalance[]>([]);
  const [signatures, setSignatures] = useState<string[]>([]);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [doneSteps, setDoneSteps] = useState<ProcessingStep[]>([]);
  const [recentList, setRecentList] = useState<string[]>([]);
  const [walletAddress, setWalletAddress] = useState<string | null>(
    () => getCachedSession()?.publicKey.toBase58() ?? null,
  );

  const { start: startRec, stop: stopRec } = useVoiceRecorder();
  const clusterRef = useRef(cluster);
  clusterRef.current = cluster;

  // Load persisted recent intents on mount
  useEffect(() => {
    loadRecentIntents().then(setRecentList);
  }, []);

  // Block Android back button during execution
  useEffect(() => {
    if (phase !== 'executing') return;
    const sub = BackHandler.addEventListener('hardwareBackPress', () => true);
    return () => sub.remove();
  }, [phase]);

  // Recording timer
  useEffect(() => {
    if (phase !== 'recording') {
      setRecordingSeconds(0);
      return;
    }
    const id = setInterval(() => setRecordingSeconds((s) => s + 1), 1000);
    return () => clearInterval(id);
  }, [phase]);

  // Reset processing steps when returning to idle/recording
  useEffect(() => {
    if (phase === 'idle' || phase === 'recording') setDoneSteps([]);
  }, [phase]);

  const refreshBalances = useCallback(async () => {
    const session = getCachedSession();
    if (!session) return;
    try {
      const conn = getConnection();
      const bals = await fetchBalances(session.publicKey, conn);
      setBalances(bals);
      const mints = bals.map((b) => b.mint);
      fetchPrices(mints).then((p) => setPrices((prev) => ({ ...prev, ...p })));
    } catch {
      // non-critical
    }
  }, []);

  const resetToIdle = useCallback(() => {
    setPhase('idle');
    setError(null);
    setTranscript('');
    setEditText(null);
    setBundle(null);
    setSignatures([]);
  }, []);

  const handleNetworkChange = useCallback(
    (newCluster: Cluster) => {
      setCluster(newCluster);
      setClusterState(newCluster);
      clearSession();
      setWalletAddress(null);
      setBalances([]);
      resetToIdle();
    },
    [resetToIdle],
  );

  // Core processing pipeline shared by voice and text paths
  const processText = useCallback(async (text: string) => {
    const currentCluster = clusterRef.current;
    saveRecentIntent(text).then(setRecentList);

    // Lazy wallet connection
    let session = getCachedSession();
    if (!session && Platform.OS === 'android') {
      session = await connectWallet();
      if (session) {
        setWalletAddress(session.publicKey.toBase58());
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
    setDoneSteps((prev) => [...prev, 'parsed']);

    setPhase('simulating');
    const simulated = pubkey
      ? await simulateBundle(parsed, pubkey, currentCluster)
      : {
          ...parsed,
          warnings: [...parsed.warnings, 'No wallet connected — simulation skipped'],
        };
    setDoneSteps((prev) => [...prev, 'simulated']);

    fetchPrices(mintsFromBundle(simulated.steps)).then((p) =>
      setPrices((prev) => ({ ...prev, ...p })),
    );

    setBundle(simulated);
    setPhase('reviewing');
  }, []);

  const handlePressIn = useCallback(() => {
    if (phase !== 'idle') return;
    setPhase('recording');
    startRec().catch((err: unknown) => {
      setError({
        phase: 'recording',
        message: err instanceof Error ? err.message : 'Recording failed',
      });
      setPhase('error');
    });
  }, [phase, startRec]);

  const handlePressOut = useCallback(async () => {
    if (phase !== 'recording') return;
    try {
      setPhase('transcribing');
      const uri = await stopRec();
      const text = await transcribeAudio(uri);
      setTranscript(text);
      setDoneSteps(['transcribed']);
      await processText(text);
    } catch (err) {
      setError({
        phase: 'transcribing',
        message: err instanceof Error ? err.message : 'Unknown error',
      });
      setPhase('error');
    }
  }, [phase, stopRec, processText]);

  const handleReRunIntent = useCallback(
    async (text: string) => {
      if (phase !== 'idle') return;
      setTranscript(text);
      setDoneSteps(['transcribed']);
      try {
        setPhase('transcribing');
        await processText(text);
      } catch (err) {
        setError({
          phase: 'parsing',
          message: err instanceof Error ? err.message : 'Unknown error',
        });
        setPhase('error');
      }
    },
    [phase, processText],
  );

  const handleSubmitEdit = useCallback(async () => {
    const text = editText?.trim();
    if (!text) return;
    setEditText(null);
    setTranscript(text);
    setDoneSteps(['transcribed']);
    try {
      setPhase('transcribing');
      await processText(text);
    } catch (err) {
      setError({
        phase: 'parsing',
        message: err instanceof Error ? err.message : 'Parse failed',
      });
      setPhase('error');
    }
  }, [editText, processText]);

  const handleConfirm = useCallback(async () => {
    if (!bundle) return;
    const txBase64s = bundle.steps
      .map((s) => s.transaction)
      .filter((t): t is string => t != null);
    if (txBase64s.length === 0) return;
    if (Platform.OS !== 'android') return;

    try {
      setPhase('executing');
      const sigs = await signAndSendTransactions(txBase64s);
      setSignatures(sigs);
      setPhase('done');
      refreshBalances();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Execution failed';
      const isRejection =
        msg.toLowerCase().includes('cancelled') || msg.toLowerCase().includes('rejected');
      if (isRejection) {
        setPhase('reviewing');
      } else {
        setError({ phase: 'executing', message: msg });
        setPhase('error');
      }
    }
  }, [bundle, refreshBalances]);

  // × on review panel → back to idle
  const handleDismissReview = useCallback(() => resetToIdle(), [resetToIdle]);

  // "Edit Intent" footer button → back to idle with text pre-filled
  const handleEditIntent = useCallback(() => {
    setEditText(transcript);
    setPhase('idle');
    setBundle(null);
  }, [transcript]);

  const handleConnectWallet = useCallback(async () => {
    if (walletAddress || Platform.OS !== 'android') return;
    try {
      const session = await connectWallet();
      if (session) {
        setWalletAddress(session.publicKey.toBase58());
        refreshBalances();
      }
    } catch {
      // user cancelled
    }
  }, [walletAddress, refreshBalances]);

  const isProcessing =
    phase === 'transcribing' || phase === 'parsing' || phase === 'simulating';

  const formatTime = (s: number) =>
    `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;

  return (
    <SafeAreaView style={styles.root}>
      {/* ── Header ── */}
      <View style={styles.header}>
        <Text style={styles.appName}>voice0</Text>
        <View style={styles.headerRight}>
          <NetworkToggle cluster={cluster} onChange={handleNetworkChange} />
          <Pressable style={styles.walletBtn} onPress={handleConnectWallet}>
            <View style={[styles.walletDot, walletAddress && styles.walletDotConnected]} />
            <Text style={styles.walletBtnText}>
              {walletAddress
                ? `${walletAddress.slice(0, 4)}…${walletAddress.slice(-4)}`
                : 'Connect'}
            </Text>
          </Pressable>
        </View>
      </View>

      {/* ── Balance strip ── */}
      {balances.length > 0 && <BalanceStrip balances={balances} prices={prices} />}

      {/* ── Center content ── */}
      <View style={styles.center}>
        <VoiceButton
          phase={phase}
          onPressIn={handlePressIn}
          onPressOut={handlePressOut}
        />

        {/* Idle hint */}
        {phase === 'idle' && editText === null && (
          <Text style={styles.hint}>Hold and speak your intent</Text>
        )}

        {/* Recording timer */}
        {phase === 'recording' && (
          <Text style={styles.timer}>{formatTime(recordingSeconds)}</Text>
        )}

        {/* Processing micro-steps */}
        {isProcessing && (
          <View style={styles.steps}>
            <Text style={[styles.step, doneSteps.includes('transcribed') && styles.stepDone]}>
              {doneSteps.includes('transcribed') ? '✦ Transcribed' : '◌ Transcribing…'}
            </Text>
            {(phase === 'parsing' || phase === 'simulating') && (
              <Text style={[styles.step, doneSteps.includes('parsed') && styles.stepDone]}>
                {doneSteps.includes('parsed') ? '✦ Parsed' : '◌ Parsing…'}
              </Text>
            )}
            {phase === 'simulating' && (
              <Text style={styles.step}>◌ Simulating…</Text>
            )}
          </View>
        )}

        {/* Error state */}
        {phase === 'error' && error && (
          <View style={styles.errorBox}>
            <Text style={styles.errorText}>{error.message}</Text>
            <Pressable onPress={resetToIdle}>
              <Text style={styles.retryLink}>Tap to try again</Text>
            </Pressable>
          </View>
        )}

        {/* Edit intent input */}
        {phase === 'idle' && editText !== null && (
          <View style={styles.editBox}>
            <TextInput
              style={styles.editInput}
              value={editText}
              onChangeText={setEditText}
              multiline
              autoFocus
              placeholderTextColor="#475569"
              placeholder="Edit your intent…"
            />
            <View style={styles.editActions}>
              <Pressable style={styles.editCancel} onPress={() => setEditText(null)}>
                <Text style={styles.editCancelText}>Discard</Text>
              </Pressable>
              <Pressable style={styles.editSubmit} onPress={handleSubmitEdit}>
                <Text style={styles.editSubmitText}>Submit</Text>
              </Pressable>
            </View>
          </View>
        )}
      </View>

      {/* ── Recent intents ── */}
      {phase === 'idle' && editText === null && recentList.length > 0 && (
        <View style={styles.recents}>
          <Text style={styles.recentsLabel}>Recent</Text>
          {recentList.map((intent, i) => (
            <Pressable key={i} style={styles.recentRow} onPress={() => handleReRunIntent(intent)}>
              <Text style={styles.recentIcon}>↩</Text>
              <Text style={styles.recentText} numberOfLines={1}>{intent}</Text>
            </Pressable>
          ))}
        </View>
      )}

      {/* ── Screen 2: Review (slides up) ── */}
      <Modal
        visible={phase === 'reviewing'}
        animationType="slide"
        presentationStyle="fullScreen"
        statusBarTranslucent
      >
        {bundle && (
          <TxReviewPanel
            bundle={bundle}
            prices={prices}
            transcript={transcript}
            onConfirm={handleConfirm}
            onDismiss={handleDismissReview}
            onEditIntent={handleEditIntent}
          />
        )}
      </Modal>

      {/* ── Screen 3: Execution ── */}
      <Modal
        visible={phase === 'executing'}
        animationType="fade"
        presentationStyle="fullScreen"
        statusBarTranslucent
      >
        {bundle && <ExecutionScreen bundle={bundle} />}
      </Modal>

      {/* ── Screen 4: Success ── */}
      <Modal
        visible={phase === 'done'}
        animationType="fade"
        presentationStyle="fullScreen"
        statusBarTranslucent
      >
        {bundle && (
          <SuccessScreen
            bundle={bundle}
            signatures={signatures}
            cluster={cluster}
            onNewIntent={resetToIdle}
          />
        )}
      </Modal>
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
    paddingBottom: 8,
  },
  appName: {
    color: '#e2e8f0',
    fontSize: 22,
    fontWeight: '700',
    letterSpacing: -0.5,
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  walletBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: '#0f172a',
    borderRadius: 20,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderWidth: 1,
    borderColor: '#1e293b',
  },
  walletDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#475569',
  },
  walletDotConnected: {
    backgroundColor: '#22c55e',
  },
  walletBtnText: {
    color: '#94a3b8',
    fontSize: 12,
    fontWeight: '500',
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
    gap: 20,
  },
  hint: {
    color: '#334155',
    fontSize: 15,
    letterSpacing: 0.2,
  },
  timer: {
    color: '#dc2626',
    fontSize: 20,
    fontWeight: '600',
    fontVariant: ['tabular-nums'],
    letterSpacing: 2,
  },
  steps: {
    alignItems: 'center',
    gap: 6,
  },
  step: {
    color: '#475569',
    fontSize: 13,
    fontWeight: '500',
  },
  stepDone: {
    color: '#38bdf8',
  },
  errorBox: {
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 20,
  },
  errorText: {
    color: '#f87171',
    fontSize: 14,
    textAlign: 'center',
    maxWidth: 300,
  },
  retryLink: {
    color: '#38bdf8',
    fontSize: 13,
    textDecorationLine: 'underline',
  },
  editBox: {
    width: '100%',
    backgroundColor: '#0f172a',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#1e293b',
    padding: 16,
    gap: 12,
  },
  editInput: {
    color: '#e2e8f0',
    fontSize: 15,
    lineHeight: 22,
    minHeight: 64,
    textAlignVertical: 'top',
  },
  editActions: {
    flexDirection: 'row',
    gap: 10,
    justifyContent: 'flex-end',
  },
  editCancel: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#1e293b',
  },
  editCancelText: {
    color: '#64748b',
    fontSize: 13,
    fontWeight: '500',
  },
  editSubmit: {
    paddingHorizontal: 18,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#0369a1',
  },
  editSubmitText: {
    color: '#fff',
    fontSize: 13,
    fontWeight: '600',
  },
  recents: {
    paddingHorizontal: 20,
    paddingBottom: 24,
    gap: 4,
  },
  recentsLabel: {
    color: '#1e293b',
    fontSize: 10,
    fontWeight: '600',
    letterSpacing: 1.5,
    textTransform: 'uppercase',
    marginBottom: 6,
  },
  recentRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 10,
    paddingHorizontal: 14,
    backgroundColor: '#0c1221',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#0f1a2e',
  },
  recentIcon: {
    color: '#1e3a5f',
    fontSize: 14,
  },
  recentText: {
    color: '#475569',
    fontSize: 13,
    flex: 1,
  },
});
