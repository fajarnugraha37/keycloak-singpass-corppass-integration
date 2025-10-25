# 🔐 SSL Certificate Management for SSO Application

A comprehensive certificate and key management system for Single Sign-On (SSO) applications, supporting SSL/TLS, SAML, and OIDC/JWT authentication protocols.

## 🚀 Quick Start

### Prerequisites
- **OpenSSL** - Required for certificate generation and validation
- **PowerShell 5.1+** - For running the management scripts

### Generate All Certificates
```powershell
# Generate all certificates and keys (interactive)
.\generate-certificates.ps1

# Generate with custom validity period (2 years)
.\generate-certificates.ps1 -ValidityDays 730

# Force regeneration without prompts
.\generate-certificates.ps1 -Force
```

### Generate Custom CA (New!)
```powershell
# Generate a custom Certificate Authority
.\generate-ca.ps1 -OrganizationName "My Company"

# Update CA bundle with Mozilla's trusted root certificates
.\update-ca-bundle.ps1 -Source mozilla

# Generate CA with custom validity (10 years)
.\generate-ca.ps1 -OrganizationName "Dev Team" -ValidityDays 3650
```

### View Certificate Information
```powershell
# Display detailed certificate information
.\show-certificates.ps1

# Validate all certificates and keys
.\validate-certificates.ps1
```

### Cross-Platform (Makefile) - *NEW!*
```bash
# Show all available commands
make help

# Generate certificates
make generate

# Generate with custom validity (2 years)
make generate VALIDITY_DAYS=730

# Generate custom CA
make generate-ca ORG_NAME="My Company"

# Update CA bundle with Mozilla's trusted roots
make update-ca-bundle

# Install CA in system trust store (requires admin/sudo)
make install-ca

# Set up complete development environment (includes CA!)
make dev-setup

# Generate environment file
make env-dotenv

# Show status of all certificates and CA files
make status

# View CA certificate information
make ca-info
```

### Docker
```bash
# Build and run certificate generator
docker-compose up ssl-generator

# Generate certificates with custom validity
VALIDITY_DAYS=730 docker-compose up ssl-generator

# Run validation
docker-compose --profile validate up ssl-validator

# Test with nginx server (includes SSL verification)
docker-compose --profile test up test-server

# Generate everything and test
COMMAND=all docker-compose up ssl-generator
```

## 📁 Repository Structure

```
ssl/
├── 📂 certs/              # SSL certificates
│   ├── eservice.crt       # eService domain certificate
│   └── mockpass.crt       # MockPass domain certificate
├── 📂 private/            # Private keys
│   ├── eservice.key       # eService private key
│   └── mockpass.key       # MockPass private key
├── 📂 saml/               # SAML signing certificates
│   ├── public.crt         # SAML public certificate
│   ├── key.pem           # SAML private key
│   ├── public_base64.txt  # Base64 encoded public cert
│   └── key_base64.txt     # Base64 encoded private key
├── 📂 oidc/               # OIDC/JWT signing keys
│   ├── private_key.pem    # JWT private key (PKCS#8)
│   ├── public_key.pem     # JWT public key
│   ├── private_key_base64.txt # Base64 private key
│   └── public_key_base64.txt  # Base64 public key
├── � ca/ (NEW!)          # Certificate Authority
│   ├── 📂 certs/
│   │   ├── ca.crt         # CA certificate (CRT format)
│   │   ├── ca.pem         # CA certificate (PEM format)
│   │   ├── ca-bundle.crt  # CA bundle (147+ trusted root CAs)
│   │   ├── ca-chain.pem   # Certificate chain file
│   │   ├── cacert.pem     # Common name link
│   │   └── cert.pem       # Generic certificate bundle
│   ├── 📂 private/
│   │   └── ca.key         # CA private key
│   ├── ca.conf            # CA configuration
│   └── INSTALL_INSTRUCTIONS.md # CA installation guide
├── �🔧 eservice.conf       # OpenSSL config for eService
├── 🔧 mockpass.conf       # OpenSSL config for MockPass
├── 🔑 encryption.key      # Application encryption key
└── 📜 Scripts and docs... # Management scripts
```

## 🛠️ Available Scripts

### Core Management Scripts

| Script | Description | Usage |
|--------|-------------|--------|
| `generate-certificates.ps1` | Generate all certificates and keys | `.\generate-certificates.ps1 [-Force] [-ValidityDays <days>]` |
| `generate-ca.ps1` **(NEW!)** | Generate custom Certificate Authority | `.\generate-ca.ps1 -OrganizationName "Company" [-ValidityDays <days>]` |
| `update-ca-bundle.ps1` **(NEW!)** | Update CA bundle with trusted roots | `.\update-ca-bundle.ps1 [-Source mozilla\|system\|custom]` |
| `show-certificates.ps1` | Display certificate information | `.\show-certificates.ps1` |
| `validate-certificates.ps1` | Validate certificates and keys | `.\validate-certificates.ps1` |
| `cleanup-certificates.ps1` | Clean up certificates | `.\cleanup-certificates.ps1 [-All\|-Expired\|-Backups] [-Force]` |
| `load-environment.ps1` | Generate environment variables | `.\load-environment.ps1 [-Format powershell\|bash\|dotenv] [-Output <file>]` |

### Script Examples

```powershell
# Generate custom CA and update bundle
.\generate-ca.ps1 -OrganizationName "My Company"
.\update-ca-bundle.ps1 -Source mozilla

# Validate all certificates
.\validate-certificates.ps1

# Clean up expired certificates only
.\cleanup-certificates.ps1 -Expired

# Generate .env file with certificate paths
.\load-environment.ps1 -Format dotenv -Output .env

# Load environment variables in current session
.\load-environment.ps1 -Format powershell | Invoke-Expression
```

## 🔑 Certificate Types Generated

### 1. SSL/TLS Certificates
- **Purpose**: HTTPS endpoints for web applications
- **Domains**: `eservice.localhost`, `mockpass.localhost`
- **Key Size**: RSA 2048-bit
- **Validity**: 365 days (configurable)
- **Extensions**: Subject Alternative Names (SAN)

### 2. SAML Signing Certificates  
- **Purpose**: SAML assertion signing and verification
- **Key Size**: RSA 2048-bit
- **Format**: X.509 certificate + PEM private key
- **Base64**: Available for environment variables

### 3. OIDC/JWT Signing Keys
- **Purpose**: JWT token signing and verification
- **Key Size**: RSA 3072-bit (recommended for JWT)
- **Format**: PKCS#8 private key + public key
- **Base64**: Available for environment variables

### 4. Encryption Key
- **Purpose**: Application-level encryption
- **Type**: Random 32-byte key
- **Format**: Base64 encoded

### 5. Certificate Authority (CA) - *NEW!*
- **Purpose**: Sign certificates to eliminate browser warnings
- **Key Size**: RSA 4096-bit (maximum security)
- **Validity**: 10 years (3650 days) by default
- **Bundle**: Includes 147+ Mozilla trusted root CAs
- **Formats**: CRT, PEM, and bundle variations

## 🔧 Configuration Files

### SSL Configuration (`*.conf`)
Defines certificate parameters including:
- Subject information (Country, State, Organization)
- Subject Alternative Names (SAN)
- Key usage extensions
- Extended key usage

### Environment Template (`.env.template`)
Complete template for application environment variables including:
- Certificate file paths
- Base64 encoded keys
- Application URLs
- SAML/OIDC configuration
- Security settings

## � Usage in Applications

### Loading Certificates in Node.js
```javascript
// Using file paths
const https = require('https');
const fs = require('fs');

const options = {
  key: fs.readFileSync(process.env.SSL_ESERVICE_KEY_PATH),
  cert: fs.readFileSync(process.env.SSL_ESERVICE_CERT_PATH),
  ca: fs.readFileSync('ca/certs/ca-bundle.crt') // NEW: Use CA bundle
};

// Using base64 environment variables
const privateKey = Buffer.from(process.env.JWT_PRIVATE_KEY_BASE64, 'base64').toString('utf8');

// Trust custom CA for outgoing requests
process.env.NODE_EXTRA_CA_CERTS = 'ca/certs/ca-bundle.crt';
```

### SAML Configuration
```javascript
const samlConfig = {
  cert: process.env.SAML_PUBLIC_CERT_BASE64,
  privateKey: process.env.SAML_PRIVATE_KEY_BASE64,
  issuer: 'urn:example:sso',
  callbackUrl: 'https://eservice.localhost:3000/saml/callback'
};
```

### JWT Configuration
```javascript
const jwt = require('jsonwebtoken');

const token = jwt.sign(payload, 
  Buffer.from(process.env.JWT_PRIVATE_KEY_BASE64, 'base64').toString('utf8'),
  { algorithm: 'RS256' }
);
```

## 🛡️ Security Best Practices

### File Permissions
- Private keys should have restricted permissions (600)
- Certificate files can be readable (644)
- **CA private keys** should be heavily protected (600, limited access)
- Use `.gitignore` to prevent committing sensitive files

### CA Security
- **Protect CA private key** - This can sign any certificate
- **Limit CA certificate validity** - Don't make it too long
- **Regular CA bundle updates** - Keep trusted roots current
- **Monitor CA usage** - Track what certificates are signed

### Certificate Rotation
- Monitor certificate expiry dates
- Set up automated renewal processes
- Use the validation script to check certificate health

### Environment Variables
- Use base64 encoded versions for containerized deployments
- Store sensitive keys in secure key management systems
- Rotate encryption keys regularly

## 🔍 Troubleshooting

### Common Issues

**OpenSSL not found**
```powershell
# Install OpenSSL on Windows
choco install openssl
# Or download from: https://slproweb.com/products/Win32OpenSSL.html
```

**Browser still shows certificate warnings (NEW!)**
```powershell
# Install your custom CA in the system trust store
# Windows (Run as Administrator):
certlm.msc  # Import ca/certs/ca.crt to Trusted Root Certification Authorities

# macOS:
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ca/certs/ca.crt

# Linux:
sudo cp ca/certs/ca.crt /usr/local/share/ca-certificates/custom-ca.crt
sudo update-ca-certificates
```

**Certificate validation fails**
```powershell
# Check certificate details
openssl x509 -in certs/eservice.crt -text -noout

# Verify key pair match
openssl x509 -in certs/eservice.crt -pubkey -noout > cert_pub.key
openssl rsa -in private/eservice.key -pubout > key_pub.key
diff cert_pub.key key_pub.key
```

**Browser certificate warnings**
- Add certificates to trusted root store
- Use `localhost` in browser URLs
- Configure application to use generated certificates

### Validation Commands
```powershell
# Check certificate expiry
openssl x509 -in certs/eservice.crt -noout -dates

# Verify certificate chain
openssl verify -CAfile saml/public.crt certs/eservice.crt

# Test SSL connection
openssl s_client -connect eservice.localhost:3000 -servername eservice.localhost
```

## 📚 References

- [OpenSSL Documentation](https://www.openssl.org/docs/)
- [SAML Certificate Requirements](https://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-tech-overview-2.0.html)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [SSL/TLS Configuration Guide](https://mozilla.github.io/server-side-tls/ssl-config-generator/)

## 🤝 Contributing

1. Test changes with the validation script
2. Update documentation for new features
3. Follow security best practices
4. Add appropriate error handling

---

## 🎉 **Recent Updates**

### Version 2.0 - Certificate Authority Support
- ✅ **Custom CA Generation** - Create your own trusted Certificate Authority
- ✅ **Mozilla CA Bundle** - 147+ trusted root certificates included
- ✅ **Cross-Platform Makefile** - Linux/macOS/Windows compatibility
- ✅ **Docker Support** - Containerized certificate generation
- ✅ **Enhanced Security** - 4096-bit CA keys, comprehensive validation
- ✅ **Multiple Formats** - PEM, CRT, bundle, and chain variations
- ✅ **System Integration** - Easy installation in browser/system trust stores

### New Files Added
- 📜 `generate-ca.ps1` - Custom CA generator
- 📜 `update-ca-bundle.ps1` - Mozilla CA bundle updater  
- 📜 `Makefile` - Cross-platform build system
- 📜 `Dockerfile` & `docker-compose.yml` - Container support
- 📜 `DEVELOPER_GUIDE.md` - Comprehensive integration guide
- 📂 `ca/` directory - Complete CA infrastructure

---

**⚠️ Security Notice**: This repository contains tools for generating development certificates. The custom CA feature eliminates browser warnings for development. Do not use these certificates in production environments. Always use certificates from trusted Certificate Authorities for production deployments.