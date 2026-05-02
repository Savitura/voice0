import 'react-native-get-random-values';
import 'react-native-url-polyfill/auto';
import { Buffer } from '@craftzdog/react-native-buffer';

if (typeof globalThis.Buffer === 'undefined') {
  // @ts-expect-error — RN Buffer is compatible for our use but types differ slightly
  globalThis.Buffer = Buffer;
}
