# PowerShell Companion Script for Makefile
param(
    [string]$Target = "help",
    [string]$Service = "",
    [switch]$Follow,
    [switch]$Tail,
    [int]$Lines = 100
)

function Write-ColorText {
    param([string]$Text, [string]$Color = "White")
    Write-Host $Text -ForegroundColor $Color
}

function Show-Help {
    Write-ColorText "SSO Application PowerShell Helper" "Blue"
    Write-ColorText "=================================" "Blue"
    Write-Host ""
    Write-ColorText "Usage: .\make.ps1 [target] [options]" "White"
    Write-Host ""
    Write-ColorText "Targets:" "Green"
    Write-Host "  help          Show this help message"
    Write-Host "  status        Show service status"
    Write-Host "  up            Start all services"
    Write-Host "  down          Stop all services"
    Write-Host "  logs          Show logs"
    Write-Host ""
    Write-ColorText "Examples:" "Cyan"
    Write-Host "  .\make.ps1 up"
    Write-Host "  .\make.ps1 status"
    Write-Host "  .\make.ps1 logs"
}

function Show-Status {
    Write-ColorText "Service Status:" "Blue"
    docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
}

function Start-Services {
    Write-ColorText "Starting all services..." "Green"
    docker compose up -d
}

function Stop-Services {
    Write-ColorText "Stopping all services..." "Red"
    docker compose down
}

function Show-Logs {
    Write-ColorText "Showing logs..." "Blue"
    if ($Service) {
        docker compose logs -f $Service
    } else {
        docker compose logs -f
    }
}

switch ($Target.ToLower()) {
    "help" { Show-Help }
    "status" { Show-Status }
    "up" { Start-Services }
    "down" { Stop-Services }
    "logs" { Show-Logs }
    default { 
        Write-ColorText "Unknown target: $Target" "Red"
        Show-Help
    }
}
