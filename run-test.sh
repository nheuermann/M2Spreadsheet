#!/bin/bash
# M2Spreadsheet Test Runner
# Usage: ./run-test.sh [testMethod]
# Example: ./run-test.sh testSimpleCustomerTemplate
# Or: ./run-test.sh (runs all tests)

cd "$(dirname "$0")/tests/io.github.nheuermann.m2spreadsheet.tests"

if [ -z "$1" ]; then
    echo "🧪 Running all M2Spreadsheet tests..."
    mvn test
else
    echo "🧪 Running test: $1"
    mvn test -Dtest=SimpleM2SpreadsheetTest#$1
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Tests passed!"
    echo "📁 Check output: tests/io.github.nheuermann.m2spreadsheet.tests/target/test-output/"
    ls -lh tests/io.github.nheuermann.m2spreadsheet.tests/target/test-output/ 2>/dev/null | tail -n +2
else
    echo ""
    echo "❌ Tests failed"
    exit 1
fi
