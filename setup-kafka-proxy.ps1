# setup-kafka-proxy.ps1
# Auto-detects the WSL2 IP and sets up port forwarding so that
# localhost:9092 (Kafka) and localhost:2181 (Zookeeper) are reachable from Windows.
# Must be run as Administrator.

# Get the WSL2 IP address
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
Write-Host "Kafka is now reachable at localhost:9092" -ForegroundColor Green
Write-Host "Zookeeper is now reachable at localhost:2181" -ForegroundColor Green
