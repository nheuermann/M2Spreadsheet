package io.github.nheuermann.m2spreadsheet.tests;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test to verify the structured FMEA hierarchy.
 */
public class StructuredFmeaTest {
    
    @Test
    public void testStructuredFmeaHierarchy() {
        // Get the structured FMEA model
        Map<String, Object> fmeaModel = MockModelData.getStructuredFmea();
        
        // Verify model properties
        assertNotNull("FMEA model should exist", fmeaModel);
        assertEquals("FMEA-MODEL-001", fmeaModel.get("id"));
        assertEquals("ESL System FMEA", fmeaModel.get("name"));
        
        // Verify components
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) fmeaModel.get("components");
        assertNotNull("Components list should exist", components);
        assertEquals("Should have 10 components", 10, components.size());
        
        // Verify first component structure
        Map<String, Object> component1 = components.get(0);
        assertEquals("COMP-001", component1.get("id"));
        assertEquals("Component 1", component1.get("name"));
        
        // Verify failure modes
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> failureModes = (List<Map<String, Object>>) component1.get("failureModes");
        assertNotNull("Failure modes should exist", failureModes);
        assertEquals("Each component should have 3 failure modes", 3, failureModes.size());
        
        // Verify first failure mode
        Map<String, Object> failureMode1 = failureModes.get(0);
        assertEquals("FM-001-001", failureMode1.get("id"));
        assertEquals("Failure Mode 1.1", failureMode1.get("name"));
        
        // Verify effects
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> effects = (List<Map<String, Object>>) failureMode1.get("effects");
        assertNotNull("Effects should exist", effects);
        assertEquals("Each failure mode should have 3 effects", 3, effects.size());
        
        Map<String, Object> effect1 = effects.get(0);
        assertEquals("EFF-001-001-001", effect1.get("id"));
        assertEquals("Effect 1.1.1", effect1.get("name"));
        
        // Verify causes
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> causes = (List<Map<String, Object>>) failureMode1.get("causes");
        assertNotNull("Causes should exist", causes);
        assertEquals("Each failure mode should have 2 causes", 2, causes.size());
        
        Map<String, Object> cause1 = causes.get(0);
        assertEquals("CAUSE-001-001-001", cause1.get("id"));
        assertEquals("Cause 1.1.1", cause1.get("name"));
        
        // Verify total counts
        int totalComponents = components.size();
        int totalFailureModes = 0;
        int totalEffects = 0;
        int totalCauses = 0;
        
        for (Map<String, Object> component : components) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fms = (List<Map<String, Object>>) component.get("failureModes");
            totalFailureModes += fms.size();
            
            for (Map<String, Object> fm : fms) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> effs = (List<Map<String, Object>>) fm.get("effects");
                totalEffects += effs.size();
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> caus = (List<Map<String, Object>>) fm.get("causes");
                totalCauses += caus.size();
            }
        }
        
        assertEquals("Should have 10 components", 10, totalComponents);
        assertEquals("Should have 30 failure modes (10 * 3)", 30, totalFailureModes);
        assertEquals("Should have 90 effects (30 * 3)", 90, totalEffects);
        assertEquals("Should have 60 causes (30 * 2)", 60, totalCauses);
        
        System.out.println("✓ Structured FMEA verification:");
        System.out.println("  - Components: " + totalComponents);
        System.out.println("  - Failure Modes: " + totalFailureModes);
        System.out.println("  - Effects: " + totalEffects);
        System.out.println("  - Causes: " + totalCauses);
    }
    
    @Test
    public void testStructuredFmeaInVariables() {
        // Verify the structured FMEA is accessible through variables
        Map<String, Object> variables = MockModelData.getVariables();
        
        assertNotNull("Variables should exist", variables);
        assertTrue("Variables should contain structuredFmea", variables.containsKey("structuredFmea"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fmeaModel = (Map<String, Object>) variables.get("structuredFmea");
        
        assertNotNull("structuredFmea should not be null", fmeaModel);
        assertEquals("FMEA-MODEL-001", fmeaModel.get("id"));
        
        System.out.println("✓ structuredFmea is accessible in variables");
    }
}
