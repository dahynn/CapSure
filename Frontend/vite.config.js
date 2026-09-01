import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

const backendApiTarget = process.env.CAPSURE_API_TARGET || 'http://localhost:8080';

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        proxy: {
            '/auth': backendApiTarget,
            '/subscriptions': backendApiTarget,
            '/analysis': backendApiTarget,
            '/insurers': backendApiTarget,
            '/dashboard': backendApiTarget,
            '/mydata': backendApiTarget,
            '/api': backendApiTarget,
            '/actuator': backendApiTarget,
        }
    }
});
