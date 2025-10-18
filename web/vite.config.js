import { defineConfig } from 'vite';
import { relative, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { globSync } from 'glob';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
    plugins: [tailwindcss()],
    build: {
        outDir: '../webroot',
        emptyOutDir: true,
        cssMinify: true,
        cssCodeSplit: true,
        minify: true,
        rollupOptions: {
            input: Object.fromEntries(['./index.html', ...globSync('cpds/**/*.html'), ...globSync('aceas/**/*.html')].map(file => {
                const relativePath = relative('', file.slice(0, file.length - extname(file).length));
                const filePath = fileURLToPath(new URL(file, import.meta.url));

                return [
                    relativePath,
                    filePath,
                ];
            })),
        },
    }
})
