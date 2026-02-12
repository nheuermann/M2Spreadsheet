#!/usr/bin/env python3
"""
Simple XLSX to TSV/CSV converter for debugging M2Spreadsheet generation.
Requires: openpyxl (pip install openpyxl)

Usage:
    python3 xlsx_to_csv.py input.xlsx [output.csv]
    
If output is omitted, prints to stdout.
"""

import sys
import os

def xlsx_to_tsv(xlsx_path, output_path=None):
    try:
        import openpyxl
    except ImportError:
        print("Error: openpyxl not installed. Install with: pip3 install openpyxl", file=sys.stderr)
        sys.exit(1)
    
    wb = openpyxl.load_workbook(xlsx_path, data_only=True)
    sheet = wb.active
    
    print(f"=== {xlsx_path} ===", file=sys.stderr)
    print(f"Sheet: {sheet.title} ({sheet.max_row} rows, {sheet.max_column} columns)\n", file=sys.stderr)
    
    # Debug: Show detailed cell info
    print("Detailed cell inspection:", file=sys.stderr)
    for row_idx in range(1, min(6, sheet.max_row + 1)):
        print(f"  Row {row_idx}:", file=sys.stderr, end="")
        for col_idx in range(1, min(4, sheet.max_column + 1)):
            cell = sheet.cell(row=row_idx, column=col_idx)
            val = cell.value
            print(f" [{col_idx}:{repr(val)}]", file=sys.stderr, end="")
        print(file=sys.stderr)
    print(file=sys.stderr)
    
    output = sys.stdout if output_path is None else open(output_path, 'w', encoding='utf-8')
    
    try:
        for row in sheet.iter_rows():
            values = []
            for cell in row:
                value = cell.value
                if value is None:
                    values.append("")
                else:
                    # Convert to string and escape tabs/newlines
                    values.append(str(value).replace('\t', '\\t').replace('\n', '\\n').replace('\r', '\\r'))
            
            output.write('\t'.join(values) + '\n')
        
        if output_path:
            print(f"\nConverted to: {output_path}", file=sys.stderr)
    finally:
        if output_path:
            output.close()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 xlsx_to_csv.py <input.xlsx> [output.csv]", file=sys.stderr)
        print("  If output is omitted, prints to stdout", file=sys.stderr)
        sys.exit(1)
    
    xlsx_path = sys.argv[1]
    
    if not os.path.exists(xlsx_path):
        print(f"Error: File not found: {xlsx_path}", file=sys.stderr)
        sys.exit(1)
    
    output_path = sys.argv[2] if len(sys.argv) >= 3 else None
    
    xlsx_to_tsv(xlsx_path, output_path)
