
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot ".env"
$secretsDir = Join-Path $PSScriptRoot "secrets"
$truststorePath = Join-Path $secretsDir "kafka.truststore.jks"

$containerName = "skating-kafka"
$bootstrapServer = "localhost:9092"

$requiredTopics = @(
    "payments.requested",
    "payments.completed",
    "payments.failed"
)

function Read-DotEnvFile {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw ".env file not found: $Path"
    }

    $values = @{}

    foreach ($rawLine in Get-Content -LiteralPath $Path) {
        $line = $rawLine.Trim()

        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
            continue
        }

        $separator = $line.IndexOf("=")

        if ($separator -lt 1) {
            continue
        }

        $key = $line.Substring(0, $separator).Trim()
        $value = $line.Substring($separator + 1).Trim()

        if ($value.Length -ge 2) {
            $first = $value[0]
            $last = $value[$value.Length - 1]

            if (($first -eq '"' -and $last -eq '"') -or
                ($first -eq "'" -and $last -eq "'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }

        $values[$key] = $value
    }

    return $values
}

function Escape-JaasValue {
    param([string]$Value)

    return $Value.Replace('\', '\\').Replace('"', '\"')
}

function Escape-PropertiesValue {
    param([string]$Value)

    return $Value.Replace('\', '\\')
}

Write-Host "`n=== Kafka topic initialization ===" -ForegroundColor Cyan

# 1. Read environment variables

$envValues = Read-DotEnvFile -Path $envPath

$requiredVariables = @(
    "KAFKA_APP_USERNAME",
    "KAFKA_APP_PASSWORD",
    "KAFKA_SSL_STORE_PASSWORD"
)

foreach ($variable in $requiredVariables) {
    if (-not $envValues.ContainsKey($variable) -or
        [string]::IsNullOrWhiteSpace($envValues[$variable])) {
        throw "Missing required variable in .env: $variable"
    }
}

if (-not (Test-Path -LiteralPath $truststorePath)) {
    throw "Kafka truststore not found: $truststorePath"
}

$username = $envValues["KAFKA_APP_USERNAME"]
$password = $envValues["KAFKA_APP_PASSWORD"]
$truststorePassword = $envValues["KAFKA_SSL_STORE_PASSWORD"]

# 2. Check Docker and Kafka container

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker CLI is not available."
}

$containerStatus = & docker inspect --format "{{.State.Status}}" $containerName 2>$null

if ($LASTEXITCODE -ne 0 -or "$containerStatus".Trim() -ne "running") {
    throw "Kafka container is not running. Start it with: docker compose up -d kafka"
}

Write-Host "[OK] Kafka container is running." -ForegroundColor Green

# 3. Generate temporary Kafka client configuration

$configFileName = "kafka-init-client-$PID.properties"
$configHostPath = Join-Path $secretsDir $configFileName
$configContainerPath = "/etc/kafka/secrets/$configFileName"

$jaasConfig = 'org.apache.kafka.common.security.scram.ScramLoginModule required username="' +
    (Escape-JaasValue $username) +
    '" password="' +
    (Escape-JaasValue $password) +
    '";'

$properties = @(
    "security.protocol=SASL_SSL",
    "sasl.mechanism=SCRAM-SHA-512",
    "sasl.jaas.config=$(Escape-PropertiesValue $jaasConfig)",
    "ssl.truststore.location=/etc/kafka/secrets/kafka.truststore.jks",
    "ssl.truststore.password=$(Escape-PropertiesValue $truststorePassword)",
    "ssl.truststore.type=JKS"
)

$utf8 = [System.Text.UTF8Encoding]::new($false)

try {
    [System.IO.File]::WriteAllLines(
        $configHostPath,
        $properties,
        $utf8
    )

    $baseArgs = @(
        "exec",
        $containerName,
        "/opt/kafka/bin/kafka-topics.sh",
        "--bootstrap-server",
        $bootstrapServer,
        "--command-config",
        $configContainerPath
    )

    # 4. Wait for Kafka and verify authentication

    Write-Host "[INFO] Connecting to Kafka..." -ForegroundColor Yellow

    $connected = $false
    $lastError = $null

    for ($attempt = 1; $attempt -le 15; $attempt++) {

        $listArgs = $baseArgs + @("--list")
        $output = & docker @listArgs 2>&1
        $exitCode = $LASTEXITCODE

        if ($exitCode -eq 0) {
            $existingTopics = @(
                $output |
                    ForEach-Object { "$_".Trim() } |
                    Where-Object { $_ -ne "" }
            )

            $connected = $true
            break
        }

        $lastError = $output -join "`n"

        if ($lastError -match "SaslAuthenticationException|Authentication failed|SSLHandshakeException") {
            break
        }

        Start-Sleep -Seconds 2
    }

    if (-not $connected) {
        Write-Host $lastError -ForegroundColor Red
        throw "Cannot connect to Kafka. Check SCRAM credentials and truststore."
    }

    Write-Host "[OK] Kafka connection established." -ForegroundColor Green

    # 5. Create missing topics

    foreach ($topic in $requiredTopics) {

        if ($existingTopics -contains $topic) {
            Write-Host "[EXISTS] $topic" -ForegroundColor DarkYellow
            continue
        }

        $createArgs = $baseArgs + @(
            "--create",
            "--if-not-exists",
            "--topic", $topic,
            "--partitions", "1",
            "--replication-factor", "1"
        )

        $createOutput = & docker @createArgs 2>&1

        if ($LASTEXITCODE -ne 0) {
            Write-Host ($createOutput -join "`n") -ForegroundColor Red
            throw "Failed to create topic: $topic"
        }

        Write-Host "[CREATED] $topic" -ForegroundColor Green
    }

    # 6. Display final topic list

    Write-Host "`n=== Current Kafka topics ===" -ForegroundColor Cyan

    $finalArgs = $baseArgs + @("--list")
    $finalTopics = & docker @finalArgs

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to retrieve final topic list."
    }

    $finalTopics | ForEach-Object {
        Write-Host "  $_"
    }

    Write-Host "`n[OK] Kafka initialization completed." -ForegroundColor Green
}
finally {

    # Remove temporary credentials file

    if (Test-Path -LiteralPath $configHostPath) {
        Remove-Item -LiteralPath $configHostPath -Force
    }
}
