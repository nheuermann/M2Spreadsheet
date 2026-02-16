package io.github.nheuermann.m2spreadsheet.tests;

import io.github.nheuermann.m2spreadsheet.generator.GenerationResult;
import io.github.nheuermann.m2spreadsheet.util.M2SpreadsheetUtils;
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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test AQL error handling - specifically checking if warnings/errors are shown
 * when accessing non-existent fields.
 */
public class AqlErrorHandlingTest {

    private static final String OUTPUT_DIR = "target/test-output/aql-errors";

    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }

    @Test
    public void testNonExistentFieldAccess() throws Exception {
        System.out.println("\n=== Test: Non-Existent Field Access ===");

        XSSFWorkbook templateWb = new XSSFWorkbook();
        Sheet templateSheet = templateWb.createSheet("Test");

        // Create test data with 'date' field (not 'datex')
        Map<String, Object> ref1 = new HashMap<>();
        ref1.put("title", "ISO 26262");
        ref1.put("date", "2018-12-01");  // Note: 'date', not 'datex'
        ref1.put("version", "2nd Edition");

        Map<String, Object> ref2 = new HashMap<>();
        ref2.put("title", "ASPICE");
        ref2.put("date", "2020-06-15");
        ref2.put("version", "3.1");

        List<Map<String, Object>> refs = new ArrayList<>();
        refs.add(ref1);
        refs.add(ref2);

        // Row 0: Header
        Row row0 = templateSheet.createRow(0);
        row0.createCell(0).setCellValue("Title");
        row0.createCell(1).setCellValue("Date (WRONG)");
        row0.createCell(2).setCellValue("Date (CORRECT)");

        // Row 1: for_row
        Row row1 = templateSheet.createRow(1);
        row1.createCell(0).setCellValue("{m:for_row ref | referenceDocuments}");

        // Row 2: Data row with WRONG field name
        Row row2 = templateSheet.createRow(2);
        row2.createCell(0).setCellValue("{m:ref.title}");
        row2.createCell(1).setCellValue("{m:ref.datex}");  // WRONG: should be 'date', not 'datex'
        row2.createCell(2).setCellValue("{m:ref.date}");   // CORRECT

        // Row 3: endfor
        Row row3 = templateSheet.createRow(3);
        row3.createCell(0).setCellValue("{m:endfor_row}");

        // Generate
        Map<String, Object> variables = new HashMap<>();
        variables.put("referenceDocuments", refs);

        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        File outputFile = new File(OUTPUT_DIR, "nonexistent-field.xlsx");
        URI outputURI = URI.createFileURI(outputFile.getAbsolutePath());

        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWb, queryEnv, variables, resourceSet, outputURI, new BasicMonitor()
        );

        System.out.println("\n=== Generation Result ===");
        System.out.println("Successful: " + result.isSuccessful());
        System.out.println("Errors: " + result.getGenerationErrors().size());
        for (Exception e : result.getGenerationErrors()) {
            System.out.println("  - " + e.getMessage());
        }

        // Read generated file
        try (FileInputStream fis = new FileInputStream(outputFile);
             XSSFWorkbook generatedWb = new XSSFWorkbook(fis)) {
            
            Sheet sheet = generatedWb.getSheetAt(0);
            
            System.out.println("\n=== Generated Content ===");
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    System.out.print("Row " + i + ": ");
                    for (int j = 0; j < 3; j++) {
                        Cell cell = row.getCell(j);
                        String value = cell != null ? cell.toString() : "NULL";
                        System.out.print("[" + value + "] ");
                    }
                    System.out.println();
                }
            }

            // Check what happened with the wrong field
            Row dataRow1 = sheet.getRow(1);
            Cell wrongFieldCell = dataRow1.getCell(1);
            String wrongFieldValue = wrongFieldCell != null ? wrongFieldCell.toString() : "NULL";

            System.out.println("\n=== Analysis ===");
            System.out.println("Value from wrong field 'ref.datex': [" + wrongFieldValue + "]");
            System.out.println("Expected: Either error message or empty/null");
            
            // The issue: This is likely empty or null, with NO warning/error shown!
            assertTrue("Wrong field should produce empty value or error message", 
                wrongFieldValue.isEmpty() || wrongFieldValue.contains("ERROR") || wrongFieldValue.contains("WARNING"));
        }

        templateWb.close();
    }

    @Test
    public void testSimpleNonExistentField() throws Exception {
        System.out.println("\n=== Test: Simple Non-Existent Field ===");

        XSSFWorkbook templateWb = new XSSFWorkbook();
        Sheet templateSheet = templateWb.createSheet("Test");

        Row row0 = templateSheet.createRow(0);
        row0.createCell(0).setCellValue("{m:customer.name}");
        row0.createCell(1).setCellValue("{m:customer.nonExistentField}");

        // Create variables
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "John Doe");
        // Note: no 'nonExistentField'

        Map<String, Object> variables = new HashMap<>();
        variables.put("customer", customer);

        // Generate
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        File outputFile = new File(OUTPUT_DIR, "simple-nonexistent.xlsx");
        URI outputURI = URI.createFileURI(outputFile.getAbsolutePath());

        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWb, queryEnv, variables, resourceSet, outputURI, new BasicMonitor()
        );

        System.out.println("\n=== Result ===");
        System.out.println("Errors: " + result.getGenerationErrors().size());

        // Read and check
        try (FileInputStream fis = new FileInputStream(outputFile);
             XSSFWorkbook generatedWb = new XSSFWorkbook(fis)) {
            
            Sheet sheet = generatedWb.getSheetAt(0);
            Row row = sheet.getRow(0);
            
            String validField = row.getCell(0) != null ? row.getCell(0).toString() : "NULL";
            String invalidField = row.getCell(1) != null ? row.getCell(1).toString() : "NULL";
            
            System.out.println("Valid field:   [" + validField + "]");
            System.out.println("Invalid field: [" + invalidField + "]");
            System.out.println("\nIssue: The invalid field is silently ignored (no error/warning shown)!");
        }

        templateWb.close();
    }
}
