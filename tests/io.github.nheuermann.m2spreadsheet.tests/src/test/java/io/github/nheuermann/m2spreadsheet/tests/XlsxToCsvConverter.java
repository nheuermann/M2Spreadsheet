package io.github.nheuermann.m2spreadsheet.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility to convert XLSX files to CSV for debugging purposes.
 * 
 * Usage: 
 * - From command line: java XlsxToCsvConverter input.xlsx output.csv
 * - From code: XlsxToCsvConverter.convert("input.xlsx", "output.csv")
 * 
 * @author nheuermann
 */
public class XlsxToCsvConverter {
    
    /**
     * Convert an XLSX file to CSV.
     * 
     * @param xlsxPath path to the .xlsx file
     * @param csvPath path for the output .csv file
     * @throws IOException if reading or writing fails
     */
    public static void convert(String xlsxPath, String csvPath) throws IOException {
        System.out.println("Converting: " + xlsxPath + " -> " + csvPath);
        
        try (FileInputStream fis = new FileInputStream(xlsxPath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis);
             FileOutputStream fos = new FileOutputStream(csvPath);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            
            // Convert first sheet
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("Sheet: " + sheet.getSheetName());
            System.out.println("Rows: " + (sheet.getLastRowNum() + 1));
            
            int rowCount = 0;
            int lastRowNum = sheet.getLastRowNum();
            
            for (int rowNum = 0; rowNum <= lastRowNum; rowNum++) {
                Row row = sheet.getRow(rowNum);
                
                if (row == null) {
                    writer.write("\n");
                    continue;
                }
                
                short lastCellNum = row.getLastCellNum();
                for (int cellNum = 0; cellNum < lastCellNum; cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    
                    if (cellNum > 0) {
                        writer.write("\t");
                    }
                    
                    if (cell != null) {
                        String value = getCellValue(cell);
                        // Escape tabs and newlines
                        value = value.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
                        writer.write(value);
                    }
                }
                
                writer.write("\n");
                rowCount++;
            }
            
            System.out.println("Converted " + rowCount + " rows");
            System.out.println("CSV written to: " + csvPath);
        }
    }
    
    /**
     * Get the string value of a cell.
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        CellType cellType = cell.getCellType();
        
        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Format numeric values
                double numValue = cell.getNumericCellValue();
                if (numValue == (long) numValue) {
                    return String.valueOf((long) numValue);
                } else {
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            case ERROR:
                return "ERROR:" + cell.getErrorCellValue();
            default:
                return "";
        }
    }
    
    /**
     * Convert an XLSX file to CSV, printing to stdout.
     * 
     * @param xlsxPath path to the .xlsx file
     * @throws IOException if reading fails
     */
    public static void printAsCsv(String xlsxPath) throws IOException {
        System.out.println("\n=== " + xlsxPath + " ===\n");
        
        try (FileInputStream fis = new FileInputStream(xlsxPath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            
            System.out.println("Sheet: " + sheet.getSheetName() + " (" + (lastRowNum + 1) + " rows)\n");
            
            for (int rowNum = 0; rowNum <= lastRowNum; rowNum++) {
                Row row = sheet.getRow(rowNum);
                
                System.out.printf("Row %3d: ", rowNum);
                
                if (row == null) {
                    System.out.println("[NULL ROW]");
                    continue;
                }
                
                short lastCellNum = row.getLastCellNum();
                for (int cellNum = 0; cellNum < lastCellNum; cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    
                    if (cellNum > 0) {
                        System.out.print("\t");
                    }
                    
                    if (cell != null) {
                        String value = getCellValue(cell);
                        // Truncate long values for display
                        if (value.length() > 50) {
                            value = value.substring(0, 47) + "...";
                        }
                        System.out.print(value);
                    } else {
                        System.out.print("[NULL]");
                    }
                }
                
                System.out.println();
            }
            
            System.out.println("\n=== End ===\n");
        }
    }
    
    /**
     * Main method for command-line usage.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java XlsxToCsvConverter <input.xlsx> [output.csv]");
            System.err.println("  If output.csv is omitted, prints to stdout");
            System.exit(1);
        }
        
        String xlsxPath = args[0];
        
        try {
            if (args.length >= 2) {
                String csvPath = args[1];
                convert(xlsxPath, csvPath);
            } else {
                printAsCsv(xlsxPath);
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
