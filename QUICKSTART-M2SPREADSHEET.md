# M2Spreadsheet - Quick Start Guide

## 🎯 What Was Created

A complete standalone testing setup for M2Spreadsheet development in VS Code:

### 1. **M2Doc Baseline Tests** (`tests/org.obeonetwork.m2doc.standalone.tests/`)
- Validates M2Doc works without OSGi
- Proves test infrastructure works

### 2. **M2Spreadsheet Plugin** (`plugins/io.github.nheuermann.m2spreadsheet/`)
- Core generation logic
- `M2SpreadsheetUtils` - Main API
- `GenerationResult` - Result container

### 3. **M2Spreadsheet Tests** (`tests/io.github.nheuermann.m2spreadsheet.tests/`)
- Working test with your template: `"Name: {m:customer.name} from company {m:customer.company}"`
- Multiple test scenarios
- No template files needed - creates programmatically!

## 🚀 Running Your First Test

### Option 1: VS Code (Recommended)
1. Open file: `tests/io.github.nheuermann.m2spreadsheet.tests/src/test/java/io/github/nheuermann/m2spreadsheet/tests/SimpleM2SpreadsheetTest.java`
2. Look for the green ▶️ "Run Test" button above `testSimpleCustomerTemplate()`
3. Click it
4. Check `tests/io.github.nheuermann.m2spreadsheet.tests/target/test-output/simple-generated.xlsx`
5. Open in Excel to verify!

### Option 2: Maven Command Line
```bash
# From project root
cd plugins/io.github.nheuermann.m2spreadsheet
mvn clean install

cd ../../tests/io.github.nheuermann.m2spreadsheet.tests
mvn clean test
```

## 📋 What Each Test Does

### `testSimpleCustomerTemplate()`
Your requested template! Tests:
- Input: `"Name: {m:customer.name} from company {m:customer.company}"`
- Variables: `customer.name = "John Doe"`, `customer.company = "Acme Corp"`
- Output: `"Name: John Doe from company Acme Corp"`

### `testMultipleCells()`
Tests separate cells:
- Cell A1: `{m:customer.name}` → `"Alice Johnson"`
- Cell B1: `{m:customer.company}` → `"Digital Solutions"`

### `testWithTemplateFile()`
Loads from actual `.xlsx` file (skipped if file doesn't exist)

## 📁 Generated Files

All output goes to: `tests/io.github.nheuermann.m2spreadsheet.tests/target/test-output/`

Open these in Excel to verify generation!

## 🔧 Current Implementation

### What Works Now
✅ Parse `{m:expression}` syntax  
✅ Simple variable lookup (`{m:customer.name}`)  
✅ Property navigation (`customer.name`, `customer.company`)  
✅ Multiple expressions in one cell  
✅ Multiple sheets and cells  
✅ All without needing template files!

### What's Next (Simple to Add)
- [ ] Real AQL expression evaluation (not just variable lookup)
- [ ] Row iteration (like `{m:for customer | customers}`)
- [ ] Conditional sections (`{m:if condition}`)
- [ ] Cell styling preservation
- [ ] Services (formatting, etc.)

## 🎨 Architecture

Based on M2Doc patterns:
```
M2SpreadsheetUtils (main API)
  ↓
Generate from template workbook
  ↓
Process each cell for {m:...} expressions
  ↓
Evaluate expressions (simple for now, AQL later)
  ↓
Write to output workbook
  ↓
Return GenerationResult
```

## 🐛 Troubleshooting

### "Cannot resolve dependencies"
```bash
cd plugins/io.github.nheuermann.m2spreadsheet
mvn clean install
```

### "Output file not found"
Check: `tests/io.github.nheuermann.m2spreadsheet.tests/target/test-output/`

### "Test skipped"
Some tests skip if template files don't exist - that's OK! The programmatic tests will work.

## 📝 Making Changes

### Add a new expression type
1. Edit `M2SpreadsheetUtils.evaluateSimpleExpression()`
2. Add test in `SimpleM2SpreadsheetTest`
3. Run test to verify

### Change the template
Edit the test directly:
```java
cell.setCellValue("Your template: {m:your.expression}");
```

No need to create actual files!

## 🎯 Next Development Steps

1. **Now**: Run the test, verify it works!
2. **Next**: Add proper AQL evaluation
3. **Then**: Add iteration support
4. **Then**: Add M2Doc-style parser for reading .xlsx templates
5. **Finally**: Connect to real medini models

---

## ✨ Key Achievement

You now have **working spreadsheet generation** with:
- ✅ Template expressions
- ✅ Variable evaluation  
- ✅ Running in VS Code
- ✅ No OSGi/Eclipse needed
- ✅ Can open results in Excel immediately

The foundation is **working**! Now you can iterate quickly. 🚀
