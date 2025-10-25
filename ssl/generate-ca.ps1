#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Generate a custom Certificate Authority (CA) for internal use

.DESCRIPTION
    This script creates a custom CA that can be used to sign certificates
    for internal development and testing. The CA certificate can be installed
    in browser trust stores to avoid security warnings.

.PARAMETER OrganizationName
    Name of the organization for the CA certificate

.PARAMETER ValidityDays
    Number of days the CA certificate should be valid (default: 3650 = 10 years)

.PARAMETER KeySize
    RSA key size for the CA (default: 4096 for maximum security)

.EXAMPLE
    .\generate-ca.ps1 -OrganizationName "My Company"
    .\generate-ca.ps1 -OrganizationName "Dev Team" -ValidityDays 1825
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$OrganizationName,
    [int]$ValidityDays = 3650,
    [int]$KeySize = 4096
)

# Colors for output
$Green = "`e[32m"
$Yellow = "`e[33m"
$Red = "`e[31m"
$Blue = "`e[34m"
$Reset = "`e[0m"

function Write-ColorOutput {
    param([string]$Message, [string]$Color = $Reset)
    Write-Host "$Color$Message$Reset"
}

Write-ColorOutput "🏛️ Custom Certificate Authority Generator" $Blue
Write-ColorOutput "Organization: $OrganizationName" $Blue
Write-ColorOutput "Validity: $ValidityDays days" $Blue
Write-ColorOutput "Key Size: $KeySize bits" $Blue
Write-ColorOutput "========================================" $Blue

# Create CA directory structure
$caDir = "ca"
if (-not (Test-Path $caDir)) {
    New-Item -ItemType Directory -Path $caDir -Force | Out-Null
    Write-ColorOutput "Created CA directory: $caDir" $Green
}

# Create subdirectories
@("private", "certs", "crl", "newcerts") | ForEach-Object {
    $dir = Join-Path $caDir $_
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}

# Initialize CA database files
$indexFile = Join-Path $caDir "index.txt"
$serialFile = Join-Path $caDir "serial"
$crlSerialFile = Join-Path $caDir "crlnumber"

if (-not (Test-Path $indexFile)) {
    New-Item -ItemType File -Path $indexFile -Force | Out-Null
}

if (-not (Test-Path $serialFile)) {
    "1000" | Out-File -FilePath $serialFile -Encoding ascii -NoNewline
}

if (-not (Test-Path $crlSerialFile)) {
    "1000" | Out-File -FilePath $crlSerialFile -Encoding ascii -NoNewline
}

# Generate CA configuration
$caConfigPath = Join-Path $caDir "ca.conf"
$caConfig = @"
[ ca ]
default_ca = CA_default

[ CA_default ]
dir               = ./ca
certs             = `$dir/certs
crl_dir           = `$dir/crl
new_certs_dir     = `$dir/newcerts
database          = `$dir/index.txt
serial            = `$dir/serial
RANDFILE          = `$dir/private/.rand

private_key       = `$dir/private/ca.key
certificate       = `$dir/certs/ca.crt

crlnumber         = `$dir/crlnumber
crl               = `$dir/crl/ca.crl
crl_extensions    = crl_ext
default_crl_days  = 30

default_md        = sha256
name_opt          = ca_default
cert_opt          = ca_default
default_days      = 365
preserve          = no
policy            = policy_strict

[ policy_strict ]
countryName             = match
stateOrProvinceName     = match
organizationName        = match
organizationalUnitName  = optional
commonName              = supplied
emailAddress            = optional

[ req ]
default_bits        = $KeySize
distinguished_name  = req_distinguished_name
string_mask         = utf8only
default_md          = sha256
x509_extensions     = v3_ca

[ req_distinguished_name ]
countryName                     = Country Name (2 letter code)
stateOrProvinceName             = State or Province Name
localityName                    = Locality Name
0.organizationName              = Organization Name
organizationalUnitName          = Organizational Unit Name
commonName                      = Common Name
emailAddress                    = Email Address

countryName_default             = SG
stateOrProvinceName_default     = Singapore
localityName_default            = Singapore
0.organizationName_default      = $OrganizationName
organizationalUnitName_default  = Certificate Authority
commonName_default              = $OrganizationName Root CA

[ v3_ca ]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true
keyUsage = critical, digitalSignature, cRLSign, keyCertSign

[ v3_intermediate_ca ]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true, pathlen:0
keyUsage = critical, digitalSignature, cRLSign, keyCertSign

[ server_cert ]
basicConstraints = CA:FALSE
nsCertType = server
nsComment = "OpenSSL Generated Server Certificate"
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer:always
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth

[ crl_ext ]
authorityKeyIdentifier=keyid:always

[ ocsp ]
basicConstraints = CA:FALSE
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer
keyUsage = critical, digitalSignature
extendedKeyUsage = critical, OCSPSigning
"@

$caConfig | Out-File -FilePath $caConfigPath -Encoding utf8

Write-ColorOutput "Generated CA configuration: $caConfigPath" $Green

# Generate CA private key
$caKeyPath = Join-Path $caDir "private/ca.key"
Write-ColorOutput "Generating CA private key ($KeySize bits)..." $Yellow

& openssl genrsa -out $caKeyPath $KeySize

if ($LASTEXITCODE -ne 0) {
    Write-ColorOutput "Failed to generate CA private key" $Red
    exit 1
}

Write-ColorOutput "✅ Generated CA private key: $caKeyPath" $Green

# Generate CA certificate
$caCertPath = Join-Path $caDir "certs/ca.crt"
Write-ColorOutput "Generating CA certificate..." $Yellow

& openssl req -config $caConfigPath -key $caKeyPath -new -x509 -days $ValidityDays -sha256 -extensions v3_ca -out $caCertPath -batch

if ($LASTEXITCODE -ne 0) {
    Write-ColorOutput "Failed to generate CA certificate" $Red
    exit 1
}

Write-ColorOutput "✅ Generated CA certificate: $caCertPath" $Green

# Create PEM format certificate (same content, different extension for compatibility)
$caPemPath = Join-Path $caDir "certs/ca.pem"
Copy-Item $caCertPath $caPemPath
Write-ColorOutput "✅ Created PEM format certificate: $caPemPath" $Green

# Create CA bundle (includes your custom CA + system CAs)
Write-ColorOutput "Creating CA bundle..." $Yellow
$caBundlePath = Join-Path $caDir "certs/ca-bundle.crt"

# Start with your custom CA
Get-Content $caCertPath | Out-File -FilePath $caBundlePath -Encoding utf8

# Try to append system CA bundle if available
$systemCaBundles = @(
    "C:\Program Files\Git\mingw64\ssl\certs\ca-bundle.crt",  # Git for Windows
    "/etc/ssl/certs/ca-certificates.crt",                     # Linux
    "/usr/local/etc/openssl/cert.pem",                        # macOS Homebrew
    "/etc/pki/tls/certs/ca-bundle.crt"                        # RHEL/CentOS
)

$foundSystemBundle = $false
foreach ($systemBundle in $systemCaBundles) {
    if (Test-Path $systemBundle) {
        Write-ColorOutput "Adding system CA bundle: $systemBundle" $Green
        "" | Out-File -FilePath $caBundlePath -Append -Encoding utf8  # Add blank line
        Get-Content $systemBundle | Out-File -FilePath $caBundlePath -Append -Encoding utf8
        $foundSystemBundle = $true
        break
    }
}

if (-not $foundSystemBundle) {
    Write-ColorOutput "⚠️  System CA bundle not found - using custom CA only" $Yellow
    Write-ColorOutput "   You can manually append system CAs to ca-bundle.crt later" $Yellow
}

Write-ColorOutput "✅ Generated CA bundle: $caBundlePath" $Green

# Create chain file (useful for server configurations)
$caChainPath = Join-Path $caDir "certs/ca-chain.pem"
Copy-Item $caCertPath $caChainPath
Write-ColorOutput "✅ Created CA chain file: $caChainPath" $Green

# Display CA certificate info
Write-ColorOutput "`n📋 CA Certificate Information:" $Blue
& openssl x509 -in $caCertPath -text -noout | Select-String -Pattern "Subject:|Issuer:|Not Before|Not After|Serial Number:"

# Generate installation instructions
$installInstructions = @"
# 📋 CA Certificate Installation Instructions

## Windows
1. Double-click on ca/certs/ca.crt
2. Click "Install Certificate..."
3. Select "Local Machine" (requires admin)
4. Select "Place all certificates in the following store"
5. Browse and select "Trusted Root Certification Authorities"
6. Click "Next" and "Finish"

## macOS
```bash
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ca/certs/ca.crt
```

## Linux (Ubuntu/Debian)
```bash
sudo cp ca/certs/ca.crt /usr/local/share/ca-certificates/custom-ca.crt
sudo update-ca-certificates
```

## Browser-specific (Chrome/Edge)
1. Go to Settings > Privacy and Security > Security
2. Click "Manage certificates"
3. Go to "Trusted Root Certification Authorities" tab
4. Click "Import..." and select ca/certs/ca.crt

## Browser-specific (Firefox)
1. Go to Settings > Privacy & Security
2. Scroll to "Certificates" and click "View Certificates"
3. Go to "Authorities" tab
4. Click "Import..." and select ca/certs/ca.crt
5. Check "Trust this CA to identify websites"
"@

$installInstructions | Out-File -FilePath (Join-Path $caDir "INSTALL_INSTRUCTIONS.md") -Encoding utf8

Write-ColorOutput "✅ Generated installation instructions: ca/INSTALL_INSTRUCTIONS.md" $Green

Write-ColorOutput "`n🎉 Custom CA generation complete!" $Green
Write-ColorOutput "📁 CA files located in: $caDir/" $Blue
Write-ColorOutput ""
Write-ColorOutput "📋 Generated Files:" $Blue
Write-ColorOutput "🔑 Private key:     $caKeyPath" $Yellow
Write-ColorOutput "📜 Certificate:     $caCertPath" $Yellow  
Write-ColorOutput "📜 PEM format:      $caPemPath" $Yellow
Write-ColorOutput "📦 CA bundle:       $caBundlePath" $Yellow
Write-ColorOutput "🔗 Chain file:      $caChainPath" $Yellow
Write-ColorOutput ""
Write-ColorOutput "💡 Next steps:" $Blue
Write-ColorOutput "1. Install the CA certificate in your system/browser trust store" $Reset
Write-ColorOutput "2. Use .\generate-certificates.ps1 -UseCustomCA to generate CA-signed certificates" $Reset
Write-ColorOutput "3. Review ca/INSTALL_INSTRUCTIONS.md for detailed installation steps" $Reset