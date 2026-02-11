# M2Doc Standalone Tests

This module tests M2Doc functionality in standalone mode (without OSGi/Eclipse runtime).
The purpose is to validate that M2Doc works in a plain Maven environment before building M2Spreadsheet.

## Purpose

- ✅ Prove M2Doc can run without OSGi
- ✅ Validate test setup for VS Code development
- ✅ Serve as baseline before M2Spreadsheet development

## Running Tests

### Using Maven
```bash
cd tests/org.obeonetwork.m2doc.standalone.tests
mvn clean test
```

### Using VS Code
1. Open `SimpleM2DocStandaloneTest.java`
2. Click "Run Test" button above the test method
3. View results in Test Explorer

## Current Tests

### `testM2DocAPIAccessible()`
- Verifies M2Doc classes can be loaded
- Initializes query environment
- Tests service registration

### `testSimpleDocumentGeneration()` (Commented Out)
- Will test full document generation
- Requires template file: `src/test/resources/simple-template.docx`
- Uncomment when template is available

## Dependencies

This module depends on:
- M2Doc plugin (from workspace) - **requires building M2Doc first**
- Apache POI (via Maven)
- EMF (via Maven)
- Acceleo Query (via Maven)

## Building M2Doc Plugin First

Before running these tests, you need to build the M2Doc plugin:

```bash
# From project root
cd plugins/org.obeonetwork.m2doc
mvn clean install -DskipTests
```

This creates the JAR that the standalone tests depend on.

## Next Steps

Once this baseline works:
1. ✅ Create M2Spreadsheet plugin
2. ✅ Create M2Spreadsheet tests
3. ⏭️ Port M2Doc patterns to spreadsheets
