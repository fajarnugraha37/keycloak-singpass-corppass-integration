# SSL Certificate Management PowerShell Module
# This file can be imported to add certificate management functions to your PowerShell session

# Import all required functions
. "$PSScriptRoot\generate-certificates.ps1"
. "$PSScriptRoot\show-certificates.ps1"  
. "$PSScriptRoot\validate-certificates.ps1"
. "$PSScriptRoot\cleanup-certificates.ps1"
. "$PSScriptRoot\load-environment.ps1"

# Export functions for module usage
function Invoke-CertificateGeneration {
    [CmdletBinding()]
    param(
        [switch]$Force,
        [int]$ValidityDays = 365
    )
    
    & "$PSScriptRoot\generate-certificates.ps1" -Force:$Force -ValidityDays $ValidityDays
}

function Show-CertificateInfo {
    [CmdletBinding()]
    param()
    
    & "$PSScriptRoot\show-certificates.ps1"
}

function Test-CertificateHealth {
    [CmdletBinding()]
    param()
    
    & "$PSScriptRoot\validate-certificates.ps1"
}

function Remove-Certificates {
    [CmdletBinding()]
    param(
        [switch]$All,
        [switch]$Expired,
        [switch]$Backups,
        [switch]$Force
    )
    
    $params = @{}
    if ($All) { $params.Add('All', $true) }
    if ($Expired) { $params.Add('Expired', $true) }
    if ($Backups) { $params.Add('Backups', $true) }
    if ($Force) { $params.Add('Force', $true) }
    
    & "$PSScriptRoot\cleanup-certificates.ps1" @params
}

function Export-CertificateEnvironment {
    [CmdletBinding()]
    param(
        [ValidateSet("powershell", "bash", "dotenv")]
        [string]$Format = "powershell",
        [string]$Output
    )
    
    $params = @{ Format = $Format }
    if ($Output) { $params.Add('Output', $Output) }
    
    & "$PSScriptRoot\load-environment.ps1" @params
}

# Quick access aliases
Set-Alias -Name gencerts -Value Invoke-CertificateGeneration
Set-Alias -Name showcerts -Value Show-CertificateInfo
Set-Alias -Name testcerts -Value Test-CertificateHealth
Set-Alias -Name cleancerts -Value Remove-Certificates
Set-Alias -Name loadenv -Value Export-CertificateEnvironment

Write-Host "🔐 SSL Certificate Management Module Loaded" -ForegroundColor Green
Write-Host "Available commands:" -ForegroundColor Cyan
Write-Host "  Invoke-CertificateGeneration (gencerts) - Generate certificates" -ForegroundColor White
Write-Host "  Show-CertificateInfo (showcerts)        - Display certificate info" -ForegroundColor White  
Write-Host "  Test-CertificateHealth (testcerts)      - Validate certificates" -ForegroundColor White
Write-Host "  Remove-Certificates (cleancerts)        - Clean up certificates" -ForegroundColor White
Write-Host "  Export-CertificateEnvironment (loadenv) - Generate environment vars" -ForegroundColor White