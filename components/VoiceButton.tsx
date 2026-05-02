import { useEffect } from 'react';
import { Pressable, View, Text, StyleSheet } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withSequence,
  withTiming,
  withSpring,
  cancelAnimation,
} from 'react-native-reanimated';
import type { AppPhase } from '@/lib/types';

interface VoiceButtonProps {
  phase: AppPhase;
  onPressIn: () => void;
  onPressOut: () => void;
}

const PHASE_COLORS: Partial<Record<AppPhase, string>> = {
  idle: '#0a7ea4',
  recording: '#ef4444',
  transcribing: '#64748b',
  parsing: '#64748b',
  simulating: '#64748b',
  executing: '#f59e0b',
  done: '#22c55e',
  error: '#f97316',
};

const PHASE_LABELS: Partial<Record<AppPhase, string>> = {
  idle: 'Hold to speak',
  recording: 'Listening…',
  transcribing: 'Transcribing…',
  parsing: 'Parsing…',
  simulating: 'Simulating…',
  reviewing: 'Review below',
  executing: 'Executing…',
  done: 'Done!',
  error: 'Error — try again',
};

const BUSY_PHASES: AppPhase[] = [
  'transcribing',
  'parsing',
  'simulating',
  'executing',
];

export function VoiceButton({ phase, onPressIn, onPressOut }: VoiceButtonProps) {
  const scale = useSharedValue(1);
  const ringScale = useSharedValue(1);
  const ringOpacity = useSharedValue(0);

  const isRecording = phase === 'recording';
  const isBusy = BUSY_PHASES.includes(phase);
  const color = PHASE_COLORS[phase] ?? '#0a7ea4';
  const label = PHASE_LABELS[phase] ?? '';

  useEffect(() => {
    if (isRecording) {
      ringOpacity.value = withTiming(1, { duration: 200 });
      ringScale.value = withRepeat(
        withSequence(
          withTiming(1.6, { duration: 700 }),
          withTiming(1, { duration: 700 }),
        ),
        -1,
        false,
      );
    } else {
      cancelAnimation(ringScale);
      cancelAnimation(ringOpacity);
      ringScale.value = withTiming(1, { duration: 200 });
      ringOpacity.value = withTiming(0, { duration: 200 });
    }
  }, [isRecording]);

  const buttonStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  const ringStyle = useAnimatedStyle(() => ({
    transform: [{ scale: ringScale.value }],
    opacity: ringOpacity.value,
  }));

  return (
    <View style={styles.container}>
      <Animated.View style={[styles.ring, { borderColor: color }, ringStyle]} />

      <Pressable
        disabled={isBusy || phase === 'reviewing'}
        onPressIn={() => {
          scale.value = withSpring(0.92);
          onPressIn();
        }}
        onPressOut={() => {
          scale.value = withSpring(1);
          onPressOut();
        }}
      >
        <Animated.View
          style={[styles.button, { backgroundColor: color }, buttonStyle]}
        >
          <Text style={styles.icon}>
            {isRecording ? '⏹' : isBusy ? '⏳' : phase === 'done' ? '✓' : '🎙'}
          </Text>
        </Animated.View>
      </Pressable>

      <Text style={[styles.label, { color }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
  },
  ring: {
    position: 'absolute',
    width: 120,
    height: 120,
    borderRadius: 60,
    borderWidth: 2,
  },
  button: {
    width: 88,
    height: 88,
    borderRadius: 44,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 8,
    elevation: 8,
  },
  icon: {
    fontSize: 32,
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    textAlign: 'center',
    marginTop: 8,
  },
});
