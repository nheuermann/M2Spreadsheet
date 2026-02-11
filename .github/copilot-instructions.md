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
