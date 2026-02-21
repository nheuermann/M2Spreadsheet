# M2Spreadsheet Local Dependencies Setup
# This script installs required JAR files from libs/ into your local Maven repository

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Blue
Write-Host "M2Spreadsheet Dependency Setup" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# Check if libs directory exists
if (-not (Test-Path "libs")) {
    Write-Host "✗ Error: libs/ directory not found" -ForegroundColor Red
    Write-Host "  Are you running this from the project root?" -ForegroundColor Red
    exit 1
}

# Check if Maven is installed
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "✗ Error: Maven is not installed" -ForegroundColor Red
    Write-Host "  Please install Maven first: https://maven.apache.org/download.cgi" -ForegroundColor Red
    exit 1
}

Write-Host "Installing local dependencies to Maven repository..." -ForegroundColor Blue
Write-Host ""

# Get absolute path to project root
$ProjectRoot = (Get-Location).Path

# Change to temp directory to avoid project POM issues
Set-Location -Path $env:TEMP

# Install Acceleo Query JAR
$AcceleoJar = Join-Path $ProjectRoot "libs\org.eclipse.acceleo.query-7.0.0.jar"
if (Test-Path $AcceleoJar) {
    Write-Host "[1/2] Installing Acceleo Query Language..." -ForegroundColor Blue
    mvn install:install-file `
        -Dfile="$AcceleoJar" `
        -DgroupId=org.eclipse.acceleo `
        -DartifactId=org.eclipse.acceleo.query `
        -Dversion=7.0.0 `
        -Dpackaging=jar `
        -q
    Write-Host "✓ Acceleo Query installed" -ForegroundColor Green
} else {
    Write-Host "✗ Error: $AcceleoJar not found" -ForegroundColor Red
    exit 1
}

# Install ANTLR4 Runtime JAR
$AntlrJar = Join-Path $ProjectRoot "libs\antlr4-runtime-4.7.2.jar"
if (Test-Path $AntlrJar) {
    Write-Host "[2/2] Installing ANTLR4 Runtime..." -ForegroundColor Blue
    mvn install:install-file `
        -Dfile="$AntlrJar" `
        -DgroupId=org.antlr `
        -DartifactId=antlr4-runtime `
        -Dversion=4.7.2 `
        -Dpackaging=jar `
        -q
    Write-Host "✓ ANTLR4 Runtime installed" -ForegroundColor Green
} else {
    Write-Host "✗ Error: $AntlrJar not found" -ForegroundColor Red
    exit 1
}

# Return to project directory
Set-Location -Path $ProjectRoot

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✓ Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Dependencies installed to: $env:USERPROFILE\.m2\repository" -ForegroundColor Blue
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Blue
Write-Host "  1. Build the plugin:"
Write-Host "     cd plugins\io.github.nheuermann.m2spreadsheet"
Write-Host "     mvn clean install -DskipTests"
Write-Host ""
Write-Host "  2. Run tests:"
Write-Host "     cd tests\io.github.nheuermann.m2spreadsheet.tests"
Write-Host "     mvn test -Dtest=FolderBasedTemplatesTest"
Write-Host ""

exit 0
