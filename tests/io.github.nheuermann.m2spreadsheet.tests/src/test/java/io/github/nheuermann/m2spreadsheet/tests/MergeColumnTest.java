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
 * Test for merge_column feature that merges cells across column loop iterations.
 */
public class MergeColumnTest {

    private static final String OUTPUT_DIR = "target/test-output/merge-column";

    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }

    @Test
    public void testMergeColumnBasic() throws Exception {
        System.out.println("\n=== Test: Basic merge_column ===");

        // Create template with merge_column directive
        XSSFWorkbook templateWb = new XSSFWorkbook();
        Sheet templateSheet = templateWb.createSheet("Test");

        // Row 0: Headers with column loop
        Row row0 = templateSheet.createRow(0);
        row0.createCell(0).setCellValue("Status");
        row0.createCell(1).setCellValue("{m:for_column phase | phases}");
        row0.createCell(2).setCellValue("{m:merge_column phase}{m:phase.name}");
        row0.createCell(3).setCellValue("{m:for_column task | phase.tasks}");
        row0.createCell(4).setCellValue("{m:task.name}");
        row0.createCell(5).setCellValue("{m:endfor_column}");
        row0.createCell(6).setCellValue("{m:endfor_column}");

        // Row 1: Data row
        Row row1 = templateSheet.createRow(1);
        row1.createCell(0).setCellValue("In Progress");

        // Prepare test data: 2 phases, each with 3 tasks
        List<Map<String, Object>> phases = new ArrayList<>();
        
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> phase = new HashMap<>();
            phase.put("name", "Phase-" + i);
            
            List<Map<String, Object>> tasks = new ArrayList<>();
            for (int j = 1; j <= 3; j++) {
                Map<String, Object> task = new HashMap<>();
                task.put("name", "Task-" + i + "-" + j);
                tasks.add(task);
            }
            phase.put("tasks", tasks);
            phases.add(phase);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("phases", phases);

        // Generate
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        File outputFile = new File(OUTPUT_DIR, "merge-column-basic.xlsx");
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
                    for (int j = 0; j < lastCol; j++) {
                        Cell cell = row.getCell(j);
                        String value = cell != null && cell.getCellType() == CellType.STRING ? 
                                     cell.getStringCellValue() : "";
                        sb.append(value).append(" | ");
                    }
                    System.out.println(sb.toString());
                }
            }

            // Verify structure
            Row headerRow = sheet.getRow(0);
            assertEquals("Status", headerRow.getCell(0).getStringCellValue());
            
            // Phase headers are merged across task columns
            assertEquals("Phase-1", headerRow.getCell(1).getStringCellValue());
            assertEquals("Phase-2", headerRow.getCell(5).getStringCellValue());
            
            // Verify merged regions
            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
            System.out.println("\n=== Merged Regions ===");
            for (CellRangeAddress region : mergedRegions) {
                System.out.println("Merged: rows " + region.getFirstRow() + "-" + region.getLastRow() + 
                                 ", columns " + region.getFirstColumn() + "-" + region.getLastColumn());
            }

            // Should have 2 merged regions (one for each phase header spanning its tasks)
            assertTrue("Should have at least 2 merged regions", mergedRegions.size() >= 2);

            // Verify first merge (Phase-1 header spanning 3 task columns)
            boolean foundPhase1Merge = false;
            boolean foundPhase2Merge = false;
            
            for (CellRangeAddress merge : mergedRegions) {
                if (merge.getFirstRow() == 0 && merge.getLastRow() == 0) {
                    int colSpan = merge.getLastColumn() - merge.getFirstColumn() + 1;
                    if (colSpan >= 2) {  // At least 2 columns (could be 3 for tasks)
                        if (!foundPhase1Merge) {
                            foundPhase1Merge = true;
                            System.out.println("Found Phase-1 merge: columns " + 
                                             merge.getFirstColumn() + "-" + merge.getLastColumn());
                        } else {
                            foundPhase2Merge = true;
                            System.out.println("Found Phase-2 merge: columns " + 
                                             merge.getFirstColumn() + "-" + merge.getLastColumn());
                        }
                    }
                }
            }

            assertTrue("Should find Phase-1 merge", foundPhase1Merge);
            assertTrue("Should find Phase-2 merge", foundPhase2Merge);

            System.out.println("✓ Merge_column test passed!");
        }
    }
}
