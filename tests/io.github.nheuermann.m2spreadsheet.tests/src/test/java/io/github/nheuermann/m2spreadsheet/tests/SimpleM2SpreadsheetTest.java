package io.github.nheuermann.m2spreadsheet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
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
 * Tests for M2Spreadsheet basic functionality.
 * Tests the generation of Excel spreadsheets from templates with AQL expressions.
 * 
 * @author nheuermann
 */
public class SimpleM2SpreadsheetTest {
    
    private static final String TEMPLATE_PATH = "src/test/resources/templates/simple-template.xlsx";
    private static final String OUTPUT_PATH = "target/test-output/simple-generated.xlsx";
    
    /**
     * Setup test output directory.
     */
    @Before
    public void setUp() {
        new File("target/test-output").mkdirs();
    }
    
    /**
     * Test that we can create a template with an expression and generate from it.
     * Template: Cell A1 contains "Name: {m:customer.name} from company {m:customer.company}"
     */
    @Test
    public void testSimpleCustomerTemplate() throws Exception {
        // Setup: Create template workbook programmatically
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Data");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Name: {m:customer.name} from company {m:customer.company}");
        
        // Setup: Variables - using nested Map to represent customer object
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "John Doe");
        customer.put("company", "Acme Corp");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customer", customer);
        
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
        
        // Verify: Output content
        try (FileInputStream fis = new FileInputStream(OUTPUT_PATH);
             XSSFWorkbook outputWorkbook = new XSSFWorkbook(fis)) {
            
            Sheet outputSheet = outputWorkbook.getSheetAt(0);
            assertNotNull("Output sheet should exist", outputSheet);
            
            Row outputRow = outputSheet.getRow(0);
            assertNotNull("Output row should exist", outputRow);
            
            Cell outputCell = outputRow.getCell(0);
            assertNotNull("Output cell should exist", outputCell);
            
            String expectedValue = "Name: John Doe from company Acme Corp";
            String actualValue = outputCell.getStringCellValue();
            
            assertEquals("Cell value should be evaluated", expectedValue, actualValue);
        }
        
        templateWorkbook.close();
        
        System.out.println("✓ Spreadsheet generated successfully: " + OUTPUT_PATH);
        System.out.println("✓ Template: 'Name: {m:customer.name} from company {m:customer.company}'");
        System.out.println("✓ Result: 'Name: John Doe from company Acme Corp'");
    }
    
    /**
     * Test with an actual template file (if it exists).
     */
    @Test
    public void testWithTemplateFile() throws Exception {
        File templateFile = new File(TEMPLATE_PATH);
        
        if (!templateFile.exists()) {
            System.out.println("SKIP: Template file not found: " + TEMPLATE_PATH);
            System.out.println("      This test will run when the template file is created.");
            return;
        }
        
        // Load template
        try (FileInputStream fis = new FileInputStream(templateFile);
             XSSFWorkbook templateWorkbook = new XSSFWorkbook(fis)) {
            
            // Setup: Variables
            Map<String, Object> customer = new HashMap<>();
            customer.put("name", "Jane Smith");
            customer.put("company", "TechCorp Inc");
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("customer", customer);
            
            // Setup: Query environment
            ResourceSet resourceSet = new ResourceSetImpl();
            IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
            
            // Generate
            String outputPath = "target/test-output/from-file-generated.xlsx";
            URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
            GenerationResult result = M2SpreadsheetUtils.generate(
                templateWorkbook,
                queryEnv,
                variables,
                resourceSet,
                outputURI,
                new BasicMonitor()
            );
            
            // Verify
            assertTrue("Generation should succeed", result.isSuccessful());
            assertTrue("Output file should exist", new File(outputPath).exists());
            
            System.out.println("✓ Generated from template file: " + outputPath);
        }
    }
    
    /**
     * Test multiple expressions in different cells.
     */
    @Test
    public void testMultipleCells() throws Exception {
        // Create template with multiple cells
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Customers");
        
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("Company");
        
        // Data row with expressions
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("{m:customer.name}");
        dataRow.createCell(1).setCellValue("{m:customer.company}");
        
        // Setup: Variables
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "Alice Johnson");
        customer.put("company", "Digital Solutions");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customer", customer);
        
        // Setup: Query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = "target/test-output/multiple-cells.xlsx";
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        // Verify
        assertTrue("Generation should succeed", result.isSuccessful());
        
        // Verify content
        try (FileInputStream fis = new FileInputStream(outputPath);
             XSSFWorkbook outputWorkbook = new XSSFWorkbook(fis)) {
            
            Sheet outputSheet = outputWorkbook.getSheetAt(0);
            Row outputDataRow = outputSheet.getRow(1);
            
            assertEquals("Name should be evaluated", "Alice Johnson", 
                outputDataRow.getCell(0).getStringCellValue());
            assertEquals("Company should be evaluated", "Digital Solutions", 
                outputDataRow.getCell(1).getStringCellValue());
        }
        
        templateWorkbook.close();
        
        System.out.println("✓ Multiple cells generated successfully: " + outputPath);
    }
}
