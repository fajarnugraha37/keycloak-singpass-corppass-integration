#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Generate SSL certificates, SAML keys, and OIDC/JWT keys for local development

.DESCRIPTION
    This script generates all necessary certificates and keys for SSO development:
    - SSL certificates for eservice and mockpass domains
    - SAML signing keys and certificates
    - OIDC/JWT signing keys (RSA 3072-bit)
    - Base64 encoded versions for easy environment variable usage

.PARAMETER Force
    Overwrite existing certificates without prompting

.PARAMETER ValidityDays
    Number of days the certificates should be valid (default: 365)

.EXAMPLE
    .\generate-certificates.ps1
    .\generate-certificates.ps1 -Force -ValidityDays 730
#>

param(
    [switch]$Force,
    [int]$ValidityDays = 365
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

function Test-OpenSSL {
    try {
        $null = & openssl version 2>$null
        return $true
    }
    catch {
        return $false
    }
}

function New-Directory {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
        Write-ColorOutput "Created directory: $Path" $Green
    }
}

function Backup-ExistingFile {
    param([string]$FilePath)
    if (Test-Path $FilePath) {
        $backupPath = "$FilePath.backup.$(Get-Date -Format 'yyyyMMdd_HHmmss')"
        Move-Item $FilePath $backupPath
        Write-ColorOutput "Backed up existing file to: $backupPath" $Yellow
    }
}

function ConvertTo-Base64File {
    param(
        [string]$InputFile,
        [string]$OutputFile
    )
    
    if (Test-Path $InputFile) {
        $content = Get-Content $InputFile -Raw
        $base64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($content))
        $base64 | Out-File -FilePath $OutputFile -Encoding utf8 -NoNewline
        Write-ColorOutput "Generated base64 file: $OutputFile" $Green
    }
}

# Check if OpenSSL is available
if (-not (Test-OpenSSL)) {
    Write-ColorOutput "ERROR: OpenSSL is not installed or not in PATH" $Red
    Write-ColorOutput "Please install OpenSSL and ensure it's available in your PATH" $Red
    exit 1
}

Write-ColorOutput "🔐 SSL Certificate and Key Generation Script" $Blue
Write-ColorOutput "Validity period: $ValidityDays days" $Blue
Write-ColorOutput "============================================" $Blue

# Create directories
New-Directory "certs"
New-Directory "private"
New-Directory "saml"
New-Directory "oidc"

# Function to check if file exists and handle force/backup
function Confirm-GenerateFile {
    param([string]$FilePath, [string]$Description)
    
    if (Test-Path $FilePath) {
        if ($Force) {
            Backup-ExistingFile $FilePath
            return $true
        }
        else {
            $response = Read-Host "$Description already exists. Overwrite? (y/N)"
            if ($response -eq 'y' -or $response -eq 'Y') {
                Backup-ExistingFile $FilePath
                return $true
            }
            return $false
        }
    }
    return $true
}

# Generate SSL Certificates
Write-ColorOutput "`n🌐 Generating SSL Certificates..." $Blue

$sslCerts = @(
    @{ Config = "eservice.conf"; Key = "private/eservice.key"; Cert = "certs/eservice.crt"; Name = "eservice" },
    @{ Config = "mockpass.conf"; Key = "private/mockpass.key"; Cert = "certs/mockpass.crt"; Name = "mockpass" }
)

foreach ($ssl in $sslCerts) {
    if ((Confirm-GenerateFile $ssl.Key "$($ssl.Name) SSL key") -and (Confirm-GenerateFile $ssl.Cert "$($ssl.Name) SSL certificate")) {
        Write-ColorOutput "Generating $($ssl.Name) SSL certificate..." $Green
        & openssl req -x509 -nodes -days $ValidityDays -newkey rsa:2048 `
            -keyout $ssl.Key `
            -out $ssl.Cert `
            -config $ssl.Config `
            -extensions v3_req
        
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✅ Generated: $($ssl.Key) and $($ssl.Cert)" $Green
        }
        else {
            Write-ColorOutput "❌ Failed to generate $($ssl.Name) SSL certificate" $Red
        }
    }
}

# Generate SAML Keys
Write-ColorOutput "`n🔑 Generating SAML Keys..." $Blue

if ((Confirm-GenerateFile "saml/key.pem" "SAML private key") -and (Confirm-GenerateFile "saml/public.crt" "SAML public certificate")) {
    Write-ColorOutput "Generating SAML signing keys..." $Green
    & openssl req -x509 -newkey rsa:2048 -keyout saml/key.pem -out saml/public.crt -sha256 -days $ValidityDays -nodes -subj "/C=SG/ST=Singapore/L=Singapore/O=SSO/OU=SAML/CN=saml-signing"
    
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "✅ Generated SAML keys" $Green
        
        # Generate base64 versions
        ConvertTo-Base64File "saml/key.pem" "saml/key_base64.txt"
        ConvertTo-Base64File "saml/public.crt" "saml/public_base64.txt"
    }
    else {
        Write-ColorOutput "❌ Failed to generate SAML keys" $Red
    }
}

# Generate OIDC/JWT Keys
Write-ColorOutput "`n🔐 Generating OIDC/JWT Keys..." $Blue

if (Confirm-GenerateFile "oidc/private_key.pem" "OIDC private key") {
    Write-ColorOutput "Generating OIDC/JWT RSA keys (3072-bit)..." $Green
    
    # Generate private key
    & openssl genrsa -out oidc/private-key.pem 3072
    
    if ($LASTEXITCODE -eq 0) {
        # Convert to PKCS8 format
        & openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in oidc/private-key.pem -out oidc/private_key.pem
        
        # Generate public key
        & openssl rsa -in oidc/private_key.pem -pubout -out oidc/public_key.pem
        
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✅ Generated OIDC/JWT keys" $Green
            
            # Generate base64 versions
            ConvertTo-Base64File "oidc/private_key.pem" "oidc/private_key_base64.txt"
            ConvertTo-Base64File "oidc/public_key.pem" "oidc/public_key_base64.txt"
        }
        else {
            Write-ColorOutput "❌ Failed to convert OIDC keys" $Red
        }
    }
    else {
        Write-ColorOutput "❌ Failed to generate OIDC private key" $Red
    }
}

# Generate encryption key
Write-ColorOutput "`n🔒 Generating Encryption Key..." $Blue
if (Confirm-GenerateFile "encryption.key" "Encryption key") {
    & openssl rand -base64 32 | Out-File -FilePath "encryption.key" -Encoding utf8 -NoNewline
    Write-ColorOutput "✅ Generated encryption key: encryption.key" $Green
}

Write-ColorOutput "`n🎉 Certificate generation complete!" $Green
Write-ColorOutput "📋 Run '.\show-certificates.ps1' to view certificate details" $Blue