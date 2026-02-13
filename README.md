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
* **Nesting**: 
    * Multiple `for_column` loops can be nested
    * There can be multiple sequential `for_column` loops in a sheet
    * Nesting with `for_row`: `for_column` loops are always treated to be *within* any `for_row` (one or many) loops
    * Recommendation: If nested loops are used, you may use `{m:endfor_column variable}` for overview purposes (additional statements after `endfor_column` are ignored by the parser)