#!/bin/bash

# M2Spreadsheet Local Dependencies Setup
# This script installs required JAR files from libs/ into your local Maven repository

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}M2Spreadsheet Dependency Setup${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if libs directory exists
if [ ! -d "libs" ]; then
    echo -e "${RED}✗ Error: libs/ directory not found${NC}"
    echo -e "${RED}  Are you running this from the project root?${NC}"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Error: Maven is not installed${NC}"
    echo -e "${RED}  Please install Maven first: https://maven.apache.org/download.cgi${NC}"
    exit 1
fi

echo -e "${BLUE}Installing local dependencies to Maven repository...${NC}"
echo ""

# Get absolute path to project root
PROJECT_ROOT="$(pwd)"

# Change to temp directory to avoid project POM issues
cd /tmp

# Install Acceleo Query JAR
ACCELEO_JAR="$PROJECT_ROOT/libs/org.eclipse.acceleo.query-7.0.0.jar"
if [ -f "$ACCELEO_JAR" ]; then
    echo -e "${BLUE}[1/2] Installing Acceleo Query Language...${NC}"
    mvn install:install-file \
        -Dfile="$ACCELEO_JAR" \
        -DgroupId=org.eclipse.acceleo \
        -DartifactId=org.eclipse.acceleo.query \
        -Dversion=7.0.0 \
        -Dpackaging=jar \
        -q
    echo -e "${GREEN}✓ Acceleo Query installed${NC}"
else
    echo -e "${RED}✗ Error: $ACCELEO_JAR not found${NC}"
    exit 1
fi

# Install ANTLR4 Runtime JAR
ANTLR_JAR="$PROJECT_ROOT/libs/antlr4-runtime-4.7.2.jar"
if [ -f "$ANTLR_JAR" ]; then
    echo -e "${BLUE}[2/2] Installing ANTLR4 Runtime...${NC}"
    mvn install:install-file \
        -Dfile="$ANTLR_JAR" \
        -DgroupId=org.antlr \
        -DartifactId=antlr4-runtime \
        -Dversion=4.7.2 \
        -Dpackaging=jar \
        -q
    echo -e "${GREEN}✓ ANTLR4 Runtime installed${NC}"
else
    echo -e "${RED}✗ Error: $ANTLR_JAR not found${NC}"
    exit 1
fi

# Return to project directory
cd "$PROJECT_ROOT"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✓ Setup Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Dependencies installed to: $HOME/.m2/repository${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "  1. Build the plugin:"
echo -e "     cd plugins/io.github.nheuermann.m2spreadsheet"
echo -e "     mvn clean install -DskipTests"
echo -e ""
echo -e "  2. Run tests:"
echo -e "     ./run-test.sh FolderBasedTemplatesTest"
echo ""

exit 0
