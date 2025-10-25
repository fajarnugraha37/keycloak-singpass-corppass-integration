#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Validate SSL certificates and keys for common issues

.DESCRIPTION
    This script validates certificates and keys for:
    - Certificate expiry
    - File existence
    - Key strength validation

.EXAMPLE
    .\validate-certificates.ps1
#>

param()

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

# Check if OpenSSL is available
if (-not (Test-OpenSSL)) {
    Write-ColorOutput "ERROR: OpenSSL is not installed or not in PATH" $Red
    Write-ColorOutput "Certificate validation requires OpenSSL" $Red
    exit 1
}

Write-ColorOutput "🔍 SSL Certificate and Key Validation" $Blue
Write-ColorOutput "====================================" $Blue

$totalTests = 0
$passedTests = 0

function Test-FileExists {
    param([string]$FilePath, [string]$Description)
    
    $global:totalTests++
    if (Test-Path $FilePath) {
        Write-ColorOutput "✅ $Description exists" $Green
        $global:passedTests++
        return $true
    }
    else {
        Write-ColorOutput "❌ $Description missing" $Red
        return $false
    }
}

function Test-CertificateExpiry {
    param([string]$CertPath, [string]$Description)
    
    $global:totalTests++
    if (-not (Test-Path $CertPath)) {
        Write-ColorOutput "❌ $Description - file missing" $Red
        return $false
    }
    
    try {
        $certText = & openssl x509 -in $CertPath -text -noout 2>$null
        if ($LASTEXITCODE -ne 0) {
            Write-ColorOutput "❌ $Description - failed to read certificate" $Red
            return $false
        }
        
        $notAfterLine = ($certText | Select-String "Not After").Line
        if ($notAfterLine) {
            $notAfterStr = ($notAfterLine -split "Not After : ")[1]
            $notAfterDate = [DateTime]::Parse($notAfterStr)
            $daysUntilExpiry = ($notAfterDate - (Get-Date)).Days
            
            if ($daysUntilExpiry -lt 0) {
                Write-ColorOutput "❌ $Description - EXPIRED" $Red
                return $false
            }
            elseif ($daysUntilExpiry -lt 30) {
                Write-ColorOutput "⚠️  $Description - expires in $daysUntilExpiry days" $Yellow
                $global:passedTests++
                return $true
            }
            else {
                Write-ColorOutput "✅ $Description - valid for $daysUntilExpiry days" $Green
                $global:passedTests++
                return $true
            }
        }
        else {
            Write-ColorOutput "❌ $Description - could not parse expiry" $Red
            return $false
        }
    }
    catch {
        Write-ColorOutput "❌ $Description - error checking expiry" $Red
        return $false
    }
}

function Test-KeyStrength {
    param([string]$KeyPath, [string]$Description, [int]$MinBits = 2048)
    
    $global:totalTests++
    if (-not (Test-Path $KeyPath)) {
        Write-ColorOutput "❌ $Description - key file missing" $Red
        return $false
    }
    
    try {
        $keyInfo = & openssl rsa -in $KeyPath -text -noout 2>$null
        if ($LASTEXITCODE -ne 0) {
            Write-ColorOutput "❌ $Description - failed to read key" $Red
            return $false
        }
        
        $keySizeLine = ($keyInfo | Select-String "Private-Key").Line
        if ($keySizeLine -match "\((\d+) bit") {
            $keySize = [int]$matches[1]
            if ($keySize -ge $MinBits) {
                Write-ColorOutput "✅ $Description - adequate strength ($keySize bits)" $Green
                $global:passedTests++
                return $true
            }
            else {
                Write-ColorOutput "⚠️  $Description - weak key ($keySize bits)" $Yellow
                return $false
            }
        }
        else {
            Write-ColorOutput "❌ $Description - could not determine key size" $Red
            return $false
        }
    }
    catch {
        Write-ColorOutput "❌ $Description - error checking key strength" $Red
        return $false
    }
}

function Test-Base64File {
    param([string]$FilePath, [string]$Description)
    
    $global:totalTests++
    if (-not (Test-Path $FilePath)) {
        Write-ColorOutput "❌ $Description - file missing" $Red
        return $false
    }
    
    try {
        $content = Get-Content $FilePath -Raw
        if ([string]::IsNullOrWhiteSpace($content)) {
            Write-ColorOutput "❌ $Description - file is empty" $Red
            return $false
        }
        
        $bytes = [Convert]::FromBase64String($content)
        if ($bytes.Length -gt 0) {
            Write-ColorOutput "✅ $Description - valid base64" $Green
            $global:passedTests++
            return $true
        }
        else {
            Write-ColorOutput "❌ $Description - invalid base64" $Red
            return $false
        }
    }
    catch {
        Write-ColorOutput "❌ $Description - base64 decode error" $Red
        return $false
    }
}

# Test SSL Certificates
Write-ColorOutput "`n🌐 SSL Certificates" $Cyan
Test-FileExists "certs/eservice.crt" "eService SSL Certificate"
Test-FileExists "private/eservice.key" "eService SSL Key"
Test-CertificateExpiry "certs/eservice.crt" "eService SSL Certificate"
Test-KeyStrength "private/eservice.key" "eService SSL Key"

Test-FileExists "certs/mockpass.crt" "MockPass SSL Certificate"
Test-FileExists "private/mockpass.key" "MockPass SSL Key"
Test-CertificateExpiry "certs/mockpass.crt" "MockPass SSL Certificate"
Test-KeyStrength "private/mockpass.key" "MockPass SSL Key"

# Test SAML Keys
Write-ColorOutput "`n🔑 SAML Keys" $Cyan
Test-FileExists "saml/public.crt" "SAML Public Certificate"
Test-FileExists "saml/key.pem" "SAML Private Key"
Test-CertificateExpiry "saml/public.crt" "SAML Public Certificate"
Test-KeyStrength "saml/key.pem" "SAML Private Key"
Test-Base64File "saml/public_base64.txt" "SAML Public Base64"
Test-Base64File "saml/key_base64.txt" "SAML Key Base64"

# Test OIDC Keys
Write-ColorOutput "`n🔐 OIDC/JWT Keys" $Cyan
Test-FileExists "oidc/private_key.pem" "OIDC Private Key"
Test-FileExists "oidc/public_key.pem" "OIDC Public Key"
Test-KeyStrength "oidc/private_key.pem" "OIDC Private Key" 3072
Test-Base64File "oidc/private_key_base64.txt" "OIDC Private Base64"
Test-Base64File "oidc/public_key_base64.txt" "OIDC Public Base64"

# Test additional files
Write-ColorOutput "`n🔒 Additional Files" $Cyan
Test-FileExists "encryption.key" "Encryption Key"

# Summary
Write-ColorOutput "`n📊 Validation Summary" $Blue
Write-ColorOutput "===================" $Blue

$successRate = if ($totalTests -gt 0) { [math]::Round(($passedTests / $totalTests) * 100, 1) } else { 0 }
Write-ColorOutput "Results: $passedTests/$totalTests tests passed ($successRate%)" $Blue

if ($successRate -eq 100) {
    Write-ColorOutput "🎉 All validations passed!" $Green
}
elseif ($successRate -ge 80) {
    Write-ColorOutput "⚠️  Most validations passed, some issues found" $Yellow
}
else {
    Write-ColorOutput "❌ Several validation failures - please review" $Red
}