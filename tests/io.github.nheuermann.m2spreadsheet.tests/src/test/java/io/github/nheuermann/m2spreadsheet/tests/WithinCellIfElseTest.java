package io.github.nheuermann.m2spreadsheet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
 * Tests for within-cell if/elseif/else statement support.
 * Tests the {m:if condition} ... {m:elseif condition} ... {m:else} ... {m:endif} syntax.
 * 
 * @author nheuermann
 */
public class WithinCellIfElseTest {
    
    private static final String OUTPUT_DIR = "target/test-output";
    
    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }
    
    /**
     * Test simple if/else statement.
     */
    @Test
    public void testSimpleIfElse() throws Exception {
        System.out.println("\n=== Test: Simple If/Else ===");
        
        // Create template
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        
        // Template: Status: {m:if active}Active{m:else}Inactive{m:endif}
        cell.setCellValue("Status: {m:if active}Active{m:else}Inactive{m:endif}");
        
        // Test with active = true
        Map<String, Object> variables = new HashMap<>();
        variables.put("active", true);
        
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        String outputPath = OUTPUT_DIR + "/simple-if-else-true.xlsx";
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        assertTrue("Generation should succeed", result.isSuccessful());
        
        XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
        Sheet outputSheet = outputWorkbook.getSheetAt(0);
        String actualValue = outputSheet.getRow(0).getCell(0).getStringCellValue();
        
        System.out.println("Expected: Status: Active");
        System.out.println("Actual:   " + actualValue);
        assertEquals("Status: Active", actualValue);
        outputWorkbook.close();
        
        // Test with active = false
        variables.put("active", false);
        outputPath = OUTPUT_DIR + "/simple-if-else-false.xlsx";
        outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        assertTrue("Generation should succeed", result.isSuccessful());
        
        outputWorkbook = new XSSFWorkbook(new File(outputPath));
        outputSheet = outputWorkbook.getSheetAt(0);
        actualValue = outputSheet.getRow(0).getCell(0).getStringCellValue();
        
        System.out.println("Expected: Status: Inactive");
        System.out.println("Actual:   " + actualValue);
        assertEquals("Status: Inactive", actualValue);
        outputWorkbook.close();
        
        templateWorkbook.close();
        System.out.println("✓ Simple if/else test passed");
    }
    
    /**
     * Test if/elseif/else statement with numeric grades.
     */
    @Test
    public void testIfElseIfElse() throws Exception {
        System.out.println("\n=== Test: If/ElseIf/Else ===");
        
        // Create template
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        
        // Template: Grade: {m:if score >= 90}A{m:elseif score >= 80}B{m:elseif score >= 70}C{m:elseif score >= 60}D{m:else}F{m:endif}
        cell.setCellValue("Grade: {m:if score >= 90}A{m:elseif score >= 80}B{m:elseif score >= 70}C{m:elseif score >= 60}D{m:else}F{m:endif}");
        
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Test different scores
        int[][] testCases = {{95, 'A'}, {85, 'B'}, {75, 'C'}, {65, 'D'}, {55, 'F'}};
        
        for (int[] testCase : testCases) {
            int score = testCase[0];
            char expectedGrade = (char) testCase[1];
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("score", score);
            
            String outputPath = OUTPUT_DIR + "/if-elseif-else-score-" + score + ".xlsx";
            URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
            GenerationResult result = M2SpreadsheetUtils.generate(
                templateWorkbook,
                queryEnv,
                variables,
                resourceSet,
                outputURI,
                new BasicMonitor()
            );
            
            assertTrue("Generation should succeed for score " + score, result.isSuccessful());
            
            XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
            Sheet outputSheet = outputWorkbook.getSheetAt(0);
            String actualValue = outputSheet.getRow(0).getCell(0).getStringCellValue();
            String expectedValue = "Grade: " + expectedGrade;
            
            System.out.println("Score " + score + " - Expected: " + expectedValue + ", Actual: " + actualValue);
            assertEquals(expectedValue, actualValue);
            outputWorkbook.close();
        }
        
        templateWorkbook.close();
        System.out.println("✓ If/elseif/else test passed");
    }
    
    /**
     * Test multiple elseif clauses.
     */
    @Test
    public void testMultipleElseIf() throws Exception {
        System.out.println("\n=== Test: Multiple ElseIf ===");
        
        // Create template
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        
        // Template: {m:if day == 'Mon'}Monday{m:elseif day == 'Tue'}Tuesday{m:elseif day == 'Wed'}Wednesday{m:else}Other{m:endif}
        cell.setCellValue("{m:if day == 'Mon'}Monday{m:elseif day == 'Tue'}Tuesday{m:elseif day == 'Wed'}Wednesday{m:else}Other{m:endif}");
        
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        String[][] testCases = {
            {"Mon", "Monday"},
            {"Tue", "Tuesday"},
            {"Wed", "Wednesday"},
            {"Thu", "Other"}
        };
        
        for (String[] testCase : testCases) {
            String day = testCase[0];
            String expected = testCase[1];
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("day", day);
            
            String outputPath = OUTPUT_DIR + "/multiple-elseif-" + day + ".xlsx";
            URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
            GenerationResult result = M2SpreadsheetUtils.generate(
                templateWorkbook,
                queryEnv,
                variables,
                resourceSet,
                outputURI,
                new BasicMonitor()
            );
            
            assertTrue("Generation should succeed for day " + day, result.isSuccessful());
            
            XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
            Sheet outputSheet = outputWorkbook.getSheetAt(0);
            String actualValue = outputSheet.getRow(0).getCell(0).getStringCellValue();
            
            System.out.println("Day " + day + " - Expected: " + expected + ", Actual: " + actualValue);
            assertEquals(expected, actualValue);
            outputWorkbook.close();
        }
        
        templateWorkbook.close();
        System.out.println("✓ Multiple elseif test passed");
    }
    
    /**
     * Test nested if/else statements.
     */
    @Test
    public void testNestedIfElse() throws Exception {
        System.out.println("\n=== Test: Nested If/Else ===");
        
        // Create template
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        
        // Template with nested if: {m:if hasLicense}{m:if age >= 18}Can drive{m:else}Too young{m:endif}{m:else}No license{m:endif}
        cell.setCellValue("{m:if hasLicense}{m:if age >= 18}Can drive{m:else}Too young{m:endif}{m:else}No license{m:endif}");
        
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        Object[][] testCases = {
            {true, 20, "Can drive"},
            {true, 16, "Too young"},
            {false, 20, "No license"},
            {false, 16, "No license"}
        };
        
        for (Object[] testCase : testCases) {
            boolean hasLicense = (Boolean) testCase[0];
            int age = (Integer) testCase[1];
            String expected = (String) testCase[2];
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("hasLicense", hasLicense);
            variables.put("age", age);
            
            String outputPath = OUTPUT_DIR + "/nested-if-" + hasLicense + "-" + age + ".xlsx";
            URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
            GenerationResult result = M2SpreadsheetUtils.generate(
                templateWorkbook,
                queryEnv,
                variables,
                resourceSet,
                outputURI,
                new BasicMonitor()
            );
            
            assertTrue("Generation should succeed for hasLicense=" + hasLicense + ", age=" + age, result.isSuccessful());
            
            XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
            Sheet outputSheet = outputWorkbook.getSheetAt(0);
            String actualValue = outputSheet.getRow(0).getCell(0).getStringCellValue();
            
            System.out.println("hasLicense=" + hasLicense + ", age=" + age + " - Expected: " + expected + ", Actual: " + actualValue);
            assertEquals(expected, actualValue);
            outputWorkbook.close();
        }
        
        templateWorkbook.close();
        System.out.println("✓ Nested if/else test passed");
    }
}
