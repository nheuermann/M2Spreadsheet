package io.github.nheuermann.m2spreadsheet.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.acceleo.query.runtime.Query;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Test;

import io.github.nheuermann.m2spreadsheet.generator.GenerationResult;
import io.github.nheuermann.m2spreadsheet.util.M2SpreadsheetUtils;

/**
 * Test nested sheet-level for_column loops.
 * 
 * Template structure:
 *   Row 0: {m:for_column component | structuredFmea.components}  
 *          {m:for_column fm | component.failureModes}    
 *          {m:for_column effect | fm.effects}
 *          {m:merge_column component}{m:component.name}    
 *          {m:endfor_column}  
 *          {m:endfor_column}  
 *          {m:endfor_column}
 *   Row 1: (merge_column header spans)
 *   Row 2: {m:fm.name}{m:merge_column fm}
 *   Row 3: {m:effect.name}{m:merge_column effect}
 * 
 * Expected: Variables from all nested loops accessible in all rows.
 */
public class NestedSheetColumnLoopsTest {

    @Test
    public void testNestedSheetColumnLoopsWithMerge() throws Exception {
        // Create test data: 2 components -> 2 failure modes each -> 2 effects each
        Map<String, Object> data = createStructuredFmeaData();
        Map<String, Object> variables = new HashMap<>();
        variables.put("structuredFmea", data);
        
        // Create template workbook
        XSSFWorkbook templateWb = new XSSFWorkbook();
        Sheet sheet = templateWb.createSheet("Test");
        
        // Row 0: Nested for_column declarations (all in one row!)
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("{m:for_column component | structuredFmea.components}");
        row0.createCell(1).setCellValue("{m:for_column fm | component.failureModes}");
        row0.createCell(2).setCellValue("{m:for_column effect | fm.effects}");
        row0.createCell(3).setCellValue("{m:merge_column component}{m:component.name}");
        row0.createCell(4).setCellValue("{m:endfor_column}");  // end effect
        row0.createCell(5).setCellValue("{m:endfor_column}");  // end fm
        row0.createCell(6).setCellValue("{m:endfor_column}");  // end component
        
        // Row 1: Static header
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("FM Names:");
        
        // Row 2: Failure mode name with merge - placed in column 3 which is inside the fm loop (cols 1-5)
        Row row2 = sheet.createRow(2);
        row2.createCell(3).setCellValue("{m:fm.name}{m:merge_column fm}");
        
        // Row 3: Effect name with merge - placed in column 3 which is inside the effect loop (cols 2-4)
        Row row3 = sheet.createRow(3);
        row3.createCell(3).setCellValue("{m:effect.name}{m:merge_column effect}");
        
        // Save template
        File templateFile = new File("target/test-nested-sheet-column-loops-template.xlsx");
        templateFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(templateFile)) {
            templateWb.write(fos);
        }
        templateWb.close();
        
        // Generate
        ResourceSet resourceSet = new ResourceSetImpl();
        IQueryEnvironment queryEnv = M2SpreadsheetUtils.createQueryEnvironment();
        File outputFile = new File("target/test-nested-sheet-column-loops-output.xlsx");
        URI outputURI = URI.createFileURI(outputFile.getAbsolutePath());
        
        // Reload template for generation
        XSSFWorkbook templateWb2;
        try (FileInputStream fis = new FileInputStream(templateFile)) {
            templateWb2 = new XSSFWorkbook(fis);
        }
        
        GenerationResult result = M2SpreadsheetUtils.generate(
            templateWb2, queryEnv, variables, resourceSet, outputURI, new BasicMonitor()
        );
        
        // Verify no generation errors (there may be evaluation errors for undefined variables)
        // but we want to check the actual output
        
        // Read result
        XSSFWorkbook resultWb;
        try (FileInputStream fis = new FileInputStream(outputFile)) {
            resultWb = new XSSFWorkbook(fis);
        }
        
        System.out.println("\n=== Generated Output ===");
        Sheet resultSheet = resultWb.getSheetAt(0);
        
        // Debug: Print all cells
        for (Row row : resultSheet) {
            System.out.print("Row " + row.getRowNum() + ": ");
            for (int c = 0; c < 20; c++) {
                Cell cell = row.getCell(c);
                if (cell != null) {
                    String value = getCellValue(cell);
                    if (value != null && !value.isEmpty()) {
                        System.out.print("C" + c + "=[" + value + "] ");
                    }
                }
            }
            System.out.println();
        }
        
        // Verify: Row 2 should have failure mode names in the output columns (not literal {m:fm.name})
        Row resultRow2 = resultSheet.getRow(2);
        assertNotNull("Row 2 should exist", resultRow2);
        
        // Find the first non-empty cell with actual content (after loop expansion)
        Cell firstCell = null;
        String firstValue = null;
        for (int c = 0; c < 20; c++) {
            Cell cell = resultRow2.getCell(c);
            String value = getCellValue(cell);
            if (value != null && !value.trim().isEmpty()) {
                firstCell = cell;
                firstValue = value;
                break;
            }
        }
        
        System.out.println("\nFirst non-empty cell in row 2: [" + firstValue + "]");
        
        // Should NOT contain error messages or literal template expressions
        assertNotNull("First cell should have content", firstValue);
        assertFalse("Should not have error about 'fm' variable", 
                   firstValue != null && firstValue.contains("Couldn't find the 'fm' variable"));
        assertFalse("Should not have literal {m:fm.name}", 
                   firstValue != null && firstValue.contains("{m:fm.name}"));
        
        // Should contain an actual failure mode name
        assertTrue("Should contain FM-1-1 or FM-2-1", 
                  firstValue.contains("FM-1-1") || firstValue.contains("FM-2-1"));
        
        resultWb.close();
        
        System.out.println("✓ Nested sheet column loops test passed!");
    }
    
    private Map<String, Object> createStructuredFmeaData() {
        java.util.List<Map<String, Object>> components = new java.util.ArrayList<>();
        
        // Component 1
        Map<String, Object> comp1 = new HashMap<>();
        comp1.put("name", "Component-1");
        
        java.util.List<Map<String, Object>> fms1 = new java.util.ArrayList<>();
        
        // FM 1-1
        Map<String, Object> fm11 = new HashMap<>();
        fm11.put("name", "FM-1-1");
        java.util.List<Map<String, Object>> effects11 = new java.util.ArrayList<>();
        
        Map<String, Object> effect111 = new HashMap<>();
        effect111.put("name", "Effect-1-1-1");
        effects11.add(effect111);
        
        Map<String, Object> effect112 = new HashMap<>();
        effect112.put("name", "Effect-1-1-2");
        effects11.add(effect112);
        
        fm11.put("effects", effects11);
        fms1.add(fm11);
        
        // FM 1-2
        Map<String, Object> fm12 = new HashMap<>();
        fm12.put("name", "FM-1-2");
        java.util.List<Map<String, Object>> effects12 = new java.util.ArrayList<>();
        
        Map<String, Object> effect121 = new HashMap<>();
        effect121.put("name", "Effect-1-2-1");
        effects12.add(effect121);
        
        Map<String, Object> effect122 = new HashMap<>();
        effect122.put("name", "Effect-1-2-2");
        effects12.add(effect122);
        
        fm12.put("effects", effects12);
        fms1.add(fm12);
        
        comp1.put("failureModes", fms1);
        components.add(comp1);
        
        // Component 2
        Map<String, Object> comp2 = new HashMap<>();
        comp2.put("name", "Component-2");
        
        java.util.List<Map<String, Object>> fms2 = new java.util.ArrayList<>();
        
        // FM 2-1
        Map<String, Object> fm21 = new HashMap<>();
        fm21.put("name", "FM-2-1");
        java.util.List<Map<String, Object>> effects21 = new java.util.ArrayList<>();
        
        Map<String, Object> effect211 = new HashMap<>();
        effect211.put("name", "Effect-2-1-1");
        effects21.add(effect211);
        
        Map<String, Object> effect212 = new HashMap<>();
        effect212.put("name", "Effect-2-1-2");
        effects21.add(effect212);
        
        fm21.put("effects", effects21);
        fms2.add(fm21);
        
        // FM 2-2
        Map<String, Object> fm22 = new HashMap<>();
        fm22.put("name", "FM-2-2");
        java.util.List<Map<String, Object>> effects22 = new java.util.ArrayList<>();
        
        Map<String, Object> effect221 = new HashMap<>();
        effect221.put("name", "Effect-2-2-1");
        effects22.add(effect221);
        
        Map<String, Object> effect222 = new HashMap<>();
        effect222.put("name", "Effect-2-2-2");
        effects22.add(effect222);
        
        fm22.put("effects", effects22);
        fms2.add(fm22);
        
        comp2.put("failureModes", fms2);
        components.add(comp2);
        
        Map<String, Object> fmea = new HashMap<>();
        fmea.put("components", components);
        
        return fmea;
    }
    
    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
}
