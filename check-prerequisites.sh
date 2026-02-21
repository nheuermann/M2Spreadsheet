#!/bin/bash

# M2Spreadsheet Prerequisites Checker
# This script verifies that your system has all required tools to build and develop M2Spreadsheet

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}M2Spreadsheet Prerequisites Checker${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

ERRORS=0
WARNINGS=0

# Function to check command existence
check_command() {
    if command -v "$1" &> /dev/null; then
        echo -e "${GREEN}✓${NC} $2 is installed"
        return 0
    else
        echo -e "${RED}✗${NC} $2 is NOT installed"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

# Function to check version
check_version() {
    local cmd=$1
    local version_flag=$2
    local min_version=$3
    local name=$4
    
    if command -v "$cmd" &> /dev/null; then
        version_output=$($cmd $version_flag 2>&1 | head -n 1)
        echo -e "  ${BLUE}→${NC} Installed: $version_output"
        
        # Extract version number (simple approach)
        current_version=$(echo "$version_output" | grep -oE '[0-9]+\.[0-9]+' | head -n 1)
        
        if [ ! -z "$current_version" ] && [ ! -z "$min_version" ]; then
            if [[ "$current_version" < "$min_version" ]]; then
                echo -e "  ${YELLOW}⚠${NC}  Warning: Minimum recommended version is $min_version"
                WARNINGS=$((WARNINGS + 1))
            fi
        fi
    fi
}

# Check Java
echo -e "${BLUE}[1/5] Checking Java...${NC}"
if check_command java "Java"; then
    check_version java -version "17" "Java"
    
    # Check JAVA_HOME
    if [ -z "$JAVA_HOME" ]; then
        echo -e "  ${YELLOW}⚠${NC}  Warning: JAVA_HOME environment variable is not set"
        WARNINGS=$((WARNINGS + 1))
        
        # Try to suggest JAVA_HOME path
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            java_home_path=$(/usr/libexec/java_home 2>/dev/null)
            if [ ! -z "$java_home_path" ]; then
                echo -e "  ${BLUE}→${NC} Suggested JAVA_HOME: $java_home_path"
                echo -e "  ${BLUE}→${NC} To set permanently, add to ~/.zshrc or ~/.bash_profile:"
                echo -e "      export JAVA_HOME=\$(/usr/libexec/java_home)"
            fi
        else
            # Linux/Other
            echo -e "  ${BLUE}→${NC} To find Java installation:"
            echo -e "      which java"
            echo -e "      readlink -f \$(which java)"
            echo -e "  ${BLUE}→${NC} Then add to ~/.bashrc or ~/.profile:"
            echo -e "      export JAVA_HOME=/path/to/java"
        fi
    else
        echo -e "  ${GREEN}✓${NC} JAVA_HOME: $JAVA_HOME"
    fi
else
    echo -e "  ${RED}→${NC} Please install Java 17 or higher"
    echo -e "  ${BLUE}→${NC} Download: https://adoptium.net/"
fi
echo ""

# Check Maven
echo -e "${BLUE}[2/5] Checking Maven...${NC}"
if check_command mvn "Apache Maven"; then
    check_version mvn --version "3.6" "Maven"
else
    echo -e "  ${RED}→${NC} Please install Apache Maven 3.6 or higher"
    echo -e "  ${BLUE}→${NC} macOS: brew install maven"
    echo -e "  ${BLUE}→${NC} Download: https://maven.apache.org/download.cgi"
fi
echo ""

# Check Git
echo -e "${BLUE}[3/5] Checking Git...${NC}"
if check_command git "Git"; then
    check_version git --version "2.0" "Git"
else
    echo -e "  ${RED}→${NC} Please install Git"
    echo -e "  ${BLUE}→${NC} macOS: brew install git"
    echo -e "  ${BLUE}→${NC} Download: https://git-scm.com/downloads"
fi
echo ""

# Check VS Code (optional but recommended)
echo -e "${BLUE}[4/5] Checking VS Code (optional)...${NC}"
if command -v code &> /dev/null; then
    echo -e "${GREEN}✓${NC} VS Code is installed"
    check_version code --version "" "VS Code"
    
    # Check for recommended extensions
    echo -e "  ${BLUE}→${NC} Checking recommended VS Code extensions..."
    
    extensions=(
        "vscjava.vscode-java-pack:Java Extension Pack"
        "vscjava.vscode-maven:Maven for Java"
    )
    
    for ext_info in "${extensions[@]}"; do
        IFS=':' read -r ext_id ext_name <<< "$ext_info"
        if code --list-extensions | grep -qi "^${ext_id}$"; then
            echo -e "    ${GREEN}✓${NC} $ext_name"
        else
            echo -e "    ${YELLOW}○${NC} $ext_name (not installed)"
            echo -e "      Install: code --install-extension $ext_id"
        fi
    done
    
    # Check for GitHub Copilot (optional, check both main and chat)
    if code --list-extensions | grep -qi "github.copilot"; then
        echo -e "    ${GREEN}✓${NC} GitHub Copilot (optional)"
    else
        echo -e "    ${YELLOW}○${NC} GitHub Copilot (optional, not installed)"
    fi
else
    echo -e "${YELLOW}○${NC} VS Code is not installed (optional)"
    echo -e "  ${BLUE}→${NC} Download: https://code.visualstudio.com/"
fi
echo ""

# Check required libraries and tools
echo -e "${BLUE}[5/5] Checking project structure...${NC}"
if [ -f "pom.xml" ]; then
    echo -e "${GREEN}✓${NC} Root pom.xml found"
else
    echo -e "${RED}✗${NC} Root pom.xml not found - are you in the project root?"
    ERRORS=$((ERRORS + 1))
fi

if [ -d "plugins/io.github.nheuermann.m2spreadsheet" ]; then
    echo -e "${GREEN}✓${NC} M2Spreadsheet plugin directory found"
else
    echo -e "${RED}✗${NC} M2Spreadsheet plugin directory not found"
    ERRORS=$((ERRORS + 1))
fi

if [ -d "tests/io.github.nheuermann.m2spreadsheet.tests" ]; then
    echo -e "${GREEN}✓${NC} Test directory found"
else
    echo -e "${RED}✗${NC} Test directory not found"
    ERRORS=$((ERRORS + 1))
fi

if [ -d "libs" ]; then
    echo -e "${GREEN}✓${NC} libs directory found"
    
    # Check for required JARs in libs/
    ACCELEO_JAR="libs/org.eclipse.acceleo.query-7.0.0.jar"
    ANTLR_JAR="libs/antlr4-runtime-4.7.2.jar"
    
    if [ -f "$ACCELEO_JAR" ]; then
        echo -e "  ${GREEN}✓${NC} Acceleo Query JAR found in libs/"
    else
        echo -e "  ${RED}✗${NC} Acceleo Query JAR not found in libs/"
        ERRORS=$((ERRORS + 1))
    fi
    
    if [ -f "$ANTLR_JAR" ]; then
        echo -e "  ${GREEN}✓${NC} ANTLR4 Runtime JAR found in libs/"
    else
        echo -e "  ${RED}✗${NC} ANTLR4 Runtime JAR not found in libs/"
        ERRORS=$((ERRORS + 1))
    fi
    
    # Check if JARs are installed in local Maven repository
    echo -e "  ${BLUE}→${NC} Checking local Maven repository installation..."
    MAVEN_REPO="$HOME/.m2/repository"
    ACCELEO_MAVEN="$MAVEN_REPO/org/eclipse/acceleo/org.eclipse.acceleo.query/7.0.0/org.eclipse.acceleo.query-7.0.0.jar"
    ANTLR_MAVEN="$MAVEN_REPO/org/antlr/antlr4-runtime/4.7.2/antlr4-runtime-4.7.2.jar"
    
    NEED_INSTALL=false
    
    if [ -f "$ACCELEO_MAVEN" ]; then
        echo -e "    ${GREEN}✓${NC} Acceleo Query installed in Maven repository"
    else
        echo -e "    ${YELLOW}○${NC} Acceleo Query NOT installed in Maven repository"
        NEED_INSTALL=true
    fi
    
    if [ -f "$ANTLR_MAVEN" ]; then
        echo -e "    ${GREEN}✓${NC} ANTLR4 Runtime installed in Maven repository"
    else
        echo -e "    ${YELLOW}○${NC} ANTLR4 Runtime NOT installed in Maven repository"
        NEED_INSTALL=true
    fi
    
    if [ "$NEED_INSTALL" = true ]; then
        echo -e "  ${YELLOW}⚠${NC}  Required JARs need to be installed to Maven repository"
        echo -e "  ${BLUE}→${NC} Run: ./setup-local-dependencies.sh"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${RED}✗${NC} libs directory not found"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Summary${NC}"
echo -e "${BLUE}========================================${NC}"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All prerequisites are met!${NC}"
    echo ""
    echo -e "${BLUE}Next steps:${NC}"
    echo -e "  1. Build the project: ./run-test.sh"
    echo -e "  2. Or manually:"
    echo -e "     cd plugins/io.github.nheuermann.m2spreadsheet"
    echo -e "     mvn clean install"
    echo -e "     cd ../../tests/io.github.nheuermann.m2spreadsheet.tests"
    echo -e "     mvn test"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ All required tools are installed (${WARNINGS} warning(s))${NC}"
    echo -e "${YELLOW}You can proceed, but consider addressing the warnings above.${NC}"
    exit 0
else
    echo -e "${RED}✗ Found ${ERRORS} error(s) and ${WARNINGS} warning(s)${NC}"
    echo -e "${RED}Please install the missing prerequisites before continuing.${NC}"
    exit 1
fi
