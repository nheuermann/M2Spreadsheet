# M2Spreadsheet Folder-Based Test Cases

This directory contains folder-based test cases for M2Spreadsheet template generation.

## Structure

Each test case is a subfolder containing:

- **`{name}-template.xlsx`** (required): The template spreadsheet with M2Spreadsheet expressions
- **`{name}-expected.xlsx`** (optional): The expected output after generation
- **`{name}-generated.xlsx`** (created by test): The actual generated output
- **`{name}-diff.xlsx`** (created if mismatch): Differences between expected and generated

## Running Tests

```bash
mvn test -Dtest=FolderBasedTemplatesTest
```

## Creating New Test Cases

1. Create a new subfolder: `mkdir src/test/resources/cases/my-test`
2. Create template: `my-test/my-test-template.xlsx`
3. (Optional) Run generation manually and save as: `my-test/my-test-expected.xlsx`
4. Run tests: The framework will auto-discover the new test case

## Available Variables

Templates can use these variables (defined in FolderBasedTemplatesTest):

- **Simple data**: `items` (list of strings), `numbers` (list of integers), `title`, `author`
- **Project data**: `projectName`, `projectCode`, `safetyStandard`, `asil`
- **Mock model data**: `requirements`, `faultTreeEvents`, `referenceDocuments`, `fmeaEntries`
- **Nested data**: `categories` (list with category name and items)

## Example Test Cases

### simple-list
Tests basic `m:for_row` loop with row repetition.

### within-cell-loop
Tests within-cell `m:for` / `m:endfor` loops.

## Debugging Tips

- Check generated files manually in Excel/LibreOffice
- Review diff files to understand mismatches
- Look at test output for validation messages
- Set breakpoints in AbstractSpreadsheetsTestSuite.generation()
