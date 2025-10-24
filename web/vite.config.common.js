/**
 * @type {import('vite').UserConfig}
 */
export const commonConfig = {
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
};
