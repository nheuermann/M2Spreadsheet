# M2Spreadsheet Development - GitHub Copilot Instructions

## Project Context

This is **M2Spreadsheet**, a document generation tool for creating .xlsx files from EMF model data, derived from M2Doc. M2Doc generates .docx files using Apache POI (XWPF), and we're creating a similar tool for spreadsheets using POI XSSF.

### Core Concepts

- **M2Doc (ancestor)**: Generates Word .docx files with template syntax like `{m:expression}` in Word runs
- **M2Spreadsheet (this project)**: Will generate Excel .xlsx files with template syntax like `{m:expression}` in spreadsheet cells
- **Architecture**: Parse → Validate → Generate
- **Key Components**:
  - Parser: Reads template and creates AST
  - Evaluator: Executes AQL expressions and generates output
  - Services: Extensible functions available in templates
  - Query Environment: Acceleo Query Language (AQL) runtime

### Technology Stack

- **Java 17+** (as per existing M2Doc setup)
- **Apache POI**: Document processing
  - M2Doc uses: `org.apache.poi.xwpf` (Word)
  - M2Spreadsheet should use: `org.apache.poi.xssf` (Excel)
- **Eclipse Acceleo Query (AQL)**: Expression language
- **EMF (Eclipse Modeling Framework)**: Model handling
- **Maven/Tycho**: Build system (currently)
- **JUnit**: Testing framework

### Development Approach

We're working in **VS Code** with a focus on:
1. **Standalone testing** without Eclipse runtime (OSGi can be removed for tests)
2. **Incremental development** based on M2Doc patterns
3. **Mock input data** for testing (focus on rendering engine and output generation)
4. **Excel-based verification** of outputs

### Critical Development Workflow Rules

⚠️ **ALWAYS COMPILE AFTER CODE CHANGES** ⚠️

During testing and debugging, **always recompile** after making code changes before running tests again. Common mistake pattern:
1. Make code change
2. Run test → see unexpected output
3. Make another code change
4. Run test → still see same output (because previous change not compiled!)
5. Get confused and make more changes...

**Correct workflow:**
```bash
# After ANY code change to plugins:
cd plugins/io.github.nheuermann.m2spreadsheet
mvn clean install -DskipTests

# Then run tests:
cd ../../tests/io.github.nheuermann.m2spreadsheet.tests
mvn test -Dtest=FolderBasedTemplatesTest
```

**Always compile first, then test!**

### Key Architectural Patterns from M2Doc

#### 1. Template Structure
```java
DocumentTemplate
├── Body (Block of statements)
├── Headers
└── Footers
```

For M2Spreadsheet, consider:
```java
WorkbookTemplate
├── Sheets
│   └── Cells (with template expressions)
└── Styles
```

#### 2. Main Classes to Mirror

| M2Doc Class | M2Spreadsheet Equivalent | Purpose |
|-------------|--------------------------|---------|
| `M2DocUtils` | `M2SpreadsheetUtils` | Main API entry point |
| `M2DocEvaluator` | `M2SpreadsheetEvaluator` | Template evaluation/rendering |
| `DocumentTemplate` | `WorkbookTemplate` | Template representation |
| `M2DocParser` | `M2SpreadsheetParser` | Template parsing |
| `AbstractTemplatesTestSuite` | `AbstractSpreadsheetsTestSuite` | Test framework |

#### 3. POI Class Mapping

| M2Doc (Word) | M2Spreadsheet (Excel) |
|--------------|----------------------|
| `XWPFDocument` | `XSSFWorkbook` |
| `XWPFParagraph` | `XSSFRow` |
| `XWPFRun` | `XSSFCell` |
| `XWPFTable` | `XSSFSheet` (or native tables) |

#### 4. Generation Flow

```java
// M2Doc flow
1. Parse template .docx → DocumentTemplate AST
2. Validate expressions against query environment
3. Initialize destination document
4. Evaluate template with variables
5. Save result .docx

// M2Spreadsheet should follow similar pattern
1. Parse template .xlsx → WorkbookTemplate AST
2. Validate expressions against query environment
3. Initialize destination workbook
4. Evaluate template with variables
5. Save result .xlsx
```

### Testing Strategy

#### Standalone Test Setup (Phase 1)
1. Create simple Maven/Gradle test module **without OSGi dependencies**
2. Use plain JUnit tests that can run in VS Code
3. Mock EMF model data as input
4. Focus on generation output (rendering engine)
5. Verify output by opening in Excel

#### Test Structure Pattern
```java
@Test
public void testSimpleGeneration() throws Exception {
    // 1. Setup: Load template
    URI templateURI = URI.createFileURI("test-resources/simple-template.xlsx");
    
    // 2. Setup: Prepare query environment and variables
    IQueryEnvironment queryEnv = Query.newEnvironmentWithDefaultServices();
    Map<String, Object> variables = new HashMap<>();
    variables.put("data", mockModelData);
    
    // 3. Execute: Generate
    GenerationResult result = M2SpreadsheetUtils.generate(
        workbookTemplate, queryEnv, variables, 
        resourceSet, outputURI, monitor);
    
    // 4. Verify: Check result
    assertTrue(result.getErrors().isEmpty());
    // Manually verify output in Excel or use POI to assert cell values
}
```

#### Debug and Inspection Utilities

The following utilities are available for debugging generated spreadsheets. **Use these instead of creating new ones!**

##### 1. ExcelReader.java
Location: `tests/io.github.nheuermann.m2spreadsheet.tests/src/test/java/io/github/nheuermann/m2spreadsheet/tests/ExcelReader.java`

Dumps Excel file content for debugging (shows sheet structure, row counts, and first 5 rows with cell values).

**Usage:**
```bash
cd tests/io.github.nheuermann.m2spreadsheet.tests
mvn exec:java -Dexec.mainClass="io.github.nheuermann.m2spreadsheet.tests.ExcelReader" \
  -Dexec.args="src/test/resources/cases/simple-features/simple-features-generated.xlsx"
```

**Output format:** Shows sheets, row numbers, and cell contents with column indices.

##### 2. XlsxToCsvConverter.java
Location: `tests/io.github.nheuermann.m2spreadsheet.tests/src/test/java/io/github/nheuermann/m2spreadsheet/tests/XlsxToCsvConverter.java`

Converts XLSX files to CSV/TSV format for easy diffing and inspection.

**Usage:**
```bash
cd tests/io.github.nheuermann.m2spreadsheet.tests

# Convert to file:
mvn exec:java -Dexec.mainClass="io.github.nheuermann.m2spreadsheet.tests.XlsxToCsvConverter" \
  -Dexec.args="input.xlsx output.csv"

# Print to stdout:
mvn exec:java -Dexec.mainClass="io.github.nheuermann.m2spreadsheet.tests.XlsxToCsvConverter" \
  -Dexec.args="input.xlsx"
```

**Use cases:** 
- Compare expected vs generated using standard diff tools
- Quick inspection of cell values without opening Excel

##### 3. DiffReader.java
Location: `tests/io.github.nheuermann.m2spreadsheet.tests/src/test/java/io/github/nheuermann/m2spreadsheet/tests/folder/DiffReader.java`

Reads and displays diff files created by the test suite (shows what differences were detected).

**Usage:**
```bash
cd tests/io.github.nheuermann.m2spreadsheet.tests
mvn exec:java -Dexec.mainClass="io.github.nheuermann.m2spreadsheet.tests.folder.DiffReader" \
  -Dexec.args="src/test/resources/cases/structured-fmea/structured-fmea-diff.xlsx"
```

**Output format:** Lists all cells in the diff file with their values (differences are highlighted by the test suite).

**Use cases:**
- Quickly see what differences exist between expected and generated
- Debug test failures without opening Excel

### Code Generation Guidelines

When generating code for M2Spreadsheet:

1. **Start with M2Doc equivalent**: Always check if M2Doc has a corresponding class/method
2. **Adapt for cells vs runs**: Remember the fundamental difference:
   - Word: Text flows in runs within paragraphs
   - Excel: Discrete cell values in a grid
3. **Expression locations**: In Excel, expressions are typically cell-level, not within text
4. **Keep AQL integration**: Reuse all the Acceleo Query Language infrastructure
5. **Maintain service pattern**: Services for formatting, formulas, etc.
6. **Test-driven**: Write tests before/alongside implementation

### ⚠️ Critical: Row/Column Code Alignment

**ALWAYS keep for_row/for_column and merge_row/merge_column implementations aligned!**

These operations are directional mirrors of each other. Any logic in one MUST have its counterpart in the other:

- **for_row** ↔ **for_column**: Loop directives for rows vs columns
- **merge_row** ↔ **merge_column**: Merge directives for vertical vs horizontal merging

**Key principles:**
1. Use **identical data structures**: If `processForRowLoop` uses `Map<Integer, MergeInfo>`, then `expandRowWithLoop` must too
2. Use **identical validation logic**: Multiple directive checks, conflict detection, error messages
3. Use **identical debug output format**: Same message patterns, same level of detail
4. Use **identical error handling**: No divergence in how errors are reported

**Before implementing row OR column logic:**
- Check if the counterpart exists
- Copy and adapt the pattern exactly
- Don't create "custom" solutions for just one direction

**When reviewing code:**
- Always compare row vs column implementations side-by-side
- Look for any structural differences (List vs Map, different validation, etc.)
- Align immediately - don't let divergence accumulate!

### Package Structure (Recommended)

```
org.obeonetwork.m2spreadsheet/
├── src/
│   └── org/obeonetwork/m2spreadsheet/
│       ├── parser/          # Template parsing
│       ├── generator/       # Template evaluation & generation
│       ├── template/        # AST model (EMF)
│       ├── element/         # Spreadsheet element wrappers
│       ├── services/        # Built-in AQL services
│       └── util/            # M2SpreadsheetUtils main API

tests/org.obeonetwork.m2spreadsheet.tests/
└── src/
    └── org/obeonetwork/m2spreadsheet/tests/
        ├── AbstractSpreadsheetsTestSuite.java
        └── resources/       # Test templates and expected outputs
```

### Removing OSGi Dependencies (For Tests)

When adapting M2Doc code for standalone testing:

1. **Remove**: `Require-Bundle` from MANIFEST.MF (for test modules)
2. **Replace**: OSGi service loading with direct instantiation
3. **Keep**: Core EMF, AQL, and POI dependencies as regular Maven deps
4. **Use**: Standard Maven/Gradle dependency management

Example dependency in test pom.xml:
```xml
<dependencies>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.3</version>
    </dependency>
    <dependency>
        <groupId>org.eclipse.acceleo</groupId>
        <artifactId>org.eclipse.acceleo.query</artifactId>
        <version>6.0.1</version>
    </dependency>
    <!-- ... other deps ... -->
</dependencies>
```

### Important Notes

- **Cell vs Run distinction**: The biggest architectural difference
  - Word Run: Can contain formatted text spans
  - Excel Cell: Single value (or formula) with cell-wide formatting
- **Template syntax**: Consider if `{m:expression}` should be:
  - The entire cell content, OR
  - Embedded within text (Excel cells can contain text)
- **Iteration patterns**: 
  - Word: Repeat paragraphs/rows
  - Excel: Repeat rows/cells (more natural for tables)
- **Styling**: Excel has very different styling model (cell styles, conditional formatting, etc.)

### Current Project State

- **Existing**: M2Doc fully functional for .docx generation
- **Goal**: Create parallel M2Spreadsheet for .xlsx generation  
- **Approach**: Pattern-based development using M2Doc as reference
- **Focus**: Getting rendering engine working first with mocked input

### Next Development Phases

1. **Phase 1**: Basic test infrastructure (current focus)
   - Get M2Doc tests running in VS Code standalone
   - Create minimal M2Spreadsheet stub
   - First successful .xlsx generation (even if simple)

2. **Phase 2**: Core parsing and evaluation
   - Implement spreadsheet parser (cells, expressions)
   - Port M2DocEvaluator logic to spreadsheets
   - Handle basic AQL expressions in cells

3. **Phase 3**: Services and features
   - Port/adapt M2Doc services
   - Add spreadsheet-specific services (formulas, formatting)
   - Iteration/condition handling

4. **Phase 4**: Integration with medini
   - Connect to real medini EMF models
   - Production-ready generation

---

## Coding Style Preferences

- Follow existing M2Doc conventions
- Use clear, descriptive names mirroring M2Doc where applicable
- Comprehensive JavaDoc following the M2Doc style
- Keep test resources organized by feature
- Prefer composition over deep inheritance
- Make generation flow explicit and traceable

## When in Doubt

- Check the corresponding M2Doc class/pattern first
- Consider the Word Run → Excel Cell translation
- Ask questions about Excel-specific behaviors
- Test incrementally with simple cases first
