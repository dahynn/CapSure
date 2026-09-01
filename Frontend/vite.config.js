import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        proxy: {
            '/auth': 'http://localhost:8080',
            '/subscriptions': 'http://localhost:8080',
            '/analysis': 'http://localhost:8080',
            '/insurers': 'http://localhost:8080',
            '/dashboard': 'http://localhost:8080',
            '/mydata': 'http://localhost:8080',
            '/api': 'http://localhost:8080',
            '/actuator': 'http://localhost:8080',
        }
    }
});
