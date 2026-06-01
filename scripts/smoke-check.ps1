param(
    [string]$GatewayUrl = "http://localhost:8085",
    [string]$DashboardUiUrl = "http://localhost:8501",
    [string]$RabbitContainer = "rabbitmq",
    [switch]$CheckRabbit,
    [switch]$CheckDashboard
)

$ErrorActionPreference = "Stop"

function Assert-HttpCode {
    param(
        [string]$Name,
        [string]$Code,
        [int[]]$Expected
    )

    if (-not ($Expected -contains [int]$Code)) {
        throw "$Name failed. HTTP $Code (expected: $($Expected -join ','))"
    }
}

function Get-StatusCode {
    param(
        [string]$Method,
        [string]$Url,
        [string]$BodyFile = ""
    )

    if ($BodyFile) {
        return & curl.exe -s -o NUL -w "%{http_code}" -X $Method -H "Content-Type: application/json" --data-binary "@$BodyFile" $Url
    }

    return & curl.exe -s -o NUL -w "%{http_code}" -X $Method $Url
}

$payloadObject = @(
    @{
        id = 101
        name = "sensor-101"
        manufacturer = "acme"
        type = "SENSOR_TEMPERATURE"
        capabilities = @("temp")
        location = @{
            x = 1
            y = 2
            z = 3
        }
        status = @{
            isOnline = $false
            batteryLevel = 3
            signalStrength = 5
            lastHeartbeat = "2026-04-26T08:00:00Z"
        }
    }
)

$payload = ConvertTo-Json -InputObject $payloadObject -Depth 6

Write-Host "Checking gateway endpoints..."
$simStatusCode = Get-StatusCode -Method "GET" -Url "$GatewayUrl/api/v1/simulator/status"
Assert-HttpCode -Name "GET /api/v1/simulator/status" -Code $simStatusCode -Expected @(200)

$analyticsStatusCode = Get-StatusCode -Method "GET" -Url "$GatewayUrl/api/v1/analytics/status"
Assert-HttpCode -Name "GET /api/v1/analytics/status" -Code $analyticsStatusCode -Expected @(200)

$analyticsLiveCode = Get-StatusCode -Method "GET" -Url "$GatewayUrl/api/v1/analytics/live/summary"
Assert-HttpCode -Name "GET /api/v1/analytics/live/summary" -Code $analyticsLiveCode -Expected @(200)

Write-Host "Configuring analytics method..."
$analyticsConfigCode = Get-StatusCode -Method "POST" -Url "$GatewayUrl/api/v1/analytics/config?method=Parallel&batchSize=50"
Assert-HttpCode -Name "POST /api/v1/analytics/config" -Code $analyticsConfigCode -Expected @(200, 201, 202)

if ($CheckDashboard) {
    Write-Host "Checking dashboard endpoints..."
    $dashboardUiCode = Get-StatusCode -Method "GET" -Url $DashboardUiUrl
    Assert-HttpCode -Name "GET / (dashboard-ui)" -Code $dashboardUiCode -Expected @(200)
}

Write-Host "POST $GatewayUrl/api/v1/controller"
$payloadFile = [System.IO.Path]::GetTempFileName()
Set-Content -Path $payloadFile -Value $payload -NoNewline

try {
    $statusCode = Get-StatusCode -Method "POST" -Url "$GatewayUrl/api/v1/controller" -BodyFile $payloadFile
} finally {
    Remove-Item -Path $payloadFile -ErrorAction SilentlyContinue
}

Write-Host "Ingest status: $statusCode"
Assert-HttpCode -Name "POST /api/v1/controller" -Code $statusCode -Expected @(200, 201, 202)

$analytics = @()
$maxAttempts = 8
for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    Start-Sleep -Seconds 2
    $analytics = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/analytics/history?limit=5"
    if ($analytics -and $analytics.Count -gt 0) {
        break
    }
}

$alerts = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/alerts?limit=10"
$liveSummary = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/analytics/live/summary"
$liveByType = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/analytics/live/by-type"

$to = [DateTime]::UtcNow
$from = $to.AddMinutes(-5)
$fromIso = $from.ToString("yyyy-MM-ddTHH:mm:ssZ")
$toIso = $to.ToString("yyyy-MM-ddTHH:mm:ssZ")
$reportWindow = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/analytics/report/window?from=$fromIso&to=$toIso"

if (-not $analytics -or $analytics.Count -eq 0) {
    throw "Analytics history is empty after ingest and retries."
}

Write-Host ""
Write-Host "Analytics:"
$analytics | ConvertTo-Json -Depth 8

Write-Host ""
Write-Host "Alerts:"
$alerts | ConvertTo-Json -Depth 8

Write-Host ""
Write-Host "Analytics Live Summary:"
$liveSummary | ConvertTo-Json -Depth 8

Write-Host ""
Write-Host "Analytics Live By Type:"
$liveByType | ConvertTo-Json -Depth 8

Write-Host ""
Write-Host "Analytics Report Window (last 5m):"
$reportWindow | ConvertTo-Json -Depth 8

if ($CheckRabbit) {
    Write-Host ""
    Write-Host "RabbitMQ queues:"
    docker exec -i $RabbitContainer rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
}

Write-Host ""
Write-Host "Smoke-check completed successfully."
