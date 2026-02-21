#!/bin/bash
# M2Spreadsheet Test Runner
# Usage: ./run-test.sh [testClass]
# Example: ./run-test.sh FolderBasedTemplatesTest
# Or: ./run-test.sh (runs all tests)
# Add -d for debug output: ./run-test.sh FolderBasedTemplatesTest -d

cd "$(dirname "$0")/tests/io.github.nheuermann.m2spreadsheet.tests"

# Color codes
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

DEBUG_FLAG=""
TEST_NAME=""

# Parse arguments
for arg in "$@"; do
    if [ "$arg" = "-d" ] || [ "$arg" = "--debug" ]; then
        DEBUG_FLAG="-Dtest.debug=true"
    else
        TEST_NAME="$arg"
    fi
done

# Filter function to remove unwanted Maven error messages
filter_output() {
    grep -v "Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin" | \
    grep -v "Please refer to.*surefire-reports" | \
    grep -v "Please refer to dump files" | \
    grep -v "\[Help 1\]" | \
    grep -v "To see the full stack trace" | \
    grep -v "Re-run Maven using the -X switch" | \
    grep -v "For more information about the errors" | \
    grep -v "http://cwiki.apache.org/confluence" | \
    grep -v "^\[ERROR\] Failures:" | \
    grep -v "^\[ERROR\]   FolderBased" | \
    grep -v "^\[ERROR\]   .*AbstractSpreadsheetsTestSuite\.generation" | \
    grep -v "^Generated output does not match expected" | \
    sed '/^\[ERROR\]$/d' | \
    sed '/^\[INFO\] Results:$/,/^\[INFO\] BUILD FAILURE$/d'
}

if [ -z "$TEST_NAME" ]; then
    echo "🧪 Running all M2Spreadsheet tests..."
    OUTPUT=$(mvn test $DEBUG_FLAG 2>&1 | filter_output | awk 'NF || !seen {seen = !NF; if (NF) print}')
else
    echo "🧪 Running test: $TEST_NAME"
    OUTPUT=$(mvn test -Dtest=$TEST_NAME $DEBUG_FLAG 2>&1 | filter_output | awk 'NF || !seen {seen = !NF; if (NF) print}')
fi

# Print output up to and including BUILD FAILURE or BUILD SUCCESS
# Colorize ERROR and WARNING messages
echo "$OUTPUT" | sed '/\[INFO\] BUILD FAILURE/q' | sed '/\[INFO\] BUILD SUCCESS/q' | \
    sed $'s/ERROR/\033[0;31m&\033[0m/g' | \
    sed $'s/WARNING/\033[1;33m&\033[0m/g'

# Check if tests passed or failed
if echo "$OUTPUT" | grep -q "\[INFO\] BUILD SUCCESS"; then
    echo ""
    echo "✅ All tests passed!"
else
    echo ""
    echo "❌ Tests failed"
    exit 1
fi
