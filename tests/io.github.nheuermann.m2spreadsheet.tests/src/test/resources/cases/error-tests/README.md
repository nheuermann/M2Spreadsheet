# Error Tests

This test case validates that all syntax errors are properly detected and reported during both validation and generation.

## Template Structure (error-tests-template.xlsx)

### Sheet: IfErrors
Tests various if/elseif/else/endif errors:

| Row | Cell A | Description |
|-----|--------|-------------|
| 0 | `{m:if data.value = 'test'}OK{m:endif}` | Valid (control) |
| 1 | `{m:if data.value =` | Missing closing brace } |
| 2 | `{m:if data.invalidField}X{m:endif}` | Invalid field reference |
| 3 | `{m:if data.value = 'test'}X` | Missing {m:endif} |
| 4 | `{m:if data.value = 'test'}X{m:elseif data.value =}Y{m:endif}` | Malformed elseif |
| 5 | `{m:if data.value = 'test'}X{m:else}Y` | Missing endif after else |

### Sheet: ForLoopErrors
Tests various for loop errors:

| Row | Cell A | Description |
|-----|--------|-------------|
| 0 | `{m:for item \| data.items}{m:item.name}{m:endfor}` | Valid (control) |
| 1 | `{m:for item data.items}{m:item.name}{m:endfor}` | Missing pipe separator |
| 2 | `{m:for item \| data.invalidItems}{m:item.name}{m:endfor}` | Invalid collection |
| 3 | `{m:for item \| data.items}{m:item.name}` | Missing {m:endfor} |
| 4 | `{m:for item \| data.value}{m:item.name}{m:endfor}` | Collection is not iterable |

### Sheet: ForRowErrors
Tests row-level loop errors:

| Row | Cell A | Description |
|-----|--------|-------------|
| 0 | `{m:for_row item \| data.items}` | Valid start (control) |
| 1 | `{m:item.name}` | Body |
| 2 | `{m:endfor_row}` | Valid end |
| 3 | `{m:for_row item data.items}` | Missing pipe |
| 4 | `{m:for_row item \| data.invalidItems}` | Invalid collection |
| 5 | (empty - testing missing endfor_row) |

### Sheet: ForColumnErrors
Tests column-level loop errors:

| Cell | A | B | C |
|------|---|---|---|
| 0 | `{m:for_column item \| data.items}` | `{m:item.name}` | `{m:endfor_column}` (valid control) |
| 1 | `{m:for_column item data.items}` | Missing pipe |  |
| 2 | `{m:for_column item \| data.invalidItems}` | Invalid collection |  |

### Sheet: MergeErrors
Tests merge directive errors:

| Row | Cell A | Description |
|-----|--------|-------------|
| 0 | `{m:for_row item \| data.items}` |  |
| 1 | `{m:merge_row item}{m:merge_row item}Duplicate` | Multiple merge_row directives |
| 2 | `{m:endfor_row}` |  |

### Sheet: ExpressionErrors
Tests basic expression errors:

| Row | Cell A | Description |
|-----|--------|-------------|
| 0 | `{m:data.value}` | Valid (control) |
| 1 | `{m:data.invalidField}` | Invalid field |
| 2 | `{m:data.value +}` | Incomplete expression |
| 3 | `{m:` | Malformed expression |

## Expected Validation Messages

All errors should be reported with:
- ValidationMessageLevel.ERROR
- Clear error message explaining the problem
- Correct sheet name, row index, and column index

## Notes

- Tests should run in both validation mode and generation mode
- In generation mode, errors should appear in the output workbook's error sheet
- In validation mode, validation workbook should highlight all error cells
