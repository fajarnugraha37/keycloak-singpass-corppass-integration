import { defineConfig } from 'vite';
import { relative, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { globSync } from 'glob';
import tailwindcss from '@tailwindcss/vite';
import handlebarsPlugin from '@yoichiro/vite-plugin-handlebars';
import path from 'path';

const partial = path.resolve(__dirname, '__partials__');
console.log('Handlebars partials directory:', partial);

export default defineConfig({
    plugins: [
        tailwindcss(),
        handlebarsPlugin({
            templateFileExtension: 'hbs',
            optimizePartialRegistration: true,
            partialDirectoryPath: path.resolve(__dirname, '__partials__'),
            partialsDirectoryPath: path.resolve(__dirname, '__partials__'),
            compileOptions: {
            },
            transformIndexHtmlOptions: {
                context: () => ({
                    message: 'Hello, world!'
                }),
                helpers: {
                    /**
                     * @param {string} str 
                    * @returns {string}
                     */
                    'upper-case': (str, opts) => str.toUpperCase(),
                    'coalesce': (val, defaultVal, opts) => val != null ? val : defaultVal,
                    /**
                     * usage: {{create-array val1 val2 val3 ...}}
                     * @param  {...any} args 
                     * @returns 
                     */
                    /**
                     * Create an object from key-value pairs
                     * usage: {{obj key1="value1" key2="value2"}}
                     * @param {*} obj 
                     * @param {*} options 
                     * @returns 
                     */
                    'obj': function (obj, options) {
                        return obj;
                    },
                    /**
                     * Create an array from values
                     * usage: {{create-array val1 val2 val3 ...}}
                     * @param  {...any} args 
                     * @returns 
                     */
                    'arr': function (...args) {
                        return Array.prototype.slice.call(arguments, 0, -1).map(arg => arg.hash);
                    }
                },
            },
        }),
    ],

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
                ...globSync('aceas/**/*.html'),
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
                    'vendor-htmx': ['htmx.org'],

                    // Shared application code
                    'shared': ['./__scripts__/shared/index.js'],
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
            'keycloak-js',
            'htmx.org'
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
