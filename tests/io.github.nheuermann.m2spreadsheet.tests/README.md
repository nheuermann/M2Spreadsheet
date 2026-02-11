# M2Spreadsheet Test Module

This module contains standalone tests for M2Spreadsheet that can be run in VS Code without OSGi/Eclipse runtime dependencies.

## Running Tests

### Using Maven
```bash
cd tests/io.github.nheuermann.m2spreadsheet.tests
mvn clean test
```

### Using VS Code
1. Open a test file (e.g., `SimpleM2SpreadsheetTest.java`)
2. Click the "Run Test" or "Debug Test" button above the test method
3. View results in the Test Explorer

## Test Output

Generated spreadsheets are written to `target/test-output/`:
- `simple-generated.xlsx` - Basic template with customer name/company
- `multiple-cells.xlsx` - Multiple cells with individual expressions
- `from-file-generated.xlsx` - Generated from template file (if exists)

You can open these files in Excel to verify the generation worked correctly.

## Current Features Tested

✅ Basic expression evaluation: `{m:customer.name}`
✅ Property navigation: `{m:customer.company}`
✅ Multiple expressions in one cell: `"Name: {m:x} from {m:y}"`
✅ Multiple cells with expressions
✅ Template created programmatically (no file needed)

## Next Steps

- [ ] Add proper AQL evaluation (currently uses simple string lookup)
- [ ] Add row iteration support
- [ ] Add conditional sections
- [ ] Add cell styling preservation
- [ ] Add formula generation
- [ ] Add services for common operations
