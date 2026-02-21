# M2Spreadsheet Prerequisites Checker (Windows PowerShell)
# This script verifies that your system has all required tools to build and develop M2Spreadsheet
#
# Usage: .\check-prerequisites.ps1
#
# Note: If you get an execution policy error, run PowerShell as Administrator and execute:
#   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
# Or run with: powershell -ExecutionPolicy Bypass -File .\check-prerequisites.ps1

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Blue
Write-Host "M2Spreadsheet Prerequisites Checker" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

$script:Errors = 0
$script:Warnings = 0

# Function to check command existence
function Test-Command {
    param(
        [string]$Command,
        [string]$DisplayName
    )
    
    $exists = Get-Command $Command -ErrorAction SilentlyContinue
    if ($exists) {
        Write-Host "✓ $DisplayName is installed" -ForegroundColor Green
        return $true
    } else {
        Write-Host "✗ $DisplayName is NOT installed" -ForegroundColor Red
        $script:Errors++
        return $false
    }
}

# Function to check version
function Get-VersionInfo {
    param(
        [string]$Command,
        [string]$VersionFlag,
        [string]$MinVersion,
        [string]$DisplayName
    )
    
    if (Get-Command $Command -ErrorAction SilentlyContinue) {
        try {
            $versionOutput = & $Command $VersionFlag 2>&1 | Select-Object -First 1
            Write-Host "  → Installed: $versionOutput" -ForegroundColor Blue
        } catch {
            Write-Host "  → Could not determine version" -ForegroundColor Yellow
        }
    }
}

# Check Java
Write-Host "[1/5] Checking Java..." -ForegroundColor Blue
if (Test-Command "java" "Java") {
    Get-VersionInfo "java" "-version" "17" "Java"
    
    # Check JAVA_HOME
    $javaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
    if (-not $javaHome) {
        $javaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
    }
    
    if (-not $javaHome) {
        Write-Host "  ⚠  Warning: JAVA_HOME environment variable is not set" -ForegroundColor Yellow
        $script:Warnings++
        
        # Try to suggest JAVA_HOME path
        $javaPath = (Get-Command java -ErrorAction SilentlyContinue).Source
        if ($javaPath) {
            $javaDir = Split-Path (Split-Path $javaPath -Parent) -Parent
            Write-Host "  → Suggested JAVA_HOME: $javaDir" -ForegroundColor Blue
            Write-Host "  → To set permanently (run as Administrator):" -ForegroundColor Blue
            Write-Host "      [System.Environment]::SetEnvironmentVariable('JAVA_HOME', '$javaDir', 'Machine')" -ForegroundColor Blue
            Write-Host "  → Or set for current user:" -ForegroundColor Blue
            Write-Host "      [System.Environment]::SetEnvironmentVariable('JAVA_HOME', '$javaDir', 'User')" -ForegroundColor Blue
        }
    } else {
        Write-Host "  ✓ JAVA_HOME: $javaHome" -ForegroundColor Green
    }
} else {
    Write-Host "  → Please install Java 17 or higher" -ForegroundColor Red
    Write-Host "  → Download: https://adoptium.net/" -ForegroundColor Blue
}
Write-Host ""

# Check Maven
Write-Host "[2/5] Checking Maven..." -ForegroundColor Blue
if (Test-Command "mvn" "Apache Maven") {
    Get-VersionInfo "mvn" "--version" "3.6" "Maven"
} else {
    Write-Host "  → Please install Apache Maven 3.6 or higher" -ForegroundColor Red
    Write-Host "  → Download: https://maven.apache.org/download.cgi" -ForegroundColor Blue
    Write-Host "  → Or use Chocolatey: choco install maven" -ForegroundColor Blue
}
Write-Host ""

# Check Git
Write-Host "[3/5] Checking Git..." -ForegroundColor Blue
if (Test-Command "git" "Git") {
    Get-VersionInfo "git" "--version" "2.0" "Git"
} else {
    Write-Host "  → Please install Git" -ForegroundColor Red
    Write-Host "  → Download: https://git-scm.com/downloads" -ForegroundColor Blue
    Write-Host "  → Or use Chocolatey: choco install git" -ForegroundColor Blue
}
Write-Host ""

# Check VS Code (optional but recommended)
Write-Host "[4/5] Checking VS Code (optional)..." -ForegroundColor Blue
if (Get-Command "code" -ErrorAction SilentlyContinue) {
    Write-Host "✓ VS Code is installed" -ForegroundColor Green
    Get-VersionInfo "code" "--version" "" "VS Code"
    
    # Check for recommended extensions
    Write-Host "  → Checking recommended VS Code extensions..." -ForegroundColor Blue
    
    $extensions = @{
        "vscjava.vscode-java-pack" = "Java Extension Pack"
        "vscjava.vscode-maven" = "Maven for Java"
    }
    
    foreach ($ext in $extensions.GetEnumerator()) {
        $installed = & code --list-extensions 2>&1 | Select-String -Pattern "^$($ext.Key)$" -Quiet
        if ($installed) {
            Write-Host "    ✓ $($ext.Value)" -ForegroundColor Green
        } else {
            Write-Host "    ○ $($ext.Value) (not installed)" -ForegroundColor Yellow
            Write-Host "      Install: code --install-extension $($ext.Key)" -ForegroundColor Blue
        }
    }
    
    # Check for GitHub Copilot (optional, check both main and chat)
    $copilotInstalled = & code --list-extensions 2>&1 | Select-String -Pattern "github.copilot" -Quiet
    if ($copilotInstalled) {
        Write-Host "    ✓ GitHub Copilot (optional)" -ForegroundColor Green
    } else {
        Write-Host "    ○ GitHub Copilot (optional, not installed)" -ForegroundColor Yellow
    }
} else {
    Write-Host "○ VS Code is not installed (optional)" -ForegroundColor Yellow
    Write-Host "  → Download: https://code.visualstudio.com/" -ForegroundColor Blue
}
Write-Host ""

# Check required libraries and tools
Write-Host "[5/5] Checking project structure..." -ForegroundColor Blue

if (Test-Path "pom.xml") {
    Write-Host "✓ Root pom.xml found" -ForegroundColor Green
} else {
    Write-Host "✗ Root pom.xml not found - are you in the project root?" -ForegroundColor Red
    $script:Errors++
}

if (Test-Path "plugins\io.github.nheuermann.m2spreadsheet") {
    Write-Host "✓ M2Spreadsheet plugin directory found" -ForegroundColor Green
} else {
    Write-Host "✗ M2Spreadsheet plugin directory not found" -ForegroundColor Red
    $script:Errors++
}

if (Test-Path "tests\io.github.nheuermann.m2spreadsheet.tests") {
    Write-Host "✓ Test directory found" -ForegroundColor Green
} else {
    Write-Host "✗ Test directory not found" -ForegroundColor Red
    $script:Errors++
}

if (Test-Path "libs") {
    Write-Host "✓ libs directory found" -ForegroundColor Green
    
    # Check for required JARs in libs/
    $AcceleoJar = "libs\org.eclipse.acceleo.query-7.0.0.jar"
    $AntlrJar = "libs\antlr4-runtime-4.7.2.jar"
    
    if (Test-Path $AcceleoJar) {
        Write-Host "  ✓ Acceleo Query JAR found in libs\" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Acceleo Query JAR not found in libs\" -ForegroundColor Red
        $script:Errors++
    }
    
    if (Test-Path $AntlrJar) {
        Write-Host "  ✓ ANTLR4 Runtime JAR found in libs\" -ForegroundColor Green
    } else {
        Write-Host "  ✗ ANTLR4 Runtime JAR not found in libs\" -ForegroundColor Red
        $script:Errors++
    }
    
    # Check if JARs are installed in local Maven repository
    Write-Host "  → Checking local Maven repository installation..." -ForegroundColor Blue
    $MavenRepo = Join-Path $env:USERPROFILE ".m2\repository"
    $AcceleoMaven = Join-Path $MavenRepo "org\eclipse\acceleo\org.eclipse.acceleo.query\7.0.0\org.eclipse.acceleo.query-7.0.0.jar"
    $AntlrMaven = Join-Path $MavenRepo "org\antlr\antlr4-runtime\4.7.2\antlr4-runtime-4.7.2.jar"
    
    $NeedInstall = $false
    
    if (Test-Path $AcceleoMaven) {
        Write-Host "    ✓ Acceleo Query installed in Maven repository" -ForegroundColor Green
    } else {
        Write-Host "    ○ Acceleo Query NOT installed in Maven repository" -ForegroundColor Yellow
        $NeedInstall = $true
    }
    
    if (Test-Path $AntlrMaven) {
        Write-Host "    ✓ ANTLR4 Runtime installed in Maven repository" -ForegroundColor Green
    } else {
        Write-Host "    ○ ANTLR4 Runtime NOT installed in Maven repository" -ForegroundColor Yellow
        $NeedInstall = $true
    }
    
    if ($NeedInstall) {
        Write-Host "  ⚠  Required JARs need to be installed to Maven repository" -ForegroundColor Yellow
        Write-Host "  → Run: .\setup-local-dependencies.ps1" -ForegroundColor Blue
        $script:Errors++
    }
} else {
    Write-Host "✗ libs directory not found" -ForegroundColor Red
    $script:Errors++
}
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Blue
Write-Host "Summary" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue

if ($script:Errors -eq 0 -and $script:Warnings -eq 0) {
    Write-Host "✓ All prerequisites are met!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Blue
    Write-Host "  1. Build the project: .\run-test.ps1 (if available)" -ForegroundColor Blue
    Write-Host "  2. Or manually:" -ForegroundColor Blue
    Write-Host "     cd plugins\io.github.nheuermann.m2spreadsheet" -ForegroundColor Blue
    Write-Host "     mvn clean install" -ForegroundColor Blue
    Write-Host "     cd ..\..\tests\io.github.nheuermann.m2spreadsheet.tests" -ForegroundColor Blue
    Write-Host "     mvn test" -ForegroundColor Blue
    exit 0
} elseif ($script:Errors -eq 0) {
    Write-Host "⚠ All required tools are installed ($script:Warnings warning(s))" -ForegroundColor Yellow
    Write-Host "You can proceed, but consider addressing the warnings above." -ForegroundColor Yellow
    exit 0
} else {
    Write-Host "✗ Found $script:Errors error(s) and $script:Warnings warning(s)" -ForegroundColor Red
    Write-Host "Please install the missing prerequisites before continuing." -ForegroundColor Red
    exit 1
}
