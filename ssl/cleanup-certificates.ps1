#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Clean up certificates and keys

.DESCRIPTION
    This script provides options to clean up certificates and keys:
    - Remove all generated certificates and keys
    - Remove only expired certificates
    - Clean up backup files
    - Reset to fresh state

.PARAMETER All
    Remove all certificates and keys

.PARAMETER Expired
    Remove only expired certificates

.PARAMETER Backups
    Remove only backup files

.PARAMETER Force
    Skip confirmation prompts

.EXAMPLE
    .\cleanup-certificates.ps1 -All
    .\cleanup-certificates.ps1 -Expired -Force
    .\cleanup-certificates.ps1 -Backups
#>

param(
    [switch]$All,
    [switch]$Expired,
    [switch]$Backups,
    [switch]$Force
)

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

function Remove-FileIfExists {
    param([string]$FilePath, [string]$Description)
    
    if (Test-Path $FilePath) {
        if ($Force -or (Read-Host "Remove $Description ($FilePath)? (y/N)") -eq 'y') {
            Remove-Item $FilePath -Force
            Write-ColorOutput "✅ Removed: $FilePath" $Green
            return $true
        }
        else {
            Write-ColorOutput "⏭️  Skipped: $FilePath" $Yellow
            return $false
        }
    }
    return $false
}

function Test-CertificateExpired {
    param([string]$CertPath)
    
    if (-not (Test-Path $CertPath)) {
        return $false
    }
    
    if (-not (Test-OpenSSL)) {
        return $false
    }
    
    try {
        $certText = & openssl x509 -in $CertPath -text -noout 2>$null
        if ($LASTEXITCODE -ne 0) {
            return $false
        }
        
        $notAfterLine = ($certText | Select-String "Not After").Line
        if ($notAfterLine) {
            $notAfterStr = ($notAfterLine -split "Not After : ")[1]
            $notAfterDate = [DateTime]::Parse($notAfterStr)
            return $notAfterDate -lt (Get-Date)
        }
        return $false
    }
    catch {
        return $false
    }
}

Write-ColorOutput "🧹 SSL Certificate Cleanup Utility" $Blue
Write-ColorOutput "===================================" $Blue

if (-not $All -and -not $Expired -and -not $Backups) {
    Write-ColorOutput "No cleanup option specified. Available options:" $Yellow
    Write-ColorOutput "  -All      Remove all certificates and keys" $Cyan
    Write-ColorOutput "  -Expired  Remove only expired certificates" $Cyan
    Write-ColorOutput "  -Backups  Remove only backup files" $Cyan
    Write-ColorOutput "  -Force    Skip confirmation prompts" $Cyan
    Write-ColorOutput ""
    Write-ColorOutput "Example: .\cleanup-certificates.ps1 -All -Force" $Green
    exit 0
}

$removedCount = 0

if ($Backups) {
    Write-ColorOutput "`n🗑️  Cleaning up backup files..." $Cyan
    
    $backupFiles = Get-ChildItem -Recurse -Filter "*.backup.*" 2>$null
    if ($backupFiles) {
        foreach ($file in $backupFiles) {
            if (Remove-FileIfExists $file.FullName "backup file") {
                $removedCount++
            }
        }
    }
    else {
        Write-ColorOutput "No backup files found" $Green
    }
}

if ($Expired) {
    if (-not (Test-OpenSSL)) {
        Write-ColorOutput "ERROR: OpenSSL is required to check certificate expiry" $Red
        exit 1
    }
    
    Write-ColorOutput "`n⏰ Cleaning up expired certificates..." $Cyan
    
    $certFiles = @(
        "certs/eservice.crt",
        "certs/mockpass.crt", 
        "saml/public.crt"
    )
    
    foreach ($certFile in $certFiles) {
        if (Test-CertificateExpired $certFile) {
            Write-ColorOutput "Found expired certificate: $certFile" $Yellow
            if (Remove-FileIfExists $certFile "expired certificate") {
                $removedCount++
                
                # Also remove corresponding key files
                $keyFile = $certFile.Replace("certs/", "private/").Replace(".crt", ".key")
                if ($certFile -eq "saml/public.crt") {
                    $keyFile = "saml/key.pem"
                }
                
                if (Remove-FileIfExists $keyFile "corresponding key") {
                    $removedCount++
                }
            }
        }
    }
}

if ($All) {
    Write-ColorOutput "`n🗑️  Removing all certificates and keys..." $Cyan
    
    if (-not $Force) {
        Write-ColorOutput "⚠️  This will remove ALL certificates and keys!" $Red
        $confirm = Read-Host "Are you sure? Type 'yes' to continue"
        if ($confirm -ne 'yes') {
            Write-ColorOutput "Cleanup cancelled" $Yellow
            exit 0
        }
    }
    
    # SSL Certificates
    $allFiles = @(
        "certs/eservice.crt",
        "certs/mockpass.crt",
        "private/eservice.key", 
        "private/mockpass.key",
        "saml/public.crt",
        "saml/key.pem",
        "saml/public_base64.txt",
        "saml/key_base64.txt",
        "oidc/private_key.pem",
        "oidc/private-key.pem",
        "oidc/public_key.pem",
        "oidc/private_key_base64.txt",
        "oidc/public_key_base64.txt",
        "encryption.key"
    )
    
    foreach ($file in $allFiles) {
        if (Remove-FileIfExists $file "certificate/key file") {
            $removedCount++
        }
    }
    
    # Remove empty directories
    $directories = @("certs", "private", "saml", "oidc")
    foreach ($dir in $directories) {
        if (Test-Path $dir) {
            $items = Get-ChildItem $dir 2>$null
            if (-not $items) {
                Remove-Item $dir -Force
                Write-ColorOutput "✅ Removed empty directory: $dir" $Green
            }
        }
    }
}

Write-ColorOutput "`n📊 Cleanup Summary" $Blue
Write-ColorOutput "=================" $Blue
Write-ColorOutput "Files removed: $removedCount" $Green

if ($removedCount -gt 0) {
    Write-ColorOutput "🎉 Cleanup complete!" $Green
    Write-ColorOutput "💡 Run '.\generate-certificates.ps1' to create new certificates" $Cyan
}
else {
    Write-ColorOutput "No files were removed" $Yellow
}