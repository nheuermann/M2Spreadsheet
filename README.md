# Welcome to M2Spreadsheet by Nils Heuermann
M2Spreadsheet enables the generation of [Office Open XML](https://fr.wikipedia.org/wiki/Office_Open_XML) spreadsheet documents (.xlsx) from models. 

It's inspired by (and originally forked from) M2Doc https://www.m2doc.org/.

# Documentation
## Syntax
### Repetition / Loops

#### Repeating within a cell

|   |
| - |
| `{m:for variable \| AQL expression}` ... `{m:endfor}` |

* **Placement**: `for`/`endfor` statements have to be in the *same cell*
* **Nesting**: 
    * Multiple `for` loops can be nested
    * There can be multiple sequential `for` loops in a cell
    * Recommendation: If nested loops are used, you may use `{m:endfor variable}` for overview purposes (additional statements after `endfor` are ignored by the parser)


#### Repeating rows

|   |
| - |
| `{m:for_row variable \| AQL expression}` |
| ...              |
| `{m:endfor_row}` |

* **Placement**: `for_row`/`endfor_row` statements have to be in the *first column* of the sheet
* **Nesting**: 
    * Multiple `for_row` loops can be nested
    * There can be multiple sequential `for_row` loops in a sheet
    * Nesting with `for_column`: `for_column` loops are always treated to be *within* any `for_row` (one or many) loops
    * Recommendation: If nested loops are used, you may use `{m:endfor_row variable}` for overview purposes (additional statements after `endfor_row` are ignored by the parser)
* **Cell Merging**: Use `{m:merge_row variablename}` in a cell to merge it vertically across all iterations of that loop variable (e.g., `{m:merge_row component}{m:component.name}` merges the component name across its failure modes)

#### Repeating columns
|   |   |   |
| - | - | - |
| `{m:for_column variable \| AQL expression}` | ... | `{m:endfor_column}` |

* **Placement**: `for_column`/`endfor_column` statements have to be in the *first row* of the sheet
* **Scope**: Column loops declared in the first row apply to **ALL rows** in the sheet
  - Variables from `for_column` are available in all subsequent rows
  - Each row automatically repeats columns according to the column loop
  - Example: If row 0 has `{m:for_column event | events}`, then row 1, row 2, etc. all expand horizontally for each event
* **Nesting**: 
    * Multiple `for_column` loops can be nested to create hierarchical column structures
    * Nested loops in row 0 apply to ALL subsequent rows with full variable context
    * Example: `{m:for_column component | components}{m:for_column fm | component.failureModes}{m:for_column effect | fm.effects}` creates a 3-level hierarchy where all variables (`component`, `fm`, `effect`) are available in rows below
    * There can be multiple sequential `for_column` loops in a sheet
    * Nesting with `for_row`: `for_column` loops work within `for_row` loops - each for_row iteration gets the full column expansion
    * Recommendation: If nested loops are used, you may use `{m:endfor_column variable}` for overview purposes (additional statements after `endfor_column` are ignored by the parser)
* **Cell Merging**: Use `{m:merge_column variablename}` in rows below row 0 to merge cells horizontally across all iterations of that loop variable (e.g., `{m:merge_column phase}{m:phase.name}` merges the phase header across its tasks)

#### Index variables

All loop types (`for`, `for_row`, `for_column`) automatically provide an index variable that tracks the current iteration (0-based).

**Pattern**: For a loop variable named `variable`, the index is available as `{m:variable_index}`

**Examples**:
* `{m:for item | items}` → access index with `{m:item_index}` (0, 1, 2, ...)
* `{m:for_row person | people}` → access index with `{m:person_index}` 
* `{m:for_column category | categories}` → access index with `{m:category_index}`

**Use cases**:
* Display row/column numbers (add 1 for 1-based numbering: `{m:item_index + 1}`)
* Access parallel arrays by index: `{m:scores->at(item_index + 1)}`
* Conditional formatting based on position (e.g., alternate colors)

### Conditional logic

#### Conditional content within a cell

|   |
| - |
| `{m:if condition}` ... [`{m:elseif condition}`] ... [`{m:else}`] ... `{m:endif}` |

* **Placement**: `if`/`elseif`/`else`/`endif` statements have to be in the *same cell*
* **Syntax**:
  - `{m:if condition}` - Required: starts the conditional block
  - `{m:elseif condition}` - Optional: can have multiple elseif branches (evaluated in order)
  - `{m:else}` - Optional: fallback when all previous conditions are false
  - `{m:endif}` - Required: ends the conditional block
* **Condition**: Any AQL expression that evaluates to a boolean value
  - Boolean values: `true`, `false`
  - Comparisons: `req.dal = 'DAL-A'`, `count > 5`, `name->size() > 0`
  - Collections: Non-empty collections evaluate to `true`
  - Objects: Non-null objects evaluate to `true`
  - Numbers: Non-zero numbers evaluate to `true`
* **Nesting**: 
    * Multiple `if` statements can be nested
    * There can be multiple sequential `if` statements in a cell
    * Can be combined with `for` loops
    * Recommendation: Use `{m:endif condition}` for clarity (extra text after `endif` is ignored)

**Examples**:

Simple if/else:
```
Status: {m:if active}Active{m:else}Inactive{m:endif}
```

Grade calculation with multiple elseif:
```
Grade: {m:if score >= 90}A{m:elseif score >= 80}B{m:elseif score >= 70}C{m:elseif score >= 60}D{m:else}F{m:endif}
```

String matching:
```
Day: {m:if day == 'Mon'}Monday{m:elseif day == 'Tue'}Tuesday{m:elseif day == 'Wed'}Wednesday{m:else}Other{m:endif}
```

Nested conditionals:
```
{m:if hasLicense}{m:if age >= 18}Can drive{m:else}Too young{m:endif}{m:else}No license{m:endif}
```

Used with for_row loops:
```
{m:for_row req | requirements}{m:if req.traces->includes(event.id)}X{m:elseif req.traces->size() > 0}?{m:else}-{m:endif}{m:endfor_row}
```

### Rich text formatting

M2Spreadsheet preserves **inline text formatting** (bold, italic, colors, etc.) from templates through to generated outputs.

**How it works**:
* Template formatting within `{m:...}` expressions is preserved when evaluating replacements
* The formatting of the `m` character in `{m:expression}` determines the formatting of the replacement text
* Multiple formatting styles within a cell are preserved through transformations

**Example use cases**:

Bold value in conditional:
```
Template cell: "{m:if condition}X{m:endif}" with only the X in bold
Generated output: "X" will be bold (if condition is true)
```

Colored status indicators:
```
Template: "Status: {m:status}" with {m:status} in red
If status evaluates to "Active", the word "Active" appears in red
```

Mixed formatting in loops:
```
Template: "{m:for item | items}{m:item} {m:endfor}" 
If item names alternate bold/regular, that formatting flows through
```

**Formatting rules**:
1. **Expression brackets** (`{m:...}`): The 'm' character's formatting is applied to replacement text
2. **Text replacement**: Inherits the formatting from the template position
3. **Disappearing expressions**: When an expression evaluates to empty, formatting is irrelevant

**Limitations**:
* Rich text formatting is supported for XSSF (Excel .xlsx) workbooks
* Currently, formatting tracking through complex nested transformations uses a simplified approach
* (Future enhancement: Full character-by-character formatting tracking through all transformations)


# Test infrastructure

## Folder-based tests

Folder-based tests automatically discover and run all test cases in `src/test/resources/cases/`.

### Build the plugins after changes - in case not yet done
```bash
cd plugins/io.github.nheuermann.m2spreadsheet
mvn clean install -DskipTests
```

### Run all folder-based tests
```bash
cd /Users/nils/Documents/GitHub/M2Spreadsheet/tests/io.github.nheuermann.m2spreadsheet.tests
mvn test -Dtest=FolderBasedTemplatesTest
```

### Add a new test case
```bash
mkdir src/test/resources/cases/my-test
```
* Create `my-test/my-test-template.xlsx` with M2Spreadsheet expressions
* Create `my-test/my-test-expected.xlsx` (optional - for validation)
* Tests auto-discover and run it!

## Ad-hoc template execution

For quick testing of a single template without setting up a folder structure, use `AdHocTemplateTest`:

### Quick start
1. Edit `AdHocTemplateTest.java`:
   ```java
   private static final String TEMPLATE_PATH = "/path/to/my-template.xlsx";
   private static final String OUTPUT_PATH = "/path/to/output.xlsx";
   ```

2. Customize `getTestVariables()` to provide your data:
   ```java
   variables.put("myData", Arrays.asList("a", "b", "c"));
   ```

3. Remove the `@Ignore` annotation from the test

4. Run:
   ```bash
   mvn test -Dtest=AdHocTemplateTest
   ```

The generated file will be saved to your specified output path with detailed console output showing the generation process and any errors.


# TODO
I have a question (don't implement for now):
The failure modes have each 3 effects and 2 causes. I would like to display them properly with merged cells. Ideally the failure mode spans across 6 rows, each effect spans across 2 rows, and each cause spans across 3 rows. With the construct below that's not possible. Do you have an idea how to achieve this? 
First in terms of how to define it in the xlsx template as a user.
Second how to implement it.

  {m:for_row fm | component.failureModes}		
    {m:for_row effect | fm.effects}		
    {m:for_row cause | fm.causes}		
{m:merge_row component}{m:component.name}	{m:fm.name}{m:merge_row fm}	{m:effect.name}{m:merge_row effect}
    {m:endfor_row}		
    {m:endfor_row}		
  {m:endfor_row}		
