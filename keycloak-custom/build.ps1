# Build script for optimized Keycloak custom image (PowerShell)
# Usage: .\build.ps1 [-Tag "tag"] [-Name "name"] [-File "dockerfile"] [-CacheFrom "image"] [-Push]

param(
    [string]$Tag = "latest",
    [string]$Name = "custom-keycloak", 
    [string]$File = "Dockerfile",
    [string]$CacheFrom = "",
    [switch]$Push = $false,
    [switch]$Help = $false
)

if ($Help) {
    Write-Host "Usage: .\build.ps1 [OPTIONS]"
    Write-Host "Options:"
    Write-Host "  -Tag TAG         Set image tag (default: latest)"
    Write-Host "  -Name NAME       Set image name (default: custom-keycloak)"
    Write-Host "  -File FILE       Dockerfile to use (default: Dockerfile)"
    Write-Host "  -CacheFrom IMAGE Use cache from specified image"
    Write-Host "  -Push            Push image after build"
    Write-Host "  -Help            Show this help message"
    exit 0
}

$FullImageName = "${Name}:${Tag}"
$BuildContext = "."

Write-Host "Building Keycloak custom image..." -ForegroundColor Green
Write-Host "Image: $FullImageName" -ForegroundColor Yellow
Write-Host "Dockerfile: $File" -ForegroundColor Yellow
Write-Host "Build context: $BuildContext" -ForegroundColor Yellow

# Prepare cache arguments
$CacheArgs = @()
if ($CacheFrom) {
    $CacheArgs += "--cache-from=$CacheFrom"
}

# Build the image with buildkit optimizations
$env:DOCKER_BUILDKIT = "1"

try {
    docker build @CacheArgs `
        --progress=plain `
        --file $File `
        --tag $FullImageName `
        --build-arg BUILDKIT_INLINE_CACHE=1 `
        $BuildContext

    if ($LASTEXITCODE -ne 0) {
        throw "Docker build failed"
    }

    Write-Host "Build completed successfully!" -ForegroundColor Green

    # Push if requested
    if ($Push) {
        Write-Host "Pushing image to registry..." -ForegroundColor Yellow
        docker push $FullImageName
        
        if ($LASTEXITCODE -ne 0) {
            throw "Docker push failed"
        }
        
        Write-Host "Push completed!" -ForegroundColor Green
    }

    Write-Host "Image: $FullImageName" -ForegroundColor Cyan
    
    # Get image size
    $ImageSize = docker images --format "{{.Size}}" $FullImageName
    Write-Host "Size: $ImageSize" -ForegroundColor Cyan

} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
}