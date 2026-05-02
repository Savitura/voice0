import { Audio } from 'expo-av';
import * as FileSystem from 'expo-file-system';

const ELEVENLABS_STT_URL = 'https://api.elevenlabs.io/v1/speech-to-text';

const RECORDING_OPTIONS: Audio.RecordingOptions = {
  android: {
    extension: '.m4a',
    outputFormat: Audio.AndroidOutputFormat.MPEG_4,
    audioEncoder: Audio.AndroidAudioEncoder.AAC,
    sampleRate: 16000,
    numberOfChannels: 1,
    bitRate: 64000,
  },
  ios: {
    extension: '.m4a',
    outputFormat: Audio.IOSOutputFormat.MPEG4AAC,
    audioQuality: Audio.IOSAudioQuality.MEDIUM,
    sampleRate: 16000,
    numberOfChannels: 1,
    bitRate: 64000,
    linearPCMBitDepth: 16,
    linearPCMIsBigEndian: false,
    linearPCMIsFloat: false,
  },
  web: {
    mimeType: 'audio/webm',
    bitsPerSecond: 64000,
  },
};

let _recording: Audio.Recording | null = null;

export async function startRecording(): Promise<void> {
  const { status } = await Audio.requestPermissionsAsync();
  if (status !== 'granted') {
    throw new Error('Microphone permission denied');
  }

  await Audio.setAudioModeAsync({
    allowsRecordingIOS: true,
    playsInSilentModeIOS: true,
  });

  const { recording } = await Audio.Recording.createAsync(RECORDING_OPTIONS);
  _recording = recording;
}

export async function stopRecording(): Promise<string> {
  if (!_recording) throw new Error('No active recording');
  await _recording.stopAndUnloadAsync();
  const uri = _recording.getURI();
  _recording = null;

  await Audio.setAudioModeAsync({ allowsRecordingIOS: false });

  if (!uri) throw new Error('Recording produced no file URI');
  return uri;
}

export async function transcribeAudio(fileUri: string): Promise<string> {
  const apiKey = process.env.EXPO_PUBLIC_ELEVENLABS_API_KEY;
  if (!apiKey) throw new Error('EXPO_PUBLIC_ELEVENLABS_API_KEY is not set');

  const formData = new FormData();
  formData.append('audio', {
    uri: fileUri,
    type: 'audio/m4a',
    name: 'recording.m4a',
  } as unknown as Blob);
  formData.append('model_id', 'scribe_v1');
  formData.append('language_code', 'en');

  const response = await fetch(ELEVENLABS_STT_URL, {
    method: 'POST',
    headers: { 'xi-api-key': apiKey },
    body: formData,
  });

  // Clean up audio file regardless of result
  await FileSystem.deleteAsync(fileUri, { idempotent: true });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`ElevenLabs STT failed (${response.status}): ${text}`);
  }

  const data = (await response.json()) as { text: string };
  return data.text;
}

export function isRecording(): boolean {
  return _recording !== null;
}
