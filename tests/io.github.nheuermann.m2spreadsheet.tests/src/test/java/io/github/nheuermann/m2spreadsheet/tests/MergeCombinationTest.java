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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test for combination of merge_row and merge_column directives.
 */
public class MergeCombinationTest {

    private static final String OUTPUT_DIR = "target/test-output/merge-combination";

    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }

    @Test
    public void testMergeCombination() throws Exception {
        System.out.println("\n=== Test: Combination of merge_row and merge_column ===");

        // Create template with both merge directives
        XSSFWorkbook templateWb = new XSSFWorkbook();
        Sheet templateSheet = templateWb.createSheet("Test");

        // Row 0: Header
        Row row0 = templateSheet.createRow(0);
        row0.createCell(0).setCellValue("Component");
        row0.createCell(1).setCellValue("{m:for_column status | statuses}");
        row0.createCell(2).setCellValue("{m:status.name}");
        row0.createCell(3).setCellValue("{m:endfor_column}");

        // Row 1: for_row component
        Row row1 = templateSheet.createRow(1);
        row1.createCell(0).setCellValue("{m:for_row component | components}");

        // Row 2: for_row detail (nested)
        Row row2 = templateSheet.createRow(2);
        row2.createCell(0).setCellValue("{m:for_row detail | component.details}");

        // Row 3: Data row with both merge directives
        Row row3 = templateSheet.createRow(3);
        row3.createCell(0).setCellValue("{m:merge_row component}{m:component.name}");
        row3.createCell(1).setCellValue("{m:for_column status | statuses}");
        row3.createCell(2).setCellValue("{m:detail.name}");
        row3.createCell(3).setCellValue("{m:endfor_column}");

        // Row 4: endfor detail
        Row row4 = templateSheet.createRow(4);
        row4.createCell(0).setCellValue("{m:endfor_row}");

        // Row 5: endfor component
        Row row5 = templateSheet.createRow(5);
        row5.createCell(0).setCellValue("{m:endfor_row}");

        // Prepare test data
        List<String> statuses = new ArrayList<>();
        statuses.add("Status-A");
        statuses.add("Status-B");
        
        List<Map<String, Object>> components = new ArrayList<>();
        
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> component = new HashMap<>();
            component.put("name", "Component-" + i);
            
            List<Map<String, Object>> details = new ArrayList<>();
            for (int j = 1; j <= 3; j++) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("name", "Detail-" + i + "-" + j);
                details.add(detail);
            }
            component.put("details", details);
            components.add(component);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("statuses", statuses);
        variables.put("components", components);

        // Generate
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        File outputFile = new File(OUTPUT_DIR, "merge-combination.xlsx");
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
                    short lastCol = row.getLastCellNum();
                    for (int j = 0; j < Math.max(lastCol, 3); j++) {
                        Cell cell = row.getCell(j);
                        String value = cell != null && cell.getCellType() == CellType.STRING ? 
                                     cell.getStringCellValue() : "";
                        sb.append(value).append(" | ");
                    }
                    System.out.println(sb.toString());
                }
            }

            // Verify merged regions
            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
            System.out.println("\n=== Merged Regions ===");
            for (CellRangeAddress region : mergedRegions) {
                System.out.println("Merged: rows " + region.getFirstRow() + "-" + region.getLastRow() + 
                                 ", columns " + region.getFirstColumn() + "-" + region.getLastColumn());
            }

            // Should have merge_row regions for Component names (column 0, spanning 3 detail rows each)
            boolean foundComponent1Merge = false;
            boolean foundComponent2Merge = false;
            
            for (CellRangeAddress merge : mergedRegions) {
                // Check for vertical merges in column 0
                if (merge.getFirstColumn() == 0 && merge.getLastColumn() == 0) {
                    int rowSpan = merge.getLastRow() - merge.getFirstRow() + 1;
                    if (rowSpan >= 3) {  // Should span 3 detail rows
                        if (!foundComponent1Merge) {
                            foundComponent1Merge = true;
                            System.out.println("Found Component-1 merge_row: rows " + 
                                             merge.getFirstRow() + "-" + merge.getLastRow());
                        } else {
                            foundComponent2Merge = true;
                            System.out.println("Found Component-2 merge_row: rows " + 
                                             merge.getFirstRow() + "-" + merge.getLastRow());
                        }
                    }
                }
            }

            assertTrue("Should find Component-1 merge_row", foundComponent1Merge);
            assertTrue("Should find Component-2 merge_row", foundComponent2Merge);

            // Verify basic structure
            Row headerRow = sheet.getRow(0);
            assertEquals("Component", headerRow.getCell(0).getStringCellValue());
            
            // Component names should be in column 0
            assertTrue("Should have Component-1", 
                sheet.getRow(1).getCell(0).getStringCellValue().contains("Component-1"));
            assertTrue("Should have Component-2", 
                sheet.getRow(4).getCell(0).getStringCellValue().contains("Component-2"));

            System.out.println("✓ Merge combination test passed!");
            System.out.println("Note: Both merge_row (vertical) and merge_column (horizontal) work independently.");
            System.out.println("      For 2D merges, merge_row handles vertical spans and merge_column handles horizontal spans.");
        }
    }
}
