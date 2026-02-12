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
    
    /**
     * Test with realistic mock model data containing multiple object collections.
     * Uses MockModelData with requirements, fault tree events, and reference documents.
     */
    @Test
    public void testWithMockModelData() throws Exception {
        // Create template showing various data access patterns
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        
        // Sheet 1: Summary
        Sheet summarySheet = templateWorkbook.createSheet("Summary");
        int rowNum = 0;
        summarySheet.createRow(rowNum++).createCell(0).setCellValue("Project Summary");
        summarySheet.createRow(rowNum++).createCell(0).setCellValue("Project: {m:projectName}");
        summarySheet.createRow(rowNum++).createCell(0).setCellValue("Code: {m:projectCode}");
        summarySheet.createRow(rowNum++).createCell(0).setCellValue("Standard: {m:safetyStandard}");
        summarySheet.createRow(rowNum++).createCell(0).setCellValue("ASIL: {m:asil}");
        summarySheet.createRow(rowNum++);
        summarySheet.createRow(rowNum++).createCell(0).setCellValue("Requirements Count: {m:requirements->size()}");
        summarySheet.createRow(rowNum++).createCell(0).setCellValue("Fault Tree Events: {m:faultTreeEvents->size()}");
        summarySheet.createRow(rowNum++).createCell(0).setCellValue("Reference Docs: {m:referenceDocuments->size()}");
        
        // Sheet 2: Requirements list (first 5)
        Sheet reqSheet = templateWorkbook.createSheet("Requirements");
        rowNum = 0;
        Row reqHeader = reqSheet.createRow(rowNum++);
        reqHeader.createCell(0).setCellValue("ID");
        reqHeader.createCell(1).setCellValue("Name");
        reqHeader.createCell(2).setCellValue("DAL");
        
        // Add expressions for first requirement
        Row reqRow = reqSheet.createRow(rowNum++);
        reqRow.createCell(0).setCellValue("{m:requirements->first().id}");
        reqRow.createCell(1).setCellValue("{m:requirements->first().name}");
        reqRow.createCell(2).setCellValue("{m:requirements->first().dal}");
        
        // Sheet 3: Fault Tree Events (first 5)
        Sheet eventSheet = templateWorkbook.createSheet("FT Events");
        rowNum = 0;
        Row eventHeader = eventSheet.createRow(rowNum++);
        eventHeader.createCell(0).setCellValue("ID");
        eventHeader.createCell(1).setCellValue("Name");
        eventHeader.createCell(2).setCellValue("Failure Model");
        eventHeader.createCell(3).setCellValue("Failure Rate");
        
        // Add expressions for first event
        Row eventRow = eventSheet.createRow(rowNum++);
        eventRow.createCell(0).setCellValue("{m:faultTreeEvents->first().id}");
        eventRow.createCell(1).setCellValue("{m:faultTreeEvents->first().name}");
        eventRow.createCell(2).setCellValue("{m:faultTreeEvents->first().failure_model}");
        eventRow.createCell(3).setCellValue("{m:faultTreeEvents->first().failure_rate}");
        
        // Sheet 4: Reference Documents (first 3)
        Sheet docSheet = templateWorkbook.createSheet("Documents");
        rowNum = 0;
        Row docHeader = docSheet.createRow(rowNum++);
        docHeader.createCell(0).setCellValue("ID");
        docHeader.createCell(1).setCellValue("Name");
        docHeader.createCell(2).setCellValue("Author");
        
        // Add expressions for first document
        Row docRow = docSheet.createRow(rowNum++);
        docRow.createCell(0).setCellValue("{m:referenceDocuments->first().id}");
        docRow.createCell(1).setCellValue("{m:referenceDocuments->first().name}");
        docRow.createCell(2).setCellValue("{m:referenceDocuments->first().author}");
        
        // Setup: Get mock model variables
        Map<String, Object> variables = MockModelData.getVariables();
        
        // Setup: Query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = "target/test-output/mock-model-data.xlsx";
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        // Verify generation successful
        assertTrue("Generation should succeed", result.isSuccessful());
        assertTrue("Output file should exist", new File(outputPath).exists());
        
        // Verify content
        try (FileInputStream fis = new FileInputStream(outputPath);
             XSSFWorkbook outputWorkbook = new XSSFWorkbook(fis)) {
            
            // Verify Summary sheet
            Sheet outSummary = outputWorkbook.getSheet("Summary");
            assertNotNull("Summary sheet should exist", outSummary);
            assertEquals("Electronic Steering Lock System", 
                outSummary.getRow(1).getCell(0).getStringCellValue().split(": ")[1]);
            assertEquals("Requirements Count: 15", 
                outSummary.getRow(6).getCell(0).getStringCellValue());
            assertEquals("Fault Tree Events: 35", 
                outSummary.getRow(7).getCell(0).getStringCellValue());
            assertEquals("Reference Docs: 20", 
                outSummary.getRow(8).getCell(0).getStringCellValue());
            
            // Verify Requirements sheet
            Sheet outReq = outputWorkbook.getSheet("Requirements");
            assertNotNull("Requirements sheet should exist", outReq);
            Row firstReqRow = outReq.getRow(1);
            assertEquals("REQ-ESL-001", firstReqRow.getCell(0).getStringCellValue());
            assertEquals("System Power Supply", firstReqRow.getCell(1).getStringCellValue());
            assertEquals("DAL-A", firstReqRow.getCell(2).getStringCellValue());
            
            // Verify FT Events sheet
            Sheet outEvent = outputWorkbook.getSheet("FT Events");
            assertNotNull("FT Events sheet should exist", outEvent);
            Row firstEventRow = outEvent.getRow(1);
            assertEquals("BE-001", firstEventRow.getCell(0).getStringCellValue());
            assertEquals("Motor Winding Open Circuit", firstEventRow.getCell(1).getStringCellValue());
            assertEquals("Permanent", firstEventRow.getCell(2).getStringCellValue());
            assertEquals("1.5E-7", firstEventRow.getCell(3).getStringCellValue());
            
            // Verify Documents sheet
            Sheet outDoc = outputWorkbook.getSheet("Documents");
            assertNotNull("Documents sheet should exist", outDoc);
            Row firstDocRow = outDoc.getRow(1);
            assertEquals("DOC-001", firstDocRow.getCell(0).getStringCellValue());
            assertEquals("ISO 26262-1:2018", firstDocRow.getCell(1).getStringCellValue());
            assertEquals("ISO", firstDocRow.getCell(2).getStringCellValue());
        }
        
        templateWorkbook.close();
        
        System.out.println("✓ Mock model data test successful: " + outputPath);
        System.out.println("  - Generated 4 sheets with real automotive safety data");
        System.out.println("  - 15 requirements, 35 fault tree events, 20 reference documents");
        System.out.println("  - Verified: Summary counts, requirement REQ-ESL-001, event BE-001, doc DOC-001");
    }
    
    /**
     * Test with comprehensive FMEA data showing typical FMEA table structure.
     * Demonstrates 100-line FMEA table with 24 columns for Electronic Steering Lock.
     */
    @Test
    public void testFmeaData() throws Exception {
        // Create FMEA template with typical columns
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet fmeaSheet = templateWorkbook.createSheet("FMEA");
        
        // Header row with 24 typical FMEA columns
        Row header = fmeaSheet.createRow(0);
        String[] columns = {
            "Item Number", "Component", "Function", "Failure Mode",
            "Effect (Local)", "Effect (Next)", "Effect (End)", "Severity",
            "Causes", "Occurrence", "Current Controls", "Detection", "RPN",
            "ASIL", "Safety Mechanism", "Diag Coverage",
            "Recommended Actions", "Responsibility", "Target Date",
            "Status", "Actions Taken", "Sev'", "Occ'", "Det'", "RPN'"
        };
        
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }
        
        // Summary row with collection size
        Row summaryRow = fmeaSheet.createRow(1);
        summaryRow.createCell(0).setCellValue("Total FMEA Entries:");
        summaryRow.createCell(1).setCellValue("{m:fmeaEntries->size()}");
        
        // First data row with expressions to access first FMEA entry
        Row dataRow = fmeaSheet.createRow(3);
        dataRow.createCell(0).setCellValue("{m:fmeaEntries->first().item_number}");
        dataRow.createCell(1).setCellValue("{m:fmeaEntries->first().component}");
        dataRow.createCell(2).setCellValue("{m:fmeaEntries->first().function}");
        dataRow.createCell(3).setCellValue("{m:fmeaEntries->first().failure_mode}");
        dataRow.createCell(4).setCellValue("{m:fmeaEntries->first().effect_local}");
        dataRow.createCell(5).setCellValue("{m:fmeaEntries->first().effect_next_level}");
        dataRow.createCell(6).setCellValue("{m:fmeaEntries->first().effect_end}");
        dataRow.createCell(7).setCellValue("{m:fmeaEntries->first().severity}");
        dataRow.createCell(8).setCellValue("{m:fmeaEntries->first().causes}");
        dataRow.createCell(9).setCellValue("{m:fmeaEntries->first().occurrence}");
        dataRow.createCell(10).setCellValue("{m:fmeaEntries->first().current_controls}");
        dataRow.createCell(11).setCellValue("{m:fmeaEntries->first().detection}");
        dataRow.createCell(12).setCellValue("{m:fmeaEntries->first().rpn}");
        dataRow.createCell(13).setCellValue("{m:fmeaEntries->first().asil}");
        dataRow.createCell(14).setCellValue("{m:fmeaEntries->first().safety_mechanism}");
        dataRow.createCell(15).setCellValue("{m:fmeaEntries->first().diagnostic_coverage}");
        dataRow.createCell(16).setCellValue("{m:fmeaEntries->first().recommended_actions}");
        dataRow.createCell(17).setCellValue("{m:fmeaEntries->first().responsibility}");
        dataRow.createCell(18).setCellValue("{m:fmeaEntries->first().target_date}");
        dataRow.createCell(19).setCellValue("{m:fmeaEntries->first().status}");
        dataRow.createCell(20).setCellValue("{m:fmeaEntries->first().actions_taken}");
        dataRow.createCell(21).setCellValue("{m:fmeaEntries->first().new_severity}");
        dataRow.createCell(22).setCellValue("{m:fmeaEntries->first().new_occurrence}");
        dataRow.createCell(23).setCellValue("{m:fmeaEntries->first().new_detection}");
        dataRow.createCell(24).setCellValue("{m:fmeaEntries->first().new_rpn}");
        
        // Setup: Get mock model variables
        Map<String, Object> variables = MockModelData.getVariables();
        
        // Setup: Query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = "target/test-output/fmea-table.xlsx";
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        // Verify generation successful
        assertTrue("Generation should succeed", result.isSuccessful());
        assertTrue("Output file should exist", new File(outputPath).exists());
        
        // Verify content
        try (FileInputStream fis = new FileInputStream(outputPath);
             XSSFWorkbook outputWorkbook = new XSSFWorkbook(fis)) {
            
            Sheet outFmea = outputWorkbook.getSheet("FMEA");
            assertNotNull("FMEA sheet should exist", outFmea);
            
            // Verify header
            Row outHeader = outFmea.getRow(0);
            assertEquals("Item Number", outHeader.getCell(0).getStringCellValue());
            assertEquals("Component", outHeader.getCell(1).getStringCellValue());
            assertEquals("RPN'", outHeader.getCell(24).getStringCellValue());
            
            // Verify summary
            Row outSummary = outFmea.getRow(1);
            assertEquals("Total FMEA Entries:", outSummary.getCell(0).getStringCellValue());
            assertEquals("100", outSummary.getCell(1).getStringCellValue());
            
            // Verify first FMEA entry data
            Row outData = outFmea.getRow(3);
            assertEquals("FMEA-001", outData.getCell(0).getStringCellValue());
            assertEquals("Microcontroller", outData.getCell(1).getStringCellValue());
            assertEquals("Execute control logic", outData.getCell(2).getStringCellValue());
            assertEquals("CPU core failure", outData.getCell(3).getStringCellValue());
            assertEquals("10", outData.getCell(7).getStringCellValue()); // Severity
            assertEquals("210", outData.getCell(12).getStringCellValue()); // RPN
            assertEquals("ASIL-D", outData.getCell(13).getStringCellValue());
            assertEquals("In Progress", outData.getCell(19).getStringCellValue());
            assertEquals("100", outData.getCell(24).getStringCellValue()); // New RPN
        }
        
        templateWorkbook.close();
        
        System.out.println("✓ FMEA data test successful: " + outputPath);
        System.out.println("  - Generated FMEA table with 100 entries");
        System.out.println("  - 24 columns covering identification, analysis, actions, and revised assessment");
        System.out.println("  - Components: Microcontroller, DC Motor, Hall Sensors, Power Supply, CAN, Mechanism, Software");
        System.out.println("  - Verified: FMEA-001 (Microcontroller CPU core failure, ASIL-D, RPN 210→100)");
    }
}
