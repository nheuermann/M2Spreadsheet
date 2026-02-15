package io.github.nheuermann.m2spreadsheet.tests.folder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.github.nheuermann.m2spreadsheet.generator.GenerationResult;
import io.github.nheuermann.m2spreadsheet.util.M2SpreadsheetUtils;

/**
 * Abstract test suite for folder-based spreadsheet template tests.
 * 
 * <p>This class provides infrastructure for testing M2Spreadsheet templates using
 * a folder-based approach similar to M2Doc. Each test case is a folder containing:</p>
 * 
 * <ul>
 *   <li><b>{foldername}-template.xlsx</b>: The template spreadsheet</li>
 *   <li><b>{foldername}-expected.xlsx</b>: The expected output (optional)</li>
 *   <li><b>{foldername}.properties</b>: Variable definitions (optional)</li>
 * </ul>
 * 
 * <p>When executed, the test will:</p>
 * <ol>
 *   <li>Load the template</li>
 *   <li>Generate the output to {foldername}-generated.xlsx</li>
 *   <li>If expected output exists, compare and create {foldername}-diff.xlsx</li>
 * </ol>
 * 
 * <p><b>Usage:</b> Extend this class and implement:</p>
 * <ul>
 *   <li>{@link #getTestRootFolder()} - return the root folder containing test cases</li>
 *   <li>{@link #getVariables(String)} - provide variables for template evaluation</li>
 * </ul>
 * 
 * <p><b>Example implementation:</b></p>
 * <pre>
 * public class MyTemplatesTest extends AbstractSpreadsheetsTestSuite {
 *     public MyTemplatesTest(String testFolderPath) {
 *         super(testFolderPath);
 *     }
 *     
 *     &#64;Parameters(name = "{0}")
 *     public static Collection&lt;Object[]&gt; data() {
 *         return retrieveTestFolders("src/test/resources/cases");
 *     }
 *     
 *     &#64;Override
 *     protected Map&lt;String, Object&gt; getVariables(String testFolderPath) {
 *         Map&lt;String, Object&gt; vars = new HashMap&lt;&gt;();
 *         vars.put("myData", Arrays.asList("a", "b", "c"));
 *         return vars;
 *     }
 * }
 * </pre>
 * 
 * @author nheuermann
 */
@RunWith(Parameterized.class)
public abstract class AbstractSpreadsheetsTestSuite {
    
    /**
     * The test folder path.
     */
    private final String testFolderPath;
    
    /**
     * Constructor.
     * 
     * @param testFolderPath the test folder path
     */
    public AbstractSpreadsheetsTestSuite(String testFolderPath) {
        this.testFolderPath = testFolderPath;
    }
    
    /**
     * Retrieves all test folders from the given root path.
     * Each subfolder is considered a test case.
     * 
     * @param rootPath the root path containing test case folders
     * @return collection of test folder paths
     */
    protected static Collection<Object[]> retrieveTestFolders(String rootPath) {
        List<Object[]> parameters = new ArrayList<>();
        
        File root = new File(rootPath);
        if (!root.exists() || !root.isDirectory()) {
            System.err.println("WARNING: Test root folder does not exist: " + rootPath);
            System.err.println("         No folder-based tests will run.");
            return parameters;
        }
        
        // Get all subdirectories
        File[] testFolders = root.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && !pathname.getName().startsWith(".");
            }
        });
        
        if (testFolders == null || testFolders.length == 0) {
            System.err.println("WARNING: No test folders found in: " + rootPath);
            return parameters;
        }
        
        // Each folder becomes a test case
        for (File testFolder : testFolders) {
            parameters.add(new Object[] { testFolder.getAbsolutePath() });
        }
        
        System.out.println("Found " + parameters.size() + " test case(s) in: " + rootPath);
        
        return parameters;
    }
    
    /**
     * Main test method that runs generation and validation.
     * 
     * @throws Exception if test fails
     */
    @Test
    public void generation() throws Exception {
        File testFolder = new File(testFolderPath);
        String folderName = testFolder.getName();
        
        System.out.println("\n=== Running test case: " + folderName + " ===");
        
        // Get file paths
        File templateFile = new File(testFolder, folderName + "-template.xlsx");
        File expectedFile = new File(testFolder, folderName + "-expected.xlsx");
        File generatedFile = new File(testFolder, folderName + "-generated.xlsx");
        File diffFile = new File(testFolder, folderName + "-diff.xlsx");
        
        // Verify template exists
        if (!templateFile.exists()) {
            fail("Template file does not exist: " + templateFile.getAbsolutePath());
        }
        
        System.out.println("Template: " + templateFile.getName());
        
        // Load template
        XSSFWorkbook templateWorkbook;
        try (FileInputStream fis = new FileInputStream(templateFile)) {
            templateWorkbook = new XSSFWorkbook(fis);
        }
        
        // Get variables from subclass
        Map<String, Object> variables = getVariables(testFolderPath);
        if (variables == null) {
            variables = new HashMap<>();
        }
        
        System.out.println("Variables: " + variables.keySet());
        
        // Setup query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        URI generatedURI = URI.createFileURI(generatedFile.getAbsolutePath());
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            generatedURI,
            new BasicMonitor()
        );
        
        templateWorkbook.close();
        
        // Report generation result
        if (!result.isSuccessful()) {
            System.err.println("Generation FAILED:");
            for (Exception error : result.getGenerationErrors()) {
                System.err.println("  ERROR: " + error.getMessage());
                error.printStackTrace();
            }
        }
        
        if (!result.getValidationMessages().isEmpty()) {
            System.out.println("Validation messages:");
            for (String message : result.getValidationMessages()) {
                System.out.println("  " + message);
            }
        }
        
        // Verify generated file exists
        assertTrue("Generated file should exist: " + generatedFile.getName(), 
                   generatedFile.exists());
        
        System.out.println("✓ Generated: " + generatedFile.getName());
        
        // Compare with expected if it exists
        if (expectedFile.exists()) {
            System.out.println("Expected: " + expectedFile.getName());
            
            boolean filesMatch = compareSpreadsheets(expectedFile, generatedFile, diffFile);
            
            if (filesMatch) {
                System.out.println("✓ Generated output matches expected");
                // Clean up diff file if exists from previous run
                if (diffFile.exists()) {
                    diffFile.delete();
                }
            } else {
                System.err.println("✗ Generated output differs from expected");
                System.err.println("  See diff file: " + diffFile.getName());
                fail("Generated output does not match expected. See: " + diffFile.getAbsolutePath());
            }
        } else {
            System.out.println("NOTE: No expected file to compare against.");
            System.out.println("      Create " + expectedFile.getName() + " to enable comparison.");
        }
        
        System.out.println("=== Test case complete: " + folderName + " ===\n");
    }
    
    /**
     * Compare two spreadsheet files and generate a diff file if they differ.
     * 
     * @param expectedFile the expected output file
     * @param actualFile the generated output file
     * @param diffFile the diff output file
     * @return true if files match, false otherwise
     * @throws IOException if files cannot be read
     */
    private boolean compareSpreadsheets(File expectedFile, File actualFile, File diffFile) 
            throws IOException {
        
        XSSFWorkbook expectedWb = new XSSFWorkbook(new FileInputStream(expectedFile));
        XSSFWorkbook actualWb = new XSSFWorkbook(new FileInputStream(actualFile));
        
        List<String> differences = new ArrayList<>();
        boolean filesMatch = true;
        
        // Compare number of sheets
        int expectedSheets = expectedWb.getNumberOfSheets();
        int actualSheets = actualWb.getNumberOfSheets();
        
        if (expectedSheets != actualSheets) {
            differences.add("Sheet count differs: expected " + expectedSheets + 
                          ", actual " + actualSheets);
            filesMatch = false;
        }
        
        // Compare each sheet
        int sheetsToCompare = Math.min(expectedSheets, actualSheets);
        for (int sheetIdx = 0; sheetIdx < sheetsToCompare; sheetIdx++) {
            Sheet expectedSheet = expectedWb.getSheetAt(sheetIdx);
            Sheet actualSheet = actualWb.getSheetAt(sheetIdx);
            
            String sheetName = expectedSheet.getSheetName();
            
            // Compare sheet names
            if (!expectedSheet.getSheetName().equals(actualSheet.getSheetName())) {
                differences.add("Sheet " + sheetIdx + " name differs: expected '" + 
                              expectedSheet.getSheetName() + "', actual '" + 
                              actualSheet.getSheetName() + "'");
                filesMatch = false;
            }
            
            // Compare rows
            int expectedRows = expectedSheet.getLastRowNum() + 1;
            int actualRows = actualSheet.getLastRowNum() + 1;
            
            if (expectedRows != actualRows) {
                differences.add("Sheet '" + sheetName + "' row count differs: expected " + 
                              expectedRows + ", actual " + actualRows);
                filesMatch = false;
            }
            
            // Compare merged regions
            List<org.apache.poi.ss.util.CellRangeAddress> expectedMerges = expectedSheet.getMergedRegions();
            List<org.apache.poi.ss.util.CellRangeAddress> actualMerges = actualSheet.getMergedRegions();
            
            if (expectedMerges.size() != actualMerges.size()) {
                differences.add("Sheet '" + sheetName + "' merged region count differs: expected " + 
                              expectedMerges.size() + ", actual " + actualMerges.size());
                filesMatch = false;
            }
            
            // Find missing merges
            for (org.apache.poi.ss.util.CellRangeAddress expectedMerge : expectedMerges) {
                if (!actualMerges.contains(expectedMerge)) {
                    differences.add("Sheet '" + sheetName + "' missing merge: " + 
                                  formatMergeRegion(expectedMerge));
                    filesMatch = false;
                }
            }
            
            // Find extra merges
            for (org.apache.poi.ss.util.CellRangeAddress actualMerge : actualMerges) {
                if (!expectedMerges.contains(actualMerge)) {
                    differences.add("Sheet '" + sheetName + "' extra merge: " + 
                                  formatMergeRegion(actualMerge));
                    filesMatch = false;
                }
            }
            
            // Compare cell contents
            int rowsToCompare = Math.max(expectedRows, actualRows);
            for (int rowIdx = 0; rowIdx < rowsToCompare; rowIdx++) {
                Row expectedRow = expectedSheet.getRow(rowIdx);
                Row actualRow = actualSheet.getRow(rowIdx);
                
                if (expectedRow == null && actualRow == null) continue;
                
                if (expectedRow == null) {
                    differences.add("Sheet '" + sheetName + "' row " + rowIdx + 
                                  ": expected null, actual has data");
                    filesMatch = false;
                    continue;
                }
                
                if (actualRow == null) {
                    differences.add("Sheet '" + sheetName + "' row " + rowIdx + 
                                  ": expected has data, actual is null");
                    filesMatch = false;
                    continue;
                }
                
                // Compare cells in row
                int expectedCells = expectedRow.getLastCellNum();
                int actualCells = actualRow.getLastCellNum();
                int cellsToCompare = Math.max(expectedCells, actualCells);
                
                for (int cellIdx = 0; cellIdx < cellsToCompare; cellIdx++) {
                    Cell expectedCell = expectedRow.getCell(cellIdx);
                    Cell actualCell = actualRow.getCell(cellIdx);
                    
                    String expectedValue = getCellStringValue(expectedCell);
                    String actualValue = getCellStringValue(actualCell);
                    
                    if (!expectedValue.equals(actualValue)) {
                        differences.add("Sheet '" + sheetName + "' cell [" + rowIdx + "," + 
                                      cellIdx + "]: expected '" + expectedValue + 
                                      "', actual '" + actualValue + "'");
                        filesMatch = false;
                    }
                }
            }
        }
        
        expectedWb.close();
        actualWb.close();
        
        // Create diff file if differences found
        if (!filesMatch) {
            createDiffFile(diffFile, differences);
        }
        
        return filesMatch;
    }
    
    /**
     * Get string value of a cell, handling null and different types.
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    /**
     * Format a merge region for display.
     */
    private String formatMergeRegion(org.apache.poi.ss.util.CellRangeAddress merge) {
        return "rows " + merge.getFirstRow() + "-" + merge.getLastRow() + 
               ", cols " + merge.getFirstColumn() + "-" + merge.getLastColumn();
    }
    
    /**
     * Create a diff file showing all differences.
     */
    private void createDiffFile(File diffFile, List<String> differences) throws IOException {
        XSSFWorkbook diffWb = new XSSFWorkbook();
        Sheet diffSheet = diffWb.createSheet("Differences");
        
        Row headerRow = diffSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Difference");
        
        for (int i = 0; i < differences.size(); i++) {
            Row row = diffSheet.createRow(i + 1);
            row.createCell(0).setCellValue(differences.get(i));
        }
        
        // Auto-size column
        diffSheet.autoSizeColumn(0);
        
        // Write to file
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(diffFile)) {
            diffWb.write(fos);
        }
        
        diffWb.close();
        
        System.out.println("Created diff file with " + differences.size() + " difference(s)");
    }
    
    /**
     * Get variables for template evaluation.
     * Subclasses must implement this to provide test-specific variables.
     * 
     * @param testFolderPath the test folder path
     * @return map of variable names to values
     */
    protected abstract Map<String, Object> getVariables(String testFolderPath);
}
