param([string]$BaseUrl = "https://api.example.com")
$ErrorActionPreference = "Stop"
Write-Host "Checking readiness..."
$health = Invoke-WebRequest -Uri "$BaseUrl/actuator/health/readiness" -UseBasicParsing
if ($health.StatusCode -ne 200) { throw "Readiness check failed: $($health.StatusCode)" }
$headers = (Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing).Headers
if (-not $headers.ContainsKey("Strict-Transport-Security")) { Write-Warning "HSTS was not observed; verify TLS/ingress configuration." }
Write-Host "Production smoke test passed."
