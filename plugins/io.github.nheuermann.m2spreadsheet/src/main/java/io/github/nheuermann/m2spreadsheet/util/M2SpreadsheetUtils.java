package io.github.nheuermann.m2spreadsheet.util;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.acceleo.query.runtime.IQueryBuilderEngine;
import org.eclipse.acceleo.query.runtime.IQueryBuilderEngine.AstResult;
import org.eclipse.acceleo.query.runtime.IQueryEvaluationEngine;
import org.eclipse.acceleo.query.runtime.EvaluationResult;
import org.eclipse.acceleo.query.runtime.IService;
import org.eclipse.acceleo.query.runtime.Query;
import org.eclipse.acceleo.query.runtime.ServiceUtils;
import org.eclipse.acceleo.query.runtime.impl.QueryBuilderEngine;
import org.eclipse.acceleo.query.runtime.impl.QueryEvaluationEngine;
import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;

import io.github.nheuermann.m2spreadsheet.generator.GenerationResult;
import io.github.nheuermann.m2spreadsheet.services.MapServices;

/**
 * Main API entry point for M2Spreadsheet.
 * Provides utilities for generating Excel spreadsheets from templates with AQL expressions.
 * 
 * <p>Based on M2DocUtils architecture, adapted for spreadsheet generation.</p>
 * 
 * @author nheuermann
 */
public class M2SpreadsheetUtils {
    
    /**
     * The field start marker for template expressions.
     */
    public static final String FIELD_START = "{";
    
    /**
     * The m: prefix for M2Spreadsheet expressions.
     */
    public static final String M = "m:";
    
    /**
     * The M2Spreadsheet field start marker.
     */
    public static final String M_FIELD_START = FIELD_START + M;
    
    /**
     * The field end marker for template expressions.
     */
    public static final String FIELD_END = "}";
    
    /**
     * Excel extension file.
     */
    public static final String XLSX_EXTENSION_FILE = "xlsx";
    
    /**
     * Creates a query environment configured with M2Spreadsheet services.
     * This includes default AQL services plus Map access support.
     * 
     * @return the configured {@link IQueryEnvironment}
     */
    public static IQueryEnvironment createQueryEnvironment() {
        IQueryEnvironment queryEnv = Query.newEnvironmentWithDefaultServices(null);
        
        // Register Map services for HashMap property access
        Set<IService> services = ServiceUtils.getServices(queryEnv, MapServices.class);
        ServiceUtils.registerServices(queryEnv, services);
        
        // Future: Add more M2Spreadsheet-specific services here
        // services = ServiceUtils.getServices(queryEnv, SpreadsheetServices.class);
        // ServiceUtils.registerServices(queryEnv, services);
        
        return queryEnv;
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private M2SpreadsheetUtils() {
        // Utility class
    }
    
    /**
     * Generates a spreadsheet from the given template workbook and variables.
     * 
     * <p>Supports for loops with syntax: {m:for var | collection} ... {m:endfor}</p>
     * 
     * @param templateWorkbook the template workbook containing expressions
     * @param queryEnvironment the AQL query environment for expression evaluation
     * @param variables the variables map for template evaluation
     * @param resourceSetForModels the resource set for model elements
     * @param destinationURI the destination URI for the generated file
     * @param monitor the progress monitor
     * @return the generation result
     * @throws IOException if generation fails
     */
    public static GenerationResult generate(
            XSSFWorkbook templateWorkbook,
            IQueryEnvironment queryEnvironment,
            Map<String, Object> variables,
            ResourceSet resourceSetForModels,
            URI destinationURI,
            Monitor monitor) throws IOException {

        
        GenerationResult result = new GenerationResult();
        
        try {
            monitor.beginTask("Generating spreadsheet", 100);
            monitor.subTask("Processing template cells");
            
            // Create destination workbook (clone of template)
            XSSFWorkbook destinationWorkbook = new XSSFWorkbook();
            
            // Process each sheet in the template
            for (int i = 0; i < templateWorkbook.getNumberOfSheets(); i++) {
                Sheet templateSheet = templateWorkbook.getSheetAt(i);
                Sheet destSheet = destinationWorkbook.createSheet(templateSheet.getSheetName());
                
                processSheet(templateSheet, destSheet, variables, queryEnvironment, result);
            }
            
            monitor.worked(80);
            monitor.subTask("Saving workbook");
            
            // Save the workbook
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(new java.io.File(destinationURI.toFileString()))) {
                destinationWorkbook.write(fos);
            }
            
            destinationWorkbook.close();
            
            monitor.worked(20);
            
        } catch (Exception e) {
            result.getGenerationErrors().add(new Exception("Generation failed: " + e.getMessage(), e));
        } finally {
            monitor.done();
        }
        
        return result;
    }
    
    /**
     * Process a sheet, handling for loops and regular rows.
     */
    private static void processSheet(Sheet templateSheet, Sheet destSheet, 
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            GenerationResult result) {
        
        System.out.println("DEBUG: Processing sheet: " + templateSheet.getSheetName());
        System.out.println("DEBUG: Last row num: " + templateSheet.getLastRowNum());
        
        int destRowNum = 0;
        int templateRowNum = 0;
        int lastRowNum = templateSheet.getLastRowNum();
        
        while (templateRowNum <= lastRowNum) {
            Row templateRow = templateSheet.getRow(templateRowNum);
            
            if (templateRow == null) {
                System.out.println("DEBUG: Row " + templateRowNum + " is null, skipping");
                templateRowNum++;
                continue;
            }
            
            // Check if this row contains a for loop start
            Cell firstCell = templateRow.getCell(0);
            String firstCellContent = getCellContent(firstCell);
            
            System.out.println("DEBUG: Row " + templateRowNum + ", cell A content: " + firstCellContent);
            
            if (firstCellContent != null && isForLoopStart(firstCellContent)) {
                System.out.println("DEBUG: Detected for loop at row " + templateRowNum);
                // Parse for loop: {m:for var | collection}
                ForLoopInfo forLoop = parseForLoop(firstCellContent);
                
                if (forLoop != null) {
                    System.out.println("DEBUG: Parsed for loop: var=" + forLoop.varName + ", collection=" + forLoop.collectionExpr);
                    // Find the endfor row
                    int endforRow = findEndForRow(templateSheet, templateRowNum + 1);
                    System.out.println("DEBUG: Found endfor at row " + endforRow);
                    
                    if (endforRow > templateRowNum) {
                        // Process the for loop
                        destRowNum = processForLoop(
                            templateSheet, destSheet, 
                            templateRowNum, endforRow,
                            destRowNum, forLoop,
                            variables, queryEnvironment, result);
                        
                        // Skip to after endfor
                        templateRowNum = endforRow + 1;
                        continue;
                    } else {
                        result.getValidationMessages().add(
                            "Warning: {m:for} at row " + templateRowNum + " has no matching {m:endfor}");
                    }
                }
            }
            
            // Regular row (no for loop)
            System.out.println("DEBUG: Copying regular row " + templateRowNum + " to dest row " + destRowNum);
            copyRow(templateRow, destSheet.createRow(destRowNum), variables, queryEnvironment);
            destRowNum++;
            templateRowNum++;
        }
        
        System.out.println("DEBUG: Sheet processing complete, total dest rows: " + destRowNum);
    }
    
    /**
     * Process a for loop, repeating the body rows for each item in the collection.
     */
    private static int processForLoop(Sheet templateSheet, Sheet destSheet,
            int forRowNum, int endforRowNum, int destRowNum,
            ForLoopInfo forLoop, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment, GenerationResult result) {
        
        System.out.println("DEBUG: Processing for loop from row " + forRowNum + " to " + endforRowNum);
        System.out.println("DEBUG: Variable: " + forLoop.varName + ", Collection: " + forLoop.collectionExpr);
        
        // Evaluate the collection expression
        Object collectionObj;
        try {
            collectionObj = evaluateAqlExpression(forLoop.collectionExpr, variables, queryEnvironment);
            System.out.println("DEBUG: Collection evaluated to: " + (collectionObj != null ? collectionObj.getClass().getName() : "null"));
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to evaluate collection: " + e.getMessage());
            result.getGenerationErrors().add(new Exception(
                "Failed to evaluate for loop collection: " + forLoop.collectionExpr, e));
            return destRowNum;
        }
        
        // Convert to iterable
        java.util.Collection<?> collection;
        if (collectionObj instanceof java.util.Collection) {
            collection = (java.util.Collection<?>) collectionObj;
        } else if (collectionObj != null) {
            collection = java.util.Collections.singletonList(collectionObj);
        } else {
            result.getValidationMessages().add(
                "Warning: For loop collection is null at row " + forRowNum);
            return destRowNum;
        }
        
        System.out.println("DEBUG: Collection size: " + collection.size());
        System.out.println("DEBUG: Body rows: " + (forRowNum + 1) + " to " + (endforRowNum - 1));
        
        // Iterate over collection
        int index = 0;
        for (Object item : collection) {
            // Create new variable context with loop variable
            Map<String, Object> loopVars = new java.util.HashMap<>(variables);
            loopVars.put(forLoop.varName, item);
            loopVars.put(forLoop.varName + "Index", index);
            
            if (index < 3) {  // Debug first 3 iterations
                System.out.println("DEBUG: Iteration " + index + ", item: " + item);
            }
            
            index++;
            
            // Copy body rows (between for and endfor)
            for (int bodyRowNum = forRowNum + 1; bodyRowNum < endforRowNum; bodyRowNum++) {
                Row templateRow = templateSheet.getRow(bodyRowNum);
                if (templateRow != null) {
                    Row destRow = destSheet.createRow(destRowNum);
                    if (index <= 3) {  // Debug first 3
                        System.out.println("DEBUG:   Creating dest row " + destRowNum + " from template row " + bodyRowNum);
                    }
                    copyRow(templateRow, destRow, loopVars, queryEnvironment);
                    destRowNum++;
                }
            }
        }
        
        System.out.println("DEBUG: For loop complete, final dest row: " + destRowNum);
        return destRowNum;
    }
    
    /**
     * Copy a row from template to destination, evaluating expressions.
     */
    private static void copyRow(Row templateRow, Row destRow,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment) {
        
        // Get the last cell index to ensure we copy all columns
        short lastCellNum = templateRow.getLastCellNum();
        
        for (int cellIdx = 0; cellIdx < lastCellNum; cellIdx++) {
            Cell templateCell = templateRow.getCell(cellIdx);
            Cell destCell = destRow.createCell(cellIdx);
            
            if (templateCell == null) {
                // Empty cell in template, create empty dest cell
                continue;
            }
            
            // Get cell content
            String cellContent = getCellContent(templateCell);
            
            if (cellContent != null && containsExpression(cellContent)) {
                // Evaluate and replace expressions
                String evaluated = evaluateExpressions(cellContent, variables, queryEnvironment);
                destCell.setCellValue(evaluated);
            } else if (cellContent != null) {
                // Copy as-is
                destCell.setCellValue(cellContent);
            }
            
            // TODO: Copy cell style properly (need to clone to destination workbook)
            // For now, skip style copying to avoid workbook style source mismatch
            // if (templateCell.getCellStyle() != null) {
            //     destCell.setCellStyle(templateCell.getCellStyle());
            // }
        }
        
        // Copy row height
        destRow.setHeight(templateRow.getHeight());
    }
    
    /**
     * Check if content is a for loop start command.
     */
    private static boolean isForLoopStart(String content) {
        if (content == null) return false;
        String trimmed = content.trim();
        return trimmed.startsWith("{m:for ") && trimmed.endsWith("}");
    }
    
    /**
     * Check if content is a for loop end command.
     */
    private static boolean isForLoopEnd(String content) {
        if (content == null) return false;
        String trimmed = content.trim();
        return trimmed.equals("{m:endfor}");
    }
    
    /**
     * Parse for loop command: {m:for var | collection}
     */
    private static ForLoopInfo parseForLoop(String content) {
        // Remove {m:for and }
        String inner = content.trim();
        if (inner.startsWith("{m:for ")) {
            inner = inner.substring(7); // Remove "{m:for "
        }
        if (inner.endsWith("}")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        
        // Split by |
        String[] parts = inner.split("\\|");
        if (parts.length == 2) {
            String varName = parts[0].trim();
            String collectionExpr = parts[1].trim();
            return new ForLoopInfo(varName, collectionExpr);
        }
        
        return null;
    }
    
    /**
     * Find the row containing {m:endfor}
     */
    private static int findEndForRow(Sheet sheet, int startRow) {
        int lastRow = sheet.getLastRowNum();
        for (int rowNum = startRow; rowNum <= lastRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                Cell firstCell = row.getCell(0);
                String content = getCellContent(firstCell);
                if (isForLoopEnd(content)) {
                    return rowNum;
                }
            }
        }
        return -1;
    }
    
    /**
     * Data class for for loop information.
     */
    private static class ForLoopInfo {
        final String varName;
        final String collectionExpr;
        
        ForLoopInfo(String varName, String collectionExpr) {
            this.varName = varName;
            this.collectionExpr = collectionExpr;
        }
    }
    
    /**
     * Gets the string content of a cell.
     */
    private static String getCellContent(Cell cell) {
        if (cell == null) {
            return null;
        }
        
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
    
    /**
     * Checks if the content contains M2Spreadsheet expressions.
     */
    private static boolean containsExpression(String content) {
        return content != null && content.contains(M_FIELD_START) && content.contains(FIELD_END);
    }
    
    /**
     * Evaluates AQL expressions in the content string.
     * Uses Acceleo Query Language (AQL) for expression evaluation.
     */
    private static String evaluateExpressions(String text, Map<String, Object> variables, 
            IQueryEnvironment queryEnvironment) {
        String result = text;
        
        // Find all {m:expression} patterns
        int startIdx = 0;
        while ((startIdx = result.indexOf(M_FIELD_START, startIdx)) != -1) {
            int endIdx = result.indexOf(FIELD_END, startIdx);
            if (endIdx == -1) {
                break;
            }
            
            // Extract expression (without {m: and })
            String expression = result.substring(startIdx + M_FIELD_START.length(), endIdx).trim();
            
            // Evaluate using AQL (fallback to simple evaluator if AQL fails or is null)
            Object value;
            if (queryEnvironment != null) {
                try {
                    value = evaluateAqlExpression(expression, variables, queryEnvironment);
                } catch (Exception e) {
                    // Fallback to simple evaluator on AQL error
                    value = evaluateSimpleExpression(expression, variables);
                }
            } else {
                value = evaluateSimpleExpression(expression, variables);
            }
            
            String replacement = value != null ? value.toString() : "";
            
            // Replace the expression with its value
            result = result.substring(0, startIdx) + replacement + result.substring(endIdx + 1);
            startIdx += replacement.length();
        }
        
        return result;
    }
    
    /**
     * Evaluates an AQL expression using the query environment.
     */
    private static Object evaluateAqlExpression(String expression, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment) {
        // Create query builder engine
        IQueryBuilderEngine queryBuilder = new QueryBuilderEngine(queryEnvironment);
        
        // Parse the expression
        AstResult astResult = queryBuilder.build(expression);
        
        if (!astResult.getDiagnostic().getChildren().isEmpty()) {
            // If there are parsing errors, throw exception to fallback to simple evaluator
            throw new RuntimeException("AQL parse error: " + astResult.getDiagnostic());
        }
        
        // Create evaluation engine and evaluate
        IQueryEvaluationEngine evaluationEngine = new QueryEvaluationEngine(queryEnvironment);
        EvaluationResult evalResult = evaluationEngine.eval(astResult, variables);
        
        // Check for evaluation errors (not warnings) - look for ERROR severity
        if (evalResult.getDiagnostic() != null && hasErrors(evalResult.getDiagnostic())) {
            throw new RuntimeException("AQL evaluation error: " + evalResult.getDiagnostic());
        }
        
        // If there are warnings but no errors, check if we got null and fallback
        Object result = evalResult.getResult();
        if (result == null && evalResult.getDiagnostic() != null && !evalResult.getDiagnostic().getChildren().isEmpty()) {
            throw new RuntimeException("AQL returned null: " + evalResult.getDiagnostic());
        }
        
        return result;
    }
    
    /**
     * Check if a diagnostic contains errors (not just warnings).
     */
    private static boolean hasErrors(org.eclipse.emf.common.util.Diagnostic diagnostic) {
        if (diagnostic.getSeverity() >= org.eclipse.emf.common.util.Diagnostic.ERROR) {
            return true;
        }
        for (org.eclipse.emf.common.util.Diagnostic child : diagnostic.getChildren()) {
            if (hasErrors(child)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Simple expression evaluator - handles basic variable access like "customer.name".
     * TODO: Replace with proper AQL evaluation
     */
    private static Object evaluateSimpleExpression(String expression, Map<String, Object> variables) {
        // Handle simple property access: customer.name
        String[] parts = expression.split("\\.");
        
        if (parts.length == 0) {
            return null;
        }
        
        Object current = variables.get(parts[0]);
        
        // Navigate through properties
        for (int i = 1; i < parts.length && current != null; i++) {
            // For now, if current is a Map, try to get the next property
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                // TODO: Use reflection or AQL to access properties
                // For now, just return null if we can't navigate further
                return null;
            }
        }
        
        return current;
    }
}
