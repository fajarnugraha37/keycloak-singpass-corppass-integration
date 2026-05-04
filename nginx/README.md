# Nginx Configuration

## Overview

This document explains all the nginx configuration made and how to use them.

## **File Structure**

```
nginx/
├── nginx.conf              # Main nginx configuration
├── common.conf             # Common settings (security, CORS, compression)
├── proxy.conf              # Proxy settings (headers, timeouts, buffers)
├── locations.conf          # Reusable location blocks
├── cors.conf               # CORS configuration
├── ssl.conf                # SSL/TLS configuration
├── cache.conf              # Caching configuration
├── vite-cache.conf         # Vite-specific cache busting
└── conf.d/
    ├── default.conf        # Default server
    ├── eservice.localhost.conf    # eService configuration 
    └── mockpass.localhost.conf    # Mockpass configuration 
```

## **Core**

### **1. Performance**

#### **Worker Processes & Connections**
- **Auto worker processes** - Matches CPU cores automatically
- **Increased connections** - 2048 per worker (from 1024)
- **Optimized event handling** - Uses epoll and multi_accept

#### **Connection**
- **TCP** - tcp_nopush, tcp_nodelay enabled
- **Keepalive** - 1000 requests per connection
- **Buffer** - Larger buffers for better performance

#### **Compression**
- **Enhanced gzip** - More file types, optimal compression levels
- **Brotli ready** - Configuration for Brotli compression (commented)
- **Font** - Proper CORS headers for web fonts

### **2. Security**

#### **Security Headers**
```nginx
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: (policy)
```

#### **Rate Limiting**
- **Authentication endpoints** - 60 req/min burst 100
- **API endpoints** - 100 req/min burst 100
- **Static content** - 200 req/min burst 100
- **Connection limiting** - Max 20 connections per IP

#### **Access Control**
- **Enhanced CORS** - Proper preflight handling via cors.conf
- **Monitoring endpoints** - Restricted to private networks
- **Error page protection** - Custom error pages with fallback

### **3. Code**

#### **Structure**
- **Common** - Common settings in shared files
- **Location** - Shared location blocks in `locations.conf`
- **CORS** - Dedicated `cors.conf` for API endpoints

#### **Include Strategy**
```nginx
# Main config includes
include /etc/nginx/common.conf;     # Security & basic settings
include /etc/nginx/proxy.conf;      # Proxy settings
include /etc/nginx/locations.conf;  # Common locations

# In API locations
include /etc/nginx/cors.conf;       # CORS headers
```

### **4. Monitoring & Debugging**

#### **Logging**
- **Structured JSON logs** - Better parsing and analysis
- **Additional fields** - Upstream response times, addresses
- **Request tracking** - X-Request-ID for tracing

#### **Health Endpoints**
- **/health** - Basic health check
- **/nginx-status** - Nginx statistics
- **Service-specific health** - Per-service health checks

#### **Error Handling**
- **Custom error pages** - 404.html and 50x.html
- **Graceful fallbacks** - Error page routing
- **Auto-refresh for 5xx** - Automatic retry for service errors

## **Vite Cache Busting Integration**

### **How It Works**

1. **Vite Build Process**:
   ```bash
   npm run build  # Generates: assets/index-D0N7gXsW-mgwgoinm.js
   ```

2. **Nginx Pattern Recognition**:
   ```nginx
   # Matches Vite's pattern: name-contenthash-timestamp.ext
   location ~* ^/assets/.+-[a-zA-Z0-9_-]{8,}-[a-zA-Z0-9]{8,}\.(css|js)$
   ```

3. **Cache Strategy**:
   - **Hashed files**: Cache for 1 year (safe due to content hashing)
   - **HTML files**: Zero cache (references new hashed URLs)
   - **Non-hashed files**: 1 hour cache

### **Testing Cache Busting**

1. **Build & Check URLs**:
   ```bash
   cd web && npm run build
   # Look for: assets/index-hash-timestamp.js pattern
   ```

2. **Verify Cache Headers**:
   ```bash
   curl -I http://localhost/assets/index-D0N7gXsW-mgwgoinm.js
   # Should show: X-Vite-Cache: hashed-asset
   ```

3. **Test Fresh HTML**:
   ```bash
   curl -I http://localhost/
   # Should show: X-Vite-Build: fresh
   ```

4. **Verify Cache Busting**:
   ```bash
   # Build again
   npm run build
   # URLs should change, forcing fresh downloads
   ```

### **Cache Busting Benefits**

1. **Maximum Performance**: Hashed assets cached aggressively 
2. **Fresh HTML Always**: HTML references new asset URLs
3. **Debug Headers**: `X-Vite-Cache` and `X-Vite-Build` for monitoring

## **Configuration Details**

### **Rate Limiting Zones **

| Zone | Rate | Burst | Usage | Notes |
|------|------|-------|-------|-------|
| `auth` | 60/min | 100 | Authentication endpoints | Increased from 10/min to prevent 404s |
| `api` | 100/min | 100 | API endpoints | Increased burst capacity |
| `static` | 200/min | 100 | Static content | Optimized for SPA resources |

> **⚠️ Critical Fix**: The auth zone rate limit was increased from 10/min to 60/min to resolve intermittent 404 errors during Keycloak authentication flows. Keycloak authentication requires multiple requests (HTML, CSS, JS, fonts) that exceeded the previous limit.

### **Service Routing (Updated)**

| Path | Service | Rate Limit | Caching | CORS |
|------|---------|------------|---------|------|
| `/auth/` | Keycloak | auth zone (60/min) | No cache | ✅ |
| `/aceas/api/` | ACEAS API | api zone | 5 min | ✅ |
| `/cpds/api/` | CPDS API | api zone | 5 min | ✅ |
| `/ids/` | Identity Service | api zone | 5 min | ✅ |
| `/assets/` | Static files | static zone | 1 year | N/A |
| `/aceas/` | ACEAS SPA | static zone | 5 min | N/A |
| `/cpds/` | CPDS SPA | static zone | 5 min | N/A |
| `/health` | Nginx Health | No limit | No cache | N/A |
| `/nginx-status` | Nginx Stats | No limit | No cache | N/A |

## **Usage Examples**

### **Basic Deployment**
```bash
# Test configuration
make nginx-test

# Reload configuration
make nginx-reload

# Restart nginx service
make re-web

# View logs
make log-web

# Check nginx status
curl http://localhost/nginx-status  # (restricted to private networks)

# Health check
curl http://localhost/health
```

### **Container Management**
```bash
# Full stack up
make up

# Full stack down
make down

# Restart specific service
make re-<service>

# Check all services
make status
```

### **Enable SSL**
1. Uncomment SSL server blocks in `ssl.conf`
2. Update certificate paths
3. Enable HTTP to HTTPS redirect

```nginx
# In your server config
include /etc/nginx/ssl.conf;
```

### **Enable Caching**
```nginx
# Add to main nginx.conf for traditional caching
include /etc/nginx/cache.conf;

# Add to server config for Vite cache busting
include /etc/nginx/vite-cache.conf;
```

### **Custom Service**
```nginx
# Add to locations.conf
location /my-service/ {
    limit_req zone=api burst=30 nodelay;
    include /etc/nginx/proxy.conf;
    include /etc/nginx/cors.conf;  # If API needs CORS
    proxy_set_header X-Forwarded-Prefix /my-service;
    proxy_pass http://my-service:8080;
}
```

## **Troubleshooting**

### **Configuration Testing**
```bash
# Test nginx configuration
make nginx-test

# Test specific config file
docker exec web nginx -t

# Reload configuration
docker exec web nginx -s reload

# Check nginx status
curl http://localhost/nginx-status
```

### **Common Issues**

#### **Rate Limiting Triggered (429 Too Many Requests)**
**Symptom**: Intermittent 404s on `/auth/` endpoints during authentication
```bash
# Check logs for rate limiting messages
docker logs web | grep "limiting requests"

# Solution: Adjust rate limits in nginx.conf
limit_req_zone $binary_remote_addr zone=auth:10m rate=60r/m;  # Increase from 10r/m
```

#### **Missing Error Pages (404/50x not found)**
**Symptom**: Generic nginx error instead of custom error pages
```bash
# Ensure error pages exist in webroot
ls webroot/404.html webroot/50x.html

# Check docker-compose volume mounts
docker-compose config | grep webroot
```

#### **CORS Issues**
**Symptom**: Browser CORS errors on API calls
```bash
# Check if cors.conf is included
docker exec web nginx -T | grep cors.conf

# Verify CORS headers
curl -H "Origin: http://localhost" -I http://localhost/api/
```

#### **Upstream Connection Issues (502 Bad Gateway)**
**Symptom**: Service unavailable errors
```bash
# Check service health
make health
make status

# Check service logs
make log-<service>

# Verify service is running
docker ps | grep <service>
```

#### **Configuration Syntax Errors**
**Symptom**: Nginx container fails to start
```bash
# Check syntax
make nginx-test

# View container logs
docker logs web

# Common issues:
# - Missing semicolons
# - Duplicate directives
# - Include files not mounted
```

### **Debug Commands**
```bash
# Full nginx configuration
docker exec web nginx -T

# Test configuration without restart
docker exec web nginx -t

# View all includes
docker exec web find /etc/nginx -name "*.conf" -exec echo "=== {} ===" \; -exec cat {} \;

# Check file permissions
docker exec web ls -la /etc/nginx/

# Monitor real-time logs
docker logs -f web

# Check rate limiting zones
docker exec web nginx -T | grep limit_req_zone
```
