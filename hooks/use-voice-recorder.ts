import { useAudioRecorder, AudioModule, RecordingPresets } from 'expo-audio';

export function useVoiceRecorder() {
  const recorder = useAudioRecorder(RecordingPresets.HIGH_QUALITY);

  const start = async (): Promise<void> => {
    const { granted } = await AudioModule.requestRecordingPermissionsAsync();
    if (!granted) throw new Error('Microphone permission denied');
    await AudioModule.setIsAudioActiveAsync(true);
    await recorder.record();
  };

  const stop = async (): Promise<string> => {
    await recorder.stop();
    await AudioModule.setIsAudioActiveAsync(false);
    const uri = recorder.uri;
    if (!uri) throw new Error('Recording produced no URI');
    return uri;
  };

  return { start, stop, isRecording: recorder.isRecording };
}
