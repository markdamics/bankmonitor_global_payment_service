import react from '@vitejs/plugin-react'
import { defineConfig, type UserConfig } from 'vite'

// The `test` key is Vitest's, not Vite's. Vitest reads it off this same config object at
// runtime regardless of what Vite's own types know about, so it's typed loosely here rather
// than importing `defineConfig` from `vitest/config` — that package bundles its own (older)
// copy of Vite whose plugin types are incompatible with this project's Vite version.
const config: UserConfig & { test?: unknown } = {
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
}

// https://vite.dev/config/
export default defineConfig(config)
