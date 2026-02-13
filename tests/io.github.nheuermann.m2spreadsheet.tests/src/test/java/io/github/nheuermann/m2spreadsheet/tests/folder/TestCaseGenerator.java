package io.github.nheuermann.m2spreadsheet.tests.folder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

/**
 * Helper class to generate initial test case templates.
 * Run this once to create the template and expected files for test cases.
 * 
 * <p>After running this, you can:</p>
 * <ol>
 *   <li>Open the generated templates and customize them</li>
 *   <li>Run the generated templates to create expected outputs</li>
 *   <li>Manually verify and adjust the expected outputs</li>
 *   <li>Rename the expected outputs to match the naming convention</li>
 * </ol>
 * 
 * @author nheuermann
 */
public class TestCaseGenerator {
    
    private static final String CASES_DIR = "src/test/resources/cases";
    
    @Before
    public void setUp() {
        new File(CASES_DIR).mkdirs();
    }
    
    /**
     * Generate simple-list test case.
     * Tests basic for_row loop with a list of items.
     */
    @Test
    public void generateSimpleListTestCase() throws IOException {
        File caseDir = new File(CASES_DIR, "simple-list");
        caseDir.mkdirs();
        
        // Create template
        XSSFWorkbook template = new XSSFWorkbook();
        Sheet sheet = template.createSheet("Items");
        
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Item");
        row0.createCell(1).setCellValue("Index");
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("{m:for_row item | items}");
        
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("{m:item}");
        row2.createCell(1).setCellValue("{m:item_index}");
        
        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("{m:endfor_row}");
        
        File templateFile = new File(caseDir, "simple-list-template.xlsx");
        try (FileOutputStream fos = new FileOutputStream(templateFile)) {
            template.write(fos);
        }
        template.close();
        
        // Create expected output
        XSSFWorkbook expected = new XSSFWorkbook();
        Sheet expectedSheet = expected.createSheet("Items");
        
        Row expRow0 = expectedSheet.createRow(0);
        expRow0.createCell(0).setCellValue("Item");
        expRow0.createCell(1).setCellValue("Index");
        
        Row expRow1 = expectedSheet.createRow(1);
        expRow1.createCell(0).setCellValue("apple");
        expRow1.createCell(1).setCellValue("0");
        
        Row expRow2 = expectedSheet.createRow(2);
        expRow2.createCell(0).setCellValue("banana");
        expRow2.createCell(1).setCellValue("1");
        
        Row expRow3 = expectedSheet.createRow(3);
        expRow3.createCell(0).setCellValue("cherry");
        expRow3.createCell(1).setCellValue("2");
        
        File expectedFile = new File(caseDir, "simple-list-expected.xlsx");
        try (FileOutputStream fos = new FileOutputStream(expectedFile)) {
            expected.write(fos);
        }
        expected.close();
        
        System.out.println("✓ Created: " + templateFile.getAbsolutePath());
        System.out.println("✓ Created: " + expectedFile.getAbsolutePath());
    }
    
    /**
     * Generate within-cell-loop test case.
     * Tests within-cell for loops (m:for / m:endfor).
     */
    @Test
    public void generateWithinCellLoopTestCase() throws IOException {
        File caseDir = new File(CASES_DIR, "within-cell-loop");
        caseDir.mkdirs();
        
        // Create template
        XSSFWorkbook template = new XSSFWorkbook();
        Sheet sheet = template.createSheet("Data");
        
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("List: {m:for item | items}{m:item}, {m:endfor}");
        
        File templateFile = new File(caseDir, "within-cell-loop-template.xlsx");
        try (FileOutputStream fos = new FileOutputStream(templateFile)) {
            template.write(fos);
        }
        template.close();
        
        // Create expected output
        XSSFWorkbook expected = new XSSFWorkbook();
        Sheet expectedSheet = expected.createSheet("Data");
        
        Row expRow0 = expectedSheet.createRow(0);
        expRow0.createCell(0).setCellValue("List: apple, banana, cherry, ");
        
        File expectedFile = new File(caseDir, "within-cell-loop-expected.xlsx");
        try (FileOutputStream fos = new FileOutputStream(expectedFile)) {
            expected.write(fos);
        }
        expected.close();
        
        System.out.println("✓ Created: " + templateFile.getAbsolutePath());
        System.out.println("✓ Created: " + expectedFile.getAbsolutePath());
    }
    
    /**
     * Generate README file documenting the test cases structure.
     */
    @Test
    public void generateReadme() throws IOException {
        File readme = new File(CASES_DIR, "README.md");
        
        String content = "# M2Spreadsheet Folder-Based Test Cases\n\n" +
            "This directory contains folder-based test cases for M2Spreadsheet template generation.\n\n" +
            "## Structure\n\n" +
            "Each test case is a subfolder containing:\n\n" +
            "- **`{name}-template.xlsx`** (required): The template spreadsheet with M2Spreadsheet expressions\n" +
            "- **`{name}-expected.xlsx`** (optional): The expected output after generation\n" +
            "- **`{name}-generated.xlsx`** (created by test): The actual generated output\n" +
            "- **`{name}-diff.xlsx`** (created if mismatch): Differences between expected and generated\n\n" +
            "## Running Tests\n\n" +
            "```bash\n" +
            "mvn test -Dtest=FolderBasedTemplatesTest\n" +
            "```\n\n" +
            "## Creating New Test Cases\n\n" +
            "1. Create a new subfolder: `mkdir src/test/resources/cases/my-test`\n" +
            "2. Create template: `my-test/my-test-template.xlsx`\n" +
            "3. (Optional) Run generation manually and save as: `my-test/my-test-expected.xlsx`\n" +
            "4. Run tests: The framework will auto-discover the new test case\n\n" +
            "## Available Variables\n\n" +
            "Templates can use these variables (defined in FolderBasedTemplatesTest):\n\n" +
            "- **Simple data**: `items` (list of strings), `numbers` (list of integers), `title`, `author`\n" +
            "- **Project data**: `projectName`, `projectCode`, `safetyStandard`, `asil`\n" +
            "- **Mock model data**: `requirements`, `faultTreeEvents`, `referenceDocuments`, `fmeaEntries`\n" +
            "- **Nested data**: `categories` (list with category name and items)\n\n" +
            "## Example Test Cases\n\n" +
            "### simple-list\n" +
            "Tests basic `m:for_row` loop with row repetition.\n\n" +
            "### within-cell-loop\n" +
            "Tests within-cell `m:for` / `m:endfor` loops.\n\n" +
            "## Debugging Tips\n\n" +
            "- Check generated files manually in Excel/LibreOffice\n" +
            "- Review diff files to understand mismatches\n" +
            "- Look at test output for validation messages\n" +
            "- Set breakpoints in AbstractSpreadsheetsTestSuite.generation()\n";
        
        try (FileOutputStream fos = new FileOutputStream(readme)) {
            fos.write(content.getBytes("UTF-8"));
        }
        
        System.out.println("✓ Created: " + readme.getAbsolutePath());
    }
}
