package io.github.nheuermann.m2spreadsheet.tests;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for merge region comparison functionality in the diff tool.
 */
public class MergeComparisonTest {

    private static final String OUTPUT_DIR = "target/test-output/merge-comparison";

    @Before
    public void setUp() {
        new File(OUTPUT_DIR).mkdirs();
    }

    @Test
    public void testMergeComparison_Horizontal() throws Exception {
        System.out.println("\n=== Test: Merge Comparison - Horizontal ===");

        // Create expected file with horizontal merge
        File expectedFile = new File(OUTPUT_DIR, "horizontal-expected.xlsx");
        createFileWithHorizontalMerge(expectedFile);

        // Create actual file with same horizontal merge
        File actualFile = new File(OUTPUT_DIR, "horizontal-actual.xlsx");
        createFileWithHorizontalMerge(actualFile);

        // Compare - should match
        List<String> differences = compareSpreadsheetsWithMerges(expectedFile, actualFile);
        assertTrue("Files with same horizontal merges should match", differences.isEmpty());

        // Create actual file WITHOUT merge
        File actualFileNoMerge = new File(OUTPUT_DIR, "horizontal-actual-no-merge.xlsx");
        createFileWithoutMerge(actualFileNoMerge);

        // Compare - should differ
        differences = compareSpreadsheetsWithMerges(expectedFile, actualFileNoMerge);
        assertFalse("Files with different merges should differ", differences.isEmpty());
        assertTrue("Should detect missing merge", 
                   differences.stream().anyMatch(d -> d.contains("Missing merge")));

        System.out.println("✓ Horizontal merge comparison works");
    }

    @Test
    public void testMergeComparison_Vertical() throws Exception {
        System.out.println("\n=== Test: Merge Comparison - Vertical ===");

        // Create expected file with vertical merge
        File expectedFile = new File(OUTPUT_DIR, "vertical-expected.xlsx");
        createFileWithVerticalMerge(expectedFile);

        // Create actual file with same vertical merge
        File actualFile = new File(OUTPUT_DIR, "vertical-actual.xlsx");
        createFileWithVerticalMerge(actualFile);

        // Compare - should match
        List<String> differences = compareSpreadsheetsWithMerges(expectedFile, actualFile);
        assertTrue("Files with same vertical merges should match", differences.isEmpty());

        // Create actual file WITHOUT merge
        File actualFileNoMerge = new File(OUTPUT_DIR, "vertical-actual-no-merge.xlsx");
        createFileWithoutMerge(actualFileNoMerge);

        // Compare - should differ
        differences = compareSpreadsheetsWithMerges(expectedFile, actualFileNoMerge);
        assertFalse("Files with different merges should differ", differences.isEmpty());
        assertTrue("Should detect missing merge", 
                   differences.stream().anyMatch(d -> d.contains("Missing merge")));

        System.out.println("✓ Vertical merge comparison works");
    }

    @Test
    public void testMergeComparison_Both() throws Exception {
        System.out.println("\n=== Test: Merge Comparison - Both Directions ===");

        // Create expected file with 2D merge
        File expectedFile = new File(OUTPUT_DIR, "both-expected.xlsx");
        createFileWith2DMerge(expectedFile);

        // Create actual file with same 2D merge
        File actualFile = new File(OUTPUT_DIR, "both-actual.xlsx");
        createFileWith2DMerge(actualFile);

        // Compare - should match
        List<String> differences = compareSpreadsheetsWithMerges(expectedFile, actualFile);
        assertTrue("Files with same 2D merges should match", differences.isEmpty());

        // Create actual file WITHOUT merge
        File actualFileNoMerge = new File(OUTPUT_DIR, "both-actual-no-merge.xlsx");
        createFileWithoutMerge(actualFileNoMerge);

        // Compare - should differ
        differences = compareSpreadsheetsWithMerges(expectedFile, actualFileNoMerge);
        assertFalse("Files with different merges should differ", differences.isEmpty());
        assertTrue("Should detect missing merge", 
                   differences.stream().anyMatch(d -> d.contains("Missing merge")));

        System.out.println("✓ 2D merge comparison works");
    }

    @Test
    public void testMergeComparison_ExtraMerge() throws Exception {
        System.out.println("\n=== Test: Merge Comparison - Extra Merge ===");

        // Create expected file without merge
        File expectedFile = new File(OUTPUT_DIR, "extra-expected.xlsx");
        createFileWithoutMerge(expectedFile);

        // Create actual file WITH merge
        File actualFile = new File(OUTPUT_DIR, "extra-actual.xlsx");
        createFileWithHorizontalMerge(actualFile);

        // Compare - should differ
        List<String> differences = compareSpreadsheetsWithMerges(expectedFile, actualFile);
        assertFalse("Files with extra merge should differ", differences.isEmpty());
        assertTrue("Should detect extra merge", 
                   differences.stream().anyMatch(d -> d.contains("Extra merge")));

        System.out.println("✓ Extra merge detection works");
    }

    // Helper methods to create test files

    private void createFileWithHorizontalMerge(File file) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Test");
        
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Merged");
        row.createCell(1).setCellValue("");
        row.createCell(2).setCellValue("");
        
        // Merge columns 0-2 in row 0
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
    }

    private void createFileWithVerticalMerge(File file) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Test");
        
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Merged");
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("");
        
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("");
        
        // Merge rows 0-2 in column 0
        sheet.addMergedRegion(new CellRangeAddress(0, 2, 0, 0));
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
    }

    private void createFileWith2DMerge(File file) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Test");
        
        for (int r = 0; r < 3; r++) {
            Row row = sheet.createRow(r);
            for (int c = 0; c < 3; c++) {
                if (r == 0 && c == 0) {
                    row.createCell(c).setCellValue("Merged");
                } else {
                    row.createCell(c).setCellValue("");
                }
            }
        }
        
        // Merge rows 0-2, columns 0-2
        sheet.addMergedRegion(new CellRangeAddress(0, 2, 0, 2));
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
    }

    private void createFileWithoutMerge(File file) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Test");
        
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Not Merged");
        row.createCell(1).setCellValue("Separate");
        row.createCell(2).setCellValue("Cells");
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
    }

    /**
     * Simplified version of merge-aware comparison.
     * Returns list of differences including merge differences.
     */
    private List<String> compareSpreadsheetsWithMerges(File expectedFile, File actualFile) throws IOException {
        List<String> differences = new ArrayList<>();
        
        try (FileInputStream fis1 = new FileInputStream(expectedFile);
             FileInputStream fis2 = new FileInputStream(actualFile);
             XSSFWorkbook expectedWb = new XSSFWorkbook(fis1);
             XSSFWorkbook actualWb = new XSSFWorkbook(fis2)) {
            
            for (int sheetIdx = 0; sheetIdx < expectedWb.getNumberOfSheets(); sheetIdx++) {
                Sheet expectedSheet = expectedWb.getSheetAt(sheetIdx);
                Sheet actualSheet = actualWb.getSheetAt(sheetIdx);
                
                // Compare merged regions
                List<CellRangeAddress> expectedMerges = expectedSheet.getMergedRegions();
                List<CellRangeAddress> actualMerges = actualSheet.getMergedRegions();
                
                // Find missing merges
                for (CellRangeAddress expectedMerge : expectedMerges) {
                    if (!actualMerges.contains(expectedMerge)) {
                        differences.add("Missing merge in sheet '" + expectedSheet.getSheetName() + 
                                      "': " + formatMerge(expectedMerge));
                    }
                }
                
                // Find extra merges
                for (CellRangeAddress actualMerge : actualMerges) {
                    if (!expectedMerges.contains(actualMerge)) {
                        differences.add("Extra merge in sheet '" + expectedSheet.getSheetName() + 
                                      "': " + formatMerge(actualMerge));
                    }
                }
            }
        }
        
        return differences;
    }

    private String formatMerge(CellRangeAddress merge) {
        return "rows " + merge.getFirstRow() + "-" + merge.getLastRow() + 
               ", cols " + merge.getFirstColumn() + "-" + merge.getLastColumn();
    }
}
