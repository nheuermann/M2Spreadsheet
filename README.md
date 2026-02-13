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
    * Multiple `for_column` loops can be nested
    * There can be multiple sequential `for_column` loops in a sheet
    * Nesting with `for_row`: `for_column` loops work within `for_row` loops - each for_row iteration gets the full column expansion
    * Recommendation: If nested loops are used, you may use `{m:endfor_column variable}` for overview purposes (additional statements after `endfor_column` are ignored by the parser)

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
| `{m:if condition}` ... `{m:endif}` |

* **Placement**: `if`/`endif` statements have to be in the *same cell*
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
* `Status: {m:if isActive}Active{m:endif}{m:if not isActive}Inactive{m:endif}`
* `{m:if req.dal = 'DAL-A'}Critical{m:endif}`
* `{m:if traces->notEmpty()}Has traces{m:endif}`
* `{m:for_row req | requirements}{m:if req.traces->includes(event.id)}X{m:endif}{m:endfor_row}`


# Test infrastructure

## Folder-based tests

Folder-based tests automatically discover and run all test cases in `src/test/resources/cases/`.

### Run all folder-based tests
```bash
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