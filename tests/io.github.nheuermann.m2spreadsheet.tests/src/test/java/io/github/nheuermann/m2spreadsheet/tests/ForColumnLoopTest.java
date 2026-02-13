package io.github.nheuermann.m2spreadsheet.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

import io.github.nheuermann.m2spreadsheet.util.M2SpreadsheetUtils;
import io.github.nheuermann.m2spreadsheet.generator.GenerationResult;

import static org.junit.Assert.*;

/**
 * Tests for for_column loop functionality.
 * Tests horizontal column repetition and integration with for_row.
 * 
 * @author nheuermann
 */
public class ForColumnLoopTest {
    
    private static final String OUTPUT_DIR = "test-output";
    
    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }
    
    /**
     * Test simple for_column loop without for_row.
     */
    @Test
    public void testSimpleForColumnLoop() throws Exception {
        System.out.println("\n=== Test: Simple For_Column Loop ===");
        
        // Create template workbook
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        
        // Row 0: for_column with items
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Item:");
        row0.createCell(1).setCellValue("{m:for_column item | items}");
        row0.createCell(2).setCellValue("{m:item}");
        row0.createCell(3).setCellValue("{m:endfor_column}");
        row0.createCell(4).setCellValue("Total");
        
        // Save template
        String templatePath = OUTPUT_DIR + "/for_column_simple_template.xlsx";
        try (FileOutputStream fos = new FileOutputStream(templatePath)) {
            templateWorkbook.write(fos);
        }
        templateWorkbook.close();
        
        // Create variables
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        Map<String, Object> variables = new HashMap<>();
        variables.put("items", Arrays.asList("apple", "banana", "cherry"));
        
        System.out.println("Variables: " + variables.keySet());
        
        // Generate
        String outputPath = OUTPUT_DIR + "/for_column_simple.xlsx";
        ResourceSet resourceSet = new ResourceSetImpl();
        URI templateURI = URI.createFileURI(new File(templatePath).getAbsolutePath());
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        
        // Load template from file
        XSSFWorkbook loadedTemplate = new XSSFWorkbook(new File(templatePath));
        GenerationResult result = M2SpreadsheetUtils.generate(
            loadedTemplate,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        loadedTemplate.close();
        
        // Check generation succeeded
        assertTrue("Generation should succeed", result.isSuccessful());
        assertTrue("Output file should exist", new File(outputPath).exists());
        
        // Verify: should have columns: Item: | apple | banana | cherry | Total
        XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
        Sheet outSheet = outputWorkbook.getSheetAt(0);
        Row outRow0 = outSheet.getRow(0);
        
        System.out.println("\n=== Generated Row 0 ===");
        for (int i = 0; i < 5; i++) {
            String value = outRow0.getCell(i) != null ? outRow0.getCell(i).getStringCellValue() : "NULL";
            System.out.println("Cell " + i + ": " + value);
        }
        
        // Check results
        assertEquals("Item:", outRow0.getCell(0).getStringCellValue());
        assertEquals("apple", outRow0.getCell(1).getStringCellValue());
        assertEquals("banana", outRow0.getCell(2).getStringCellValue());
        assertEquals("cherry", outRow0.getCell(3).getStringCellValue());
        assertEquals("Total", outRow0.getCell(4).getStringCellValue());
        
        System.out.println("✓ Simple for_column test passed");
        
        outputWorkbook.close();
    }
    
    /**
     * Test for_column with index variable.
     */
    @Test
    public void testForColumnWithIndex() throws Exception {
        System.out.println("\n=== Test: For_Column with Index ===");
        
        // Create template workbook
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        
        // Row 0: headers with for_column
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Index:");
        row0.createCell(1).setCellValue("{m:for_column item | items}");
        row0.createCell(2).setCellValue("{m:item_index}");
        row0.createCell(3).setCellValue("{m:endfor_column}");
        
        // Row 1: values
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Value:");
        row1.createCell(1).setCellValue("{m:for_column item | items}");
        row1.createCell(2).setCellValue("{m:item}");
        row1.createCell(3).setCellValue("{m:endfor_column}");
        
        // Save template
        String templatePath = OUTPUT_DIR + "/for_column_index_template.xlsx";
        try (FileOutputStream fos = new FileOutputStream(templatePath)) {
            templateWorkbook.write(fos);
        }
        templateWorkbook.close();
        
        // Create variables
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        Map<String, Object> variables = new HashMap<>();
        variables.put("items", Arrays.asList("A", "B", "C"));
        
        // Generate
        String outputPath = OUTPUT_DIR + "/for_column_index.xlsx";
        ResourceSet resourceSet = new ResourceSetImpl();
        URI templateURI = URI.createFileURI(new File(templatePath).getAbsolutePath());
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        
        XSSFWorkbook loadedTemplate = new XSSFWorkbook(new File(templatePath));
        GenerationResult result = M2SpreadsheetUtils.generate(
            loadedTemplate,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        loadedTemplate.close();
        
        assertTrue("Generation should succeed", result.isSuccessful());
        
        // Verify
        XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
        Sheet outSheet = outputWorkbook.getSheetAt(0);
        Row outRow0 = outSheet.getRow(0);
        Row outRow1 = outSheet.getRow(1);
        
        System.out.println("\n=== Generated ===");
        System.out.println("Row 0: " + 
            outRow0.getCell(0).getStringCellValue() + " | " +
            outRow0.getCell(1).getStringCellValue() + " | " +
            outRow0.getCell(2).getStringCellValue() + " | " +
            outRow0.getCell(3).getStringCellValue());
        System.out.println("Row 1: " + 
            outRow1.getCell(0).getStringCellValue() + " | " +
            outRow1.getCell(1).getStringCellValue() + " | " +
            outRow1.getCell(2).getStringCellValue() + " | " +
            outRow1.getCell(3).getStringCellValue());
        
        // Check row 0 (indices)
        assertEquals("0", outRow0.getCell(1).getStringCellValue());
        assertEquals("1", outRow0.getCell(2).getStringCellValue());
        assertEquals("2", outRow0.getCell(3).getStringCellValue());
        
        // Check row 1 (values)
        assertEquals("A", outRow1.getCell(1).getStringCellValue());
        assertEquals("B", outRow1.getCell(2).getStringCellValue());
        assertEquals("C", outRow1.getCell(3).getStringCellValue());
        
        System.out.println("✓ For_column with index test passed");
        
        outputWorkbook.close();
    }
    
    /**
     * Test for_column nested within for_row.
     * This creates a dynamic table structure.
     */
    @Test
    public void testForColumnWithinForRow() throws Exception {
        System.out.println("\n=== Test: For_Column within For_Row ===");
        
        // Create template workbook
        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        Sheet sheet = templateWorkbook.createSheet("Test");
        
        // Row 0: Headers with for_column
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Name");
        row0.createCell(1).setCellValue("{m:for_column category | categories}");
        row0.createCell(2).setCellValue("{m:category}");
        row0.createCell(3).setCellValue("{m:endfor_column}");
        
        // Row 1: for_row start
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("{m:for_row person | people}");
        
        // Row 2: Data row with nested for_column
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("{m:person.name}");
        row2.createCell(1).setCellValue("{m:for_column category | categories}");
        row2.createCell(2).setCellValue("{m:person.scores->at(category_index + 1)}");
        row2.createCell(3).setCellValue("{m:endfor_column}");
        
        // Row 3: endfor_row
        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("{m:endfor_row}");
        
        // Save template
        String templatePath = OUTPUT_DIR + "/for_column_nested_template.xlsx";
        try (FileOutputStream fos = new FileOutputStream(templatePath)) {
            templateWorkbook.write(fos);
        }
        templateWorkbook.close();
        
        // Create variables
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        Map<String, Object> variables = new HashMap<>();
        
        variables.put("categories", Arrays.asList("Math", "Science", "English"));
        
        // Create people with scores
        Map<String, Object> person1 = new HashMap<>();
        person1.put("name", "Alice");
        person1.put("scores", Arrays.asList(95, 88, 92));
        
        Map<String, Object> person2 = new HashMap<>();
        person2.put("name", "Bob");
        person2.put("scores", Arrays.asList(87, 91, 85));
        
        variables.put("people", Arrays.asList(person1, person2));
        
        System.out.println("Variables: " + variables.keySet());
        
        // Generate
        String outputPath = OUTPUT_DIR + "/for_column_nested.xlsx";
        ResourceSet resourceSet = new ResourceSetImpl();
        URI templateURI = URI.createFileURI(new File(templatePath).getAbsolutePath());
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        
        XSSFWorkbook loadedTemplate = new XSSFWorkbook(new File(templatePath));
        GenerationResult result = M2SpreadsheetUtils.generate(
            loadedTemplate,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        loadedTemplate.close();
        
        assertTrue("Generation should succeed", result.isSuccessful());
        
        // Verify structure
        XSSFWorkbook outputWorkbook = new XSSFWorkbook(new File(outputPath));
        Sheet outSheet = outputWorkbook.getSheetAt(0);
        
        System.out.println("\n=== Generated Table ===");
        for (int r = 0; r <= 2; r++) {
            Row row = outSheet.getRow(r);
            System.out.print("Row " + r + ": ");
            for (int c = 0; c < 4; c++) {
                String value = row.getCell(c) != null ? row.getCell(c).toString() : "NULL";
                System.out.print(value + " | ");
            }
            System.out.println();
        }
        
        // Check header row
        Row outRow0 = outSheet.getRow(0);
        assertEquals("Name", outRow0.getCell(0).getStringCellValue());
        assertEquals("Math", outRow0.getCell(1).getStringCellValue());
        assertEquals("Science", outRow0.getCell(2).getStringCellValue());
        assertEquals("English", outRow0.getCell(3).getStringCellValue());
        
        // Check Alice's row
        Row outRow1 = outSheet.getRow(1);
        assertEquals("Alice", outRow1.getCell(0).getStringCellValue());
        assertEquals("95", outRow1.getCell(1).getStringCellValue());
        assertEquals("88", outRow1.getCell(2).getStringCellValue());
        assertEquals("92", outRow1.getCell(3).getStringCellValue());
        
        // Check Bob's row
        Row outRow2 = outSheet.getRow(2);
        assertEquals("Bob", outRow2.getCell(0).getStringCellValue());
        assertEquals("87", outRow2.getCell(1).getStringCellValue());
        assertEquals("91", outRow2.getCell(2).getStringCellValue());
        assertEquals("85", outRow2.getCell(3).getStringCellValue());
        
        System.out.println("✓ For_column within for_row test passed");
        
        outputWorkbook.close();
    }
}
