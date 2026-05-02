const { getDefaultConfig } = require('expo/metro-config');
const { withNativeWind } = require('nativewind/metro');

const config = getDefaultConfig(__dirname);

// Redirect Node built-ins to React Native compatible packages.
// Handles packages that do `import { Buffer } from 'buffer'` or
// `import crypto from 'crypto'` at module level.
config.resolver.extraNodeModules = {
  ...config.resolver.extraNodeModules,
  buffer: require.resolve('@craftzdog/react-native-buffer'),
  crypto: require.resolve('react-native-quick-crypto'),
};

// Apply NativeWind transformer first, then wrap its getPolyfills.
const nativeWindConfig = withNativeWind(config, { input: './global.css' });

// Inject Buffer polyfill BEFORE every other polyfill and before the module
// system initialises — this is the earliest hook available in Metro.
const _originalGetPolyfills = nativeWindConfig.serializer?.getPolyfills ?? (() => []);
nativeWindConfig.serializer = {
  ...nativeWindConfig.serializer,
  getPolyfills: (ctx) => [
    require.resolve('./polyfill-buffer.js'),
    ..._originalGetPolyfills(ctx),
  ],
};

module.exports = nativeWindConfig;
