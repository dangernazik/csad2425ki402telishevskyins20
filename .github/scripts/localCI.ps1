param(
    [string]$ComPort = "COM3",
    [int]$BaudRate = 9600
)

$ErrorActionPreference = "Stop"
Write-Host "Starting CI process for Java and Arduino..."

# Визначаємо кореневий шлях проекту
if ($env:GITHUB_WORKSPACE) {
    # Якщо скрипт виконується на GitHub Actions
    $ROOT_PROJECT_PATH = $env:GITHUB_WORKSPACE
} else {
    # Локально піднімаємось на два рівні вище
    $ROOT_PROJECT_PATH = (Get-Location).Path | Split-Path -Parent | Split-Path -Parent
}

$CLIENT_PROJECT_PATH = Join-Path -Path $ROOT_PROJECT_PATH -ChildPath "client\RPS-client"
$SERVER_PROJECT_PATH = Join-Path -Path $ROOT_PROJECT_PATH -ChildPath "server\RPS-server"
$DEPLOY_DIR = Join-Path -Path $ROOT_PROJECT_PATH -ChildPath "deploy"

if (-Not (Test-Path $DEPLOY_DIR)) {
    New-Item -ItemType Directory -Path $DEPLOY_DIR
}

Write-Host "Current directory: $(Get-Location)"
Write-Host "Deploy directory exists: $(Test-Path $DEPLOY_DIR)"
Write-Host "=== Client Application Build and Test ==="

Set-Location $CLIENT_PROJECT_PATH
Write-Host "Compiling Client project..."
& mvn clean compile -q

Write-Host "Running tests..."
& mvn test

$TEST_RESULTS_DIR = "target\surefire-reports"
Write-Host "Copying test results to $DEPLOY_DIR\test-results..."
Copy-Item -Path $TEST_RESULTS_DIR -Destination "$DEPLOY_DIR\tests" -Recurse -Force

Write-Host "Packaging JAR..."
& mvn package -q -B

Write-Host "Copying JAR artifact to $DEPLOY_DIR..."
Copy-Item -Path "target\*.jar" -Destination $DEPLOY_DIR

Set-Location -Path "..\.."

Write-Host "=== Doxygen documentation generation ==="
Write-Host "Generating documentation..."
Write-Host "Checking if doxygen is installed..."
if (-not (Get-Command doxygen -ErrorAction SilentlyContinue)) {
    Write-Host "Doxygen is not installed. Installing..."
    # Наприклад, для Windows, завантаження і встановлення doxygen:
    Invoke-WebRequest -Uri https://doxygen.nl/files/doxygen-1.9.3.windows.x86_64.bin.zip -OutFile "doxygen.zip"
    Expand-Archive -Path "doxygen.zip" -DestinationPath "doxygen"
    $env:PATH += ";$PWD\doxygen\bin"
    Write-Host "Doxygen installed and added to PATH."
} else {
    Write-Host "Doxygen is already installed."
}
doxygen Doxyfile

Set-Location $ROOT_PROJECT_PATH
Write-Host "=== Server Application Build ==="
Write-Host "Compiling Arduino project..."
& arduino-cli compile --fqbn arduino:avr:uno "$SERVER_PROJECT_PATH\RPS-server.ino"

if ($ComPort -ne "") {
    Write-Host "Uploading program to the board on port $ComPort with baud rate $BaudRate..."
    & arduino-cli upload -p $ComPort --fqbn arduino:avr:uno "$SERVER_PROJECT_PATH\RPS-server.ino"
} else {
    Write-Host "Skipping Arduino upload as COM port is not specified (GitHub CI environment)"
}

Write-Host "CI completed successfully."