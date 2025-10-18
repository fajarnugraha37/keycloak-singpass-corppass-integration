import { defineConfig } from 'vite';
import { relative, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { globSync } from 'glob';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
    plugins: [tailwindcss()],
    
    // Development server configuration
    server: {
        port: 3000,
        open: true,
        cors: true,
        hmr: {
            overlay: true
        },
        // Disable caching in development
        headers: {
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache',
            'Expires': '0'
        }
    },
    
    // Preview server configuration
    preview: {
        port: 3000,
        open: true,
        // Cache control for preview
        headers: {
            'Cache-Control': 'no-cache, max-age=0'
        }
    },
    
    // Build configuration
    build: {
        outDir: '../webroot',
        emptyOutDir: true,
        sourcemap: true,
        
        // CSS optimization
        cssMinify: 'esbuild',
        cssCodeSplit: true,
        
        // JavaScript optimization
        minify: 'esbuild',
        target: 'esnext',
        
        // Rollup options
        rollupOptions: {
            // Multi-page application setup
            input: Object.fromEntries([
                './index.html', 
                './404.html', 
                './50x.html', 
                ...globSync('mockpass/**/*.html'), 
                ...globSync('cpds/**/*.html'), 
                ...globSync('aceas/**/*.html')
            ].map(file => {
                const relativePath = relative('', file.slice(0, file.length - extname(file).length));
                const filePath = fileURLToPath(new URL(file, import.meta.url));

                return [relativePath, filePath];
            })),
            
            // Output configuration with enhanced cache busting
            output: {
                // Chunk splitting for better caching
                manualChunks: {
                    // Vendor chunks
                    'vendor-signals': ['@preact/signals'],
                    'vendor-oidc': ['oidc-client-ts', 'keycloak-js'],
                    
                    // Shared application code
                    'shared': ['./src/shared/index.js']
                },
                
                // Enhanced asset naming with timestamps for aggressive cache busting
                chunkFileNames: (chunkInfo) => {
                    const timestamp = Date.now().toString(36);
                    return `assets/[name]-[hash]-${timestamp}.js`;
                },
                entryFileNames: (chunkInfo) => {
                    const timestamp = Date.now().toString(36);
                    return `assets/[name]-[hash]-${timestamp}.js`;
                },
                assetFileNames: (assetInfo) => {
                    const info = assetInfo.name.split('.');
                    const ext = info[info.length - 1];
                    const timestamp = Date.now().toString(36);

                    if (/png|jpe?g|svg|gif|tiff|bmp|ico/i.test(ext)) {
                        return `assets/images/[name]-[hash]-${timestamp}[extname]`;
                    }
                    if (/css/i.test(ext)) {
                        return `assets/styles/[name]-[hash]-${timestamp}[extname]`;
                    }
                    return `assets/[name]-[hash]-${timestamp}[extname]`;
                }
            }
        },
        
        // Performance optimizations
        chunkSizeWarningLimit: 1000,
        reportCompressedSize: false,
        
        // Modern build with legacy fallback
        modulePreload: {
            polyfill: true
        }
    },
    
    // Optimization configuration
    optimizeDeps: {
        include: [
            '@preact/signals',
            'oidc-client-ts',
            'keycloak-js'
        ],
        exclude: []
    },
    
    // ESBuild configuration
    esbuild: {
        // Remove console logs and debugger statements in production
        drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : [],
        
        // Target modern browsers
        target: 'esnext',
        
        // Enable minification
        minify: true
    },
    
    // CSS configuration
    css: {
        devSourcemap: true
    },
    
    // Base path for deployment
    base: '/',
    
    // Asset handling
    assetsInclude: ['**/*.woff', '**/*.woff2'],
    
    // Define global constants
    define: {
        __APP_VERSION__: JSON.stringify(process.env.npm_package_version || '1.0.0'),
        __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
        __BUILD_TIMESTAMP__: JSON.stringify(Date.now()),
        __DEV__: JSON.stringify(process.env.NODE_ENV === 'development')
    }
})
