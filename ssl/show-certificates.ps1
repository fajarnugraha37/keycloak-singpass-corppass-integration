#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Display information about SSL certificates and keys

.DESCRIPTION
    This script shows detailed information about all certificates and keys in the repository:
    - SSL certificate validity and subject information
    - SAML certificate details
    - OIDC/JWT key information
    - File sizes and creation dates

.EXAMPLE
    .\show-certificates.ps1
#>

# Colors for output
$Green = "`e[32m"
$Yellow = "`e[33m"
$Red = "`e[31m"
$Blue = "`e[34m"
$Cyan = "`e[36m"
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

function Show-CertificateInfo {
    param(
        [string]$CertPath,
        [string]$Name
    )
    
    if (Test-Path $CertPath) {
        Write-ColorOutput "`n📜 $Name Certificate ($CertPath)" $Cyan
        Write-ColorOutput "=" * 50 $Blue
        
        # Get file info
        $fileInfo = Get-Item $CertPath
        Write-ColorOutput "File Size: $($fileInfo.Length) bytes" $Green
        Write-ColorOutput "Created: $($fileInfo.CreationTime)" $Green
        Write-ColorOutput "Modified: $($fileInfo.LastWriteTime)" $Green
        
        # Get certificate details using OpenSSL
        try {
            $certText = & openssl x509 -in $CertPath -text -noout 2>$null
            if ($LASTEXITCODE -eq 0) {
                # Extract key information
                $subject = ($certText | Select-String "Subject:").Line
                $issuer = ($certText | Select-String "Issuer:").Line
                $notBefore = ($certText | Select-String "Not Before:").Line
                $notAfter = ($certText | Select-String "Not After:").Line
                $signatureAlg = ($certText | Select-String "Signature Algorithm:").Line | Select-Object -First 1
                
                if ($subject) { Write-ColorOutput $subject.Trim() $Yellow }
                if ($issuer) { Write-ColorOutput $issuer.Trim() $Yellow }
                if ($notBefore) { Write-ColorOutput $notBefore.Trim() $Yellow }
                if ($notAfter) { Write-ColorOutput $notAfter.Trim() $Yellow }
                if ($signatureAlg) { Write-ColorOutput $signatureAlg.Trim() $Yellow }
                
                # Check if certificate is expired
                $notAfterDate = [DateTime]::Parse(($notAfter -split "Not After : ")[1])
                if ($notAfterDate -lt (Get-Date)) {
                    Write-ColorOutput "⚠️  CERTIFICATE EXPIRED!" $Red
                }
                elseif ($notAfterDate -lt (Get-Date).AddDays(30)) {
                    Write-ColorOutput "⚠️  Certificate expires within 30 days!" $Yellow
                }
                else {
                    Write-ColorOutput "✅ Certificate is valid" $Green
                }
                
                # Show Subject Alternative Names
                $sanLines = $certText | Select-String "DNS:" 
                if ($sanLines) {
                    Write-ColorOutput "Subject Alternative Names:" $Cyan
                    foreach ($line in $sanLines) {
                        $dnsNames = $line.Line -split "DNS:" | Where-Object { $_ -and $_.Trim() }
                        foreach ($dns in $dnsNames) {
                            Write-ColorOutput "  - DNS: $($dns.Trim().Replace(',',''))" $Green
                        }
                    }
                }
            }
        }
        catch {
            Write-ColorOutput "Could not parse certificate details" $Red
        }
    }
    else {
        Write-ColorOutput "`n❌ $Name Certificate not found: $CertPath" $Red
    }
}

function Show-KeyInfo {
    param(
        [string]$KeyPath,
        [string]$Name
    )
    
    if (Test-Path $KeyPath) {
        Write-ColorOutput "`n🔑 $Name Key ($KeyPath)" $Cyan
        Write-ColorOutput "=" * 40 $Blue
        
        # Get file info
        $fileInfo = Get-Item $KeyPath
        Write-ColorOutput "File Size: $($fileInfo.Length) bytes" $Green
        Write-ColorOutput "Created: $($fileInfo.CreationTime)" $Green
        Write-ColorOutput "Modified: $($fileInfo.LastWriteTime)" $Green
        
        # Try to get key details
        try {
            if ($KeyPath -like "*.pem") {
                # Check if it's a private key
                $keyContent = Get-Content $KeyPath -Raw
                if ($keyContent -like "*BEGIN PRIVATE KEY*" -or $keyContent -like "*BEGIN RSA PRIVATE KEY*") {
                    $keyInfo = & openssl rsa -in $KeyPath -text -noout 2>$null
                    if ($LASTEXITCODE -eq 0) {
                        $keySize = ($keyInfo | Select-String "Private-Key:").Line
                        if ($keySize) {
                            Write-ColorOutput $keySize.Trim() $Yellow
                        }
                    }
                }
                elseif ($keyContent -like "*BEGIN PUBLIC KEY*") {
                    $keyInfo = & openssl rsa -pubin -in $KeyPath -text -noout 2>$null
                    if ($LASTEXITCODE -eq 0) {
                        $keySize = ($keyInfo | Select-String "Public-Key:").Line
                        if ($keySize) {
                            Write-ColorOutput $keySize.Trim() $Yellow
                        }
                    }
                }
            }
        }
        catch {
            Write-ColorOutput "Could not parse key details" $Yellow
        }
    }
    else {
        Write-ColorOutput "`n❌ $Name Key not found: $KeyPath" $Red
    }
}

function Show-Base64Info {
    param(
        [string]$FilePath,
        [string]$Name
    )
    
    if (Test-Path $FilePath) {
        $fileInfo = Get-Item $FilePath
        $content = Get-Content $FilePath -Raw
        Write-ColorOutput "`n📄 $Name Base64 ($FilePath)" $Cyan
        Write-ColorOutput "File Size: $($fileInfo.Length) bytes" $Green
        Write-ColorOutput "Base64 Length: $($content.Length) characters" $Green
        Write-ColorOutput "Preview: $($content.Substring(0, [Math]::Min(50, $content.Length)))..." $Yellow
    }
    else {
        Write-ColorOutput "`n❌ $Name Base64 file not found: $FilePath" $Red
    }
}

# Check if OpenSSL is available
if (-not (Test-OpenSSL)) {
    Write-ColorOutput "WARNING: OpenSSL is not installed or not in PATH" $Yellow
    Write-ColorOutput "Certificate details will be limited" $Yellow
}

Write-ColorOutput "🔐 SSL Certificate and Key Information" $Blue
Write-ColorOutput "=====================================" $Blue

# SSL Certificates
Show-CertificateInfo "certs/eservice.crt" "eService SSL"
Show-KeyInfo "private/eservice.key" "eService SSL Private"

Show-CertificateInfo "certs/mockpass.crt" "MockPass SSL"  
Show-KeyInfo "private/mockpass.key" "MockPass SSL Private"

# SAML Keys
Show-CertificateInfo "saml/public.crt" "SAML Public"
Show-KeyInfo "saml/key.pem" "SAML Private"
Show-Base64Info "saml/public_base64.txt" "SAML Public"
Show-Base64Info "saml/key_base64.txt" "SAML Private"

# OIDC/JWT Keys
Show-KeyInfo "oidc/private_key.pem" "OIDC/JWT Private"
Show-KeyInfo "oidc/public_key.pem" "OIDC/JWT Public"
Show-Base64Info "oidc/private_key_base64.txt" "OIDC/JWT Private"
Show-Base64Info "oidc/public_key_base64.txt" "OIDC/JWT Public"

# Encryption Key
if (Test-Path "encryption.key") {
    $fileInfo = Get-Item "encryption.key"
    $content = Get-Content "encryption.key" -Raw
    Write-ColorOutput "`n🔒 Encryption Key (encryption.key)" $Cyan
    Write-ColorOutput "File Size: $($fileInfo.Length) bytes" $Green
    Write-ColorOutput "Key Length: $($content.Length) characters" $Green
    Write-ColorOutput "Preview: $($content.Substring(0, [Math]::Min(20, $content.Length)))..." $Yellow
}

Write-ColorOutput "`n🎉 Certificate information display complete!" $Green