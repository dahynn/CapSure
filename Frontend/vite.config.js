import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { fileURLToPath } from 'node:url';

const backendApiTarget = process.env.CAPSURE_API_TARGET || 'http://localhost:8080';

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@': path.resolve(fileURLToPath(new URL('.', import.meta.url)), './src'),
        },
    },
    server: {
        proxy: {
            '/auth': backendApiTarget,
            '/subscriptions': backendApiTarget,
            '/analysis': backendApiTarget,
            '/insurers': backendApiTarget,
            '/dashboard/home': backendApiTarget,
            '/dashboard/summary': backendApiTarget,
            '/dashboard/audits': backendApiTarget,
            '/mydata': backendApiTarget,
            '/api': backendApiTarget,
            '/actuator': backendApiTarget,
        }
    }
});
