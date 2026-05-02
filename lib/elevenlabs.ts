import * as FileSystem from 'expo-file-system/legacy';

const ELEVENLABS_STT_URL = 'https://api.elevenlabs.io/v1/speech-to-text';

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
