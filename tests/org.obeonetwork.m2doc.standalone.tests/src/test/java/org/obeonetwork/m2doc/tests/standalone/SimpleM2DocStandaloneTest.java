package org.obeonetwork.m2doc.tests.standalone;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Test;
import org.obeonetwork.m2doc.generator.GenerationResult;
import org.obeonetwork.m2doc.parser.DocumentParserException;
import org.obeonetwork.m2doc.template.DocumentTemplate;
import org.obeonetwork.m2doc.util.ClassProvider;
import org.obeonetwork.m2doc.util.M2DocUtils;

/**
 * Simple standalone test to verify M2Doc works without OSGi.
 * This proves the baseline testing setup before creating M2Spreadsheet.
 * 
 * @author nheuermann
 */
public class SimpleM2DocStandaloneTest {

    /**
     * Test that we can create a simple document generation.
     * NOTE: This test requires a template file - for now it will just verify the API is accessible.
     */
    @Test
    public void testM2DocAPIAccessible() throws Exception {
        // Setup: Create query environment and resource set
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = org.eclipse.acceleo.query.runtime.Query
                .newEnvironmentWithDefaultServices(null);
        
        // Register M2Doc services
        M2DocUtils.prepareEnvironmentServices(queryEnv, resourceSet, null, new HashMap<>());
        
        // Verify: Query environment is initialized
        assertNotNull("Query environment should be initialized", queryEnv);
        
        System.out.println("✓ M2Doc API is accessible in standalone mode");
        System.out.println("✓ Query environment initialized successfully");
    }
    
    /**
     * Test that we can parse and generate a simple document.
     * This will be uncommented once we have a template file.
     */
    // @Test
    public void testSimpleDocumentGeneration() throws Exception {
        // Setup: Paths
        String templatePath = "src/test/resources/simple-template.docx";
        String outputPath = "target/test-output/simple-generated.docx";
        
        // Verify template exists
        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            System.out.println("SKIP: Template file not found: " + templatePath);
            return;
        }
        
        // Setup: Resource set and query environment
        ResourceSet resourceSet = new ResourceSetImpl();
        URI templateURI = URI.createFileURI(templateFile.getAbsolutePath());
        IQueryEnvironment queryEnv = M2DocUtils.getQueryEnvironment(resourceSet, templateURI, new HashMap<>());
        
        // Parse template
        DocumentTemplate template = M2DocUtils.parse(
            resourceSet.getURIConverter(),
            templateURI,
            queryEnv,
            new ClassProvider(this.getClass().getClassLoader()),
            new BasicMonitor()
        );
        
        // Setup: Variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Test User");
        variables.put("company", "Test Company");
        
        // Generate
        URI outputURI = URI.createFileURI(new File(outputPath).getAbsolutePath());
        new File(outputPath).getParentFile().mkdirs();
        
        GenerationResult result = M2DocUtils.generate(
            template,
            queryEnv,
            variables,
            resourceSet,
            outputURI,
            new BasicMonitor()
        );
        
        // Verify: No errors
        assertTrue("Generation should have no errors", result.getGenerationErrors().isEmpty());
        assertTrue("Output file should exist", new File(outputPath).exists());
        
        template.close();
        
        System.out.println("✓ Document generated successfully: " + outputPath);
    }
}
