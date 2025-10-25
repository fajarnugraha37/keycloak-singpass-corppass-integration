# 🚀 Developer Guide - SSL Certificate Management

## Quick Start Commands

### Windows (PowerShell)
```powershell
# Generate all certificates
.\generate-certificates.ps1

# View certificate details
.\show-certificates.ps1

# Validate certificates
.\validate-certificates.ps1

# Generate .env file
.\load-environment.ps1 -Format dotenv -Output .env

# Load into current PowerShell session
.\load-environment.ps1 | Invoke-Expression
```

### Cross-Platform (Make)
```bash
# Show all available commands
make help

# Generate certificates
make generate

# Generate with custom validity
make generate VALIDITY_DAYS=730

# Set up complete development environment
make dev-setup

# Generate environment file
make env-dotenv
```

### Docker
```bash
# Build and run certificate generator
docker-compose up ssl-generator

# Generate certificates with custom validity
VALIDITY_DAYS=730 docker-compose up ssl-generator

# Run validation
docker-compose --profile validate up ssl-validator

# Test with nginx server
docker-compose --profile test up test-server
```

## 🔧 Integration Examples

### Node.js / Express
```javascript
const fs = require('fs');
const https = require('https');
const express = require('express');

// Load certificates
const options = {
  key: fs.readFileSync(process.env.SSL_ESERVICE_KEY_PATH),
  cert: fs.readFileSync(process.env.SSL_ESERVICE_CERT_PATH)
};

const app = express();

// Create HTTPS server
https.createServer(options, app).listen(3000, () => {
  console.log('HTTPS Server running on port 3000');
});
```

### SAML Configuration (Passport-SAML)
```javascript
const SamlStrategy = require('passport-saml').Strategy;

passport.use(new SamlStrategy({
  path: '/saml/callback',
  entryPoint: 'https://mockpass.localhost:5000/saml/idp',
  issuer: 'passport-saml',
  cert: Buffer.from(process.env.SAML_PUBLIC_CERT_BASE64, 'base64').toString(),
  privateCert: Buffer.from(process.env.SAML_PRIVATE_KEY_BASE64, 'base64').toString(),
  signatureAlgorithm: 'sha256'
}, function(profile, done) {
  return done(null, profile);
}));
```

### JWT Token Signing
```javascript
const jwt = require('jsonwebtoken');

// Sign token
const privateKey = Buffer.from(process.env.JWT_PRIVATE_KEY_BASE64, 'base64').toString();
const token = jwt.sign(
  { userId: 123, email: 'user@example.com' },
  privateKey,
  { algorithm: 'RS256', expiresIn: '1h' }
);

// Verify token
const publicKey = Buffer.from(process.env.JWT_PUBLIC_KEY_BASE64, 'base64').toString();
const decoded = jwt.verify(token, publicKey, { algorithms: ['RS256'] });
```

### Python / FastAPI
```python
import ssl
import uvicorn
from fastapi import FastAPI

app = FastAPI()

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=3000,
        ssl_keyfile="ssl/private/eservice.key",
        ssl_certfile="ssl/certs/eservice.crt",
        ssl_version=ssl.PROTOCOL_TLS_SERVER
    )
```

### Spring Boot (Java)
```yaml
# application.yml
server:
  port: 3000
  ssl:
    enabled: true
    key-store-type: PKCS12
    key-store: ssl/keystore.p12
    key-store-password: changeit
    
# Or using PEM files
server:
  ssl:
    certificate: ssl/certs/eservice.crt
    certificate-private-key: ssl/private/eservice.key
```

## 🐳 Docker Integration

### Multi-stage Dockerfile
```dockerfile
# Stage 1: Generate certificates
FROM alpine:latest as cert-generator
RUN apk add --no-cache openssl
WORKDIR /certs
COPY ssl/ .
RUN ./generate-certificates.ps1 || make generate-unix

# Stage 2: Application
FROM node:18-alpine
WORKDIR /app
COPY --from=cert-generator /certs/certs/ ./ssl/certs/
COPY --from=cert-generator /certs/private/ ./ssl/private/
COPY package*.json ./
RUN npm install
COPY . .
EXPOSE 3000
CMD ["node", "app.js"]
```

### Docker Compose with SSL
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "3000:3000"
    volumes:
      - ./ssl:/app/ssl:ro
    environment:
      - SSL_ESERVICE_CERT_PATH=/app/ssl/certs/eservice.crt
      - SSL_ESERVICE_KEY_PATH=/app/ssl/private/eservice.key
    depends_on:
      - cert-generator
      
  cert-generator:
    build:
      context: ./ssl
    volumes:
      - ./ssl:/ssl
    command: generate
```

## 🔐 Security Considerations

### Development vs Production

**Development (Current Setup)**
- ✅ Self-signed certificates
- ✅ Local domain names (localhost)
- ✅ Embedded private keys
- ✅ Extended validity periods

**Production Requirements**
- 🚫 Use CA-signed certificates (Let's Encrypt, commercial CA)
- 🚫 Real domain names
- 🚫 Secure key management (Key Vault, HSM)
- 🚫 Short validity periods with auto-renewal

### Key Management Best Practices

```bash
# File permissions (Unix/Linux)
chmod 600 ssl/private/*.key    # Private keys - owner read/write only
chmod 644 ssl/certs/*.crt      # Certificates - world readable
chmod 755 ssl/                 # Directory - executable for traversal

# Windows PowerShell
icacls "ssl\private" /inheritance:d
icacls "ssl\private" /grant:r "$env:USERNAME:(OI)(CI)F"
icacls "ssl\private" /remove "Users" "Everyone" "Authenticated Users"
```

### Environment Variable Security

```bash
# Never commit .env files with real keys
echo ".env" >> .gitignore
echo "ssl/private/" >> .gitignore
echo "ssl/certs/" >> .gitignore

# Use secrets management in production
# Docker secrets
docker secret create jwt_private_key ssl/oidc/private_key.pem

# Kubernetes secrets
kubectl create secret tls app-tls --cert=ssl/certs/eservice.crt --key=ssl/private/eservice.key
```

## 🧪 Testing and Validation

### Manual Testing
```bash
# Test SSL connection
openssl s_client -connect eservice.localhost:3000 -servername eservice.localhost

# Verify certificate
openssl x509 -in ssl/certs/eservice.crt -text -noout

# Test key pair matching
openssl x509 -in ssl/certs/eservice.crt -pubkey -noout > cert.pub
openssl rsa -in ssl/private/eservice.key -pubout > key.pub
diff cert.pub key.pub
```

### Automated Testing
```javascript
// Jest test example
const fs = require('fs');
const forge = require('node-forge');

describe('SSL Certificates', () => {
  test('certificate should be valid', () => {
    const certPem = fs.readFileSync('ssl/certs/eservice.crt', 'utf8');
    const cert = forge.pki.certificateFromPem(certPem);
    
    expect(cert.validity.notAfter).toBeInstanceOf(Date);
    expect(cert.validity.notAfter.getTime()).toBeGreaterThan(Date.now());
  });

  test('key pair should match', () => {
    const certPem = fs.readFileSync('ssl/certs/eservice.crt', 'utf8');
    const keyPem = fs.readFileSync('ssl/private/eservice.key', 'utf8');
    
    const cert = forge.pki.certificateFromPem(certPem);
    const key = forge.pki.privateKeyFromPem(keyPem);
    
    // Verify key pair matches
    const publicKeyFromCert = cert.publicKey;
    const publicKeyFromPrivate = forge.pki.rsa.setPublicKey(key.n, key.e);
    
    expect(publicKeyFromCert.n.toString()).toBe(publicKeyFromPrivate.n.toString());
  });
});
```

## 🔄 Certificate Rotation

### Automated Rotation Script
```bash
#!/bin/bash
# cert-rotation.sh

BACKUP_DIR="ssl/backup/$(date +%Y%m%d_%H%M%S)"
ALERT_EMAIL="admin@example.com"

# Function to check certificate expiry
check_expiry() {
    local cert_file=$1
    local days_warning=$2
    
    if openssl x509 -in "$cert_file" -checkend $((days_warning * 24 * 3600)) -noout; then
        return 0  # Certificate valid
    else
        return 1  # Certificate expiring soon or expired
    fi
}

# Check all certificates
for cert in ssl/certs/*.crt ssl/saml/*.crt; do
    if ! check_expiry "$cert" 30; then
        echo "WARNING: $cert expires within 30 days"
        
        # Backup existing certificates
        mkdir -p "$BACKUP_DIR"
        cp -r ssl/ "$BACKUP_DIR/"
        
        # Regenerate certificates
        make generate
        
        # Send alert
        echo "Certificate renewed: $cert" | mail -s "SSL Certificate Rotation" "$ALERT_EMAIL"
        
        # Restart services (customize as needed)
        systemctl reload nginx
        docker-compose restart app
        
        break
    fi
done
```

### Cron Job Setup
```bash
# Add to crontab (crontab -e)
# Check certificates daily at 2 AM
0 2 * * * /path/to/ssl/cert-rotation.sh >> /var/log/cert-rotation.log 2>&1
```

## 🔍 Troubleshooting

### Common Issues

**Certificate not trusted in browser**
```bash
# Add to system trust store (macOS)
sudo security add-trusted-cert -d -r trustRoot -k /System/Library/Keychains/SystemRootCertificates.keychain ssl/certs/eservice.crt

# Windows (Run as Administrator)
certlm.msc  # Import to Trusted Root Certification Authorities
```

**"Private key does not match certificate"**
```bash
# Check certificate and key modulus
openssl x509 -in ssl/certs/eservice.crt -modulus -noout | openssl md5
openssl rsa -in ssl/private/eservice.key -modulus -noout | openssl md5
# These should match
```

**"SSL handshake failed"**
```bash
# Check SSL configuration
openssl s_client -connect localhost:3000 -debug -state

# Common causes:
# 1. Wrong certificate file path
# 2. File permissions
# 3. Key format (try converting to PKCS#8)
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in ssl/private/eservice.key -out ssl/private/eservice_pkcs8.key
```

### Logging and Monitoring

```javascript
// SSL certificate monitoring middleware
const certificateMonitor = (req, res, next) => {
  const cert = req.socket.getPeerCertificate();
  const now = new Date();
  const expiry = new Date(cert.valid_to);
  const daysUntilExpiry = Math.ceil((expiry - now) / (1000 * 60 * 60 * 24));
  
  if (daysUntilExpiry < 30) {
    console.warn(`Certificate expires in ${daysUntilExpiry} days:`, cert.subject);
  }
  
  next();
};
```

## 📊 Performance Considerations

### SSL/TLS Optimization
```nginx
# nginx.conf optimizations
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;
ssl_buffer_size 4k;
ssl_stapling on;
ssl_stapling_verify on;

# Use HTTP/2 for better performance
listen 443 ssl http2;
```

### Key Size vs Performance
- **RSA 2048-bit**: Good balance of security and performance
- **RSA 3072-bit**: Higher security, slower performance (JWT recommended)
- **ECDSA**: Faster than RSA, same security level with smaller keys

## 📈 Monitoring and Alerting

### Certificate Expiry Monitoring
```python
# Python monitoring script
import ssl
import socket
from datetime import datetime, timedelta

def check_certificate_expiry(hostname, port=443):
    context = ssl.create_default_context()
    with socket.create_connection((hostname, port)) as sock:
        with context.wrap_socket(sock, server_hostname=hostname) as ssock:
            cert = ssock.getpeercert()
            expiry_date = datetime.strptime(cert['notAfter'], '%b %d %H:%M:%S %Y %Z')
            days_until_expiry = (expiry_date - datetime.now()).days
            return days_until_expiry

# Usage
days = check_certificate_expiry('eservice.localhost', 3000)
if days < 30:
    send_alert(f"Certificate expires in {days} days")
```

### Health Check Endpoint
```javascript
app.get('/health/ssl', (req, res) => {
  const certPath = process.env.SSL_ESERVICE_CERT_PATH;
  const cert = fs.readFileSync(certPath);
  const x509 = new crypto.X509Certificate(cert);
  
  const now = new Date();
  const expiry = new Date(x509.validTo);
  const daysUntilExpiry = Math.ceil((expiry - now) / (1000 * 60 * 60 * 24));
  
  res.json({
    status: daysUntilExpiry > 0 ? 'ok' : 'expired',
    daysUntilExpiry,
    issuer: x509.issuer,
    subject: x509.subject,
    serialNumber: x509.serialNumber
  });
});
```