package io.github.nheuermann.m2spreadsheet.tests;

import io.github.nheuermann.m2spreadsheet.generator.GenerationResult;
import io.github.nheuermann.m2spreadsheet.util.M2SpreadsheetUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
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
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test for merge_row feature that merges cells across loop iterations.
 */
public class MergeRowTest {

    private static final String OUTPUT_DIR = "target/test-output/merge-row";

    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }

    @Test
    public void testMergeRowBasic() throws Exception {
        System.out.println("\n=== Test: Basic merge_row ===");

        // Create template with merge_row directive
        XSSFWorkbook templateWb = new XSSFWorkbook();
        Sheet templateSheet = templateWb.createSheet("Test");

        // Row 0: Header
        Row row0 = templateSheet.createRow(0);
        row0.createCell(0).setCellValue("Component");
        row0.createCell(1).setCellValue("Failure Mode");

        // Row 1: for_row component
        Row row1 = templateSheet.createRow(1);
        row1.createCell(0).setCellValue("{m:for_row component | components}");

        // Row 2: Nested for_row fm
        Row row2 = templateSheet.createRow(2);
        row2.createCell(0).setCellValue("{m:for_row fm | component.failureModes}");

        // Row 3: Component name with merge_row + Failure mode name
        Row row3 = templateSheet.createRow(3);
        row3.createCell(0).setCellValue("{m:merge_row component}{m:component.name}");
        row3.createCell(1).setCellValue("{m:fm.name}");

        // Row 4: endfor fm
        Row row4 = templateSheet.createRow(4);
        row4.createCell(0).setCellValue("{m:endfor_row}");

        // Row 5: endfor component
        Row row5 = templateSheet.createRow(5);
        row5.createCell(0).setCellValue("{m:endfor_row}");


        // Prepare test data - 2 components, 3 failure modes each
        List<Map<String, Object>> components = new ArrayList<>();
        
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> component = new HashMap<>();
            component.put("name", "Component-" + i);
            
            List<Map<String, Object>> failureModes = new ArrayList<>();
            for (int j = 1; j <= 3; j++) {
                Map<String, Object> fm = new HashMap<>();
                fm.put("name", "FM-" + i + "-" + j);
                failureModes.add(fm);
            }
            component.put("failureModes", failureModes);
            components.add(component);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("components", components);

        // Generate
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        File outputFile = new File(OUTPUT_DIR, "merge-row-basic.xlsx");
        URI outputURI = URI.createFileURI(outputFile.getAbsolutePath());

        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWb, queryEnv, variables, resourceSet, outputURI, new BasicMonitor()
        );

        // Verify no errors
        assertTrue("Generation should succeed", result.getGenerationErrors().isEmpty());

        // Read generated file
        try (FileInputStream fis = new FileInputStream(outputFile);
             XSSFWorkbook generatedWb = new XSSFWorkbook(fis)) {
            
            Sheet sheet = generatedWb.getSheetAt(0);
            
            System.out.println("\n=== Generated Data ===");
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    StringBuilder sb = new StringBuilder("Row " + i + ": ");
                    for (int j = 0; j < 2; j++) {
                        Cell cell = row.getCell(j);
                        String value = cell != null && cell.getCellType() == CellType.STRING ? 
                                     cell.getStringCellValue() : "";
                        sb.append(value).append(" | ");
                    }
                    System.out.println(sb.toString());
                }
            }

            // Verify structure
            // Header row
            assertEquals("Component", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Failure Mode", sheet.getRow(0).getCell(1).getStringCellValue());

            // Component 1 (rows 1-3) with merged cell in column 0
            assertEquals("Component-1", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("FM-1-1", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("FM-1-2", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("FM-1-3", sheet.getRow(3).getCell(1).getStringCellValue());

            // Component 2 (rows 4-6) with merged cell in column 0
            assertEquals("Component-2", sheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals("FM-2-1", sheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals("FM-2-2", sheet.getRow(5).getCell(1).getStringCellValue());
            assertEquals("FM-2-3", sheet.getRow(6).getCell(1).getStringCellValue());

            // Verify merged regions
            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
            System.out.println("\n=== Merged Regions ===");
            for (CellRangeAddress region : mergedRegions) {
                System.out.println("Merged: rows " + region.getFirstRow() + "-" + region.getLastRow() + 
                                 ", columns " + region.getFirstColumn() + "-" + region.getLastColumn());
            }

            // Should have 2 merged regions (one for each component)
            assertEquals("Should have 2 merged regions", 2, mergedRegions.size());

            // Verify first merge (Component-1, rows 1-3, column 0)
            CellRangeAddress merge1 = mergedRegions.get(0);
            assertEquals("First merge should start at row 1", 1, merge1.getFirstRow());
            assertEquals("First merge should end at row 3", 3, merge1.getLastRow());
            assertEquals("First merge should be in column 0", 0, merge1.getFirstColumn());
            assertEquals("First merge should be in column 0", 0, merge1.getLastColumn());

            // Verify second merge (Component-2, rows 4-6, column 0)
            CellRangeAddress merge2 = mergedRegions.get(1);
            assertEquals("Second merge should start at row 4", 4, merge2.getFirstRow());
            assertEquals("Second merge should end at row 6", 6, merge2.getLastRow());
            assertEquals("Second merge should be in column 0", 0, merge2.getFirstColumn());
            assertEquals("Second merge should be in column 0", 0, merge2.getLastColumn());

            System.out.println("✓ Merge_row test passed!");
        }
    }
}
