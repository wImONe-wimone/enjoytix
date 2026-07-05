[CmdletBinding()]
param(
    [string]$RedisPassword = $env:ENJOYTIX_REDIS_PASSWORD,
    [string]$RedisHost = $(if ($env:ENJOYTIX_REDIS_HOST) { $env:ENJOYTIX_REDIS_HOST } else { "common-redis-dev.magestack.cn" }),
    [int]$RedisPort = $(if ($env:ENJOYTIX_REDIS_PORT) { [int]$env:ENJOYTIX_REDIS_PORT } else { 19389 }),
    [string]$UniqueName = $(if ($env:ENJOYTIX_UNIQUE_NAME) { $env:ENJOYTIX_UNIQUE_NAME } else { "enjoytix_wimone" }),
    [string]$RedisPrefix = $(if ($env:ENJOYTIX_REDIS_PREFIX) { $env:ENJOYTIX_REDIS_PREFIX } else { "enjoytix_wimone:" }),
    [string]$RocketMqNameServer = $(if ($env:ENJOYTIX_ROCKETMQ_NAME_SERVER) { $env:ENJOYTIX_ROCKETMQ_NAME_SERVER } else { "common-rocketmq-dev.magestack.cn:9876" }),
    [string]$NacosServer = $(if ($env:ENJOYTIX_NACOS_SERVER) { $env:ENJOYTIX_NACOS_SERVER } else { "common-nacos-dev.magestack.cn:8848" }),
    [string]$MysqlHost = $(if ($env:ENJOYTIX_MYSQL_HOST) { $env:ENJOYTIX_MYSQL_HOST } else { "127.0.0.1" }),
    [int]$MysqlPort = $(if ($env:ENJOYTIX_MYSQL_PORT) { [int]$env:ENJOYTIX_MYSQL_PORT } else { 3306 }),
    [string]$MysqlUser = $(if ($env:ENJOYTIX_MYSQL_USER) { $env:ENJOYTIX_MYSQL_USER } else { "root" }),
    [string]$MysqlPassword = $(if ($env:ENJOYTIX_MYSQL_PASSWORD) { $env:ENJOYTIX_MYSQL_PASSWORD } else { "1234" }),
    [long]$UserId = 1,
    [long]$ShowId = 2001,
    [long]$CategoryId = 3001,
    [long]$SeatId = 400105,
    [int]$TicketLockTtlMinutes = 1,
    [int]$WaitTimeoutSeconds = 120,
    [int]$PollIntervalSeconds = 5,
    [string]$JavaExecutable = "java",
    [string]$MavenExecutable = "mvn",
    [string]$MysqlExecutable = "mysql",
    [switch]$SkipBuild,
    [switch]$KeepServices
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 3.0

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$LogDir = Join-Path $ProjectRoot ("build\integration-logs\order-timeout-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
$TicketPort = 9030
$OrderPort = 9040
$CompensationDelayMillis = 10000
$services = @()
$oldMysqlPwd = $env:MYSQL_PWD
$IntegrationFailed = $false
$TranscriptStarted = $false

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

function Invoke-CommandLine {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )
    Write-Host ("> " + $FilePath + " " + ($Arguments -join " "))
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $FilePath"
    }
}

function Invoke-MysqlRows {
    param([string]$Sql)
    try {
        $env:MYSQL_PWD = $MysqlPassword
        $args = @(
            "-h$MysqlHost",
            "-P$MysqlPort",
            "-u$MysqlUser",
            "--batch",
            "--raw",
            "--skip-column-names",
            "-e",
            $Sql
        )
        $output = & $MysqlExecutable @args
        if ($LASTEXITCODE -ne 0) {
            throw "mysql command failed with exit code $LASTEXITCODE"
        }
        return @($output)
    } finally {
        $env:MYSQL_PWD = $oldMysqlPwd
    }
}

function Invoke-MysqlSingle {
    param([string]$Sql)
    $rows = @(Invoke-MysqlRows -Sql $Sql)
    if ($rows.Count -ne 1) {
        throw "Expected one MySQL row, got $($rows.Count). SQL: $Sql"
    }
    return [string]$rows[0]
}

function Stop-PortOwner {
    param([int]$Port)
    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        if ($connection.OwningProcess -and $connection.OwningProcess -ne 0) {
            Write-Host "Stopping process $($connection.OwningProcess) on port $Port"
            Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    }
}

function Start-ServiceJar {
    param(
        [string]$Name,
        [string]$JarPath,
        [string[]]$JvmArguments
    )
    $outFile = Join-Path $LogDir "$Name.out.log"
    $errFile = Join-Path $LogDir "$Name.err.log"
    $arguments = $JvmArguments + @("-jar", $JarPath)
    Write-Host "Starting $Name, logs: $outFile"
    $process = Start-Process `
        -FilePath $JavaExecutable `
        -ArgumentList $arguments `
        -WorkingDirectory $ProjectRoot `
        -RedirectStandardOutput $outFile `
        -RedirectStandardError $errFile `
        -WindowStyle Hidden `
        -PassThru
    return [pscustomobject]@{
        Name = $Name
        Process = $process
        OutLog = $outFile
        ErrLog = $errFile
    }
}

function Wait-Health {
    param(
        [string]$Name,
        [int]$Port,
        [int]$TimeoutSeconds = 90
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $uri = "http://127.0.0.1:$Port/actuator/health"
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-RestMethod -Uri $uri -TimeoutSec 5
            if ($response.status -eq "UP") {
                Write-Host "$Name health is UP"
                return
            }
        } catch {
            Start-Sleep -Seconds 2
            continue
        }
        Start-Sleep -Seconds 2
    }
    throw "$Name did not become healthy in $TimeoutSeconds seconds"
}

function Assert-Equal {
    param(
        [string]$Name,
        [object]$Actual,
        [object]$Expected
    )
    if ([string]$Actual -ne [string]$Expected) {
        throw "$Name assertion failed. Expected=[$Expected], Actual=[$Actual]"
    }
    Write-Host "OK: $Name = $Actual"
}

function Assert-NoLogPattern {
    param(
        [string]$Pattern,
        [string[]]$Files
    )
    $matches = Select-String -Path $Files -Pattern $Pattern -CaseSensitive:$false -ErrorAction SilentlyContinue
    if ($matches) {
        $first = $matches | Select-Object -First 1
        throw "Unexpected log pattern [$Pattern] at $($first.Path):$($first.LineNumber): $($first.Line)"
    }
}

function Stop-StartedServices {
    if ($KeepServices) {
        Write-Host "KeepServices is set; services are still running."
        return
    }
    foreach ($service in $services) {
        try {
            $process = $service.Process
            if ($process -and $process.Id) {
                Write-Host "Stopping $($service.Name), pid=$($process.Id)"
                Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            }
        } catch {
            Write-Host "Skip process cleanup for $($service.Name): $($_.Exception.Message)"
        }
    }
    Stop-PortOwner -Port $TicketPort
    Stop-PortOwner -Port $OrderPort
}

try {
    Set-Location $ProjectRoot
    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
    Start-Transcript -Path (Join-Path $LogDir "script.log") -Force | Out-Null
    $TranscriptStarted = $true

    if ([string]::IsNullOrWhiteSpace($RedisPassword)) {
        throw "Redis password is required. Set ENJOYTIX_REDIS_PASSWORD in IDEA environment variables or pass -RedisPassword."
    }

    Write-Step "Preparing ports and build artifacts"
    Stop-PortOwner -Port $TicketPort
    Stop-PortOwner -Port $OrderPort

    if (-not $SkipBuild) {
        Invoke-CommandLine -FilePath $MavenExecutable -Arguments @("-pl", "services/ticket-service,services/order-service", "-am", "clean", "install", "-DskipTests")
        Invoke-CommandLine -FilePath $MavenExecutable -Arguments @("-pl", "services/ticket-service", "package", "spring-boot:repackage", "-DskipTests")
        Invoke-CommandLine -FilePath $MavenExecutable -Arguments @("-pl", "services/order-service", "package", "spring-boot:repackage", "-DskipTests")
    }

    Write-Step "Resetting test inventory"
    Invoke-MysqlRows -Sql @"
UPDATE enjoytix_order.et_order
SET status = 'CLOSED', update_time = NOW()
WHERE show_id = $ShowId AND status = 'PENDING_PAYMENT';
UPDATE enjoytix_ticket.et_seat_lock
SET status = 'RELEASED', update_time = NOW()
WHERE show_id = $ShowId AND category_id = $CategoryId AND status = 'LOCKED';
UPDATE enjoytix_ticket.et_ticket_stock
SET locked_stock = 0, sold_stock = 0, update_time = NOW()
WHERE show_id = $ShowId AND category_id = $CategoryId;
UPDATE enjoytix_ticket.et_seat_stock
SET status = 'AVAILABLE', lock_id = NULL, update_time = NOW()
WHERE show_id = $ShowId AND seat_id = $SeatId;
"@ | Out-Null

    $commonJvm = @(
        "-Dspring.profiles.active=mysql",
        "-Dspring.data.redis.password=$RedisPassword",
        "-Dspring.data.redis.port=$RedisPort",
        "-Dunique-name=$UniqueName",
        "-Dframework.cache.redis.prefix=$RedisPrefix",
        "-Dspring.data.redis.host=$RedisHost",
        "-Drocketmq.name-server=$RocketMqNameServer",
        "-Dspring.cloud.nacos.discovery.server-addr=$NacosServer",
        "-Dspring.cloud.nacos.config.server-addr=$NacosServer"
    )
    $ticketJvm = $commonJvm + @(
        "-Denjoytix.lock.type=redisson",
        "-Dticket.lock.ttl-minutes=$TicketLockTtlMinutes"
    )
    $orderJvm = $commonJvm + @(
        "-Drocketmq.producer.group=${UniqueName}_order_service_producer",
        "-Dorder.timeout-message.topic=${UniqueName}_order_timeout",
        "-Dorder.timeout-message.consumer-group=${UniqueName}_order_timeout_close_cg",
        "-Dorder.timeout-message.compensation-fixed-delay-millis=$CompensationDelayMillis",
        "-Dorder.expired-order-scan-fixed-delay-millis=600000"
    )

    Write-Step "Starting ticket-service and order-service"
    $ticketJar = Join-Path $ProjectRoot "services\ticket-service\target\enjoytix-ticket-service-0.1.0-SNAPSHOT.jar"
    $orderJar = Join-Path $ProjectRoot "services\order-service\target\enjoytix-order-service-0.1.0-SNAPSHOT.jar"
    $services += Start-ServiceJar -Name "ticket-service" -JarPath $ticketJar -JvmArguments $ticketJvm
    Start-Sleep -Seconds 8
    $services += Start-ServiceJar -Name "order-service" -JarPath $orderJar -JvmArguments $orderJvm

    Wait-Health -Name "ticket-service" -Port $TicketPort
    Wait-Health -Name "order-service" -Port $OrderPort

    Write-Step "Creating pending order"
    $createBody = @{
        showId = $ShowId
        categoryId = $CategoryId
        quantity = 1
        seatIds = @($SeatId)
    } | ConvertTo-Json
    $createResp = Invoke-RestMethod `
        -Method Post `
        -Uri "http://127.0.0.1:$OrderPort/api/order/create" `
        -Headers @{ "X-User-Id" = [string]$UserId } `
        -ContentType "application/json" `
        -Body $createBody `
        -TimeoutSec 30

    if ($createResp.success -ne $true -or $createResp.code -ne "0") {
        throw "Create order failed: $($createResp | ConvertTo-Json -Depth 10)"
    }

    $orderId = [string]$createResp.data.orderId
    $lockId = [string]$createResp.data.lockId
    Write-Host "Created orderId=$orderId, lockId=$lockId, payExpireTime=$($createResp.data.payExpireTime)"

    Write-Step "Checking pending state"
    Assert-Equal -Name "order status after create" -Actual (Invoke-MysqlSingle "SELECT status FROM enjoytix_order.et_order WHERE id = $orderId;") -Expected "PENDING_PAYMENT"
    Assert-Equal -Name "timeout message status after create" -Actual (Invoke-MysqlSingle "SELECT status FROM enjoytix_order.et_order_timeout_message_log WHERE order_id = $orderId;") -Expected "SENT"
    Assert-Equal -Name "seat lock status after create" -Actual (Invoke-MysqlSingle "SELECT status FROM enjoytix_ticket.et_seat_lock WHERE id = $lockId;") -Expected "LOCKED"
    Assert-Equal -Name "ticket stock after create" -Actual (Invoke-MysqlSingle "SELECT CONCAT(locked_stock, '|', sold_stock) FROM enjoytix_ticket.et_ticket_stock WHERE show_id = $ShowId AND category_id = $CategoryId;") -Expected "1|0"
    Assert-Equal -Name "seat stock after create" -Actual (Invoke-MysqlSingle "SELECT CONCAT(status, '|', IFNULL(lock_id, 'NULL')) FROM enjoytix_ticket.et_seat_stock WHERE show_id = $ShowId AND seat_id = $SeatId;") -Expected "LOCKED|$lockId"

    Write-Step "Waiting for RocketMQ delayed timeout close"
    $deadline = (Get-Date).AddSeconds($WaitTimeoutSeconds)
    $finalSnapshot = $null
    while ((Get-Date) -lt $deadline) {
        $orderStatus = Invoke-MysqlSingle "SELECT status FROM enjoytix_order.et_order WHERE id = $orderId;"
        $messageSnapshot = Invoke-MysqlSingle "SELECT CONCAT(status, '|', retry_count, '|', IFNULL(last_error, 'NULL')) FROM enjoytix_order.et_order_timeout_message_log WHERE order_id = $orderId;"
        $lockStatus = Invoke-MysqlSingle "SELECT status FROM enjoytix_ticket.et_seat_lock WHERE id = $lockId;"
        $stockSnapshot = Invoke-MysqlSingle "SELECT CONCAT(locked_stock, '|', sold_stock) FROM enjoytix_ticket.et_ticket_stock WHERE show_id = $ShowId AND category_id = $CategoryId;"
        $seatSnapshot = Invoke-MysqlSingle "SELECT CONCAT(status, '|', IFNULL(lock_id, 'NULL')) FROM enjoytix_ticket.et_seat_stock WHERE show_id = $ShowId AND seat_id = $SeatId;"
        $finalSnapshot = "order=$orderStatus message=$messageSnapshot lock=$lockStatus stock=$stockSnapshot seat=$seatSnapshot"
        Write-Host ("poll " + (Get-Date -Format "HH:mm:ss") + " " + $finalSnapshot)
        if ($orderStatus -eq "CLOSED" -and
            $messageSnapshot -eq "SUCCESS|0|NULL" -and
            $lockStatus -eq "RELEASED" -and
            $stockSnapshot -eq "0|0" -and
            $seatSnapshot -eq "AVAILABLE|NULL") {
            break
        }
        Start-Sleep -Seconds $PollIntervalSeconds
    }

    if (-not $finalSnapshot -or $finalSnapshot -notlike "order=CLOSED message=SUCCESS|0|NULL lock=RELEASED stock=0|0 seat=AVAILABLE|NULL") {
        throw "Timeout workflow did not reach expected state in $WaitTimeoutSeconds seconds. Last snapshot: $finalSnapshot"
    }

    Write-Step "Checking close reason, idempotency log, and service logs"
    Assert-Equal -Name "close reason" -Actual (Invoke-MysqlSingle "SELECT reason FROM enjoytix_order.et_order_status_log WHERE order_id = $orderId AND to_status = 'CLOSED' ORDER BY create_time DESC LIMIT 1;") -Expected "payment timeout message"
    Assert-Equal -Name "timeout message log count" -Actual (Invoke-MysqlSingle "SELECT COUNT(*) FROM enjoytix_order.et_order_timeout_message_log WHERE order_id = $orderId;") -Expected "1"

    $ticketOut = $services[0].OutLog
    $orderOut = $services[1].OutLog
    Assert-NoLogPattern -Pattern "UnknownHostException|Failed to resolve|Order timeout close skipped|Send order timeout message failed|Consume order timeout message failed|RECONSUME|DLQ" -Files @($ticketOut, $orderOut)

    $redissonConnection = Select-String -Path $ticketOut -Pattern ("connections initialized for " + [regex]::Escape($RedisHost) + "/") -CaseSensitive:$false -ErrorAction SilentlyContinue
    if (-not $redissonConnection) {
        throw "Redisson connection log for Redis host [$RedisHost] was not found."
    }

    Write-Step "Integration passed"
    Write-Host "orderId=$orderId"
    Write-Host "lockId=$lockId"
    Write-Host "logs=$LogDir"
} catch {
    $script:IntegrationFailed = $true
    Write-Host ""
    Write-Host "Integration failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "logs=$LogDir"
} finally {
    try {
        Stop-StartedServices
    } catch {
        Write-Host "Service cleanup failed: $($_.Exception.Message)"
    }
    if ($TranscriptStarted) {
        Stop-Transcript | Out-Null
    }
}

if ($IntegrationFailed) {
    exit 1
}

exit 0
