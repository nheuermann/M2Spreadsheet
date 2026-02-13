package io.github.nheuermann.m2spreadsheet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
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
 * Tests for within-cell for loop support.
 * Tests the {m:for var | collection} ... {m:endfor} syntax within a single cell.
 * 
 * @author nheuermann
 */
public class WithinCellForLoopTest {
    
    private static final String OUTPUT_DIR = "target/test-output";
    
    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }
    
    /**
     * Test simple within-cell for loop with string concatenation.
     * Cell contains: "Items: {m:for item | items}{m:item}, {m:endfor}"
     * Expected result: "Items: apple, banana, cherry, "
     */
    @Test
    public void testSimpleWithinCellForLoop() throws Exception {
        System.out.println("\n=== Test: Simple Within-Cell For Loop ===");
        
        // Create template
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        
        // Template: Items: {m:for item | items}{m:item}, {m:endfor}
        cell.setCellValue("Items: {m:for item | items}{m:item}, {m:endfor}");
        
        // Setup variables
        List<String> items = Arrays.asList("apple", "banana", "cherry");
        Map<String, Object> variables = new HashMap<>();
        variables.put("items", items);
        
        // Setup query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = OUTPUT_DIR + "/simple-within-cell-for-loop.xlsx";
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
        
        // Verify output
        XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
        Sheet outputSheet = outputWorkbook.getSheetAt(0);
        Row outputRow = outputSheet.getRow(0);
        Cell outputCell = outputRow.getCell(0);
        
        String actualValue = outputCell.getStringCellValue();
        String expectedValue = "Items: apple, banana, cherry, ";
        
        System.out.println("Expected: " + expectedValue);
        System.out.println("Actual:   " + actualValue);
        
        assertEquals("Cell should contain concatenated items", expectedValue, actualValue);
        
        outputWorkbook.close();
        templateWorkbook.close();
        
        System.out.println("✓ Simple within-cell for loop test passed");
    }
    
    /**
     * Test nested within-cell for loops.
     * Outer loop iterates over categories, inner loop over items in each category.
     */
    @Test
    public void testNestedWithinCellForLoop() throws Exception {
        System.out.println("\n=== Test: Nested Within-Cell For Loop ===");
        
        // Create template
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        
        // Template with nested loops
        cell.setCellValue(
            "{m:for cat | categories}" +
            "{m:cat.name}: {m:for item | cat.items}{m:item}, {m:endfor}; " +
            "{m:endfor}"
        );
        
        // Setup variables with nested structure
        Map<String, Object> fruit = new HashMap<>();
        fruit.put("name", "Fruit");
        fruit.put("items", Arrays.asList("apple", "banana"));
        
        Map<String, Object> veggie = new HashMap<>();
        veggie.put("name", "Vegetable");
        veggie.put("items", Arrays.asList("carrot", "potato"));
        
        List<Map<String, Object>> categories = Arrays.asList(fruit, veggie);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("categories", categories);
        
        // Setup query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = OUTPUT_DIR + "/nested-within-cell-for-loop.xlsx";
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
        
        // Verify output
        XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
        Sheet outputSheet = outputWorkbook.getSheetAt(0);
        Row outputRow = outputSheet.getRow(0);
        Cell outputCell = outputRow.getCell(0);
        
        String actualValue = outputCell.getStringCellValue();
        String expectedValue = "Fruit: apple, banana, ; Vegetable: carrot, potato, ; ";
        
        System.out.println("Expected: " + expectedValue);
        System.out.println("Actual:   " + actualValue);
        
        assertEquals("Cell should contain nested loop output", expectedValue, actualValue);
        
        outputWorkbook.close();
        templateWorkbook.close();
        
        System.out.println("✓ Nested within-cell for loop test passed");
    }
    
    /**
     * Test multiple sequential for loops in same cell.
     */
    @Test
    public void testSequentialWithinCellForLoops() throws Exception {
        System.out.println("\n=== Test: Sequential Within-Cell For Loops ===");
        
        // Create template
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        
        // Template with two sequential loops
        cell.setCellValue(
            "Fruits: {m:for f | fruits}{m:f} {m:endfor}| " +
            "Colors: {m:for c | colors}{m:c} {m:endfor}"
        );
        
        // Setup variables
        List<String> fruits = Arrays.asList("apple", "banana");
        List<String> colors = Arrays.asList("red", "yellow");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("fruits", fruits);
        variables.put("colors", colors);
        
        // Setup query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = OUTPUT_DIR + "/sequential-within-cell-for-loops.xlsx";
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
        
        // Verify output
        XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
        Sheet outputSheet = outputWorkbook.getSheetAt(0);
        Row outputRow = outputSheet.getRow(0);
        Cell outputCell = outputRow.getCell(0);
        
        String actualValue = outputCell.getStringCellValue();
        String expectedValue = "Fruits: apple banana | Colors: red yellow ";
        
        System.out.println("Expected: " + expectedValue);
        System.out.println("Actual:   " + actualValue);
        
        assertEquals("Cell should contain both loop outputs", expectedValue, actualValue);
        
        outputWorkbook.close();
        templateWorkbook.close();
        
        System.out.println("✓ Sequential within-cell for loops test passed");
    }
    
    /**
     * Test for loop with index variable.
     * Uses the auto-generated {var}_index variable.
     */
    @Test
    public void testForLoopWithIndex() throws Exception {
        System.out.println("\n=== Test: For Loop With Index ===");
        
        // Create template
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        
        // Template using index variable
        cell.setCellValue("List: {m:for item | items}{m:item_index}={m:item} {m:endfor}");
        
        // Setup variables
        List<String> items = Arrays.asList("first", "second", "third");
        Map<String, Object> variables = new HashMap<>();
        variables.put("items", items);
        
        // Setup query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        String outputPath = OUTPUT_DIR + "/for-loop-with-index.xlsx";
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
        
        // Verify output
        XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
        Sheet outputSheet = outputWorkbook.getSheetAt(0);
        Row outputRow = outputSheet.getRow(0);
        Cell outputCell = outputRow.getCell(0);
        
        String actualValue = outputCell.getStringCellValue();
        String expectedValue = "List: 0=first 1=second 2=third ";
        
        System.out.println("Expected: " + expectedValue);
        System.out.println("Actual:   " + actualValue);
        
        assertEquals("Cell should contain indexed items", expectedValue, actualValue);
        
        outputWorkbook.close();
        templateWorkbook.close();
        
        System.out.println("✓ For loop with index test passed");
    }
}
