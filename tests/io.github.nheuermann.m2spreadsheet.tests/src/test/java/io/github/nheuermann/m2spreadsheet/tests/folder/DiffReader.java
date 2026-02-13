package io.github.nheuermann.m2spreadsheet.tests.folder;

import java.io.FileInputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Simple utility to read and display diff file contents.
 */
public class DiffReader {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: DiffReader <path-to-diff.xlsx>");
            System.exit(1);
        }
        
        String filePath = args[0];
        System.out.println("Reading diff file: " + filePath);
        System.out.println();
        
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("=== Sheet: " + sheet.getSheetName() + " ===");
                
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        String value = getCellValue(cell);
                        if (value != null && !value.trim().isEmpty()) {
                            String cellRef = cell.getAddress().formatAsString();
                            System.out.println(cellRef + ": " + value);
                        }
                    }
                }
                System.out.println();
            }
        }
    }
    
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return "FORMULA: " + cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return cell.toString();
        }
    }
}
