import 'react-native-get-random-values';
import 'react-native-url-polyfill/auto';
import { Buffer } from '@craftzdog/react-native-buffer';

// Set unconditionally — don't guard with typeof check.
// Some Hermes builds see the property as absent even after a previous assignment.
// @ts-expect-error — RN Buffer is compatible but types differ slightly
global.Buffer = Buffer;
