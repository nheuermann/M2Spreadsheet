package io.github.nheuermann.m2spreadsheet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Before;
import org.junit.Test;

import io.github.nheuermann.m2spreadsheet.generator.GenerationResult;
import io.github.nheuermann.m2spreadsheet.util.M2SpreadsheetUtils;

/**
 * Tests for FMEA template generation with for loop support.
 * Tests that the template correctly repeats rows for each FMEA entry.
 * 
 * @author nheuermann
 */
public class FmeaTemplateTest {
    
    private static final String TEMPLATE_PATH = "src/test/resources/templates/fmea-table-template.xlsx";
    private static final String OUTPUT_PATH = "target/test-output/fmea-generated.xlsx";
    
    /**
     * Setup test output directory.
     */
    @Before
    public void setUp() {
        new File("target/test-output").mkdirs();
    }
    
    /**
     * Test FMEA template generation with for_row loop.
     * Template structure:
     * - Row 1: Header row
     * - Row 2: {m:for_row c | fmeaEntries}
     * - Row 3: Data row with {m:c.field} expressions
     * - Row 4: {m:endfor_row}
     * 
     * Expected output:
     * - Row 1: Header row (preserved)
     * - Rows 2-101: 100 data rows (one per FMEA entry)
     */
    @Test
    public void testFmeaTemplateWithForLoop() throws Exception {
        File templateFile = new File(TEMPLATE_PATH);
        
        if (!templateFile.exists()) {
            System.out.println("SKIP: FMEA template file not found: " + TEMPLATE_PATH);
            System.out.println("      Expected user to upload the file to this location.");
            return;
        }
        
        System.out.println("Loading FMEA template from: " + templateFile.getAbsolutePath());
        
        // Load template
        try (FileInputStream fis = new FileInputStream(templateFile);
             XSSFWorkbook templateWorkbook = new XSSFWorkbook(fis)) {
            
            // Setup: Variables with FMEA entries from MockModelData
            List<Map<String, Object>> fmeaEntries = MockModelData.getFmeaEntries();
            Map<String, Object> variables = new HashMap<>();
            variables.put("fmeaEntries", fmeaEntries);
            
            System.out.println("FMEA entries count: " + fmeaEntries.size());
            
            // Setup: Query environment
            ResourceSet resourceSet = new ResourceSetImpl();
            IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
            
            // Generate
            URI outputURI = URI.createFileURI(new File(OUTPUT_PATH).getAbsolutePath());
            GenerationResult result = M2SpreadsheetUtils.generate(
                templateWorkbook,
                queryEnv,
                variables,
                resourceSet,
                outputURI,
                new BasicMonitor()
            );
            
            // Verify: Generation successful
            assertTrue("Generation should succeed", result.isSuccessful());
            assertTrue("Output file should exist", new File(OUTPUT_PATH).exists());
            
            System.out.println("✓ FMEA spreadsheet generated successfully: " + OUTPUT_PATH);
            
            // Verify: Output content
            try (FileInputStream fis2 = new FileInputStream(OUTPUT_PATH);
                 XSSFWorkbook outputWorkbook = new XSSFWorkbook(fis2)) {
                
                Sheet outputSheet = outputWorkbook.getSheetAt(0);
                assertNotNull("Output sheet should exist", outputSheet);
                
                // Verify row count: Header (1) + FMEA entries (100) = 101 rows
                // Note: getLastRowNum() returns 0-based index, so for 101 rows it returns 100
                int expectedLastRow = 100; // 0-based: rows 0-100 = 101 rows
                int actualLastRow = outputSheet.getLastRowNum();
                
                System.out.println("Output rows: " + (actualLastRow + 1) + " (expected: " + (expectedLastRow + 1) + ")");
                
                // Should have at least 101 rows (header + 100 FMEA entries)
                assertTrue("Should have at least 101 rows", actualLastRow >= expectedLastRow);
                
                // Verify first data row (row 1, after header)
                Row firstDataRow = outputSheet.getRow(1);
                assertNotNull("First data row should exist", firstDataRow);
                
                Cell firstCell = firstDataRow.getCell(0);
                assertNotNull("First cell should exist", firstCell);
                
                String firstItemNumber = firstCell.getStringCellValue();
                assertEquals("First FMEA entry should be FMEA-001", "FMEA-001", firstItemNumber);
                
                System.out.println("✓ First data row item number: " + firstItemNumber);
                
                // Verify second column (component)
                Cell componentCell = firstDataRow.getCell(1);
                assertNotNull("Component cell should exist", componentCell);
                
                String component = componentCell.getStringCellValue();
                assertEquals("First entry component should be Microcontroller", "Microcontroller", component);
                
                System.out.println("✓ First data row component: " + component);
                
                // Verify last data row (row 100)
                Row lastDataRow = outputSheet.getRow(100);
                assertNotNull("Last data row should exist", lastDataRow);
                
                Cell lastCell = lastDataRow.getCell(0);
                assertNotNull("Last cell should exist", lastCell);
                
                String lastItemNumber = lastCell.getStringCellValue();
                assertEquals("Last FMEA entry should be FMEA-100", "FMEA-100", lastItemNumber);
                
                System.out.println("✓ Last data row item number: " + lastItemNumber);
                
                System.out.println("✓ All FMEA data rows verified successfully!");
            }
        }
    }
    
    /**
     * Test that for loop properly evaluates nested expressions.
     */
    @Test
    public void testForLoopExpressionEvaluation() throws Exception {
        // Create a simple template with for loop programmatically
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        
        // Row 0: Header
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Item Number");
        headerRow.createCell(1).setCellValue("Component");
        headerRow.createCell(2).setCellValue("RPN");
        
        // Row 1: for_row command
        Row forRow = sheet.createRow(1);
        forRow.createCell(0).setCellValue("{m:for_row entry | fmeaEntries}");
        
        // Row 2: data template
        Row dataRow = sheet.createRow(2);
        dataRow.createCell(0).setCellValue("{m:entry.item_number}");
        dataRow.createCell(1).setCellValue("{m:entry.component}");
        dataRow.createCell(2).setCellValue("{m:entry.rpn}");
        
        System.out.println("\n=== Template Structure ===");
        System.out.println("Row 2 cells:");
        for (int i = 0; i < 3; i++) {
            Cell c = dataRow.getCell(i);
            System.out.println("  Cell " + i + ": " + (c != null ? c.getStringCellValue() : "NULL"));
        }
        System.out.println("Row 2 lastCellNum: " + dataRow.getLastCellNum());
        System.out.println("=======================\n");
        
        // Row 3: endfor_row
        Row endforRow = sheet.createRow(3);
        endforRow.createCell(0).setCellValue("{m:endfor_row}");
        
        // Variables: Only first 3 FMEA entries for testing
        List<Map<String, Object>> allFmeaEntries = MockModelData.getFmeaEntries();
        List<Map<String, Object>> testEntries = allFmeaEntries.subList(0, 3);
        
        System.out.println("\n=== Test Data ===");
        for (int i = 0; i < testEntries.size(); i++) {
            Map<String, Object> entry = testEntries.get(i);
            System.out.println("Entry " + i + ": item_number=" + entry.get("item_number") 
                + ", component=" + entry.get("component") 
                + ", rpn=" + entry.get("rpn"));
        }
        System.out.println("=================\n");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("fmeaEntries", testEntries);
        
        // Setup query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = "target/test-output/fmea-simple-for-loop.xlsx";
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        templateWorkbook.close();
        
        // Debug: Print any errors or warnings
        if (!result.isSuccessful() || !result.getGenerationErrors().isEmpty() || !result.getValidationMessages().isEmpty()) {
            System.err.println("\n=== Generation Result ===");
            System.err.println("Successful: " + result.isSuccessful());
            if (!result.getGenerationErrors().isEmpty()) {
                System.err.println("Errors:");
                for (Exception e : result.getGenerationErrors()) {
                    System.err.println("  - " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if (!result.getValidationMessages().isEmpty()) {
                System.err.println("Warnings:");
                for (String msg : result.getValidationMessages()) {
                    System.err.println("  - " + msg);
                }
            }
            System.err.println("=======================\n");
        }
        
        // Debug: Convert to CSV for inspection
        System.out.println("\n=== Converting generated file to CSV for inspection ===");
        try {
            XlsxToCsvConverter.printAsCsv(outputPath);
        } catch (Exception e) {
            System.err.println("Failed to convert to CSV: " + e.getMessage());
        }
        
        // Verify
        assertTrue("Generation should succeed: " + 
            (result.getGenerationErrors().isEmpty() ? "no errors" : 
             result.getGenerationErrors().get(0).getMessage()), 
            result.isSuccessful());
        
        try (FileInputStream fis = new FileInputStream(outputPath);
             XSSFWorkbook outputWorkbook = new XSSFWorkbook(fis)) {
            
            Sheet outputSheet = outputWorkbook.getSheetAt(0);
            
            // Debug: print what was actually generated
            System.out.println("\n=== Generated Output ===");
            System.out.println("Last row num: " + outputSheet.getLastRowNum());
            for (int i = 0; i <= Math.min(5, outputSheet.getLastRowNum()); i++) {
                 Row row = outputSheet.getRow(i);
                if (row != null) {
                    System.out.print("Row " + i + ": ");
                    for (int j = 0; j < 3; j++) {
                        Cell cell = row.getCell(j);
                        System.out.print("[" + j + ":" + (cell != null ? cell.getStringCellValue() : "NULL") + "] ");
                    }
                    System.out.println();
                } else {
                    System.out.println("Row " + i + ": NULL");
                }
            }
            System.out.println("======================\n");
            
            // Should have 4 rows: Header + 3 data rows
            assertEquals("Should have 4 rows (0-based = 3)", 3, outputSheet.getLastRowNum());
            
            // Verify each data row
            for (int i = 0; i < 3; i++) {
                Row row = outputSheet.getRow(i + 1); // Skip header
                assertNotNull("Row " + (i + 1) + " should exist", row);
                
                Cell itemNumberCell = row.getCell(0);
                Cell componentCell = row.getCell(1);
                Cell rpnCell = row.getCell(2);
                
                assertNotNull("Item number cell should exist at row " + (i + 1), itemNumberCell);
                assertNotNull("Component cell should exist at row " + (i + 1), componentCell);
                assertNotNull("RPN cell should exist at row " + (i + 1), rpnCell);
                
                String itemNumber = itemNumberCell.getStringCellValue();
                String component = componentCell.getStringCellValue();
                String rpn = rpnCell.getStringCellValue();
                
                Map<String, Object> expectedEntry = testEntries.get(i);
                assertEquals("Item number mismatch at row " + (i + 1), 
                    expectedEntry.get("item_number"), itemNumber);
                assertEquals("Component mismatch at row " + (i + 1), 
                    expectedEntry.get("component"), component);
                assertEquals("RPN mismatch at row " + (i + 1), 
                    expectedEntry.get("rpn"), rpn);
                
                System.out.println("✓ Row " + (i + 1) + ": " + itemNumber + " - " + component + " - RPN: " + rpn);
            }
        }
        
        System.out.println("✓ For loop expression evaluation test passed!");
    }
}
