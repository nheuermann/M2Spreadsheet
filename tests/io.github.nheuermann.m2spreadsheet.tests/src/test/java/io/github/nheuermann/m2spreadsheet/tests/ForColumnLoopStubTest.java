package io.github.nheuermann.m2spreadsheet.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
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
 * Tests for for_column loop implementation.
 * 
 * These tests verify that:
 * 1. The syntax is recognized
 * 2. for_column generates columns without warnings
 * 3. Templates with and without for_column work correctly
 * 
 * NOTE: See ForColumnLoopTest for comprehensive for_column testing.
 * 
 * @author nheuermann
 */
public class ForColumnLoopStubTest {
    
    private static final String OUTPUT_DIR = "target/test-output";
    
    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }
    
    /**
     * Test that for_column syntax is recognized and generates output correctly.
     */
    @Test
    public void testForColumnLoopWorks() throws Exception {
        System.out.println("\n=== Test: for_column Loop Works ===");
        
        // Create template with for_column in first row
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row0 = sheet.createRow(0);
        
        // Template: {m:for_column item | items} ... {m:endfor_column}
        row0.createCell(0).setCellValue("{m:for_column item | items}");
        row0.createCell(1).setCellValue("{m:item}");
        row0.createCell(2).setCellValue("{m:endfor_column}");
        
        // Add a second row with regular content
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Regular content");
        
        // Setup variables
        List<String> items = Arrays.asList("apple", "banana", "cherry");
        Map<String, Object> variables = new HashMap<>();
        variables.put("items", items);
        
        // Setup query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = OUTPUT_DIR + "/for-column-basic-test.xlsx";
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        // Verify: Generation should succeed
        assertTrue("Generation should succeed with implemented for_column", 
                   result.isSuccessful());
        
        // Verify: Should NOT have warnings about for_column (it's now implemented)
        for (io.github.nheuermann.m2spreadsheet.validation.TemplateValidationMessage message : result.getValidationMessages()) {
            System.out.println("Validation message: " + message);
            assertFalse("Should not have warnings about for_column not being implemented",
                       message.getMessage().contains("for_column") && message.getMessage().contains("not yet implemented"));
        }
        
        // Verify: Output file exists
        assertTrue("Output file should be created", new File(outputPath).exists());
        
        templateWorkbook.close();
        
        System.out.println("✓ for_column test passed");
        System.out.println("  - Syntax recognized");
        System.out.println("  - No warnings generated");
        System.out.println("  - Processing completed successfully");
        System.out.println("  - See ForColumnLoopTest for comprehensive tests");
    }
    
    /**
     * Test that templates without for_column work normally.
     * This ensures the stub doesn't break existing functionality.
     */
    @Test
    public void testNormalTemplateStillWorks() throws Exception {
        System.out.println("\n=== Test: Normal Template (No for_column) ===");
        
        // Create template without for_column
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Name: {m:name}");
        
        // Setup variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Test User");
        
        // Setup query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = OUTPUT_DIR + "/normal-template-test.xlsx";
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        // Verify: Generation should succeed without warnings
        assertTrue("Generation should succeed", result.isSuccessful());
        
        // Should have no for_column warnings
        boolean hasForColumnWarning = false;
        for (io.github.nheuermann.m2spreadsheet.validation.TemplateValidationMessage message : result.getValidationMessages()) {
            if (message.getMessage().contains("for_column")) {
                hasForColumnWarning = true;
            }
        }
        assertFalse("Should not have for_column warnings", hasForColumnWarning);
        
        templateWorkbook.close();
        
        System.out.println("✓ Normal template works without for_column warnings");
    }
    
    /**
     * Test that for_column in any row is handled correctly.
     * for_column can now be used in any row, including within for_row loops.
     */
    @Test
    public void testForColumnInAnyRow() throws Exception {
        System.out.println("\n=== Test: for_column in Any Row ===");
        
        // Create template with for_column in second row
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Header");
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("{m:for_column item | items}");
        row1.createCell(1).setCellValue("{m:item}");
        row1.createCell(2).setCellValue("{m:endfor_column}");
        
        // Setup variables
        List<String> items = Arrays.asList("apple", "banana");
        Map<String, Object> variables = new HashMap<>();
        variables.put("items", items);
        
        // Setup query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = OUTPUT_DIR + "/for-column-any-row.xlsx";
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        // Should succeed (for_column can be used in any row)
        assertTrue("Generation should succeed", result.isSuccessful());
        
        templateWorkbook.close();
        
        System.out.println("✓ for_column in any row works correctly");
        System.out.println("  Note: for_column can be used anywhere, including within for_row loops");
    }
}
