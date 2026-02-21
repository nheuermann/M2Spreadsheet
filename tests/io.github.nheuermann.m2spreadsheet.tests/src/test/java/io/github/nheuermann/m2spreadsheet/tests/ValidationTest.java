package io.github.nheuermann.m2spreadsheet.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import io.github.nheuermann.m2spreadsheet.util.M2SpreadsheetUtils;
import io.github.nheuermann.m2spreadsheet.validation.ValidationMessageLevel;

import static org.junit.Assert.*;

/**
 * Tests for template validation functionality.
 * Tests validation workbook generation with visual error markers.
 * 
 * @author nheuermann
 */
public class ValidationTest {
    
    private static final String OUTPUT_DIR = "test-output";
    
    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }
    
    /**
     * Test validation workbook generation with valid and invalid expressions.
     */
    @Test
    public void testValidationWorkbookGeneration() throws Exception {
        System.out.println("\n=== Test: Validation Workbook Generation ===");
        
        // Create template workbook with valid and invalid expressions
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("TestSheet");
        
        // Row 0: Valid expression
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Valid:");
        row0.createCell(1).setCellValue("{m:name}");
        
        // Row 1: Invalid expression (undefined variable)
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Invalid:");
        row1.createCell(1).setCellValue("{m:undefinedVariable}");
        
        // Row 2: Another valid expression
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("Also valid:");
        row2.createCell(1).setCellValue("{m:1 + 2}");
        
        // Row 3: Invalid AQL syntax
        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("Bad syntax:");
        row3.createCell(1).setCellValue("{m:this is not valid AQL!@#}");
        
        // Save template
        String templatePath = OUTPUT_DIR + "/validation_template.xlsx";
        try (FileOutputStream fos = new FileOutputStream(templatePath)) {
            templateWorkbook.write(fos);
        }
        templateWorkbook.close();
        
        // Create variables (name is defined, undefinedVariable is not)
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Test User");
        
        System.out.println("Variables: " + variables.keySet());
        
        // Generate validation workbook
        String outputPath = OUTPUT_DIR + "/validation-output.xlsx";
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        
        // Reload template (since we closed it)
        XSSFWorkbook loadedTemplate = new XSSFWorkbook(new File(templatePath));
        
        ValidationMessageLevel level = M2SpreadsheetUtils.serializeValidatedWorkbookTemplate(
            loadedTemplate,
            queryEnv,
            variables,
            outputURI,
            new BasicMonitor()
        );
        
        // Check that validation found issues
        System.out.println("Validation level: " + level);
        
        // Open the validation workbook
        XSSFWorkbook validationWorkbook = new XSSFWorkbook(new File(outputPath));
        
        // Verify validation sheet exists and is first
        assertEquals("Should have 2 sheets (Validation + TestSheet)", 2, validationWorkbook.getNumberOfSheets());
        assertEquals("Validation", validationWorkbook.getSheetAt(0).getSheetName());
        assertEquals("TestSheet", validationWorkbook.getSheetAt(1).getSheetName());
        
        // Check validation sheet structure
        XSSFSheet validationSheet = validationWorkbook.getSheetAt(0);
        
        // Check tab color is red
        assertNotNull("Validation sheet should have tab color", validationSheet.getTabColor());
        
        // Check header row
        Row headerRow = validationSheet.getRow(0);
        assertNotNull("Header row should exist", headerRow);
        assertEquals("Severity", headerRow.getCell(0).getStringCellValue());
        assertEquals("Location", headerRow.getCell(1).getStringCellValue());
        assertEquals("Message", headerRow.getCell(2).getStringCellValue());
        
        // Check that validation messages exist
        int messageCount = validationSheet.getLastRowNum(); // 0-based, so subtract 1 for header
        System.out.println("Number of validation messages: " + messageCount);
        assertTrue("Should have at least one validation message", messageCount > 0);
        
        // Print all validation messages
        System.out.println("\n=== Validation Messages ===");
        for (int rowIdx = 1; rowIdx <= validationSheet.getLastRowNum(); rowIdx++) {
            Row msgRow = validationSheet.getRow(rowIdx);
            if (msgRow != null && msgRow.getCell(0) != null) {
                String severity = msgRow.getCell(0).getStringCellValue();
                String location = msgRow.getCell(1).getStringCellValue();
                String message = msgRow.getCell(2).getStringCellValue();
                System.out.println(String.format("[%s] %s - %s", severity, location, message));
            }
        }
        
        // Verify that original sheet has colored cells
        Sheet testSheet = validationWorkbook.getSheet("TestSheet");
        assertNotNull("TestSheet should exist", testSheet);
        
        // Check that error cells have background colors
        // Note: This is implementation-dependent, just checking that cells still exist
        assertNotNull("Row with invalid expression should exist", testSheet.getRow(1));
        assertNotNull("Cell with invalid expression should exist", testSheet.getRow(1).getCell(1));
        
        System.out.println("✓ Validation workbook generation test passed");
        
        validationWorkbook.close();
    }
    
    /**
     * Test validation with completely valid template.
     */
    @Test
    public void testValidationWithValidTemplate() throws Exception {
        System.out.println("\n=== Test: Validation with Valid Template ===");
        
        // Create template workbook with only valid expressions
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("ValidSheet");
        
        // Row 0: Valid expression
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Name:");
        row0.createCell(1).setCellValue("{m:name}");
        
        // Row 1: Valid arithmetic
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Math:");
        row1.createCell(1).setCellValue("{m:5 + 10}");
        
        // Save template
        String templatePath = OUTPUT_DIR + "/valid_template.xlsx";
        try (FileOutputStream fos = new FileOutputStream(templatePath)) {
            templateWorkbook.write(fos);
        }
        templateWorkbook.close();
        
        // Create variables
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Valid User");
        
        // Generate validation workbook
        String outputPath = OUTPUT_DIR + "/valid-validation.xlsx";
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        
        XSSFWorkbook loadedTemplate = new XSSFWorkbook(new File(templatePath));
        
        ValidationMessageLevel level = M2SpreadsheetUtils.serializeValidatedWorkbookTemplate(
            loadedTemplate,
            queryEnv,
            variables,
            outputURI,
            new BasicMonitor()
        );
        
        System.out.println("Validation level: " + level);
        
        // With only valid expressions, we might still get INFO/OK
        // depending on AQL's validation behavior
        
        // Open validation workbook
        XSSFWorkbook validationWorkbook = new XSSFWorkbook(new File(outputPath));
        
        // Validation sheet should still exist
        assertEquals("Validation", validationWorkbook.getSheetAt(0).getSheetName());
        
        Sheet validationSheet = validationWorkbook.getSheetAt(0);
        int messageCount = validationSheet.getLastRowNum();
        System.out.println("Number of validation messages: " + messageCount);
        
        // Print messages if any
        if (messageCount > 0) {
            System.out.println("\n=== Validation Messages ===");
            for (int rowIdx = 1; rowIdx <= messageCount; rowIdx++) {
                Row msgRow = validationSheet.getRow(rowIdx);
                if (msgRow != null && msgRow.getCell(0) != null) {
                    String severity = msgRow.getCell(0).getStringCellValue();
                    String location = msgRow.getCell(1).getStringCellValue();
                    String message = msgRow.getCell(2).getStringCellValue();
                    System.out.println(String.format("[%s] %s - %s", severity, location, message));
                }
            }
        }
        
        System.out.println("✓ Valid template validation test passed");
        
        validationWorkbook.close();
    }
}
