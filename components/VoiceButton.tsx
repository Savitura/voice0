import { useEffect } from 'react';
import { Pressable, View, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withSequence,
  withTiming,
  withSpring,
  cancelAnimation,
  Easing,
} from 'react-native-reanimated';
import type { AppPhase } from '@/lib/types';

interface VoiceButtonProps {
  phase: AppPhase;
  onPressIn: () => void;
  onPressOut: () => void;
}

const BUSY_PHASES: AppPhase[] = ['transcribing', 'parsing', 'simulating', 'executing'];

export function VoiceButton({ phase, onPressIn, onPressOut }: VoiceButtonProps) {
  const scale = useSharedValue(1);
  const ring1Scale = useSharedValue(1);
  const ring1Opacity = useSharedValue(0);
  const ring2Scale = useSharedValue(1);
  const ring2Opacity = useSharedValue(0);
  const spinAngle = useSharedValue(0);
  const glowOpacity = useSharedValue(0.18);

  const isIdle = phase === 'idle';
  const isRecording = phase === 'recording';
  const isBusy = BUSY_PHASES.includes(phase);

  // Idle: slow ambient glow pulse on the button itself
  useEffect(() => {
    if (isIdle) {
      glowOpacity.value = withRepeat(
        withSequence(
          withTiming(0.35, { duration: 2000, easing: Easing.inOut(Easing.ease) }),
          withTiming(0.1, { duration: 2000, easing: Easing.inOut(Easing.ease) }),
        ),
        -1,
        false,
      );
      ring1Scale.value = withRepeat(
        withSequence(
          withTiming(1.25, { duration: 2000, easing: Easing.out(Easing.ease) }),
          withTiming(1, { duration: 2000 }),
        ),
        -1,
        false,
      );
      ring1Opacity.value = withRepeat(
        withSequence(withTiming(0.2, { duration: 2000 }), withTiming(0.04, { duration: 2000 })),
        -1,
        false,
      );
    } else {
      cancelAnimation(glowOpacity);
      cancelAnimation(ring1Scale);
      cancelAnimation(ring1Opacity);
      glowOpacity.value = withTiming(0.18, { duration: 300 });
      ring1Scale.value = withTiming(1, { duration: 200 });
      ring1Opacity.value = withTiming(0, { duration: 200 });
    }
  }, [isIdle]);

  // Recording: fast pulsing rings
  useEffect(() => {
    if (isRecording) {
      glowOpacity.value = 0.5;
      ring1Scale.value = withRepeat(
        withSequence(withTiming(1.4, { duration: 450 }), withTiming(1.05, { duration: 450 })),
        -1,
        false,
      );
      ring1Opacity.value = withRepeat(
        withSequence(withTiming(0.7, { duration: 450 }), withTiming(0.2, { duration: 450 })),
        -1,
        false,
      );
      ring2Scale.value = withRepeat(
        withSequence(
          withTiming(1, { duration: 0 }),
          withTiming(1.9, { duration: 800, easing: Easing.out(Easing.ease) }),
        ),
        -1,
        false,
      );
      ring2Opacity.value = withRepeat(
        withSequence(withTiming(0.5, { duration: 0 }), withTiming(0, { duration: 800 })),
        -1,
        false,
      );
    } else {
      cancelAnimation(ring2Scale);
      cancelAnimation(ring2Opacity);
      ring2Scale.value = withTiming(1, { duration: 150 });
      ring2Opacity.value = withTiming(0, { duration: 150 });
    }
  }, [isRecording]);

  // Busy: spin arc
  useEffect(() => {
    if (isBusy) {
      spinAngle.value = withRepeat(
        withTiming(360, { duration: 1100, easing: Easing.linear }),
        -1,
        false,
      );
    } else {
      cancelAnimation(spinAngle);
      spinAngle.value = 0;
    }
  }, [isBusy]);

  const buttonStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  const glowStyle = useAnimatedStyle(() => ({
    opacity: glowOpacity.value,
  }));

  const ring1Style = useAnimatedStyle(() => ({
    transform: [{ scale: ring1Scale.value }],
    opacity: ring1Opacity.value,
  }));

  const ring2Style = useAnimatedStyle(() => ({
    transform: [{ scale: ring2Scale.value }],
    opacity: ring2Opacity.value,
  }));

  const spinStyle = useAnimatedStyle(() => ({
    transform: [{ rotate: `${spinAngle.value}deg` }],
  }));

  const btnColor = isRecording ? '#dc2626' : isBusy ? '#0f172a' : '#0c4a6e';
  const ringColor = isRecording ? '#ef4444' : '#38bdf8';

  return (
    <View style={styles.container}>
      {/* Expanding ripple on recording */}
      <Animated.View style={[styles.ring, { borderColor: ringColor }, ring2Style]} />
      {/* Pulsing inner ring */}
      <Animated.View style={[styles.ring, { borderColor: ringColor }, ring1Style]} />

      <Pressable
        disabled={isBusy || phase === 'reviewing'}
        onPressIn={() => {
          if (phase !== 'idle') return;
          scale.value = withSpring(0.91, { damping: 14, stiffness: 200 });
          onPressIn();
        }}
        onPressOut={() => {
          scale.value = withSpring(1, { damping: 14, stiffness: 200 });
          onPressOut();
        }}
      >
        <Animated.View style={[styles.button, { backgroundColor: btnColor }, buttonStyle]}>
          {/* Ambient glow overlay */}
          <Animated.View
            style={[styles.glow, { backgroundColor: isRecording ? '#ef4444' : '#38bdf8' }, glowStyle]}
          />

          {isBusy ? (
            <Animated.View style={[styles.spinner, spinStyle]} />
          ) : isRecording ? (
            <Ionicons name="stop" size={32} color="#fff" />
          ) : (
            <Ionicons name="mic" size={44} color="#fff" />
          )}
        </Animated.View>
      </Pressable>
    </View>
  );
}

const BTN = 120;
const RING = 160;

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
    width: RING + 40,
    height: RING + 40,
  },
  ring: {
    position: 'absolute',
    width: RING,
    height: RING,
    borderRadius: RING / 2,
    borderWidth: 1.5,
  },
  button: {
    width: BTN,
    height: BTN,
    borderRadius: BTN / 2,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
    shadowColor: '#38bdf8',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.6,
    shadowRadius: 24,
    elevation: 14,
  },
  glow: {
    position: 'absolute',
    width: BTN,
    height: BTN,
    borderRadius: BTN / 2,
  },
  spinner: {
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 3,
    borderColor: '#38bdf8',
    borderTopColor: 'transparent',
  },
});
