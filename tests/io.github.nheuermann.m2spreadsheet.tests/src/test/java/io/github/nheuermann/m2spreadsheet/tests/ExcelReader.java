package io.github.nheuermann.m2spreadsheet.tests;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;

/**
 * Utility to dump Excel file content for debugging.
 */
public class ExcelReader {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: ExcelReader <filename>");
            return;
        }
        
        String filename = args[0];
        File file = new File(filename);
        
        if (!file.exists()) {
            System.out.println("File not found: " + filename);
            return;
        }
        
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            System.out.println("=== Excel File: " + filename + " ===");
            System.out.println("Number of sheets: " + workbook.getNumberOfSheets());
            System.out.println();
            
            for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
                Sheet sheet = workbook.getSheetAt(sheetIdx);
                System.out.println("--- Sheet " + sheetIdx + ": " + sheet.getSheetName() + " ---");
                
                if (sheet.getPhysicalNumberOfRows() == 0) {
                    System.out.println("(empty sheet)");
                    System.out.println();
                    continue;
                }
                
                int lastRowNum = sheet.getLastRowNum();
                System.out.println("Last row num: " + lastRowNum);
                
                // Show detailed column mapping for first 5 rows
                for (int rowIdx = 0; rowIdx <= Math.min(lastRowNum, 4); rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) {
                        System.out.println("Row " + rowIdx + ": (null)");
                        continue;
                    }
                    
                    short lastCellNum = row.getLastCellNum();
                    System.out.println("Row " + rowIdx + " (lastCellNum=" + lastCellNum + "):");
                    for (int colIdx = 0; colIdx < Math.min(lastCellNum, 25); colIdx++) {
                        Cell cell = row.getCell(colIdx);
                        String content = getCellContent(cell);
                        if (content != null && !content.trim().isEmpty()) {
                            System.out.println("  [" + colIdx + "]: " + content);
                        }
                    }
                }
                
                System.out.println();
            }
        }
    }
    
    private static String getCellContent(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            case ERROR:
                return "ERROR";
            default:
                return "";
        }
    }
}
