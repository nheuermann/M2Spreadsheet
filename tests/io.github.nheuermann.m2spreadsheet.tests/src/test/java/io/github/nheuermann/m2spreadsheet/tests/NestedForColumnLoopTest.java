package io.github.nheuermann.m2spreadsheet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
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
 * Test nested for_column loops.
 */
public class NestedForColumnLoopTest {

    @Before
    public void setUp() {
        new File("target/test-output").mkdirs();
    }

    @Test
    public void testNestedForColumnLoops() throws Exception {
        System.out.println("\n=== Test: Nested for_column Loops ===");
        
        // Create template workbook with nested for_column loops
        XSSFWorkbook templateWb = new XSSFWorkbook();
        XSSFSheet sheet = templateWb.createSheet("Test");
        
        // Row 0: Headers with nested for_column
        XSSFRow row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Category");
        row0.createCell(1).setCellValue("{m:for_column cat | categories}");
        row0.createCell(2).setCellValue("{m:for_column item | cat.items}");
        row0.createCell(3).setCellValue("{m:item}");
        row0.createCell(4).setCellValue("{m:endfor_column}");
        row0.createCell(5).setCellValue("{m:endfor_column}");
        
        // Row 1: Data
        XSSFRow row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Items");
        row1.createCell(1).setCellValue("{m:for_column cat | categories}");
        row1.createCell(2).setCellValue("{m:for_column item | cat.items}");
        row1.createCell(3).setCellValue("{m:item}");
        row1.createCell(4).setCellValue("{m:endfor_column}");
        row1.createCell(5).setCellValue("{m:endfor_column}");
        
        // Prepare variables
        Map<String, Object> variables = new HashMap<>();
        
        // Create nested structure: categories with items
        Map<String, Object> cat1 = new HashMap<>();
        cat1.put("name", "Fruits");
        cat1.put("items", Arrays.asList("apple", "banana"));
        
        Map<String, Object> cat2 = new HashMap<>();
        cat2.put("name", "Vegetables");
        cat2.put("items", Arrays.asList("carrot", "potato", "tomato"));
        
        variables.put("categories", Arrays.asList(cat1, cat2));
        
        // Generate
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        URI outputURI = URI.createFileURI(new File("target/test-output/nested-for-column-generated.xlsx").getAbsolutePath());
        
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWb,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        // Verify
        assertTrue("Generation should succeed", result.isSuccessful());
        
        // Open and check result
        try (FileInputStream fis = new FileInputStream("target/test-output/nested-for-column-generated.xlsx");
             XSSFWorkbook resultWb = new XSSFWorkbook(fis)) {
            
            XSSFSheet resultSheet = resultWb.getSheet("Test");
            assertNotNull("Sheet should exist", resultSheet);
            
            XSSFRow resultRow0 = resultSheet.getRow(0);
            
            // Expected columns: Category | apple | banana | carrot | potato | tomato
            // (2 items from Fruits + 3 items from Vegetables = 5 data columns)
            System.out.println("\n=== Generated Data ===");
            System.out.print("Row 0: ");
            for (int col = 0; col < resultRow0.getLastCellNum(); col++) {
                XSSFCell cell = resultRow0.getCell(col);
                if (cell != null) {
                    System.out.print(cell.getStringCellValue() + " | ");
                }
            }
            System.out.println();
            
            // Verify structure
            assertEquals("Category", resultRow0.getCell(0).getStringCellValue());
            assertEquals("apple", resultRow0.getCell(1).getStringCellValue());
            assertEquals("banana", resultRow0.getCell(2).getStringCellValue());
            assertEquals("carrot", resultRow0.getCell(3).getStringCellValue());
            assertEquals("potato", resultRow0.getCell(4).getStringCellValue());
            assertEquals("tomato", resultRow0.getCell(5).getStringCellValue());
            
            System.out.println("✓ Nested for_column test passed!");
            System.out.println("  - 2 categories processed");
            System.out.println("  - 2 items in Fruits + 3 items in Vegetables = 5 columns");
        }
        
        templateWb.close();
    }
}
