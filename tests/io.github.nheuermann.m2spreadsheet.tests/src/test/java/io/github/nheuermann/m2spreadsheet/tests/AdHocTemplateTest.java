package io.github.nheuermann.m2spreadsheet.tests;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Ignore;
import org.junit.Test;

import io.github.nheuermann.m2spreadsheet.generator.GenerationResult;
import io.github.nheuermann.m2spreadsheet.util.M2SpreadsheetUtils;

/**
 * Ad-hoc template execution test.
 * 
 * <p>Use this test to quickly execute a single template .xlsx file without
 * needing to set up a folder-based test structure.</p>
 * 
 * <p><b>Usage:</b></p>
 * <ol>
 *   <li>Put your template file anywhere (e.g., in target/test-output/ or Desktop)</li>
 *   <li>Edit {@link #TEMPLATE_PATH} to point to your template</li>
 *   <li>Edit {@link #OUTPUT_PATH} to specify where output should go</li>
 *   <li>Customize {@link #getTestVariables()} to provide your data</li>
 *   <li>Remove the {@code @Ignore} annotation</li>
 *   <li>Run: {@code mvn test -Dtest=AdHocTemplateTest}</li>
 * </ol>
 * 
 * <p><b>Example:</b></p>
 * <pre>
 * // Edit these paths:
 * private static final String TEMPLATE_PATH = "/Users/yourname/Desktop/my-template.xlsx";
 * private static final String OUTPUT_PATH = "/Users/yourname/Desktop/my-output.xlsx";
 * 
 * // Run:
 * mvn test -Dtest=AdHocTemplateTest
 * </pre>
 * 
 * <p><b>Note:</b> This test is marked with {@code @Ignore} by default so it doesn't
 * run in the full test suite. Remove {@code @Ignore} when you want to use it.</p>
 * 
 * @author nheuermann
 */
public class AdHocTemplateTest {
    
    // ========================================================================
    // CONFIGURATION - Edit these to test your template
    // ========================================================================
    
    /**
     * Path to your template file.
     * Can be absolute path or relative to project root.
     */
    private static final String TEMPLATE_PATH = "target/test-output/my-template.xlsx";
    
    /**
     * Path where generated output should be saved.
     * Can be absolute path or relative to project root.
     */
    private static final String OUTPUT_PATH = "target/test-output/my-output.xlsx";
    
    // ========================================================================
    // TEST EXECUTION
    // ========================================================================
    
    /**
     * Execute template generation.
     * Remove the @Ignore annotation to run this test.
     * 
     * @throws Exception if generation fails
     */
    @Test
    @Ignore("Remove this @Ignore annotation to run ad-hoc generation")
    public void executeTemplate() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== Ad-Hoc Template Execution ===");
        System.out.println("=".repeat(70));
        
        File templateFile = new File(TEMPLATE_PATH);
        File outputFile = new File(OUTPUT_PATH);
        
        // Validate template exists
        if (!templateFile.exists()) {
            System.err.println("\n❌ ERROR: Template file not found!");
            System.err.println("   Looking for: " + templateFile.getAbsolutePath());
            System.err.println("\n💡 TIP: Edit TEMPLATE_PATH in AdHocTemplateTest.java");
            throw new IllegalArgumentException("Template file not found: " + templateFile.getAbsolutePath());
        }
        
        System.out.println("📄 Template: " + templateFile.getAbsolutePath());
        System.out.println("📊 Output:   " + outputFile.getAbsolutePath());
        
        // Create output directory if needed
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
            System.out.println("📁 Created output directory: " + outputDir.getAbsolutePath());
        }
        
        // Load template
        System.out.println("\n⏳ Loading template...");
        XSSFWorkbook templateWorkbook;
        try (FileInputStream fis = new FileInputStream(templateFile)) {
            templateWorkbook = new XSSFWorkbook(fis);
        }
        System.out.println("✓ Template loaded: " + templateWorkbook.getNumberOfSheets() + " sheet(s)");
        
        // Get test variables
        Map<String, Object> variables = getTestVariables();
        System.out.println("\n📋 Variables provided:");
        for (String key : variables.keySet()) {
            Object value = variables.get(key);
            if (value instanceof List) {
                System.out.println("   - " + key + ": List with " + ((List<?>) value).size() + " item(s)");
            } else if (value instanceof Map) {
                System.out.println("   - " + key + ": Map with " + ((Map<?, ?>) value).size() + " item(s)");
            } else {
                System.out.println("   - " + key + ": " + value);
            }
        }
        
        // Setup query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        
        // Generate
        System.out.println("\n⚙️  Generating output...");
        URI outputURI = URI.createFileURI(outputFile.getAbsolutePath());
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWorkbook,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        templateWorkbook.close();
        
        // Report results
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== Generation Result ===");
        System.out.println("=".repeat(70));
        
        if (result.isSuccessful()) {
            System.out.println("✅ Generation SUCCESSFUL!");
        } else {
            System.out.println("❌ Generation FAILED!");
        }
        
        // Show validation messages
        if (!result.getValidationMessages().isEmpty()) {
            System.out.println("\n⚠️  Validation Messages:");
            for (io.github.nheuermann.m2spreadsheet.validation.TemplateValidationMessage message : result.getValidationMessages()) {
                System.out.println("   - " + message);
            }
        }
        
        // Show generation errors
        if (!result.getGenerationErrors().isEmpty()) {
            System.out.println("\n❌ Generation Errors:");
            for (Exception error : result.getGenerationErrors()) {
                System.out.println("   - " + error.getMessage());
                error.printStackTrace(System.out);
            }
        }
        
        if (result.isSuccessful()) {
            System.out.println("\n📂 Open the generated file:");
            System.out.println("   " + outputFile.getAbsolutePath());
            System.out.println("\n💡 Or run: open \"" + outputFile.getAbsolutePath() + "\"");
        }
        
        System.out.println("=".repeat(70) + "\n");
        
        // Assert success for JUnit
        if (!result.isSuccessful()) {
            throw new AssertionError("Generation failed - see errors above");
        }
    }
    
    // ========================================================================
    // TEST DATA - Customize this method to provide your variables
    // ========================================================================
    
    /**
     * Provide variables for template evaluation.
     * 
     * <p><b>Customize this method to provide data for your template.</b></p>
     * 
     * <p>Example templates can use:</p>
     * <ul>
     *   <li>{@code {m:projectName}} - Simple string</li>
     *   <li>{@code {m:for item | items}} - List iteration</li>
     *   <li>{@code {m:for_row person | people}} - Row repetition</li>
     *   <li>{@code {m:for_column category | categories}} - Column repetition</li>
     * </ul>
     * 
     * @return map of variable name to value
     */
    private Map<String, Object> getTestVariables() {
        Map<String, Object> variables = new HashMap<>();
        
        // === Simple values ===
        variables.put("projectName", "My Project");
        variables.put("projectCode", "PRJ-001");
        variables.put("title", "Test Report");
        variables.put("author", "Test User");
        variables.put("date", "2026-02-13");
        
        // === Simple lists ===
        variables.put("items", Arrays.asList("apple", "banana", "cherry", "date", "elderberry"));
        variables.put("numbers", Arrays.asList(1, 2, 3, 4, 5));
        variables.put("colors", Arrays.asList("red", "green", "blue"));
        
        // === Nested data (for for_row loops) ===
        List<Map<String, Object>> people = Arrays.asList(
            createPerson("Alice", 30, Arrays.asList(95, 88, 92)),
            createPerson("Bob", 25, Arrays.asList(87, 91, 85)),
            createPerson("Charlie", 35, Arrays.asList(90, 86, 89))
        );
        variables.put("people", people);
        
        // === Categories (for for_column loops) ===
        variables.put("categories", Arrays.asList("Math", "Science", "English"));
        
        // === Complex nested data ===
        List<Map<String, Object>> departments = Arrays.asList(
            createDepartment("Engineering", Arrays.asList("Alice", "Bob", "Charlie")),
            createDepartment("Marketing", Arrays.asList("David", "Eve")),
            createDepartment("Sales", Arrays.asList("Frank", "Grace", "Henry", "Iris"))
        );
        variables.put("departments", departments);
        
        // === Mock model data (for FMEA, requirements, etc.) ===
        variables.put("requirements", MockModelData.getRequirements());
        variables.put("faultTreeEvents", MockModelData.getFaultTreeBaseEvents());
        variables.put("referenceDocuments", MockModelData.getReferenceDocuments());
        variables.put("fmeaEntries", MockModelData.getFmeaEntries());
        
        return variables;
    }
    
    /**
     * Helper: Create a person map.
     */
    private Map<String, Object> createPerson(String name, int age, List<Integer> scores) {
        Map<String, Object> person = new HashMap<>();
        person.put("name", name);
        person.put("age", age);
        person.put("scores", scores);
        return person;
    }
    
    /**
     * Helper: Create a department map.
     */
    private Map<String, Object> createDepartment(String name, List<String> members) {
        Map<String, Object> dept = new HashMap<>();
        dept.put("name", name);
        dept.put("members", members);
        dept.put("memberCount", members.size());
        return dept;
    }
}
