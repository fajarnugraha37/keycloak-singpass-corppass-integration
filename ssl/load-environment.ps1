#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Load certificate information into environment variables

.DESCRIPTION
    This script loads certificates and keys into environment variables for easy use in applications.
    It can generate PowerShell, Bash, or .env file formats.

.PARAMETER Format
    Output format: powershell, bash, or dotenv (default: powershell)

.PARAMETER Output
    Output file path (if not specified, outputs to console)

.EXAMPLE
    .\load-environment.ps1 -Format powershell
    .\load-environment.ps1 -Format dotenv -Output .env
    .\load-environment.ps1 -Format bash > setup_env.sh
#>

param(
    [ValidateSet("powershell", "bash", "dotenv")]
    [string]$Format = "powershell",
    [string]$Output
)

# Colors for output (only used for console output)
$Green = "`e[32m"
$Yellow = "`e[33m"
$Red = "`e[31m"
$Blue = "`e[34m"
$Reset = "`e[0m"

function Write-ColorOutput {
    param([string]$Message, [string]$Color = $Reset)
    if (-not $Output) {
        Write-Host "$Color$Message$Reset"
    }
}

function Get-Base64Content {
    param([string]$FilePath)
    
    if (Test-Path $FilePath) {
        try {
            $content = Get-Content $FilePath -Raw
            return [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($content))
        }
        catch {
            return $null
        }
    }
    return $null
}

function Get-Base64File {
    param([string]$FilePath)
    
    if (Test-Path $FilePath) {
        try {
            return Get-Content $FilePath -Raw
        }
        catch {
            return $null
        }
    }
    return $null
}

Write-ColorOutput "🔐 Loading SSL Certificate Environment Variables" $Blue
Write-ColorOutput "Format: $Format" $Blue

$envVars = @{}

# SSL Certificate paths
$envVars["SSL_ESERVICE_CERT_PATH"] = "./ssl/certs/eservice.crt"
$envVars["SSL_ESERVICE_KEY_PATH"] = "./ssl/private/eservice.key"
$envVars["SSL_MOCKPASS_CERT_PATH"] = "./ssl/certs/mockpass.crt"
$envVars["SSL_MOCKPASS_KEY_PATH"] = "./ssl/private/mockpass.key"

# SAML paths
$envVars["SAML_SIGNING_CERT_PATH"] = "./ssl/saml/public.crt"
$envVars["SAML_SIGNING_KEY_PATH"] = "./ssl/saml/key.pem"

# JWT paths
$envVars["JWT_PRIVATE_KEY_PATH"] = "./ssl/oidc/private_key.pem"
$envVars["JWT_PUBLIC_KEY_PATH"] = "./ssl/oidc/public_key.pem"

# Base64 encoded content
Write-ColorOutput "Loading base64 encoded certificates..." $Yellow

# SAML base64
$samlPublicBase64 = Get-Base64File "saml/public_base64.txt"
if ($samlPublicBase64) {
    $envVars["SAML_PUBLIC_CERT_BASE64"] = $samlPublicBase64
    Write-ColorOutput "✅ Loaded SAML public certificate base64" $Green
}
else {
    Write-ColorOutput "⚠️  SAML public certificate base64 not found" $Yellow
    $envVars["SAML_PUBLIC_CERT_BASE64"] = ""
}

$samlPrivateBase64 = Get-Base64File "saml/key_base64.txt"
if ($samlPrivateBase64) {
    $envVars["SAML_PRIVATE_KEY_BASE64"] = $samlPrivateBase64
    Write-ColorOutput "✅ Loaded SAML private key base64" $Green
}
else {
    Write-ColorOutput "⚠️  SAML private key base64 not found" $Yellow
    $envVars["SAML_PRIVATE_KEY_BASE64"] = ""
}

# JWT base64
$jwtPrivateBase64 = Get-Base64File "oidc/private_key_base64.txt"
if ($jwtPrivateBase64) {
    $envVars["JWT_PRIVATE_KEY_BASE64"] = $jwtPrivateBase64
    Write-ColorOutput "✅ Loaded JWT private key base64" $Green
}
else {
    Write-ColorOutput "⚠️  JWT private key base64 not found" $Yellow
    $envVars["JWT_PRIVATE_KEY_BASE64"] = ""
}

$jwtPublicBase64 = Get-Base64File "oidc/public_key_base64.txt"
if ($jwtPublicBase64) {
    $envVars["JWT_PUBLIC_KEY_BASE64"] = $jwtPublicBase64
    Write-ColorOutput "✅ Loaded JWT public key base64" $Green
}
else {
    Write-ColorOutput "⚠️  JWT public key base64 not found" $Yellow
    $envVars["JWT_PUBLIC_KEY_BASE64"] = ""
}

# Encryption key
$encryptionKey = Get-Base64File "encryption.key"
if ($encryptionKey) {
    $envVars["ENCRYPTION_KEY"] = $encryptionKey
    Write-ColorOutput "✅ Loaded encryption key" $Green
}
else {
    Write-ColorOutput "⚠️  Encryption key not found" $Yellow
    $envVars["ENCRYPTION_KEY"] = ""
}

# Generate output based on format
$outputLines = @()

if (-not $Output) {
    Write-ColorOutput "`n📋 Environment Variables ($Format format):" $Blue
    Write-ColorOutput "=" * 50 $Blue
}

switch ($Format) {
    "powershell" {
        $outputLines += "# PowerShell Environment Variables"
        $outputLines += "# Generated on $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        $outputLines += ""
        
        foreach ($var in $envVars.GetEnumerator()) {
            if ($var.Value) {
                $outputLines += "`$env:$($var.Key) = '$($var.Value)'"
            }
            else {
                $outputLines += "# `$env:$($var.Key) = ''"
            }
        }
        
        $outputLines += ""
        $outputLines += "Write-Host 'SSL certificate environment variables loaded' -ForegroundColor Green"
    }
    
    "bash" {
        $outputLines += "#!/bin/bash"
        $outputLines += "# Bash Environment Variables"
        $outputLines += "# Generated on $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        $outputLines += ""
        
        foreach ($var in $envVars.GetEnumerator()) {
            if ($var.Value) {
                $outputLines += "export $($var.Key)='$($var.Value)'"
            }
            else {
                $outputLines += "# export $($var.Key)=''"
            }
        }
        
        $outputLines += ""
        $outputLines += "echo 'SSL certificate environment variables loaded'"
    }
    
    "dotenv" {
        $outputLines += "# Environment Variables"
        $outputLines += "# Generated on $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        $outputLines += ""
        
        foreach ($var in $envVars.GetEnumerator()) {
            if ($var.Value) {
                $outputLines += "$($var.Key)=$($var.Value)"
            }
            else {
                $outputLines += "# $($var.Key)="
            }
        }
        
        # Add additional common variables
        $outputLines += ""
        $outputLines += "# Additional Configuration"
        $outputLines += "ESERVICE_URL=https://eservice.localhost:3000"
        $outputLines += "MOCKPASS_URL=https://mockpass.localhost:5000"
        $outputLines += "NODE_ENV=development"
        $outputLines += "LOG_LEVEL=info"
    }
}

# Output to file or console
if ($Output) {
    $outputLines | Out-File -FilePath $Output -Encoding utf8
    Write-ColorOutput "✅ Environment variables written to: $Output" $Green
}
else {
    $outputLines | ForEach-Object { Write-Host $_ }
}

Write-ColorOutput "`n🎉 Environment variable generation complete!" $Green