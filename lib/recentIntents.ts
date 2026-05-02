import * as FileSystem from 'expo-file-system/legacy';

const FILE_URI = (FileSystem.cacheDirectory ?? '') + 'voice0_recents.json';
const MAX = 3;

export async function loadRecentIntents(): Promise<string[]> {
  try {
    const info = await FileSystem.getInfoAsync(FILE_URI);
    if (!info.exists) return [];
    const text = await FileSystem.readAsStringAsync(FILE_URI);
    return JSON.parse(text) as string[];
  } catch {
    return [];
  }
}

export async function saveRecentIntent(text: string): Promise<string[]> {
  const current = await loadRecentIntents();
  const idx = current.indexOf(text);
  if (idx !== -1) current.splice(idx, 1);
  current.unshift(text);
  if (current.length > MAX) current.pop();
  try {
    await FileSystem.writeAsStringAsync(FILE_URI, JSON.stringify(current));
  } catch {
    // non-critical — fall back to in-memory list
  }
  return current;
}
