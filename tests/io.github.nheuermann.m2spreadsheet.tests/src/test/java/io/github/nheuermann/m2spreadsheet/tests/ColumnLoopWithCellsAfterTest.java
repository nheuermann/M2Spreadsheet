package io.github.nheuermann.m2spreadsheet.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Test;

import io.github.nheuermann.m2spreadsheet.generator.GenerationResult;
import io.github.nheuermann.m2spreadsheet.util.M2SpreadsheetUtils;

/**
 * Test for_column loops with cells AFTER the loop definitions.
 * This tests the scenario where:
 * - Row 0 has for_column definitions
 * - Row 0 has additional cells AFTER the endfor_column markers
 * - These cells should appear ONCE in the output, not repeated
 */
public class ColumnLoopWithCellsAfterTest {

    @Test
    public void testCellsAfterColumnLoop() throws Exception {
        // Create test data
        Map<String, Object> variables = new HashMap<>();
        java.util.List<Map<String, Object>> components = new java.util.ArrayList<>();
        
        Map<String, Object> comp1 = new HashMap<>();
        comp1.put("name", "Component-1");
        java.util.List<Map<String, Object>> fms1 = new java.util.ArrayList<>();
        
        Map<String, Object> fm11 = new HashMap<>();
        fm11.put("name", "FM-1-1");
        fms1.add(fm11);
        
        Map<String, Object> fm12 = new HashMap<>();
        fm12.put("name", "FM-1-2");
        fms1.add(fm12);
        
        comp1.put("failureModes", fms1);
        components.add(comp1);
        
        Map<String, Object> structuredFmea = new HashMap<>();
        structuredFmea.put("components", components);
        variables.put("structuredFmea", structuredFmea);
        
        // Create template workbook
        XSSFWorkbook templateWb = new XSSFWorkbook();
        Sheet sheet = templateWb.createSheet("Test");
        
        // Row 0: Static cell | for_column | ... | endfor_column | Cell After Loop
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Component");  // Column A - BEFORE loop
        row0.createCell(1).setCellValue("{m:for_column component | structuredFmea.components}");  // Column B
        row0.createCell(2).setCellValue("{m:component.name}");  // Column C - loop body
        row0.createCell(3).setCellValue("{m:endfor_column}");  // Column D
        
        Cell cellAfter = row0.createCell(11);  // Column L - AFTER loop
        cellAfter.setCellValue("CELL_AFTER_LOOP");
        
        // Set background color to make it more obvious
        XSSFCellStyle style = templateWb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellAfter.setCellStyle(style);
        
        // Row 1: Should expand based on row 0 loops
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Failure Modes");  // Column A - BEFORE loop (should appear once)
        row1.createCell(11).setCellValue("Data After");  // Column L - AFTER loop (should appear once)
        
        // Save template
        File templateFile = new File("target/test-cells-after-loop-template.xlsx");
        templateFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(templateFile)) {
            templateWb.write(fos);
        }
        templateWb.close();
        
        // Generate
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        File outputFile = new File("target/test-cells-after-loop-output.xlsx");
        URI outputURI = URI.createFileURI(outputFile.getAbsolutePath());
        
        XSSFWorkbook templateWb2;
        try (FileInputStream fis = new FileInputStream(templateFile)) {
            templateWb2 = new XSSFWorkbook(fis);
        }
        
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWb2, queryEnv, variables, resourceSet, outputURI, new BasicMonitor()
        );
        
        // Read result
        XSSFWorkbook resultWb;
        try (FileInputStream fis = new FileInputStream(outputFile)) {
            resultWb = new XSSFWorkbook(fis);
        }
        
        System.out.println("\n=== Test: Cells After Column Loop ===");
        Sheet resultSheet = resultWb.getSheetAt(0);
        
        // Print output
        for (Row row : resultSheet) {
            System.out.print("Row " + row.getRowNum() + ": ");
            for (int c = 0; c < 15; c++) {
                Cell cell = row.getCell(c);
                if (cell != null) {
                    String value = getCellValue(cell);
                    if (value != null && !value.isEmpty()) {
                        System.out.print("C" + c + "=[" + value + "] ");
                    }
                }
            }
            System.out.println();
        }
        
        // Verify row 0
        Row resultRow0 = resultSheet.getRow(0);
        assertNotNull("Row 0 should exist", resultRow0);
        
        // Col 0: "Component" (before loop)
        assertEquals("Component", getCellValue(resultRow0.getCell(0)));
        
        // Cols 1-2: Should have Component-1 name (1 component, 1 iteration)
        assertEquals("Component-1", getCellValue(resultRow0.getCell(1)));
        
        // Find where "CELL_AFTER_LOOP" appears
        int cellAfterLoopCount = 0;
        int firstAfterCol = -1;
        int lastAfterCol = -1;
        
        for (int c = 0; c < resultRow0.getLastCellNum(); c++) {
            Cell cell = resultRow0.getCell(c);
            if (cell != null && "CELL_AFTER_LOOP".equals(getCellValue(cell))) {
                cellAfterLoopCount++;
                if (firstAfterCol == -1) firstAfterCol = c;
                lastAfterCol = c;
            }
        }
        
        System.out.println("\n'CELL_AFTER_LOOP' found " + cellAfterLoopCount + " time(s)");
        System.out.println("First occurrence: column " + firstAfterCol);
        System.out.println("Last occurrence: column " + lastAfterCol);
        
        // CRUCIAL: Should appear only ONCE
        assertEquals("CELL_AFTER_LOOP should appear exactly once", 1, cellAfterLoopCount);
        
        // Verify row 1
        Row resultRow1 = resultSheet.getRow(1);
        assertEquals("Failure Modes", getCellValue(resultRow1.getCell(0)));
        
        int dataAfterCount = 0;
        for (int c = 0; c < resultRow1.getLastCellNum(); c++) {
            Cell cell = resultRow1.getCell(c);
            if (cell != null && "Data After".equals(getCellValue(cell))) {
                dataAfterCount++;
            }
        }
        
        System.out.println("'Data After' found " + dataAfterCount + " time(s)");
        assertEquals        ("Data After should appear exactly once", 1, dataAfterCount);
        
        resultWb.close();
        
        System.out.println("✓ Cells after column loop test passed!");
    }
    
    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}
