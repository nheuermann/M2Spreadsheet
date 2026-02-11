# M2Doc Services Analysis for M2Spreadsheet

This document lists all services available in M2Doc, categorized by their relevance to M2Spreadsheet (Excel generation).

## Status Legend
- ✅ **Update Now**: High priority, directly applicable to spreadsheets
- 🔄 **Update Later**: Useful but lower priority, adapt when needed
- ❌ **Delete/Skip**: Not applicable to spreadsheets
- ⚠️ **Needs Review**: Requires analysis to determine applicability

---

## ✅ UPDATE NOW - High Priority Services

### 1. ExcelServices
**Location**: `org.obeonetwork.m2doc.services.ExcelServices`

**Purpose**: Import Excel data into M2Doc documents

**Key Methods**:
- `asTable(String uri, String sheetName, String topLeft, String bottomRight)` - Import Excel range as table
- Cell reading and formatting from Excel files
- Style preservation (colors, fonts, borders)

**M2Spreadsheet Action**: 
- **ADAPT and EXPAND** - This is perfect for M2Spreadsheet!
- Instead of importing Excel to Word, we'll use similar logic for:
  - Reading template Excel files
  - Copying cell styles
  - Preserving formatting during generation
- Add methods for writing cells, not just reading
- Add formula generation support

**Implementation Notes**:
```java
// M2Doc: reads Excel → inserts into Word
String uri = 'data.xlsx'.asTable('Sheet1', 'A1', 'C10')

// M2Spreadsheet: reads/writes Excel → generates new Excel
// Keep reading logic, add writing logic
```

---

### 2. BooleanServices
**Location**: `org.obeonetwork.m2doc.services.BooleanServices`

**Purpose**: Format boolean values

**Key Methods**:
- `check(boolean)` - Returns "X" for true, "" for false
- `yesNo(boolean)` - Returns "Yes" for true, "No" for false

**M2Spreadsheet Action**:
- **KEEP AS-IS** - Works perfectly for spreadsheets
- Useful for checkbox-style cells
- Can extend with: `truefalse()`, `10()`, custom symbols

**Example Usage**:
```
{m:isActive.check()} → "X" or ""
{m:hasPermission.yesNo()} → "Yes" or "No"
```

---

## 🔄 UPDATE LATER - Medium Priority Services

### 3. LinkServices
**Location**: `org.obeonetwork.m2doc.services.LinkServices`

**Purpose**: Create hyperlinks and bookmarks

**Key Methods**:
- `asLink(String text, String url)` - Create hyperlink
- `asLink(String text, String url, String tooltip)` - Hyperlink with tooltip
- `asBookmark(String text, String id)` - Create bookmark
- `asBookmarkRef(String text, String id)` - Reference bookmark

**M2Spreadsheet Action**:
- **ADAPT** - Excel supports hyperlinks (different format than Word)
- Keep `asLink()` - use POI's `XSSFHyperlink`
- Drop bookmarks - Excel uses named ranges instead
- Add: `asNamedRange(String name, CellAddress)` for Excel-style references

**Implementation Priority**: Phase 2-3 (after basic text generation works)

---

### 4. DocumentServices  
**Location**: `org.obeonetwork.m2doc.services.DocumentServices`

**Purpose**: Manipulate document properties (title, author, version, custom properties)

**Key Methods**:
- `addDocumentProperty(boolean value, String name)` - Add custom property
- `addDocumentProperty(double value, String name)` - Add numeric property
- `addDocumentProperty(String value, String name)` - Add text property
- `getDocumentProperty(String name)` - Get property value
- Document metadata (title, author, created date, etc.)

**M2Spreadsheet Action**:
- **ADAPT** - Excel has similar workbook properties
- Excel workbook properties: title, subject, author, company, etc.
- Use POI's `POIXMLProperties` (same as M2Doc, but for workbooks)
- Methods translate 1:1 to Excel

**Implementation Priority**: Phase 3 (nice-to-have for metadata)

---

## ❌ DELETE/SKIP - Not Applicable to Spreadsheets

### 5. ImageServices
**Location**: `org.obeonetwork.m2doc.services.ImageServices`

**Purpose**: Insert images into Word documents

**Key Methods**:
- `asImage(String uri)` - Insert image from URI
- `asImage(String uri, String format)` - Insert with specific format
- `resize(MImage, int width, int height)` - Resize images
- Image manipulation (rotate, flip, crop)

**M2Spreadsheet Action**:
- **SKIP FOR NOW** - Excel can embed images, but it's not a priority
- Word documents are image-heavy, spreadsheets are data-heavy
- If needed later: Use `XSSFDrawing` and `XSSFClientAnchor`

**Rationale**: Focus on data/formulas/tables first, images are edge case for spreadsheets

---

### 6. PaginationServices
**Location**: `org.obeonetwork.m2doc.services.PaginationServices`

**Purpose**: Control page layout, breaks, sections in Word

**Key Methods**:
- `newTableOfContents()` - Generate TOC
- `newColumn()` - Column break
- `newPage()` - Page break
- `newParagraph(String text, String style)` - Styled paragraph
- `newTextWrappingSection()` - Text wrapping sections
- Style management for Word documents

**M2Spreadsheet Action**:
- **DELETE** - Excel doesn't have Word-style pagination
- Excel has: sheets (tabs), page breaks for printing, but very different model
- Equivalent for Excel:
  - `newSheet(String name)` - Create new worksheet
  - `setPageBreak(int row)` - Add page break for printing
  - `freezePanes(int row, int col)` - Freeze panes

**Implementation**: Create new `SpreadsheetServices` class with Excel-specific methods

---

### 7. PromptServices
**Location**: `org.obeonetwork.m2doc.services.PromptServices`

**Purpose**: Interactive console prompts during generation

**Key Methods**:
- `promptString(String message)` - Prompt for string input
- `promptInteger(String message)` - Prompt for integer
- `promptLong(String message)` - Prompt for long
- `promptReal(String message)` - Prompt for double

**M2Spreadsheet Action**:
- **DELETE** - Not useful for automated spreadsheet generation
- M2Spreadsheet should be non-interactive (batch processing)
- If user interaction needed, handle at higher level (before template processing)

**Rationale**: Spreadsheet generation should be automated, not interactive

---

## ⚠️ NEEDS REVIEW - Requires Analysis

### 8. POIServices (Utility Class)
**Location**: `org.obeonetwork.m2doc.POIServices`

**Purpose**: Low-level Apache POI utilities for Word documents

**Key Functionality**:
- Load XWPFDocument from URI
- OPC Package handling
- Template custom properties
- Style management

**M2Spreadsheet Action**:
- **REVIEW AND ADAPT** - We need similar utilities for Excel
- Create `POISpreadsheetServices`:
  - Load XSSFWorkbook from URI
  - Handle Excel packages
  - Template properties for .xlsx
- Use EMF URIConverter for consistency with M2Doc

**Implementation**: Create early (needed for basic infrastructure)

---

## New Services Needed for M2Spreadsheet

### SpreadsheetServices (NEW)
**Purpose**: Excel-specific operations not in M2Doc

**Proposed Methods**:
```java
// Sheet management
newSheet(String name) - Create worksheet
getSheet(String name) - Access existing sheet
deleteSheet(String name) - Remove worksheet

// Formula services  
formula(String expression) - Create Excel formula
sumRange(String range) - =SUM(range)
vlookup(String lookup, String table, int col) - =VLOOKUP()

// Formatting services
formatCurrency(double value) - Apply currency format
formatDate(Date value, String pattern) - Date formatting
formatPercent(double value) - Percentage format

// Cell styling
bold(String text) - Bold text in cell
color(String text, String hexColor) - Colored text
backgroundColor(String hexColor) - Cell background

// Range operations
mergeAcross(int cols) - Merge cells horizontally
mergeDown(int rows) - Merge cells vertically
freezePanes(int row, int col) - Freeze header rows/cols

// Data validation
dropdown(List<String> options) - Create dropdown list
numberRange(double min, double max) - Numeric validation
```

---

## Implementation Roadmap

### Phase 1: Core Services (NOW)
1. ✅ BooleanServices - Port as-is
2. ✅ Basic ExcelServices - Reading/writing cells
3. ✅ Create POISpreadsheetServices utility
4. ✅ Create SpreadsheetServices for Excel-specific features

### Phase 2: Enhanced Services (SOON)  
1. 🔄 LinkServices - Adapt for Excel hyperlinks
2. 🔄 ExcelServices - Advanced formatting, styles
3. 🔄 SpreadsheetServices - Formulas, validation
4. 🔄 DocumentServices - Workbook properties

### Phase 3: Advanced Services (LATER)
1. 🔄 SpreadsheetServices - Charts, conditional formatting
2. 🔄 ImageServices - If needed for embedded images
3. 🔄 Custom user-defined services

### Not Implementing
1. ❌ PaginationServices - Word-specific
2. ❌ PromptServices - Non-interactive generation

---

## Service Registration

M2Doc uses `@ServiceProvider` annotation for AQL integration:

```java
@ServiceProvider(
  value = "Services available for Spreadsheets"
)
public class SpreadsheetServices {
    // Methods here are automatically available in templates
}
```

For M2Spreadsheet, we need to:
1. Register services with AQL QueryEnvironment
2. Use same annotation pattern for consistency
3. Load services via ServiceLoader or manual registration

---

## Migration Checklist

- [ ] Port BooleanServices (unchanged)
- [ ] Adapt ExcelServices for reading/writing
- [ ] Create POISpreadsheetServices utility
- [ ] Create new SpreadsheetServices class
- [ ] Adapt LinkServices for hyperlinks
- [ ] Adapt DocumentServices for workbook properties
- [ ] Skip PaginationServices (not applicable)
- [ ] Skip PromptServices (not applicable)
- [ ] Skip ImageServices (defer to later phase)
- [ ] Set up service registration mechanism
- [ ] Write tests for each service

---

## Notes

**Key Difference**: M2Doc focuses on document formatting (Word), M2Spreadsheet focuses on data presentation (Excel). Services should reflect this:
- Less focus on text styling, pagination, images
- More focus on formulas, data validation, cell formatting
- Excel's grid model vs Word's flow model

**Compatibility**: Keep service method signatures similar to M2Doc where possible for easier learning curve.
