import { useEffect } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import type { SolanaTxBundle } from '@/lib/types';

interface ExecutionScreenProps {
  bundle: SolanaTxBundle;
}

export function ExecutionScreen({ bundle }: ExecutionScreenProps) {
  const spinAngle = useSharedValue(0);
  const dotOpacity = useSharedValue(1);

  useEffect(() => {
    spinAngle.value = withRepeat(
      withTiming(360, { duration: 1100, easing: Easing.linear }),
      -1,
      false,
    );
    dotOpacity.value = withRepeat(
      withTiming(0.2, { duration: 700 }),
      -1,
      true,
    );
  }, []);

  const spinStyle = useAnimatedStyle(() => ({
    transform: [{ rotate: `${spinAngle.value}deg` }],
  }));

  const dotStyle = useAnimatedStyle(() => ({
    opacity: dotOpacity.value,
  }));

  return (
    <SafeAreaView style={styles.root}>
      <View style={styles.content}>
        {/* Spinner */}
        <Animated.View style={[styles.spinner, spinStyle]} />

        <Text style={styles.title}>Awaiting wallet</Text>
        <Animated.Text style={[styles.subtitle, dotStyle]}>
          Sign the transaction in your wallet app
        </Animated.Text>

        {/* Step list — all pending while wallet adapter is active */}
        <View style={styles.stepList}>
          {bundle.steps.map((step, i) => (
            <View key={step.id} style={styles.stepRow}>
              <View style={styles.stepDot} />
              <View style={styles.stepInfo}>
                <Text style={styles.stepNum}>
                  {String(i + 1).padStart(2, '0')}
                </Text>
                <Text style={styles.stepSummary} numberOfLines={1}>
                  {step.humanSummary}
                </Text>
              </View>
              <Text style={styles.stepStatus}>Queued</Text>
            </View>
          ))}
        </View>

        <Text style={styles.note}>
          Do not close this screen while signing
        </Text>
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
    paddingHorizontal: 32,
    gap: 20,
  },
  spinner: {
    width: 52,
    height: 52,
    borderRadius: 26,
    borderWidth: 3,
    borderColor: '#0369a1',
    borderTopColor: '#38bdf8',
  },
  title: {
    color: '#e2e8f0',
    fontSize: 22,
    fontWeight: '600',
  },
  subtitle: {
    color: '#475569',
    fontSize: 14,
    textAlign: 'center',
  },
  stepList: {
    width: '100%',
    gap: 10,
    marginTop: 8,
  },
  stepRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: '#0c1221',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderWidth: 1,
    borderColor: '#0f1a2e',
  },
  stepDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#1e3a5f',
  },
  stepInfo: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  stepNum: {
    color: '#1e3a5f',
    fontSize: 11,
    fontWeight: '700',
    fontVariant: ['tabular-nums'],
  },
  stepSummary: {
    color: '#475569',
    fontSize: 13,
    flex: 1,
  },
  stepStatus: {
    color: '#1e3a5f',
    fontSize: 11,
    fontWeight: '500',
  },
  note: {
    color: '#1e293b',
    fontSize: 12,
    textAlign: 'center',
    marginTop: 8,
  },
});
