import { useEffect } from 'react';
import { View, Text, Pressable, Share, StyleSheet, Linking } from 'react-native';
import * as ExpoClipboard from 'expo-clipboard';
import bs58 from 'bs58';
import { SafeAreaView } from 'react-native-safe-area-context';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withSpring,
  withTiming,
  withDelay,
  withSequence,
} from 'react-native-reanimated';
import type { SolanaTxBundle } from '@/lib/types';
import type { Cluster } from '@/lib/network';

interface SuccessScreenProps {
  bundle: SolanaTxBundle;
  signatures: string[];
  cluster: Cluster;
  onNewIntent: () => void;
}

function toBase58(base64Sig: string): string {
  try {
    return bs58.encode(Buffer.from(base64Sig, 'base64'));
  } catch {
    return base64Sig.slice(0, 32) + '…';
  }
}

function solscanUrl(sig: string, cluster: Cluster): string {
  const base = 'https://solscan.io/tx/';
  return cluster === 'devnet' ? `${base}${sig}?cluster=devnet` : `${base}${sig}`;
}

export function SuccessScreen({ bundle, signatures, cluster, onNewIntent }: SuccessScreenProps) {
  const checkScale = useSharedValue(0);
  const checkOpacity = useSharedValue(0);
  const contentOpacity = useSharedValue(0);

  useEffect(() => {
    checkOpacity.value = withTiming(1, { duration: 200 });
    checkScale.value = withSequence(
      withSpring(1.2, { damping: 10, stiffness: 300 }),
      withSpring(1, { damping: 14, stiffness: 200 }),
    );
    contentOpacity.value = withDelay(400, withTiming(1, { duration: 400 }));
  }, []);

  const checkStyle = useAnimatedStyle(() => ({
    transform: [{ scale: checkScale.value }],
    opacity: checkOpacity.value,
  }));

  const contentStyle = useAnimatedStyle(() => ({
    opacity: contentOpacity.value,
  }));

  const summary = bundle.steps.length === 1
    ? bundle.steps[0].humanSummary
    : `${bundle.steps.length} transactions confirmed`;

  const handleShare = async () => {
    try {
      await Share.share({ message: `Voice0 intent executed: "${bundle.intent}"` });
    } catch {
      // user cancelled
    }
  };

  const base58Sigs = signatures.map(toBase58);

  return (
    <SafeAreaView style={styles.root}>
      <View style={styles.content}>
        {/* Checkmark */}
        <Animated.View style={[styles.checkCircle, checkStyle]}>
          <Text style={styles.checkIcon}>✓</Text>
        </Animated.View>

        <Animated.View style={[styles.textBlock, contentStyle]}>
          <Text style={styles.title}>Done</Text>
          <Text style={styles.summary}>{summary}</Text>
        </Animated.View>

        {/* Signatures */}
        {base58Sigs.length > 0 && (
          <Animated.View style={[styles.sigsBlock, contentStyle]}>
            {base58Sigs.map((sig, i) => (
              <View key={i} style={styles.sigRow}>
                <Text style={styles.sigText} numberOfLines={1}>
                  {sig.slice(0, 20)}…{sig.slice(-6)}
                </Text>
                <View style={styles.sigActions}>
                  <Pressable
                    style={styles.sigBtn}
                    onPress={() => ExpoClipboard.setStringAsync(sig)}
                  >
                    <Text style={styles.sigBtnText}>Copy</Text>
                  </Pressable>
                  <Pressable
                    style={styles.sigBtn}
                    onPress={() => Linking.openURL(solscanUrl(sig, cluster))}
                  >
                    <Text style={styles.sigBtnText}>Solscan ↗</Text>
                  </Pressable>
                </View>
              </View>
            ))}
          </Animated.View>
        )}

        {/* Actions */}
        <Animated.View style={[styles.actions, contentStyle]}>
          <Pressable style={styles.newBtn} onPress={onNewIntent}>
            <Text style={styles.newBtnText}>New Intent</Text>
          </Pressable>
          <Pressable style={styles.shareBtn} onPress={handleShare}>
            <Text style={styles.shareBtnText}>Share</Text>
          </Pressable>
        </Animated.View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#080818',
  },
  content: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 28,
    gap: 28,
  },
  checkCircle: {
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: '#052e16',
    borderWidth: 2,
    borderColor: '#16a34a',
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkIcon: {
    color: '#22c55e',
    fontSize: 40,
    fontWeight: '700',
  },
  textBlock: {
    alignItems: 'center',
    gap: 8,
  },
  title: {
    color: '#e2e8f0',
    fontSize: 28,
    fontWeight: '700',
    letterSpacing: -0.5,
  },
  summary: {
    color: '#64748b',
    fontSize: 15,
    textAlign: 'center',
    maxWidth: 280,
    lineHeight: 22,
  },
  sigsBlock: {
    width: '100%',
    gap: 8,
  },
  sigRow: {
    backgroundColor: '#0c1221',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#0f1a2e',
    padding: 12,
    gap: 8,
  },
  sigText: {
    color: '#334155',
    fontSize: 11,
    fontFamily: 'monospace',
  },
  sigActions: {
    flexDirection: 'row',
    gap: 8,
  },
  sigBtn: {
    paddingHorizontal: 10,
    paddingVertical: 5,
    backgroundColor: '#0f172a',
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#1e293b',
  },
  sigBtnText: {
    color: '#38bdf8',
    fontSize: 11,
    fontWeight: '500',
  },
  actions: {
    width: '100%',
    flexDirection: 'row',
    gap: 12,
  },
  newBtn: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 14,
    backgroundColor: '#0369a1',
    alignItems: 'center',
  },
  newBtnText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '700',
  },
  shareBtn: {
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderRadius: 14,
    backgroundColor: '#0f172a',
    borderWidth: 1,
    borderColor: '#1e293b',
    alignItems: 'center',
  },
  shareBtnText: {
    color: '#64748b',
    fontSize: 15,
    fontWeight: '500',
  },
});
