'use strict';
/**
 * Buffer polyfill — injected by metro.config.js serializer.getPolyfills.
 * Runs before ANY module code in the bundle, including route files discovered
 * by Expo Router's require.context. This guarantees global.Buffer is available
 * before @solana/spl-token-metadata (and similar packages) initialise.
 */
var nativeBuffer = require('@craftzdog/react-native-buffer');
global.Buffer = global.Buffer || nativeBuffer.Buffer;
