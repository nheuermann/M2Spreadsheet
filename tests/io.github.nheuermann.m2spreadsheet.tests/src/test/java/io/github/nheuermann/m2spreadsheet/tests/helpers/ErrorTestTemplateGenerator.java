package io.github.nheuermann.m2spreadsheet.tests.helpers;

import org.apache.poi.xssf.usermodel.*;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Helper to generate error test templates.
 * Run this as a main class to create error-tests-template.xlsx
 */
public class ErrorTestTemplateGenerator {
    
    public static void main(String[] args) throws IOException {
        XSSFWorkbook workbook = createErrorTestTemplate();
        String outputPath = "src/test/resources/cases/error-tests/error-tests-template.xlsx";
        
        try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
            workbook.write(fileOut);
            System.out.println("Created: " + outputPath);
        }
        
        workbook.close();
    }
    
    public static XSSFWorkbook createErrorTestTemplate() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        
        // Sheet 1: IfErrors
        XSSFSheet ifErrors = workbook.createSheet("IfErrors");
        createRow(ifErrors, 0, "{m:if data.value = 'test'}OK{m:endif}");  // Valid control
        createRow(ifErrors, 1, "{m:if data.value =");  // Missing closing brace
        createRow(ifErrors, 2, "{m:if data.invalidField}X{m:endif}");  // Invalid field
        createRow(ifErrors, 3, "{m:if data.value = 'test'}X");  // Missing endif
        createRow(ifErrors, 4, "{m:if data.value = 'test'}X{m:elseif data.value =}Y{m:endif}");  // Malformed elseif
        createRow(ifErrors, 5, "{m:if data.value = 'test'}X{m:else}Y");  // Missing endif after else
        
        // Sheet 2: ForLoopErrors
        XSSFSheet forLoopErrors = workbook.createSheet("ForLoopErrors");
        createRow(forLoopErrors, 0, "{m:for item | data.items}{m:item.name}{m:endfor}");  // Valid
        createRow(forLoopErrors, 1, "{m:for item data.items}{m:item.name}{m:endfor}");  // Missing pipe
        createRow(forLoopErrors, 2, "{m:for item | data.invalidItems}{m:item.name}{m:endfor}");  // Invalid collection
        createRow(forLoopErrors, 3, "{m:for item | data.items}{m:item.name}");  // Missing endfor
        createRow(forLoopErrors, 4, "{m:for item | data.value}{m:item.name}{m:endfor}");  // Not iterable
        
        // Sheet 3: ForRowErrors
        XSSFSheet forRowErrors = workbook.createSheet("ForRowErrors");
        createRow(forRowErrors, 0, "{m:for_row item | data.items}");  // Valid start
        createRow(forRowErrors, 1, "{m:item.name}");  // Body
        createRow(forRowErrors, 2, "{m:endfor_row}");  // Valid end
        createRow(forRowErrors, 3, "{m:for_row item data.items}");  // Missing pipe
        createRow(forRowErrors, 4, "{m:for_row item | data.invalidItems}");  // Invalid collection
        // Row 5 left empty intentionally (testing missing endfor_row)
        
        // Sheet 4: ForColumnErrors
        XSSFSheet forColumnErrors = workbook.createSheet("ForColumnErrors");
        XSSFRow row0 = forColumnErrors.createRow(0);
        createCell(row0, 0, "{m:for_column item | data.items}");
        createCell(row0, 1, "{m:item.name}");
        createCell(row0, 2, "{m:endfor_column}");
        XSSFRow row1 = forColumnErrors.createRow(1);
        createCell(row1, 0, "{m:for_column item data.items}");  // Missing pipe
        createCell(row1, 1, "Error");
        XSSFRow row2 = forColumnErrors.createRow(2);
        createCell(row2, 0, "{m:for_column item | data.invalidItems}");  // Invalid collection
        createCell(row2, 1, "Error");
        
        // Sheet 5: MergeErrors
        XSSFSheet mergeErrors = workbook.createSheet("MergeErrors");
        createRow(mergeErrors, 0, "{m:for_row item | data.items}");
        createRow(mergeErrors, 1, "{m:merge_row item}{m:merge_row item}Duplicate");  // Multiple merge directives
        createRow(mergeErrors, 2, "{m:endfor_row}");
        
        // Sheet 6: ExpressionErrors
        XSSFSheet expressionErrors = workbook.createSheet("ExpressionErrors");
        createRow(expressionErrors, 0, "{m:data.value}");  // Valid control
        createRow(expressionErrors, 1, "{m:data.invalidField}");  // Invalid field
        createRow(expressionErrors, 2, "{m:data.value +}");  // Incomplete expression
        createRow(expressionErrors, 3, "{m:");  // Malformed
        
        return workbook;
    }
    
    private static void createRow(XSSFSheet sheet, int rowNum, String cellContent) {
        XSSFRow row = sheet.createRow(rowNum);
        createCell(row, 0, cellContent);
    }
    
    private static void createCell(XSSFRow row, int colNum, String content) {
        XSSFCell cell = row.createCell(colNum);
        cell.setCellValue(content);
    }
}
