# setup-kafka-proxy.ps1
# After PC reboot:
#   1. Starts Kafka + mock-external-api Docker containers
#   2. Waits for Kafka to be ready
#   3. Auto-detects WSL2 IP and sets up port forwarding
# Must be run as Administrator.

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# ── Step 1: Start Docker containers ─────────────────────────────────────────
Write-Host "Starting Kafka and mock-external-api containers..." -ForegroundColor Cyan
Push-Location $projectDir
docker compose up -d
Pop-Location

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: docker compose up failed. Is Rancher Desktop running?" -ForegroundColor Red
    exit 1
}

# ── Step 2: Wait for Kafka broker to be ready (up to 60s) ───────────────────
Write-Host "Waiting for Kafka broker to be ready (up to 60s)..." -ForegroundColor Cyan
$maxWait = 60
$waited = 0
$ready = $false
while ($waited -lt $maxWait) {
    Start-Sleep -Seconds 3
    $waited += 3
    $status = docker inspect --format='{{.State.Health.Status}}' kafka 2>$null
    if ($status -eq 'healthy') {
        $ready = $true
        break
    }
    Write-Host "  Still waiting... ($waited s)" -ForegroundColor Gray
}

if ($ready) {
    Write-Host "Kafka broker is healthy!" -ForegroundColor Green
} else {
    Write-Host "WARNING: Kafka health check timed out - proceeding anyway." -ForegroundColor Yellow
}

# ── Step 3: Set up WSL2 port forwarding ─────────────────────────────────────
$wslIp = (wsl -- ip addr show eth0 | Select-String -Pattern 'inet (\d+\.\d+\.\d+\.\d+)').Matches.Groups[1].Value

if (-not $wslIp) {
    Write-Host "ERROR: Could not detect WSL2 IP address. Is Rancher Desktop running?" -ForegroundColor Red
    exit 1
}

Write-Host "Detected WSL2 IP: $wslIp" -ForegroundColor Green

# Remove existing port proxies (ignore errors if they don't exist)
netsh interface portproxy delete v4tov4 listenport=9092 listenaddress=127.0.0.1 2>$null
netsh interface portproxy delete v4tov4 listenport=2181 listenaddress=127.0.0.1 2>$null

# Add port forwarding rules
netsh interface portproxy add v4tov4 listenport=9092 listenaddress=127.0.0.1 connectport=9092 connectaddress=$wslIp
netsh interface portproxy add v4tov4 listenport=2181 listenaddress=127.0.0.1 connectport=2181 connectaddress=$wslIp

Write-Host "Port forwarding configured:" -ForegroundColor Cyan
netsh interface portproxy show v4tov4

Write-Host ""
Write-Host "All done! Kafka is reachable at localhost:9092" -ForegroundColor Green
Write-Host "You can now start the Spring Boot backend." -ForegroundColor Green
